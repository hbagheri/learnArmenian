package com.learnarm.feature.learning.letters

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.learnarm.core.audio.AudioRequest
import com.learnarm.core.audio.LetterAudioPlayer
import com.learnarm.core.audio.PlayResult
import com.learnarm.core.data.api.BatchWordDto
import com.learnarm.core.data.api.CurriculumService
import com.learnarm.core.data.api.LetterBatchDto
import com.learnarm.core.data.api.LetterCurriculumDto
import com.learnarm.core.data.progress.ProgressStore
import com.learnarm.core.data.repository.LetterRepository
import com.learnarm.core.database.entity.LetterEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface LetterLessonUiState {
    data object Loading : LetterLessonUiState

    /** A learning lesson for batch [batchIndex] (currentBatch + 1). */
    data class BatchLesson(
        val batchIndex: Int,
        val totalBatches: Int,
        val passedCount: Int,
        val batch: LetterBatchDto,
        val batchLetters: List<LetterEntity>,
        val playingKey: String? = null,
    ) : LetterLessonUiState

    /** End-of-round periodic review is pending — user must clear it before next batch. */
    data class ReviewPending(
        val roundIndex: Int,
        val justFinishedBatch: Int,
        val cumulativeLetterIds: List<Int>,
    ) : LetterLessonUiState

    /** All 10 batches + both reviews cleared — direct user to the final matching-game exam. */
    data object AllBatchesCompleted : LetterLessonUiState
}

@HiltViewModel
class LetterLessonViewModel @Inject constructor(
    private val curriculumService: CurriculumService,
    private val letterRepository: LetterRepository,
    private val progressStore: ProgressStore,
    private val audioPlayer: LetterAudioPlayer,
) : ViewModel() {

    private val _uiState = MutableStateFlow<LetterLessonUiState>(LetterLessonUiState.Loading)
    val uiState: StateFlow<LetterLessonUiState> = _uiState.asStateFlow()

    private var curriculum: LetterCurriculumDto = FALLBACK_LETTER_CURRICULUM
    private var playJob: Job? = null

    init {
        viewModelScope.launch {
            curriculum = try {
                curriculumService.getLetterCurriculum()
            } catch (e: Exception) {
                FALLBACK_LETTER_CURRICULUM
            }
            // Combine progress + letters so the lesson screen reflects the latest passed-batch
            // state. (Letters table is seeded once and stable.)
            combine(
                progressStore.lettersCurrentBatch,
                progressStore.lettersReviewsPassed,
                letterRepository.observeLetters(),
            ) { currentBatch, reviewsPassed, letters ->
                computeState(currentBatch, reviewsPassed, letters)
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    private fun computeState(
        currentBatch: Int,
        reviewsPassed: Int,
        letters: List<LetterEntity>,
    ): LetterLessonUiState {
        val total = curriculum.batches.size

        // All batches done — has the user also cleared all required reviews?
        val expectedReviews = curriculum.periodicReviewBatches.count { it <= currentBatch }
        if (currentBatch >= total && reviewsPassed >= expectedReviews) {
            return LetterLessonUiState.AllBatchesCompleted
        }

        // Pending review? (just-finished batch is a review boundary AND review not yet passed)
        val nextReviewBoundary = curriculum.periodicReviewBatches
            .firstOrNull { it == currentBatch && reviewsPassed < (curriculum.periodicReviewBatches.indexOf(it) + 1) }
        if (nextReviewBoundary != null) {
            val roundIndex = curriculum.periodicReviewBatches.indexOf(nextReviewBoundary) + 1
            return LetterLessonUiState.ReviewPending(
                roundIndex = roundIndex,
                justFinishedBatch = nextReviewBoundary,
                cumulativeLetterIds = curriculum.cumulativeLetterIds(nextReviewBoundary),
            )
        }

        // Otherwise: show next batch's lesson.
        val nextBatchIndex = currentBatch + 1
        val batch = curriculum.batches.firstOrNull { it.index == nextBatchIndex }
            ?: return LetterLessonUiState.AllBatchesCompleted

        val byId = letters.associateBy { it.id }
        val batchLetters = batch.letterIds.mapNotNull { byId[it] }

        return LetterLessonUiState.BatchLesson(
            batchIndex = nextBatchIndex,
            totalBatches = total,
            passedCount = currentBatch,
            batch = batch,
            batchLetters = batchLetters,
            playingKey = currentPlayingKey(),
        )
    }

    private fun currentPlayingKey(): String? = (_uiState.value as? LetterLessonUiState.BatchLesson)?.playingKey

    fun playLetterAudio(letter: LetterEntity) {
        play(key = "letter-${letter.id}", text = letter.name)
    }

    fun playWordAudio(word: BatchWordDto) {
        play(key = "word-${word.armenian}", text = word.armenian)
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
        if (state is LetterLessonUiState.BatchLesson) {
            _uiState.value = state.copy(playingKey = key)
        }
    }

    override fun onCleared() {
        audioPlayer.stop()
        super.onCleared()
    }
}
