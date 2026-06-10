package com.learnarm.feature.learning.letters

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.learnarm.core.audio.AudioRequest
import com.learnarm.core.audio.LetterAudioPlayer
import com.learnarm.core.data.api.CurriculumService
import com.learnarm.core.data.api.LetterCurriculumDto
import com.learnarm.core.data.progress.ProgressStore
import com.learnarm.core.data.repository.LetterRepository
import com.learnarm.core.database.entity.LetterEntity
import com.learnarm.feature.learning.navigation.LetterTypingTestRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class Mark { Correct, Wrong, Timeout }

enum class TestPhase { Main, Retry }

sealed interface TypingTestUiState {
    data object Loading : TypingTestUiState

    /** A question being answered (or showing an inter-question ack flash). */
    data class Question(
        val phase: TestPhase,
        val retryRound: Int,                // 0 in main pass, 1+ in retry rounds
        val current: Int,                  // 1-based within the current queue
        val total: Int,                    // size of the current queue
        val letter: LetterEntity,
        val input: String = "",
        val playingKey: String? = null,
        val hintShown: Boolean = false,    // hint timer elapsed
        val ack: AnswerAck? = null,        // brief overlay after a submit
        val correctSoFar: Int = 0,         // first-attempt corrects in main pass
        val wrongSoFar: Int = 0,           // first-attempt non-corrects in main pass
        val attemptInThisRound: Int = 0,   // attempts so far on this letter in this round
    ) : TypingTestUiState

    /** All rounds done; verdict computed from first-pass first-attempt corrects. */
    data class Result(
        val correct: Int,
        val total: Int,
        val passThresholdPercent: Int,
        val passed: Boolean,
        val isReview: Boolean,
        val forReplay: Boolean,
    ) : TypingTestUiState
}

/** Inter-question flash overlay describing how the last answer scored. */
data class AnswerAck(
    val mark: Mark,
    val correctLetter: LetterEntity,
    val userInput: String,
)

