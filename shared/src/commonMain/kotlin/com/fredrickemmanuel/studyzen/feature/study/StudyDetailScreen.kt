package com.fredrickemmanuel.studyzen.feature.study

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fredrickemmanuel.studyzen.core.designsystem.components.AppIcon
import com.fredrickemmanuel.studyzen.core.designsystem.components.EmptyState
import com.fredrickemmanuel.studyzen.core.designsystem.components.ErrorState
import com.fredrickemmanuel.studyzen.core.designsystem.components.ProgressRing
import com.fredrickemmanuel.studyzen.core.designsystem.components.RoundIconButton
import com.fredrickemmanuel.studyzen.core.designsystem.components.ScreenScaffold
import com.fredrickemmanuel.studyzen.core.designsystem.components.SectionHeader
import com.fredrickemmanuel.studyzen.core.designsystem.components.StudyZenButton
import com.fredrickemmanuel.studyzen.core.designsystem.components.StudyZenCard
import com.fredrickemmanuel.studyzen.core.designsystem.components.StudyZenIcon
import com.fredrickemmanuel.studyzen.core.designsystem.components.SubjectIconTile
import com.fredrickemmanuel.studyzen.core.designsystem.components.TagChip
import com.fredrickemmanuel.studyzen.core.designsystem.components.style
import com.fredrickemmanuel.studyzen.core.designsystem.components.PillTabs
import com.fredrickemmanuel.studyzen.core.designsystem.components.StudyZenTextButton
import com.fredrickemmanuel.studyzen.core.designsystem.components.SwitchRow
import com.fredrickemmanuel.studyzen.core.time.CivilDate
import com.fredrickemmanuel.studyzen.core.time.todayEpochDay
import com.fredrickemmanuel.studyzen.data.LocalStudyStore
import com.fredrickemmanuel.studyzen.domain.SessionStatus
import com.fredrickemmanuel.studyzen.domain.Study
import com.fredrickemmanuel.studyzen.navigation.LocalAppNavigator
import com.fredrickemmanuel.studyzen.navigation.Screen
import com.fredrickemmanuel.studyzen.navigation.StudyTool

