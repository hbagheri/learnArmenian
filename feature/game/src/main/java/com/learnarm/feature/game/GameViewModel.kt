package com.learnarm.feature.game

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

data class WordPair(
    val id: Int,
    val armenian: String,
    val persian: String,
    val transliteration: String? = null,
)

data class GameMatch(
    val armPosition: Int,
    val persianPosition: Int,
    val wordPair: WordPair,
    val isCorrect: Boolean,
)

sealed class GameUiState {
    object Loading : GameUiState()
    data class Playing(
        val wordPairs: List<WordPair>,
        val persianOptions: List<String>,
        val matches: List<GameMatch>,
        val score: Int,
        val totalWords: Int,
        val selectedArmenian: Int? = null,
    ) : GameUiState()
    data class GameOver(
        val score: Int,
        val totalWords: Int,
        val percentage: Int,
    ) : GameUiState()
}

@HiltViewModel
class GameViewModel @Inject constructor() : ViewModel() {
    private val _gameState = MutableStateFlow<GameUiState>(GameUiState.Loading)
    val gameState: StateFlow<GameUiState> = _gameState

    init {
        loadGame()
    }

    private fun loadGame() {
        val wordPairs = listOf(
            WordPair(1, "Ա", "آ", "a"),
            WordPair(2, "Բ", "ب", "b"),
            WordPair(3, "Գ", "گ", "g"),
            WordPair(4, "Դ", "د", "d"),
            WordPair(5, "Ե", "ه", "e"),
            WordPair(6, "Զ", "ز", "z"),
            WordPair(7, "Է", "ع", "ē"),
            WordPair(8, "Ը", "ه", "ə"),
            WordPair(9, "Թ", "ث", "th"),
            WordPair(10, "Ժ", "ج", "zh"),
        ).shuffled()

        val persianShuffled = wordPairs.map { it.persian }.shuffled()

        _gameState.value = GameUiState.Playing(
            wordPairs = wordPairs,
            persianOptions = persianShuffled,
            matches = emptyList(),
            score = 0,
            totalWords = wordPairs.size,
        )
    }

    fun selectArmenian(position: Int) {
        val state = _gameState.value
        if (state is GameUiState.Playing) {
            _gameState.value = state.copy(selectedArmenian = position)
        }
    }

    fun matchWord(armPosition: Int, persianPosition: Int) {
        val state = _gameState.value
        if (state is GameUiState.Playing) {
            val armenianWord = state.wordPairs[armPosition]
            val selectedPersian = state.persianOptions[persianPosition]

            val isCorrect = armenianWord.persian == selectedPersian

            val newMatch = GameMatch(
                armPosition = armPosition,
                persianPosition = persianPosition,
                wordPair = armenianWord,
                isCorrect = isCorrect,
            )

            val newMatches = state.matches + newMatch
            val newScore = if (isCorrect) state.score + 1 else state.score

            if (newMatches.size == state.totalWords) {
                _gameState.value = GameUiState.GameOver(
                    score = newScore,
                    totalWords = state.totalWords,
                    percentage = (newScore * 100) / state.totalWords,
                )
            } else {
                _gameState.value = state.copy(
                    matches = newMatches,
                    score = newScore,
                    selectedArmenian = null,
                )
            }
        }
    }

    fun restartGame() {
        loadGame()
    }
}
