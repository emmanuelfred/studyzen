package com.fredrickemmanuel.studyzen.feature.notes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fredrickemmanuel.studyzen.core.Ids
import com.fredrickemmanuel.studyzen.core.designsystem.components.FilterPill
import com.fredrickemmanuel.studyzen.core.designsystem.components.ScreenHeader
import com.fredrickemmanuel.studyzen.core.designsystem.components.SectionHeader
import com.fredrickemmanuel.studyzen.core.designsystem.components.StudyZenTextButton
import com.fredrickemmanuel.studyzen.core.designsystem.components.SwitchRow
import com.fredrickemmanuel.studyzen.core.designsystem.components.TagChip
import com.fredrickemmanuel.studyzen.core.designsystem.theme.StudyZenGradients
import com.fredrickemmanuel.studyzen.core.time.todayEpochDay
import com.fredrickemmanuel.studyzen.data.LocalStudyStore
import com.fredrickemmanuel.studyzen.domain.BlockKind
import com.fredrickemmanuel.studyzen.domain.Note
import com.fredrickemmanuel.studyzen.domain.NoteBlock
import com.fredrickemmanuel.studyzen.domain.NoteSource
import com.fredrickemmanuel.studyzen.domain.applyTextEdit
import com.fredrickemmanuel.studyzen.domain.blocksFromBody
import com.fredrickemmanuel.studyzen.domain.firstLine
import com.fredrickemmanuel.studyzen.domain.formatDuration
import com.fredrickemmanuel.studyzen.domain.insertMedia
import com.fredrickemmanuel.studyzen.domain.isEmptyNote
import com.fredrickemmanuel.studyzen.domain.mediaFiles
import com.fredrickemmanuel.studyzen.domain.removeEmptyBlock
import com.fredrickemmanuel.studyzen.domain.toPlainText
import com.fredrickemmanuel.studyzen.feature.media.LocalMediaPlatform
import com.fredrickemmanuel.studyzen.navigation.LocalAppNavigator
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlin.time.TimeSource

/**
 * Write or edit a note. The note is a list of blocks: paragraphs, headings, bullets, checklist
 * items, photos and voice notes. It saves itself while you type, so there is no Save button.
 * [studyId] pre-selects a Study; a note without one is a general note.
 */
