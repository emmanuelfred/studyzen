package com.fredrickemmanuel.studyzen.core.designsystem.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/*
 * Small line icons drawn with Canvas on a 24 x 24 grid, so the project needs no icon library.
 * All icons share the same stroke style and take a [tint].
 */

private fun lineStyle(u: Float, width: Float = 2f) =
    Stroke(width = width * u, cap = StrokeCap.Round, join = StrokeJoin.Round)

@Composable
fun BackIcon(
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    tint: Color = LocalContentColor.current,
) {
    Canvas(modifier.size(size)) {
        val u = this.size.width / 24f
        val path = Path().apply {
            moveTo(14.5f * u, 5f * u)
            lineTo(7.5f * u, 12f * u)
            lineTo(14.5f * u, 19f * u)
        }
        drawPath(path, tint, style = lineStyle(u, 2.4f))
    }
}

@Composable
fun CheckIcon(
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    tint: Color = LocalContentColor.current,
) {
    Canvas(modifier.size(size)) {
        val u = this.size.width / 24f
        val path = Path().apply {
            moveTo(5f * u, 12.5f * u)
            lineTo(10f * u, 17.5f * u)
            lineTo(19f * u, 7f * u)
        }
        drawPath(path, tint, style = lineStyle(u, 2.6f))
    }
}

/** Eye icon; set [crossed] to draw the "hide" variant with a slash. */
@Composable
fun EyeIcon(
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    tint: Color = LocalContentColor.current,
    crossed: Boolean = false,
) {
    Canvas(modifier.size(size)) {
        val u = this.size.width / 24f
        val style = lineStyle(u)
        val eye = Path().apply {
            moveTo(2.5f * u, 12f * u)
            cubicTo(5f * u, 7.5f * u, 8.5f * u, 6f * u, 12f * u, 6f * u)
            cubicTo(15.5f * u, 6f * u, 19f * u, 7.5f * u, 21.5f * u, 12f * u)
            cubicTo(19f * u, 16.5f * u, 15.5f * u, 18f * u, 12f * u, 18f * u)
            cubicTo(8.5f * u, 18f * u, 5f * u, 16.5f * u, 2.5f * u, 12f * u)
            close()
        }
        drawPath(eye, tint, style = style)
        drawCircle(tint, radius = 3f * u, center = Offset(12f * u, 12f * u), style = style)
        if (crossed) {
            drawLine(
                color = tint,
                start = Offset(4f * u, 4f * u),
                end = Offset(20f * u, 20f * u),
                strokeWidth = 2f * u,
                cap = StrokeCap.Round,
            )
        }
    }
}

@Composable
fun MailIcon(
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    tint: Color = LocalContentColor.current,
) {
    Canvas(modifier.size(size)) {
        val u = this.size.width / 24f
        val style = lineStyle(u)
        drawRoundRect(
            color = tint,
            topLeft = Offset(3f * u, 5.5f * u),
            size = Size(18f * u, 13f * u),
            cornerRadius = CornerRadius(3f * u, 3f * u),
            style = style,
        )
        val flap = Path().apply {
            moveTo(4.5f * u, 8f * u)
            lineTo(12f * u, 13.5f * u)
            lineTo(19.5f * u, 8f * u)
        }
        drawPath(flap, tint, style = style)
    }
}

@Composable
fun LockIcon(
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    tint: Color = LocalContentColor.current,
) {
    Canvas(modifier.size(size)) {
        val u = this.size.width / 24f
        val style = lineStyle(u)
        drawRoundRect(
            color = tint,
            topLeft = Offset(5f * u, 11f * u),
            size = Size(14f * u, 9.5f * u),
            cornerRadius = CornerRadius(3f * u, 3f * u),
            style = style,
        )
        // Shackle: upper half circle plus two short legs
        drawArc(
            color = tint,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(8f * u, 4f * u),
            size = Size(8f * u, 8f * u),
            style = style,
        )
        drawLine(tint, Offset(8f * u, 8f * u), Offset(8f * u, 11f * u), strokeWidth = 2f * u, cap = StrokeCap.Round)
        drawLine(tint, Offset(16f * u, 8f * u), Offset(16f * u, 11f * u), strokeWidth = 2f * u, cap = StrokeCap.Round)
    }
}

/** Double upward chevron used by the onboarding swipe-up handle. Drawn in the given scope. */
internal fun DrawScope.drawDoubleChevronUp(center: Offset, width: Float, tint: Color) {
    val half = width / 2f
    val rise = width * 0.5f
    val gap = width * 0.55f
    val stroke = Stroke(width = width * 0.18f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    for (i in 0..1) {
        val y = center.y + i * gap
        val path = Path().apply {
            moveTo(center.x - half, y + rise / 2f)
            lineTo(center.x, y - rise / 2f)
            lineTo(center.x + half, y + rise / 2f)
        }
        drawPath(path, tint, style = stroke)
    }
}
