package com.learnarm.feature.lessons

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.learnarm.core.database.entity.LessonEntity
import com.learnarm.core.database.entity.LessonStepEntity
import com.learnarm.core.data.repository.LessonRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LessonsListViewModel @Inject constructor(
    private val lessonRepository: LessonRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<LessonsListUiState>(LessonsListUiState.Loading)
    val uiState: StateFlow<LessonsListUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            lessonRepository.observeLessons().collect { lessons ->
                _uiState.value = if (lessons.isEmpty()) {
                    LessonsListUiState.Empty
                } else {
                    LessonsListUiState.Content(lessons)
                }
            }
        }
    }
}

sealed class LessonsListUiState {
    object Loading : LessonsListUiState()
    object Empty : LessonsListUiState()
    data class Content(val lessons: List<LessonEntity>) : LessonsListUiState()
}

@HiltViewModel
class LessonRunnerViewModel @Inject constructor(
    private val lessonRepository: LessonRepository,
) : ViewModel() {

    private val _lessonId = MutableStateFlow(1)
    private val _uiState = MutableStateFlow<LessonRunnerUiState>(LessonRunnerUiState.Loading)
    val uiState: StateFlow<LessonRunnerUiState> = _uiState.asStateFlow()

    private val _currentStepIndex = MutableStateFlow(0)
    val currentStepIndex: StateFlow<Int> = _currentStepIndex.asStateFlow()

    private val _completedStepIds = MutableStateFlow<Set<String>>(emptySet())
    val completedStepIds: StateFlow<Set<String>> = _completedStepIds.asStateFlow()

    fun setLessonId(id: Int) {
        _lessonId.value = id
        loadLesson(id)
    }

    private fun loadLesson(lessonId: Int) {
        viewModelScope.launch {
            val lesson = lessonRepository.getLesson(lessonId)
            if (lesson != null) {
                val steps = lessonRepository.getSteps(lessonId)
                _uiState.value = LessonRunnerUiState.Content(lesson, steps)
            } else {
                _uiState.value = LessonRunnerUiState.Error("درس یافت نشد")
            }
        }
    }

    fun markStepCompleted(stepId: String) {
        _completedStepIds.value = _completedStepIds.value + stepId
    }

    fun moveToNextStep() {
        val current = _currentStepIndex.value
        _currentStepIndex.value = current + 1
    }

    fun isLessonComplete(totalSteps: Int): Boolean {
        return _completedStepIds.value.size >= totalSteps
    }
}

sealed class LessonRunnerUiState {
    object Loading : LessonRunnerUiState()
    data class Content(val lesson: LessonEntity, val steps: List<LessonStepEntity>) : LessonRunnerUiState()
    data class Error(val message: String) : LessonRunnerUiState()
}
