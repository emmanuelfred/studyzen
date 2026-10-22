package com.fredrickemmanuel.studyzen.feature.plan

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Alignment
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
import com.fredrickemmanuel.studyzen.core.designsystem.components.StudyZenButton
import com.fredrickemmanuel.studyzen.core.designsystem.components.StudyZenCard
import com.fredrickemmanuel.studyzen.core.designsystem.components.StudyZenTextButton
import com.fredrickemmanuel.studyzen.core.designsystem.components.StudyZenTextField
import com.fredrickemmanuel.studyzen.core.designsystem.components.SwitchRow
import com.fredrickemmanuel.studyzen.core.designsystem.components.TagChip
import com.fredrickemmanuel.studyzen.core.designsystem.components.TimeStepper
import com.fredrickemmanuel.studyzen.core.time.CivilDate
import com.fredrickemmanuel.studyzen.core.time.DayOfWeek
import com.fredrickemmanuel.studyzen.core.time.formatClock
import com.fredrickemmanuel.studyzen.core.time.formatMinutes
import com.fredrickemmanuel.studyzen.core.time.nowMinuteOfDay
import com.fredrickemmanuel.studyzen.core.time.todayEpochDay
import com.fredrickemmanuel.studyzen.data.LocalStudyStore
import com.fredrickemmanuel.studyzen.domain.AssignmentCadence
import com.fredrickemmanuel.studyzen.domain.PlanGenerator
import com.fredrickemmanuel.studyzen.domain.Priority
import com.fredrickemmanuel.studyzen.domain.Reminder
import com.fredrickemmanuel.studyzen.domain.ScheduleRules
import com.fredrickemmanuel.studyzen.domain.SoundOptions
import com.fredrickemmanuel.studyzen.domain.StudyPlan
import com.fredrickemmanuel.studyzen.domain.StudySession
import com.fredrickemmanuel.studyzen.domain.StudyTopic
import com.fredrickemmanuel.studyzen.domain.Subject
import com.fredrickemmanuel.studyzen.domain.TimeBlock
import com.fredrickemmanuel.studyzen.feature.reminders.LocalReminderPlatform
import com.fredrickemmanuel.studyzen.navigation.LocalAppNavigator
import com.fredrickemmanuel.studyzen.navigation.Screen

/** Reminder lead times offered to the learner, in minutes. */
internal val ReminderLeadTimes = listOf(0, 5, 10, 15, 30, 60)

internal fun leadTimeLabel(minutes: Int) = if (minutes == 0) "At start" else "$minutes min before"

/**
 * Create or edit a study plan: which Studies, the start and end date, which days, and for each day
 * its own session times (several per day, never overlapping), assignments and reminders.
 */
