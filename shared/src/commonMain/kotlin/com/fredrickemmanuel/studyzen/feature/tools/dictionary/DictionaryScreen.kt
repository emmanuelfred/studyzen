package com.fredrickemmanuel.studyzen.feature.tools.dictionary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.fredrickemmanuel.studyzen.core.Ids
import com.fredrickemmanuel.studyzen.domain.LogKind
import com.fredrickemmanuel.studyzen.domain.StudyLogEntry
import androidx.compose.runtime.LaunchedEffect
import com.fredrickemmanuel.studyzen.core.designsystem.components.BannerTone
import com.fredrickemmanuel.studyzen.core.designsystem.components.InfoBanner
import com.fredrickemmanuel.studyzen.core.designsystem.components.ScreenScaffold
import com.fredrickemmanuel.studyzen.core.designsystem.components.SectionHeader
import com.fredrickemmanuel.studyzen.core.designsystem.components.StudyZenButton
import com.fredrickemmanuel.studyzen.core.designsystem.components.StudyZenCard
import com.fredrickemmanuel.studyzen.core.designsystem.components.StudyZenTextButton
import com.fredrickemmanuel.studyzen.core.designsystem.components.StudyZenTextField
import com.fredrickemmanuel.studyzen.core.designsystem.components.TagChip
import com.fredrickemmanuel.studyzen.core.time.todayEpochDay
import com.fredrickemmanuel.studyzen.data.LocalStudyStore
import com.fredrickemmanuel.studyzen.data.dictionary.DictionaryClient
import com.fredrickemmanuel.studyzen.data.dictionary.DictionaryEntry
import com.fredrickemmanuel.studyzen.data.dictionary.LookupResult
import com.fredrickemmanuel.studyzen.data.dictionary.WordBank
import com.fredrickemmanuel.studyzen.domain.FavoriteWord
import com.fredrickemmanuel.studyzen.domain.Note
import com.fredrickemmanuel.studyzen.domain.NoteSource
import com.fredrickemmanuel.studyzen.feature.media.LocalMediaPlatform
import com.fredrickemmanuel.studyzen.navigation.LocalAppNavigator
import kotlinx.coroutines.launch

/** What the screen is showing below the search box. */
private sealed interface View {
    data object Home : View
    data class Loading(val word: String) : View
    data class Found(val entry: DictionaryEntry) : View
    /** Nothing to show for the lookup; [saved] is the learner's own copy of the word, when there is one. */
    data class Message(val text: String, val warning: Boolean, val saved: FavoriteWord?) : View
}

