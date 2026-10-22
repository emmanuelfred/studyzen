package com.fredrickemmanuel.studyzen.feature.study

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fredrickemmanuel.studyzen.core.Ids
import com.fredrickemmanuel.studyzen.core.designsystem.components.BannerTone
import com.fredrickemmanuel.studyzen.core.designsystem.components.ErrorState
import com.fredrickemmanuel.studyzen.core.designsystem.components.FilterPill
import com.fredrickemmanuel.studyzen.core.designsystem.components.InfoBanner
import com.fredrickemmanuel.studyzen.core.designsystem.components.ProgressRing
import com.fredrickemmanuel.studyzen.core.designsystem.components.ScreenScaffold
import com.fredrickemmanuel.studyzen.core.designsystem.components.SectionHeader
import com.fredrickemmanuel.studyzen.core.designsystem.components.Stepper
import com.fredrickemmanuel.studyzen.core.designsystem.components.StudyZenButton
import com.fredrickemmanuel.studyzen.core.designsystem.components.StudyZenCard
import com.fredrickemmanuel.studyzen.core.designsystem.components.StudyZenTextButton
import com.fredrickemmanuel.studyzen.core.designsystem.components.StudyZenTextField
import com.fredrickemmanuel.studyzen.core.designsystem.components.TagChip
import com.fredrickemmanuel.studyzen.core.time.CivilDate
import com.fredrickemmanuel.studyzen.core.time.todayEpochDay
import com.fredrickemmanuel.studyzen.data.LocalStudyStore
import com.fredrickemmanuel.studyzen.data.ai.LocalAiSettings
import com.fredrickemmanuel.studyzen.data.ai.StudyAi
import com.fredrickemmanuel.studyzen.data.ai.StudyAiOutcome
import com.fredrickemmanuel.studyzen.domain.Assessment
import com.fredrickemmanuel.studyzen.domain.AssessmentAnswer
import com.fredrickemmanuel.studyzen.domain.AssessmentKind
import com.fredrickemmanuel.studyzen.domain.AssessmentResult
import com.fredrickemmanuel.studyzen.domain.assessmentTitle
import com.fredrickemmanuel.studyzen.domain.isComplete
import com.fredrickemmanuel.studyzen.domain.recommendKind
import com.fredrickemmanuel.studyzen.domain.weakTopicsOf
import com.fredrickemmanuel.studyzen.domain.withAnswer
import com.fredrickemmanuel.studyzen.feature.ai.AnswerText
import com.fredrickemmanuel.studyzen.navigation.LocalAppNavigator
import com.fredrickemmanuel.studyzen.navigation.Screen
import kotlinx.coroutines.launch

// --- Setup -------------------------------------------------------------------------------------

