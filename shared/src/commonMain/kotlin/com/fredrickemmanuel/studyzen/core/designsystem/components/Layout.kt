package com.fredrickemmanuel.studyzen.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fredrickemmanuel.studyzen.core.designsystem.theme.StudyZenGradients
import com.fredrickemmanuel.studyzen.core.designsystem.theme.StudyZenShape
import com.fredrickemmanuel.studyzen.core.designsystem.theme.StudyZenTheme
import com.fredrickemmanuel.studyzen.core.designsystem.theme.softShadow

/** White rounded card with the soft purple shadow. Clickable when [onClick] is given. */
@Composable
fun StudyZenCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    shape: Shape = StudyZenShape.Card,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentPadding: Int = 16,
    content: @Composable ColumnScope.() -> Unit,
) {
    val base = modifier
        .softShadow(shape = shape, elevation = 10.dp)
        .clip(shape)
        .background(containerColor)
    val clickable = if (onClick != null) base.clickable(role = Role.Button, onClick = onClick) else base
    Column(clickable.padding(contentPadding.dp), content = content)
}

/** 44dp white circle button with a soft shadow (back, bell, calendar...). */
@Composable
fun RoundIconButton(
    icon: AppIcon,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onBackground,
    container: Color = MaterialTheme.colorScheme.surface,
) {
    Box(
        modifier = modifier
            .softShadow(shape = CircleShape, elevation = 8.dp)
            .size(44.dp)
            .clip(CircleShape)
            .background(container)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        StudyZenIcon(icon, tint = tint, size = 22.dp)
    }
}

/** Title row with an optional back button and trailing actions. */
@Composable
fun ScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            AuthBackIcon(onBack)
        } else {
            Spacer(Modifier.size(44.dp))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        if (trailing != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically, content = trailing)
        } else {
            Spacer(Modifier.size(44.dp))
        }
    }
}

@Composable
private fun AuthBackIcon(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .softShadow(shape = CircleShape, elevation = 8.dp)
            .size(44.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(role = Role.Button, onClick = onBack)
            .semantics { contentDescription = "Back" },
        contentAlignment = Alignment.Center,
    ) {
        BackIcon(tint = MaterialTheme.colorScheme.onBackground)
    }
}

/**
 * Standard screen frame: lavender background, header, scrolling content, optional pinned
 * [bottomBar] (use it for the main button). Content is centered with a maximum width so it
 * also looks right on tablets, desktop and web.
 */
@Composable
fun ScreenScaffold(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
    bottomBar: (@Composable () -> Unit)? = null,
    scroll: Boolean = true,
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
                .widthIn(max = 560.dp)
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = spacing.screenHorizontal),
        ) {
            ScreenHeader(title = title, onBack = onBack, trailing = trailing)
            val body = Modifier
                .weight(1f)
                .fillMaxWidth()
            Column(
                modifier = if (scroll) body.verticalScroll(rememberScrollState()) else body,
                content = content,
            )
            if (bottomBar != null) {
                Box(Modifier.padding(vertical = spacing.md)) { bottomBar() }
            }
        }
    }
}

/** Section title with an optional action on the right ("Today's Plan   See All"). */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
        if (actionLabel != null && onAction != null) {
            Text(
                text = actionLabel,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .clickable(role = Role.Button, onClick = onAction)
                    .padding(vertical = 8.dp),
            )
        }
    }
}

/** A tappable row for settings lists: icon tile, title, optional subtitle, trailing content. */
@Composable
fun SettingsRow(
    icon: AppIcon,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    trailing: (@Composable () -> Unit)? = {
        StudyZenIcon(AppIcon.ChevronRight, size = 20.dp, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    },
) {
    val rowModifier = if (onClick != null) {
        modifier.clickable(role = Role.Button, onClick = onClick)
    } else {
        modifier
    }
    Row(
        modifier = rowModifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(StudyZenShape.IconTile)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            StudyZenIcon(icon, size = 20.dp, tint = iconTint)
        }
        Column(
            Modifier
                .weight(1f)
                .padding(horizontal = 14.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = titleColor)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (trailing != null) trailing()
    }
}