@Composable
fun NoteEditorScreen(studyId: String?, noteId: String?, modifier: Modifier = Modifier) {
    val store = LocalStudyStore.current
    val nav = LocalAppNavigator.current
    val media = LocalMediaPlatform.current
    val colors = MaterialTheme.colorScheme
    val focusManager = LocalFocusManager.current

    val existing = remember { store.note(noteId) }
    val myId = remember { existing?.id ?: Ids.next("note") }
    fun newId() = Ids.next("blk")

    var title by remember { mutableStateOf(existing?.title ?: "") }
    var linkedStudy by remember { mutableStateOf(existing?.studyId ?: studyId) }
    var pinned by remember { mutableStateOf(existing?.pinned ?: false) }
    val blocks = remember {
        mutableStateListOf<NoteBlock>().apply {
            addAll(existing?.blocks?.takeIf { it.isNotEmpty() } ?: blocksFromBody(existing?.body.orEmpty()) { newId() })
        }
    }

    var focusedId by remember { mutableStateOf<String?>(null) }
    var pendingFocus by remember { mutableStateOf<String?>(null) }
    var showDetails by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var discarded by remember { mutableStateOf(false) }
    var recording by remember { mutableStateOf(false) }
    var elapsedMs by remember { mutableStateOf(0L) }
    var playingId by remember { mutableStateOf<String?>(null) }
    var micDenied by remember { mutableStateOf(false) }

    fun setAll(list: List<NoteBlock>) {
        blocks.clear()
        blocks.addAll(list)
    }

    fun save(final: Boolean) {
        if (discarded) return
        val list = blocks.toList()
        if (title.isBlank() && list.isEmptyNote()) {
            // nothing written: when leaving, an emptied old note is removed and a new one never created
            if (final && store.note(myId) != null) store.deleteNote(myId)
            return
        }
        val note = Note(
            id = myId,
            studyId = linkedStudy,
            title = title.trim().ifEmpty { list.firstLine() },
            body = list.toPlainText(),
            source = existing?.source ?: NoteSource.Manual,
            updatedAtEpochDay = todayEpochDay(),
            pinned = pinned,
            blocks = list,
        )
        if (store.note(myId) != note) store.saveNote(note)
    }
    val saveLatest by rememberUpdatedState<(Boolean) -> Unit> { final -> save(final) }

    // save a moment after the learner stops typing, and once more when leaving
    LaunchedEffect(Unit) {
        snapshotFlow { listOf(title, blocks.toList(), linkedStudy, pinned) }.collectLatest {
            delay(1_200)
            saveLatest(false)
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            media.stopPlayback()
            if (recording) media.cancelRecording()
            saveLatest(true)
        }
    }
    LaunchedEffect(recording) {
        if (recording) {
            val started = TimeSource.Monotonic.markNow()
            while (true) {
                elapsedMs = started.elapsedNow().inWholeMilliseconds
                delay(250)
            }
        }
    }

    // --- actions on the block being edited ------------------------------------------------------

    fun currentIndex(): Int {
        val i = blocks.indexOfFirst { it.id == focusedId }
        return if (i >= 0) i else blocks.indexOfLast { it.isText }.let { if (it >= 0) it else blocks.lastIndex }
    }
    fun updateCurrent(change: (NoteBlock) -> NoteBlock) {
        val i = currentIndex()
        val b = blocks.getOrNull(i) ?: return
        if (b.isText) blocks[i] = change(b)
    }
    fun toggleKind(kind: BlockKind) = updateCurrent { it.copy(kind = if (it.kind == kind) BlockKind.Text else kind, checked = false) }

    fun addMedia(block: NoteBlock) {
        val edit = insertMedia(blocks.toList(), currentIndex(), block) { newId() }
        setAll(edit.blocks)
        pendingFocus = edit.focusId
    }
    fun removeBlock(block: NoteBlock) {
        block.media?.let { media.deleteMedia(it) }
        if (playingId == block.id) {
            media.stopPlayback()
            playingId = null
        }
        val rest = blocks.filter { it.id != block.id }
        setAll(if (rest.isEmpty()) listOf(NoteBlock(newId())) else rest)
    }

    val current = blocks.getOrNull(currentIndex())

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(StudyZenGradients.Screen),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            Modifier
                .widthIn(max = 560.dp)
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 20.dp),
        ) {
            ScreenHeader(
                title = "Note",
                onBack = {
                    save(true)
                    nav.back()
                },
                trailing = { StudyZenTextButton("Details", onClick = { showDetails = true }) },
            )

            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                if (existing != null && existing.source != NoteSource.Manual) {
                    TagChip(existing.source.label)
                    Spacer(Modifier.height(8.dp))
                }
                BasicTextField(
                    value = title,
                    onValueChange = { title = it.replace("\n", " ") },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.headlineSmall.copy(
                        color = colors.onSurface,
                        fontWeight = FontWeight.Bold,
                    ),
                    cursorBrush = SolidColor(colors.primary),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    decorationBox = { inner ->
                        Box {
                            if (title.isEmpty()) {
                                Text("Title", style = MaterialTheme.typography.headlineSmall, color = colors.onSurfaceVariant.copy(alpha = 0.6f))
                            }
                            inner()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                )
                Spacer(Modifier.height(4.dp))

                blocks.forEachIndexed { index, block ->
                    key(block.id) {
                        when (block.kind) {
                            BlockKind.Image -> ImageBlockRow(block, onDelete = { removeBlock(block) })
                            BlockKind.Audio -> AudioBlockRow(
                                block = block,
                                playing = playingId == block.id,
                                onToggle = {
                                    if (playingId == block.id) {
                                        media.stopPlayback()
                                        playingId = null
                                    } else {
                                        playingId = block.id
                                        media.playFile(block.media.orEmpty()) { if (playingId == block.id) playingId = null }
                                    }
                                },
                                onDelete = { removeBlock(block) },
                            )
                            else -> TextBlockRow(
                                block = block,
                                placeholder = when {
                                    block.kind == BlockKind.Check -> "To-do"
                                    block.kind == BlockKind.Bullet -> "List item"
                                    block.kind == BlockKind.Heading -> "Heading"
                                    blocks.size == 1 -> "Note something down"
                                    else -> null
                                },
                                focusRequestId = pendingFocus,
                                onFocusHandled = { pendingFocus = null },
                                onFocused = { focusedId = block.id },
                                onTextChange = { text ->
                                    val edit = applyTextEdit(blocks.toList(), index, text) { newId() }
                                    if (edit.blocks.size == blocks.size && edit.blocks.getOrNull(index)?.text == text) {
                                        blocks[index] = edit.blocks[index]
                                    } else {
                                        setAll(edit.blocks)
                                        pendingFocus = edit.focusId
                                    }
                                },
                                onToggleChecked = { blocks[index] = block.copy(checked = !block.checked) },
                                onBackspaceWhenEmpty = {
                                    val edit = removeEmptyBlock(blocks.toList(), index)
                                    setAll(edit.blocks)
                                    pendingFocus = edit.focusId
                                },
                            )
                        }
                    }
                }

                // tapping the empty space below the note puts the cursor at the end
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clickable(indication = null, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }) {
                            val last = blocks.lastOrNull()
                            if (last != null && last.isText) {
                                pendingFocus = last.id
                            } else {
                                val fresh = NoteBlock(newId())
                                blocks.add(fresh)
                                pendingFocus = fresh.id
                            }
                        },
                )
            }

            if (recording) {
                RecordingBar(
                    elapsedMs = elapsedMs,
                    onCancel = {
                        media.cancelRecording()
                        recording = false
                    },
                    onSave = {
                        val audio = media.stopRecording()
                        recording = false
                        if (audio != null) {
                            addMedia(NoteBlock(newId(), BlockKind.Audio, media = audio.fileName, durationMs = audio.durationMs))
                        }
                    },
                )
            }
            if (micDenied) {
                Text(
                    "Allow the microphone in your phone settings to record voice notes.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.error,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
            }

            Toolbar(
                bold = current?.bold == true,
                italic = current?.italic == true,
                kind = current?.kind,
                canFormat = current?.isText == true,
                mediaSupported = media.supported,
                recording = recording,
                onBold = { updateCurrent { it.copy(bold = !it.bold) } },
                onItalic = { updateCurrent { it.copy(italic = !it.italic) } },
                onKind = { toggleKind(it) },
                onPhoto = { camera ->
                    val at = currentIndex()
                    val onPicked: (String?) -> Unit = { name ->
                        if (name != null) {
                            val edit = insertMedia(blocks.toList(), at.coerceAtMost(blocks.lastIndex), NoteBlock(newId(), BlockKind.Image, media = name)) { newId() }
                            setAll(edit.blocks)
                            pendingFocus = edit.focusId
                        }
                    }
                    if (camera) media.captureImage(onPicked) else media.pickImage(onPicked)
                },
                onVoice = {
                    micDenied = false
                    media.stopPlayback()
                    playingId = null
                    media.startRecording { started ->
                        recording = started
                        micDenied = !started
                    }
                },
            )
        }
    }

    if (showDetails) {
        AlertDialog(
            onDismissRequest = { showDetails = false },
            title = { Text("Note details") },
            text = {
                Column {
                    SectionHeader("Belongs to")
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterPill("General", linkedStudy == null, { linkedStudy = null })
                        store.activeStudies().forEach { FilterPill(it.title, linkedStudy == it.id, { linkedStudy = it.id }) }
                    }
                    Spacer(Modifier.height(8.dp))
                    SwitchRow(
                        title = "Pin to the top",
                        subtitle = "Pinned notes stay at the top of your list.",
                        checked = pinned,
                        onCheckedChange = { pinned = it },
                    )
                    if (blocks.any { it.isMedia }) {
                        Text(
                            "Photos and voice notes stay on this phone for now. Text and checklists sync to your other phones.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                    if (store.note(myId) != null) {
                        Spacer(Modifier.height(4.dp))
                        StudyZenTextButton("Delete note", onClick = { showDetails = false; confirmDelete = true })
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showDetails = false }) { Text("Done") } },
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this note?") },
            text = { Text("Its photos and voice notes are deleted too. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    discarded = true
                    blocks.toList().mediaFiles().forEach { media.deleteMedia(it) }
                    store.deleteNote(myId)
                    nav.back()
                }) { Text("Delete", color = colors.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

// --- Blocks --------------------------------------------------------------------------------------

@Composable
private fun TextBlockRow(
    block: NoteBlock,
    placeholder: String?,
    focusRequestId: String?,
    onFocusHandled: () -> Unit,
    onFocused: () -> Unit,
    onTextChange: (String) -> Unit,
    onToggleChecked: () -> Unit,
    onBackspaceWhenEmpty: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val requester = remember { FocusRequester() }
    LaunchedEffect(focusRequestId) {
        if (focusRequestId == block.id) {
            runCatching { requester.requestFocus() }
            onFocusHandled()
        }
    }

    val done = block.kind == BlockKind.Check && block.checked
    val base = if (block.kind == BlockKind.Heading) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyLarge
    val style: TextStyle = base.copy(
        color = if (done) colors.onSurfaceVariant else colors.onSurface,
        fontWeight = if (block.bold || block.kind == BlockKind.Heading) FontWeight.Bold else FontWeight.Normal,
        fontStyle = if (block.italic) FontStyle.Italic else FontStyle.Normal,
        textDecoration = if (done) TextDecoration.LineThrough else TextDecoration.None,
        fontSize = if (block.kind == BlockKind.Heading) 22.sp else 17.sp,
        lineHeight = if (block.kind == BlockKind.Heading) 28.sp else 24.sp,
    )

    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = if (block.kind == BlockKind.Heading) 6.dp else 2.dp),
        verticalAlignment = Alignment.Top,
    ) {
        when (block.kind) {
            BlockKind.Bullet -> Text("•", style = style.copy(fontWeight = FontWeight.Bold), modifier = Modifier.padding(end = 10.dp, start = 4.dp))
            BlockKind.Check -> {
                val shape = RoundedCornerShape(6.dp)
                Box(
                    Modifier
                        .padding(top = 3.dp, end = 10.dp)
                        .size(22.dp)
                        .clip(shape)
                        .border(1.5.dp, colors.primary, shape)
                        .background(if (block.checked) colors.primary else Color.Transparent)
                        .clickable(onClick = onToggleChecked),
                    contentAlignment = Alignment.Center,
                ) {
                    if (block.checked) Text("✓", color = colors.onPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
            else -> Unit
        }
        BasicTextField(
            value = block.text,
            onValueChange = onTextChange,
            textStyle = style,
            cursorBrush = SolidColor(colors.primary),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            decorationBox = { inner ->
                Box {
                    if (block.text.isEmpty() && placeholder != null) {
                        Text(placeholder, style = style.copy(fontWeight = FontWeight.Normal, textDecoration = TextDecoration.None), color = colors.onSurfaceVariant.copy(alpha = 0.6f))
                    }
                    inner()
                }
            },
            modifier = Modifier
                .weight(1f)
                .focusRequester(requester)
                .onFocusChanged { if (it.isFocused) onFocused() }
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown && event.key == Key.Backspace && block.text.isEmpty()) {
                        onBackspaceWhenEmpty()
                        true
                    } else {
                        false
                    }
                },
        )
    }
}

@Composable
private fun ImageBlockRow(block: NoteBlock, onDelete: () -> Unit) {
    val media = LocalMediaPlatform.current
    val colors = MaterialTheme.colorScheme
    val bitmap = remember(block.media) { block.media?.let { media.loadImage(it) } }
    val shape = RoundedCornerShape(14.dp)
    Box(Modifier.fillMaxWidth().padding(vertical = 8.dp).clip(shape)) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = "Photo in your note",
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp),
            )
        } else {
            Box(
                Modifier.fillMaxWidth().height(96.dp).background(colors.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Text("This photo is not on this phone", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
        }
        RoundIconButton("✕", onDelete, Modifier.align(Alignment.TopEnd).padding(8.dp))
    }
}

@Composable
private fun AudioBlockRow(block: NoteBlock, playing: Boolean, onToggle: () -> Unit, onDelete: () -> Unit) {
    val media = LocalMediaPlatform.current
    val colors = MaterialTheme.colorScheme
    val available = remember(block.media) { block.media?.let { media.hasMedia(it) } == true }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(colors.surfaceVariant)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (available) colors.primary else colors.outline)
                .clickable(enabled = available, onClick = onToggle),
            contentAlignment = Alignment.Center,
        ) {
            Text(if (playing) "■" else "▶", color = colors.onPrimary, fontSize = 16.sp)
        }
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text("Voice note", style = MaterialTheme.typography.titleSmall, color = colors.onSurface)
            Text(
                if (available) formatDuration(block.durationMs) else "Not on this phone",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
        }
        RoundIconButton("✕", onDelete, Modifier, dark = false)
    }
}

