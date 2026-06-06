package com.learnarm.feature.reviews

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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
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
import com.learnarm.core.data.srs.ReviewItem
import com.learnarm.core.data.srs.ReviewQuality
import com.learnarm.core.designsystem.theme.LearnArmTheme
import com.learnarm.core.database.entity.LetterEntity

@Composable
fun ReviewsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReviewsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ReviewsContent(
        state = state,
        onReveal = viewModel::reveal,
        onRate = viewModel::rate,
        onBack = onBack,
        modifier = modifier,
    )
}

@Composable
private fun ReviewsContent(
    state: ReviewsUiState,
    onReveal: () -> Unit,
    onRate: (ReviewQuality) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(text = "مرور", style = MaterialTheme.typography.titleLarge)

        when (state) {
            ReviewsUiState.Loading -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            ReviewsUiState.AllDone -> AllDoneBody(onBack)

            is ReviewsUiState.Reviewing -> ReviewingBody(state, onReveal, onRate)
        }
    }
}

@Composable
private fun AllDoneBody(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(40.dp))
        Text(
            text = "همه‌ی مرورها انجام شد!",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "بعداً برگرد — کارت‌های بعدی روی برنامه‌ی فاصله‌دار میان.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("بازگشت")
        }
    }
}

@Composable
private fun ReviewingBody(
    state: ReviewsUiState.Reviewing,
    onReveal: () -> Unit,
    onRate: (ReviewQuality) -> Unit,
) {
    Text(
        text = if (state.item is ReviewItem.Letter) "این حرف چه صدایی داره؟" else "این عبارت یعنی چی؟",
        style = MaterialTheme.typography.bodyLarge,
    )
    FlipCard(item = state.item, revealed = state.revealed, onClick = onReveal)
    Spacer(modifier = Modifier.height(8.dp))
    if (!state.revealed) {
        OutlinedButton(onClick = onReveal, modifier = Modifier.fillMaxWidth()) {
            Text("نمایش جواب")
        }
    } else {
        RatingButtons(onRate)
    }
}

@Composable
private fun FlipCard(item: ReviewItem, revealed: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = item.frontPrimary,
                fontSize = when (item) {
                    is ReviewItem.Letter -> 72.sp
                    is ReviewItem.Phrase -> 36.sp
                },
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            if (revealed) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.backTransliteration,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = item.backPersian,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun RatingButtons(onRate: (ReviewQuality) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        RatingButton(label = "دوباره", color = AgainRed, modifier = Modifier.weight(1f)) {
            onRate(ReviewQuality.Again)
        }
        RatingButton(label = "سخت", color = HardOrange, modifier = Modifier.weight(1f)) {
            onRate(ReviewQuality.Hard)
        }
        RatingButton(label = "خوب", color = GoodGreen, modifier = Modifier.weight(1f)) {
            onRate(ReviewQuality.Good)
        }
        RatingButton(label = "آسون", color = EasyTeal, modifier = Modifier.weight(1f)) {
            onRate(ReviewQuality.Easy)
        }
    }
}

@Composable
private fun RatingButton(
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = Color.White),
        modifier = modifier,
    ) {
        Text(label)
    }
}

private val AgainRed = Color(0xFFC62828)
private val HardOrange = Color(0xFFEF6C00)
private val GoodGreen = Color(0xFF2E7D32)
private val EasyTeal = Color(0xFF00838F)

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun ReviewsPreview() {
    LearnArmTheme {
        val letter = LetterEntity(1, 1, "Ա", "ա", "այբ", "ayb", "آ", "/a/", "արև", "arev", "خورشید")
        ReviewsContent(
            state = ReviewsUiState.Reviewing(
                item = ReviewItem.Letter(letter),
                revealed = true,
            ),
            onReveal = {},
            onRate = {},
            onBack = {},
        )
    }
}
