package com.fredrickemmanuel.studyzen.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fredrickemmanuel.studyzen.core.designsystem.theme.StudyZenShape
import com.fredrickemmanuel.studyzen.domain.Subject

/** Icon and colors used wherever a subject is shown (tiles, chips, charts). */
data class SubjectStyle(val icon: AppIcon, val color: Color, val container: Color)

fun Subject.style(): SubjectStyle = when (this) {
    Subject.Programming -> SubjectStyle(AppIcon.Book, Color(0xFF7655D9), Color(0xFFEAE3FF))
    Subject.Mathematics -> SubjectStyle(AppIcon.Calculator, Color(0xFF3F7DE0), Color(0xFFE2EDFF))
    Subject.Science -> SubjectStyle(AppIcon.Target, Color(0xFF22A07A), Color(0xFFDDF6EC))
    Subject.Languages -> SubjectStyle(AppIcon.Chat, Color(0xFFB98300), Color(0xFFFFF0C9))
    Subject.Business -> SubjectStyle(AppIcon.Chart, Color(0xFFD0651F), Color(0xFFFFE8D6))
    Subject.Arts -> SubjectStyle(AppIcon.Image, Color(0xFFD1427A), Color(0xFFFFE3EE))
    Subject.Other -> SubjectStyle(AppIcon.Note, Color(0xFF6F6989), Color(0xFFEEEAF8))
}

/** Small rounded square holding the subject's icon, as in the Today's Plan rows. */
@Composable
fun SubjectIconTile(subject: Subject, modifier: Modifier = Modifier, size: Dp = 44.dp) {
    val style = subject.style()
    Box(
        modifier = modifier
            .size(size)
            .clip(StudyZenShape.IconTile)
            .background(style.container),
        contentAlignment = Alignment.Center,
    ) {
        StudyZenIcon(style.icon, size = size * 0.5f, tint = style.color)
    }
}
