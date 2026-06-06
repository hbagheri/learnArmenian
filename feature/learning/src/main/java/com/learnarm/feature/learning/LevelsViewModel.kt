package com.learnarm.feature.learning

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.learnarm.core.data.api.CurriculumService
import com.learnarm.core.data.api.PhaseDto
import com.learnarm.core.data.api.LevelDto
import javax.inject.Inject

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
            try {
                val levelsDto = curriculumService.getLevels()
                val levels = levelsDto.map { Level.fromDto(it) }
                _levelsState.value = LevelsUiState.Ready(levels)
            } catch (e: Exception) {
                // Fallback to empty list on error
                _levelsState.value = LevelsUiState.Ready(emptyList())
            }
        }
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
