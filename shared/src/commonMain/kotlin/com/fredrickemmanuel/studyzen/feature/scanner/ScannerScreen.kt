package com.fredrickemmanuel.studyzen.feature.scanner

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.fredrickemmanuel.studyzen.core.Ids
import com.fredrickemmanuel.studyzen.core.designsystem.components.AppIcon
import com.fredrickemmanuel.studyzen.core.designsystem.components.BannerTone
import com.fredrickemmanuel.studyzen.core.designsystem.components.InfoBanner
import com.fredrickemmanuel.studyzen.core.designsystem.components.ScreenScaffold
import com.fredrickemmanuel.studyzen.core.designsystem.components.SectionHeader
import com.fredrickemmanuel.studyzen.core.designsystem.components.StudyZenButton
import com.fredrickemmanuel.studyzen.core.designsystem.components.StudyZenCard
import com.fredrickemmanuel.studyzen.core.designsystem.components.StudyZenIcon
import com.fredrickemmanuel.studyzen.core.designsystem.components.StudyZenTextButton
import com.fredrickemmanuel.studyzen.core.designsystem.components.StudyZenTextField
import com.fredrickemmanuel.studyzen.core.time.todayEpochDay
import com.fredrickemmanuel.studyzen.data.LocalStudyStore
import com.fredrickemmanuel.studyzen.data.ai.AiFailure
import com.fredrickemmanuel.studyzen.data.ai.AiImage
import com.fredrickemmanuel.studyzen.data.ai.AiMessage
import com.fredrickemmanuel.studyzen.data.ai.AiResult
import com.fredrickemmanuel.studyzen.data.ai.AiRole
import com.fredrickemmanuel.studyzen.data.ai.AiService
import com.fredrickemmanuel.studyzen.data.ai.LocalAiSettings
import com.fredrickemmanuel.studyzen.data.ai.ScanPreset
import com.fredrickemmanuel.studyzen.data.ai.StudyPrompts
import com.fredrickemmanuel.studyzen.domain.BlockKind
import com.fredrickemmanuel.studyzen.domain.Note
import com.fredrickemmanuel.studyzen.domain.NoteBlock
import com.fredrickemmanuel.studyzen.domain.NoteSource
import com.fredrickemmanuel.studyzen.domain.ScanRecord
import com.fredrickemmanuel.studyzen.domain.ScanTurnRecord
import com.fredrickemmanuel.studyzen.domain.answerToBlocks
import com.fredrickemmanuel.studyzen.domain.blocksFromBody
import com.fredrickemmanuel.studyzen.domain.scanTitle
import com.fredrickemmanuel.studyzen.domain.toPlainText
import com.fredrickemmanuel.studyzen.feature.ai.AnswerText
import com.fredrickemmanuel.studyzen.feature.media.LocalMediaPlatform
import com.fredrickemmanuel.studyzen.navigation.LocalAppNavigator
import com.fredrickemmanuel.studyzen.navigation.Screen
import kotlinx.coroutines.launch

/** One message in the scan conversation. [text] is what was sent; [shown] is what the learner sees. */
private data class ScanTurn(
    val fromUser: Boolean,
    val text: String,
    val shown: String = text,
    /** What the learner typed themselves (null when they only tapped a button). */
    val typed: String? = null,
    val provider: String? = null,
)

/**
 * Snap or pick a photo of a problem (or just type), choose what to do with one tap or write your
 * own instruction, and get a detailed answer with steps. Follow-up questions continue the same chat.
 * Uses the learner's own AI key (see AI settings), so the app needs no AI of its own.
 */
