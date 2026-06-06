package com.learnarm.feature.lessons.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.learnarm.feature.lessons.LessonsListScreen
import com.learnarm.feature.lessons.LessonRunnerScreen
import com.learnarm.feature.lessons.LessonsSharedViewModel
import kotlinx.serialization.Serializable

@Serializable
object LessonsRoute

@Serializable
object LessonRunnerRoute

fun NavGraphBuilder.lessonsScreen(
    sharedViewModel: LessonsSharedViewModel,
    onLessonSelected: (Int) -> Unit,
    onBack: () -> Unit,
) {
    composable<LessonsRoute> {
        LessonsListScreen(
            onLessonSelected = onLessonSelected,
            onBack = onBack,
        )
    }
}

fun NavGraphBuilder.lessonRunnerScreen(
    sharedViewModel: LessonsSharedViewModel,
    onStepCompleted: () -> Unit,
    onBack: () -> Unit,
) {
    composable<LessonRunnerRoute> {
        LessonRunnerScreen(
            sharedViewModel = sharedViewModel,
            onStepCompleted = onStepCompleted,
            onBack = onBack,
        )
    }
}

fun NavController.navigateToLessons() {
    navigate(LessonsRoute)
}

fun NavController.navigateToLessonRunner() {
    navigate(LessonRunnerRoute)
}
