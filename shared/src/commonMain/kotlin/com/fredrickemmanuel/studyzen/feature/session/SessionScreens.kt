package com.fredrickemmanuel.studyzen.feature.session

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fredrickemmanuel.studyzen.core.designsystem.components.BannerTone
import com.fredrickemmanuel.studyzen.core.designsystem.components.EmptyState
import com.fredrickemmanuel.studyzen.core.designsystem.components.ErrorState
import com.fredrickemmanuel.studyzen.core.designsystem.components.FilterPill
import com.fredrickemmanuel.studyzen.core.designsystem.components.InfoBanner
import com.fredrickemmanuel.studyzen.core.designsystem.components.ProgressRing
import com.fredrickemmanuel.studyzen.core.designsystem.components.ScreenScaffold
import com.fredrickemmanuel.studyzen.core.designsystem.components.SectionHeader
import com.fredrickemmanuel.studyzen.core.designsystem.components.SoftProgressBar
import com.fredrickemmanuel.studyzen.core.designsystem.components.StudyZenButton
import com.fredrickemmanuel.studyzen.core.designsystem.components.StudyZenCard
import com.fredrickemmanuel.studyzen.core.designsystem.components.StudyZenTextButton
import com.fredrickemmanuel.studyzen.core.designsystem.components.StudyZenTextField
import com.fredrickemmanuel.studyzen.core.designsystem.components.SubjectIconTile
import com.fredrickemmanuel.studyzen.core.designsystem.components.TagChip
import com.fredrickemmanuel.studyzen.core.designsystem.components.style
import com.fredrickemmanuel.studyzen.core.time.currentTimeMillis
import com.fredrickemmanuel.studyzen.core.time.formatClock
import com.fredrickemmanuel.studyzen.core.time.formatMinutes
import com.fredrickemmanuel.studyzen.core.time.nowMinuteOfDay
import com.fredrickemmanuel.studyzen.core.time.todayEpochDay
import com.fredrickemmanuel.studyzen.data.LocalStudyStore
import com.fredrickemmanuel.studyzen.domain.SessionStatus
import com.fredrickemmanuel.studyzen.domain.Stats
import com.fredrickemmanuel.studyzen.domain.Study
import com.fredrickemmanuel.studyzen.navigation.LocalAppNavigator
import com.fredrickemmanuel.studyzen.navigation.Screen
import com.fredrickemmanuel.studyzen.navigation.StudyTool

// --- Start studying: every open study in one place ----------------------------------------------

