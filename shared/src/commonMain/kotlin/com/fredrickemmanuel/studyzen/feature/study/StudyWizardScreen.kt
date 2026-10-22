package com.fredrickemmanuel.studyzen.feature.study

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.fredrickemmanuel.studyzen.core.Ids
import com.fredrickemmanuel.studyzen.core.designsystem.components.BannerTone
import com.fredrickemmanuel.studyzen.core.designsystem.components.DateStepper
import com.fredrickemmanuel.studyzen.core.designsystem.components.DayChips
import com.fredrickemmanuel.studyzen.core.designsystem.components.FilterPill
import com.fredrickemmanuel.studyzen.core.designsystem.components.InfoBanner
import com.fredrickemmanuel.studyzen.core.designsystem.components.ScreenScaffold
import com.fredrickemmanuel.studyzen.core.designsystem.components.SectionHeader
import com.fredrickemmanuel.studyzen.core.designsystem.components.Stepper
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
import com.fredrickemmanuel.studyzen.core.time.nowMinuteOfDay
import com.fredrickemmanuel.studyzen.core.time.todayEpochDay
import com.fredrickemmanuel.studyzen.data.LocalStudyStore
import com.fredrickemmanuel.studyzen.data.ai.AiFailure
import com.fredrickemmanuel.studyzen.data.ai.LocalAiSettings
import com.fredrickemmanuel.studyzen.data.ai.StudyAi
import com.fredrickemmanuel.studyzen.data.ai.StudyAiOutcome
import com.fredrickemmanuel.studyzen.data.ai.StudyBrief
import com.fredrickemmanuel.studyzen.data.mock.MockAi
import com.fredrickemmanuel.studyzen.domain.KnowledgeLevel
import com.fredrickemmanuel.studyzen.domain.LearningModule
import com.fredrickemmanuel.studyzen.domain.PlanGenerator
import com.fredrickemmanuel.studyzen.domain.Reminder
import com.fredrickemmanuel.studyzen.domain.Study
import com.fredrickemmanuel.studyzen.domain.StudyPlan
import com.fredrickemmanuel.studyzen.domain.StudyPurpose
import com.fredrickemmanuel.studyzen.domain.StudyTopic
import com.fredrickemmanuel.studyzen.domain.Subject
import com.fredrickemmanuel.studyzen.feature.reminders.LocalReminderPlatform
import com.fredrickemmanuel.studyzen.navigation.LocalAppNavigator
import com.fredrickemmanuel.studyzen.navigation.Screen
import kotlinx.coroutines.launch

private enum class PlanChoice { Attach, Create, None }

private val STEP_TITLES = listOf("About your study", "Schedule and plan", "Learning path", "Review")

/** Everything the learner has typed so far. It lives for the whole wizard, so going back loses nothing. */
private class WizardState(today: Long, defaultDays: Set<DayOfWeek>, defaultStart: Int) {
    var step by mutableIntStateOf(0)

    // 1. About
    var title by mutableStateOf("")
    var purpose by mutableStateOf(StudyPurpose.Skill)
    var description by mutableStateOf("")
    var level by mutableStateOf(KnowledgeLevel.Beginner)
    var subject by mutableStateOf(Subject.Other)
    var curriculum by mutableStateOf("")
    var focus by mutableStateOf("")
    var hasDeadline by mutableStateOf(false)
    var deadline by mutableStateOf(today + 30)
    var titleError by mutableStateOf<String?>(null)

    // 2. Plan
    var planChoice by mutableStateOf(PlanChoice.None)
    var attachPlanId by mutableStateOf<String?>(null)
    var planName by mutableStateOf("")
    var planStart by mutableStateOf(today)
    var planEnd by mutableStateOf(today + 27)
    var planDays by mutableStateOf(defaultDays)
    var planStartMinute by mutableIntStateOf(defaultStart)
    var planMinutes by mutableIntStateOf(45)
    var planReminder by mutableStateOf(true)
    var planError by mutableStateOf<String?>(null)

    // 3. Learning path
    var wantAiPath by mutableStateOf(true)
    var guided by mutableStateOf(false)
    var modules by mutableStateOf<List<LearningModule>?>(null)
    var aiMade by mutableStateOf(false)
    var generating by mutableStateOf(false)
    var pathError by mutableStateOf<String?>(null)
    var pathErrorKind by mutableStateOf<AiFailure?>(null)
    var providerUsed by mutableStateOf("")
}

