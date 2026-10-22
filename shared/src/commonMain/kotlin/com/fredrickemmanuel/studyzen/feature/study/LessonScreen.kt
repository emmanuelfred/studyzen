package com.fredrickemmanuel.studyzen.feature.study

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fredrickemmanuel.studyzen.core.designsystem.components.BannerTone
import com.fredrickemmanuel.studyzen.core.designsystem.components.ErrorState
import com.fredrickemmanuel.studyzen.core.designsystem.components.FilterPill
import com.fredrickemmanuel.studyzen.core.designsystem.components.InfoBanner
import com.fredrickemmanuel.studyzen.core.designsystem.components.ScreenScaffold
import com.fredrickemmanuel.studyzen.core.designsystem.components.SectionHeader
import com.fredrickemmanuel.studyzen.core.designsystem.components.StudyZenButton
import com.fredrickemmanuel.studyzen.core.designsystem.components.StudyZenCard
import com.fredrickemmanuel.studyzen.core.designsystem.components.StudyZenTextButton
import com.fredrickemmanuel.studyzen.core.designsystem.components.TagChip
import com.fredrickemmanuel.studyzen.data.LocalStudyStore
import com.fredrickemmanuel.studyzen.data.ai.LocalAiSettings
import com.fredrickemmanuel.studyzen.data.ai.StudyAi
import com.fredrickemmanuel.studyzen.domain.LessonKind
import com.fredrickemmanuel.studyzen.domain.ResourceSource
import com.fredrickemmanuel.studyzen.feature.ai.AnswerText
import com.fredrickemmanuel.studyzen.navigation.LocalAppNavigator
import com.fredrickemmanuel.studyzen.navigation.Screen
import com.fredrickemmanuel.studyzen.navigation.StudyTool
import kotlinx.coroutines.launch

