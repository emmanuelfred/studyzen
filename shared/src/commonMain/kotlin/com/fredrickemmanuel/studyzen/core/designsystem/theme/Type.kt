package com.fredrickemmanuel.studyzen.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.Font
import studyzen.shared.generated.resources.Res
import studyzen.shared.generated.resources.nunito_sans_bold
import studyzen.shared.generated.resources.nunito_sans_extrabold
import studyzen.shared.generated.resources.nunito_sans_medium
import studyzen.shared.generated.resources.nunito_sans_regular
import studyzen.shared.generated.resources.nunito_sans_semibold

/**
 * Nunito Sans (rounded, friendly, highly readable). Static weights live in
 * `composeResources/font`, so the font is identical on every platform.
 */
@Composable
fun rememberStudyZenFontFamily(): FontFamily {
    val regular = Font(Res.font.nunito_sans_regular, FontWeight.Normal)
    val medium = Font(Res.font.nunito_sans_medium, FontWeight.Medium)
    val semiBold = Font(Res.font.nunito_sans_semibold, FontWeight.SemiBold)
    val bold = Font(Res.font.nunito_sans_bold, FontWeight.Bold)
    val extraBold = Font(Res.font.nunito_sans_extrabold, FontWeight.ExtraBold)
    return remember(regular, medium, semiBold, bold, extraBold) {
        FontFamily(regular, medium, semiBold, bold, extraBold)
    }
}

/**
 * Type scale:
 *  - display / headline: bold, clear page headings (onboarding, screen titles)
 *  - title: friendly section headings and card titles
 *  - body: readable content
 *  - label: buttons, pills, tabs, small muted metadata
 */
@Composable
fun rememberStudyZenTypography(): Typography {
    val family = rememberStudyZenFontFamily()
    return remember(family) {
        fun style(
            size: Int,
            lineHeight: Int,
            weight: FontWeight,
            letterSpacing: Double = 0.0,
        ) = TextStyle(
            fontFamily = family,
            fontWeight = weight,
            fontSize = size.sp,
            lineHeight = lineHeight.sp,
            letterSpacing = letterSpacing.sp,
        )

        Typography(
            displayLarge = style(44, 52, FontWeight.ExtraBold, -0.5),
            displayMedium = style(38, 46, FontWeight.ExtraBold, -0.4),
            displaySmall = style(32, 40, FontWeight.ExtraBold, -0.3),

            headlineLarge = style(30, 38, FontWeight.Bold, -0.2),
            headlineMedium = style(26, 34, FontWeight.Bold, -0.1),
            headlineSmall = style(22, 30, FontWeight.Bold),

            titleLarge = style(20, 28, FontWeight.Bold),
            titleMedium = style(16, 24, FontWeight.SemiBold, 0.1),
            titleSmall = style(14, 20, FontWeight.SemiBold, 0.1),

            bodyLarge = style(16, 24, FontWeight.Normal, 0.1),
            bodyMedium = style(14, 20, FontWeight.Normal, 0.1),
            bodySmall = style(12, 16, FontWeight.Normal, 0.2),

            labelLarge = style(15, 20, FontWeight.SemiBold, 0.2),
            labelMedium = style(12, 16, FontWeight.SemiBold, 0.3),
            labelSmall = style(11, 14, FontWeight.Medium, 0.3),
        )
    }
}
