package com.learnarm.feature.game

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.learnarm.core.data.api.CurriculumService
import com.learnarm.core.data.api.WordPairDto
import com.learnarm.core.data.progress.ProgressStore
import com.learnarm.core.data.repository.LetterRepository
import com.learnarm.feature.game.navigation.GameRoute
import com.learnarm.feature.game.navigation.GameTypes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
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
        val gameType: String,
        val selectedArmenian: Int? = null,
    ) : GameUiState()
    data class GameOver(
        val score: Int,
        val totalWords: Int,
        val percentage: Int,
        val gameType: String,
        val isUnlocked: Boolean,
    ) : GameUiState()
}

private val LETTER_PAIRS = listOf(
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
)

private val VOCABULARY_PAIRS = listOf(
    WordPair(101, "Բարև", "سلام", "barev"),
    WordPair(102, "Շնորհակալություն", "ممنون", "shnorhakalut'yun"),
    WordPair(103, "Ջուր", "آب", "jur"),
    WordPair(104, "Հաց", "نان", "hats"),
    WordPair(105, "Տուն", "خانه", "tun"),
    WordPair(106, "Օր", "روز", "or"),
    WordPair(107, "Գիշեր", "شب", "gisher"),
    WordPair(108, "Մայր", "مادر", "mayr"),
    WordPair(109, "Հայր", "پدر", "hayr"),
    WordPair(110, "Ընկեր", "دوست", "ənker"),
)

private val SENTENCE_PAIRS = listOf(
    WordPair(201, "Որտե՞ղ է", "کجاست", "vortegh e"),
    WordPair(202, "Որքա՞ն", "چقدر", "vorqan"),
    WordPair(203, "Բարի օր", "روز بخیر", "bari or"),
    WordPair(204, "Չեմ հասկանում", "نمی‌فهمم", "chem haskanum"),
    WordPair(205, "Կարո՞ղ եմ", "می‌توانم؟", "karogh em"),
    WordPair(206, "Շատ լավ", "خیلی خوب", "shat lav"),
    WordPair(207, "Ներողություն", "ببخشید", "neroghutyun"),
    WordPair(208, "Անունդ ի՞նչ է", "اسمت چیست", "anund inch e"),
    WordPair(209, "Ինչպե՞ս ես", "چطوری", "inchpes es"),
    WordPair(210, "Բարի գիշեր", "شب بخیر", "bari gisher"),
)

private const val LETTERS_PASS_THRESHOLD = 100
private const val VOCABULARY_PASS_THRESHOLD = 90
private const val SENTENCES_PASS_THRESHOLD = 80
private const val ROUND_SIZE = 10

@HiltViewModel
class GameViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val progressStore: ProgressStore,
    private val curriculumService: CurriculumService,
    private val letterRepository: LetterRepository,
) : ViewModel() {
    val gameType: String = savedStateHandle.toRoute<GameRoute>().gameType

    private val _gameState = MutableStateFlow<GameUiState>(GameUiState.Loading)
    val gameState: StateFlow<GameUiState> = _gameState

    init {
        loadGame()
    }

    private fun loadGame() {
        viewModelScope.launch {
            val source = when (gameType) {
                GameTypes.VOCABULARY -> fetchOrFallback({ curriculumService.getVocabulary() }, VOCABULARY_PAIRS)
                    .shuffled().take(ROUND_SIZE)
                GameTypes.SENTENCES -> fetchOrFallback({ curriculumService.getSentences() }, SENTENCE_PAIRS)
                    .shuffled().take(ROUND_SIZE)
                else -> fetchLettersOrFallback().shuffled()
            }
            val persianShuffled = source.map { it.persian }.shuffled()

            _gameState.value = GameUiState.Playing(
                wordPairs = source,
                persianOptions = persianShuffled,
                matches = emptyList(),
                score = 0,
                totalWords = source.size,
                gameType = gameType,
            )
        }
    }

    private suspend fun fetchLettersOrFallback(): List<WordPair> = try {
        val letters = letterRepository.observeLetters().first()
        if (letters.isEmpty()) LETTER_PAIRS
        else letters.map { letter ->
            WordPair(
                id = letter.id,
                armenian = "${letter.upper} ${letter.lower}",
                persian = "${letter.pronunciationFa} (${letter.nameLatin})",
                transliteration = letter.nameLatin,
            )
        }
    } catch (e: Exception) {
        LETTER_PAIRS
    }

    private suspend fun fetchOrFallback(
        fetcher: suspend () -> List<WordPairDto>,
        fallback: List<WordPair>,
    ): List<WordPair> = try {
        fetcher().map { WordPair(it.id, it.armenian, it.persian, it.transliteration) }
            .ifEmpty { fallback }
    } catch (e: Exception) {
        fallback
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
                val percentage = (newScore * 100) / state.totalWords
                val threshold = when (gameType) {
                    GameTypes.VOCABULARY -> VOCABULARY_PASS_THRESHOLD
                    GameTypes.SENTENCES -> SENTENCES_PASS_THRESHOLD
                    else -> LETTERS_PASS_THRESHOLD
                }
                viewModelScope.launch {
                    when (gameType) {
                        GameTypes.VOCABULARY -> progressStore.recordVocabularyScore(percentage)
                        GameTypes.SENTENCES -> progressStore.recordSentencesScore(percentage)
                        else -> progressStore.recordLettersScore(percentage)
                    }
                }
                _gameState.value = GameUiState.GameOver(
                    score = newScore,
                    totalWords = state.totalWords,
                    percentage = percentage,
                    gameType = gameType,
                    isUnlocked = percentage >= threshold,
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
