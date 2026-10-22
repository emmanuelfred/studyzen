package com.fredrickemmanuel.studyzen.core.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fredrickemmanuel.studyzen.core.designsystem.theme.StudyZenShape
import com.fredrickemmanuel.studyzen.core.designsystem.theme.StudyZenSizes

/**
 * "Continue with Google": white pill with a thin border and the multicolor G, following
 * Google's button style (light theme).
 */
@Composable
fun GoogleButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String = "Continue with Google",
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    val colors = MaterialTheme.colorScheme
    OutlinedButton(
        onClick = { if (!loading) onClick() },
        modifier = modifier
            .fillMaxWidth()
            .height(StudyZenSizes.ButtonHeight),
        enabled = enabled,
        shape = StudyZenShape.Pill,
        border = BorderStroke(1.dp, colors.outline),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = colors.surface,
            contentColor = colors.onSurface,
            disabledContainerColor = colors.surface,
            disabledContentColor = colors.onSurface.copy(alpha = 0.5f),
        ),
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = colors.primary,
                strokeWidth = 2.5.dp,
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                GoogleLogo(size = 22.dp)
                Text(text = text, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

/**
 * Simplified multicolor "G" drawn in code so no image is needed.
 * Before publishing the app, replace it with Google's official asset from their branding
 * guidelines (they require the official logo in released apps).
 */
@Composable
fun GoogleLogo(modifier: Modifier = Modifier, size: Dp = 24.dp) {
    Canvas(modifier.size(size)) {
        val u = this.size.width / 24f
        val stroke = Stroke(width = 4.2f * u)
        val topLeft = Offset(4f * u, 4f * u)
        val arcSize = Size(16f * u, 16f * u)

        val red = Color(0xFFEA4335)
        val blue = Color(0xFF4285F4)
        val green = Color(0xFF34A853)
        val yellow = Color(0xFFFBBC05)

        // Compose angles: 0 = 3 o'clock, clockwise. The gap at 320..360 is the G's opening.
        drawArc(red, 220f, 100f, false, topLeft, arcSize, style = stroke)
        drawArc(yellow, 140f, 80f, false, topLeft, arcSize, style = stroke)
        drawArc(green, 50f, 90f, false, topLeft, arcSize, style = stroke)
        drawArc(blue, 0f, 50f, false, topLeft, arcSize, style = stroke)
        // Horizontal bar of the G
        drawRect(
            color = blue,
            topLeft = Offset(12f * u, 9.9f * u),
            size = Size(10f * u, 4.2f * u),
        )
    }
}

/** A thin line with "or" in the middle, used between the main button and social buttons. */
@Composable
fun OrDivider(modifier: Modifier = Modifier, text: String = "or") {
    val line = MaterialTheme.colorScheme.outline
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.weight(1f).height(1.dp).background(line))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Box(Modifier.weight(1f).height(1.dp).background(line))
    }
}
