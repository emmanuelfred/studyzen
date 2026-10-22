package com.fredrickemmanuel.studyzen.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.fredrickemmanuel.studyzen.core.designsystem.theme.StudyZenGradients
import com.fredrickemmanuel.studyzen.core.designsystem.theme.StudyZenSizes
import com.fredrickemmanuel.studyzen.core.designsystem.theme.softShadow

data class NavItem(val icon: AppIcon, val label: String)

/**
 * Compact bottom bar: two items, a raised round center action, then two more items
 * (Home, Plan, [+], Progress, Profile). [items] must contain four entries.
 */
@Composable
fun BottomNavBar(
    items: List<NavItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onCenterAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    Box(modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .softShadow(shape = shape, elevation = 16.dp)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surface)
                .navigationBarsPadding()
                .height(StudyZenSizes.BottomBarHeight),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEachIndexed { index, item ->
                if (index == 2) Box(Modifier.weight(1f))   // room for the center action
                NavBarItem(
                    item = item,
                    selected = index == selectedIndex,
                    onClick = { onSelect(index) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-StudyZenSizes.CenterAction / 2) + 4.dp)
                .softShadow(shape = CircleShape, elevation = 14.dp)
                .size(StudyZenSizes.CenterAction)
                .clip(CircleShape)
                .background(StudyZenGradients.Primary)
                .clickable(role = Role.Button, onClick = onCenterAction)
                .semantics { contentDescription = "Create" },
            contentAlignment = Alignment.Center,
        ) {
            StudyZenIcon(AppIcon.Plus, tint = MaterialTheme.colorScheme.onPrimary, size = 28.dp)
        }
    }
}

@Composable
private fun NavBarItem(
    item: NavItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier = modifier
            .height(StudyZenSizes.BottomBarHeight)
            .clickable(role = Role.Tab, onClick = onClick)
            .semantics { this.selected = selected },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
    ) {
        StudyZenIcon(item.icon, size = 24.dp, tint = color)
        Text(item.label, style = MaterialTheme.typography.labelSmall, color = color)
    }
}
