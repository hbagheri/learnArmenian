package com.learnarm.feature.learning

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.learnarm.core.data.api.CurriculumService
import com.learnarm.core.data.progress.ProgressStore
import com.learnarm.feature.learning.navigation.LevelDetailRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LevelDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val curriculumService: CurriculumService,
    progressStore: ProgressStore,
) : ViewModel() {
    val levelId: Int = savedStateHandle.toRoute<LevelDetailRoute>().levelId

    private val _state = MutableStateFlow<LevelDetailUiState>(LevelDetailUiState.Loading)
    val state: StateFlow<LevelDetailUiState> = _state

    val bestScore: StateFlow<Int> = scoreFor(levelId, progressStore)
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            val level = try {
                Level.fromDto(curriculumService.getLevel(levelId))
            } catch (e: Exception) {
                fallbackLevel(levelId)
            }
            _state.value = if (level != null) {
                LevelDetailUiState.Ready(level)
            } else {
                LevelDetailUiState.Loading
            }
        }
    }
}

private fun scoreFor(levelId: Int, store: ProgressStore): Flow<Int> = when (levelId) {
    1 -> store.lettersScore
    2 -> store.vocabularyScore
    3 -> store.sentencesScore
    else -> flowOf(0)
}

private fun fallbackLevel(id: Int): Level? = MOCK_LEVELS.find { it.id == id }

private val MOCK_LEVELS = listOf(
    Level(
        id = 1,
        title = "الفبا",
        titleFa = "Letters - الفبای ارمنی",
        description = "یادگیری ۳۹ حرف ارمنی",
        isUnlocked = true,
        phases = emptyList(),
    ),
    Level(
        id = 2,
        title = "واژگان",
        titleFa = "Vocabulary - کلمات",
        description = "یادگیری واژه‌های پرکاربرد",
        isUnlocked = false,
        phases = emptyList(),
    ),
    Level(
        id = 3,
        title = "جملات",
        titleFa = "Sentences - جملات",
        description = "یادگیری جملات روزمره",
        isUnlocked = false,
        phases = emptyList(),
    ),
    Level(
        id = 4,
        title = "رسانه‌ها",
        titleFa = "Media - فیلم و خبر",
        description = "فیلم‌ها و اخبار به ارمنی",
        isUnlocked = false,
        phases = emptyList(),
    ),
)
