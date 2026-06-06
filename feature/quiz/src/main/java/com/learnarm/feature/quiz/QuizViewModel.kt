package com.learnarm.feature.quiz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.learnarm.core.data.repository.LetterRepository
import com.learnarm.core.data.repository.ReviewRepository
import com.learnarm.core.data.srs.ReviewItem
import com.learnarm.core.data.srs.ReviewQuality
import com.learnarm.core.data.streak.StreakStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface QuizUiState {
    data object Loading : QuizUiState
    data object NotEnoughLetters : QuizUiState
    data class Playing(
        val question: QuizQuestion,
        val questionNumber: Int,
        val totalQuestions: Int,
        val score: Int,
        val selected: SelectedAnswer? = null,
    ) : QuizUiState
    data class Finished(
        val score: Int,
        val totalQuestions: Int,
    ) : QuizUiState
}

data class SelectedAnswer(
    val index: Int,
    val isCorrect: Boolean,
)

@HiltViewModel
class QuizViewModel @Inject constructor(
    private val repository: LetterRepository,
    private val reviewRepository: ReviewRepository,
    private val streakStore: StreakStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow<QuizUiState>(QuizUiState.Loading)
    val uiState: StateFlow<QuizUiState> = _uiState.asStateFlow()

    private var questions: List<QuizQuestion> = emptyList()
    private var index: Int = 0
    private var score: Int = 0
    private var advanceJob: Job? = null

    init {
        startNewRound()
    }

    fun startNewRound() {
        advanceJob?.cancel()
        viewModelScope.launch {
            _uiState.value = QuizUiState.Loading
            val letters = repository.observeLetters().first()
            if (letters.size < QuizGenerator.OPTIONS_PER_QUESTION) {
                _uiState.value = QuizUiState.NotEnoughLetters
                return@launch
            }
            questions = QuizGenerator.generateRound(letters, questionCount = ROUND_SIZE)
            index = 0
            score = 0
            emitPlaying()
        }
    }

    fun onAnswerSelected(optionIndex: Int) {
        val current = _uiState.value as? QuizUiState.Playing ?: return
        if (current.selected != null) return

        val isCorrect = optionIndex == current.question.correctIndex
        if (isCorrect) score++

        _uiState.value = current.copy(
            score = score,
            selected = SelectedAnswer(index = optionIndex, isCorrect = isCorrect),
        )

        viewModelScope.launch {
            reviewRepository.recordReview(
                key = ReviewItem.letterKey(current.question.letter.id),
                quality = if (isCorrect) ReviewQuality.Good else ReviewQuality.Again,
            )
            streakStore.recordStudiedToday()
        }

        advanceJob?.cancel()
        advanceJob = viewModelScope.launch {
            delay(ADVANCE_DELAY_MS)
            advance()
        }
    }

    private fun advance() {
        index++
        if (index >= questions.size) {
            _uiState.value = QuizUiState.Finished(score = score, totalQuestions = questions.size)
        } else {
            emitPlaying()
        }
    }

    private fun emitPlaying() {
        _uiState.value = QuizUiState.Playing(
            question = questions[index],
            questionNumber = index + 1,
            totalQuestions = questions.size,
            score = score,
            selected = null,
        )
    }

    override fun onCleared() {
        advanceJob?.cancel()
        super.onCleared()
    }

    companion object {
        const val ROUND_SIZE = 10
        const val ADVANCE_DELAY_MS = 1100L
    }
}
