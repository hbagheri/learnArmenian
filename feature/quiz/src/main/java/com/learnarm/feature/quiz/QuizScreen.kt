package com.learnarm.feature.quiz

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.learnarm.core.database.entity.LetterEntity
import com.learnarm.core.designsystem.theme.LearnArmTheme

@Composable
fun QuizScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: QuizViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    QuizContent(
        state = state,
        onAnswer = viewModel::onAnswerSelected,
        onRestart = viewModel::startNewRound,
        onBack = onBack,
        modifier = modifier,
    )
}

@Composable
private fun QuizContent(
    state: QuizUiState,
    onAnswer: (Int) -> Unit,
    onRestart: () -> Unit,
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
            text = "آزمون الفبا",
            style = MaterialTheme.typography.titleLarge,
        )

        when (state) {
            QuizUiState.Loading -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            QuizUiState.NotEnoughLetters -> Text(
                text = "حروف کافی برای ساختن آزمون موجود نیست.",
                style = MaterialTheme.typography.bodyLarge,
            )

            is QuizUiState.Playing -> PlayingBody(state, onAnswer)

            is QuizUiState.Finished -> FinishedBody(
                state = state,
                onRestart = onRestart,
                onBack = onBack,
            )
        }
    }
}

@Composable
private fun PlayingBody(
    state: QuizUiState.Playing,
    onAnswer: (Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = "سؤال ${state.questionNumber} از ${state.totalQuestions}",
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = "امتیاز: ${state.score}",
            style = MaterialTheme.typography.bodyMedium,
        )
    }

    LinearProgressIndicator(
        progress = { state.questionNumber.toFloat() / state.totalQuestions },
        modifier = Modifier.fillMaxWidth(),
    )

    Text(
        text = "این چه حرفیه؟",
        style = MaterialTheme.typography.bodyLarge,
    )

    GlyphCard(state.question.letter)

    Spacer(modifier = Modifier.height(8.dp))

    state.question.options.forEachIndexed { idx, option ->
        OptionButton(
            label = option,
            state = optionState(idx, state),
            onClick = { onAnswer(idx) },
        )
    }

    val selected = state.selected
    if (selected != null) {
        val msg = if (selected.isCorrect) {
            "درست بود!"
        } else {
            "اشتباه — جواب درست: ${state.question.options[state.question.correctIndex]}"
        }
        Text(
            text = msg,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = if (selected.isCorrect) CorrectGreen else WrongRed,
        )
    }
}

private enum class OptionVisualState { Idle, SelectedCorrect, SelectedWrong, RevealedCorrect, Disabled }

private fun optionState(index: Int, state: QuizUiState.Playing): OptionVisualState {
    val selected = state.selected ?: return OptionVisualState.Idle
    return when {
        index == selected.index && selected.isCorrect -> OptionVisualState.SelectedCorrect
        index == selected.index && !selected.isCorrect -> OptionVisualState.SelectedWrong
        index == state.question.correctIndex && !selected.isCorrect -> OptionVisualState.RevealedCorrect
        else -> OptionVisualState.Disabled
    }
}

@Composable
private fun OptionButton(
    label: String,
    state: OptionVisualState,
    onClick: () -> Unit,
) {
    val containerColor = when (state) {
        OptionVisualState.Idle -> MaterialTheme.colorScheme.surfaceVariant
        OptionVisualState.SelectedCorrect, OptionVisualState.RevealedCorrect -> CorrectGreen
        OptionVisualState.SelectedWrong -> WrongRed
        OptionVisualState.Disabled -> MaterialTheme.colorScheme.surface
    }
    val contentColor = when (state) {
        OptionVisualState.SelectedCorrect,
        OptionVisualState.RevealedCorrect,
        OptionVisualState.SelectedWrong -> Color.White
        else -> MaterialTheme.colorScheme.onSurface
    }
    val clickable = state == OptionVisualState.Idle
    Card(
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .then(if (clickable) Modifier.clickable(onClick = onClick) else Modifier),
    ) {
        Text(
            text = label,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 12.dp),
            textAlign = TextAlign.Center,
            fontSize = 22.sp,
        )
    }
}

@Composable
private fun GlyphCard(letter: LetterEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "${letter.upper} ${letter.lower}",
                fontSize = 72.sp,
                textAlign = TextAlign.Center,
            )
            Text(
                text = letter.nameLatin,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Composable
private fun FinishedBody(
    state: QuizUiState.Finished,
    onRestart: () -> Unit,
    onBack: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "تمام!",
                style = MaterialTheme.typography.displaySmall,
            )
            Text(
                text = "امتیاز شما: ${state.score} از ${state.totalQuestions}",
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = feedbackMessage(state.score, state.totalQuestions),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onRestart, modifier = Modifier.fillMaxWidth()) {
                Text("دوباره")
            }
            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text("بازگشت به الفبا")
            }
        }
    }
}

private fun feedbackMessage(score: Int, total: Int): String {
    if (total == 0) return ""
    val ratio = score.toFloat() / total
    return when {
        ratio >= 0.9f -> "عالی بود! 🌟"
        ratio >= 0.7f -> "خوبه — یه بار دیگه بهتر می‌شی"
        ratio >= 0.4f -> "نزدیک شدی — تمرین رو ادامه بده"
        else -> "اشکالی نداره — دوباره امتحان کن"
    }
}

private val CorrectGreen = Color(0xFF2E7D32)
private val WrongRed = Color(0xFFC62828)

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun QuizPlayingPreview() {
    LearnArmTheme {
        val letter = LetterEntity(
            id = 1,
            orderIndex = 1,
            upper = "Ա",
            lower = "ա",
            name = "այբ",
            nameLatin = "ayb",
            pronunciationFa = "آ",
            ipa = "/a/",
            exampleArmenian = "արև",
            exampleLatin = "arev",
            exampleFa = "خورشید",
        )
        QuizContent(
            state = QuizUiState.Playing(
                question = QuizQuestion(
                    letter = letter,
                    options = listOf("آ", "ب", "گ", "د"),
                    correctIndex = 0,
                ),
                questionNumber = 3,
                totalQuestions = 10,
                score = 2,
            ),
            onAnswer = {},
            onRestart = {},
            onBack = {},
            modifier = Modifier.padding(PaddingValues(0.dp)),
        )
    }
}
