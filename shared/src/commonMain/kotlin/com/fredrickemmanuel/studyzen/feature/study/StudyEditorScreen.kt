package com.fredrickemmanuel.studyzen.feature.study

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fredrickemmanuel.studyzen.core.Ids
import com.fredrickemmanuel.studyzen.core.designsystem.components.FilterPill
import com.fredrickemmanuel.studyzen.core.designsystem.components.ScreenScaffold
import com.fredrickemmanuel.studyzen.core.designsystem.components.StudyZenButton
import com.fredrickemmanuel.studyzen.core.designsystem.components.StudyZenTextField
import com.fredrickemmanuel.studyzen.core.designsystem.components.SwitchRow
import com.fredrickemmanuel.studyzen.core.time.todayEpochDay
import com.fredrickemmanuel.studyzen.data.LocalStudyStore
import com.fredrickemmanuel.studyzen.data.mock.MockAi
import com.fredrickemmanuel.studyzen.domain.KnowledgeLevel
import com.fredrickemmanuel.studyzen.domain.Study
import com.fredrickemmanuel.studyzen.domain.StudyPurpose
import com.fredrickemmanuel.studyzen.domain.Subject
import com.fredrickemmanuel.studyzen.navigation.LocalAppNavigator
import com.fredrickemmanuel.studyzen.navigation.Screen

/** Create a new Study, or edit an existing one when [studyId] is set. */
@Composable
fun StudyEditorScreen(studyId: String?, modifier: Modifier = Modifier) {
    val store = LocalStudyStore.current
    val nav = LocalAppNavigator.current
    val existing = store.study(studyId)
    val colors = MaterialTheme.colorScheme

    var title by remember { mutableStateOf(existing?.title ?: "") }
    var description by remember { mutableStateOf(existing?.description ?: "") }
    var objective by remember { mutableStateOf(existing?.objective ?: "") }
    var subject by remember { mutableStateOf(existing?.subject ?: Subject.Programming) }
    var level by remember { mutableStateOf(existing?.level ?: KnowledgeLevel.Beginner) }
    var purpose by remember { mutableStateOf(existing?.purpose ?: StudyPurpose.Skill) }
    var curriculum by remember { mutableStateOf(existing?.curriculum ?: "") }
    var focus by remember { mutableStateOf(existing?.focus ?: "") }
    var guided by remember { mutableStateOf(existing?.guided ?: false) }
    var generatePath by remember { mutableStateOf(existing == null) }
    val plans = store.plans.toList()
    var planId by remember { mutableStateOf(existing?.let { store.planFor(it.id)?.id }) }
    var titleError by remember { mutableStateOf<String?>(null) }

    fun save() {
        val clean = title.trim()
        if (clean.isEmpty()) {
            titleError = "Give your Study a name"
            return
        }
        if (existing != null) {
            store.updateStudy(
                existing.copy(
                    title = clean, description = description.trim(), objective = objective.trim(), subject = subject, level = level,
                    purpose = purpose, curriculum = curriculum.trim(), focus = focus.trim(), guided = guided && existing.modules.isNotEmpty(),
                ),
            )
            val before = store.planFor(existing.id)?.id
            if (planId != before) {
                val chosen = planId
                if (chosen == null) store.detachStudyFromPlan(existing.id) else store.attachStudyToPlan(existing.id, chosen)
            }
            nav.back()
        } else {
            val modules = if (generatePath) MockAi.learningPath(clean, subject, level) else emptyList()
            val study = Study(
                id = Ids.next("study"),
                title = clean,
                description = description.trim(),
                objective = objective.trim(),
                subject = subject,
                level = level,
                modules = modules,
                createdAtEpochDay = todayEpochDay(),
            )
            store.addStudy(study)
            nav.replaceTop(Screen.StudyDetail(study.id))
        }
    }

    ScreenScaffold(
        title = if (existing == null) "New Study" else "Edit Study",
        onBack = nav::back,
        modifier = modifier,
        bottomBar = { StudyZenButton(if (existing == null) "Create Study" else "Save changes", onClick = ::save) },
    ) {
        StudyZenTextField(
            value = title,
            onValueChange = { title = it; titleError = null },
            label = "Study name",
            placeholder = "e.g. Data Structures",
            error = titleError,
        )
        Spacer(Modifier.height(16.dp))
        StudyZenTextField(
            value = description,
            onValueChange = { description = it },
            label = "Description (optional)",
            placeholder = "What is this Study about?",
            singleLine = false,
            minLines = 2,
        )
        Spacer(Modifier.height(16.dp))
        StudyZenTextField(
            value = objective,
            onValueChange = { objective = it },
            label = "Learning objective (optional)",
            placeholder = "What do you want to be able to do?",
            singleLine = false,
            minLines = 2,
        )
        Spacer(Modifier.height(16.dp))
        Text("Subject", style = MaterialTheme.typography.labelLarge, color = colors.onBackground)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Subject.entries.forEach { FilterPill(it.label, subject == it, { subject = it }) }
        }
        Spacer(Modifier.height(16.dp))
        Text("Your level", style = MaterialTheme.typography.labelLarge, color = colors.onBackground)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KnowledgeLevel.entries.forEach { FilterPill(it.label, level == it, { level = it }) }
        }
        if (existing != null) {
            Spacer(Modifier.height(16.dp))
            Text("Purpose", style = MaterialTheme.typography.labelLarge, color = colors.onBackground)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StudyPurpose.entries.forEach { FilterPill(it.label, purpose == it, { purpose = it }) }
            }
            purpose.curriculumLabel?.let { label ->
                Spacer(Modifier.height(12.dp))
                StudyZenTextField(value = curriculum, onValueChange = { curriculum = it }, label = label, placeholder = purpose.curriculumHint)
            }
            Spacer(Modifier.height(12.dp))
            StudyZenTextField(value = focus, onValueChange = { focus = it }, label = "Areas to focus on", singleLine = false, minLines = 2)
            if (existing.modules.isNotEmpty()) {
                SwitchRow(
                    title = "Open lessons in order",
                    subtitle = "Off: open any lesson any time.",
                    checked = guided,
                    onCheckedChange = { guided = it },
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }
            Spacer(Modifier.height(8.dp))
            Text("Study plan", style = MaterialTheme.typography.labelLarge, color = colors.onBackground)
            Text("A study can be in one plan at a time. Choosing another plan moves it.", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterPill("No plan", planId == null, { planId = null })
                plans.forEach { p -> FilterPill(p.name, planId == p.id, { planId = p.id }) }
            }
        }
        if (existing == null) {
            Spacer(Modifier.height(16.dp))
            SwitchRow(
                title = "Generate a learning path",
                subtitle = "Adds sample modules and lessons you can edit later.",
                checked = generatePath,
                onCheckedChange = { generatePath = it },
                modifier = Modifier.padding(vertical = 4.dp),
            )
        }
    }
}
