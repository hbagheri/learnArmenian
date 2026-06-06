package com.learnarm.feature.practice

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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.learnarm.core.audio.ScoreResult
import com.learnarm.core.database.entity.PhraseEntity
import com.learnarm.core.designsystem.theme.LearnArmTheme

@Composable
fun PracticeScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PracticeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
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

    PracticeContent(
        state = state,
        hasMicPermission = hasMicPermission,
        onRequestMic = {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        },
        onStartRecording = viewModel::startRecording,
        onStopAndScore = viewModel::stopAndScore,
        onNext = viewModel::pickNext,
        onBack = onBack,
        modifier = modifier,
    )
}

@Composable
private fun PracticeContent(
    state: PracticeUiState,
    hasMicPermission: Boolean,
    onRequestMic: () -> Unit,
    onStartRecording: () -> Unit,
    onStopAndScore: () -> Unit,
    onNext: () -> Unit,
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
            text = "تمرین تلفظ",
            style = MaterialTheme.typography.titleLarge,
        )
        when (state) {
            PracticeUiState.Loading -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            PracticeUiState.NoPhrases -> Text(
                text = "هنوز عبارتی برای تمرین موجود نیست.",
                style = MaterialTheme.typography.bodyLarge,
            )

            is PracticeUiState.Ready -> ReadyBody(
                state = state,
                hasMicPermission = hasMicPermission,
                onRequestMic = onRequestMic,
                onStartRecording = onStartRecording,
                onStopAndScore = onStopAndScore,
                onNext = onNext,
                onBack = onBack,
            )
        }
    }
}

@Composable
private fun ReadyBody(
    state: PracticeUiState.Ready,
    hasMicPermission: Boolean,
    onRequestMic: () -> Unit,
    onStartRecording: () -> Unit,
    onStopAndScore: () -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
) {
    Text(
        text = "این عبارت را با صدای بلند تکرار کنید:",
        style = MaterialTheme.typography.bodyLarge,
    )
    TargetCard(state.target)

    state.result?.let { ResultCard(it, state.target.armenian) }
    state.error?.let {
        Text(
            text = errorMessage(it),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
        )
    }

    Spacer(modifier = Modifier.height(8.dp))

    when (state.phase) {
        PracticePhase.Idle -> {
            if (!hasMicPermission) {
                Button(onClick = onRequestMic, modifier = Modifier.fillMaxWidth()) {
                    Text("اجازه‌ی دسترسی به میکروفون")
                }
            } else {
                MicButton(
                    label = "ضبط",
                    color = MaterialTheme.colorScheme.primary,
                    onClick = onStartRecording,
                )
            }
        }
        PracticePhase.Recording -> {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Text(
                text = "در حال ضبط… وقتی تمام شد، توقف بزنید.",
                style = MaterialTheme.typography.bodyMedium,
            )
            MicButton(
                label = "توقف و امتیاز",
                color = RecordingRed,
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
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }

    if (state.phase == PracticePhase.Idle) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) {
                Text("بازگشت")
            }
            Button(onClick = onNext, modifier = Modifier.weight(1f)) {
                Text("عبارت بعدی")
            }
        }
    }
}

@Composable
private fun TargetCard(phrase: PhraseEntity) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = phrase.armenian,
                fontSize = 36.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            Text(
                text = phrase.transliteration,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = phrase.persian,
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

@Composable
private fun ResultCard(success: ScoreResult.Success, target: String) {
    val percent = (success.score * 100).toInt().coerceIn(0, 100)
    val color = when {
        success.score >= 0.8f -> CorrectGreen
        success.score >= 0.5f -> WarnAmber
        else -> WrongRed
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = color, contentColor = Color.White),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = "امتیاز: %$percent",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "بازخورد: ${success.feedback}",
                style = MaterialTheme.typography.bodyLarge,
            )
            if (success.recognized.isNotBlank() && success.recognized != target) {
                Text(
                    text = "شنیده شد: ${success.recognized}",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun MicButton(
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

private fun errorMessage(reason: ScoreResult.Reason): String = when (reason) {
    ScoreResult.Reason.Network -> "خطای شبکه — اتصال را بررسی کنید"
    ScoreResult.Reason.Server -> "سرور پاسخ‌گو نیست"
    ScoreResult.Reason.EmptyAudio -> "صدایی ضبط نشد — دوباره امتحان کنید"
    ScoreResult.Reason.Parse -> "پاسخ سرور قابل پردازش نبود"
}

private val CorrectGreen = Color(0xFF2E7D32)
private val WarnAmber = Color(0xFFEF6C00)
private val WrongRed = Color(0xFFC62828)
private val RecordingRed = Color(0xFFD32F2F)

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun PracticeReadyPreview() {
    LearnArmTheme {
        val phrase = PhraseEntity(
            id = 1, orderIndex = 1, category = "greeting",
            armenian = "Բարև", transliteration = "barev",
            persian = "سلام", note = "محاوره‌ای",
        )
        PracticeContent(
            state = PracticeUiState.Ready(
                target = phrase,
                phase = PracticePhase.Idle,
                result = ScoreResult.Success(score = 0.82f, recognized = "barev", feedback = "خوب"),
            ),
            hasMicPermission = true,
            onRequestMic = {},
            onStartRecording = {},
            onStopAndScore = {},
            onNext = {},
            onBack = {},
        )
    }
}
