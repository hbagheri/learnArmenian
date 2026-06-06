package com.learnarm.feature.lessons

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class LessonsSharedViewModel @Inject constructor() : ViewModel() {
    private val _selectedLessonId = MutableStateFlow(1)
    val selectedLessonId: StateFlow<Int> = _selectedLessonId.asStateFlow()

    fun setSelectedLessonId(id: Int) {
        _selectedLessonId.value = id
    }
}
