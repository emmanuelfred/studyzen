package com.fredrickemmanuel.studyzen.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** 4dp-based spacing scale. Use these instead of raw dp values in screens. */
@Immutable
data class StudyZenSpacing(
    val xxs: Dp = 2.dp,
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 12.dp,
    val lg: Dp = 16.dp,
    val xl: Dp = 20.dp,
    val xxl: Dp = 24.dp,
    val xxxl: Dp = 32.dp,
    val huge: Dp = 48.dp,
    /** Horizontal padding of every full-width screen. */
    val screenHorizontal: Dp = 20.dp,
)

/** Fixed component dimensions so heights stay consistent across screens. */
object StudyZenSizes {
    val ButtonHeight = 56.dp
    val PillHeight = 40.dp
    val IconSmall = 18.dp
    val Icon = 24.dp
    val IconTile = 44.dp
    val Avatar = 44.dp
    val BottomBarHeight = 72.dp
    val CenterAction = 60.dp
    val ProgressRing = 40.dp
}

internal val LocalStudyZenSpacing = staticCompositionLocalOf { StudyZenSpacing() }