/** "What do you want to study?" Today's planned sessions, then every open study with a one-tap timer. */
@Composable
fun StartStudyingScreen(modifier: Modifier = Modifier) {
    val store = LocalStudyStore.current
    val nav = LocalAppNavigator.current
    val colors = MaterialTheme.colorScheme
    val today = todayEpochDay()
    var minutes by remember { mutableStateOf(25) }

    val run = store.activeRun
    val runSession = store.session(run?.sessionId)
    val planned = store.sessionsOn(today).filter { it.status == SessionStatus.Scheduled && it.id != run?.sessionId }
    val studies = store.activeStudies()

    ScreenScaffold(title = "Start studying", onBack = { nav.back() }, modifier = modifier) {
        if (run != null && runSession != null) {
            StudyZenCard(onClick = { nav.navigate(Screen.StudySession(run.sessionId)) }) {
                TagChip(if (run.isRunning) "Running now" else "Paused")
                Text(runSession.title, style = MaterialTheme.typography.titleMedium, color = colors.onSurface, modifier = Modifier.padding(top = 6.dp))
                Text("Tap to continue this session", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
            Spacer(Modifier.height(16.dp))
        }

        if (planned.isNotEmpty()) {
            SectionHeader("Planned for today")
            Spacer(Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                planned.forEach { s ->
                    StudyZenCard(onClick = { nav.navigate(Screen.StudySession(s.id)) }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SubjectIconTile(s.subject)
                            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                                Text(s.title, style = MaterialTheme.typography.titleSmall, color = colors.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${formatClock(s.startMinute)} · ${formatMinutes(s.durationMinutes)}", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                            }
                            TagChip("Start", container = colors.primary, content = colors.onPrimary)
                        }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }

        SectionHeader("Your studies", actionLabel = "New", onAction = { nav.navigate(Screen.StudyEditor(null)) })
        Spacer(Modifier.height(8.dp))
        if (studies.isEmpty()) {
            EmptyState(
                title = "No studies yet",
                message = "Create a study for any topic, then start a session on it.",
                actionLabel = "Create a study",
                onAction = { nav.navigate(Screen.StudyEditor(null)) },
            )
            return@ScreenScaffold
        }
        Text("Timer length", style = MaterialTheme.typography.labelLarge, color = colors.onBackground)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(15, 25, 45, 60, 90).forEach { FilterPill("$it min", minutes == it, { minutes = it }) }
        }
        Spacer(Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            studies.forEach { study ->
                StudyChoice(
                    study = study,
                    onOpen = { nav.navigate(Screen.StudyDetail(study.id)) },
                    onStart = {
                        val session = store.startQuickSession(study, minutes)
                        nav.replaceTop(Screen.StudySession(session.id))
                    },
                )
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun StudyChoice(study: Study, onOpen: () -> Unit, onStart: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    StudyZenCard(onClick = onOpen) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SubjectIconTile(study.subject, size = 44.dp)
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(study.title, style = MaterialTheme.typography.titleSmall, color = colors.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val next = study.currentLesson
                Text(
                    if (next != null) "Next: ${next.title}" else "${study.purpose.label} · ${study.level.label}",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            ProgressRing(study.progress, size = 40.dp, color = study.subject.style().color)
        }
        if (study.allLessons.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            SoftProgressBar(study.progress, color = study.subject.style().color)
            Text(
                "${study.completedLessons} of ${study.allLessons.size} lessons",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StudyZenTextButton("Open study", onClick = onOpen)
            StudyZenTextButton("Start timer", onClick = onStart)
        }
    }
}

// --- The study timer ------------------------------------------------------------------------------

private fun clock(millis: Long): String {
    val total = (millis / 1000).coerceAtLeast(0)
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    fun two(n: Long) = if (n < 10) "0$n" else "$n"
    return if (h > 0) "$h:${two(m)}:${two(s)}" else "${two(m)}:${two(s)}"
}

/** The session in progress: a timer that counts only active time, the study's tools, and a finish button. */
@Composable
fun StudySessionScreen(sessionId: String, modifier: Modifier = Modifier) {
    val store = LocalStudyStore.current
    val nav = LocalAppNavigator.current
    val colors = MaterialTheme.colorScheme
    val session = store.session(sessionId)

    if (session == null) {
        ScreenScaffold("Session", onBack = { nav.back() }, modifier = modifier) {
            ErrorState(message = "This session could not be found. It may have been deleted.", onRetry = { nav.back() }, title = "Session not found")
        }
        return
    }
    if (session.status == SessionStatus.Completed) {
        LaunchedEffect(sessionId) { nav.replaceTop(Screen.SessionSummary(sessionId)) }
        return
    }

    val study = store.study(session.studyId)
    val active = store.activeRun
    val run = active?.takeIf { it.sessionId == sessionId }
    val other = active?.takeIf { it.sessionId != sessionId }

    var now by remember { mutableStateOf(currentTimeMillis()) }
    LaunchedEffect(run?.isRunning) {
        while (run?.isRunning == true) {
            now = currentTimeMillis()
            kotlinx.coroutines.delay(500)
        }
        now = currentTimeMillis()
    }
    val elapsed = run?.elapsedMillis(now) ?: 0L
    val plannedMillis = session.durationMinutes * 60_000L
    val reached = run != null && elapsed >= plannedMillis
    var confirmFinish by remember { mutableStateOf(false) }

    fun finish() {
        val minutes = ((elapsed + 30_000L) / 60_000L).toInt().coerceAtLeast(1)
        store.completeSession(sessionId, minutes)
        store.clearRun()
        nav.replaceTop(Screen.SessionSummary(sessionId))
    }

    ScreenScaffold(title = "Study session", onBack = { nav.back() }, modifier = modifier) {
        if (session.status == SessionStatus.Skipped) {
            InfoBanner("This session was skipped. Starting it again will count it as studied.", tone = BannerTone.Warning)
            Spacer(Modifier.height(12.dp))
        }
        other?.let {
            InfoBanner("Another session is running. Starting this one will stop that timer without saving it.", tone = BannerTone.Warning)
            StudyZenTextButton("Go to the running session", onClick = { nav.replaceTop(Screen.StudySession(it.sessionId)) })
            Spacer(Modifier.height(8.dp))
        }

        Text(session.title, style = MaterialTheme.typography.titleLarge, color = colors.onBackground)
        Text(
            "${session.kind.label} · planned ${formatClock(session.startMinute)} for ${formatMinutes(session.durationMinutes)}",
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))

        StudyZenCard {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(contentAlignment = Alignment.Center) {
                    ProgressRing((elapsed.toFloat() / plannedMillis).coerceIn(0f, 1f), size = 168.dp, strokeWidth = 10.dp)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(clock(elapsed), style = MaterialTheme.typography.displaySmall, color = colors.onSurface, textAlign = TextAlign.Center)
                        Text(
                            when {
                                run == null -> "Not started"
                                run.isRunning -> "Studying"
                                else -> "Paused"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant,
                        )
                    }
                }
                if (reached) {
                    Spacer(Modifier.height(8.dp))
                    Text("Planned time reached. Finish when you are ready, or keep going.", style = MaterialTheme.typography.bodySmall, color = colors.primary, textAlign = TextAlign.Center)
                } else if (run != null) {
                    Spacer(Modifier.height(8.dp))
                    Text("${clock(plannedMillis - elapsed)} left", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                }
            }
        }
        Spacer(Modifier.height(12.dp))

        when {
            run == null -> StudyZenButton("Start timer", onClick = { store.startRun(sessionId) })
            run.isRunning -> StudyZenButton("Pause", onClick = { store.pauseRun() })
            else -> StudyZenButton("Resume", onClick = { store.resumeRun() })
        }
        if (run != null) {
            Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StudyZenTextButton("Finish session", onClick = { if (elapsed < 60_000L) confirmFinish = true else finish() })
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StudyZenTextButton("Skip", onClick = { store.skipSession(sessionId); nav.back() })
                StudyZenTextButton("Tomorrow", onClick = { store.postponeSession(sessionId); nav.back() })
            }
        }

        // what to study
        Spacer(Modifier.height(20.dp))
        if (study != null) {
            val lesson = study.currentLesson
            SectionHeader("Studying")
            Spacer(Modifier.height(8.dp))
            StudyZenCard(onClick = { nav.navigate(Screen.StudyDetail(study.id)) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SubjectIconTile(study.subject, size = 40.dp)
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Text(study.title, style = MaterialTheme.typography.titleSmall, color = colors.onSurface)
                        Text("${study.completedLessons} of ${study.allLessons.size} lessons done", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                    }
                }
            }
            if (lesson != null) {
                Spacer(Modifier.height(8.dp))
                StudyZenButton("Open lesson: ${lesson.title}", onClick = { nav.navigate(Screen.Lesson(study.id, lesson.id)) })
            }
            Spacer(Modifier.height(12.dp))
            Text("Tools", style = MaterialTheme.typography.labelLarge, color = colors.onBackground)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StudyTool.entries.forEach { tool ->
                    FilterPill(tool.label, false, { nav.navigate(Screen.ToolHistory(study.id, tool, lesson?.id)) })
                }
                FilterPill("Practice", false, { nav.navigate(Screen.QuizSetup(study.id, lesson?.id)) })
            }
        } else {
            SectionHeader("Tools")
            Spacer(Modifier.height(8.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterPill("Notes", false, { nav.navigate(Screen.Notes) })
                FilterPill("Scan", false, { nav.navigate(Screen.Scanner(null)) })
                FilterPill("Dictionary", false, { nav.navigate(Screen.Dictionary(null)) })
                FilterPill("Calculator", false, { nav.navigate(Screen.Calculator(null)) })
            }
        }
        Spacer(Modifier.height(16.dp))
    }

    if (confirmFinish) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { confirmFinish = false },
            title = { Text("Finish this early?") },
            text = { Text("You have studied for less than a minute. It will still be saved as a session.") },
            confirmButton = { androidx.compose.material3.TextButton(onClick = { confirmFinish = false; finish() }) { Text("Finish") } },
            dismissButton = { androidx.compose.material3.TextButton(onClick = { confirmFinish = false }) { Text("Keep studying") } },
        )
    }
}

// --- After the session ---------------------------------------------------------------------------

/** What was achieved, an optional reflection, and the next step. */
@Composable
fun SessionSummaryScreen(sessionId: String, modifier: Modifier = Modifier) {
    val store = LocalStudyStore.current
    val nav = LocalAppNavigator.current
    val colors = MaterialTheme.colorScheme
    val session = store.session(sessionId)

    if (session == null) {
        ScreenScaffold("Summary", onBack = { nav.back() }, modifier = modifier) {
            ErrorState(message = "This session could not be found.", onRetry = { nav.back() }, title = "Session not found")
        }
        return
    }

    val study = store.study(session.studyId)
    val today = todayEpochDay()
    var reflection by remember(sessionId) { mutableStateOf(session.reflection) }
    var saved by remember(sessionId) { mutableStateOf(false) }
    val streak = Stats.streak(store.sessions, today)
    val todayMinutes = store.sessionsOn(today).filter { it.status == SessionStatus.Completed }.sumOf { it.actualMinutes }

    ScreenScaffold(title = "Session complete", onBack = { nav.replaceAll(Screen.Main) }, modifier = modifier) {
        StudyZenCard {
            Text(session.title, style = MaterialTheme.typography.titleMedium, color = colors.onSurface)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Stat(formatMinutes(session.actualMinutes), "Studied")
                Stat(formatMinutes(session.durationMinutes), "Planned")
                Stat(if (streak == 1) "1 day" else "$streak days", "Streak")
            }
            Spacer(Modifier.height(8.dp))
            Text("Today in total: ${formatMinutes(todayMinutes)}", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        }

        Spacer(Modifier.height(16.dp))
        StudyZenTextField(
            value = reflection,
            onValueChange = { reflection = it; saved = false },
            label = "How did it go? (optional)",
            placeholder = "What did you learn? What was hard?",
            singleLine = false,
            minLines = 3,
        )
        StudyZenTextButton(if (saved) "Saved" else "Save reflection", enabled = !saved && reflection != session.reflection, onClick = {
            store.saveSession(session.copy(reflection = reflection.trim()))
            saved = true
        })

        if (study != null) {
            Spacer(Modifier.height(16.dp))
            SectionHeader("Next")
            Spacer(Modifier.height(8.dp))
            val next = study.currentLesson
            if (next != null && !next.completed) {
                StudyZenButton("Continue: ${next.title}", onClick = { nav.replaceTop(Screen.Lesson(study.id, next.id)) })
            }
            StudyZenTextButton("Test yourself on ${study.title}", onClick = { nav.navigate(Screen.QuizSetup(study.id, next?.id)) })
            StudyZenTextButton("Open the study", onClick = { nav.navigate(Screen.StudyDetail(study.id)) })
        }
        Spacer(Modifier.height(8.dp))
        StudyZenButton("Done", onClick = { nav.replaceAll(Screen.Main) })
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun Stat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
