package com.learnarm.feature.learning.vocab

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.learnarm.core.audio.AudioRequest
import com.learnarm.core.audio.LetterAudioPlayer
import com.learnarm.core.audio.PlayResult
import com.learnarm.core.data.api.CurriculumService
import com.learnarm.core.data.api.WordPairDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface VocabReviewUiState {
    data object Loading : VocabReviewUiState
    data class Ready(
        val words: List<WordPairDto>,
        val playingKey: String? = null,
        val lastError: PlayResult.Reason? = null,
    ) : VocabReviewUiState
}

@HiltViewModel
class VocabReviewViewModel @Inject constructor(
    private val curriculumService: CurriculumService,
    private val audioPlayer: LetterAudioPlayer,
) : ViewModel() {

    private val _uiState = MutableStateFlow<VocabReviewUiState>(VocabReviewUiState.Loading)
    val uiState: StateFlow<VocabReviewUiState> = _uiState.asStateFlow()

    private var currentJob: Job? = null

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            val words = try {
                curriculumService.getVocabulary()
            } catch (e: Exception) {
                MOCK_VOCAB
            }
            _uiState.value = VocabReviewUiState.Ready(words = words)
        }
    }

    fun playWord(word: WordPairDto) {
        val key = "word-${word.id}"
        currentJob?.cancel()
        updatePlayback(key, error = null)
        currentJob = viewModelScope.launch {
            val result = audioPlayer.play(AudioRequest(key = key, text = word.armenian))
            when (result) {
                PlayResult.Played -> updatePlayback(null, error = null)
                is PlayResult.Failed -> updatePlayback(null, error = result.reason)
            }
        }
    }

    private fun updatePlayback(playingKey: String?, error: PlayResult.Reason?) {
        val current = _uiState.value
        if (current is VocabReviewUiState.Ready) {
            _uiState.value = current.copy(playingKey = playingKey, lastError = error)
        }
    }

    override fun onCleared() {
        audioPlayer.stop()
        super.onCleared()
    }
}

private val MOCK_VOCAB = listOf(
    WordPairDto(1, "բարև", "سلام", "barev"),
    WordPairDto(2, "շնորհակալություն", "ممنون", "shnorhakalutyun"),
    WordPairDto(3, "այո", "بله", "ayo"),
    WordPairDto(4, "ոչ", "نه", "voch"),
    WordPairDto(5, "ջուր", "آب", "jur"),
    WordPairDto(6, "հաց", "نان", "hats"),
    WordPairDto(7, "տուն", "خانه", "tun"),
    WordPairDto(8, "մայր", "مادر", "mayr"),
    WordPairDto(9, "հայր", "پدر", "hayr"),
    WordPairDto(10, "ընկեր", "دوست", "ynker"),
)
