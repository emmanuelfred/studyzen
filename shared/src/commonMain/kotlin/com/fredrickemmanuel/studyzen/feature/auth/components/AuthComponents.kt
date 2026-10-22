package com.fredrickemmanuel.studyzen.feature.auth.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.fredrickemmanuel.studyzen.core.designsystem.components.BackIcon
import com.fredrickemmanuel.studyzen.core.designsystem.components.CheckIcon
import com.fredrickemmanuel.studyzen.core.designsystem.components.StudyZenTextButton
import com.fredrickemmanuel.studyzen.core.designsystem.theme.StudyZenGradients
import com.fredrickemmanuel.studyzen.core.designsystem.theme.StudyZenTheme
import com.fredrickemmanuel.studyzen.core.designsystem.theme.softShadow
import com.fredrickemmanuel.studyzen.feature.auth.AuthValidation

/**
 * Shared layout for every auth screen: lavender background, optional back button, a [header]
 * slot (badge or logo), title, subtitle and the form [content]. Scrolls when the keyboard is
 * open and keeps a comfortable maximum width on tablets, desktop and web.
 */
@Composable
fun AuthScaffold(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    header: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val spacing = StudyZenTheme.spacing

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(StudyZenGradients.Screen),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(max = 480.dp)
                .fillMaxWidth()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.screenHorizontal, vertical = spacing.md),
        ) {
            Box(Modifier.height(48.dp), contentAlignment = Alignment.CenterStart) {
                if (onBack != null) {
                    AuthBackButton(onClick = onBack)
                }
            }
            Spacer(Modifier.height(spacing.lg))

            if (header != null) {
                header()
                Spacer(Modifier.height(spacing.xl))
            }

            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(spacing.sm))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(spacing.xxl))

            content()

            Spacer(Modifier.height(spacing.xxl))
        }
    }
}

/** Round white back button with a soft shadow. */
@Composable
fun AuthBackButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .softShadow(shape = CircleShape, elevation = 8.dp)
            .size(44.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = "Back" },
        contentAlignment = Alignment.Center,
    ) {
        BackIcon(tint = MaterialTheme.colorScheme.onBackground)
    }
}

/** Large soft-lavender circle holding an icon, shown above titles on verify and reset screens. */
@Composable
fun AuthBadge(
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        icon()
    }
}

/** Server or form-level error shown above the submit button. */
@Composable
fun AuthErrorBanner(message: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer,
        )
    }
}

/** "Already have an account? Log in" style footer. */
@Composable
fun AuthFooterLink(
    prompt: String,
    action: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = prompt,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        StudyZenTextButton(text = action, onClick = onClick)
    }
}

/** Live checklist under new-password fields. A tick (not only a color) shows a rule is met. */
@Composable
fun PasswordRequirements(password: String, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        RequirementRow(
            text = "At least ${AuthValidation.MIN_PASSWORD_LENGTH} characters",
            met = AuthValidation.hasMinLength(password),
        )
        RequirementRow(
            text = "Contains a letter and a number",
            met = AuthValidation.hasLetterAndDigit(password),
        )
    }
}

@Composable
private fun RequirementRow(text: String, met: Boolean) {
    val colors = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(if (met) colors.tertiary else colors.outlineVariant),
            contentAlignment = Alignment.Center,
        ) {
            if (met) {
                CheckIcon(size = 12.dp, tint = colors.onTertiary)
            }
        }
        Spacer(Modifier.size(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = if (met) colors.onBackground else colors.onSurfaceVariant,
        )
    }
}
