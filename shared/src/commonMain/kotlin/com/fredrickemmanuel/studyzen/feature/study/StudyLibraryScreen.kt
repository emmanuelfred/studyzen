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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fredrickemmanuel.studyzen.core.designsystem.components.EmptyState
import com.fredrickemmanuel.studyzen.core.designsystem.components.FilterPill
import com.fredrickemmanuel.studyzen.core.designsystem.components.ScreenScaffold
import com.fredrickemmanuel.studyzen.core.designsystem.components.SoftProgressBar
import com.fredrickemmanuel.studyzen.core.designsystem.components.StudyZenButton
import com.fredrickemmanuel.studyzen.core.designsystem.components.StudyZenCard
import com.fredrickemmanuel.studyzen.core.designsystem.components.SubjectIconTile
import com.fredrickemmanuel.studyzen.core.designsystem.components.TagChip
import com.fredrickemmanuel.studyzen.core.designsystem.components.style
import com.fredrickemmanuel.studyzen.data.LocalStudyStore
import com.fredrickemmanuel.studyzen.navigation.LocalAppNavigator
import com.fredrickemmanuel.studyzen.navigation.Screen

/** All Studies, with an Active / Archived filter. */
@Composable
fun StudyLibraryScreen(modifier: Modifier = Modifier) {
    val store = LocalStudyStore.current
    val nav = LocalAppNavigator.current
    val colors = MaterialTheme.colorScheme
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val list = if (tab == 0) store.activeStudies() else store.archivedStudies()

    ScreenScaffold(
        title = "Study library",
        onBack = nav::back,
        modifier = modifier,
        bottomBar = {
            StudyZenButton("New Study", onClick = { nav.navigate(Screen.StudyEditor(null)) })
        },
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterPill("Active (${store.activeStudies().size})", tab == 0, { tab = 0 })
            FilterPill("Archived (${store.archivedStudies().size})", tab == 1, { tab = 1 })
        }
        Spacer(Modifier.height(16.dp))
        if (list.isEmpty()) {
            EmptyState(
                title = if (tab == 0) "No Studies yet" else "Nothing archived",
                message = if (tab == 0) "A Study is a workspace for one topic: lessons, notes, practice and progress." else "Archived Studies are kept here and can be restored any time.",
                actionLabel = if (tab == 0) "Create a Study" else null,
                onAction = if (tab == 0) ({ nav.navigate(Screen.StudyEditor(null)) }) else null,
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                list.forEach { study ->
                    StudyZenCard(onClick = { nav.navigate(Screen.StudyDetail(study.id)) }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SubjectIconTile(study.subject)
                            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                                Text(study.title, style = MaterialTheme.typography.titleSmall, color = colors.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    "${study.subject.label} · ${study.level.label}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.onSurfaceVariant,
                                )
                            }
                            if (study.archived) TagChip("Archived")
                        }
                        Spacer(Modifier.height(12.dp))
                        SoftProgressBar(study.progress, color = study.subject.style().color)
                        Text(
                            if (study.allLessons.isEmpty()) "No learning path yet" else "${study.completedLessons} of ${study.allLessons.size} lessons",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                }
            }
        }
    }
}
