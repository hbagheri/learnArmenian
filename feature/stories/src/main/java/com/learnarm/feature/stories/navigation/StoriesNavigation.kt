package com.learnarm.feature.stories.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.learnarm.feature.stories.StoriesListScreen
import com.learnarm.feature.stories.StoryReaderScreen
import kotlinx.serialization.Serializable

@Serializable
data object StoriesRoute

@Serializable
data object StoryReaderRoute

fun NavGraphBuilder.storiesScreen(
    onBack: () -> Unit,
    onStorySelected: () -> Unit,
) {
    composable<StoriesRoute> {
        StoriesListScreen(
            onBack = onBack,
            onStorySelected = onStorySelected,
        )
    }
    composable<StoryReaderRoute> {
        StoryReaderScreen(onBack = onBack)
    }
}

fun NavController.navigateToStories() {
    navigate(StoriesRoute)
}

fun NavController.navigateToStoryReader() {
    navigate(StoryReaderRoute)
}
