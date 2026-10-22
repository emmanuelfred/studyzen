package com.fredrickemmanuel.studyzen.core.designsystem.theme

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The soft, purple-tinted shadow used under cards, the bottom bar and the center action.
 * (Tint colors apply on Android 9+, iOS, desktop and web; older Android shows a neutral shadow.)
 */
fun Modifier.softShadow(
    shape: Shape = StudyZenShape.Card,
    elevation: Dp = 12.dp,
    tint: Color = StudyZenPalette.Purple,
): Modifier = shadow(
    elevation = elevation,
    shape = shape,
    ambientColor = tint.copy(alpha = 0.10f),
    spotColor = tint.copy(alpha = 0.25f),
)