@Composable
private fun RoundIconButton(label: String, onClick: () -> Unit, modifier: Modifier, dark: Boolean = true) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(if (dark) Color.Black.copy(alpha = 0.55f) else colors.surface)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = if (dark) Color.White else colors.onSurfaceVariant, fontSize = 13.sp)
    }
}

@Composable
private fun RecordingBar(elapsedMs: Long, onCancel: () -> Unit, onSave: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(colors.errorContainer)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("●", color = colors.error, fontSize = 14.sp)
        Text(
            "Recording  ${formatDuration(elapsedMs)}",
            style = MaterialTheme.typography.titleSmall,
            color = colors.onErrorContainer,
            modifier = Modifier.weight(1f).padding(start = 10.dp),
        )
        StudyZenTextButton("Cancel", onClick = onCancel)
        StudyZenTextButton("Save", onClick = onSave)
    }
}

// --- Toolbar -------------------------------------------------------------------------------------

@Composable
private fun Toolbar(
    bold: Boolean,
    italic: Boolean,
    kind: BlockKind?,
    canFormat: Boolean,
    mediaSupported: Boolean,
    recording: Boolean,
    onBold: () -> Unit,
    onItalic: () -> Unit,
    onKind: (BlockKind) -> Unit,
    onPhoto: (camera: Boolean) -> Unit,
    onVoice: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(colors.surface)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ToolButton("B", "Bold", bold, canFormat, onBold, FontWeight.Bold)
        ToolButton("I", "Italic", italic, canFormat, onItalic, FontWeight.Normal, FontStyle.Italic)
        ToolButton("H", "Heading", kind == BlockKind.Heading, canFormat, { onKind(BlockKind.Heading) }, FontWeight.Bold)
        ToolButton("•", "Bulleted list", kind == BlockKind.Bullet, canFormat, { onKind(BlockKind.Bullet) })
        ToolButton("☑", "Checklist", kind == BlockKind.Check, canFormat, { onKind(BlockKind.Check) })
        if (mediaSupported) {
            Box(Modifier.padding(horizontal = 4.dp).size(width = 1.dp, height = 24.dp).background(colors.outline.copy(alpha = 0.4f)))
            ToolButton("Camera", "Take a photo", false, !recording, { onPhoto(true) }, small = true)
            ToolButton("Gallery", "Add a photo from your gallery", false, !recording, { onPhoto(false) }, small = true)
            ToolButton("Voice", "Record a voice note", recording, true, onVoice, small = true)
        }
    }
}

@Composable
private fun ToolButton(
    label: String,
    description: String,
    active: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    weight: FontWeight = FontWeight.Normal,
    fontStyle: FontStyle = FontStyle.Normal,
    small: Boolean = false,
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(12.dp)
    Box(
        Modifier
            .heightIn(min = 44.dp)
            .widthIn(min = 44.dp)
            .clip(shape)
            .background(if (active) colors.primaryContainer else Color.Transparent)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = if (small) 12.dp else 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            fontSize = if (small) 14.sp else 18.sp,
            fontWeight = if (small) FontWeight.Medium else weight,
            fontStyle = fontStyle,
            color = when {
                !enabled -> colors.onSurfaceVariant.copy(alpha = 0.4f)
                active -> colors.onPrimaryContainer
                else -> colors.onSurface
            },
        )
    }
}
