package com.fredrickemmanuel.studyzen.feature.notes

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
import com.fredrickemmanuel.studyzen.core.designsystem.components.FilterPill
import com.fredrickemmanuel.studyzen.core.designsystem.components.RoundIconButton
import com.fredrickemmanuel.studyzen.core.designsystem.components.ScreenScaffold
import com.fredrickemmanuel.studyzen.core.designsystem.components.StudyZenCard
import com.fredrickemmanuel.studyzen.core.designsystem.components.StudyZenIcon
import com.fredrickemmanuel.studyzen.core.designsystem.components.StudyZenTextField
import com.fredrickemmanuel.studyzen.core.designsystem.components.TagChip
import com.fredrickemmanuel.studyzen.core.time.CivilDate
import com.fredrickemmanuel.studyzen.data.LocalStudyStore
import com.fredrickemmanuel.studyzen.navigation.LocalAppNavigator
import com.fredrickemmanuel.studyzen.navigation.Screen

/** Every note in one place: write general notes or notes for a Study, search them and pin the important ones. */
@Composable
fun NotesScreen(modifier: Modifier = Modifier) {
    val store = LocalStudyStore.current
    val nav = LocalAppNavigator.current
    val colors = MaterialTheme.colorScheme

    var query by remember { mutableStateOf("") }
    // null = all notes, "" = general notes only, otherwise a Study id
    var filter by remember { mutableStateOf<String?>(null) }

    val all = store.allNotes()
    val shown = all.filter { note ->
        val matchesFilter = when (filter) {
            null -> true
            "" -> note.studyId == null
            else -> note.studyId == filter
        }
        val q = query.trim()
        matchesFilter && (q.isEmpty() || note.title.contains(q, ignoreCase = true) || note.body.contains(q, ignoreCase = true))
    }

    ScreenScaffold(
        title = "Notes",
        onBack = nav::back,
        modifier = modifier,
        trailing = { RoundIconButton(AppIcon.Plus, "New note", onClick = { nav.navigate(Screen.NoteEditor(filter?.takeIf { it.isNotEmpty() }, null)) }) },
    ) {
        StudyZenTextField(
            value = query,
            onValueChange = { query = it },
            label = "Search notes",
            placeholder = "Search by title or text",
        )
        Spacer(Modifier.height(12.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterPill("All", filter == null, { filter = null })
            FilterPill("General", filter == "", { filter = "" })
            store.activeStudies().forEach { FilterPill(it.title, filter == it.id, { filter = it.id }) }
        }
        Spacer(Modifier.height(16.dp))

        if (shown.isEmpty()) {
            EmptyState(
                title = if (all.isEmpty()) "No notes yet" else "No notes match",
                message = if (all.isEmpty()) {
                    "Jot down ideas, formulas or anything you want to remember. You can also save answers from the dictionary and calculator."
                } else {
                    "Try a different search or filter."
                },
                actionLabel = if (all.isEmpty()) "Write a note" else null,
                onAction = if (all.isEmpty()) ({ nav.navigate(Screen.NoteEditor(null, null)) }) else null,
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                shown.forEach { note ->
                    StudyZenCard(onClick = { nav.navigate(Screen.NoteEditor(note.studyId, note.id)) }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                note.title.ifBlank { "Untitled note" },
                                style = MaterialTheme.typography.titleSmall,
                                color = colors.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            if (note.pinned) StudyZenIcon(AppIcon.Flame, size = 18.dp, tint = colors.primary)
                        }
                        if (note.body.isNotBlank()) {
                            Text(
                                note.body,
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 2.dp),
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            TagChip(store.study(note.studyId)?.title ?: "General")
                            if (note.source.label != "Note") TagChip(note.source.label)
                            Text(
                                CivilDate.fromEpochDay(note.updatedAtEpochDay).shortLabel(),
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}