/** Look up words, save favourites, and see a new word every day. [studyId] links notes saved from here to a Study. */
@Composable
fun DictionaryScreen(studyId: String?, modifier: Modifier = Modifier, initialWord: String? = null) {
    val nav = LocalAppNavigator.current
    val store = LocalStudyStore.current
    val colors = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    val client = remember { DictionaryClient() }

    var query by remember { mutableStateOf("") }
    var view by remember { mutableStateOf<View>(View.Home) }
    var noteSaved by remember { mutableStateOf(false) }

    val today = todayEpochDay()
    val wordOfTheDay = remember(today) { WordBank.wordOfTheDay(today) }

    fun lookup(word: String, fallback: FavoriteWord? = null) {
        val clean = word.trim()
        if (clean.isEmpty()) return
        query = clean
        noteSaved = false
        view = View.Loading(clean)
        scope.launch {
            view = when (val r = client.lookup(clean)) {
                is LookupResult.Found -> {
                    if (studyId != null) {
                        store.addLog(StudyLogEntry(Ids.next("log"), studyId, LogKind.Dictionary, clean, r.entry.meanings.firstOrNull { it.definitions.isNotEmpty() }?.definitions?.first()?.text.orEmpty(), todayEpochDay()))
                    }
                    View.Found(r.entry)
                }
                LookupResult.NotFound -> View.Message(
                    "No definition found for \"$clean\". Check the spelling or try another word.",
                    warning = false,
                    saved = fallback,
                )
                is LookupResult.Failed -> View.Message(r.message, warning = true, saved = fallback)
            }
        }
    }

    LaunchedEffect(initialWord) { if (!initialWord.isNullOrBlank()) lookup(initialWord) }

    ScreenScaffold(title = "Dictionary", onBack = nav::back, modifier = modifier) {
        StudyZenTextField(
            value = query,
            onValueChange = { query = it },
            label = "Look up a word",
            placeholder = "e.g. hypothesis",
            imeAction = ImeAction.Search,
            onImeAction = { lookup(query) },
        )
        Spacer(Modifier.height(12.dp))
        StudyZenButton(
            "Look up",
            onClick = { lookup(query) },
            enabled = query.isNotBlank(),
            loading = view is View.Loading,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(16.dp))

        when (val v = view) {
            View.Home, is View.Loading -> Unit

            is View.Found -> {
                EntryCard(
                    entry = v.entry,
                    saved = store.isFavoriteWord(v.entry.word),
                    onToggleSaved = {
                        if (store.isFavoriteWord(v.entry.word)) {
                            store.removeFavoriteWord(v.entry.word.trim().lowercase())
                        } else {
                            v.entry.toFavorite(today)?.let(store::addFavoriteWord)
                        }
                    },
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StudyZenTextButton(
                        if (noteSaved) "Added to notes" else "Add to notes",
                        enabled = !noteSaved,
                        onClick = {
                            store.saveNote(
                                Note(
                                    id = Ids.next("note"),
                                    studyId = studyId,
                                    title = v.entry.word,
                                    body = v.entry.toNoteText(),
                                    source = NoteSource.Dictionary,
                                    updatedAtEpochDay = today,
                                ),
                            )
                            noteSaved = true
                        },
                    )
                    StudyZenTextButton("Clear", onClick = { view = View.Home; query = "" })
                }
            }

            is View.Message -> {
                InfoBanner(v.text, tone = if (v.warning) BannerTone.Warning else BannerTone.Info)
                v.saved?.let {
                    Spacer(Modifier.height(12.dp))
                    SectionHeader("Your saved copy")
                    Spacer(Modifier.height(8.dp))
                    SavedWordCard(it, onRemove = { store.removeFavoriteWord(it.id) })
                }
                StudyZenTextButton("Clear", onClick = { view = View.Home; query = "" })
            }
        }

        if (view is View.Home || view is View.Loading) {
            // --- Word of the day
            SectionHeader("Word of the day")
            Spacer(Modifier.height(8.dp))
            StudyZenCard {
                Text(wordOfTheDay.word, style = MaterialTheme.typography.headlineSmall, color = colors.onSurface)
                TagChip(wordOfTheDay.partOfSpeech, modifier = Modifier.padding(top = 6.dp))
                Text(
                    wordOfTheDay.definition,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurface,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Text(
                    "\"${wordOfTheDay.example}\"",
                    style = MaterialTheme.typography.bodySmall,
                    fontStyle = FontStyle.Italic,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
                Spacer(Modifier.height(8.dp))
                val saved = store.isFavoriteWord(wordOfTheDay.word)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    PronounceButton(wordOfTheDay.word, null)
                    StudyZenTextButton(
                        if (saved) "★ Saved" else "☆ Save",
                        onClick = {
                            if (saved) store.removeFavoriteWord(wordOfTheDay.word.lowercase())
                            else store.addFavoriteWord(wordOfTheDay.toFavorite(today))
                        },
                    )
                    StudyZenTextButton("Full entry", onClick = { lookup(wordOfTheDay.word, wordOfTheDay.toFavorite(today)) })
                }
            }

            // --- Favourites
            Spacer(Modifier.height(20.dp))
            SectionHeader("Favourite words")
            Spacer(Modifier.height(8.dp))
            if (store.favoriteWords.isEmpty()) {
                StudyZenCard {
                    Text(
                        "Words you save with ☆ appear here, and stay available without internet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    store.favoriteWords.forEach { fav ->
                        SavedWordCard(
                            fav,
                            onOpen = { lookup(fav.word, fav) },
                            onRemove = { store.removeFavoriteWord(fav.id) },
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun EntryCard(entry: DictionaryEntry, saved: Boolean, onToggleSaved: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    StudyZenCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(entry.word, style = MaterialTheme.typography.headlineSmall, color = colors.onSurface)
                entry.phonetic?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant) }
            }
            PronounceButton(entry.word, entry.audioUrl)
            StudyZenTextButton(if (saved) "★ Saved" else "☆ Save", onClick = onToggleSaved)
        }
        entry.meanings.forEach { meaning ->
            Spacer(Modifier.height(14.dp))
            if (meaning.partOfSpeech.isNotBlank()) TagChip(meaning.partOfSpeech)
            meaning.definitions.take(3).forEachIndexed { i, d ->
                Text(
                    "${i + 1}. ${d.text}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurface,
                    modifier = Modifier.padding(top = 6.dp),
                )
                d.example?.let {
                    Text(
                        "\"$it\"",
                        style = MaterialTheme.typography.bodySmall,
                        fontStyle = FontStyle.Italic,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.padding(start = 16.dp, top = 2.dp),
                    )
                }
            }
            if (meaning.synonyms.isNotEmpty()) {
                Text(
                    "Similar: " + meaning.synonyms.take(6).joinToString(", "),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.primary,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun SavedWordCard(word: FavoriteWord, onOpen: (() -> Unit)? = null, onRemove: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    StudyZenCard(onClick = onOpen) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(word.word, style = MaterialTheme.typography.titleMedium, color = colors.onSurface)
                if (word.partOfSpeech.isNotBlank()) {
                    Text(word.partOfSpeech, style = MaterialTheme.typography.labelMedium, color = colors.primary)
                }
            }
            PronounceButton(word.word, null)
            StudyZenTextButton("★ Remove", onClick = onRemove)
        }
        Text(
            word.definition,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurface,
            modifier = Modifier.padding(top = 4.dp),
        )
        word.example?.let {
            Text(
                "\"$it\"",
                style = MaterialTheme.typography.bodySmall,
                fontStyle = FontStyle.Italic,
                color = colors.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

/**
 * Plays the word. Uses the dictionary's recording when there is one (needs internet); otherwise,
 * or if that cannot play, the phone reads the word aloud, which works offline.
 */
@Composable
private fun PronounceButton(word: String, audioUrl: String?) {
    val media = LocalMediaPlatform.current
    if (!media.supported) return
    var playing by remember(word) { mutableStateOf(false) }
    DisposableEffect(word) { onDispose { if (playing) media.stopPlayback() } }
    StudyZenTextButton(
        if (playing) "■ Stop" else "▶ Listen",
        onClick = {
            if (playing) {
                media.stopPlayback()
                playing = false
            } else if (audioUrl != null) {
                playing = true
                media.playUrl(
                    audioUrl,
                    onDone = { playing = false },
                    onError = { playing = false; media.speak(word) },
                )
            } else {
                media.speak(word)
            }
        },
    )
}
