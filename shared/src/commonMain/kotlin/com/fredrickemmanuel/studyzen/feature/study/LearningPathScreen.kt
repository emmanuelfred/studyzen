package com.fredrickemmanuel.studyzen.feature.study

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fredrickemmanuel.studyzen.core.Ids
import com.fredrickemmanuel.studyzen.core.designsystem.components.BannerTone
import com.fredrickemmanuel.studyzen.core.designsystem.components.ErrorState
import com.fredrickemmanuel.studyzen.core.designsystem.components.InfoBanner
import com.fredrickemmanuel.studyzen.core.designsystem.components.ScreenScaffold
import com.fredrickemmanuel.studyzen.core.designsystem.components.StudyZenButton
import com.fredrickemmanuel.studyzen.core.designsystem.components.StudyZenCard
import com.fredrickemmanuel.studyzen.core.designsystem.components.StudyZenTextButton
import com.fredrickemmanuel.studyzen.core.designsystem.components.StudyZenTextField
import com.fredrickemmanuel.studyzen.core.designsystem.components.SwitchRow
import com.fredrickemmanuel.studyzen.core.time.todayEpochDay
import com.fredrickemmanuel.studyzen.data.LocalStudyStore
import com.fredrickemmanuel.studyzen.data.ai.LocalAiSettings
import com.fredrickemmanuel.studyzen.data.ai.StudyAi
import com.fredrickemmanuel.studyzen.data.ai.StudyAiOutcome
import com.fredrickemmanuel.studyzen.data.ai.StudyBrief
import com.fredrickemmanuel.studyzen.data.mock.MockAi
import com.fredrickemmanuel.studyzen.domain.LearningModule
import com.fredrickemmanuel.studyzen.domain.Lesson
import com.fredrickemmanuel.studyzen.navigation.LocalAppNavigator
import com.fredrickemmanuel.studyzen.navigation.Screen
import kotlinx.coroutines.launch

/** All modules and lessons of a study, with progress, lock state and a way to build or extend the path. */
@Composable
fun LearningPathScreen(studyId: String, modifier: Modifier = Modifier) {
    val store = LocalStudyStore.current
    val nav = LocalAppNavigator.current
    val aiStore = LocalAiSettings.current
    val colors = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    val ai = remember { StudyAi() }
    val study = store.study(studyId)

    if (study == null) {
        ScreenScaffold("Learning path", onBack = { nav.back() }, modifier = modifier) {
            ErrorState(message = "This Study could not be found.", onRetry = { nav.back() }, title = "Study not found")
        }
        return
    }

    var working by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var proposal by remember { mutableStateOf<List<LearningModule>?>(null) }
    var proposedBy by remember { mutableStateOf("") }
    var adding by remember { mutableStateOf(false) }
    var newTitle by remember { mutableStateOf("") }
    val hasKey = aiStore.settings.hasKey

    fun build() {
        if (working) return
        working = true
        error = null
        scope.launch {
            val brief = StudyBrief(
                title = study.title,
                description = study.description,
                purpose = study.purpose,
                curriculum = study.curriculum,
                focus = study.focus,
                level = study.level,
                daysUntilDeadline = study.deadlineEpochDay?.let { (it - todayEpochDay()).toInt().coerceAtLeast(1) },
                sessions = null,
                minutesPerSession = null,
            )
            when (val out = ai.proposeStructure(aiStore.settings, brief)) {
                is StudyAiOutcome.Ok -> { proposal = out.value; proposedBy = out.provider }
                is StudyAiOutcome.Err -> error = out.message
            }
            working = false
        }
    }

    ScreenScaffold(title = "Learning path", onBack = { nav.back() }, modifier = modifier) {
        Text(study.title, style = MaterialTheme.typography.titleMedium, color = colors.onBackground)
        Spacer(Modifier.height(12.dp))
        error?.let {
            InfoBanner(it, tone = BannerTone.Error)
            Spacer(Modifier.height(12.dp))
        }

        val pending = proposal
        if (pending != null) {
            Text("Proposed by $proposedBy. Nothing is saved until you accept.", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                pending.forEach { m ->
                    StudyZenCard {
                        Text(m.title, style = MaterialTheme.typography.titleSmall, color = colors.onSurface)
                        m.lessons.forEach { l -> Text("• ${l.title}", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant) }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            StudyZenButton(if (study.modules.isEmpty()) "Use this path" else "Replace my path", onClick = {
                store.setModules(study.id, pending)
                store.updateStudy((store.study(study.id) ?: study).copy(aiStructured = true, lastLessonId = null))
                proposal = null
            })
            StudyZenTextButton("Discard", onClick = { proposal = null })
            return@ScreenScaffold
        }

        if (study.modules.isEmpty()) {
            StudyZenCard {
                Text("No lessons yet", style = MaterialTheme.typography.titleSmall, color = colors.onSurface)
                Text(
                    "Build an ordered set of lessons for your goal. You can still use every tool without one.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(12.dp))
            if (hasKey) {
                StudyZenButton("Build my learning path", onClick = { build() }, loading = working)
            } else {
                InfoBanner("A tailored path needs your own free AI key. You can also start from a basic outline.")
                StudyZenTextButton("Add an AI key", onClick = { nav.navigate(Screen.AiSettings) })
            }
            StudyZenTextButton("Use a basic outline", onClick = {
                store.setModules(study.id, MockAi.learningPath(study.title, study.subject, study.level))
            })
            StudyZenTextButton("Add a lesson myself", onClick = { adding = true })
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${study.completedLessons} of ${study.allLessons.size} lessons done",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                StudyZenTextButton("Add lesson", onClick = { adding = true })
            }
            SwitchRow(
                title = "Open lessons in order",
                subtitle = "Off: open any lesson any time.",
                checked = study.guided,
                onCheckedChange = { store.updateStudy(study.copy(guided = it)) },
            )
            Spacer(Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                study.modules.forEach { module ->
                    StudyZenCard {
                        Text(module.title, style = MaterialTheme.typography.titleSmall, color = colors.onSurface)
                        Text(
                            "${module.lessons.count { it.completed }}/${module.lessons.size} done",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant,
                        )
                        module.lessons.forEach { l -> LessonRow(study, l.id) }
                    }
                }
            }
            if (hasKey && study.completedLessons == 0) {
                Spacer(Modifier.height(8.dp))
                StudyZenTextButton(if (working) "Working…" else "Rebuild with AI", enabled = !working, onClick = { build() })
            }
        }
        Spacer(Modifier.height(16.dp))
    }

    if (adding) {
        AlertDialog(
            onDismissRequest = { adding = false },
            title = { Text("Add a lesson") },
            text = { StudyZenTextField(value = newTitle, onValueChange = { newTitle = it }, label = "Lesson title") },
            confirmButton = {
                TextButton(onClick = {
                    val clean = newTitle.trim()
                    if (clean.isNotEmpty()) {
                        val lesson = Lesson(id = Ids.next("lesson"), title = clean, summary = "")
                        val modules = if (study.modules.isEmpty()) {
                            listOf(LearningModule(Ids.next("module"), "My lessons", listOf(lesson)))
                        } else {
                            study.modules.mapIndexed { i, m -> if (i == study.modules.lastIndex) m.copy(lessons = m.lessons + lesson) else m }
                        }
                        store.setModules(study.id, modules)
                    }
                    newTitle = ""
                    adding = false
                }) { Text("Add") }
            },
            dismissButton = { TextButton(onClick = { adding = false }) { Text("Cancel") } },
        )
    }
}
