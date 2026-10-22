package com.fredrickemmanuel.studyzen

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.fredrickemmanuel.studyzen.core.designsystem.theme.StudyZenPalette
import kotlin.test.Test
import kotlin.test.assertTrue

/** Guards the accessibility promise in the PRD: readable text on every core surface. */
class ThemeContrastTest {

    private fun contrast(a: Color, b: Color): Float {
        val l1 = maxOf(a.luminance(), b.luminance())
        val l2 = minOf(a.luminance(), b.luminance())
        return (l1 + 0.05f) / (l2 + 0.05f)
    }

    private fun assertReadable(foreground: Color, background: Color, name: String) {
        val ratio = contrast(foreground, background)
        assertTrue(ratio >= 4.5f, "$name contrast is $ratio, expected at least 4.5")
    }

    @Test
    fun bodyTextIsReadable() {
        assertReadable(StudyZenPalette.TextMain, StudyZenPalette.PaleBackground, "main text on background")
        assertReadable(StudyZenPalette.TextMain, StudyZenPalette.White, "main text on cards")
        assertReadable(StudyZenPalette.TextSecondary, StudyZenPalette.PaleBackground, "secondary text on background")
        assertReadable(StudyZenPalette.TextSecondary, StudyZenPalette.White, "secondary text on cards")
    }

    @Test
    fun actionsAreReadable() {
        assertReadable(StudyZenPalette.White, StudyZenPalette.Purple, "button label on primary")
        assertReadable(StudyZenPalette.PurpleDeep, StudyZenPalette.SoftLavender, "selected pill text")
        assertReadable(StudyZenPalette.White, StudyZenPalette.Error, "label on error")
    }
}