/** Creates a Study step by step: what, why, schedule and plan, then an optional AI learning path. */
@Composable
fun StudyWizardScreen(modifier: Modifier = Modifier) {
    val store = LocalStudyStore.current
    val nav = LocalAppNavigator.current
    val aiStore = LocalAiSettings.current
    val reminderStatus = LocalReminderPlatform.current.status
    val colors = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    val ai = remember { StudyAi() }
    val today = remember { todayEpochDay() }

    val w = remember {
        WizardState(today, store.profile.studyDays, store.profile.preferredStartMinute)
    }
    val availablePlans = store.plans.toList()

    fun buildPlan(studyId: String): StudyPlan {
        val blocks = w.planDays.associateWith {
            listOf(com.fredrickemmanuel.studyzen.domain.TimeBlock(w.planStartMinute, w.planStartMinute + w.planMinutes))
        }
        return StudyPlan(
            id = Ids.next("plan"),
            name = w.planName.trim().ifEmpty { "${w.title.trim()} plan" },
            goal = w.title.trim(),
            studyIds = listOf(studyId),
            startEpochDay = w.planStart,
            endEpochDay = w.planEnd,
            weeklyBlocks = blocks,
            reminder = Reminder(enabled = w.planReminder),
        )
    }

    /** Checks the plan form. Returns a message, or null when it is fine. */
    fun planProblem(): String? = when {
        w.planChoice == PlanChoice.Attach && w.attachPlanId == null -> "Choose which plan to attach this study to."
        w.planChoice != PlanChoice.Create -> null
        w.planDays.isEmpty() -> "Choose at least one study day."
        w.planEnd < w.planStart -> "The end date must be on or after the start date."
        w.planStartMinute + w.planMinutes > 24 * 60 -> "That session would run past midnight. Start earlier or make it shorter."
        else -> null
    }

    fun brief(): StudyBrief {
        var sessions: Int? = null
        var minutes: Int? = null
        if (w.planChoice == PlanChoice.Create && planProblem() == null) {
            sessions = PlanGenerator.slots(buildPlan("preview"), today).size
            minutes = w.planMinutes
        }
        return StudyBrief(
            title = w.title.trim(),
            description = w.description.trim(),
            purpose = w.purpose,
            curriculum = w.curriculum.trim(),
            focus = w.focus.trim(),
            level = w.level,
            daysUntilDeadline = if (w.hasDeadline) (w.deadline - today).toInt().coerceAtLeast(1) else null,
            sessions = sessions,
            minutesPerSession = minutes,
        )
    }

    fun generate() {
        if (w.generating) return
        w.generating = true
        w.pathError = null
        scope.launch {
            when (val outcome = ai.proposeStructure(aiStore.settings, brief())) {
                is StudyAiOutcome.Ok -> {
                    w.modules = outcome.value
                    w.aiMade = true
                    w.providerUsed = outcome.provider
                }
                is StudyAiOutcome.Err -> {
                    w.pathError = outcome.message
                    w.pathErrorKind = outcome.kind
                }
            }
            w.generating = false
        }
    }

    fun create() {
        val studyId = Ids.next("study")
        val deadline = if (w.hasDeadline && w.purpose.deadlineLabel != null) w.deadline else null
        val curriculum = if (w.purpose.curriculumLabel != null) w.curriculum.trim() else ""
        val objective = if (curriculum.isNotEmpty()) "${w.purpose.label}: $curriculum" else w.purpose.label
        val modules = if (w.wantAiPath) w.modules.orEmpty() else emptyList()
        val study = Study(
            id = studyId,
            title = w.title.trim(),
            description = w.description.trim(),
            objective = objective,
            subject = w.subject,
            level = w.level,
            modules = modules,
            createdAtEpochDay = today,
            purpose = w.purpose,
            curriculum = curriculum,
            focus = w.focus.trim(),
            deadlineEpochDay = deadline,
            guided = w.guided && modules.isNotEmpty(),
            aiStructured = w.wantAiPath && w.aiMade,
        )
        store.addStudy(study)

        var needsPermission = false
        when (w.planChoice) {
            PlanChoice.Attach -> w.attachPlanId?.let { store.attachStudyToPlan(studyId, it) }
            PlanChoice.Create -> {
                val plan = buildPlan(studyId)
                val topics = if (study.allLessons.isNotEmpty()) {
                    study.allLessons.map { StudyTopic(it.title, studyId, study.subject) }
                } else {
                    listOf(StudyTopic(study.title, studyId, study.subject))
                }
                val sessions = store.suggestSessions(plan, topics)
                store.commitPlan(plan, sessions)
                needsPermission = plan.reminder.enabled && reminderStatus.supported && !reminderStatus.ready
            }
            PlanChoice.None -> Unit
        }
        nav.replaceTop(Screen.StudyDetail(studyId))
        if (needsPermission) nav.navigate(Screen.NotificationPermission)
    }

    fun next() {
        when (w.step) {
            0 -> {
                if (w.title.isBlank()) {
                    w.titleError = "Give your study a name"
                    return
                }
                if (w.planName.isBlank()) w.planName = "${w.title.trim()} plan"
                if (w.hasDeadline && w.purpose.deadlineLabel != null) w.planEnd = maxOf(w.planStart, minOf(w.deadline, w.planStart + 180))
                w.step = 1
            }
            1 -> {
                val problem = planProblem()
                w.planError = problem
                if (problem == null) w.step = 2
            }
            2 -> {
                if (w.wantAiPath && w.modules.isNullOrEmpty()) {
                    w.pathError = "Build the learning path first, or turn it off to continue without one."
                    return
                }
                w.step = 3
            }
            else -> create()
        }
    }

    fun back() {
        if (w.step == 0) nav.back() else w.step -= 1
    }

    ScreenScaffold(
        title = "New study",
        onBack = { back() },
        modifier = modifier,
        bottomBar = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (w.step > 0) {
                    StudyZenTextButton("Back", onClick = { back() })
                }
                StudyZenButton(
                    text = if (w.step == 3) "Create study" else "Next",
                    onClick = { next() },
                    enabled = !w.generating,
                    modifier = Modifier.weight(1f),
                )
            }
        },
    ) {
        StepDots(current = w.step, total = STEP_TITLES.size)
        Spacer(Modifier.height(12.dp))
        Text(STEP_TITLES[w.step], style = MaterialTheme.typography.titleLarge, color = colors.onBackground)
        Spacer(Modifier.height(12.dp))

        when (w.step) {
            0 -> AboutStep(w)
            1 -> PlanStep(w, availablePlans)
            2 -> PathStep(w, hasKey = aiStore.settings.hasKey, onGenerate = { generate() }, onOpenAiSettings = { nav.navigate(Screen.AiSettings) })
            else -> ReviewStep(w)
        }
        Spacer(Modifier.height(16.dp))
    }
}

