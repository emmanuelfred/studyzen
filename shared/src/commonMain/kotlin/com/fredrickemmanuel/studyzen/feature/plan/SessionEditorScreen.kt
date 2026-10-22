package com.fredrickemmanuel.studyzen.feature.plan

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fredrickemmanuel.studyzen.core.Ids
import com.fredrickemmanuel.studyzen.core.designsystem.components.BannerTone
import com.fredrickemmanuel.studyzen.core.designsystem.components.DateStepper
import com.fredrickemmanuel.studyzen.core.designsystem.components.FilterPill
import com.fredrickemmanuel.studyzen.core.designsystem.components.InfoBanner
import com.fredrickemmanuel.studyzen.core.designsystem.components.ScreenScaffold
import com.fredrickemmanuel.studyzen.core.designsystem.components.SectionHeader
import com.fredrickemmanuel.studyzen.core.designsystem.components.Stepper
import com.fredrickemmanuel.studyzen.core.designsystem.components.StudyZenButton
import com.fredrickemmanuel.studyzen.core.designsystem.components.StudyZenTextButton
import com.fredrickemmanuel.studyzen.core.designsystem.components.StudyZenTextField
import com.fredrickemmanuel.studyzen.core.designsystem.components.SwitchRow
import com.fredrickemmanuel.studyzen.core.designsystem.components.TimeStepper
import com.fredrickemmanuel.studyzen.core.time.formatClock
import com.fredrickemmanuel.studyzen.core.time.formatMinutes
import com.fredrickemmanuel.studyzen.data.LocalStudyStore
import com.fredrickemmanuel.studyzen.domain.Priority
import com.fredrickemmanuel.studyzen.domain.Reminder
import com.fredrickemmanuel.studyzen.domain.ScheduleRules
import com.fredrickemmanuel.studyzen.domain.SessionKind
import com.fredrickemmanuel.studyzen.domain.SessionStatus
import com.fredrickemmanuel.studyzen.domain.StudySession
import com.fredrickemmanuel.studyzen.domain.Subject
import com.fredrickemmanuel.studyzen.domain.TimeBlock
import com.fredrickemmanuel.studyzen.navigation.LocalAppNavigator

