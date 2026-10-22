package com.fredrickemmanuel.studyzen.feature.plan

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fredrickemmanuel.studyzen.core.designsystem.components.AppIcon
import com.fredrickemmanuel.studyzen.core.designsystem.components.PillTabs
import com.fredrickemmanuel.studyzen.core.designsystem.components.RoundIconButton
import com.fredrickemmanuel.studyzen.core.designsystem.components.SectionHeader
import com.fredrickemmanuel.studyzen.core.designsystem.components.StudyZenButton
import com.fredrickemmanuel.studyzen.core.designsystem.components.StudyZenCard
import com.fredrickemmanuel.studyzen.core.designsystem.components.StudyZenIcon
import com.fredrickemmanuel.studyzen.core.designsystem.components.SubjectIconTile
import com.fredrickemmanuel.studyzen.core.designsystem.components.TagChip
import com.fredrickemmanuel.studyzen.core.time.CivilDate
import com.fredrickemmanuel.studyzen.core.time.DayOfWeek
import com.fredrickemmanuel.studyzen.core.time.formatClock
import com.fredrickemmanuel.studyzen.core.time.formatMinutes
import com.fredrickemmanuel.studyzen.core.time.todayEpochDay
import com.fredrickemmanuel.studyzen.data.LocalStudyStore
import com.fredrickemmanuel.studyzen.domain.SessionStatus
import com.fredrickemmanuel.studyzen.domain.Stats
import com.fredrickemmanuel.studyzen.feature.main.TabContainer
import com.fredrickemmanuel.studyzen.navigation.LocalAppNavigator
import com.fredrickemmanuel.studyzen.navigation.Screen

