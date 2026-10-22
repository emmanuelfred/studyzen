package com.fredrickemmanuel.studyzen.feature.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fredrickemmanuel.studyzen.core.designsystem.components.AppIcon
import com.fredrickemmanuel.studyzen.core.designsystem.components.BottomNavBar
import com.fredrickemmanuel.studyzen.core.designsystem.components.NavItem
import com.fredrickemmanuel.studyzen.core.designsystem.components.SettingsRow
import com.fredrickemmanuel.studyzen.core.designsystem.components.StudyZenCard
import com.fredrickemmanuel.studyzen.core.designsystem.theme.StudyZenGradients
import com.fredrickemmanuel.studyzen.core.designsystem.theme.StudyZenShape
import com.fredrickemmanuel.studyzen.core.time.nowMinuteOfDay
import com.fredrickemmanuel.studyzen.core.time.todayEpochDay
import com.fredrickemmanuel.studyzen.data.LocalStudyStore
import com.fredrickemmanuel.studyzen.feature.home.HomeScreen
import com.fredrickemmanuel.studyzen.feature.plan.PlanScreen
import com.fredrickemmanuel.studyzen.feature.profile.ProfileScreen
import com.fredrickemmanuel.studyzen.feature.progress.ProgressScreen
import com.fredrickemmanuel.studyzen.navigation.LocalAppNavigator
import com.fredrickemmanuel.studyzen.navigation.Screen

object MainTab {
    const val Home = 0
    const val Plan = 1
    const val Progress = 2
    const val Profile = 3
}

private val NavItems = listOf(
    NavItem(AppIcon.Home, "Home"),
    NavItem(AppIcon.Calendar, "Plan"),
    NavItem(AppIcon.Chart, "Progress"),
    NavItem(AppIcon.User, "Profile"),
)

/** The four main tabs with the bottom bar and the center "create" menu. */
@Composable
fun MainScreen(
    selectedTab: Int,
    onTabChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val nav = LocalAppNavigator.current
    val store = LocalStudyStore.current
    var menuOpen by rememberSaveable { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(StudyZenGradients.Screen),
    ) {
        when (selectedTab) {
            MainTab.Home -> HomeScreen(onSeeAllPlan = { onTabChange(MainTab.Plan) })
            MainTab.Plan -> PlanScreen()
            MainTab.Progress -> ProgressScreen()
            else -> ProfileScreen()
        }

        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .widthIn(max = 560.dp),
        ) {
            BottomNavBar(
                items = NavItems,
                selectedIndex = selectedTab,
                onSelect = onTabChange,
                onCenterAction = { menuOpen = true },
            )
        }

        if (menuOpen) {
            CreateMenu(
                onDismiss = { menuOpen = false },
                onNewStudy = { menuOpen = false; nav.navigate(Screen.StudyEditor(null)) },
                onNewPlan = { menuOpen = false; nav.navigate(Screen.PlanBuilder(null)) },
                onStartStudying = { menuOpen = false; nav.navigate(Screen.StartStudying) },
                onScan = { menuOpen = false; nav.navigate(Screen.Scanner(null)) },
                onCalculator = { menuOpen = false; nav.navigate(Screen.Calculator(null)) },
                onDictionary = { menuOpen = false; nav.navigate(Screen.Dictionary(null)) },
                onNotes = { menuOpen = false; nav.navigate(Screen.Notes) },
            )
        }
    }
}

/** Card that pops up from the center "+" button with the quick actions. */
@Composable
private fun CreateMenu(
    onDismiss: () -> Unit,
    onNewStudy: () -> Unit,
    onNewPlan: () -> Unit,
    onStartStudying: () -> Unit,
    onScan: () -> Unit,
    onCalculator: () -> Unit,
    onDictionary: () -> Unit,
    onNotes: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.40f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss,
            ),
        contentAlignment = Alignment.BottomCenter,
    ) {
        StudyZenCard(
            modifier = Modifier
                .widthIn(max = 420.dp)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 100.dp)
                // swallow taps so touching the card does not close the menu
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                ),
            shape = StudyZenShape.FeaturedCard,
        ) {
            // seven actions can be taller than a small screen, so the list scrolls
            Column(Modifier.heightIn(max = 440.dp).verticalScroll(rememberScrollState())) {
                SettingsRow(AppIcon.Book, "New Study", subtitle = "A workspace for any topic", onClick = onNewStudy)
                SettingsRow(AppIcon.Calendar, "New study plan", subtitle = "Schedule sessions and reminders", onClick = onNewPlan)
                SettingsRow(AppIcon.Play, "Start studying", subtitle = "Begin or continue a session", onClick = onStartStudying)
                SettingsRow(AppIcon.Camera, "Scan a problem", subtitle = "Get a step-by-step solution", onClick = onScan)
                SettingsRow(AppIcon.Calculator, "Calculator", subtitle = "Solve and explain", onClick = onCalculator)
                SettingsRow(AppIcon.Search, "Dictionary", subtitle = "Look up words, favourites, word of the day", onClick = onDictionary)
                SettingsRow(AppIcon.Note, "Notes", subtitle = "Write and find your notes", onClick = onNotes)
            }
        }
    }
}

/** Scrolling column used by every tab: safe top padding and room for the bottom bar. */
@Composable
fun TabContainer(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 12.dp, bottom = 130.dp),
            content = content,
        )
    }
}
