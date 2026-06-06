package com.learnarm.feature.lessons

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.learnarm.core.audio.ScoreResult
import com.learnarm.core.database.entity.LessonEntity
import com.learnarm.core.database.entity.LessonStepEntity
import com.learnarm.core.database.entity.LetterEntity
import com.learnarm.core.database.entity.PhraseEntity
import com.learnarm.core.designsystem.theme.LearnArmTheme
import com.learnarm.feature.lessons.PracticePhase

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

    val context = LocalContext.current
    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasMicPermission = granted
        if (granted) viewModel.startRecording()
    }

    val practicePhase by viewModel.practicePhase.collectAsStateWithLifecycle()
    val practiceResult by viewModel.practiceResult.collectAsStateWithLifecycle()
    val practiceError by viewModel.practiceError.collectAsStateWithLifecycle()

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
                    hasMicPermission = hasMicPermission,
                    onRequestMic = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                    practicePhase = practicePhase,
                    practiceResult = practiceResult,
                    practiceError = practiceError,
                    onPlayPhraseAudio = { text -> viewModel.playPhraseAudio(text) },
                    onStartRecording = viewModel::startRecording,
                    onStopAndScore = { viewModel.stopAndScore(stepPhrases[currentStep.itemKey]?.armenian ?: "") },
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
    hasMicPermission: Boolean,
    onRequestMic: () -> Unit,
    practicePhase: PracticePhase,
    practiceResult: ScoreResult.Success?,
    practiceError: ScoreResult.Reason?,
    onPlayPhraseAudio: (String) -> Unit,
    onStartRecording: () -> Unit,
    onStopAndScore: () -> Unit,
    onStepCompleted: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedAnswerIndex by remember { mutableIntStateOf(-1) }
    var answerResult by remember { mutableStateOf<Boolean?>(null) }
    var quizAnswerOptions by remember { mutableStateOf<List<String>>(emptyList()) }

    LaunchedEffect(currentStep.type) {
        if (currentStep.type == "QUIZ_LETTER" && stepLetter != null) {
            quizAnswerOptions = generateLetterQuizOptions(stepLetter)
        } else if (currentStep.type == "QUIZ_PHRASE" && stepPhrase != null) {
            quizAnswerOptions = generatePhraseQuizOptions(stepPhrase)
        }
        selectedAnswerIndex = -1
        answerResult = null
    }

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
                    if (stepLetter != null) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            LessonLetterCard(letter = stepLetter)
                            Text(
                                text = "کدام یک نام صحیح این حرف است؟",
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                quizAnswerOptions.forEachIndexed { index, option ->
                                    QuizAnswerButton(
                                        text = option,
                                        isSelected = selectedAnswerIndex == index,
                                        isCorrect = if (answerResult != null) index == 0 else null,
                                        enabled = answerResult == null,
                                        onClick = {
                                            selectedAnswerIndex = index
                                            answerResult = (index == 0)
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }
                            }
                            if (answerResult != null) {
                                Text(
                                    text = if (answerResult == true) "✓ درست!" else "✗ غلط، دوباره امتحان کنید",
                                    fontSize = 16.sp,
                                    color = if (answerResult == true) Color.Green else Color.Red,
                                )
                            }
                        }
                    } else {
                        Text(text = "حرف برای آزمون بارگذاری نشد", fontSize = 18.sp)
                    }
                }
                "QUIZ_PHRASE" -> {
                    if (stepPhrase != null) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            LessonPhraseCard(phrase = stepPhrase)
                            Text(
                                text = "معنی این عبارت چیست؟",
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                quizAnswerOptions.forEachIndexed { index, option ->
                                    QuizAnswerButton(
                                        text = option,
                                        isSelected = selectedAnswerIndex == index,
                                        isCorrect = if (answerResult != null) index == 0 else null,
                                        enabled = answerResult == null,
                                        onClick = {
                                            selectedAnswerIndex = index
                                            answerResult = (index == 0)
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }
                            }
                            if (answerResult != null) {
                                Text(
                                    text = if (answerResult == true) "✓ درست!" else "✗ غلط، دوباره امتحان کنید",
                                    fontSize = 16.sp,
                                    color = if (answerResult == true) Color.Green else Color.Red,
                                )
                            }
                        }
                    } else {
                        Text(text = "عبارت برای آزمون بارگذاری نشد", fontSize = 18.sp)
                    }
                }
                "PRACTICE_PHRASE" -> {
                    if (stepPhrase != null) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            Text(
                                text = "تمرین تلفظ",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            LessonPhraseCard(phrase = stepPhrase)

                            if (practicePhase == PracticePhase.Idle && practiceResult == null) {
                                OutlinedButton(
                                    onClick = { onPlayPhraseAudio(stepPhrase.armenian) },
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text("🔊 بشنو")
                                }
                                Text(
                                    text = "اول عبارت رو بشنو، بعد خودت تکرار کن.",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                )
                            }

                            practiceResult?.let { result ->
                                ScoreGaugeCard(result, stepPhrase.armenian)
                            }
                            practiceError?.let { error ->
                                Text(
                                    text = practiceLessonErrorMessage(error),
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 14.sp,
                                )
                            }

                            when (practicePhase) {
                                PracticePhase.Idle -> {
                                    if (practiceResult == null) {
                                        if (!hasMicPermission) {
                                            Button(onClick = onRequestMic,
                                                modifier = Modifier.fillMaxWidth()) {
                                                Text("اجازه‌ی دسترسی به میکروفون")
                                            }
                                        } else {
                                            PracticeMicButton(
                                                label = "🎤 ضبط کن",
                                                color = MaterialTheme.colorScheme.primary,
                                                onClick = onStartRecording,
                                            )
                                        }
                                    }
                                }
                                PracticePhase.Recording -> {
                                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                                    Text(
                                        text = "در حال ضبط… وقتی تمام شد، توقف بزنید.",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    PracticeMicButton(
                                        label = "⏹ توقف و امتیاز",
                                        color = Color(0xFFD32F2F),
                                        onClick = onStopAndScore,
                                    )
                                }
                                PracticePhase.Scoring -> {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                        Text(
                                            text = "در حال امتیازدهی…",
                                            fontSize = 12.sp,
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Text(text = "عبارت برای تمرین بارگذاری نشد", fontSize = 18.sp)
                    }
                }
                else -> {
                    Text(text = "نوع نامعلوم: ${currentStep.type}", fontSize = 16.sp)
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val isQuiz = currentStep.type in listOf("QUIZ_LETTER", "QUIZ_PHRASE")
            val isPractice = currentStep.type == "PRACTICE_PHRASE"
            val canContinue = when {
                isQuiz -> answerResult == true
                isPractice -> practiceResult != null && practicePhase == PracticePhase.Idle
                else -> true
            }

            if (!(isPractice && practicePhase != PracticePhase.Idle)) {
                Button(
                    onClick = onStepCompleted,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = canContinue,
                ) {
                    Text("تأیید و ادامه")
                }
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
private fun QuizAnswerButton(
    text: String,
    isSelected: Boolean,
    isCorrect: Boolean?,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val backgroundColor = when {
        isCorrect == true -> Color(0xFF4CAF50)
        isCorrect == false -> Color(0xFFF44336)
        isSelected && !enabled -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(containerColor = backgroundColor),
    ) {
        Text(text = text, color = Color.White)
    }
}

private fun generateLetterQuizOptions(correct: LetterEntity): List<String> {
    return listOf(correct.name) + listOf(
        "բար",
        "տե",
        "շա",
    ).shuffled().take(3)
}

private fun generatePhraseQuizOptions(correct: PhraseEntity): List<String> {
    return listOf(correct.persian) + listOf(
        "خوشحال شدم",
        "ممنون",
        "بله",
    ).shuffled().take(3)
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
private fun PracticeMicButton(
    label: String,
    color: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .background(color, shape = CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun ScoreGaugeCard(success: ScoreResult.Success, target: String) {
    val percent = (success.score * 100).toInt().coerceIn(0, 100)
    val scoreColor = when {
        success.score >= 0.8f -> Color(0xFF2E7D32)  // سبز
        success.score >= 0.5f -> Color(0xFFEF6C00)  // زرد
        else -> Color(0xFFC62828)  // قرمز
    }
    val feedbackLabel = when {
        success.score >= 0.8f -> "عالی! 👏"
        success.score >= 0.5f -> "خوب 👍"
        else -> "دوباره امتحان کن"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Circular progress gauge
            Box(
                modifier = Modifier.size(120.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    progress = { success.score.coerceIn(0f, 1f) },
                    modifier = Modifier.size(120.dp),
                    color = scoreColor,
                    strokeWidth = 8.dp,
                )
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "$percent%",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = scoreColor,
                    )
                    Text(
                        text = "تلفظ",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Text(
                text = feedbackLabel,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = scoreColor,
            )

            if (success.recognized.isNotBlank() && success.recognized != target) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = scoreColor.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp),
                        )
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "شنیده شد:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = success.recognized,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = scoreColor,
                    )
                }
            }
        }
    }
}

private fun practiceLessonErrorMessage(reason: ScoreResult.Reason): String = when (reason) {
    ScoreResult.Reason.Network -> "خطای شبکه — اتصال را بررسی کنید"
    ScoreResult.Reason.Server -> "سرور پاسخ‌گو نیست"
    ScoreResult.Reason.EmptyAudio -> "صدایی ضبط نشد — دوباره امتحان کنید"
    ScoreResult.Reason.Parse -> "پاسخ سرور قابل پردازش نبود"
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
