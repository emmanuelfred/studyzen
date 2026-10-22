package com.fredrickemmanuel.studyzen.feature.study

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.fredrickemmanuel.studyzen.core.Ids
import com.fredrickemmanuel.studyzen.core.designsystem.components.BannerTone
import com.fredrickemmanuel.studyzen.core.designsystem.components.InfoBanner
import com.fredrickemmanuel.studyzen.core.designsystem.components.ScreenScaffold
import com.fredrickemmanuel.studyzen.core.designsystem.components.StudyZenButton
import com.fredrickemmanuel.studyzen.core.designsystem.components.StudyZenCard
import com.fredrickemmanuel.studyzen.core.designsystem.components.StudyZenTextButton
import com.fredrickemmanuel.studyzen.core.designsystem.components.StudyZenTextField
import com.fredrickemmanuel.studyzen.core.designsystem.components.TagChip
import com.fredrickemmanuel.studyzen.core.time.todayEpochDay
import com.fredrickemmanuel.studyzen.data.LocalStudyStore
import com.fredrickemmanuel.studyzen.data.ai.LocalAiSettings
import com.fredrickemmanuel.studyzen.data.ai.StudyAi
import com.fredrickemmanuel.studyzen.data.ai.StudyAiOutcome
import com.fredrickemmanuel.studyzen.data.ai.WikiArticle
import com.fredrickemmanuel.studyzen.domain.Finding
import com.fredrickemmanuel.studyzen.domain.LessonSources
import com.fredrickemmanuel.studyzen.domain.Note
import com.fredrickemmanuel.studyzen.domain.NoteSource
import com.fredrickemmanuel.studyzen.domain.ResearchItem
import com.fredrickemmanuel.studyzen.navigation.LocalAppNavigator
import kotlinx.coroutines.launch

/**
 * Looks things up on Wikipedia and shows the real articles with links. An AI summary is optional, is written
 * only from those articles, and is labelled as AI. Every search is kept in the study.
 */
@Composable
fun ResearchScreen(studyId: String, itemId: String?, modifier: Modifier = Modifier) {
    val store = LocalStudyStore.current
    val nav = LocalAppNavigator.current
    val aiStore = LocalAiSettings.current
    val colors = MaterialTheme.colorScheme
    val uri = LocalUriHandler.current
    val scope = rememberCoroutineScope()
    val ai = remember { StudyAi() }
    val study = store.study(studyId)

    val saved = remember(itemId) { store.research.firstOrNull { it.id == itemId } }
    val id = remember(itemId) { saved?.id ?: Ids.next("research") }
    var query by remember { mutableStateOf(saved?.question.orEmpty()) }
    var findings by remember { mutableStateOf(saved?.findings.orEmpty()) }
    var loading by remember { mutableStateOf(false) }
    var summarising by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var warning by remember { mutableStateOf(false) }
    var searched by remember { mutableStateOf(saved != null) }
    var noteSaved by remember { mutableStateOf(false) }

    val sources = findings.filter { it.isSourced }
    val summary = findings.firstOrNull { !it.isSourced }

    fun keep(question: String, list: List<Finding>) {
        store.saveResearch(ResearchItem(id, studyId, question, list, saved = true))
    }

    fun search() {
        val q = query.trim()
        if (q.isEmpty() || loading) return
        loading = true
        message = null
        noteSaved = false
        scope.launch {
            val found = ai.searchWikipedia(q, 5)
            searched = true
            when {
                found == null -> { message = "Could not reach Wikipedia. Check your connection and try again."; warning = true }
                found.isEmpty() -> { message = "No Wikipedia articles matched. Try different words, or use the web search link below."; warning = false; findings = emptyList() }
                else -> {
                    findings = found.map { Finding(it.extract.take(700).trim(), it.title, it.url) }
                    keep(q, findings)
                }
            }
            loading = false
        }
    }

    fun summarise() {
        if (summarising || sources.isEmpty()) return
        summarising = true
        message = null
        scope.launch {
            val articles = sources.map { WikiArticle(it.sourceTitle.orEmpty(), it.sourceUrl.orEmpty(), it.text) }
            when (val out = ai.summarise(aiStore.settings, query.trim(), articles)) {
                is StudyAiOutcome.Ok -> {
                    findings = listOf(Finding(out.value, null, null)) + sources
                    keep(query.trim(), findings)
                }
                is StudyAiOutcome.Err -> { message = out.message; warning = true }
            }
            summarising = false
        }
    }

    fun saveAsNote() {
        val text = buildString {
            summary?.let { appendLine(it.text); appendLine() }
            sources.forEach { appendLine("${it.sourceTitle}: ${it.sourceUrl}") }
        }.trim()
        store.saveNote(
            Note(
                id = Ids.next("note"),
                studyId = studyId,
                title = "Research: ${query.trim()}".take(80),
                body = text,
                source = NoteSource.Research,
                updatedAtEpochDay = todayEpochDay(),
            ),
        )
        noteSaved = true
    }

    ScreenScaffold(title = "Research", onBack = { nav.back() }, modifier = modifier) {
        study?.let { Text(it.title, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant) }
        Spacer(Modifier.height(12.dp))
        StudyZenTextField(
            value = query,
            onValueChange = { query = it },
            label = "What do you want to find out?",
            placeholder = "e.g. photosynthesis light reactions",
        )
        Spacer(Modifier.height(12.dp))
        StudyZenButton("Search", onClick = { search() }, enabled = query.isNotBlank(), loading = loading)
        Spacer(Modifier.height(12.dp))
        message?.let {
            InfoBanner(it, tone = if (warning) BannerTone.Warning else BannerTone.Info)
            Spacer(Modifier.height(12.dp))
        }

        if (searched && query.isNotBlank()) {
            if (summary != null) {
                StudyZenCard {
                    TagChip("AI summary")
                    Spacer(Modifier.height(8.dp))
                    com.fredrickemmanuel.studyzen.feature.ai.AnswerText(summary.text)
                    Text(
                        "Written by an AI using only the articles below. Open them to check.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                Spacer(Modifier.height(12.dp))
            }
            if (sources.isNotEmpty()) {
                Text("Wikipedia articles found", style = MaterialTheme.typography.labelLarge, color = colors.onBackground)
                Spacer(Modifier.height(8.dp))
                sources.forEach { f ->
                    StudyZenCard(onClick = { f.sourceUrl?.let { uri.openUri(it) } }) {
                        Text(f.sourceTitle.orEmpty(), style = MaterialTheme.typography.titleSmall, color = colors.onSurface)
                        Text(f.text, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, maxLines = 4, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
                        Text("Tap to open on Wikipedia", style = MaterialTheme.typography.labelSmall, color = colors.primary, modifier = Modifier.padding(top = 6.dp))
                    }
                    Spacer(Modifier.height(8.dp))
                }
                Row {
                    if (summary == null && aiStore.settings.hasKey) {
                        StudyZenTextButton(if (summarising) "Summarising…" else "Summarise with AI", enabled = !summarising, onClick = { summarise() })
                    }
                    StudyZenTextButton(if (noteSaved) "Saved to notes" else "Save to notes", enabled = !noteSaved, onClick = { saveAsNote() })
                }
            }
            Spacer(Modifier.height(8.dp))
            Text("More places to look (these open a search, not a single page)", style = MaterialTheme.typography.labelLarge, color = colors.onBackground)
            Row {
                StudyZenTextButton("Web", onClick = { uri.openUri(LessonSources.webSearch(query.trim())) })
                StudyZenTextButton("YouTube", onClick = { uri.openUri(LessonSources.youtubeSearch(query.trim())) })
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}
