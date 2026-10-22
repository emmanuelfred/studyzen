package com.fredrickemmanuel.studyzen.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** Material shape scale: soft, generously rounded corners everywhere. */
internal val StudyZenShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

/** Named shapes for StudyZen components. */
object StudyZenShape {
    /** Buttons, filter chips, tabs, progress tracks. */
    val Pill = RoundedCornerShape(percent = 50)

    /** Standard content card (Today's Plan rows, stat tiles, tip card). */
    val Card = RoundedCornerShape(24.dp)

    /** Large featured card (the purple "Data Structures" card on Home). */
    val FeaturedCard = RoundedCornerShape(28.dp)

    /** Small rounded square behind subject icons. */
    val IconTile = RoundedCornerShape(14.dp)

    /** Bottom sheets and dialogs. */
    val Sheet = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
}
