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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.learnarm.core.database.entity.LetterEntity
import com.learnarm.core.database.entity.PhraseEntity
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
    val stepLetters by viewModel.stepLetters.collectAsStateWithLifecycle()
    val stepPhrases by viewModel.stepPhrases.collectAsStateWithLifecycle()

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
                    stepLetter = stepLetters[currentStep.itemKey],
                    stepPhrase = stepPhrases[currentStep.itemKey],
                    onStepCompleted = {
                        viewModel.markStepCompleted(currentStep.itemKey)
                        val nextIndex = currentStepIndex + 1
                        if (nextIndex >= steps.size) {
                            // Lesson complete - navigate back
                            onStepCompleted()
                        } else {
                            // Move to next step (don't call parent callback - avoids recomposition reset)
                            viewModel.moveToNextStep()
                        }
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
    stepLetter: LetterEntity?,
    stepPhrase: PhraseEntity?,
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
                "SHOW_LETTER" -> {
                    if (stepLetter != null) {
                        LessonLetterCard(letter = stepLetter)
                    } else {
                        Text(text = "حرف بارگذاری نشد", fontSize = 18.sp)
                    }
                }
                "SHOW_PHRASE" -> {
                    if (stepPhrase != null) {
                        LessonPhraseCard(phrase = stepPhrase)
                    } else {
                        Text(text = "عبارت بارگذاری نشد", fontSize = 18.sp)
                    }
                }
                "QUIZ_LETTER" -> {
                    Text(text = "آزمون حرف: ${currentStep.promptFa ?: ""}", fontSize = 18.sp)
                }
                "QUIZ_PHRASE" -> {
                    Text(text = "آزمون عبارت: ${currentStep.promptFa ?: ""}", fontSize = 18.sp)
                }
                "PRACTICE_PHRASE" -> {
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
private fun LessonLetterCard(
    letter: LetterEntity,
    modifier: Modifier = Modifier,
) {
    Card(
        colors = CardDefaults.cardColors(),
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "${letter.upper} ${letter.lower}",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Text(
                text = letter.nameLatin,
                fontSize = 20.sp,
                style = MaterialTheme.typography.labelLarge,
            )
            Text(
                text = letter.name,
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
            )
            Text(
                text = letter.pronunciationFa,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LessonPhraseCard(
    phrase: PhraseEntity,
    modifier: Modifier = Modifier,
) {
    Card(
        colors = CardDefaults.cardColors(),
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = phrase.armenian,
                fontSize = 28.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = phrase.transliteration,
                fontSize = 16.sp,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = phrase.persian,
                fontSize = 20.sp,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth(),
            )
            phrase.note?.takeIf { it.isNotBlank() }?.let { noteText ->
                Text(
                    text = noteText,
                    fontSize = 14.sp,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
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
