package com.learnarm.feature.learning

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

data class Phase(
    val id: Int,
    val title: String,
    val description: String,
    val type: String, // "learning" or "exam"
    val isCompleted: Boolean = false,
    val score: Int? = null, // null if not completed, 0-100 if completed
)

data class Level(
    val id: Int,
    val title: String,
    val titleFa: String,
    val description: String,
    val phases: List<Phase>,
    val isUnlocked: Boolean,
)

sealed class LevelsUiState {
    object Loading : LevelsUiState()
    data class Ready(val levels: List<Level>) : LevelsUiState()
}

sealed class LevelDetailUiState {
    object Loading : LevelDetailUiState()
    data class Ready(val level: Level) : LevelDetailUiState()
}

@HiltViewModel
class LevelsViewModel @Inject constructor() : ViewModel() {
    private val _levelsState = MutableStateFlow<LevelsUiState>(LevelsUiState.Loading)
    val levelsState: StateFlow<LevelsUiState> = _levelsState

    private val _levelDetailState = MutableStateFlow<LevelDetailUiState>(LevelDetailUiState.Loading)
    val levelDetailState: StateFlow<LevelDetailUiState> = _levelDetailState

    init {
        loadLevels()
    }

    private fun loadLevels() {
        val levels = listOf(
            Level(
                id = 1,
                title = "الفبا",
                titleFa = "Letters - الفبای ارمنی",
                description = "یادگیری ۳۹ حرف ارمنی",
                isUnlocked = true,
                phases = listOf(
                    Phase(
                        id = 1,
                        title = "شناسایی حروف",
                        description = "تشخیص حروف و صدای آن‌ها - نیاز: ۱۰۰%",
                        type = "learning",
                    ),
                    Phase(
                        id = 2,
                        title = "تکرار حروف",
                        description = "تمرین تلفظ درست حروف - نیاز: ۸۲%",
                        type = "learning",
                    ),
                    Phase(
                        id = 3,
                        title = "حروف در واژه‌ها",
                        description = "حرف در شروع، وسط و انتهای واژه - نیاز: ۱۰۰%",
                        type = "learning",
                    ),
                    Phase(
                        id = 4,
                        title = "امتحان حروف",
                        description = "۴ مرحله: تلفظ، تطابق، شنیدن، واژه",
                        type = "exam",
                    ),
                ),
            ),
            Level(
                id = 2,
                title = "واژگان",
                titleFa = "Vocabulary - کلمات",
                description = "یادگیری ۲۰-۳۰ واژه از هر سطح",
                isUnlocked = false,
                phases = listOf(
                    Phase(
                        id = 5,
                        title = "معرفی واژه‌ها",
                        description = "واژه‌های جدید و تلفظ آن‌ها",
                        type = "learning",
                    ),
                    Phase(
                        id = 6,
                        title = "تمرین تلفظ واژه",
                        description = "تکرار واژه‌ها - نیاز: ۹۰%",
                        type = "learning",
                    ),
                    Phase(
                        id = 7,
                        title = "امتحان واژگان",
                        description = "خوندن واژه بدون کمک صوتی - نیاز: ۹۰%",
                        type = "exam",
                    ),
                ),
            ),
            Level(
                id = 3,
                title = "جملات",
                titleFa = "Sentences - جملات",
                description = "یادگیری جملاتی و ساختار آن‌ها",
                isUnlocked = false,
                phases = listOf(
                    Phase(
                        id = 8,
                        title = "مفاهیم جملاتی",
                        description = "فعل، فاعل، مفعول، قید",
                        type = "learning",
                    ),
                    Phase(
                        id = 9,
                        title = "خوندن و تلفظ جملات",
                        description = "تمرین خوندن جملات",
                        type = "learning",
                    ),
                    Phase(
                        id = 10,
                        title = "امتحان جملات",
                        description = "ترجمه فارسی → ارمنی",
                        type = "exam",
                    ),
                ),
            ),
            Level(
                id = 4,
                title = "رسانه‌ها",
                titleFa = "Media - فیلم و خبر",
                description = "فیلم‌ها و اخبار به ارمنی",
                isUnlocked = false,
                phases = listOf(
                    Phase(
                        id = 11,
                        title = "تماشای فیلم",
                        description = "تماشای فیلم و نوشتن چیزی که شنیدی",
                        type = "learning",
                    ),
                    Phase(
                        id = 12,
                        title = "خبر و ترجمه",
                        description = "خواندن خبر و ترجمه به فارسی",
                        type = "learning",
                    ),
                ),
            ),
        )

        _levelsState.value = LevelsUiState.Ready(levels)
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
