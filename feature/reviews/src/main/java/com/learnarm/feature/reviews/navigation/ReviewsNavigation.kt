package com.learnarm.feature.reviews.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.learnarm.feature.reviews.ReviewsScreen
import kotlinx.serialization.Serializable

@Serializable
data object ReviewsRoute

fun NavGraphBuilder.reviewsScreen(onBack: () -> Unit) {
    composable<ReviewsRoute> {
        ReviewsScreen(onBack = onBack)
    }
}

fun NavController.navigateToReviews() {
    navigate(ReviewsRoute)
}
