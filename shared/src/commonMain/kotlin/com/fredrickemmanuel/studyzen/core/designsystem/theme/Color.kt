package com.fredrickemmanuel.studyzen.core.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Raw brand palette. Never use these directly in screens; read colors through
 * [androidx.compose.material3.MaterialTheme.colorScheme] or [StudyZenTheme.colors] so a
 * future dark theme only needs to change the mapping, not the screens.
 *
 * Base values come from the StudyZen PRD (section 2.2). Two values were adjusted for
 * accessibility and are marked below.
 */
object StudyZenPalette {
    // Purples
    val Purple = Color(0xFF7655D9)          // primary actions, active elements
    val PurpleDeep = Color(0xFF4F33B5)      // text/icons on soft lavender, pressed states
    val Lavender = Color(0xFFA995F4)        // secondary accents, illustrations
    val SoftLavender = Color(0xFFEAE3FF)    // selected states, gentle backgrounds

    // Surfaces
    val PaleBackground = Color(0xFFF8F6FF)  // main app background
    val White = Color(0xFFFFFFFF)           // cards and content surfaces

    // Text
    val TextMain = Color(0xFF25213B)        // headings and body
    // PRD suggests #817B97, but that is only 3.8:1 on the pale background. Darkened slightly
    // so small supporting text reaches WCAG AA (4.8:1).
    val TextSecondary = Color(0xFF6F6989)

    // Accents
    val Blue = Color(0xFF75A9F8)            // secondary categories and charts
    val Mint = Color(0xFF70D5B2)            // positive progress, selected indicators
    val Yellow = Color(0xFFFFD77B)          // occasional highlights

    // Lines and feedback
    val Outline = Color(0xFFCFC8E6)
    val OutlineSoft = Color(0xFFE8E3F7)
    val Error = Color(0xFFC8374F)           // white text on it is 5.1:1
    val ErrorContainer = Color(0xFFFFE4E8)
}

/** Material 3 mapping of the palette (light theme only for now). */
internal val StudyZenLightColorScheme: ColorScheme = lightColorScheme(
    primary = StudyZenPalette.Purple,
    onPrimary = StudyZenPalette.White,
    primaryContainer = StudyZenPalette.SoftLavender,
    onPrimaryContainer = StudyZenPalette.PurpleDeep,
    inversePrimary = StudyZenPalette.Lavender,

    secondary = StudyZenPalette.Lavender,
    onSecondary = StudyZenPalette.TextMain,
    secondaryContainer = StudyZenPalette.SoftLavender,
    onSecondaryContainer = StudyZenPalette.PurpleDeep,

    tertiary = StudyZenPalette.Mint,
    onTertiary = StudyZenPalette.TextMain,
    tertiaryContainer = Color(0xFFDDF6EC),
    onTertiaryContainer = Color(0xFF0E5A42),

    background = StudyZenPalette.PaleBackground,
    onBackground = StudyZenPalette.TextMain,

    surface = StudyZenPalette.White,
    onSurface = StudyZenPalette.TextMain,
    surfaceVariant = Color(0xFFF1EDFF),
    onSurfaceVariant = StudyZenPalette.TextSecondary,
    surfaceTint = StudyZenPalette.Purple,
    inverseSurface = StudyZenPalette.TextMain,
    inverseOnSurface = StudyZenPalette.PaleBackground,

    surfaceBright = StudyZenPalette.White,
    surfaceDim = Color(0xFFEDE9FA),
    surfaceContainerLowest = StudyZenPalette.White,
    surfaceContainerLow = StudyZenPalette.PaleBackground,
    surfaceContainer = Color(0xFFF3F0FF),
    surfaceContainerHigh = Color(0xFFEEEAFD),
    surfaceContainerHighest = StudyZenPalette.SoftLavender,

    outline = StudyZenPalette.Outline,
    outlineVariant = StudyZenPalette.OutlineSoft,

    error = StudyZenPalette.Error,
    onError = StudyZenPalette.White,
    errorContainer = StudyZenPalette.ErrorContainer,
    onErrorContainer = Color(0xFF8A1F32),

    scrim = Color(0xFF1B1730),
)

/**
 * Colors that Material 3 has no slot for: the supporting accents used for subjects,
 * charts, progress and illustrations.
 */
@Immutable
data class StudyZenColors(
    val purple: Color,
    val purpleDeep: Color,
    val lavender: Color,
    val softLavender: Color,
    val blue: Color,
    val mint: Color,
    val yellow: Color,
    val textSecondary: Color,
    /** Fill for positive progress and "done" states. */
    val success: Color,
    /** Ordered series colors for charts and subject breakdowns. */
    val chart: List<Color>,
)

internal val StudyZenExtraColors = StudyZenColors(
    purple = StudyZenPalette.Purple,
    purpleDeep = StudyZenPalette.PurpleDeep,
    lavender = StudyZenPalette.Lavender,
    softLavender = StudyZenPalette.SoftLavender,
    blue = StudyZenPalette.Blue,
    mint = StudyZenPalette.Mint,
    yellow = StudyZenPalette.Yellow,
    textSecondary = StudyZenPalette.TextSecondary,
    success = StudyZenPalette.Mint,
    chart = listOf(
        StudyZenPalette.Purple,
        StudyZenPalette.Blue,
        StudyZenPalette.Mint,
        StudyZenPalette.Yellow,
        StudyZenPalette.Lavender,
    ),
)

internal val LocalStudyZenColors = staticCompositionLocalOf { StudyZenExtraColors }