/** One lesson: what it covers, materials on request, tools that know the lesson, and previous/next. */
@Composable
fun LessonScreen(studyId: String, lessonId: String, modifier: Modifier = Modifier) {
    val store = LocalStudyStore.current
    val nav = LocalAppNavigator.current
    val aiStore = LocalAiSettings.current
    val colors = MaterialTheme.colorScheme
    val uri = LocalUriHandler.current
    val scope = rememberCoroutineScope()
    val ai = remember { StudyAi() }

    val study = store.study(studyId)
    val lesson = study?.lesson(lessonId)
    if (study == null || lesson == null) {
        ScreenScaffold("Lesson", onBack = { nav.back() }, modifier = modifier) {
            ErrorState(message = "This lesson could not be found. The learning path may have changed.", onRetry = { nav.back() }, title = "Lesson not found")
        }
        return
    }

    // remember where the learner stopped
    LaunchedEffect(lessonId) { if (study.canOpen(lessonId)) store.setLastLesson(studyId, lessonId) }

    var loading by remember(lessonId) { mutableStateOf(false) }
    var aiError by remember(lessonId) { mutableStateOf<String?>(null) }
    var offline by remember(lessonId) { mutableStateOf(false) }

    val module = study.moduleOf(lessonId)
    val previous = study.previousOf(lessonId)
    val next = study.nextOf(lessonId)
    val open = study.canOpen(lessonId)
    val material = lesson.material

    fun fetch() {
        if (loading) return
        loading = true
        aiError = null
        scope.launch {
            val result = ai.lessonMaterial(aiStore.settings, study, lesson)
            // keep bookmarks the learner already made
            val kept = lesson.material?.resources.orEmpty().filter { it.bookmarked }.map { it.url }.toSet()
            val resources = result.material.resources.map { if (it.url in kept) it.copy(bookmarked = true) else it }
            val explanation = result.material.explanation.ifBlank { lesson.material?.explanation.orEmpty() }
            val by = if (result.material.explanation.isBlank()) lesson.material?.explanationBy.orEmpty() else result.material.explanationBy
            store.setLessonMaterial(studyId, lessonId, result.material.copy(resources = resources, explanation = explanation, explanationBy = by))
            aiError = result.aiError
            offline = result.sourcesOffline
            loading = false
        }
    }

    ScreenScaffold(title = module?.title ?: "Lesson", onBack = { nav.back() }, modifier = modifier) {
        if (!open) {
            InfoBanner("Finish the earlier lessons first. This path opens lessons in order.", tone = BannerTone.Warning)
            Spacer(Modifier.height(12.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(lesson.title, style = MaterialTheme.typography.titleLarge, color = colors.onBackground, modifier = Modifier.weight(1f))
            if (lesson.kind != LessonKind.Learn) TagChip(lesson.kind.label)
        }
        if (lesson.summary.isNotBlank()) {
            Text(lesson.summary, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
        }
        if (lesson.objectives.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            StudyZenCard {
                Text("By the end you can", style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant)
                lesson.objectives.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium, color = colors.onSurface, modifier = Modifier.padding(top = 4.dp)) }
            }
        }

        Spacer(Modifier.height(16.dp))
        Text("Tools for this lesson", style = MaterialTheme.typography.labelLarge, color = colors.onBackground)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterPill("Ask AI & scan", false, { nav.navigate(Screen.ToolHistory(studyId, StudyTool.Chat, lessonId)) })
            FilterPill("Notes", false, { nav.navigate(Screen.ToolHistory(studyId, StudyTool.Notes, lessonId)) })
            FilterPill("Research", false, { nav.navigate(Screen.ToolHistory(studyId, StudyTool.Research, lessonId)) })
            FilterPill("Dictionary", false, { nav.navigate(Screen.ToolHistory(studyId, StudyTool.Dictionary, lessonId)) })
            FilterPill("Calculator", false, { nav.navigate(Screen.ToolHistory(studyId, StudyTool.Calculator, lessonId)) })
            FilterPill("Practice", false, { nav.navigate(Screen.QuizSetup(studyId, lessonId)) })
        }

        Spacer(Modifier.height(20.dp))
        SectionHeader("Materials")
        Spacer(Modifier.height(8.dp))
        aiError?.let { InfoBanner(it, tone = BannerTone.Error); Spacer(Modifier.height(8.dp)) }
        if (offline) {
            InfoBanner("Could not reach Wikipedia, so only search links are shown. Check your connection and refresh.", tone = BannerTone.Warning)
            Spacer(Modifier.height(8.dp))
        }

        if (material == null) {
            StudyZenCard {
                Text("Get materials when you are ready", style = MaterialTheme.typography.titleSmall, color = colors.onSurface)
                Text(
                    "An explanation at your level, real Wikipedia articles, and search links for videos, guides and practice questions.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(8.dp))
            StudyZenButton("Get materials", onClick = { fetch() }, loading = loading, enabled = open)
            if (!aiStore.settings.hasKey) {
                Text(
                    "Without an AI key you still get the sources. Add a key in AI settings for the explanation.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        } else {
            if (material.explanation.isNotBlank()) {
                StudyZenCard {
                    TagChip("AI-generated" + if (material.explanationBy.isNotBlank()) " · ${material.explanationBy}" else "")
                    Spacer(Modifier.height(8.dp))
                    AnswerText(material.explanation)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Written by an AI from the lesson title, so check important facts against the sources below.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(12.dp))
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                material.resources.forEach { r ->
                    StudyZenCard(onClick = { uri.openUri(r.url) }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(r.title, style = MaterialTheme.typography.titleSmall, color = colors.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Text(
                                    if (r.source == ResourceSource.Wikipedia) "Wikipedia · article found for you"
                                    else "Search link · opens a search, not a single page",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.onSurfaceVariant,
                                )
                                if (r.snippet.isNotBlank()) {
                                    Text(r.snippet, style = MaterialTheme.typography.bodySmall, color = colors.onSurface, maxLines = 3, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
                                }
                            }
                            StudyZenTextButton(if (r.bookmarked) "★" else "☆", onClick = { store.toggleBookmark(studyId, lessonId, r.id) })
                        }
                    }
                }
            }
            StudyZenTextButton(if (loading) "Refreshing…" else "Refresh materials", enabled = !loading, onClick = { fetch() })
        }

        // what has been done for this lesson
        val lessonChats = store.scansFor(studyId).count { it.lessonId == lessonId }
        val lessonTests = store.assessmentsFor(studyId).filter { it.lessonId == lessonId }
        if (lessonChats > 0 || lessonTests.isNotEmpty()) {
            Spacer(Modifier.height(20.dp))
            SectionHeader("Your work on this lesson")
            Spacer(Modifier.height(8.dp))
            StudyZenCard {
                if (lessonChats > 0) {
                    Text("$lessonChats AI conversation${if (lessonChats == 1) "" else "s"}", style = MaterialTheme.typography.bodyMedium, color = colors.onSurface)
                    StudyZenTextButton("Open", onClick = { nav.navigate(Screen.ToolHistory(studyId, StudyTool.Chat, lessonId)) })
                }
                lessonTests.forEach { a ->
                    StudyZenTextButton("${a.title}" + if (a.submitted) " · ${a.correct}/${a.total}" else " · unfinished", onClick = {
                        nav.navigate(if (a.submitted) Screen.QuizResult(studyId, a.id) else Screen.Quiz(studyId, a.id))
                    })
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        StudyZenButton(
            text = if (lesson.completed) "Marked as done · tap to undo" else "Mark as done",
            enabled = open,
            onClick = { store.setLessonCompleted(studyId, lessonId, !lesson.completed) },
        )
        Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StudyZenTextButton(
                "← ${previous?.title?.take(18) ?: "Start"}",
                enabled = previous != null,
                onClick = { previous?.let { nav.replaceTop(Screen.Lesson(studyId, it.id)) } },
                modifier = Modifier.weight(1f),
            )
            StudyZenTextButton(
                "${next?.title?.take(18) ?: "End"} →",
                enabled = next != null && study.canOpen(next.id),
                onClick = { next?.let { nav.replaceTop(Screen.Lesson(studyId, it.id)) } },
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(16.dp))
    }
}
