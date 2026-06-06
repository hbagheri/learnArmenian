package com.learnarm.feature.lessons

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.learnarm.core.database.entity.LessonEntity
import com.learnarm.core.database.entity.LessonStepEntity
import com.learnarm.core.designsystem.theme.LearnArmTheme

@Composable
fun LessonRunnerScreen(
    sharedViewModel: LessonsSharedViewModel,
    onStepCompleted: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LessonRunnerViewModel = hiltViewModel(),
) {
    val lessonId by sharedViewModel.selectedLessonId.collectAsStateWithLifecycle()

    LaunchedEffect(lessonId) {
        viewModel.setLessonId(lessonId)
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentStepIndex by viewModel.currentStepIndex.collectAsStateWithLifecycle()
    val completedStepIds by viewModel.completedStepIds.collectAsStateWithLifecycle()

    when (val state = uiState) {
        LessonRunnerUiState.Loading -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is LessonRunnerUiState.Error -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = state.message)
            }
        }
        is LessonRunnerUiState.Content -> {
            val lesson = state.lesson
            val steps = state.steps
            if (currentStepIndex >= steps.size) {
                LessonCompletedScreen(
                    lesson = lesson,
                    onBack = onBack,
                    modifier = modifier,
                )
            } else {
                val currentStep = steps[currentStepIndex]
                LessonRunnerContent(
                    lesson = lesson,
                    currentStep = currentStep,
                    stepIndex = currentStepIndex,
                    totalSteps = steps.size,
                    onStepCompleted = {
                        viewModel.markStepCompleted(currentStep.itemKey)
                        viewModel.moveToNextStep()
                        onStepCompleted()
                    },
                    onBack = onBack,
                    modifier = modifier,
                )
            }
        }
    }
}

@Composable
private fun LessonRunnerContent(
    lesson: LessonEntity,
    currentStep: LessonStepEntity,
    stepIndex: Int,
    totalSteps: Int,
    onStepCompleted: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = lesson.titleFa,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
        )
        LinearProgressIndicator(
            progress = (stepIndex + 1).toFloat() / totalSteps,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = "مرحله ${stepIndex + 1} از $totalSteps",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            when (currentStep.type) {
                "letter" -> {
                    Text(text = "حرف: ${currentStep.itemKey}", fontSize = 24.sp)
                }
                "phrase" -> {
                    Text(text = "عبارت: ${currentStep.itemKey}", fontSize = 18.sp)
                }
                "quiz" -> {
                    Text(text = "آزمون: ${currentStep.promptFa ?: ""}", fontSize = 18.sp)
                }
                "practice" -> {
                    Text(text = "تمرین: ${currentStep.promptFa ?: ""}", fontSize = 18.sp)
                }
                else -> {
                    Text(text = "نوع نامعلوم: ${currentStep.type}", fontSize = 16.sp)
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onStepCompleted,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("تأیید و ادامه")
            }
            Button(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("بازگشت")
            }
        }
    }
}

@Composable
private fun LessonCompletedScreen(
    lesson: LessonEntity,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "تبریک! 🎉",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "درس \"${lesson.titleFa}\" تکمیل شد",
            fontSize = 18.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(16.dp),
        )
        Button(onClick = onBack) {
            Text("بازگشت به درس‌ها")
        }
    }
}

@Composable
fun LessonRunnerScreenPreview() {
    LearnArmTheme {
        Box(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
            // Preview would need a LessonsSharedViewModel - skip for now
            Text("LessonRunner Preview")
        }
    }
}
