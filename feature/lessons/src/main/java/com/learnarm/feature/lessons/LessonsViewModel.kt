package com.learnarm.feature.lessons

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.learnarm.core.audio.AudioRequest
import com.learnarm.core.audio.LetterAudioPlayer
import com.learnarm.core.audio.PronunciationScorer
import com.learnarm.core.audio.ScoreResult
import com.learnarm.core.audio.SpeechRecorder
import com.learnarm.core.database.entity.LessonEntity
import com.learnarm.core.database.entity.LessonStepEntity
import com.learnarm.core.database.entity.LetterEntity
import com.learnarm.core.database.entity.PhraseEntity
import com.learnarm.core.data.repository.LessonRepository
import com.learnarm.core.data.repository.LetterRepository
import com.learnarm.core.data.repository.PhraseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LessonsListViewModel @Inject constructor(
    private val lessonRepository: LessonRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<LessonsListUiState>(LessonsListUiState.Loading)
    val uiState: StateFlow<LessonsListUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            lessonRepository.observeLessons().collect { lessons ->
                _uiState.value = if (lessons.isEmpty()) {
                    LessonsListUiState.Empty
                } else {
                    LessonsListUiState.Content(lessons)
                }
            }
        }
    }
}

sealed class LessonsListUiState {
    object Loading : LessonsListUiState()
    object Empty : LessonsListUiState()
    data class Content(val lessons: List<LessonEntity>) : LessonsListUiState()
}

enum class PracticePhase { Idle, Recording, Scoring }

