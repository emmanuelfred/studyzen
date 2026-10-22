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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** The app's icon set, drawn in code on a 24 x 24 grid. Use with [StudyZenIcon]. */
enum class AppIcon {
    Home, Calendar, Chart, User, Bell, Plus, Search, Book, Clock, Play, Pause, Stop,
    Camera, Calculator, Sparkle, Note, Chat, Send, Quiz, Tune, Logout, Shield, Globe,
    ChevronRight, ChevronDown, Close, Trash, Edit, Archive, Filter, Flame, Target,
    Image, Repeat, Bulb, More,
}

@Composable
fun StudyZenIcon(
    icon: AppIcon,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    tint: Color = LocalContentColor.current,
) {
    Canvas(modifier.size(size)) {
        val u = this.size.width / 24f
        val stroke = Stroke(width = 2f * u, cap = StrokeCap.Round, join = StrokeJoin.Round)

        fun o(x: Float, y: Float) = Offset(x * u, y * u)
        fun Path.m(x: Float, y: Float) = moveTo(x * u, y * u)
        fun Path.l(x: Float, y: Float) = lineTo(x * u, y * u)
        fun Path.c(a: Float, b: Float, c: Float, d: Float, e: Float, f: Float) =
            cubicTo(a * u, b * u, c * u, d * u, e * u, f * u)

        fun line(x1: Float, y1: Float, x2: Float, y2: Float, width: Float = 2f) =
            drawLine(tint, o(x1, y1), o(x2, y2), strokeWidth = width * u, cap = StrokeCap.Round)

        fun outline(block: Path.() -> Unit) = drawPath(Path().apply(block), tint, style = stroke)
        fun solid(block: Path.() -> Unit) = drawPath(Path().apply(block), tint)
        fun rrect(x: Float, y: Float, w: Float, h: Float, r: Float) =
            drawRoundRect(tint, o(x, y), Size(w * u, h * u), CornerRadius(r * u), style = stroke)
        fun ring(x: Float, y: Float, r: Float) = drawCircle(tint, r * u, o(x, y), style = stroke)
        fun dot(x: Float, y: Float, r: Float) = drawCircle(tint, r * u, o(x, y))

        when (icon) {
            AppIcon.Home -> {
                outline { m(4f, 11.5f); l(12f, 4.5f); l(20f, 11.5f) }
                outline { m(6f, 10f); l(6f, 19.5f); l(18f, 19.5f); l(18f, 10f) }
                outline { m(10f, 19.5f); l(10f, 14.5f); l(14f, 14.5f); l(14f, 19.5f) }
            }
            AppIcon.Calendar -> {
                rrect(4f, 5.5f, 16f, 14.5f, 3f)
                line(4f, 10f, 20f, 10f)
                line(8.5f, 3.5f, 8.5f, 7f)
                line(15.5f, 3.5f, 15.5f, 7f)
            }
            AppIcon.Chart -> {
                line(7f, 19f, 7f, 13f, 3f)
                line(12f, 19f, 12f, 7f, 3f)
                line(17f, 19f, 17f, 11f, 3f)
            }
            AppIcon.User -> {
                ring(12f, 8f, 3.8f)
                outline { m(5f, 20f); c(5f, 15.5f, 8f, 14f, 12f, 14f); c(16f, 14f, 19f, 15.5f, 19f, 20f) }
            }
            AppIcon.Bell -> {
                outline {
                    m(6f, 16f); l(6f, 11f); c(6f, 7.5f, 8.5f, 5.5f, 12f, 5.5f)
                    c(15.5f, 5.5f, 18f, 7.5f, 18f, 11f); l(18f, 16f); l(19.5f, 18f); l(4.5f, 18f); close()
                }
                outline { m(10f, 20.5f); c(10.8f, 21.5f, 13.2f, 21.5f, 14f, 20.5f) }
            }
            AppIcon.Plus -> {
                line(12f, 5f, 12f, 19f, 2.4f)
                line(5f, 12f, 19f, 12f, 2.4f)
            }
            AppIcon.Search -> {
                ring(11f, 11f, 6f)
                line(15.5f, 15.5f, 20f, 20f)
            }
            AppIcon.Book -> {
                outline {
                    m(12f, 6.5f); c(10f, 5f, 7f, 4.8f, 4f, 5.8f); l(4f, 18.5f)
                    c(7f, 17.5f, 10f, 17.7f, 12f, 19.2f); c(14f, 17.7f, 17f, 17.5f, 20f, 18.5f)
                    l(20f, 5.8f); c(17f, 4.8f, 14f, 5f, 12f, 6.5f); close()
                }
                line(12f, 6.5f, 12f, 19.2f)
            }
            AppIcon.Clock -> {
                ring(12f, 12f, 8.5f)
                outline { m(12f, 7.5f); l(12f, 12f); l(15f, 14f) }
            }
            AppIcon.Play -> solid { m(8f, 5.5f); l(18.5f, 12f); l(8f, 18.5f); close() }
            AppIcon.Pause -> {
                drawRoundRect(tint, o(6.5f, 5f), Size(3.8f * u, 14f * u), CornerRadius(1.2f * u))
                drawRoundRect(tint, o(13.7f, 5f), Size(3.8f * u, 14f * u), CornerRadius(1.2f * u))
            }
            AppIcon.Stop -> drawRoundRect(tint, o(6f, 6f), Size(12f * u, 12f * u), CornerRadius(2.5f * u))
            AppIcon.Camera -> {
                rrect(3f, 7.5f, 18f, 12f, 3f)
                ring(12f, 13.5f, 3.5f)
                outline { m(8.5f, 7.5f); l(10f, 4.8f); l(14f, 4.8f); l(15.5f, 7.5f) }
            }
            AppIcon.Calculator -> {
                rrect(5f, 3f, 14f, 18f, 3f)
                rrect(8f, 6f, 8f, 3.5f, 1f)
                dot(9f, 13.5f, 1.1f); dot(12f, 13.5f, 1.1f); dot(15f, 13.5f, 1.1f)
                dot(9f, 17f, 1.1f); dot(12f, 17f, 1.1f); dot(15f, 17f, 1.1f)
            }
            AppIcon.Sparkle -> outline {
                m(12f, 3.5f); c(12.8f, 8.5f, 15.5f, 11.2f, 20.5f, 12f)
                c(15.5f, 12.8f, 12.8f, 15.5f, 12f, 20.5f); c(11.2f, 15.5f, 8.5f, 12.8f, 3.5f, 12f)
                c(8.5f, 11.2f, 11.2f, 8.5f, 12f, 3.5f); close()
            }
            AppIcon.Note -> {
                rrect(5f, 3f, 14f, 18f, 2.5f)
                line(8.5f, 8.5f, 15.5f, 8.5f)
                line(8.5f, 12f, 15.5f, 12f)
                line(8.5f, 15.5f, 12.5f, 15.5f)
            }
            AppIcon.Chat -> {
                rrect(3.5f, 4.5f, 17f, 12f, 4f)
                outline { m(8.5f, 16.5f); l(8.5f, 20.5f); l(12.5f, 16.5f) }
            }
            AppIcon.Send -> {
                outline { m(4f, 11.5f); l(20f, 4.5f); l(13.5f, 20f); l(11f, 13.2f); close() }
                line(11f, 13.2f, 20f, 4.5f)
            }
            AppIcon.Quiz -> {
                ring(12f, 12f, 9f)
                outline { m(9.6f, 9.6f); c(9.6f, 6.8f, 14.4f, 6.8f, 14.4f, 9.8f); c(14.4f, 11.8f, 12f, 11.8f, 12f, 14f) }
                dot(12f, 16.9f, 1.1f)
            }
            AppIcon.Tune -> {
                line(4f, 7f, 20f, 7f); line(4f, 12f, 20f, 12f); line(4f, 17f, 20f, 17f)
                dot(9f, 7f, 2.4f); dot(15f, 12f, 2.4f); dot(8f, 17f, 2.4f)
            }
            AppIcon.Logout -> {
                outline { m(10f, 4.5f); l(6.5f, 4.5f); l(6.5f, 19.5f); l(10f, 19.5f) }
                line(10.5f, 12f, 19.5f, 12f)
                outline { m(16f, 8.5f); l(19.5f, 12f); l(16f, 15.5f) }
            }
            AppIcon.Shield -> outline {
                m(12f, 3.5f); l(19f, 6.2f); l(19f, 12f); c(19f, 16.3f, 16f, 19.5f, 12f, 21f)
                c(8f, 19.5f, 5f, 16.3f, 5f, 12f); l(5f, 6.2f); close()
            }
            AppIcon.Globe -> {
                ring(12f, 12f, 9f)
                drawOval(tint, o(8f, 3f), Size(8f * u, 18f * u), style = stroke)
                line(3f, 12f, 21f, 12f)
            }
            AppIcon.ChevronRight -> outline { m(9f, 5.5f); l(15.5f, 12f); l(9f, 18.5f) }
            AppIcon.ChevronDown -> outline { m(5.5f, 9f); l(12f, 15.5f); l(18.5f, 9f) }
            AppIcon.Close -> {
                line(6f, 6f, 18f, 18f, 2.4f)
                line(18f, 6f, 6f, 18f, 2.4f)
            }
            AppIcon.Trash -> {
                line(4.5f, 7f, 19.5f, 7f)
                outline { m(9.5f, 7f); l(9.5f, 4.5f); l(14.5f, 4.5f); l(14.5f, 7f) }
                outline { m(6.5f, 7f); l(7.5f, 19.5f); l(16.5f, 19.5f); l(17.5f, 7f) }
                line(10.5f, 10.5f, 10.5f, 16f); line(13.5f, 10.5f, 13.5f, 16f)
            }
            AppIcon.Edit -> {
                outline { m(4.5f, 19.5f); l(5.3f, 15f); l(15.8f, 4.5f); l(19.5f, 8.2f); l(9f, 18.7f); close() }
                line(13.5f, 6.8f, 17.2f, 10.5f)
            }
            AppIcon.Archive -> {
                rrect(3.5f, 4.5f, 17f, 4.5f, 1.5f)
                outline {
                    m(5.5f, 9f); l(5.5f, 18.5f); c(5.5f, 19.2f, 6f, 19.5f, 6.7f, 19.5f)
                    l(17.3f, 19.5f); c(18f, 19.5f, 18.5f, 19.2f, 18.5f, 18.5f); l(18.5f, 9f)
                }
                line(10f, 13f, 14f, 13f)
            }
            AppIcon.Filter -> outline { m(4f, 5.5f); l(20f, 5.5f); l(14f, 12.5f); l(14f, 19f); l(10f, 17f); l(10f, 12.5f); close() }
            AppIcon.Flame -> outline {
                m(12f, 3f); c(13f, 7.2f, 17f, 9.2f, 17f, 14f); c(17f, 17.6f, 14.8f, 20.5f, 12f, 20.5f)
                c(9.2f, 20.5f, 7f, 17.6f, 7f, 14f); c(7f, 12f, 8f, 10.5f, 9f, 9.5f)
                c(9.2f, 11f, 10f, 12f, 11f, 12.2f); c(10.4f, 8.2f, 10.8f, 5.4f, 12f, 3f); close()
            }
            AppIcon.Target -> {
                ring(12f, 12f, 9f)
                ring(12f, 12f, 5f)
                dot(12f, 12f, 1.5f)
            }
            AppIcon.Image -> {
                rrect(3.5f, 4.5f, 17f, 15f, 3f)
                ring(9f, 10f, 1.8f)
                outline { m(4f, 17f); l(9f, 13f); l(13f, 16f); l(16f, 13f); l(20f, 16.5f) }
            }
            AppIcon.Repeat -> {
                drawArc(tint, -40f, 270f, false, o(5f, 5f), Size(14f * u, 14f * u), style = stroke)
                outline { m(17.4f, 3.8f); l(17.4f, 7.5f); l(13.6f, 7.5f) }
            }
            AppIcon.Bulb -> {
                ring(12f, 10f, 5f)
                outline { m(9.5f, 14.2f); l(9.5f, 16.5f); l(14.5f, 16.5f); l(14.5f, 14.2f) }
                line(10f, 19.5f, 14f, 19.5f)
            }
            AppIcon.More -> {
                dot(5.5f, 12f, 1.8f); dot(12f, 12f, 1.8f); dot(18.5f, 12f, 1.8f)
            }
        }
    }
}
