package com.fredrickemmanuel.studyzen.domain

import kotlinx.serialization.Serializable

@Serializable
enum class BlockKind { Text, Heading, Bullet, Check, Image, Audio }

/**
 * One piece of a note. Text-like blocks ([BlockKind.Text], [BlockKind.Heading], [BlockKind.Bullet],
 * [BlockKind.Check]) use [text]; [bold] and [italic] apply to the whole block. Photos and voice
 * notes point at a file stored on the device through [media].
 */
@Serializable
data class NoteBlock(
    val id: String,
    val kind: BlockKind = BlockKind.Text,
    val text: String = "",
    val checked: Boolean = false,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val media: String? = null,
    val durationMs: Long = 0,
) {
    val isMedia: Boolean get() = kind == BlockKind.Image || kind == BlockKind.Audio
    val isText: Boolean get() = !isMedia
}

/** Turns an old plain-text note into blocks, one per line. */
fun blocksFromBody(body: String, newId: () -> String): List<NoteBlock> {
    val lines = body.split("\n")
    return lines.map { NoteBlock(id = newId(), text = it) }.ifEmpty { listOf(NoteBlock(id = newId())) }
}

fun formatDuration(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    val seconds = total % 60
    return "${total / 60}:${if (seconds < 10) "0" else ""}$seconds"
}

/** Plain text of the note: used for search and the preview in the notes list. */
fun List<NoteBlock>.toPlainText(): String = joinToString("\n") { b ->
    when (b.kind) {
        BlockKind.Text, BlockKind.Heading -> b.text
        BlockKind.Bullet -> "• ${b.text}"
        BlockKind.Check -> (if (b.checked) "☑ " else "☐ ") + b.text
        BlockKind.Image -> "[Photo]"
        BlockKind.Audio -> "[Voice note ${formatDuration(b.durationMs)}]"
    }
}.trim()

/** True when there is nothing worth saving: no words, no photos, no recordings. */
fun List<NoteBlock>.isEmptyNote(): Boolean = all { it.isText && it.text.isBlank() }

/** Files (photos and recordings) used by these blocks. */
fun List<NoteBlock>.mediaFiles(): List<String> = mapNotNull { it.media }

/** The first line of text, used as the title when the learner did not type one. */
fun List<NoteBlock>.firstLine(): String =
    firstOrNull { it.isText && it.text.isNotBlank() }?.text?.trim()?.take(40) ?: ""

/** What the editor does after the text of one block changed. */
data class BlockEdit(val blocks: List<NoteBlock>, val focusId: String?)

/**
 * Applies new text to block [index]. Pressing Enter puts a newline in the text; that splits the
 * block: lists continue with another item, headings continue as normal text, and pressing Enter
 * on an empty list item ends the list.
 */
fun applyTextEdit(blocks: List<NoteBlock>, index: Int, newText: String, newId: () -> String): BlockEdit {
    val block = blocks.getOrNull(index) ?: return BlockEdit(blocks, null)
    if (!block.isText) return BlockEdit(blocks, null)
    val newline = newText.indexOf('\n')
    if (newline < 0) {
        return BlockEdit(blocks.replaced(index, block.copy(text = newText)), null)
    }
    val before = newText.substring(0, newline)
    val after = newText.substring(newline + 1)
    val isList = block.kind == BlockKind.Bullet || block.kind == BlockKind.Check
    if (isList && before.isEmpty() && after.isEmpty()) {
        // Enter on an empty list item: stop the list
        return BlockEdit(blocks.replaced(index, block.copy(kind = BlockKind.Text, checked = false)), block.id)
    }
    val nextKind = when (block.kind) {
        BlockKind.Heading -> BlockKind.Text
        else -> block.kind
    }
    val next = NoteBlock(
        id = newId(),
        kind = nextKind,
        text = after,
        bold = if (block.kind == BlockKind.Heading) false else block.bold,
        italic = block.italic,
    )
    val result = blocks.toMutableList()
    result[index] = block.copy(text = before)
    result.add(index + 1, next)
    // pasted text can hold several lines: keep splitting until each line has its own block
    return if (after.contains('\n')) applyTextEdit(result, index + 1, after, newId) else BlockEdit(result, next.id)
}

/** Backspace on an empty text block removes it (never the only block). Returns the block to focus. */
fun removeEmptyBlock(blocks: List<NoteBlock>, index: Int): BlockEdit {
    val block = blocks.getOrNull(index) ?: return BlockEdit(blocks, null)
    if (block.isMedia || block.text.isNotEmpty() || blocks.size <= 1) return BlockEdit(blocks, null)
    if (block.kind != BlockKind.Text) {
        // first backspace turns a list item or heading back into plain text
        return BlockEdit(blocks.replaced(index, block.copy(kind = BlockKind.Text, checked = false)), block.id)
    }
    val result = blocks.toMutableList().also { it.removeAt(index) }
    val focus = result.subList(0, index).lastOrNull { it.isText } ?: result.firstOrNull { it.isText }
    return BlockEdit(result, focus?.id)
}

/**
 * Inserts a photo or voice note after block [afterIndex]. A trailing empty text block is added
 * so the learner can keep typing below it.
 */
fun insertMedia(blocks: List<NoteBlock>, afterIndex: Int, media: NoteBlock, newId: () -> String): BlockEdit {
    val result = blocks.toMutableList()
    val at = (afterIndex + 1).coerceIn(0, result.size)
    // if the block we insert after is an empty text line, put the media in its place
    val after = result.getOrNull(afterIndex)
    val replaceEmpty = after != null && after.kind == BlockKind.Text && after.text.isEmpty()
    return if (replaceEmpty) {
        result[afterIndex] = media
        val hasTextBelow = result.getOrNull(afterIndex + 1)?.isText == true
        if (!hasTextBelow) result.add(afterIndex + 1, NoteBlock(id = newId()))
        BlockEdit(result, result.getOrNull(afterIndex + 1)?.takeIf { it.isText }?.id)
    } else {
        result.add(at, media)
        val hasTextBelow = result.getOrNull(at + 1)?.isText == true
        if (!hasTextBelow) result.add(at + 1, NoteBlock(id = newId()))
        BlockEdit(result, result.getOrNull(at + 1)?.takeIf { it.isText }?.id)
    }
}

private fun List<NoteBlock>.replaced(index: Int, block: NoteBlock): List<NoteBlock> =
    toMutableList().also { it[index] = block }