@HiltViewModel
class LessonRunnerViewModel @Inject constructor(
    private val lessonRepository: LessonRepository,
    private val letterRepository: LetterRepository,
    private val phraseRepository: PhraseRepository,
    private val recorder: SpeechRecorder,
    private val scorer: PronunciationScorer,
    private val audioPlayer: LetterAudioPlayer,
) : ViewModel() {

    private val _lessonId = MutableStateFlow(1)
    private val _uiState = MutableStateFlow<LessonRunnerUiState>(LessonRunnerUiState.Loading)
    val uiState: StateFlow<LessonRunnerUiState> = _uiState.asStateFlow()

    private val _currentStepIndex = MutableStateFlow(0)
    val currentStepIndex: StateFlow<Int> = _currentStepIndex.asStateFlow()

    private val _completedStepIds = MutableStateFlow<Set<String>>(emptySet())
    val completedStepIds: StateFlow<Set<String>> = _completedStepIds.asStateFlow()

    private val _stepLetters = MutableStateFlow<Map<String, LetterEntity>>(emptyMap())
    val stepLetters: StateFlow<Map<String, LetterEntity>> = _stepLetters.asStateFlow()

    private val _stepPhrases = MutableStateFlow<Map<String, PhraseEntity>>(emptyMap())
    val stepPhrases: StateFlow<Map<String, PhraseEntity>> = _stepPhrases.asStateFlow()

    private val _practicePhase = MutableStateFlow(PracticePhase.Idle)
    val practicePhase: StateFlow<PracticePhase> = _practicePhase.asStateFlow()

    private val _practiceResult = MutableStateFlow<ScoreResult.Success?>(null)
    val practiceResult: StateFlow<ScoreResult.Success?> = _practiceResult.asStateFlow()

    private val _practiceError = MutableStateFlow<ScoreResult.Reason?>(null)
    val practiceError: StateFlow<ScoreResult.Reason?> = _practiceError.asStateFlow()

    private var scoreJob: Job? = null

    fun setLessonId(id: Int) {
        _lessonId.value = id
        loadLesson(id)
    }

    private fun loadLesson(lessonId: Int) {
        viewModelScope.launch {
            val lesson = lessonRepository.getLesson(lessonId)
            if (lesson != null) {
                val steps = lessonRepository.getSteps(lessonId)
                _uiState.value = LessonRunnerUiState.Content(lesson, steps)
                // Pre-load letters and phrases referenced in the steps
                loadStepContent(steps)
            } else {
                _uiState.value = LessonRunnerUiState.Error("درس یافت نشد")
            }
        }
    }

    private fun loadStepContent(steps: List<LessonStepEntity>) {
        viewModelScope.launch {
            val letters = mutableMapOf<String, LetterEntity>()
            val phrases = mutableMapOf<String, PhraseEntity>()

            steps.forEach { step ->
                when (step.type) {
                    "SHOW_LETTER" -> {
                        try {
                            val letterId = step.itemKey.substringAfter(":").toIntOrNull() ?: return@forEach
                            val letter = letterRepository.getById(letterId)
                            if (letter != null) {
                                letters[step.itemKey] = letter
                            }
                        } catch (e: Exception) {
                            // Silently handle errors
                        }
                    }
                    "SHOW_PHRASE" -> {
                        try {
                            val phraseId = step.itemKey.substringAfter(":").toIntOrNull() ?: return@forEach
                            val phrase = phraseRepository.getById(phraseId)
                            if (phrase != null) {
                                phrases[step.itemKey] = phrase
                            }
                        } catch (e: Exception) {
                            // Silently handle errors
                        }
                    }
                    "QUIZ_LETTER", "QUIZ_PHRASE" -> {
                        try {
                            val itemId = step.itemKey.substringAfter(":").toIntOrNull() ?: return@forEach
                            if (step.type == "QUIZ_LETTER") {
                                val letter = letterRepository.getById(itemId)
                                if (letter != null) {
                                    letters[step.itemKey] = letter
                                }
                            } else {
                                val phrase = phraseRepository.getById(itemId)
                                if (phrase != null) {
                                    phrases[step.itemKey] = phrase
                                }
                            }
                        } catch (e: Exception) {
                            // Silently handle errors
                        }
                    }
                    "PRACTICE_PHRASE" -> {
                        try {
                            val phraseId = step.itemKey.substringAfter(":").toIntOrNull() ?: return@forEach
                            val phrase = phraseRepository.getById(phraseId)
                            if (phrase != null) {
                                phrases[step.itemKey] = phrase
                            }
                        } catch (e: Exception) {
                            // Silently handle errors
                        }
                    }
                }
            }

            _stepLetters.value = letters
            _stepPhrases.value = phrases
        }
    }

    fun markStepCompleted(stepId: String) {
        _completedStepIds.value = _completedStepIds.value + stepId
    }

    fun moveToNextStep() {
        val current = _currentStepIndex.value
        _currentStepIndex.value = current + 1
    }

    fun isLessonComplete(totalSteps: Int): Boolean {
        return _completedStepIds.value.size >= totalSteps
    }

    fun startRecording() {
        if (_practicePhase.value != PracticePhase.Idle) return
        val started = recorder.start()
        if (!started) {
            _practiceError.value = ScoreResult.Reason.EmptyAudio
            return
        }
        _practicePhase.value = PracticePhase.Recording
        _practiceResult.value = null
        _practiceError.value = null
    }

    fun stopAndScore(armenianText: String) {
        if (_practicePhase.value != PracticePhase.Recording) return
        val file = recorder.stop()
        if (file == null) {
            _practicePhase.value = PracticePhase.Idle
            _practiceError.value = ScoreResult.Reason.EmptyAudio
            return
        }
        _practicePhase.value = PracticePhase.Scoring
        scoreJob = viewModelScope.launch {
            val outcome = scorer.score(file, armenianText)
            file.delete()
            val nextState = when (outcome) {
                is ScoreResult.Success -> {
                    _practiceResult.value = outcome
                    _practiceError.value = null
                    PracticePhase.Idle
                }
                is ScoreResult.Failed -> {
                    _practiceResult.value = null
                    _practiceError.value = outcome.reason
                    PracticePhase.Idle
                }
            }
            _practicePhase.value = nextState
        }
    }

    fun cancelRecording() {
        recorder.cancel()
        if (_practicePhase.value == PracticePhase.Recording) {
            _practicePhase.value = PracticePhase.Idle
        }
    }

    fun clearPracticeResult() {
        _practiceResult.value = null
        _practiceError.value = null
        _practicePhase.value = PracticePhase.Idle
    }

    fun playPhraseAudio(armenianText: String) {
        viewModelScope.launch {
            audioPlayer.play(AudioRequest(
                key = "phrase_${armenianText.hashCode()}",
                text = armenianText,
                voice = "hy-default",
            ))
        }
    }

    override fun onCleared() {
        scoreJob?.cancel()
        recorder.cancel()
        super.onCleared()
    }
}

sealed class LessonRunnerUiState {
    object Loading : LessonRunnerUiState()
    data class Content(val lesson: LessonEntity, val steps: List<LessonStepEntity>) : LessonRunnerUiState()
    data class Error(val message: String) : LessonRunnerUiState()
}
