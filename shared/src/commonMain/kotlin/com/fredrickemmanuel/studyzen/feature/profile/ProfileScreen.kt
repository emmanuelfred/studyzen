package com.fredrickemmanuel.studyzen.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.fredrickemmanuel.studyzen.navigation.LocalAuthRepository
import com.fredrickemmanuel.studyzen.navigation.LocalSyncEngine
import com.fredrickemmanuel.studyzen.data.GuestMode
import com.fredrickemmanuel.studyzen.data.sync.SyncStatus
import com.fredrickemmanuel.studyzen.core.time.formatClock
import com.fredrickemmanuel.studyzen.core.time.utcOffsetMinutes
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.fredrickemmanuel.studyzen.core.designsystem.components.AppIcon
import com.fredrickemmanuel.studyzen.core.designsystem.components.SectionHeader
import com.fredrickemmanuel.studyzen.core.designsystem.components.SettingsRow
import com.fredrickemmanuel.studyzen.core.designsystem.components.StudyZenCard
import com.fredrickemmanuel.studyzen.core.designsystem.components.SwitchRow
import com.fredrickemmanuel.studyzen.data.LocalStudyStore
import com.fredrickemmanuel.studyzen.feature.main.TabContainer
import com.fredrickemmanuel.studyzen.navigation.LocalAppNavigator
import com.fredrickemmanuel.studyzen.navigation.Screen

/** Profile tab: account, preferences, notifications, security, sample data and log out. */
@Composable
fun ProfileScreen(modifier: Modifier = Modifier) {
    val store = LocalStudyStore.current
    val nav = LocalAppNavigator.current
    val colors = MaterialTheme.colorScheme
    val profile = store.profile
    val auth = LocalAuthRepository.current
    val scope = rememberCoroutineScope()
    val sync = LocalSyncEngine.current
    val signedIn = auth?.currentUser != null

    TabContainer(modifier) {
        Text("Profile", style = MaterialTheme.typography.headlineSmall, color = colors.onBackground)
        Spacer(Modifier.height(20.dp))

        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(84.dp).clip(CircleShape).background(colors.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    profile.name.firstOrNull()?.uppercase() ?: "S",
                    style = MaterialTheme.typography.headlineMedium,
                    color = colors.onPrimaryContainer,
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(profile.name, style = MaterialTheme.typography.titleLarge, color = colors.onBackground)
            if (profile.email.isNotBlank()) {
                Text(profile.email, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(24.dp))

        SectionHeader("Account")
        Spacer(Modifier.height(8.dp))
        StudyZenCard(contentPadding = 8) {
            SettingsRow(AppIcon.User, "Edit profile", onClick = { nav.navigate(Screen.EditProfile) })
            SettingsRow(AppIcon.Tune, "Personalization", subtitle = "Interests, study days and times", onClick = { nav.navigate(Screen.Personalization(true)) })
            SettingsRow(AppIcon.Book, "Study library", subtitle = "All Studies, including archived", onClick = { nav.navigate(Screen.StudyLibrary) })
        }

        if (signedIn && sync != null) {
            Spacer(Modifier.height(20.dp))
            SectionHeader("Backup and sync")
            Spacer(Modifier.height(8.dp))
            StudyZenCard(contentPadding = 8) {
                SettingsRow(
                    AppIcon.Globe,
                    "Sync now",
                    subtitle = syncStatusText(sync.status, sync.pendingChanges),
                    onClick = { scope.launch { sync.sync(force = true) } },
                )
            }
            Text(
                "Your Studies, plans and notes are backed up to your account, so you can log in on another phone and find them there. StudyZen still works without internet.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, start = 4.dp),
            )
        }

        Spacer(Modifier.height(20.dp))
        SectionHeader("Reminders and notifications")
        Spacer(Modifier.height(8.dp))
        StudyZenCard(contentPadding = 8) {
            SettingsRow(AppIcon.Bell, "Reminders", subtitle = "Upcoming session reminders", onClick = { nav.navigate(Screen.Reminders) })
            SettingsRow(AppIcon.Tune, "Notification settings", subtitle = "Sounds, vibration, quiet hours", onClick = { nav.navigate(Screen.NotificationSettings) })
        }

        Spacer(Modifier.height(20.dp))
        SectionHeader("Security")
        Spacer(Modifier.height(8.dp))
        StudyZenCard(contentPadding = 8) {
            SettingsRow(AppIcon.Shield, "Security", subtitle = "Password and account", onClick = { nav.navigate(Screen.Security) })
        }

        if (!signedIn) {
            // Sample data replaces everything on the phone, so it is only offered when nothing is backed up.
            Spacer(Modifier.height(20.dp))
            SectionHeader("Sample data")
            Spacer(Modifier.height(8.dp))
            StudyZenCard {
                SwitchRow(
                    title = "Show sample data",
                    subtitle = "Fills the app with example Studies and sessions. Turn off to start empty.",
                    checked = store.demoDataLoaded,
                    onCheckedChange = { on -> if (on) store.loadDemoData() else store.clearAll() },
                )
            }
        }

        Spacer(Modifier.height(20.dp))
        StudyZenCard(contentPadding = 8) {
            SettingsRow(
                icon = AppIcon.Logout,
                title = if (signedIn) "Log out" else "Sign in or create an account",
                onClick = {
                    if (!signedIn) {
                        // a guest keeps their work on this device and can sign in to back it up
                        GuestMode.enabled = false
                        nav.replaceAll(Screen.Login)
                    } else {
                        scope.launch {
                            // send this phone's latest changes first; when they are safely on the server,
                            // remove the data from the phone so the next person to log in starts clean
                            val saved = sync?.finalSync() ?: true
                            auth?.logout()
                            if (saved) sync?.forgetLocalData()
                            nav.replaceAll(Screen.Login)
                        }
                    }
                },
                iconTint = if (signedIn) colors.error else colors.primary,
                titleColor = if (signedIn) colors.error else colors.primary,
                trailing = null,
            )
        }
    }
}

private fun syncStatusText(status: SyncStatus, pending: Int): String {
    val waiting = if (pending > 0) " · $pending ${if (pending == 1) "change" else "changes"} waiting" else ""
    return when (status) {
        SyncStatus.Idle -> "Tap to sync$waiting"
        SyncStatus.Syncing -> "Syncing..."
        is SyncStatus.Synced -> {
            val minute = (((status.atMillis + utcOffsetMinutes() * 60_000L).mod(86_400_000L)) / 60_000L).toInt()
            "Up to date · last synced ${formatClock(minute)}"
        }
        is SyncStatus.Failed -> status.message + waiting
    }
}
