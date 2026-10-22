package com.fredrickemmanuel.studyzen.feature.study

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fredrickemmanuel.studyzen.core.designsystem.components.ErrorState
import com.fredrickemmanuel.studyzen.core.designsystem.components.ScreenScaffold
import com.fredrickemmanuel.studyzen.core.designsystem.components.StudyZenButton
import com.fredrickemmanuel.studyzen.core.designsystem.components.StudyZenCard
import com.fredrickemmanuel.studyzen.core.designsystem.components.StudyZenTextButton
import com.fredrickemmanuel.studyzen.core.designsystem.components.TagChip
import com.fredrickemmanuel.studyzen.core.time.CivilDate
import com.fredrickemmanuel.studyzen.data.LocalStudyStore
import com.fredrickemmanuel.studyzen.domain.LogKind
import com.fredrickemmanuel.studyzen.feature.media.LocalMediaPlatform
import com.fredrickemmanuel.studyzen.navigation.LocalAppNavigator
import com.fredrickemmanuel.studyzen.navigation.Screen
import com.fredrickemmanuel.studyzen.navigation.StudyTool

/** One line of a tool's history. */
private data class HistoryRow(
    val key: String,
    val title: String,
    val detail: String,
    val tag: String? = null,
    val open: () -> Unit,
    val delete: (() -> Unit)? = null,
)

/**
 * The first thing a tool shows inside a study: what was already done there (conversations, notes, searches,
 * words, calculations), with "new" as an explicit choice. A tool with no history goes straight to a new item.
 */
@Composable
fun ToolHistoryScreen(studyId: String, tool: StudyTool, lessonId: String?, modifier: Modifier = Modifier) {
    val store = LocalStudyStore.current
    val nav = LocalAppNavigator.current
    val media = LocalMediaPlatform.current
    val colors = MaterialTheme.colorScheme
    val study = store.study(studyId)

    if (study == null) {
        ScreenScaffold(tool.label, onBack = { nav.back() }, modifier = modifier) {
            ErrorState(message = "This Study could not be found.", onRetry = { nav.back() }, title = "Study not found")
        }
        return
    }

    val lesson = study.lesson(lessonId)
    var askDelete by remember { mutableStateOf<String?>(null) }

    fun startNew(replace: Boolean) {
        val target: Screen =
            when (tool) {
                StudyTool.Chat -> Screen.Scanner(studyId, null, lessonId)
                StudyTool.Notes -> Screen.NoteEditor(studyId, null)
                StudyTool.Research -> Screen.Research(studyId, null)
                StudyTool.Dictionary -> Screen.Dictionary(studyId, null)
                StudyTool.Calculator -> Screen.Calculator(studyId, null)
            }
        if (replace) nav.replaceTop(target) else nav.navigate(target)
    }

    fun day(epochDay: Long) = CivilDate.fromEpochDay(epochDay).shortLabel()

    val rows: List<HistoryRow> = when (tool) {
        StudyTool.Chat -> store.scansFor(studyId)
            // conversations from this lesson first
            .sortedByDescending { it.lessonId != null && it.lessonId == lessonId }
            .map { sc ->
                val asked = sc.turns.count { it.fromUser }
                HistoryRow(
                    key = sc.id,
                    title = sc.title,
                    detail = "$asked message${if (asked == 1) "" else "s"} · ${day(sc.updatedAtEpochDay)}" +
                        (study.lesson(sc.lessonId)?.let { " · ${it.title}" } ?: ""),
                    tag = if (sc.imageFile != null) "Photo" else null,
                    open = { nav.navigate(Screen.Scanner(studyId, sc.id, lessonId ?: sc.lessonId)) },
                    delete = { askDelete = sc.id },
                )
            }
        StudyTool.Notes -> store.notesFor(studyId).map { n ->
            HistoryRow(
                key = n.id,
                title = n.title.ifBlank { "Untitled note" },
                detail = n.body.lineSequence().firstOrNull { it.isNotBlank() }.orEmpty().take(90).ifEmpty { day(n.updatedAtEpochDay) },
                tag = n.source.label,
                open = { nav.navigate(Screen.NoteEditor(studyId, n.id)) },
            )
        }
        StudyTool.Research -> store.research.filter { it.studyId == studyId }.map { r ->
            val sources = r.findings.count { it.isSourced }
            HistoryRow(
                key = r.id,
                title = r.question,
                detail = "$sources source${if (sources == 1) "" else "s"}",
                open = { nav.navigate(Screen.Research(studyId, r.id)) },
            )
        }
        StudyTool.Dictionary -> store.logFor(studyId).filter { it.kind == LogKind.Dictionary }.map { e ->
            HistoryRow(
                key = e.id,
                title = e.text,
                detail = e.detail.take(90).ifEmpty { day(e.epochDay) },
                open = { nav.navigate(Screen.Dictionary(studyId, e.text)) },
            )
        }
        StudyTool.Calculator -> store.logFor(studyId).filter { it.kind == LogKind.Calculator }.map { e ->
            HistoryRow(
                key = e.id,
                title = e.text,
                detail = day(e.epochDay),
                open = { nav.navigate(Screen.Calculator(studyId, e.text.substringAfterLast("= ", e.detail))) },
            )
        }
    }

    // nothing to look back on: skip the empty list and go straight to a new one
    LaunchedEffect(tool, rows.isEmpty()) { if (rows.isEmpty()) startNew(replace = true) }
    if (rows.isEmpty()) return

    ScreenScaffold(title = tool.label, onBack = { nav.back() }, modifier = modifier) {
        Text(
            study.title + (lesson?.let { " · ${it.title}" } ?: ""),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        StudyZenButton(tool.newLabel, onClick = { startNew(replace = false) })
        Spacer(Modifier.height(20.dp))
        Text("Earlier in this study", style = MaterialTheme.typography.labelLarge, color = colors.onBackground)
        Spacer(Modifier.height(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            rows.forEach { row ->
                StudyZenCard(onClick = row.open) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(row.title, style = MaterialTheme.typography.titleSmall, color = colors.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(row.detail, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                        row.tag?.let { TagChip(it, modifier = Modifier.padding(start = 8.dp)) }
                        if (row.delete != null) StudyZenTextButton("✕", onClick = { row.delete.invoke() })
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }

    askDelete?.let { id ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { askDelete = null },
            title = { Text("Delete this conversation?") },
            text = { Text("It will be removed from this study. Notes you saved from it stay.") },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    store.scan(id)?.imageFile?.let { media.deleteMedia(it) }
                    store.deleteScan(id)
                    askDelete = null
                }) { Text("Delete", color = colors.error) }
            },
            dismissButton = { androidx.compose.material3.TextButton(onClick = { askDelete = null }) { Text("Cancel") } },
        )
    }
}
