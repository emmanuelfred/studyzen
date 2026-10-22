package com.fredrickemmanuel.studyzen.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

/**
 * Root theme. Wrap every screen (and every @Preview) in this.
 *
 * Light theme only for now: the PRD's visual reference is a single light design. Adding dark
 * mode later means adding a second color scheme and [StudyZenColors] instance here; screens
 * that read colors through the theme will not need changes.
 */
@Composable
fun StudyZenTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = StudyZenLightColorScheme,
        typography = rememberStudyZenTypography(),
        shapes = StudyZenShapes,
    ) {
        CompositionLocalProvider(
            LocalStudyZenColors provides StudyZenExtraColors,
            LocalStudyZenSpacing provides StudyZenSpacing(),
            content = content,
        )
    }
}

/** Access to the StudyZen-specific tokens, next to the usual `MaterialTheme.*`. */
object StudyZenTheme {
    val colors: StudyZenColors
        @Composable @ReadOnlyComposable
        get() = LocalStudyZenColors.current

    val spacing: StudyZenSpacing
        @Composable @ReadOnlyComposable
        get() = LocalStudyZenSpacing.current
}
