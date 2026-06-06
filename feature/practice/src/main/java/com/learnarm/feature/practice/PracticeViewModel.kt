package com.learnarm.feature.practice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.learnarm.core.audio.PronunciationScorer
import com.learnarm.core.audio.ScoreResult
import com.learnarm.core.audio.SpeechRecorder
import com.learnarm.core.data.repository.PhraseRepository
import com.learnarm.core.data.repository.ReviewRepository
import com.learnarm.core.data.srs.ReviewItem
import com.learnarm.core.data.srs.ReviewQuality
import com.learnarm.core.data.streak.StreakStore
import com.learnarm.core.database.entity.PhraseEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class PracticePhase { Idle, Recording, Scoring }

sealed interface PracticeUiState {
    data object Loading : PracticeUiState
    data object NoPhrases : PracticeUiState
    data class Ready(
        val target: PhraseEntity,
        val phase: PracticePhase,
        val result: ScoreResult.Success? = null,
        val error: ScoreResult.Reason? = null,
    ) : PracticeUiState
}

@HiltViewModel
class PracticeViewModel @Inject constructor(
    private val repository: PhraseRepository,
    private val recorder: SpeechRecorder,
    private val scorer: PronunciationScorer,
    private val reviewRepository: ReviewRepository,
    private val streakStore: StreakStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow<PracticeUiState>(PracticeUiState.Loading)
    val uiState: StateFlow<PracticeUiState> = _uiState.asStateFlow()

    private var pool: List<PhraseEntity> = emptyList()
    private var lastTargetId: Int? = null
    private var scoreJob: Job? = null

    init {
        viewModelScope.launch {
            pool = repository.observePhrases().first()
            pickNext()
        }
    }

    fun pickNext() {
        scoreJob?.cancel()
        recorder.cancel()
        val candidates = if (pool.size > 1) pool.filter { it.id != lastTargetId } else pool
        val next = candidates.randomOrNull() ?: pool.firstOrNull()
        if (next == null) {
            _uiState.value = PracticeUiState.NoPhrases
            return
        }
        lastTargetId = next.id
        _uiState.value = PracticeUiState.Ready(target = next, phase = PracticePhase.Idle)
    }

    fun startRecording() {
        val ready = _uiState.value as? PracticeUiState.Ready ?: return
        if (ready.phase != PracticePhase.Idle) return
        val started = recorder.start()
        if (!started) {
            _uiState.value = ready.copy(error = ScoreResult.Reason.EmptyAudio)
            return
        }
        _uiState.value = ready.copy(
            phase = PracticePhase.Recording,
            result = null,
            error = null,
        )
    }

    fun stopAndScore() {
        val ready = _uiState.value as? PracticeUiState.Ready ?: return
        if (ready.phase != PracticePhase.Recording) return
        val file = recorder.stop()
        if (file == null) {
            _uiState.value = ready.copy(
                phase = PracticePhase.Idle,
                error = ScoreResult.Reason.EmptyAudio,
            )
            return
        }
        _uiState.value = ready.copy(phase = PracticePhase.Scoring)
        scoreJob = viewModelScope.launch {
            val outcome = scorer.score(file, ready.target.armenian)
            file.delete()
            if (outcome is ScoreResult.Success) {
                reviewRepository.recordReview(
                    key = ReviewItem.phraseKey(ready.target.id),
                    quality = qualityFromScore(outcome.score),
                )
                streakStore.recordStudiedToday()
            }
            val nextState = when (outcome) {
                is ScoreResult.Success -> ready.copy(
                    phase = PracticePhase.Idle,
                    result = outcome,
                    error = null,
                )
                is ScoreResult.Failed -> ready.copy(
                    phase = PracticePhase.Idle,
                    error = outcome.reason,
                )
            }
            if (_uiState.value is PracticeUiState.Ready) {
                _uiState.value = nextState
            }
        }
    }

    private fun qualityFromScore(score: Float): ReviewQuality = when {
        score >= 0.9f -> ReviewQuality.Easy
        score >= 0.7f -> ReviewQuality.Good
        score >= 0.4f -> ReviewQuality.Hard
        else -> ReviewQuality.Again
    }

    fun cancelRecording() {
        recorder.cancel()
        val ready = _uiState.value as? PracticeUiState.Ready ?: return
        if (ready.phase == PracticePhase.Recording) {
            _uiState.value = ready.copy(phase = PracticePhase.Idle)
        }
    }

    override fun onCleared() {
        scoreJob?.cancel()
        recorder.cancel()
        super.onCleared()
    }
}
