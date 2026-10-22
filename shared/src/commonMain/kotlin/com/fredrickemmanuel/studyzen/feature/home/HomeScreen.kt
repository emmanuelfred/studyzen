package com.fredrickemmanuel.studyzen.feature.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fredrickemmanuel.studyzen.core.designsystem.components.AppIcon
import com.fredrickemmanuel.studyzen.core.designsystem.components.EmptyState
import com.fredrickemmanuel.studyzen.core.designsystem.components.ProgressRing
import com.fredrickemmanuel.studyzen.core.designsystem.components.RoundIconButton
import com.fredrickemmanuel.studyzen.core.designsystem.components.SectionHeader
import com.fredrickemmanuel.studyzen.core.designsystem.components.SoftProgressBar
import com.fredrickemmanuel.studyzen.core.designsystem.components.StudyZenButton
import com.fredrickemmanuel.studyzen.core.designsystem.components.StudyZenCard
import com.fredrickemmanuel.studyzen.core.designsystem.components.StudyZenIcon
import com.fredrickemmanuel.studyzen.core.designsystem.components.StudyZenTextButton
import com.fredrickemmanuel.studyzen.core.designsystem.components.SubjectIconTile
import com.fredrickemmanuel.studyzen.core.designsystem.components.TagChip
import com.fredrickemmanuel.studyzen.core.designsystem.components.style
import com.fredrickemmanuel.studyzen.core.designsystem.theme.StudyZenGradients
import com.fredrickemmanuel.studyzen.core.designsystem.theme.StudyZenShape
import com.fredrickemmanuel.studyzen.core.designsystem.theme.StudyZenTheme
import com.fredrickemmanuel.studyzen.core.designsystem.theme.softShadow
import com.fredrickemmanuel.studyzen.core.time.formatClock
import com.fredrickemmanuel.studyzen.core.time.formatMinutes
import com.fredrickemmanuel.studyzen.core.time.nowMinuteOfDay
import com.fredrickemmanuel.studyzen.core.time.todayEpochDay
import com.fredrickemmanuel.studyzen.data.LocalStudyStore
import com.fredrickemmanuel.studyzen.domain.SessionStatus
import com.fredrickemmanuel.studyzen.domain.Stats
import com.fredrickemmanuel.studyzen.domain.Study
import com.fredrickemmanuel.studyzen.feature.main.TabContainer
import com.fredrickemmanuel.studyzen.navigation.LocalAppNavigator
import com.fredrickemmanuel.studyzen.navigation.Screen
import org.jetbrains.compose.resources.painterResource
import studyzen.shared.generated.resources.Res
import studyzen.shared.generated.resources.mascot_head

/**
 * Daily overview, following the middle screen of the design reference: greeting, the featured
 * Study with the next action, Today's Plan, a small progress summary and an AI tip.
 */