@Composable
fun PlanBuilderScreen(planId: String?, modifier: Modifier = Modifier) {
    val store = LocalStudyStore.current
    val nav = LocalAppNavigator.current
    val colors = MaterialTheme.colorScheme
    val existing = store.plan(planId)
    val today = todayEpochDay()
    val reminderStatus = LocalReminderPlatform.current.status

    var name by remember { mutableStateOf(existing?.name ?: "") }
    var goal by remember { mutableStateOf(existing?.goal ?: "") }
    var studyIds by remember { mutableStateOf(existing?.studyIds ?: emptyList()) }
    var startDay by remember { mutableStateOf(existing?.startEpochDay?.coerceAtLeast(today) ?: today) }
    var endDay by remember { mutableStateOf(existing?.endEpochDay ?: (today + 27)) }
    var blocks by remember { mutableStateOf(existing?.weeklyBlocks ?: emptyMap()) }
    var assignment by remember { mutableStateOf(existing?.assignment ?: AssignmentCadence.None) }
    var revision by remember { mutableStateOf(existing?.includeRevision ?: true) }
    var reminder by remember { mutableStateOf(existing?.reminder ?: Reminder(sound = store.profile.defaultSound, vibrate = store.profile.vibrate)) }

    var nameError by remember { mutableStateOf<String?>(null) }
    var formError by remember { mutableStateOf<String?>(null) }
    var preview by remember { mutableStateOf<List<StudySession>?>(null) }
    var skipped by remember { mutableStateOf(0) }
    var confirmDelete by remember { mutableStateOf(false) }

    /** Any edit makes an earlier preview out of date. */
    fun changed() { preview = null; formError = null }

    fun buildPlan() = StudyPlan(
        id = existing?.id ?: Ids.next("plan"),
        name = name.trim(),
        goal = goal.trim(),
        studyIds = studyIds,
        startEpochDay = startDay,
        endEpochDay = endDay,
        weeklyBlocks = blocks.filterValues { it.isNotEmpty() },
        assignment = assignment,
        includeRevision = revision,
        reminder = reminder,
        priority = existing?.priority ?: Priority.Medium,
    )

    fun makePreview() {
        val plan = buildPlan()
        nameError = if (plan.name.isEmpty()) "Give your plan a name" else null
        val dayProblem = plan.weeklyBlocks.values.firstNotNullOfOrNull { ScheduleRules.dayError(it) }
        formError = when {
            nameError != null -> "Add a name for the plan."
            endDay < startDay -> "The end date must be on or after the start date."
            plan.weeklyBlocks.isEmpty() -> "Choose at least one study day and add a session time."
            dayProblem != null -> dayProblem
            else -> null
        }
        if (formError != null) return

        val topics = if (plan.studyIds.isEmpty()) {
            listOf(StudyTopic(plan.name, null, Subject.Other))
        } else {
            plan.studyIds.mapNotNull { store.study(it) }.map { StudyTopic(it.title, it.id, it.subject) }
        }
        val made = store.suggestSessions(plan, topics)
        val wanted = PlanGenerator.slots(plan, today, nowMinuteOfDay()).size
        skipped = wanted - made.size
        if (made.isEmpty()) {
            formError = "No sessions fit between these dates. Check the dates and the days you chose."
            return
        }
        preview = made
    }

    ScreenScaffold(
        title = if (existing == null) "New study plan" else "Edit study plan",
        onBack = nav::back,
        modifier = modifier,
        bottomBar = {
            val ready = preview
            if (ready == null) {
                StudyZenButton("Preview schedule", onClick = ::makePreview)
            } else {
                StudyZenButton("Save plan (${ready.size} sessions)", onClick = {
                    store.commitPlan(buildPlan(), ready)
                    if (reminder.enabled && !reminderStatus.ready && reminderStatus.supported) {
                        nav.replaceTop(Screen.NotificationPermission)
                    } else {
                        nav.back()
                    }
                })
            }
        },
    ) {
        formError?.let {
            InfoBanner(it, tone = BannerTone.Error)
            Spacer(Modifier.height(12.dp))
        }

        StudyZenTextField(
            value = name,
            onValueChange = { name = it; nameError = null; changed() },
            label = "Plan name",
            placeholder = "e.g. Exam preparation",
            error = nameError,
        )
        Spacer(Modifier.height(12.dp))
        StudyZenTextField(
            value = goal,
            onValueChange = { goal = it; changed() },
            label = "Goal (optional)",
            placeholder = "What should be done by the end date?",
            singleLine = false,
            minLines = 2,
        )

        // --- What to study
        Spacer(Modifier.height(20.dp))
        SectionHeader("What are you studying?")
        Spacer(Modifier.height(8.dp))
        val studies = store.activeStudies()
        if (studies.isEmpty()) {
            Text(
                "You have no Studies yet, so sessions will use the plan name. You can add a Study later.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
        } else {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                studies.forEach { study ->
                    FilterPill(study.title, study.id in studyIds, {
                        studyIds = if (study.id in studyIds) studyIds - study.id else studyIds + study.id
                        changed()
                    })
                }
            }
            Text(
                "Pick one or more. With several, the sessions take turns between them.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp),
            )
        }

        // --- Dates
        Spacer(Modifier.height(20.dp))
        SectionHeader("When does it run?")
        Spacer(Modifier.height(8.dp))
        StudyZenCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                DateStepper("Start date", startDay, { startDay = it.coerceAtLeast(today); if (endDay < startDay) endDay = startDay; changed() })
                DateStepper("End date", endDay, { endDay = it.coerceAtLeast(startDay); changed() })
                val days = endDay - startDay + 1
                Text(
                    "$days days (${(days + 6) / 7} weeks)",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }
        }

        // --- Weekly schedule
        Spacer(Modifier.height(20.dp))
        SectionHeader("Weekly schedule")
        Text(
            "Turn on the days you will study and set times for each. A day can have several sessions, as long as they don't overlap.",
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            DayOfWeek.entries.forEach { day ->
                DayScheduleCard(
                    day = day,
                    blocks = blocks[day].orEmpty(),
                    onBlocks = { next ->
                        blocks = if (next.isEmpty()) blocks - day else blocks + (day to next)
                        changed()
                    },
                    onCopyToAll = {
                        val source = blocks[day].orEmpty()
                        blocks = blocks.mapValues { source } // only days already turned on
                        changed()
                    },
                    canCopy = blocks.keys.size > 1,
                )
            }
        }
        if (blocks.isNotEmpty()) {
            val perWeek = blocks.values.sumOf { it.size }
            val minutes = blocks.values.sumOf { list -> list.sumOf { it.minutes } }
            Text(
                "${blocks.size} days a week · $perWeek sessions · ${formatMinutes(minutes)} a week",
                style = MaterialTheme.typography.labelLarge,
                color = colors.primary,
                modifier = Modifier.padding(top = 10.dp),
            )
        }

        // --- Assignments
        Spacer(Modifier.height(20.dp))
        SectionHeader("Assignments")
        Spacer(Modifier.height(8.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AssignmentCadence.entries.forEach {
                FilterPill(it.label, assignment == it, { assignment = it; changed() })
            }
        }
        Text(
            when (assignment) {
                AssignmentCadence.None -> "No assignment is attached to your sessions."
                AssignmentCadence.EverySession -> "Every session ends with an assignment to complete."
                AssignmentCadence.Weekly -> "The last session of each week comes with an assignment."
            },
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp),
        )
        Spacer(Modifier.height(8.dp))
        SwitchRow(
            title = "Add review sessions",
            subtitle = "The first session of every third study day becomes a review.",
            checked = revision,
            onCheckedChange = { revision = it; changed() },
        )

        // --- Reminders
        Spacer(Modifier.height(20.dp))
        SectionHeader("Reminders")
        Spacer(Modifier.height(8.dp))
        StudyZenCard {
            SwitchRow(
                title = "Remind me before each session",
                subtitle = "Alarms are saved on your phone, so they still ring when you are offline.",
                checked = reminder.enabled,
                onCheckedChange = { reminder = reminder.copy(enabled = it); changed() },
            )
            if (reminder.enabled) {
                Spacer(Modifier.height(8.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ReminderLeadTimes.forEach {
                        FilterPill(leadTimeLabel(it), reminder.minutesBefore == it, { reminder = reminder.copy(minutesBefore = it); changed() })
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SoundOptions.forEach {
                        FilterPill(it, reminder.sound == it, { reminder = reminder.copy(sound = it); changed() })
                    }
                }
                SwitchRow(
                    title = "Vibrate",
                    checked = reminder.vibrate,
                    onCheckedChange = { reminder = reminder.copy(vibrate = it); changed() },
                )
            }
        }

        // --- Preview
        preview?.let { list ->
            Spacer(Modifier.height(20.dp))
            SectionHeader("Your schedule")
            Spacer(Modifier.height(8.dp))
            if (skipped > 0) {
                InfoBanner(
                    "$skipped time ${if (skipped == 1) "slot was" else "slots were"} left out because you already have a session then.",
                )
                Spacer(Modifier.height(8.dp))
            }
            StudyZenCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    list.take(8).forEach { s ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(s.title, style = MaterialTheme.typography.titleSmall, color = colors.onSurface)
                                Text(
                                    "${CivilDate.fromEpochDay(s.epochDay).shortLabel()} · ${formatClock(s.startMinute)}–${formatClock(s.endMinute)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.onSurfaceVariant,
                                )
                            }
                            if (s.kind.label != "Study") TagChip(s.kind.label)
                            if (s.hasAssignment) {
                                Spacer(Modifier.width(6.dp))
                                TagChip("Assignment")
                            }
                        }
                    }
                    if (list.size > 8) {
                        Text("+ ${list.size - 8} more sessions", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                    }
                }
            }
        }

        if (existing != null) {
            Spacer(Modifier.height(16.dp))
            StudyZenTextButton("Delete this plan", onClick = { confirmDelete = true })
        }
        Spacer(Modifier.height(16.dp))
    }

    if (confirmDelete && existing != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this plan?") },
            text = { Text("Its upcoming sessions are removed. Sessions you already finished stay in your progress.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    store.deletePlan(existing.id)
                    nav.back()
                }) { Text("Delete", color = colors.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

/** One weekday: a switch, then its own session times. */
@Composable
private fun DayScheduleCard(
    day: DayOfWeek,
    blocks: List<TimeBlock>,
    onBlocks: (List<TimeBlock>) -> Unit,
    onCopyToAll: () -> Unit,
    canCopy: Boolean,
) {
    val colors = MaterialTheme.colorScheme
    val error = ScheduleRules.dayError(blocks)
    val on = blocks.isNotEmpty()

    StudyZenCard {
        SwitchRow(
            title = day.label,
            subtitle = if (on) "${blocks.size} ${if (blocks.size == 1) "session" else "sessions"} · ${formatMinutes(blocks.sumOf { it.minutes })}" else "Rest day",
            checked = on,
            onCheckedChange = { enabled ->
                onBlocks(if (enabled) listOfNotNull(ScheduleRules.nextBlock(emptyList())) else emptyList())
            },
        )
        if (on) {
            blocks.forEachIndexed { index, block ->
                Spacer(Modifier.height(10.dp))
                Text("Session ${index + 1}  ·  ${formatMinutes(block.minutes)}", style = MaterialTheme.typography.labelLarge, color = colors.onSurface)
                Spacer(Modifier.height(6.dp))
                TimeStepper("From", block.startMinute, { start ->
                    // moving the start keeps the length, so the session slides instead of shrinking
                    val length = block.minutes
                    val newStart = start.coerceAtMost(24 * 60 - length)
                    onBlocks(blocks.replaceAt(index, TimeBlock(newStart, newStart + length)))
                })
                Spacer(Modifier.height(6.dp))
                TimeStepper("To", block.endMinute, { end ->
                    onBlocks(blocks.replaceAt(index, TimeBlock(block.startMinute, end.coerceAtLeast(block.startMinute + ScheduleRules.MIN_BLOCK_MINUTES))))
                })
                if (blocks.size > 1) {
                    StudyZenTextButton("Remove session ${index + 1}", onClick = { onBlocks(blocks.filterIndexed { i, _ -> i != index }) })
                }
            }
            error?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, style = MaterialTheme.typography.bodySmall, color = colors.error)
            }
            val next = ScheduleRules.nextBlock(blocks)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                StudyZenTextButton("Add another session", enabled = next != null, onClick = { next?.let { onBlocks(blocks + it) } })
                if (canCopy) StudyZenTextButton("Copy to other days", onClick = onCopyToAll)
            }
            if (next == null) {
                Text("No more room today.", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
        }
    }
}

private fun <T> List<T>.replaceAt(index: Int, item: T): List<T> =
    mapIndexed { i, old -> if (i == index) item else old }