/** The Study Space: where the learner is, the plan behind it, every tool, and everything done so far. */
@Composable
fun StudyDetailScreen(studyId: String, modifier: Modifier = Modifier) {
    val store = LocalStudyStore.current
    val nav = LocalAppNavigator.current
    val colors = MaterialTheme.colorScheme
    val study = store.study(studyId)

    if (study == null) {
        ScreenScaffold("Study", onBack = { nav.back() }, modifier = modifier) {
            ErrorState(message = "This Study could not be found. It may have been deleted.", onRetry = { nav.back() }, title = "Study not found")
        }
        return
    }

    var menuOpen by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var tab by remember { mutableStateOf(0) }
    val current = study.currentLesson
    val today = todayEpochDay()

    ScreenScaffold(
        title = study.title,
        onBack = { nav.back() },
        modifier = modifier,
        trailing = {
            Box {
                RoundIconButton(AppIcon.More, "More options", onClick = { menuOpen = true })
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(text = { Text("Edit") }, onClick = { menuOpen = false; nav.navigate(Screen.StudyEditor(study.id)) })
                    DropdownMenuItem(
                        text = { Text(if (study.archived) "Restore" else "Archive") },
                        onClick = { menuOpen = false; store.archiveStudy(study.id, !study.archived) },
                    )
                    DropdownMenuItem(text = { Text("Delete") }, onClick = { menuOpen = false; confirmDelete = true })
                }
            }
        },
    ) {
        StudyZenCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SubjectIconTile(study.subject, size = 52.dp)
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text("${study.purpose.label} · ${study.level.label}", style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant)
                    val about = study.objective.ifBlank { study.description }
                    if (about.isNotBlank()) {
                        Text(about, style = MaterialTheme.typography.bodyMedium, color = colors.onSurface, maxLines = 3, overflow = TextOverflow.Ellipsis)
                    }
                }
                ProgressRing(study.progress, size = 52.dp, color = study.subject.style().color)
            }
            val deadline = study.deadlineEpochDay
            if (deadline != null) {
                Spacer(Modifier.height(8.dp))
                val left = deadline - today
                TagChip(
                    when {
                        left > 1 -> "$left days to ${study.purpose.deadlineLabel?.lowercase() ?: "go"}"
                        left == 1L -> "Tomorrow"
                        left == 0L -> "Today"
                        else -> "Date passed"
                    },
                )
            }
            if (study.archived) {
                Spacer(Modifier.height(8.dp))
                TagChip("Archived")
            }
        }

        Spacer(Modifier.height(16.dp))
        if (current != null) {
            StudyZenButton(
                text = if (study.lastLessonId != null) "Continue: ${current.title}" else "Start: ${current.title}",
                onClick = { nav.navigate(Screen.Lesson(study.id, current.id)) },
            )
        } else {
            StudyZenButton("Build a learning path", onClick = { nav.navigate(Screen.LearningPath(study.id)) })
        }

        PlanCard(study)

        Spacer(Modifier.height(20.dp))
        PillTabs(listOf("Path", "Tools", "History"), tab, { tab = it })
        Spacer(Modifier.height(16.dp))
        when (tab) {
            0 -> PathTab(study)
            1 -> ToolsTab(study)
            else -> HistoryTab(study)
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this Study?") },
            text = { Text("Its lessons, notes, research, chats, scans and results will be removed. Sessions stay on your calendar. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    store.deleteStudy(study.id)
                    nav.back()
                }) { Text("Delete", color = colors.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

/** Which plan this study is in (if any), with the reminder switch. A plan is a schedule; the path is the lessons. */
@Composable
private fun PlanCard(study: Study) {
    val store = LocalStudyStore.current
    val nav = LocalAppNavigator.current
    val colors = MaterialTheme.colorScheme
    val plan = store.planFor(study.id)
    Spacer(Modifier.height(12.dp))
    StudyZenCard(onClick = if (plan != null) ({ nav.navigate(Screen.PlanBuilder(plan.id)) }) else null) {
        if (plan == null) {
            Text("Studying at your own pace", style = MaterialTheme.typography.titleSmall, color = colors.onSurface)
            Text("No study plan, so no scheduled sessions or reminders.", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            StudyZenTextButton("Add to a plan", onClick = { nav.navigate(Screen.StudyEditor(study.id)) })
        } else {
            Text("In plan: ${plan.name}", style = MaterialTheme.typography.titleSmall, color = colors.onSurface)
            Text(
                plan.studyDays.sortedBy { it.ordinal }.joinToString(" ") { it.short }.ifEmpty { "No study days yet" },
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
            val upcoming = store.sessionsForStudy(study.id).filter { it.epochDay >= todayEpochDay() && it.status == SessionStatus.Scheduled }.minByOrNull { it.epochDay * 1440 + it.startMinute }
            if (upcoming != null) {
                Text(
                    "Next: ${CivilDate.fromEpochDay(upcoming.epochDay).shortLabel()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.primary,
                )
            }
            SwitchRow(
                title = "Reminders",
                subtitle = "Before each scheduled session",
                checked = plan.reminder.enabled,
                onCheckedChange = { store.setPlanReminder(plan.id, plan.reminder.copy(enabled = it)) },
            )
        }
    }
}

@Composable
private fun PathTab(study: Study) {
    val nav = LocalAppNavigator.current
    val colors = MaterialTheme.colorScheme
    if (study.modules.isEmpty()) {
        StudyZenCard {
            Text("No lessons yet", style = MaterialTheme.typography.titleSmall, color = colors.onSurface)
            Text(
                "Build a learning path to get modules and lessons for this topic, or just use the tools and notes.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
            )
            StudyZenTextButton("Build a learning path", onClick = { nav.navigate(Screen.LearningPath(study.id)) })
        }
        return
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            "${study.completedLessons} of ${study.allLessons.size} lessons done" + if (study.guided) " · in order" else "",
            style = MaterialTheme.typography.labelLarge,
            color = colors.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        StudyZenTextButton("Full path", onClick = { nav.navigate(Screen.LearningPath(study.id)) })
    }
    val current = study.currentLesson
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        study.modules.forEach { module ->
            StudyZenCard {
                Text(module.title, style = MaterialTheme.typography.titleSmall, color = colors.onSurface)
                Text(
                    "${module.lessons.count { it.completed }}/${module.lessons.size} done",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
                if (module.lessons.any { it.id == current?.id }) {
                    module.lessons.forEach { lesson ->
                        LessonRow(study, lesson.id)
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolsTab(study: Study) {
    val nav = LocalAppNavigator.current
    val colors = MaterialTheme.colorScheme
    val lesson = study.currentLesson
    if (lesson != null) {
        Text("Tools use \"${lesson.title}\" as context where they can.", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
    }
    val tools: List<Triple<AppIcon, String, () -> Unit>> = listOf(
        Triple(AppIcon.Sparkle, "Ask AI & scan", { nav.navigate(Screen.ToolHistory(study.id, StudyTool.Chat, lesson?.id)) }),
        Triple(AppIcon.Search, "Research", { nav.navigate(Screen.ToolHistory(study.id, StudyTool.Research, lesson?.id)) }),
        Triple(AppIcon.Note, "Notes", { nav.navigate(Screen.ToolHistory(study.id, StudyTool.Notes, lesson?.id)) }),
        Triple(AppIcon.Quiz, "Practice and tests", { nav.navigate(Screen.QuizSetup(study.id, lesson?.id)) }),
        Triple(AppIcon.Calculator, "Calculator", { nav.navigate(Screen.ToolHistory(study.id, StudyTool.Calculator, lesson?.id)) }),
        Triple(AppIcon.Bulb, "Dictionary", { nav.navigate(Screen.ToolHistory(study.id, StudyTool.Dictionary, lesson?.id)) }),
        Triple(AppIcon.Book, "Learning path", { nav.navigate(Screen.LearningPath(study.id)) }),
    )
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        tools.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { (icon, label, action) ->
                    StudyZenCard(modifier = Modifier.weight(1f), onClick = action) {
                        StudyZenIcon(icon, tint = colors.primary)
                        Spacer(Modifier.height(8.dp))
                        Text(label, style = MaterialTheme.typography.titleSmall, color = colors.onSurface)
                    }
                }
            }
        }
    }
}

/** Everything done inside this study, newest first, grouped by kind. Nothing here comes from another study. */
@Composable
private fun HistoryTab(study: Study) {
    val store = LocalStudyStore.current
    val nav = LocalAppNavigator.current
    val colors = MaterialTheme.colorScheme

    val notes = store.notesFor(study.id)
    val assessments = store.assessmentsFor(study.id)
    val scans = store.scansFor(study.id)
    val research = store.research.filter { it.studyId == study.id }
    val bookmarks = store.bookmarksFor(study.id)
    val log = store.logFor(study.id)
    val results = store.resultsFor(study.id)

    val nothing = notes.isEmpty() && assessments.isEmpty() && scans.isEmpty() && research.isEmpty() &&
        bookmarks.isEmpty() && log.isEmpty() && results.isEmpty()
    if (nothing) {
        EmptyState(
            title = "Nothing here yet",
            message = "Notes, AI chats, searches, scans, tests and bookmarks you make in this study will be kept here.",
        )
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        if (assessments.isNotEmpty() || results.isNotEmpty()) {
            Column {
                SectionHeader("Tests and practice")
                Spacer(Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    assessments.take(5).forEach { a ->
                        StudyZenCard(onClick = { nav.navigate(if (a.submitted) Screen.QuizResult(study.id, a.id) else Screen.Quiz(study.id, a.id)) }) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(a.title, style = MaterialTheme.typography.titleSmall, color = colors.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(
                                        "${a.kind.label} · ${CivilDate.fromEpochDay(a.createdAtEpochDay).shortLabel()}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.onSurfaceVariant,
                                    )
                                }
                                if (!a.submitted) TagChip("In progress")
                                else if (a.questions.any { it.isObjective }) Text("${a.correct}/${a.total}", style = MaterialTheme.typography.titleMedium, color = colors.primary)
                                else TagChip("Feedback")
                            }
                        }
                    }
                }
            }
        }
        if (bookmarks.isNotEmpty()) {
            Column {
                SectionHeader("Bookmarked resources")
                Spacer(Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    bookmarks.take(8).forEach { (lesson, r) ->
                        StudyZenCard(onClick = { nav.navigate(Screen.Lesson(study.id, lesson.id)) }) {
                            Text(r.title, style = MaterialTheme.typography.titleSmall, color = colors.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text("From: ${lesson.title}", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                        }
                    }
                }
            }
        }
        if (notes.isNotEmpty()) {
            Column {
                SectionHeader("Notes", actionLabel = "New", onAction = { nav.navigate(Screen.NoteEditor(study.id, null)) })
                Spacer(Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    notes.take(5).forEach { n ->
                        StudyZenCard(onClick = { nav.navigate(Screen.NoteEditor(study.id, n.id)) }) {
                            Text(n.title, style = MaterialTheme.typography.titleSmall, color = colors.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(n.body, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
        if (scans.isNotEmpty()) {
            Column {
                SectionHeader("Ask AI & scan")
                Spacer(Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    scans.take(5).forEach { sc ->
                        StudyZenCard(onClick = { nav.navigate(Screen.Scanner(study.id, sc.id)) }) {
                            Text(sc.title, style = MaterialTheme.typography.titleSmall, color = colors.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                "${sc.turns.count { it.fromUser }} message${if (sc.turns.count { it.fromUser } == 1) "" else "s"} · ${CivilDate.fromEpochDay(sc.updatedAtEpochDay).shortLabel()}",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
        if (research.isNotEmpty()) {
            Column {
                SectionHeader("Searches")
                Spacer(Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    research.take(5).forEach { r ->
                        StudyZenCard(onClick = { nav.navigate(Screen.Research(study.id, r.id)) }) {
                            Text(r.question, style = MaterialTheme.typography.titleSmall, color = colors.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text("${r.findings.size} result${if (r.findings.size == 1) "" else "s"}", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                        }
                    }
                }
            }
        }
        if (log.isNotEmpty()) {
            Column {
                SectionHeader("Dictionary and calculator")
                Spacer(Modifier.height(8.dp))
                StudyZenCard {
                    log.take(8).forEach { e ->
                        Row(Modifier.padding(vertical = 3.dp)) {
                            TagChip(e.kind.label)
                            Text(
                                e.text,
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onSurface,
                                modifier = Modifier.padding(start = 8.dp).weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** One lesson line used by the Path tab. Shows done, locked and current states. */
@Composable
internal fun LessonRow(study: Study, lessonId: String) {
    val nav = LocalAppNavigator.current
    val colors = MaterialTheme.colorScheme
    val lesson = study.lesson(lessonId) ?: return
    val open = study.canOpen(lessonId)
    val isCurrent = study.currentLesson?.id == lessonId
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = open) { nav.navigate(Screen.Lesson(study.id, lessonId)) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            when {
                lesson.completed -> "✓"
                !open -> "🔒"
                isCurrent -> "▶"
                else -> "○"
            },
            style = MaterialTheme.typography.titleSmall,
            color = if (lesson.completed || isCurrent) colors.primary else colors.onSurfaceVariant,
            modifier = Modifier.padding(end = 12.dp),
        )
        Column(Modifier.weight(1f)) {
            Text(
                lesson.title,
                style = MaterialTheme.typography.bodyMedium,
                color = if (open) colors.onSurface else colors.onSurfaceVariant,
            )
            if (lesson.summary.isNotBlank()) {
                Text(lesson.summary, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        if (lesson.kind != com.fredrickemmanuel.studyzen.domain.LessonKind.Learn) TagChip(lesson.kind.label)
    }
}
