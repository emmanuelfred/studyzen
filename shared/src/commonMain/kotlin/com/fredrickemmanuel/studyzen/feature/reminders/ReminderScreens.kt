package com.fredrickemmanuel.studyzen.feature.reminders

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fredrickemmanuel.studyzen.core.designsystem.components.AppIcon
import com.fredrickemmanuel.studyzen.core.designsystem.components.BannerTone
import com.fredrickemmanuel.studyzen.core.designsystem.components.EmptyState
import com.fredrickemmanuel.studyzen.core.designsystem.components.FilterPill
import com.fredrickemmanuel.studyzen.core.designsystem.components.InfoBanner
import com.fredrickemmanuel.studyzen.core.designsystem.components.ScreenScaffold
import com.fredrickemmanuel.studyzen.core.designsystem.components.SectionHeader
import com.fredrickemmanuel.studyzen.core.designsystem.components.StudyZenButton
import com.fredrickemmanuel.studyzen.core.designsystem.components.StudyZenCard
import com.fredrickemmanuel.studyzen.core.designsystem.components.StudyZenIcon
import com.fredrickemmanuel.studyzen.core.designsystem.components.StudyZenTextButton
import com.fredrickemmanuel.studyzen.core.designsystem.components.SwitchRow
import com.fredrickemmanuel.studyzen.core.designsystem.components.TimeStepper
import com.fredrickemmanuel.studyzen.core.time.CivilDate
import com.fredrickemmanuel.studyzen.core.time.formatClock
import com.fredrickemmanuel.studyzen.core.time.nowMinuteOfDay
import com.fredrickemmanuel.studyzen.core.time.todayEpochDay
import com.fredrickemmanuel.studyzen.data.LocalStudyStore
import com.fredrickemmanuel.studyzen.domain.ReminderPlanner
import com.fredrickemmanuel.studyzen.domain.SoundOptions
import com.fredrickemmanuel.studyzen.navigation.LocalAppNavigator
import com.fredrickemmanuel.studyzen.navigation.Screen

