package com.fredrickemmanuel.studyzen.core.designsystem.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fredrickemmanuel.studyzen.core.designsystem.theme.StudyZenShape
import com.fredrickemmanuel.studyzen.core.designsystem.theme.StudyZenSizes

/**
 * Primary call-to-action: full width, pill shaped, solid purple.
 * While [loading] it shows a spinner and ignores taps (it stays purple, not greyed out).
 */
@Composable
fun StudyZenButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    val colors = MaterialTheme.colorScheme
    Button(
        onClick = { if (!loading) onClick() },
        modifier = modifier
            .fillMaxWidth()
            .height(StudyZenSizes.ButtonHeight),
        enabled = enabled,
        shape = StudyZenShape.Pill,
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.primary,
            contentColor = colors.onPrimary,
            disabledContainerColor = colors.primary.copy(alpha = 0.40f),
            disabledContentColor = colors.onPrimary,
        ),
        elevation = null,
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = colors.onPrimary,
                strokeWidth = 2.5.dp,
            )
        } else {
            Text(text = text, style = MaterialTheme.typography.titleMedium)
        }
    }
}

/** Low-emphasis text link such as "Forgot password?" or "Sign up". */
@Composable
fun StudyZenTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    TextButton(onClick = onClick, modifier = modifier, enabled = enabled) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}
