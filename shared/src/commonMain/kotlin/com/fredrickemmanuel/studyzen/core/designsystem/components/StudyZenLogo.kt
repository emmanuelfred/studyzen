package com.fredrickemmanuel.studyzen.core.designsystem.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.ui.layout.ContentScale
import org.jetbrains.compose.resources.painterResource
import studyzen.shared.generated.resources.Res
import studyzen.shared.generated.resources.mascot_head
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fredrickemmanuel.studyzen.core.designsystem.theme.StudyZenPalette

/**
 * StudyZen logo mark: a purple globe wearing a blue graduation cap, with a soft lavender ring.
 * Drawn as vectors on a 100 x 100 grid so it stays sharp at any size (splash, onboarding,
 * headers). Recreated from the supplied reference; swap in the final brand vector when ready.
 */
@Composable
fun StudyZenLogoMark(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
) {
    Canvas(
        modifier = modifier
            .size(size)
            .semantics { contentDescription = "StudyZen logo" },
    ) {
        val u = this.size.width / 100f
        fun p(x: Float, y: Float) = Offset(x * u, y * u)

        val globeCenter = p(50f, 60f)
        val globeRadius = 29f * u
        val capBlue = Color(0xFF6C8CF6)

        // Lavender ring around the lower-left of the globe
        drawArc(
            color = StudyZenPalette.Lavender,
            startAngle = 40f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = p(14f, 24f),
            size = Size(72f * u, 72f * u),
            style = Stroke(width = 2.4f * u, cap = StrokeCap.Round),
        )

        // Globe
        drawCircle(
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF9B86F2), Color(0xFF6A4FD8)),
                start = p(32f, 31f),
                end = p(68f, 89f),
            ),
            radius = globeRadius,
            center = globeCenter,
        )

        // Meridians and parallels, clipped to the globe
        val globeClip = Path().apply { addOval(Rect(center = globeCenter, radius = globeRadius)) }
        clipPath(globeClip) {
            val line = Color.White.copy(alpha = 0.55f)
            val stroke = Stroke(width = 2.2f * u)
            drawOval(line, topLeft = p(39f, 31f), size = Size(22f * u, 58f * u), style = stroke)
            drawOval(line, topLeft = p(29f, 31f), size = Size(42f * u, 58f * u), style = stroke)
            drawLine(line, p(50f, 31f), p(50f, 89f), strokeWidth = 2.2f * u)
            drawLine(line, p(21f, 60f), p(79f, 60f), strokeWidth = 2.2f * u)
            drawLine(line, p(25f, 73f), p(75f, 73f), strokeWidth = 2.2f * u)
        }

        // Cap base (the part that sits on the "head")
        val capBase = Path().apply {
            moveTo(31f * u, 36f * u)
            lineTo(31f * u, 46f * u)
            cubicTo(43.67f * u, 52.67f * u, 56.33f * u, 52.67f * u, 69f * u, 46f * u)
            lineTo(69f * u, 36f * u)
            close()
        }
        drawPath(capBase, Color(0xFF4A3FC4))

        // Cap board
        val capBoard = Path().apply {
            moveTo(50f * u, 14f * u)
            lineTo(84f * u, 29f * u)
            lineTo(50f * u, 44f * u)
            lineTo(16f * u, 29f * u)
            close()
        }
        drawPath(
            capBoard,
            Brush.linearGradient(
                colors = listOf(capBlue, Color(0xFF5B45D0)),
                start = p(16f, 14f),
                end = p(84f, 44f),
            ),
        )

        // Soft highlight on the upper-right of the board
        val highlight = Path().apply {
            moveTo(50f * u, 14f * u)
            lineTo(84f * u, 29f * u)
            lineTo(50f * u, 31f * u)
            close()
        }
        drawPath(highlight, Color.White.copy(alpha = 0.16f))

        // Tassel
        drawLine(capBlue, p(84f, 29f), p(84f, 46f), strokeWidth = 2f * u, cap = StrokeCap.Round)
        drawCircle(capBlue, radius = 2.6f * u, center = p(84f, 48f))
    }
}

private const val MascotAspect = 640f / 712f

/** Mascot logo + "StudyZen" wordmark, as used at the top of onboarding and auth screens. */
@Composable
fun StudyZenLogo(
    modifier: Modifier = Modifier,
    markSize: Dp = 40.dp,
    textStyle: TextStyle = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
) {
    Row(
        modifier = modifier.clearAndSetSemantics { contentDescription = "StudyZen" },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // The mascot head is the app logo everywhere (auth, onboarding, Home, empty states).
        Image(
            painter = painterResource(Res.drawable.mascot_head),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.height(markSize).aspectRatio(MascotAspect),
        )
        Text(
            text = "StudyZen",
            style = textStyle,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}