/** Plan tab: Day / Week / Month calendar, the sessions of the selected day and the study plans. */
@Composable
fun PlanScreen(modifier: Modifier = Modifier) {
    val store = LocalStudyStore.current
    val nav = LocalAppNavigator.current
    val colors = MaterialTheme.colorScheme
    val today = todayEpochDay()

    var mode by rememberSaveable { mutableIntStateOf(1) } // 0 Day, 1 Week, 2 Month
    var selected by rememberSaveable { mutableLongStateOf(today) }

    TabContainer(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Plan", style = MaterialTheme.typography.headlineSmall, color = colors.onBackground, modifier = Modifier.weight(1f))
            RoundIconButton(AppIcon.Bell, "Reminders", onClick = { nav.navigate(Screen.Reminders) })
            Spacer(Modifier.size(8.dp))
            RoundIconButton(AppIcon.Plus, "Add session", onClick = { nav.navigate(Screen.SessionEditor(null, selected)) })
        }
        Spacer(Modifier.height(16.dp))
        PillTabs(listOf("Day", "Week", "Month"), mode, { mode = it })
        Spacer(Modifier.height(16.dp))

        // --- Navigator ---
        val step = when (mode) { 0 -> 1L; 1 -> 7L; else -> 0L }
        val date = CivilDate.fromEpochDay(selected)
        val title = when (mode) {
            0 -> if (selected == today) "Today, ${date.shortLabel()}" else date.shortLabel()
            1 -> {
                val start = CivilDate.fromEpochDay(Stats.weekStart(selected))
                val end = start.plusDays(6)
                "${start.day} ${start.monthName.take(3)} – ${end.day} ${end.monthName.take(3)}"
            }
            else -> "${date.monthName} ${date.year}"
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            RoundIconButton(AppIcon.ChevronRight, "Previous", onClick = {
                selected = if (mode == 2) date.plusMonths(-1).epochDay else selected - step
            }, modifier = Modifier.rotate(180f))
            Text(title, style = MaterialTheme.typography.titleMedium, color = colors.onBackground, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            RoundIconButton(AppIcon.ChevronRight, "Next", onClick = {
                selected = if (mode == 2) date.plusMonths(1).epochDay else selected + step
            })
        }
        Spacer(Modifier.height(12.dp))

        when (mode) {
            1 -> WeekStrip(selected, today, { selected = it }, { store.sessionsOn(it).isNotEmpty() })
            2 -> MonthGrid(date, selected, today, { selected = it }, { store.sessionsOn(it).isNotEmpty() })
        }
        Spacer(Modifier.height(16.dp))

        // --- Sessions of the selected day ---
        val daySessions = store.sessionsOn(selected)
        SectionHeader(
            if (selected == today) "Today" else date.shortLabel(),
            actionLabel = "Add",
            onAction = { nav.navigate(Screen.SessionEditor(null, selected)) },
        )
        Spacer(Modifier.height(8.dp))
        if (daySessions.isEmpty()) {
            StudyZenCard {
                Text("No sessions on this day", style = MaterialTheme.typography.titleSmall, color = colors.onSurface)
                Text(
                    "Add one, or build a plan and review the suggested schedule.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                daySessions.forEach { s ->
                    StudyZenCard(onClick = { nav.navigate(Screen.SessionEditor(s.id, s.epochDay)) }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SubjectIconTile(s.subject)
                            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                                Text(s.title, style = MaterialTheme.typography.titleSmall, color = colors.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    "${formatClock(s.startMinute)} – ${formatClock(s.endMinute)} · ${formatMinutes(s.durationMinutes)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.onSurfaceVariant,
                                )
                                Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    TagChip(s.kind.label)
                                    if (s.reminder?.enabled == true) StudyZenIcon(AppIcon.Bell, size = 16.dp, tint = colors.primary)
                                }
                            }
                            when (s.status) {
                                SessionStatus.Completed -> TagChip("Done", container = Color(0xFFDDF6EC), content = Color(0xFF0E5A42))
                                SessionStatus.Skipped -> TagChip("Skipped", container = colors.surfaceVariant, content = colors.onSurfaceVariant)
                                SessionStatus.Scheduled -> if (s.epochDay == today) {
                                    TagChip("Start", container = colors.primary, content = colors.onPrimary,
                                        modifier = Modifier.clickable(role = Role.Button) { nav.navigate(Screen.StudySession(s.id)) })
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- Plans ---
        Spacer(Modifier.height(24.dp))
        SectionHeader("Study plans")
        Spacer(Modifier.height(8.dp))
        if (store.plans.isEmpty()) {
            StudyZenCard {
                Text("No study plans yet", style = MaterialTheme.typography.titleSmall, color = colors.onSurface)
                Text(
                    "A plan picks study days and times, then suggests a realistic schedule you can review.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                store.plans.forEach { plan ->
                    val upcoming = store.sessions.count { it.planId == plan.id && it.status == SessionStatus.Scheduled }
                    StudyZenCard(onClick = { nav.navigate(Screen.PlanBuilder(plan.id)) }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(plan.name, style = MaterialTheme.typography.titleSmall, color = colors.onSurface)
                                Text(
                                    "${plan.studyDays.sorted().joinToString(" ") { it.short }} · ${formatMinutes(plan.minutesPerWeek)} a week",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.onSurfaceVariant,
                                )
                                Text("$upcoming upcoming sessions", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                            }
                            StudyZenIcon(AppIcon.ChevronRight, size = 20.dp, tint = colors.onSurfaceVariant)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        StudyZenButton("New study plan", onClick = { nav.navigate(Screen.PlanBuilder(null)) })
    }
}

@Composable
private fun WeekStrip(selected: Long, today: Long, onSelect: (Long) -> Unit, hasSessions: (Long) -> Boolean) {
    val start = Stats.weekStart(selected)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        for (i in 0L..6L) {
            val day = start + i
            DayCell(
                modifier = Modifier.weight(1f),
                label = DayOfWeek.entries[i.toInt()].letter,
                number = CivilDate.fromEpochDay(day).day,
                isSelected = day == selected,
                isToday = day == today,
                hasSessions = hasSessions(day),
                onClick = { onSelect(day) },
            )
        }
    }
}

@Composable
private fun MonthGrid(
    month: CivilDate,
    selected: Long,
    today: Long,
    onSelect: (Long) -> Unit,
    hasSessions: (Long) -> Boolean,
) {
    val first = month.firstOfMonth()
    val leading = first.dayOfWeek.ordinal
    val total = leading + first.daysInMonth
    val rows = (total + 6) / 7
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth()) {
            DayOfWeek.entries.forEach {
                Text(it.letter, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            }
        }
        for (r in 0 until rows) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (c in 0 until 7) {
                    val index = r * 7 + c
                    val dayNumber = index - leading + 1
                    if (dayNumber < 1 || dayNumber > first.daysInMonth) {
                        Spacer(Modifier.weight(1f))
                    } else {
                        val epoch = first.epochDay + dayNumber - 1
                        DayCell(
                            modifier = Modifier.weight(1f),
                            label = null,
                            number = dayNumber,
                            isSelected = epoch == selected,
                            isToday = epoch == today,
                            hasSessions = hasSessions(epoch),
                            onClick = { onSelect(epoch) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    modifier: Modifier,
    label: String?,
    number: Int,
    isSelected: Boolean,
    isToday: Boolean,
    hasSessions: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val bg = if (isSelected) colors.primary else colors.surface
    val fg = if (isSelected) colors.onPrimary else colors.onSurface
    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .background(bg)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (label != null) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = fg.copy(alpha = 0.8f))
        }
        Text(
            "$number",
            style = if (isToday) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
            color = if (isToday && !isSelected) colors.primary else fg,
        )
        Box(
            Modifier
                .padding(top = 3.dp)
                .size(5.dp)
                .clip(CircleShape)
                .background(if (hasSessions) (if (isSelected) colors.onPrimary else colors.primary) else Color.Transparent),
        )
    }
}
