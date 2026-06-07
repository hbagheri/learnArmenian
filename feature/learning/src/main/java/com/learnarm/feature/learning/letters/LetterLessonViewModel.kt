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
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface LetterLessonUiState {
    data object Loading : LetterLessonUiState

    /** A learning lesson for batch [batchIndex] (currentBatch + 1, or any passed batch when [isReplay]). */
    data class BatchLesson(
        val batchIndex: Int,
        val totalBatches: Int,
        val passedCount: Int,
        val batch: LetterBatchDto,
        val batchLetters: List<LetterEntity>,
        val playingKey: String? = null,
        val isReplay: Boolean = false,
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

/** Status of a batch chip in the lesson screen rail. */
enum class BatchChipStatus { Passed, Current, Locked, Replaying }

data class BatchChip(val index: Int, val status: BatchChipStatus)

@HiltViewModel
class LetterLessonViewModel @Inject constructor(
    private val curriculumService: CurriculumService,
    private val letterRepository: LetterRepository,
    private val progressStore: ProgressStore,
    private val audioPlayer: LetterAudioPlayer,
) : ViewModel() {

    private val _uiState = MutableStateFlow<LetterLessonUiState>(LetterLessonUiState.Loading)
    val uiState: StateFlow<LetterLessonUiState> = _uiState.asStateFlow()

    private val _chips = MutableStateFlow<List<BatchChip>>(emptyList())
    val chips: StateFlow<List<BatchChip>> = _chips.asStateFlow()

    private val _replayBatchIndex = MutableStateFlow<Int?>(null)

    private var curriculum: LetterCurriculumDto = FALLBACK_LETTER_CURRICULUM
    private var playJob: Job? = null

    init {
        viewModelScope.launch {
            curriculum = try {
                curriculumService.getLetterCurriculum()
            } catch (e: Exception) {
                FALLBACK_LETTER_CURRICULUM
            }
            // Combine progress + letters + replay selection so the lesson screen reflects
            // the latest state. (Letters table is seeded once and stable.)
            combine(
                progressStore.lettersCurrentBatch,
                progressStore.lettersReviewsPassed,
                letterRepository.observeLetters(),
                _replayBatchIndex,
            ) { currentBatch, reviewsPassed, letters, replayIndex ->
                val effectiveReplay = replayIndex?.takeIf { it in 1..currentBatch }
                _chips.value = buildChips(currentBatch, effectiveReplay)
                computeState(currentBatch, reviewsPassed, letters, effectiveReplay)
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    private fun buildChips(currentBatch: Int, replayIndex: Int?): List<BatchChip> {
        val total = curriculum.batches.size
        return (1..total).map { i ->
            val status = when {
                replayIndex == i -> BatchChipStatus.Replaying
                i <= currentBatch -> BatchChipStatus.Passed
                i == currentBatch + 1 -> BatchChipStatus.Current
                else -> BatchChipStatus.Locked
            }
            BatchChip(index = i, status = status)
        }
    }

    private fun computeState(
        currentBatch: Int,
        reviewsPassed: Int,
        letters: List<LetterEntity>,
        replayIndex: Int?,
    ): LetterLessonUiState {
        val total = curriculum.batches.size

        // Replay mode overrides everything — show the selected passed batch as a lesson.
        if (replayIndex != null) {
            val batch = curriculum.batches.firstOrNull { it.index == replayIndex }
            if (batch != null) {
                val byId = letters.associateBy { it.id }
                return LetterLessonUiState.BatchLesson(
                    batchIndex = replayIndex,
                    totalBatches = total,
                    passedCount = currentBatch,
                    batch = batch,
                    batchLetters = batch.letterIds.mapNotNull { byId[it] },
                    playingKey = currentPlayingKey(),
                    isReplay = true,
                )
            }
        }

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

    fun startReplay(batchIndex: Int) {
        audioPlayer.stop()
        playJob?.cancel()
        _replayBatchIndex.value = batchIndex
    }

    fun exitReplay() {
        audioPlayer.stop()
        playJob?.cancel()
        _replayBatchIndex.value = null
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
