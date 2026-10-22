package com.fredrickemmanuel.studyzen.core.designsystem.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Gradients are used sparingly: full-screen onboarding/splash backgrounds and featured
 * learning elements. Do not give every card its own gradient.
 */
object StudyZenGradients {
    /** Full-screen pale lavender wash used by splash and onboarding. */
    val Screen: Brush = Brush.verticalGradient(
        colors = listOf(Color(0xFFE6DEFF), Color(0xFFF3F0FF), Color(0xFFFFFFFF)),
    )

    /** Featured card (e.g. the active Study on Home). */
    val FeaturedCard: Brush = Brush.linearGradient(
        colors = listOf(Color(0xFF8E7BEA), Color(0xFF7155D8)),
    )

    /** Primary call-to-action fill. */
    val Primary: Brush = Brush.linearGradient(
        colors = listOf(Color(0xFF8A6FEA), Color(0xFF6C4FD6)),
    )

    /** Soft glow placed behind the mascot. */
    val MascotGlow: Brush = Brush.radialGradient(
        colors = listOf(StudyZenPalette.Lavender.copy(alpha = 0.35f), Color.Transparent),
    )
}
