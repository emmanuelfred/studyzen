package com.fredrickemmanuel.studyzen.core.designsystem.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.ceil
import kotlin.math.max

/** Circular progress with the percentage in the middle (Today's Plan rows). */
@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    strokeWidth: Dp = 4.dp,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.outlineVariant,
    showLabel: Boolean = true,
) {
    val value = progress.coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .size(size)
            .semantics { contentDescription = "${(value * 100).toInt()} percent complete" },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val sw = strokeWidth.toPx()
            val topLeft = Offset(sw / 2f, sw / 2f)
            val arcSize = Size(this.size.width - sw, this.size.height - sw)
            drawArc(trackColor, 0f, 360f, false, topLeft, arcSize, style = Stroke(width = sw))
            if (value > 0f) {
                drawArc(color, -90f, 360f * value, false, topLeft, arcSize, style = Stroke(width = sw, cap = StrokeCap.Round))
            }
        }
        if (showLabel) {
            Text(
                text = "${(value * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

/** Thin rounded progress bar. */
@Composable
fun SoftProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.outlineVariant,
    height: Dp = 8.dp,
) {
    val value = progress.coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(CircleShape)
            .background(trackColor)
            .semantics { contentDescription = "${(value * 100).toInt()} percent" },
    ) {
        if (value > 0f) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(value)
                    .clip(CircleShape)
                    .background(color),
            )
        }
    }
}

data class DonutSegment(val value: Float, val color: Color)

/** Ring split into colored segments, with any content in the middle (Subject Breakdown). */
@Composable
fun DonutChart(
    segments: List<DonutSegment>,
    modifier: Modifier = Modifier,
    size: Dp = 180.dp,
    strokeWidth: Dp = 20.dp,
    summary: String = "",
    center: @Composable () -> Unit = {},
) {
    val track = MaterialTheme.colorScheme.outlineVariant
    Box(
        modifier = modifier
            .size(size)
            .semantics { if (summary.isNotEmpty()) contentDescription = summary },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val sw = strokeWidth.toPx()
            val topLeft = Offset(sw / 2f, sw / 2f)
            val arcSize = Size(this.size.width - sw, this.size.height - sw)
            val total = segments.sumOf { it.value.toDouble() }.toFloat()
            if (total <= 0f) {
                drawArc(track, 0f, 360f, false, topLeft, arcSize, style = Stroke(width = sw))
            } else {
                val gap = if (segments.size > 1) 3f else 0f
                var start = -90f
                segments.forEach { seg ->
                    val sweep = seg.value / total * 360f
                    drawArc(
                        color = seg.color,
                        startAngle = start + gap / 2f,
                        sweepAngle = (sweep - gap).coerceAtLeast(0.5f),
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = sw),
                    )
                    start += sweep
                }
            }
        }
        center()
    }
}

/**
 * Bars with a line over them, one per label (study time per day). Tap a bar to see its value.
 * [summary] is read aloud by screen readers, so the chart is not the only way to get the data.
 */
@Composable
fun StudyTimeChart(
    values: List<Float>,
    labels: List<String>,
    modifier: Modifier = Modifier,
    unitLabel: String = "Hours",
    summary: String = "",
) {
    val n = values.size
    if (n == 0) return
    var selected by remember(values) { mutableIntStateOf(values.indexOfLast { it > 0f }.coerceAtLeast(0)) }
    val maxValue = max(1f, ceil(values.max()))
    val primary = MaterialTheme.colorScheme.primary
    val grid = MaterialTheme.colorScheme.outlineVariant
    val axisWidth = 30.dp
    val plotHeight = 150.dp

    Column(modifier.semantics { if (summary.isNotEmpty()) contentDescription = summary }) {
        BoxWithConstraints(Modifier.fillMaxWidth().height(plotHeight + 28.dp)) {
            val plotWidth = maxWidth - axisWidth
            Row(Modifier.fillMaxWidth().height(plotHeight).offset(y = 28.dp)) {
                Column(
                    modifier = Modifier.width(axisWidth).fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    for (i in 4 downTo 0) {
                        val label = (maxValue * i / 4f).let { v -> if (v % 1f == 0f) "${v.toInt()}.0" else "${(v * 10).toInt() / 10f}" }
                        Text(label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Canvas(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .pointerInput(n) {
                            detectTapGestures { offset ->
                                selected = ((offset.x / size.width) * n).toInt().coerceIn(0, n - 1)
                            }
                        },
                ) {
                    val w = size.width
                    val h = size.height
                    val dash = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
                    for (i in 0..4) {
                        val y = h * i / 4f
                        drawLine(grid, Offset(0f, y), Offset(w, y), strokeWidth = 1f, pathEffect = dash)
                    }
                    val slot = w / n
                    val points = values.mapIndexed { i, v -> Offset(slot * (i + 0.5f), h - (v / maxValue) * h) }
                    points.forEachIndexed { i, p ->
                        val barW = slot * 0.3f
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(primary.copy(alpha = 0.28f), primary.copy(alpha = 0.03f)),
                                startY = p.y,
                                endY = h,
                            ),
                            topLeft = Offset(p.x - barW / 2f, p.y),
                            size = Size(barW, (h - p.y).coerceAtLeast(0f)),
                            cornerRadius = CornerRadius(barW / 2f),
                        )
                        if (i == selected) {
                            drawLine(primary.copy(alpha = 0.35f), Offset(p.x, p.y), Offset(p.x, h), strokeWidth = 2f, pathEffect = dash)
                        }
                    }
                    if (n > 1) {
                        val path = Path().apply {
                            points.forEachIndexed { i, p -> if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y) }
                        }
                        drawPath(path, primary, style = Stroke(width = 3f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                    }
                    points.forEachIndexed { i, p ->
                        drawCircle(Color.White, if (i == selected) 7f else 5f, p)
                        drawCircle(primary, if (i == selected) 7f else 5f, p, style = Stroke(width = 3f))
                    }
                }
            }
            // Tooltip above the selected point
            val value = values[selected]
            if (value > 0f) {
                val pillWidth = 84.dp
                val x = axisWidth + plotWidth * ((selected + 0.5f) / n) - pillWidth / 2
                val y = (plotHeight * (1f - value / maxValue)) - 6.dp
                Box(
                    modifier = Modifier
                        .offset(x = x.coerceIn(axisWidth, maxWidth - pillWidth), y = y.coerceAtLeast(0.dp))
                        .width(pillWidth)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onBackground)
                        .padding(vertical = 5.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "${(value * 10).toInt() / 10f} $unitLabel",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.background,
                    )
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(start = axisWidth, top = 6.dp)) {
            labels.forEach { label ->
                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
