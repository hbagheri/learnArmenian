package com.learnarm.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.learnarm.core.audio.AudioRequest
import com.learnarm.core.audio.LetterAudioPlayer
import com.learnarm.core.audio.PlayResult
import com.learnarm.core.data.repository.LetterRepository
import com.learnarm.core.data.repository.ReviewRepository
import com.learnarm.core.data.streak.StreakStore
import com.learnarm.core.database.entity.LetterEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    repository: LetterRepository,
    reviewRepository: ReviewRepository,
    streakStore: StreakStore,
    private val audioPlayer: LetterAudioPlayer,
) : ViewModel() {

    private val playback = MutableStateFlow(PlaybackState())
    private var currentJob: Job? = null

    val uiState: StateFlow<HomeUiState> =
        combine(
            repository.observeLetters(),
            playback,
            reviewRepository.observeDueCount(),
            streakStore.currentStreak,
        ) { letters, state, dueCount, streak ->
            HomeUiState.Ready(
                letters = letters,
                playback = state,
                dueCount = dueCount,
                streak = streak,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState.Loading,
        )

    fun onLetterTapped(letter: LetterEntity) {
        currentJob?.cancel()
        playback.value = PlaybackState(playingLetterId = letter.id, lastError = null)
        currentJob = viewModelScope.launch {
            val result = audioPlayer.play(
                AudioRequest(key = "letter-${letter.id}", text = letter.name),
            )
            playback.value = when (result) {
                PlayResult.Played -> PlaybackState()
                is PlayResult.Failed -> PlaybackState(lastError = result.reason)
            }
        }
    }

    override fun onCleared() {
        audioPlayer.stop()
        super.onCleared()
    }
}

data class PlaybackState(
    val playingLetterId: Int? = null,
    val lastError: PlayResult.Reason? = null,
)

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Ready(
        val letters: List<LetterEntity>,
        val playback: PlaybackState = PlaybackState(),
        val dueCount: Int = 0,
        val streak: Int = 0,
    ) : HomeUiState
}
