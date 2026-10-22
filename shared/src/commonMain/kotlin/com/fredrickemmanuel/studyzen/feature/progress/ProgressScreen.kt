package com.fredrickemmanuel.studyzen.feature.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.fredrickemmanuel.studyzen.core.designsystem.components.AppIcon
import com.fredrickemmanuel.studyzen.core.designsystem.components.DonutChart
import com.fredrickemmanuel.studyzen.core.designsystem.components.DonutSegment
import com.fredrickemmanuel.studyzen.core.designsystem.components.EmptyState
import com.fredrickemmanuel.studyzen.core.designsystem.components.PillTabs
import com.fredrickemmanuel.studyzen.core.designsystem.components.SectionHeader
import com.fredrickemmanuel.studyzen.core.designsystem.components.StudyTimeChart
import com.fredrickemmanuel.studyzen.core.designsystem.components.StudyZenCard
import com.fredrickemmanuel.studyzen.core.designsystem.components.StudyZenIcon
import com.fredrickemmanuel.studyzen.core.time.formatMinutes
import com.fredrickemmanuel.studyzen.core.time.todayEpochDay
import com.fredrickemmanuel.studyzen.data.LocalStudyStore
import com.fredrickemmanuel.studyzen.domain.SessionStatus
import com.fredrickemmanuel.studyzen.domain.Stats
import com.fredrickemmanuel.studyzen.feature.main.TabContainer
import com.fredrickemmanuel.studyzen.navigation.LocalAppNavigator
import com.fredrickemmanuel.studyzen.navigation.Screen

private val SegmentColors = listOf(
    Color(0xFF7155D8), Color(0xFF2FA37C), Color(0xFFE59A3C), Color(0xFF4C8DE0), Color(0xFFD9627A), Color(0xFF8C8AA3),
)

/** Progress tab. Every number comes from recorded sessions; with no data it shows an empty state. */
@Composable
fun ProgressScreen(modifier: Modifier = Modifier) {
    val store = LocalStudyStore.current
    val nav = LocalAppNavigator.current
    val colors = MaterialTheme.colorScheme
    val today = todayEpochDay()
    var rangeIndex by rememberSaveable { mutableIntStateOf(1) }
    val ranges = Stats.Range.entries
    val range = ranges[rangeIndex]

    val first = store.sessions.filter { it.status == SessionStatus.Completed }
        .minOfOrNull { it.epochDay }
    val days = Stats.daysIn(range, today, first)
    val completed = Stats.completed(store.sessions, days)
    val minutes = Stats.totalMinutes(store.sessions, days)
    val rate = Stats.completionRate(store.sessions, days, today)
    val streak = Stats.streak(store.sessions, today)

    TabContainer(modifier) {
        Text("Overview", style = MaterialTheme.typography.headlineSmall, color = colors.onBackground)
        Spacer(Modifier.height(16.dp))
        PillTabs(ranges.map { it.label }, rangeIndex, { rangeIndex = it })
        Spacer(Modifier.height(16.dp))

        if (store.sessions.none { it.status == SessionStatus.Completed } && store.results.isEmpty()) {
            EmptyState(
                title = "No progress yet",
                message = "Finish a study session and your time, streak and results will show up here.",
                actionLabel = "Plan a session",
                onAction = { nav.navigate(Screen.SessionEditor(null, today)) },
            )
            return@TabContainer
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(Modifier.weight(1f), AppIcon.Clock, if (minutes == 0) "0m" else formatMinutes(minutes), "Study time")
            StatTile(Modifier.weight(1f), AppIcon.Target, "${completed.size}", "Sessions done")
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(Modifier.weight(1f), AppIcon.Chart, rate?.let { "$it%" } ?: "–", "Completion rate")
            StatTile(Modifier.weight(1f), AppIcon.Flame, if (streak == 1) "1 day" else "$streak days", "Current streak")
        }

        Spacer(Modifier.height(24.dp))
        SectionHeader("Study time")
        Spacer(Modifier.height(8.dp))
        val (values, labels) = Stats.chartSeries(range, store.sessions, today, first)
        StudyZenCard {
            if (values.isEmpty() || values.all { it == 0f }) {
                Text("No study time recorded for this period.", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            } else {
                StudyTimeChart(
                    values = values,
                    labels = labels,
                    unitLabel = "Hours",
                    summary = "Study time chart for the selected period. Total ${formatMinutes(minutes)}.",
                )
            }
        }

        val bySubject = Stats.minutesByStudy(store.sessions, days) { store.study(it)?.title ?: "" }
        if (bySubject.isNotEmpty()) {
            Spacer(Modifier.height(24.dp))
            SectionHeader("Subject breakdown")
            Spacer(Modifier.height(8.dp))
            val total = bySubject.sumOf { it.second }.coerceAtLeast(1)
            StudyZenCard {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    DonutChart(
                        segments = bySubject.mapIndexed { i, (_, m) -> DonutSegment(m.toFloat(), SegmentColors[i % SegmentColors.size]) },
                        summary = bySubject.joinToString { "${it.first} ${it.second * 100 / total} percent" },
                        center = {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(formatMinutes(total), style = MaterialTheme.typography.titleLarge, color = colors.onSurface)
                                Text("total", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                            }
                        },
                    )
                }
                Spacer(Modifier.height(12.dp))
                bySubject.forEachIndexed { i, (name, m) ->
                    Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(12.dp).clip(CircleShape).background(SegmentColors[i % SegmentColors.size]))
                        Text(name, style = MaterialTheme.typography.bodyMedium, color = colors.onSurface, modifier = Modifier.weight(1f).padding(start = 10.dp))
                        Text("${m * 100 / total}% · ${formatMinutes(m)}", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                    }
                }
            }
        }

        // Lessons and assessments
        val lessonsDone = store.studies.sumOf { it.completedLessons }
        val lessonsAll = store.studies.sumOf { it.allLessons.size }
        Spacer(Modifier.height(24.dp))
        SectionHeader("Learning")
        Spacer(Modifier.height(8.dp))
        StudyZenCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StudyZenIcon(AppIcon.Book, tint = colors.primary)
                Text("Lessons completed", style = MaterialTheme.typography.bodyMedium, color = colors.onSurface, modifier = Modifier.weight(1f).padding(start = 12.dp))
                Text("$lessonsDone of $lessonsAll", style = MaterialTheme.typography.titleSmall, color = colors.onSurface)
            }
        }

        if (store.results.isNotEmpty()) {
            Spacer(Modifier.height(24.dp))
            SectionHeader("Assessment results")
            Spacer(Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                store.results.take(5).forEach { r ->
                    StudyZenCard(onClick = { nav.navigate(Screen.StudyDetail(r.studyId)) }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(r.title, style = MaterialTheme.typography.titleSmall, color = colors.onSurface)
                                Text(
                                    if (r.weakTopics.isEmpty()) "No weak topics found" else "Review: ${r.weakTopics.joinToString()}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.onSurfaceVariant,
                                )
                            }
                            Text("${r.correct}/${r.total}", style = MaterialTheme.typography.titleMedium, color = colors.primary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatTile(modifier: Modifier, icon: AppIcon, value: String, label: String) {
    val colors = MaterialTheme.colorScheme
    StudyZenCard(modifier = modifier) {
        StudyZenIcon(icon, tint = colors.primary)
        Spacer(Modifier.height(8.dp))
        Text(value, style = MaterialTheme.typography.titleLarge, color = colors.onSurface)
        Text(label, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
    }
}