// --- Step 1 ---------------------------------------------------------------------------------------

@Composable
private fun AboutStep(w: WizardState) {
    val colors = MaterialTheme.colorScheme
    StudyZenTextField(
        value = w.title,
        onValueChange = { w.title = it; w.titleError = null },
        label = "What do you want to study?",
        placeholder = "e.g. Organic chemistry, Android development",
        error = w.titleError,
    )

    Spacer(Modifier.height(16.dp))
    Text("Why are you studying it?", style = MaterialTheme.typography.labelLarge, color = colors.onBackground)
    Spacer(Modifier.height(8.dp))
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        StudyPurpose.entries.forEach { p ->
            ChoiceRow(p.label, selected = w.purpose == p, onClick = { w.purpose = p })
        }
    }

    // questions that only make sense for some purposes
    val curriculumLabel = w.purpose.curriculumLabel
    if (curriculumLabel != null) {
        Spacer(Modifier.height(16.dp))
        StudyZenTextField(
            value = w.curriculum,
            onValueChange = { w.curriculum = it },
            label = "$curriculumLabel (optional)",
            placeholder = w.purpose.curriculumHint,
        )
    }
    val deadlineLabel = w.purpose.deadlineLabel
    if (deadlineLabel != null) {
        Spacer(Modifier.height(8.dp))
        SwitchRow(
            title = "I have a date for this",
            subtitle = "Your ${deadlineLabel.lowercase()}. Plans and lessons can be paced to reach it.",
            checked = w.hasDeadline,
            onCheckedChange = { w.hasDeadline = it },
        )
        if (w.hasDeadline) DateStepper(deadlineLabel, w.deadline, onChange = { w.deadline = maxOf(it, todayEpochDay()) })
    }

    Spacer(Modifier.height(16.dp))
    StudyZenTextField(
        value = w.description,
        onValueChange = { w.description = it },
        label = "Tell us more (optional)",
        placeholder = "What do you already know? What do you want to be able to do? Anything you find hard?",
        singleLine = false,
        minLines = 5,
    )
    Spacer(Modifier.height(16.dp))
    StudyZenTextField(
        value = w.focus,
        onValueChange = { w.focus = it },
        label = "Areas to focus on (optional)",
        placeholder = "e.g. mechanisms, past questions, system design",
        singleLine = false,
        minLines = 2,
    )

    Spacer(Modifier.height(16.dp))
    Text("Your level", style = MaterialTheme.typography.labelLarge, color = colors.onBackground)
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        KnowledgeLevel.entries.forEach { FilterPill(it.label, w.level == it, { w.level = it }) }
    }
    Spacer(Modifier.height(16.dp))
    Text("Subject area", style = MaterialTheme.typography.labelLarge, color = colors.onBackground)
    Spacer(Modifier.height(8.dp))
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Subject.entries.forEach { FilterPill(it.label, w.subject == it, { w.subject = it }) }
    }
}