@Composable
fun ScannerScreen(
    studyId: String?,
    initialPrompt: String?,
    scanId: String? = null,
    lessonId: String? = null,
    modifier: Modifier = Modifier,
) {
    val nav = LocalAppNavigator.current
    val store = LocalStudyStore.current
    val media = LocalMediaPlatform.current
    val aiStore = LocalAiSettings.current
    val colors = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    val service = remember { AiService() }
    /** A conversation reopened from the study's history, if any. */
    val saved = remember(scanId) { store.scan(scanId) }
    val lessonForContext = lessonId ?: saved?.lessonId
    val studyTitle = remember(studyId, lessonForContext) {
        val study = store.study(studyId)
        val lesson = study?.lesson(lessonForContext)
        if (study == null) null else if (lesson != null) "${study.title} (current lesson: ${lesson.title})" else study.title
    }
    var recordId by remember { mutableStateOf(saved?.id ?: Ids.next("scan")) }

    var imageFile by remember { mutableStateOf(saved?.imageFile) }
    /** True once a saved note uses the photo: it must then be kept when leaving this screen. */
    var imageKept by remember { mutableStateOf(saved?.imageFile != null) }
    var prompt by remember { mutableStateOf(initialPrompt.orEmpty()) }
    val turns = remember {
        mutableStateListOf<ScanTurn>().also { list ->
            saved?.turns?.forEach { list += ScanTurn(it.fromUser, it.text, it.shown, it.typed, it.provider) }
        }
    }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<AiResult.Failure?>(null) }
    var lastSend by remember { mutableStateOf<(() -> Unit)?>(null) }
    var hint by remember { mutableStateOf<String?>(null) }
    var savedAsNote by remember { mutableStateOf(false) }
    var addedToNote by remember { mutableStateOf(false) }
    var pickNote by remember { mutableStateOf(false) }

    val hasKey = aiStore.settings.hasKey

    // a photo that was never saved into a note is not kept
    val imageNow by rememberUpdatedState(imageFile)
    val keptNow by rememberUpdatedState(imageKept)
    DisposableEffect(Unit) {
        onDispose {
            val file = imageNow
            if (file != null && !keptNow) media.deleteMedia(file)
        }
    }

    fun dropImage() {
        val file = imageFile
        if (file != null && !imageKept) media.deleteMedia(file)
        imageFile = null
        imageKept = false
    }

    fun setImage(name: String?) {
        if (name == null) return
        dropImage()
        imageFile = name
        hint = null
    }

    /** In a study the conversation is kept, so it can be reopened and continued from the study's history. */
    fun persist() {
        if (studyId == null || turns.none { !it.fromUser }) return
        if (imageFile != null) imageKept = true
        val first = turns.firstOrNull { it.fromUser }?.typed
        val answer = turns.firstOrNull { !it.fromUser }?.text.orEmpty()
        store.saveScan(
            ScanRecord(
                id = recordId,
                studyId = studyId,
                title = scanTitle(first, answer),
                turns = turns.map { ScanTurnRecord(it.fromUser, it.text, it.shown, it.typed, it.provider) },
                imageFile = imageFile,
                updatedAtEpochDay = todayEpochDay(),
                lessonId = lessonForContext,
            ),
        )
    }

    fun send(instruction: String, shown: String, typed: String?) {
        if (loading) return
        val first = turns.isEmpty()
        if (first && imageFile == null && typed.isNullOrBlank()) {
            hint = "Add a photo or type your question first."
            return
        }
        hint = null
        error = null
        turns += ScanTurn(true, instruction, shown, typed)
        prompt = ""
        loading = true
        val history = turns.toList()
        lastSend = { send(instruction, shown, typed) }
        scope.launch {
            val image = imageFile?.let { file -> media.readImageBase64(file)?.let { AiImage("image/jpeg", it) } }
            val messages = history.mapIndexed { i, turn ->
                AiMessage(if (turn.fromUser) AiRole.User else AiRole.Assistant, turn.text, if (i == 0) image else null)
            }
            when (val result = service.ask(aiStore.settings, StudyPrompts.system(studyTitle), messages)) {
                is AiResult.Success -> {
                    turns += ScanTurn(false, result.text, provider = result.provider.label)
                    persist()
                    savedAsNote = false
                    addedToNote = false
                }
                is AiResult.Failure -> {
                    // take the question back so the learner can retry or change it
                    turns.removeAt(turns.lastIndex)
                    error = result
                }
            }
            loading = false
        }
    }

    fun runPreset(preset: ScanPreset) {
        val typed = prompt.trim().ifEmpty { null }
        val instruction = if (typed == null) preset.instruction else "${preset.instruction}\n\n$typed"
        send(instruction, if (typed == null) preset.label else "${preset.label}: $typed", typed)
    }

    fun sendTyped() {
        val text = prompt.trim()
        if (text.isEmpty()) return
        send(text, text, text)
    }

    fun newScan() {
        if (studyId != null && imageKept) {
            // the photo belongs to the saved conversation now
            imageFile = null
            imageKept = false
        } else {
            dropImage()
        }
        recordId = Ids.next("scan")
        turns.clear()
        prompt = ""
        error = null
        hint = null
        savedAsNote = false
        addedToNote = false
        lastSend = null
    }

    /** The whole conversation as note blocks: each question followed by its answer. */
    fun conversationBlocks(): List<NoteBlock> {
        val out = mutableListOf<NoteBlock>()
        var i = 0
        while (i < turns.size) {
            val turn = turns[i]
            val answer = turns.getOrNull(i + 1)?.takeIf { !it.fromUser }
            if (turn.fromUser && answer != null) {
                out += answerToBlocks(turn.shown, answer.text) { Ids.next("blk") }
                i += 2
            } else {
                i += 1
            }
        }
        return out
    }

    fun titleText(): String {
        val firstQuestion = turns.firstOrNull { it.fromUser }?.typed
        val firstAnswer = turns.firstOrNull { !it.fromUser }?.text.orEmpty()
        return scanTitle(firstQuestion, firstAnswer)
    }

    fun saveAsNote() {
        val blocks = conversationBlocks()
        if (blocks.isEmpty()) return
        val withPhoto = if (imageFile != null && !imageKept) {
            listOf(NoteBlock(Ids.next("blk"), BlockKind.Image, media = imageFile)) + blocks
        } else {
            blocks
        }
        store.saveNote(
            Note(
                id = Ids.next("note"),
                studyId = studyId,
                title = titleText(),
                body = withPhoto.toPlainText(),
                source = NoteSource.Solver,
                updatedAtEpochDay = todayEpochDay(),
                blocks = withPhoto,
            ),
        )
        if (imageFile != null) imageKept = true
        savedAsNote = true
    }

    fun addToNote(note: Note) {
        val added = listOf(NoteBlock(Ids.next("blk"), BlockKind.Heading, titleText())) + conversationBlocks()
        val current = note.blocks.ifEmpty { blocksFromBody(note.body) { Ids.next("blk") } }
            .dropLastWhile { it.isText && it.text.isBlank() }
        val all = current + added
        store.saveNote(note.copy(blocks = all, body = all.toPlainText(), updatedAtEpochDay = todayEpochDay()))
        addedToNote = true
        pickNote = false
    }

    ScreenScaffold(
        title = "Ask AI & scan",
        onBack = { nav.back() },
        modifier = modifier,
        trailing = { StudyZenTextButton("AI settings", onClick = { nav.navigate(Screen.AiSettings) }) },
        bottomBar = {
            PromptBar(
                value = prompt,
                onValueChange = { prompt = it; hint = null },
                placeholder = if (turns.isEmpty()) "Or type your own question or instruction…" else "Ask a follow-up…",
                enabled = !loading,
                onSend = { sendTyped() },
            )
        },
    ) {
        Spacer(Modifier.height(4.dp))

        if (!hasKey) {
            StudyZenCard {
                Text("Add a free AI key to start", style = MaterialTheme.typography.titleMedium, color = colors.onSurface)
                Text(
                    "The scanner uses an AI service. It takes a minute to get a free key from Gemini, Groq or OpenRouter.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
                )
                StudyZenButton("Set up AI", onClick = { nav.navigate(Screen.AiSettings) })
            }
            Spacer(Modifier.height(16.dp))
        }

        // --- The photo
        val file = imageFile
        if (file != null) {
            PhotoPreview(
                file = file,
                compact = turns.isNotEmpty(),
                canChange = !loading && turns.isEmpty(),
                onCamera = { media.captureImage { setImage(it) } },
                onGallery = { media.pickImage { setImage(it) } },
                onRemove = { dropImage() },
            )
            Spacer(Modifier.height(16.dp))
        } else if (turns.isEmpty()) {
            if (media.supported) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PhotoOption(AppIcon.Camera, "Take a photo", Modifier.weight(1f)) { media.captureImage { setImage(it) } }
                    PhotoOption(AppIcon.Image, "Choose a photo", Modifier.weight(1f)) { media.pickImage { setImage(it) } }
                }
            } else {
                InfoBanner("Taking or choosing photos works in the Android app. You can still type a question below.")
            }
            Spacer(Modifier.height(16.dp))
        }

        // --- One-tap actions
        if (turns.isEmpty()) {
            val lessonTitle = store.study(studyId)?.lesson(lessonForContext)?.title
            if (lessonTitle != null) {
                PresetButton("Explain “$lessonTitle”", enabled = !loading && hasKey, modifier = Modifier.fillMaxWidth()) {
                    val ask = "Explain the lesson \"$lessonTitle\" clearly for my level, with an example and a few questions to check I understood."
                    send(ask, "Explain this lesson", ask)
                }
                Spacer(Modifier.height(10.dp))
            }
            SectionHeader("What should I do?")
            Spacer(Modifier.height(8.dp))
            StudyPrompts.presets.chunked(2).forEach { pair ->
                Row(Modifier.padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    pair.forEach { preset ->
                        PresetButton(preset.label, enabled = !loading && hasKey, modifier = Modifier.weight(1f)) { runPreset(preset) }
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
            hint?.let {
                InfoBanner(it, tone = BannerTone.Warning)
                Spacer(Modifier.height(8.dp))
            }
            Text(
                "Tap one, or write your own instruction below. Anything you type is added to the button you tap.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
        }

        // --- The conversation
        turns.forEach { turn ->
            Spacer(Modifier.height(12.dp))
            if (turn.fromUser) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Box(
                        Modifier
                            .widthIn(max = 300.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(colors.primaryContainer)
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                    ) {
                        Text(turn.shown, style = MaterialTheme.typography.bodyMedium, color = colors.onPrimaryContainer)
                    }
                }
            } else {
                StudyZenCard {
                    AnswerText(turn.text)
                    turn.provider?.let {
                        Text(
                            "Answered by $it. AI can make mistakes, so double-check important answers.",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.onSurfaceVariant,
                            modifier = Modifier.padding(top = 10.dp),
                        )
                    }
                }
            }
        }

        if (loading) {
            Spacer(Modifier.height(12.dp))
            StudyZenCard {
                Text("Working on it…", style = MaterialTheme.typography.titleSmall, color = colors.onSurface)
                Text(
                    "Reading your question and writing the steps. This can take up to a minute.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }

        error?.let { failure ->
            Spacer(Modifier.height(12.dp))
            InfoBanner(failure.message, tone = BannerTone.Error)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                StudyZenTextButton("Try again", onClick = { lastSend?.invoke() })
                if (failure.kind in listOf(AiFailure.NoKey, AiFailure.BadKey, AiFailure.ModelProblem, AiFailure.RateLimited)) {
                    StudyZenTextButton("AI settings", onClick = { nav.navigate(Screen.AiSettings) })
                }
            }
        }

        // --- What to do with the answer
        if (turns.isNotEmpty() && !loading && turns.last().let { !it.fromUser }) {
            Spacer(Modifier.height(16.dp))
            StudyZenButton(
                if (savedAsNote) "Saved to notes" else "Save to notes",
                onClick = { saveAsNote() },
                enabled = !savedAsNote,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                StudyZenTextButton(
                    if (addedToNote) "Added to a note" else "Add to a note",
                    onClick = { pickNote = true },
                    enabled = !addedToNote,
                )
                StudyZenTextButton("New scan", onClick = { newScan() })
            }
        }
        Spacer(Modifier.height(16.dp))
    }

    if (pickNote) {
        val notes = store.allNotes()
        AlertDialog(
            onDismissRequest = { pickNote = false },
            title = { Text("Add to which note?") },
            text = {
                if (notes.isEmpty()) {
                    Text("You have no notes yet. Use “Save to notes” to make the first one.")
                } else {
                    Column(Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState())) {
                        notes.forEach { note ->
                            Text(
                                note.title.ifBlank { "Untitled note" },
                                style = MaterialTheme.typography.bodyLarge,
                                color = colors.onSurface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { addToNote(note) }
                                    .padding(vertical = 12.dp),
                            )
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { pickNote = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun PhotoOption(icon: AppIcon, label: String, modifier: Modifier, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    StudyZenCard(modifier = modifier, onClick = onClick) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(52.dp).clip(CircleShape).background(colors.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                StudyZenIcon(icon, tint = colors.primary, size = 26.dp)
            }
            Text(
                label,
                style = MaterialTheme.typography.titleSmall,
                color = colors.onSurface,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
    }
}

@Composable
private fun PresetButton(label: String, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier
            .heightIn(min = 52.dp)
            .clip(shape)
            .background(if (enabled) colors.primaryContainer else colors.surfaceVariant)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = if (enabled) colors.onPrimaryContainer else colors.onSurfaceVariant,
        )
    }
}

@Composable
private fun PhotoPreview(
    file: String,
    compact: Boolean,
    canChange: Boolean,
    onCamera: () -> Unit,
    onGallery: () -> Unit,
    onRemove: () -> Unit,
) {
    val media = LocalMediaPlatform.current
    val colors = MaterialTheme.colorScheme
    val bitmap = remember(file) { media.loadImage(file) }
    Column {
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(colors.surfaceVariant)) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = "Your photo",
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier.fillMaxWidth().heightIn(max = if (compact) 160.dp else 320.dp),
                )
            } else {
                Text("Photo not available", modifier = Modifier.padding(24.dp), color = colors.onSurfaceVariant)
            }
        }
        if (canChange) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                StudyZenTextButton("Retake", onClick = onCamera)
                StudyZenTextButton("Choose another", onClick = onGallery)
                StudyZenTextButton("Remove", onClick = onRemove)
            }
        }
    }
}

@Composable
private fun PromptBar(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    enabled: Boolean,
    onSend: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StudyZenTextField(
            value = value,
            onValueChange = onValueChange,
            label = "Your question",
            placeholder = placeholder,
            modifier = Modifier.weight(1f),
            singleLine = false,
            enabled = enabled,
        )
        val canSend = enabled && value.isNotBlank()
        Box(
            Modifier
                .padding(bottom = 4.dp)
                .size(48.dp)
                .clip(CircleShape)
                .background(if (canSend) colors.primary else colors.surfaceVariant)
                .clickable(enabled = canSend, onClick = onSend),
            contentAlignment = Alignment.Center,
        ) {
            StudyZenIcon(AppIcon.Send, tint = if (canSend) colors.onPrimary else colors.onSurfaceVariant, size = 22.dp)
        }
    }
}
