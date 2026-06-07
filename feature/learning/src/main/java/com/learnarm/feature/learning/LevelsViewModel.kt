package com.learnarm.feature.learning

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.learnarm.core.data.api.CurriculumService
import com.learnarm.core.data.api.PhaseDto
import com.learnarm.core.data.api.LevelDto
import com.learnarm.core.data.progress.ProgressStore
import javax.inject.Inject

private const val LETTERS_UNLOCK_THRESHOLD = 100
private const val VOCAB_UNLOCK_THRESHOLD = 90
private const val SENTENCES_UNLOCK_THRESHOLD = 80

data class Phase(
    val id: Int,
    val title: String,
    val description: String,
    val type: String, // "learning" or "exam"
    val isCompleted: Boolean = false,
    val score: Int? = null,
) {
    companion object {
        fun fromDto(dto: PhaseDto) = Phase(
            id = dto.id,
            title = dto.title,
            description = dto.description,
            type = dto.type,
            isCompleted = dto.isCompleted,
            score = dto.score,
        )
    }
}

data class Level(
    val id: Int,
    val title: String,
    val titleFa: String,
    val description: String,
    val phases: List<Phase>,
    val isUnlocked: Boolean,
) {
    companion object {
        fun fromDto(dto: LevelDto) = Level(
            id = dto.id,
            title = dto.title,
            titleFa = dto.titleFa,
            description = dto.description,
            phases = dto.phases.map { Phase.fromDto(it) },
            isUnlocked = dto.isUnlocked,
        )
    }
}

sealed class LevelsUiState {
    object Loading : LevelsUiState()
    data class Ready(val levels: List<Level>) : LevelsUiState()
}

sealed class LevelDetailUiState {
    object Loading : LevelDetailUiState()
    data class Ready(val level: Level) : LevelDetailUiState()
}

@HiltViewModel
class LevelsViewModel @Inject constructor(
    private val curriculumService: CurriculumService,
    private val progressStore: ProgressStore,
) : ViewModel() {
    private val _levelsState = MutableStateFlow<LevelsUiState>(LevelsUiState.Loading)
    val levelsState: StateFlow<LevelsUiState> = _levelsState

    private val _levelDetailState = MutableStateFlow<LevelDetailUiState>(LevelDetailUiState.Loading)
    val levelDetailState: StateFlow<LevelDetailUiState> = _levelDetailState

    init {
        loadLevels()
    }

    private fun loadLevels() {
        viewModelScope.launch {
            val rawLevels = try {
                curriculumService.getLevels().map { Level.fromDto(it) }
            } catch (e: Exception) {
                getMockLevels()
            }
            _levelsState.value = LevelsUiState.Ready(applyUnlockState(rawLevels))
        }
    }

    private suspend fun applyUnlockState(levels: List<Level>): List<Level> {
        val lettersScore = progressStore.lettersScore.first()
        val vocabScore = progressStore.vocabularyScore.first()
        val sentencesScore = progressStore.sentencesScore.first()
        return levels.map { level ->
            val unlocked = when (level.id) {
                1 -> true
                2 -> lettersScore >= LETTERS_UNLOCK_THRESHOLD
                3 -> vocabScore >= VOCAB_UNLOCK_THRESHOLD
                4 -> sentencesScore >= SENTENCES_UNLOCK_THRESHOLD
                else -> false
            }
            level.copy(isUnlocked = unlocked)
        }
    }

    private fun getMockLevels(): List<Level> {
        return listOf(
            Level(
                id = 1,
                title = "الفبا",
                titleFa = "Letters - الفبای ارمنی",
                description = "یادگیری ۳۹ حرف ارمنی",
                isUnlocked = true,
                phases = listOf(
                    Phase(1, "شناسایی حروف", "تشخیص حروف و صدای آن‌ها - نیاز: ۱۰۰%", "learning"),
                    Phase(2, "تکرار حروف", "تمرین تلفظ درست حروف - نیاز: ۸۲%", "learning"),
                    Phase(3, "حروف در واژه‌ها", "حرف در شروع، وسط و انتهای واژه - نیاز: ۱۰۰%", "learning"),
                    Phase(4, "امتحان حروف", "۴ مرحله: تلفظ، تطابق، شنیدن، واژه", "exam"),
                ),
            ),
            Level(
                id = 2,
                title = "واژگان",
                titleFa = "Vocabulary - کلمات",
                description = "یادگیری ۲۰-۳۰ واژه از هر سطح",
                isUnlocked = false,
                phases = listOf(
                    Phase(5, "معرفی واژه‌ها", "واژه‌های جدید و تلفظ آن‌ها", "learning"),
                    Phase(6, "تمرین تلفظ واژه", "تکرار واژه‌ها - نیاز: ۹۰%", "learning"),
                    Phase(7, "امتحان واژگان", "خوندن واژه بدون کمک صوتی - نیاز: ۹۰%", "exam"),
                ),
            ),
            Level(
                id = 3,
                title = "جملات",
                titleFa = "Sentences - جملات",
                description = "یادگیری جملاتی و ساختار آن‌ها",
                isUnlocked = false,
                phases = listOf(
                    Phase(8, "مفاهیم جملاتی", "فعل، فاعل، مفعول، قید", "learning"),
                    Phase(9, "خوندن و تلفظ جملات", "تمرین خوندن جملات", "learning"),
                    Phase(10, "امتحان جملات", "ترجمه فارسی → ارمنی", "exam"),
                ),
            ),
            Level(
                id = 4,
                title = "رسانه‌ها",
                titleFa = "Media - فیلم و خبر",
                description = "فیلم‌ها و اخبار به ارمنی",
                isUnlocked = false,
                phases = listOf(
                    Phase(11, "تماشای فیلم", "تماشای فیلم و نوشتن چیزی که شنیدی", "learning"),
                    Phase(12, "خبر و ترجمه", "خواندن خبر و ترجمه به فارسی", "learning"),
                ),
            ),
        )
    }


    fun selectLevel(levelId: Int) {
        val state = _levelsState.value
        if (state is LevelsUiState.Ready) {
            val level = state.levels.find { it.id == levelId }
            if (level != null) {
                _levelDetailState.value = LevelDetailUiState.Ready(level)
            }
        }
    }

    fun resetLevelDetail() {
        _levelDetailState.value = LevelDetailUiState.Loading
    }
}