/** Choose what to be tested on and how, or let the app recommend a kind. Earlier attempts are listed below. */
@Composable
fun QuizSetupScreen(studyId: String, lessonId: String?, modifier: Modifier = Modifier) {
    val store = LocalStudyStore.current
    val nav = LocalAppNavigator.current
    val aiStore = LocalAiSettings.current
    val colors = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    val ai = remember { StudyAi() }
    val study = store.study(studyId)
    if (study == null) {
        ScreenScaffold("Practice", onBack = { nav.back() }, modifier = modifier) {
            ErrorState(message = "This Study could not be found.", onRetry = { nav.back() }, title = "Study not found")
        }
        return
    }

    val lesson = study.lesson(lessonId)
    var wholeStudy by remember { mutableStateOf(lesson == null) }
    val scopeLesson = if (wholeStudy) null else lesson
    val recommended = recommendKind(study, scopeLesson)
    var chosen by remember { mutableStateOf<AssessmentKind?>(null) }   // null = let the app recommend
    val kind = chosen ?: recommended
    var count by remember(kind) { mutableStateOf(if (kind.objective) 8 else 4) }
    var working by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun generate() {
        if (working) return
        working = true
        error = null
        scope.launch {
            when (val out = ai.generateAssessment(aiStore.settings, study, scopeLesson, kind, count)) {
                is StudyAiOutcome.Ok -> {
                    val made = Assessment(
                        id = Ids.next("asm"),
                        studyId = studyId,
                        lessonId = scopeLesson?.id,
                        title = assessmentTitle(kind, scopeLesson, study),
                        kind = kind,
                        createdAtEpochDay = todayEpochDay(),
                        questions = out.value,
                        generatedBy = out.provider,
                    )
                    store.saveAssessment(made)
                    working = false
                    nav.replaceTop(Screen.Quiz(studyId, made.id))
                }
                is StudyAiOutcome.Err -> { error = out.message; working = false }
            }
        }
    }

    ScreenScaffold(title = "Practice and tests", onBack = { nav.back() }, modifier = modifier) {
        Text(study.title, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        if (lesson != null) {
            Text("Test me on", style = MaterialTheme.typography.labelLarge, color = colors.onBackground)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterPill("This lesson", !wholeStudy, { wholeStudy = false })
                FilterPill("Whole study", wholeStudy, { wholeStudy = true })
            }
            Spacer(Modifier.height(16.dp))
        }

        Text("Kind of test", style = MaterialTheme.typography.labelLarge, color = colors.onBackground)
        Spacer(Modifier.height(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ChoiceRow(
                "Let StudyZen choose",
                selected = chosen == null,
                onClick = { chosen = null },
                subtitle = "Suggested for you: ${recommended.label}",
            )
            AssessmentKind.entries.forEach { k ->
                ChoiceRow(k.label, selected = chosen == k, onClick = { chosen = k }, subtitle = k.description)
            }
        }
        Spacer(Modifier.height(12.dp))
        Stepper(
            label = "Questions",
            valueText = "$count",
            onMinus = { count = (count - 1).coerceAtLeast(1) },
            onPlus = { count = (count + 1).coerceAtMost(if (kind.objective) 20 else 10) },
        )
        Spacer(Modifier.height(16.dp))

        error?.let { InfoBanner(it, tone = BannerTone.Error); Spacer(Modifier.height(8.dp)) }
        if (!aiStore.settings.hasKey) {
            InfoBanner("Tests are written by an AI using your own free key.")
            StudyZenTextButton("Add an AI key", onClick = { nav.navigate(Screen.AiSettings) })
        } else {
            StudyZenButton("Create ${kind.label.lowercase()}", onClick = { generate() }, loading = working)
            if (working) {
                Text(
                    "Writing your questions. This can take up to a minute.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }

        val earlier = store.assessmentsFor(studyId)
        if (earlier.isNotEmpty()) {
            Spacer(Modifier.height(24.dp))
            SectionHeader("Earlier attempts")
            Spacer(Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                earlier.take(10).forEach { a -> AssessmentRow(studyId, a) }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
internal fun AssessmentRow(studyId: String, a: Assessment) {
    val nav = LocalAppNavigator.current
    val colors = MaterialTheme.colorScheme
    StudyZenCard(onClick = { nav.navigate(if (a.submitted) Screen.QuizResult(studyId, a.id) else Screen.Quiz(studyId, a.id)) }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(a.title, style = MaterialTheme.typography.titleSmall, color = colors.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "${a.kind.label} · ${CivilDate.fromEpochDay(a.createdAtEpochDay).shortLabel()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }
            when {
                !a.submitted -> TagChip("Unfinished")
                a.total > 0 -> Text("${a.correct}/${a.total}", style = MaterialTheme.typography.titleMedium, color = colors.primary)
                else -> TagChip("Feedback")
            }
        }
    }
}

// --- Taking it ---------------------------------------------------------------------------------

/** Answer the questions. Answers are saved as you go, so you can leave and finish later. */
@Composable
fun QuizScreen(studyId: String, assessmentId: String, modifier: Modifier = Modifier) {
    val store = LocalStudyStore.current
    val nav = LocalAppNavigator.current
    val colors = MaterialTheme.colorScheme
    val assessment = store.assessment(assessmentId)

    if (assessment == null) {
        ScreenScaffold("Test", onBack = { nav.back() }, modifier = modifier) {
            ErrorState(message = "This test could not be found.", onRetry = { nav.back() }, title = "Test not found")
        }
        return
    }
    if (assessment.submitted) {
        LaunchedEffect(assessmentId) { nav.replaceTop(Screen.QuizResult(studyId, assessmentId)) }
        return
    }

    val done = assessment.questions.count { q ->
        val a = assessment.answerFor(q.id)
        if (q.isObjective) a != null && a.choice >= 0 else a != null && a.text.isNotBlank()
    }
    var confirm by remember { mutableStateOf(false) }

    fun submit() {
        var finished = assessment.copy(submitted = true)
        finished = finished.copy(weakTopics = weakTopicsOf(finished))
        store.saveAssessment(finished)
        if (finished.total > 0) {
            store.addResult(
                AssessmentResult(finished.id, studyId, finished.title, finished.correct, finished.total, todayEpochDay(), finished.weakTopics),
            )
        }
        nav.replaceTop(Screen.QuizResult(studyId, assessmentId))
    }

    ScreenScaffold(
        title = assessment.kind.label,
        onBack = { nav.back() },
        modifier = modifier,
        bottomBar = {
            Column {
                Text(
                    "$done of ${assessment.questions.size} answered · saved as you go",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
                StudyZenButton(
                    "Submit",
                    onClick = { if (isComplete(assessment)) submit() else confirm = true },
                )
            }
        },
    ) {
        Text(assessment.title, style = MaterialTheme.typography.titleMedium, color = colors.onBackground)
        Text(assessment.kind.description, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        assessment.questions.forEachIndexed { index, q ->
            StudyZenCard {
                Text("Question ${index + 1}", style = MaterialTheme.typography.labelLarge, color = colors.primary)
                Spacer(Modifier.height(4.dp))
                Text(q.prompt, style = MaterialTheme.typography.bodyLarge, color = colors.onSurface)
                Spacer(Modifier.height(10.dp))
                val answer = assessment.answerFor(q.id)
                if (q.isObjective) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        q.options.forEachIndexed { i, option ->
                            ChoiceRow(option, selected = answer?.choice == i, onClick = {
                                store.saveAssessment(assessment.withAnswer(AssessmentAnswer(q.id, choice = i)))
                            })
                        }
                    }
                } else {
                    StudyZenTextField(
                        value = answer?.text.orEmpty(),
                        onValueChange = { store.saveAssessment(assessment.withAnswer(AssessmentAnswer(q.id, text = it))) },
                        label = "Your answer",
                        singleLine = false,
                        minLines = if (assessment.kind == AssessmentKind.Written || assessment.kind == AssessmentKind.Coding) 8 else 4,
                    )
                    if (q.criteria.isNotBlank()) {
                        Text("Aim to include: ${q.criteria}", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }

    if (confirm) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text("Submit with unanswered questions?") },
            text = { Text("${assessment.questions.size - done} question(s) have no answer. They will count as wrong or blank.") },
            confirmButton = { androidx.compose.material3.TextButton(onClick = { confirm = false; submit() }) { Text("Submit") } },
            dismissButton = { androidx.compose.material3.TextButton(onClick = { confirm = false }) { Text("Keep going") } },
        )
    }
}

// --- Results -----------------------------------------------------------------------------------

/** Score and explanations for objective questions; AI feedback (clearly not a grade) for written ones. */
@Composable
fun QuizResultScreen(studyId: String, assessmentId: String, modifier: Modifier = Modifier) {
    val store = LocalStudyStore.current
    val nav = LocalAppNavigator.current
    val aiStore = LocalAiSettings.current
    val colors = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    val ai = remember { StudyAi() }
    val assessment = store.assessment(assessmentId)

    if (assessment == null) {
        ScreenScaffold("Result", onBack = { nav.back() }, modifier = modifier) {
            ErrorState(message = "This result could not be found.", onRetry = { nav.back() }, title = "Result not found")
        }
        return
    }
    if (!assessment.submitted) {
        LaunchedEffect(assessmentId) { nav.replaceTop(Screen.Quiz(studyId, assessmentId)) }
        return
    }

    val written = assessment.questions.filter { !it.isObjective }
    val needsFeedback = written.isNotEmpty() && assessment.overallFeedback.isBlank() &&
        assessment.answers.none { it.feedback.isNotBlank() }
    var working by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val hasKey = aiStore.settings.hasKey

    fun getFeedback() {
        if (working) return
        working = true
        error = null
        scope.launch {
            val current = store.assessment(assessmentId) ?: return@launch
            when (val out = ai.feedback(aiStore.settings, current)) {
                is StudyAiOutcome.Ok -> {
                    val fb = out.value
                    val answers = current.questions.filter { !it.isObjective }.map { q ->
                        val old = current.answerFor(q.id) ?: AssessmentAnswer(q.id)
                        old.copy(feedback = fb.feedback[q.id].orEmpty())
                    }
                    val merged = current.answers.filterNot { a -> answers.any { it.questionId == a.questionId } } + answers
                    store.saveAssessment(
                        current.copy(
                            answers = merged,
                            overallFeedback = fb.overall.ifBlank { "Feedback is below." },
                            weakTopics = (current.weakTopics + fb.weakTopics).distinct().take(6),
                        ),
                    )
                }
                is StudyAiOutcome.Err -> error = out.message
            }
            working = false
        }
    }

    LaunchedEffect(assessmentId, needsFeedback) { if (needsFeedback && hasKey) getFeedback() }

    ScreenScaffold(title = "Result", onBack = { nav.back() }, modifier = modifier) {
        Text(assessment.title, style = MaterialTheme.typography.titleMedium, color = colors.onBackground)
        Spacer(Modifier.height(12.dp))

        if (assessment.total > 0) {
            StudyZenCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ProgressRing(assessment.percent / 100f, size = 64.dp, color = colors.primary)
                    Column(Modifier.padding(start = 16.dp)) {
                        Text("${assessment.correct} of ${assessment.total} correct", style = MaterialTheme.typography.titleMedium, color = colors.onSurface)
                        Text("${assessment.percent}%", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        if (assessment.weakTopics.isNotEmpty()) {
            Text("Worth revising", style = MaterialTheme.typography.labelLarge, color = colors.onBackground)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { assessment.weakTopics.take(3).forEach { TagChip(it) } }
            if (assessment.weakTopics.size > 3) {
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { assessment.weakTopics.drop(3).forEach { TagChip(it) } }
            }
            Spacer(Modifier.height(12.dp))
        }

        if (written.isNotEmpty()) {
            if (working) {
                StudyZenCard {
                    Text("Reading your answers…", style = MaterialTheme.typography.titleSmall, color = colors.onSurface)
                    Text("Writing feedback. This can take up to a minute.", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                }
                Spacer(Modifier.height(12.dp))
            }
            error?.let {
                InfoBanner(it, tone = BannerTone.Error)
                StudyZenTextButton("Try again", onClick = { getFeedback() })
                Spacer(Modifier.height(8.dp))
            }
            if (needsFeedback && !hasKey) {
                InfoBanner("Add your AI key to get feedback on written answers. Example answers are shown below.")
                StudyZenTextButton("Add an AI key", onClick = { nav.navigate(Screen.AiSettings) })
            }
            if (assessment.overallFeedback.isNotBlank()) {
                StudyZenCard {
                    TagChip("AI feedback")
                    Spacer(Modifier.height(6.dp))
                    Text(assessment.overallFeedback, style = MaterialTheme.typography.bodyMedium, color = colors.onSurface)
                    Text(
                        "This is guidance from an AI, not an official grade. It can be wrong.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
                Spacer(Modifier.height(12.dp))
            }
        }

        assessment.questions.forEachIndexed { index, q ->
            val answer = assessment.answerFor(q.id)
            StudyZenCard {
                Text("Question ${index + 1}", style = MaterialTheme.typography.labelLarge, color = colors.primary)
                Text(q.prompt, style = MaterialTheme.typography.bodyLarge, color = colors.onSurface, modifier = Modifier.padding(top = 4.dp))
                Spacer(Modifier.height(8.dp))
                if (q.isObjective) {
                    val picked = answer?.choice ?: -1
                    val right = picked == q.correctIndex
                    TagChip(if (right) "Correct" else if (picked < 0) "Not answered" else "Not quite")
                    Spacer(Modifier.height(6.dp))
                    if (picked >= 0 && !right) {
                        Text("You chose: ${q.options[picked]}", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                    }
                    Text("Answer: ${q.options[q.correctIndex]}", style = MaterialTheme.typography.bodyMedium, color = colors.onSurface)
                    if (q.explanation.isNotBlank()) {
                        Text(q.explanation, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                    }
                } else {
                    Text("Your answer", style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
                    Text(answer?.text?.ifBlank { "(left blank)" } ?: "(left blank)", style = MaterialTheme.typography.bodyMedium, color = colors.onSurface)
                    if (!answer?.feedback.isNullOrBlank()) {
                        Spacer(Modifier.height(8.dp))
                        TagChip("AI feedback")
                        Text(answer?.feedback.orEmpty(), style = MaterialTheme.typography.bodyMedium, color = colors.onSurface, modifier = Modifier.padding(top = 4.dp))
                    }
                    if (q.criteria.isNotBlank()) {
                        Text("A good answer includes: ${q.criteria}", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
                    }
                    if (q.explanation.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text("Example answer (AI-written)", style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
                        AnswerText(q.explanation)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        StudyZenButton("Practice again", onClick = { nav.replaceTop(Screen.QuizSetup(studyId, assessment.lessonId)) })
        StudyZenTextButton("Back to study", onClick = { nav.back() })
        Spacer(Modifier.height(16.dp))
    }
}