/** Tells the learner whether alarms can ring, and fixes it with one tap. Shown on both settings screens. */
@Composable
internal fun ReminderStatusCard() {
    val platform = LocalReminderPlatform.current
    val status = platform.status
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        when {
            !status.supported -> InfoBanner(
                "Alarms that ring on their own are available on Android for now. Your reminders are still saved with your sessions.",
            )
            !status.notificationsAllowed -> {
                InfoBanner("Notifications are off, so reminders can't ring.", tone = BannerTone.Warning)
                StudyZenButton("Allow notifications", onClick = {
                    platform.requestNotificationPermission { granted -> if (!granted) platform.openNotificationSettings() }
                })
            }
            !status.exactAlarmsAllowed -> {
                InfoBanner(
                    "Alarms may arrive a few minutes late. Allow \"Alarms & reminders\" for StudyZen to ring on time.",
                    tone = BannerTone.Warning,
                )
                StudyZenButton("Allow exact alarms", onClick = platform::openExactAlarmSettings)
            }
            else -> {
                InfoBanner("Reminders are on. They are saved on this phone and ring even when you are offline.")
                StudyZenTextButton("Send a test reminder (rings in 10 seconds)", onClick = platform::sendTestReminder)
                Text(
                    "Reminders ring like an alarm clock (they show as your next alarm) and stop after a minute or when you tap Stop. If the test doesn't ring: turn the phone's sound and Do Not Disturb exceptions for alarms on, and in the phone's settings for StudyZen allow autostart and set battery to Unrestricted (Tecno, Infinix, Xiaomi, Oppo and Samsung phones often put apps to sleep).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Explains why the app asks for notifications, then asks. */
@Composable
fun NotificationPermissionScreen() {
    val nav = LocalAppNavigator.current
    val platform = LocalReminderPlatform.current
    val colors = MaterialTheme.colorScheme

    ScreenScaffold(
        title = "Stay on track",
        onBack = nav::back,
        bottomBar = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                StudyZenButton("Allow notifications", onClick = {
                    platform.requestNotificationPermission { granted ->
                        if (!granted) platform.openNotificationSettings()
                        nav.back()
                    }
                })
                StudyZenTextButton("Not now", onClick = nav::back, modifier = Modifier.fillMaxWidth())
            }
        },
    ) {
        Spacer(Modifier.height(24.dp))
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            StudyZenIcon(AppIcon.Bell, size = 56.dp, tint = colors.primary)
            Spacer(Modifier.height(16.dp))
            Text("Get a nudge before each session", style = MaterialTheme.typography.headlineSmall, color = colors.onBackground)
            Spacer(Modifier.height(8.dp))
            Text(
                "StudyZen rings a reminder before your sessions. The alarms are saved on your phone, so they still ring when you have no internet.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(20.dp))
        ReminderStatusCard()
    }
}

/** Every reminder that will ring soon, with a switch for all reminders. */
@Composable
fun RemindersScreen() {
    val store = LocalStudyStore.current
    val nav = LocalAppNavigator.current
    val colors = MaterialTheme.colorScheme
    val profile = store.profile

    val today = todayEpochDay()
    val nowLocalMinute = today * 1440L + nowMinuteOfDay()
    val upcoming = ReminderPlanner.plan(store.sessions.toList(), profile, today)
        .filter { it.localEpochMinute >= nowLocalMinute }

    ScreenScaffold(title = "Reminders", onBack = nav::back) {
        ReminderStatusCard()
        Spacer(Modifier.height(16.dp))
        SwitchRow(
            title = "Session reminders",
            subtitle = "Turn all reminders on or off.",
            checked = profile.remindersEnabled,
            onCheckedChange = { store.profile = store.profile.copy(remindersEnabled = it) },
        )
        StudyZenTextButton("Sounds, vibration and quiet hours", onClick = { nav.navigate(Screen.NotificationSettings) })

        Spacer(Modifier.height(16.dp))
        SectionHeader("Coming up")
        Spacer(Modifier.height(8.dp))
        if (upcoming.isEmpty()) {
            EmptyState(
                title = "No reminders coming up",
                message = if (profile.remindersEnabled) {
                    "Add a session or a study plan with reminders turned on and it will appear here."
                } else {
                    "Reminders are turned off."
                },
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                upcoming.take(30).forEach { alarm ->
                    StudyZenCard(onClick = { nav.navigate(Screen.SessionEditor(alarm.sessionId, alarm.epochDay)) }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(alarm.title, style = MaterialTheme.typography.titleSmall, color = colors.onSurface)
                                Text(
                                    "Rings ${CivilDate.fromEpochDay(alarm.epochDay).shortLabel()} at ${formatClock(alarm.minuteOfDay)}" +
                                        if (alarm.silent) " (silent)" else "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.primary,
                                )
                                Text(alarm.body, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                            }
                            StudyZenIcon(AppIcon.ChevronRight, size = 20.dp, tint = colors.onSurfaceVariant)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

/** Sound, vibration, quiet hours and which kinds of notifications to receive. */
@Composable
fun NotificationSettingsScreen() {
    val store = LocalStudyStore.current
    val nav = LocalAppNavigator.current
    val colors = MaterialTheme.colorScheme
    val p = store.profile

    ScreenScaffold(title = "Notification settings", onBack = nav::back) {
        ReminderStatusCard()

        Spacer(Modifier.height(20.dp))
        SectionHeader("What to send")
        Spacer(Modifier.height(8.dp))
        StudyZenCard {
            SwitchRow(
                title = "Session reminders",
                checked = p.remindersEnabled,
                onCheckedChange = { store.profile = store.profile.copy(remindersEnabled = it) },
            )
            SwitchRow(
                title = "Schedule changes",
                subtitle = "A heads-up when your plan or a session changes.",
                checked = p.scheduleChangeAlerts,
                onCheckedChange = { store.profile = store.profile.copy(scheduleChangeAlerts = it) },
            )
            SwitchRow(
                title = "Learning suggestions",
                subtitle = "Occasional tips on what to study next.",
                checked = p.learningSuggestions,
                onCheckedChange = { store.profile = store.profile.copy(learningSuggestions = it) },
            )
        }

        Spacer(Modifier.height(20.dp))
        SectionHeader("Sound and vibration")
        Spacer(Modifier.height(8.dp))
        StudyZenCard {
            Text("Default for new reminders", style = MaterialTheme.typography.labelLarge, color = colors.onSurface)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SoundOptions.forEach {
                    FilterPill(it, p.defaultSound == it, { store.profile = store.profile.copy(defaultSound = it) })
                }
            }
            Text(
                "Reminders use your phone's notification sound. Choose Silent to get them without sound.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp),
            )
            SwitchRow(
                title = "Vibrate",
                checked = p.vibrate,
                onCheckedChange = { store.profile = store.profile.copy(vibrate = it) },
            )
        }

        Spacer(Modifier.height(20.dp))
        SectionHeader("Quiet hours")
        Spacer(Modifier.height(8.dp))
        StudyZenCard {
            SwitchRow(
                title = "Quiet hours",
                subtitle = "Reminders in this window arrive silently.",
                checked = p.quietHoursEnabled,
                onCheckedChange = { store.profile = store.profile.copy(quietHoursEnabled = it) },
            )
            if (p.quietHoursEnabled) {
                Spacer(Modifier.height(8.dp))
                TimeStepper("From", p.quietStartMinute, { store.profile = store.profile.copy(quietStartMinute = it) })
                Spacer(Modifier.height(8.dp))
                TimeStepper("Until", p.quietEndMinute, { store.profile = store.profile.copy(quietEndMinute = it) })
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}
