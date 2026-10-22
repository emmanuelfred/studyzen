package com.fredrickemmanuel.studyzen.core.designsystem.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fredrickemmanuel.studyzen.core.designsystem.theme.StudyZenShape
import org.jetbrains.compose.resources.painterResource
import studyzen.shared.generated.resources.Res
import studyzen.shared.generated.resources.mascot_head

/** Friendly "nothing here yet" panel with the mascot and an optional call to action. */
@Composable
fun EmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    showMascot: Boolean = true,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (showMascot) {
            Image(
                painter = painterResource(Res.drawable.mascot_head),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(width = 96.dp, height = 106.dp),
            )
            Spacer(Modifier.height(12.dp))
        }
        Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(20.dp))
            StudyZenButton(text = actionLabel, onClick = onAction, modifier = Modifier.widthIn(max = 280.dp))
        }
    }
}

/** Something went wrong; offers a retry. */
@Composable
fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = "Something went wrong",
) {
    EmptyState(
        title = title,
        message = message,
        modifier = modifier,
        actionLabel = "Try again",
        onAction = onRetry,
        showMascot = false,
    )
}

enum class BannerTone { Info, Warning, Error }

/** Inline notice (offline, permission needed, sample content...). */
@Composable
fun InfoBanner(
    text: String,
    modifier: Modifier = Modifier,
    tone: BannerTone = BannerTone.Info,
) {
    val colors = MaterialTheme.colorScheme
    val (container, content) = when (tone) {
        BannerTone.Info -> colors.primaryContainer to colors.onPrimaryContainer
        BannerTone.Warning -> Color(0xFFFFF1CF) to Color(0xFF6B4A00)
        BannerTone.Error -> colors.errorContainer to colors.onErrorContainer
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(StudyZenShape.IconTile)
            .background(container)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, style = MaterialTheme.typography.bodySmall, color = content)
    }
}

/** A pulsing placeholder block shown while content loads. */
@Composable
fun ShimmerBlock(modifier: Modifier = Modifier, height: androidx.compose.ui.unit.Dp = 16.dp) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "shimmer-alpha",
    )
    Spacer(
        modifier = modifier
            .height(height)
            .clip(StudyZenShape.IconTile)
            .alpha(alpha)
            .background(MaterialTheme.colorScheme.outlineVariant),
    )
}
