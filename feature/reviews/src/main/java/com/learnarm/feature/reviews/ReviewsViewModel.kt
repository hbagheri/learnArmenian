package com.learnarm.feature.reviews

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.learnarm.core.data.repository.ReviewRepository
import com.learnarm.core.data.srs.ReviewItem
import com.learnarm.core.data.srs.ReviewQuality
import com.learnarm.core.data.streak.StreakStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ReviewsUiState {
    data object Loading : ReviewsUiState
    data object AllDone : ReviewsUiState
    data class Reviewing(
        val item: ReviewItem,
        val revealed: Boolean,
    ) : ReviewsUiState
}

@HiltViewModel
class ReviewsViewModel @Inject constructor(
    private val reviewRepository: ReviewRepository,
    private val streakStore: StreakStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ReviewsUiState>(ReviewsUiState.Loading)
    val uiState: StateFlow<ReviewsUiState> = _uiState.asStateFlow()

    init {
        loadNext()
    }

    fun reveal() {
        val current = _uiState.value as? ReviewsUiState.Reviewing ?: return
        if (current.revealed) return
        _uiState.value = current.copy(revealed = true)
    }

    fun rate(quality: ReviewQuality) {
        val current = _uiState.value as? ReviewsUiState.Reviewing ?: return
        viewModelScope.launch {
            reviewRepository.recordReview(key = current.item.key, quality = quality)
            streakStore.recordStudiedToday()
            loadNext()
        }
    }

    private fun loadNext() {
        viewModelScope.launch {
            _uiState.value = ReviewsUiState.Loading
            val next = reviewRepository.nextDueItem()
            _uiState.value = if (next == null) {
                ReviewsUiState.AllDone
            } else {
                ReviewsUiState.Reviewing(item = next, revealed = false)
            }
        }
    }
}
