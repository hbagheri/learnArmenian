package com.learnarm.feature.phrases

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.learnarm.core.audio.AudioRequest
import com.learnarm.core.audio.LetterAudioPlayer
import com.learnarm.core.audio.PlayResult
import com.learnarm.core.data.repository.PhraseRepository
import com.learnarm.core.database.entity.PhraseEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class PhraseCategory(val key: String, val labelFa: String) {
    Greeting(key = "greeting", labelFa = "احوال‌پرسی"),
    Courtesy(key = "courtesy", labelFa = "تعارفات"),
    Question(key = "question", labelFa = "سؤال"),
    City(key = "city", labelFa = "شهر");

    companion object {
        val ordered: List<PhraseCategory> = listOf(Greeting, Courtesy, Question, City)
        fun fromKey(key: String): PhraseCategory? = ordered.firstOrNull { it.key == key }
    }
}

data class PhrasePlayback(
    val playingPhraseId: Int? = null,
    val lastError: PlayResult.Reason? = null,
)

sealed interface PhrasesUiState {
    data object Loading : PhrasesUiState
    data class Ready(
        val selected: PhraseCategory,
        val phrases: List<PhraseEntity>,
        val playback: PhrasePlayback = PhrasePlayback(),
    ) : PhrasesUiState
}

@HiltViewModel
class PhrasesViewModel @Inject constructor(
    repository: PhraseRepository,
    private val audioPlayer: LetterAudioPlayer,
) : ViewModel() {

    private val selectedCategory = MutableStateFlow(PhraseCategory.Greeting)
    private val playback = MutableStateFlow(PhrasePlayback())
    private var currentJob: Job? = null

    val uiState: StateFlow<PhrasesUiState> =
        combine(
            repository.observePhrases(),
            selectedCategory,
            playback,
        ) { all, selected, play ->
            PhrasesUiState.Ready(
                selected = selected,
                phrases = all.filter { it.category == selected.key },
                playback = play,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = PhrasesUiState.Loading,
        )

    fun onCategorySelected(category: PhraseCategory) {
        if (selectedCategory.value == category) return
        currentJob?.cancel()
        audioPlayer.stop()
        playback.value = PhrasePlayback()
        selectedCategory.value = category
    }

    fun onPhraseTapped(phrase: PhraseEntity) {
        currentJob?.cancel()
        playback.value = PhrasePlayback(playingPhraseId = phrase.id, lastError = null)
        currentJob = viewModelScope.launch {
            val result = audioPlayer.play(
                AudioRequest(key = "phrase-${phrase.id}", text = phrase.armenian),
            )
            playback.value = when (result) {
                PlayResult.Played -> PhrasePlayback()
                is PlayResult.Failed -> PhrasePlayback(lastError = result.reason)
            }
        }
    }

    override fun onCleared() {
        audioPlayer.stop()
        super.onCleared()
    }
}