// --- Step 2 ---------------------------------------------------------------------------------------

@Composable
private fun PlanStep(w: WizardState, plans: List<StudyPlan>) {
    val colors = MaterialTheme.colorScheme
    Text(
        "A study plan sets the days and times you study and reminds you. It is optional: you can study at your own pace without one.",
        style = MaterialTheme.typography.bodyMedium,
        color = colors.onSurfaceVariant,
    )
    Spacer(Modifier.height(12.dp))
    w.planError?.let {
        InfoBanner(it, tone = BannerTone.Error)
        Spacer(Modifier.height(12.dp))
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ChoiceRow(
            "Create a new plan for this study",
            selected = w.planChoice == PlanChoice.Create,
            onClick = { w.planChoice = PlanChoice.Create; w.planError = null },
            subtitle = "Pick study days and a time. Reminders included.",
        )
        ChoiceRow(
            "Attach to a plan I already have",
            selected = w.planChoice == PlanChoice.Attach,
            enabled = plans.isNotEmpty(),
            onClick = { w.planChoice = PlanChoice.Attach; w.planError = null },
            subtitle = if (plans.isEmpty()) "You have no plans yet." else "A study can be in one plan at a time.",
        )
        ChoiceRow(
            "No plan, I will study at my own pace",
            selected = w.planChoice == PlanChoice.None,
            onClick = { w.planChoice = PlanChoice.None; w.planError = null },
            subtitle = "You can attach a plan later.",
        )
    }

    Spacer(Modifier.height(16.dp))
    when (w.planChoice) {
        PlanChoice.Attach -> {
            SectionHeader("Choose a plan")
            Spacer(Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                plans.forEach { plan ->
                    ChoiceRow(
                        plan.name,
                        selected = w.attachPlanId == plan.id,
                        onClick = { w.attachPlanId = plan.id; w.planError = null },
                        subtitle = "${plan.studyDays.joinToString(" ") { it.short }} · ${plan.studyIds.size} studies",
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Open the plan afterwards to add this study to its schedule.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
        }
        PlanChoice.Create -> {
            StudyZenTextField(
                value = w.planName,
                onValueChange = { w.planName = it },
                label = "Plan name",
                placeholder = "${w.title.trim().ifEmpty { "My study" }} plan",
            )
            Spacer(Modifier.height(12.dp))
            DateStepper("Starts", w.planStart, onChange = { w.planStart = maxOf(it, todayEpochDay()); if (w.planEnd < w.planStart) w.planEnd = w.planStart })
            DateStepper("Ends", w.planEnd, onChange = { w.planEnd = maxOf(it, w.planStart) })
            Spacer(Modifier.height(12.dp))
            Text("Study days", style = MaterialTheme.typography.labelLarge, color = colors.onBackground)
            Spacer(Modifier.height(8.dp))
            DayChips(w.planDays, onToggle = { day ->
                w.planDays = if (day in w.planDays) w.planDays - day else w.planDays + day
                w.planError = null
            })
            Spacer(Modifier.height(12.dp))
            TimeStepper("Start time", w.planStartMinute, onChange = { w.planStartMinute = it })
            Stepper(
                label = "Length of each session",
                valueText = "${w.planMinutes} min",
                onMinus = { w.planMinutes = (w.planMinutes - 15).coerceAtLeast(15) },
                onPlus = { w.planMinutes = (w.planMinutes + 15).coerceAtMost(240) },
            )
            Spacer(Modifier.height(8.dp))
            SwitchRow(
                title = "Remind me",
                subtitle = "A reminder before each session. You can change this any time.",
                checked = w.planReminder,
                onCheckedChange = { w.planReminder = it },
            )
            Text(
                "Same time on each chosen day. You can fine-tune each day later in the plan.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
        }
        PlanChoice.None -> Unit
    }
}

// --- Step 3 ---------------------------------------------------------------------------------------

@Composable
private fun PathStep(w: WizardState, hasKey: Boolean, onGenerate: () -> Unit, onOpenAiSettings: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    var renaming by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var renameText by remember { mutableStateOf("") }

    SwitchRow(
        title = "Build a learning path",
        subtitle = "Modules and lessons in a sensible order, shaped by your topic, purpose, level and schedule. You review it before it is saved.",
        checked = w.wantAiPath,
        onCheckedChange = { w.wantAiPath = it; w.pathError = null },
    )
    if (!w.wantAiPath) {
        Spacer(Modifier.height(8.dp))
        Text(
            "No problem. You will get a simple, flexible study space, and you can build a path later.",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
        )
        return
    }

    SwitchRow(
        title = "Open lessons in order",
        subtitle = "Off: open any lesson any time. On: the next lesson opens when you finish the one before.",
        checked = w.guided,
        onCheckedChange = { w.guided = it },
    )
    Spacer(Modifier.height(12.dp))

    w.pathError?.let { message ->
        InfoBanner(message, tone = BannerTone.Error)
        if (w.pathErrorKind in listOf(AiFailure.NoKey, AiFailure.BadKey, AiFailure.ModelProblem, AiFailure.RateLimited)) {
            StudyZenTextButton("AI settings", onClick = onOpenAiSettings)
        }
        Spacer(Modifier.height(8.dp))
    }

    val modules = w.modules
    if (modules.isNullOrEmpty()) {
        if (!hasKey) {
            InfoBanner("A tailored path needs your own free AI key. Add one, or use a basic outline you can edit later.")
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StudyZenTextButton("Add an AI key", onClick = onOpenAiSettings)
                StudyZenTextButton("Use a basic outline", onClick = {
                    w.modules = MockAi.learningPath(w.title.trim(), w.subject, w.level)
                    w.aiMade = false
                    w.pathError = null
                })
            }
        } else {
            StudyZenButton("Build my learning path", onClick = onGenerate, loading = w.generating)
            if (w.generating) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Working out the best order for your goal. This can take up to a minute.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }
        }
        return
    }

    // review and adjust
    val total = modules.sumOf { it.lessons.size }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            "${modules.size} modules · $total lessons" + if (w.aiMade) " · by ${w.providerUsed}" else " · basic outline",
            style = MaterialTheme.typography.labelLarge,
            color = colors.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        if (hasKey) StudyZenTextButton(if (w.generating) "Working…" else "Regenerate", enabled = !w.generating, onClick = onGenerate)
    }
    Text(
        "Tap a lesson to rename it, or remove what you do not need.",
        style = MaterialTheme.typography.bodySmall,
        color = colors.onSurfaceVariant,
    )
    Spacer(Modifier.height(8.dp))
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        modules.forEachIndexed { mi, module ->
            StudyZenCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(module.title, style = MaterialTheme.typography.titleSmall, color = colors.onSurface, modifier = Modifier.weight(1f))
                    StudyZenTextButton("Remove", onClick = { w.modules = modules.filterIndexed { i, _ -> i != mi } })
                }
                module.lessons.forEachIndexed { li, lesson ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { renaming = mi to li; renameText = lesson.title }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(lesson.title, style = MaterialTheme.typography.bodyMedium, color = colors.onSurface)
                            if (lesson.summary.isNotBlank()) {
                                Text(lesson.summary, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                            }
                        }
                        if (lesson.kind != com.fredrickemmanuel.studyzen.domain.LessonKind.Learn) TagChip(lesson.kind.label)
                        StudyZenTextButton("✕", onClick = {
                            w.modules = modules.mapIndexed { i, m ->
                                if (i == mi) m.copy(lessons = m.lessons.filterIndexed { j, _ -> j != li }) else m
                            }.filter { it.lessons.isNotEmpty() }
                        })
                    }
                }
            }
        }
    }

    renaming?.let { (mi, li) ->
        AlertDialog(
            onDismissRequest = { renaming = null },
            title = { Text("Rename lesson") },
            text = { StudyZenTextField(value = renameText, onValueChange = { renameText = it }, label = "Lesson title") },
            confirmButton = {
                TextButton(onClick = {
                    val clean = renameText.trim()
                    if (clean.isNotEmpty()) {
                        w.modules = modules.mapIndexed { i, m ->
                            if (i == mi) m.copy(lessons = m.lessons.mapIndexed { j, l -> if (j == li) l.copy(title = clean) else l }) else m
                        }
                    }
                    renaming = null
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { renaming = null }) { Text("Cancel") } },
        )
    }
}