@HiltViewModel
class LetterTypingTestViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val curriculumService: CurriculumService,
    private val letterRepository: LetterRepository,
    private val audioPlayer: LetterAudioPlayer,
    private val progressStore: ProgressStore,
) : ViewModel() {

    private val route: LetterTypingTestRoute = savedStateHandle.toRoute<LetterTypingTestRoute>()
    val batchIndex: Int = route.batchIndex
    val isReview: Boolean = route.isReview
    val forReplay: Boolean = route.forReplay

    private val _uiState = MutableStateFlow<TypingTestUiState>(TypingTestUiState.Loading)
    val uiState: StateFlow<TypingTestUiState> = _uiState.asStateFlow()

    private var curriculum: LetterCurriculumDto = FALLBACK_LETTER_CURRICULUM

    // The original first-pass letter set (preserved across retries for scoring + reset).
    private var mainPassLetters: List<LetterEntity> = emptyList()
    // The queue being presented in the current round (main or retry).
    private var currentQueue: List<LetterEntity> = emptyList()
    private var currentIndex: Int = 0

    private var phase: TestPhase = TestPhase.Main
    private var retryRound: Int = 0
    private var attemptInThisRound: Int = 0

    // Map of letterId → mark for the first attempt during the MAIN pass. Locked once set.
    private val firstAttemptMarks = mutableMapOf<Int, Mark>()
    // Letter ids that were first-attempt-correct in the CURRENT retry round.
    private val cleanThisRound = mutableSetOf<Int>()

    private var playJob: Job? = null
    private var hintJob: Job? = null

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            curriculum = try {
                curriculumService.getLetterCurriculum()
            } catch (e: Exception) {
                FALLBACK_LETTER_CURRICULUM
            }

            val ids: List<Int> = if (isReview) {
                curriculum.cumulativeLetterIds(batchIndex)
            } else {
                curriculum.batches.firstOrNull { it.index == batchIndex }?.letterIds.orEmpty()
            }

            val allLetters = letterRepository.observeLetters().first()
            val byId = allLetters.associateBy { it.id }
            mainPassLetters = ids.mapNotNull { byId[it] }.shuffled()

            if (mainPassLetters.isEmpty()) {
                finishWithResult()
                return@launch
            }

            firstAttemptMarks.clear()
            cleanThisRound.clear()
            phase = TestPhase.Main
            retryRound = 0
            currentQueue = mainPassLetters
            currentIndex = 0
            presentCurrent()
        }
    }

    private fun presentCurrent() {
        attemptInThisRound = 0
        _uiState.value = TypingTestUiState.Question(
            phase = phase,
            retryRound = retryRound,
            current = currentIndex + 1,
            total = currentQueue.size,
            letter = currentQueue[currentIndex],
            correctSoFar = firstAttemptMarks.count { it.value == Mark.Correct },
            wrongSoFar = firstAttemptMarks.count { it.value != Mark.Correct },
            attemptInThisRound = 0,
        )
        playCurrent()
        startHintTimer()
    }

    private fun startHintTimer() {
        hintJob?.cancel()
        val timeoutMs = curriculum.hintTimeoutMs
        if (timeoutMs <= 0) return
        hintJob = viewModelScope.launch {
            kotlinx.coroutines.delay(timeoutMs)
            val state = _uiState.value as? TypingTestUiState.Question ?: return@launch
            if (state.hintShown || state.ack != null) return@launch
            _uiState.value = state.copy(hintShown = true)
        }
    }

    fun onInputChanged(value: String) {
        val state = _uiState.value as? TypingTestUiState.Question ?: return
        if (state.ack != null) return
        _uiState.value = state.copy(input = value)
    }

    fun onSubmit() {
        val state = _uiState.value as? TypingTestUiState.Question ?: return
        if (state.ack != null) return
        val typed = state.input.trim()
        if (typed.isEmpty()) return

        hintJob?.cancel()
        val correct = isCorrect(typed, state.letter)
        val isFirstAttemptThisRound = attemptInThisRound == 0
        val mark: Mark = when {
            correct -> Mark.Correct
            state.hintShown -> Mark.Timeout
            else -> Mark.Wrong
        }

        // Score-tracking is per-round-first-attempt. Subsequent attempts on the same
        // letter in the same round don't move the dial — they only gate advancement.
        if (isFirstAttemptThisRound) {
            if (phase == TestPhase.Main) {
                firstAttemptMarks[state.letter.id] = mark
            } else if (mark == Mark.Correct) {
                cleanThisRound += state.letter.id
            }
        }

        attemptInThisRound++

        val newQuestion = state.copy(
            ack = AnswerAck(mark = mark, correctLetter = state.letter, userInput = typed),
            correctSoFar = firstAttemptMarks.count { it.value == Mark.Correct },
            wrongSoFar = firstAttemptMarks.count { it.value != Mark.Correct },
            attemptInThisRound = attemptInThisRound,
        )
        _uiState.value = newQuestion

        viewModelScope.launch {
            if (mark == Mark.Correct) {
                kotlinx.coroutines.delay(600L)
                advance()
            } else {
                // Wrong/Timeout: brief flash, then clear input + ack so the user can
                // retry the SAME letter. Hint stays if it was shown.
                kotlinx.coroutines.delay(1_200L)
                val cur = _uiState.value as? TypingTestUiState.Question ?: return@launch
                _uiState.value = cur.copy(
                    ack = null,
                    input = "",
                )
                // Give the user another hint window before the answer is revealed.
                if (!cur.hintShown) startHintTimer()
            }
        }
    }

    private fun advance() {
        if (currentIndex + 1 < currentQueue.size) {
            currentIndex++
            presentCurrent()
            return
        }

        // End of current round — decide what comes next.
        when (phase) {
            TestPhase.Main -> {
                val retryIds = firstAttemptMarks.entries
                    .filter { it.value != Mark.Correct }
                    .map { it.key }
                    .toSet()
                val retryLetters = mainPassLetters.filter { it.id in retryIds }
                if (retryLetters.isEmpty()) {
                    finishWithResult()
                } else {
                    phase = TestPhase.Retry
                    retryRound = 1
                    currentQueue = retryLetters.shuffled()
                    currentIndex = 0
                    cleanThisRound.clear()
                    presentCurrent()
                }
            }
            TestPhase.Retry -> {
                val dirty = currentQueue.filter { it.id !in cleanThisRound }
                if (dirty.isEmpty()) {
                    finishWithResult()
                } else {
                    retryRound += 1
                    currentQueue = dirty.shuffled()
                    currentIndex = 0
                    cleanThisRound.clear()
                    presentCurrent()
                }
            }
        }
    }

    private fun finishWithResult() {
        val correct = firstAttemptMarks.count { it.value == Mark.Correct }
        val total = mainPassLetters.size
        val percent = if (total == 0) 0 else (correct * 100) / total
        val threshold = curriculum.passThresholdPercent
        val passed = percent >= threshold
        if (passed && !forReplay) {
            viewModelScope.launch {
                if (isReview) {
                    val roundIndex = if (batchIndex >= 8) 2 else 1
                    progressStore.recordLettersReviewPassed(roundIndex)
                } else {
                    progressStore.recordLettersBatchPassed(batchIndex)
                }
            }
        }
        _uiState.value = TypingTestUiState.Result(
            correct = correct,
            total = total,
            passThresholdPercent = threshold,
            passed = passed,
            isReview = isReview,
            forReplay = forReplay,
        )
    }

    fun onRetry() {
        if (mainPassLetters.isEmpty()) return
        hintJob?.cancel()
        firstAttemptMarks.clear()
        cleanThisRound.clear()
        phase = TestPhase.Main
        retryRound = 0
        currentQueue = mainPassLetters.shuffled()
        mainPassLetters = currentQueue
        currentIndex = 0
        presentCurrent()
    }

    /**
     * Escape hatch when the user can't physically type a letter (digraph keys missing,
     * IME layout issues, etc.). Only available after the hint has been shown. Records
     * the first-attempt mark as Timeout if it isn't already set, then advances.
     */
    fun skipCurrentQuestion() {
        val state = _uiState.value as? TypingTestUiState.Question ?: return
        if (!state.hintShown) return
        hintJob?.cancel()
        if (phase == TestPhase.Main && state.letter.id !in firstAttemptMarks) {
            firstAttemptMarks[state.letter.id] = Mark.Timeout
        }
        advance()
    }

    fun onReplayAudio() {
        playCurrent()
    }

    private fun playCurrent() {
        val state = _uiState.value as? TypingTestUiState.Question ?: return
        play(key = "letter-${state.letter.id}", text = state.letter.name)
    }

    private fun play(key: String, text: String) {
        playJob?.cancel()
        updatePlayingKey(key)
        playJob = viewModelScope.launch {
            audioPlayer.play(AudioRequest(key = key, text = text))
            updatePlayingKey(null)
        }
    }

    private fun updatePlayingKey(key: String?) {
        val state = _uiState.value
        if (state is TypingTestUiState.Question) {
            _uiState.value = state.copy(playingKey = key)
        }
    }

    private fun isCorrect(typed: String, letter: LetterEntity): Boolean {
        if (typed == letter.lower || typed == letter.upper) return true
        // Special-case the ev ligature: accept the decomposed forms.
        if (letter.id == 39 && (typed == "եւ" || typed == "ԵՒ")) return true
        return false
    }

    override fun onCleared() {
        hintJob?.cancel()
        audioPlayer.stop()
        super.onCleared()
    }
}