/** Add or edit a single study session. Refuses a time that overlaps another session. */
@Composable
fun SessionEditorScreen(sessionId: String?, initialEpochDay: Long, modifier: Modifier = Modifier) {
    val store = LocalStudyStore.current
    val nav = LocalAppNavigator.current
    val colors = MaterialTheme.colorScheme
    val existing = store.session(sessionId)

    var title by remember { mutableStateOf(existing?.title ?: "") }
    var studyId by remember { mutableStateOf(existing?.studyId) }
    var kind by remember { mutableStateOf(existing?.kind ?: SessionKind.Study) }
    var priority by remember { mutableStateOf(existing?.priority ?: Priority.Medium) }
    var day by remember { mutableStateOf(existing?.epochDay ?: initialEpochDay) }
    var start by remember { mutableStateOf(existing?.startMinute ?: 18 * 60) }
    var duration by remember { mutableStateOf(existing?.durationMinutes ?: 60) }
    var reminder by remember { mutableStateOf(existing?.reminder ?: Reminder(sound = store.profile.defaultSound, vibrate = store.profile.vibrate)) }
    var assignment by remember { mutableStateOf(existing?.hasAssignment ?: false) }
    var titleError by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }

    val block = TimeBlock(start, minOf(start + duration, 24 * 60))
    val blockProblem = ScheduleRules.blockError(block)
    val clash = ScheduleRules.clash(store.sessions.toList(), day, block, ignoreId = existing?.id)

    fun save() {
        val linked = store.study(studyId)
        val clean = title.trim().ifEmpty { linked?.title.orEmpty() }
        if (clean.isEmpty()) {
            titleError = "Give the session a title or pick a Study"
            return
        }
        if (blockProblem != null || clash != null) return
        store.saveSession(
            (existing ?: StudySession(
                id = Ids.next("ses"), title = clean, studyId = studyId, subject = Subject.Other,
                epochDay = day, startMinute = start, durationMinutes = duration,
            )).copy(
                title = clean,
                studyId = studyId,
                subject = linked?.subject ?: existing?.subject ?: Subject.Other,
                epochDay = day,
                startMinute = start,
                durationMinutes = duration,
                kind = kind,
                priority = priority,
                reminder = reminder.takeIf { it.enabled },
                hasAssignment = assignment,
                // moving a skipped session puts it back on the schedule
                status = if (existing?.status == SessionStatus.Skipped) SessionStatus.Scheduled else existing?.status ?: SessionStatus.Scheduled,
            ),
        )
        nav.back()
    }

    ScreenScaffold(
        title = if (existing == null) "New session" else "Edit session",
        onBack = nav::back,
        modifier = modifier,
        bottomBar = { StudyZenButton(if (existing == null) "Add session" else "Save changes", onClick = ::save) },
    ) {
        clash?.let {
            InfoBanner(
                "This overlaps \"${it.title}\" (${formatClock(it.startMinute)}–${formatClock(it.endMinute)}). Pick another time.",
                tone = BannerTone.Error,
            )
            Spacer(Modifier.height(12.dp))
        }
        blockProblem?.let {
            InfoBanner(it, tone = BannerTone.Error)
            Spacer(Modifier.height(12.dp))
        }

        StudyZenTextField(
            value = title,
            onValueChange = { title = it; titleError = null },
            label = "Title",
            placeholder = "e.g. Binary trees practice",
            error = titleError,
        )

        val studies = store.activeStudies()
        if (studies.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            SectionHeader("Study (optional)")
            Spacer(Modifier.height(8.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterPill("None", studyId == null, { studyId = null })
                studies.forEach { FilterPill(it.title, studyId == it.id, { studyId = it.id }) }
            }
        }

        Spacer(Modifier.height(16.dp))
        SectionHeader("Type")
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SessionKind.entries.forEach { FilterPill(it.label, kind == it, { kind = it }) }
        }

        Spacer(Modifier.height(16.dp))
        SectionHeader("When")
        Spacer(Modifier.height(8.dp))
        DateStepper("Date", day, { day = it })
        Spacer(Modifier.height(8.dp))
        TimeStepper("Starts", start, { start = it })
        Spacer(Modifier.height(8.dp))
        Stepper(
            label = "Length",
            valueText = formatMinutes(duration),
            onMinus = { duration = (duration - 15).coerceAtLeast(ScheduleRules.MIN_BLOCK_MINUTES) },
            onPlus = { duration = (duration + 15).coerceAtMost(8 * 60) },
        )
        Text(
            "Ends at ${formatClock(block.endMinute)}",
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
        )

        Spacer(Modifier.height(16.dp))
        SectionHeader("Priority")
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Priority.entries.forEach { FilterPill(it.label, priority == it, { priority = it }) }
        }

        Spacer(Modifier.height(16.dp))
        SwitchRow(
            title = "Assignment due",
            subtitle = "Mark that an assignment goes with this session.",
            checked = assignment,
            onCheckedChange = { assignment = it },
        )
        SwitchRow(
            title = "Remind me",
            subtitle = "The alarm is saved on your phone, so it rings even when you are offline.",
            checked = reminder.enabled,
            onCheckedChange = { reminder = reminder.copy(enabled = it) },
        )
        if (reminder.enabled) {
            Spacer(Modifier.height(8.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ReminderLeadTimes.forEach {
                    FilterPill(leadTimeLabel(it), reminder.minutesBefore == it, { reminder = reminder.copy(minutesBefore = it) })
                }
            }
        }

        if (existing != null) {
            Spacer(Modifier.height(16.dp))
            StudyZenTextButton("Delete session", onClick = { confirmDelete = true })
        }
        Spacer(Modifier.height(16.dp))
    }

    if (confirmDelete && existing != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this session?") },
            text = { Text("It is removed from your plan and its reminder is cancelled.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    store.deleteSession(existing.id)
                    nav.back()
                }) { Text("Delete", color = colors.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}