// --- Step 4 ---------------------------------------------------------------------------------------

@Composable
private fun ReviewStep(w: WizardState) {
    val colors = MaterialTheme.colorScheme
    StudyZenCard {
        Text(w.title.trim(), style = MaterialTheme.typography.titleMedium, color = colors.onSurface)
        Text(
            "${w.purpose.label} · ${w.level.label} · ${w.subject.label}",
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp),
        )
        if (w.purpose.curriculumLabel != null && w.curriculum.isNotBlank()) {
            SummaryLine(w.purpose.curriculumLabel!!, w.curriculum.trim())
        }
        if (w.hasDeadline && w.purpose.deadlineLabel != null) {
            SummaryLine(w.purpose.deadlineLabel!!, CivilDate.fromEpochDay(w.deadline).shortLabel())
        }
        if (w.focus.isNotBlank()) SummaryLine("Focus", w.focus.trim())
    }
    Spacer(Modifier.height(12.dp))
    StudyZenCard {
        when (w.planChoice) {
            PlanChoice.Create -> {
                Text("New plan: ${w.planName.trim().ifEmpty { "${w.title.trim()} plan" }}", style = MaterialTheme.typography.titleSmall, color = colors.onSurface)
                SummaryLine("Days", w.planDays.sortedBy { it.ordinal }.joinToString(" ") { it.short })
                SummaryLine("Time", "${formatClock(w.planStartMinute)} for ${w.planMinutes} min")
                SummaryLine("Dates", "${CivilDate.fromEpochDay(w.planStart).shortLabel()} to ${CivilDate.fromEpochDay(w.planEnd).shortLabel()}")
                SummaryLine("Reminders", if (w.planReminder) "On" else "Off")
            }
            PlanChoice.Attach -> {
                val store = LocalStudyStore.current
                Text("Attached to: ${store.plan(w.attachPlanId)?.name ?: "a plan"}", style = MaterialTheme.typography.titleSmall, color = colors.onSurface)
            }
            PlanChoice.None -> Text("No study plan. You study at your own pace.", style = MaterialTheme.typography.titleSmall, color = colors.onSurface)
        }
    }
    Spacer(Modifier.height(12.dp))
    StudyZenCard {
        val modules = if (w.wantAiPath) w.modules.orEmpty() else emptyList()
        if (modules.isEmpty()) {
            Text("A simple, flexible study space", style = MaterialTheme.typography.titleSmall, color = colors.onSurface)
            Text("No lessons yet. You can build a learning path any time.", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        } else {
            Text("${modules.size} modules · ${modules.sumOf { it.lessons.size }} lessons", style = MaterialTheme.typography.titleSmall, color = colors.onSurface)
            Text(
                if (w.guided) "Lessons open in order." else "You can open any lesson.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
        }
    }
}

// --- Small pieces --------------------------------------------------------------------------------

@Composable
private fun SummaryLine(label: String, value: String) {
    val colors = MaterialTheme.colorScheme
    Row(Modifier.padding(top = 4.dp)) {
        Text("$label: ", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, color = colors.onSurface)
    }
}

@Composable
private fun StepDots(current: Int, total: Int) {
    val colors = MaterialTheme.colorScheme
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
        repeat(total) { i ->
            Box(
                Modifier
                    .weight(1f)
                    .height(5.dp)
                    .clip(CircleShape)
                    .background(if (i <= current) colors.primary else colors.outlineVariant),
            )
        }
    }
}

@Composable
internal fun ChoiceRow(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    enabled: Boolean = true,
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (selected) colors.primaryContainer else colors.surface)
            .border(1.dp, if (selected) colors.primary else colors.outlineVariant, shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(20.dp)
                .clip(CircleShape)
                .border(2.dp, if (enabled) colors.primary else colors.outlineVariant, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) Box(Modifier.size(10.dp).clip(CircleShape).background(colors.primary))
        }
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) colors.onSurface else colors.onSurfaceVariant,
            )
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
        }
    }
}
