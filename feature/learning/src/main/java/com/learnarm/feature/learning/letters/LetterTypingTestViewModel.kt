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

sealed interface TypingTestUiState {
    data object Loading : TypingTestUiState

    /** A question being answered (or showing an inter-question ack flash). */
    data class Question(
        val current: Int,                  // 1-based index
        val total: Int,
        val letter: LetterEntity,
        val input: String = "",
        val playingKey: String? = null,
        val hintShown: Boolean = false,    // 8s passed without submit
        val ack: AnswerAck? = null,        // brief overlay shown after submit, then advance
        val correctSoFar: Int = 0,
        val wrongSoFar: Int = 0,
    ) : TypingTestUiState

    /** All questions answered; verdict computed from threshold. */
    data class Result(
        val correct: Int,
        val total: Int,
        val passThresholdPercent: Int,
        val passed: Boolean,
        val isReview: Boolean,
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

    private val _uiState = MutableStateFlow<TypingTestUiState>(TypingTestUiState.Loading)
    val uiState: StateFlow<TypingTestUiState> = _uiState.asStateFlow()

    private var letters: List<LetterEntity> = emptyList()
    private var curriculum: LetterCurriculumDto = FALLBACK_LETTER_CURRICULUM
    private val marks: MutableList<Mark> = mutableListOf()
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
            // Always shuffle for both modes — prevents memorizing sequence.
            letters = ids.mapNotNull { byId[it] }.shuffled()

            if (letters.isEmpty()) {
                finishWithResult(correct = 0, total = 0)
                return@launch
            }

            marks.clear()
            presentQuestion(index1Based = 1)
        }
    }

    private fun presentQuestion(index1Based: Int) {
        _uiState.value = TypingTestUiState.Question(
            current = index1Based,
            total = letters.size,
            letter = letters[index1Based - 1],
            correctSoFar = marks.count { it == Mark.Correct },
            wrongSoFar = marks.count { it != Mark.Correct },
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
        // Don't allow editing while the ack overlay is showing (transition is brief).
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
        val mark: Mark = when {
            state.hintShown -> Mark.Timeout       // hint shown → never count as correct
            correct -> Mark.Correct
            else -> Mark.Wrong
        }
        marks += mark

        // Briefly show ack, then advance.
        _uiState.value = state.copy(
            ack = AnswerAck(mark = mark, correctLetter = state.letter, userInput = typed),
            correctSoFar = marks.count { it == Mark.Correct },
            wrongSoFar = marks.count { it != Mark.Correct },
        )
        viewModelScope.launch {
            kotlinx.coroutines.delay(if (mark == Mark.Correct) 600L else 1_200L)
            advance()
        }
    }

    private fun advance() {
        val state = _uiState.value as? TypingTestUiState.Question ?: return
        if (state.current >= state.total) {
            finishWithResult(
                correct = marks.count { it == Mark.Correct },
                total = letters.size,
            )
        } else {
            presentQuestion(index1Based = state.current + 1)
        }
    }

    private fun finishWithResult(correct: Int, total: Int) {
        val percent = if (total == 0) 0 else (correct * 100) / total
        val threshold = curriculum.passThresholdPercent
        val passed = percent >= threshold
        if (passed) {
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
        )
    }

    fun onRetry() {
        if (letters.isEmpty()) return
        hintJob?.cancel()
        marks.clear()
        // Re-shuffle for a fresh order on every retry — forces listening over memorization.
        letters = letters.shuffled()
        presentQuestion(index1Based = 1)
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