@Composable
fun HomeScreen(
    onSeeAllPlan: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val store = LocalStudyStore.current
    val nav = LocalAppNavigator.current
    val colors = MaterialTheme.colorScheme

    val today = todayEpochDay()
    val nowMinute = nowMinuteOfDay()
    val profile = store.profile
    val todays = store.sessionsOn(today)
    val next = store.nextSession(today, nowMinute)
    val run = store.activeRun
    val featured: Study? = store.study(next?.studyId) ?: store.activeStudies().firstOrNull()
    val isNewUser = store.studies.isEmpty() && store.sessions.isEmpty()

    TabContainer(modifier) {
        // --- Greeting ---
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(colors.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = profile.name.firstOrNull()?.uppercase() ?: "S",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onPrimaryContainer,
                )
            }
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(Stats.greeting(nowMinute), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                Text(profile.name, style = MaterialTheme.typography.titleLarge, color = colors.onBackground)
            }
            RoundIconButton(AppIcon.Bell, "Reminders", onClick = { nav.navigate(Screen.Reminders) })
        }
        Spacer(Modifier.height(20.dp))

        if (isNewUser) {
            EmptyState(
                title = "Welcome to StudyZen",
                message = "Create your first Study or plan your first study sessions to get started.",
                actionLabel = "Create your first Study",
                onAction = { nav.navigate(Screen.StudyEditor(null)) },
            )
            StudyZenTextButton(
                text = "Plan study sessions",
                onClick = { nav.navigate(Screen.PlanBuilder(null)) },
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            return@TabContainer
        }

        // --- Featured Study ---
        if (featured != null) {
            val doneToday = todays.count { it.status == SessionStatus.Completed }
            FeaturedStudyCard(
                study = featured,
                focusLine = if (todays.isEmpty()) {
                    "No sessions planned for today"
                } else {
                    "Today's Focus: $doneToday of ${todays.size} Sessions Complete"
                },
                upNextLine = next?.let { "Up next: ${formatClock(it.startMinute)} · ${formatMinutes(it.durationMinutes)}" },
                progress = if (todays.isEmpty()) 0f else doneToday.toFloat() / todays.size,
                actionLabel = if (run != null) "Continue session" else "Start studying",
                onAction = {
                    if (run != null) nav.navigate(Screen.StudySession(run.sessionId)) else nav.navigate(Screen.StartStudying)
                },
            )
            Spacer(Modifier.height(24.dp))
        }

        // --- Today's plan ---
        SectionHeader("Today's Plan", actionLabel = "See All", onAction = onSeeAllPlan)
        Spacer(Modifier.height(8.dp))
        if (todays.isEmpty()) {
            StudyZenCard {
                Text("Nothing planned for today", style = MaterialTheme.typography.titleSmall, color = colors.onSurface)
                Text(
                    "Add a session so StudyZen can remind you.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp, bottom = 12.dp),
                )
                StudyZenButton(text = "Plan a session", onClick = { nav.navigate(Screen.SessionEditor(null, today)) })
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                todays.forEach { session ->
                    val study = store.study(session.studyId)
                    val style = session.subject.style()
                    StudyZenCard(
                        onClick = {
                            if (session.status == SessionStatus.Completed) {
                                nav.navigate(Screen.SessionSummary(session.id))
                            } else {
                                nav.navigate(Screen.StudySession(session.id))
                            }
                        },
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SubjectIconTile(session.subject)
                            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                                Text(
                                    session.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = colors.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        "${formatClock(session.startMinute)} · ${formatMinutes(session.durationMinutes)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.onSurfaceVariant,
                                    )
                                    TagChip(session.kind.label)
                                }
                            }
                            when (session.status) {
                                SessionStatus.Completed -> TagChip("Done", container = Color(0xFFDDF6EC), content = Color(0xFF0E5A42))
                                SessionStatus.Skipped -> TagChip("Skipped", container = colors.surfaceVariant, content = colors.onSurfaceVariant)
                                SessionStatus.Scheduled -> ProgressRing(
                                    progress = study?.progress ?: 0f,
                                    color = style.color,
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- Upcoming reminder ---
        val reminder = next?.reminder
        if (profile.remindersEnabled && next != null && reminder != null && reminder.enabled && next.epochDay == today) {
            Spacer(Modifier.height(16.dp))
            StudyZenCard(onClick = { nav.navigate(Screen.Reminders) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StudyZenIcon(AppIcon.Bell, tint = colors.primary)
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        Text(
                            "Reminder at ${formatClock(next.startMinute - reminder.minutesBefore)}",
                            style = MaterialTheme.typography.titleSmall,
                            color = colors.onSurface,
                        )
                        Text(
                            "Time to study ${next.title}",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        // --- This week summary (only recorded data) ---
        Spacer(Modifier.height(24.dp))
        val week = Stats.daysIn(Stats.Range.Week, today, null)
        val weekMinutes = Stats.totalMinutes(store.sessions, week)
        val weekSessions = Stats.completed(store.sessions, week).size
        val streak = Stats.streak(store.sessions, today)
        StudyZenCard {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SummaryStat(if (weekMinutes == 0) "0m" else formatMinutes(weekMinutes), "This week")
                SummaryStat("$weekSessions", "Sessions")
                SummaryStat(if (streak == 1) "1 day" else "$streak days", "Streak")
            }
        }

        // --- Your Studies ---
        val active = store.activeStudies()
        if (active.isNotEmpty()) {
            Spacer(Modifier.height(24.dp))
            SectionHeader("Your Studies", actionLabel = "See all", onAction = { nav.navigate(Screen.StudyLibrary) })
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                active.take(6).forEach { study ->
                    StudyZenCard(
                        modifier = Modifier.width(170.dp),
                        onClick = { nav.navigate(Screen.StudyDetail(study.id)) },
                    ) {
                        SubjectIconTile(study.subject, size = 40.dp)
                        Spacer(Modifier.height(10.dp))
                        Text(study.title, style = MaterialTheme.typography.titleSmall, color = colors.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(8.dp))
                        SoftProgressBar(study.progress, color = study.subject.style().color)
                        Text(
                            "${study.completedLessons} of ${study.allLessons.size} lessons",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                }
            }
        }

        // --- AI tip (optional, from the learner's own data) ---
        if (profile.learningSuggestions) {
            Spacer(Modifier.height(24.dp))
            val hour = Stats.mostProductiveHour(store.sessions)
            val tip = if (hour != null) {
                "You have studied most around ${formatClock(hour * 60)}. Plan your next session then."
            } else {
                "Short sessions with a break in between help you remember more."
            }
            StudyZenCard(
                onClick = featured?.let { study -> { nav.navigate(Screen.Assistant(study.id)) } },
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(48.dp).clip(CircleShape).background(colors.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            painter = painterResource(Res.drawable.mascot_head),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.size(34.dp),
                        )
                    }
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Text("StudyZen AI Tip", style = MaterialTheme.typography.titleSmall, color = colors.onSurface)
                        Text(tip, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                    }
                    if (featured != null) {
                        StudyZenIcon(AppIcon.ChevronRight, size = 20.dp, tint = colors.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** The large purple card from the reference: Study title, today's focus, progress and the main action. */
@Composable
private fun FeaturedStudyCard(
    study: Study,
    focusLine: String,
    upNextLine: String?,
    progress: Float,
    actionLabel: String,
    onAction: () -> Unit,
) {
    val onPurple = Color.White
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .softShadow(shape = StudyZenShape.FeaturedCard, elevation = 14.dp)
            .clip(StudyZenShape.FeaturedCard)
            .background(StudyZenGradients.FeaturedCard),
    ) {
        Image(
            painter = painterResource(Res.drawable.mascot_head),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp)
                .size(width = 104.dp, height = 116.dp),
        )
        Column(Modifier.padding(20.dp).fillMaxWidth(0.68f)) {
            Text(
                study.title,
                style = MaterialTheme.typography.titleLarge,
                color = onPurple,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Text(focusLine, style = MaterialTheme.typography.bodySmall, color = onPurple.copy(alpha = 0.85f))
            if (upNextLine != null) {
                Text(upNextLine, style = MaterialTheme.typography.bodySmall, color = onPurple.copy(alpha = 0.85f))
            }
            Spacer(Modifier.height(12.dp))
            SoftProgressBar(progress, color = onPurple, trackColor = onPurple.copy(alpha = 0.30f), height = 6.dp)
            Spacer(Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .clip(StudyZenShape.Pill)
                    .background(onPurple)
                    .clickable(role = Role.Button, onClick = onAction)
                    .padding(horizontal = 20.dp, vertical = 11.dp),
            ) {
                Text(actionLabel, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
