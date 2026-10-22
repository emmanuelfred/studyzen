package com.fredrickemmanuel.studyzen

import com.fredrickemmanuel.studyzen.domain.BlockKind
import com.fredrickemmanuel.studyzen.domain.NoteBlock
import com.fredrickemmanuel.studyzen.domain.applyTextEdit
import com.fredrickemmanuel.studyzen.domain.blocksFromBody
import com.fredrickemmanuel.studyzen.domain.firstLine
import com.fredrickemmanuel.studyzen.domain.formatDuration
import com.fredrickemmanuel.studyzen.domain.insertMedia
import com.fredrickemmanuel.studyzen.domain.isEmptyNote
import com.fredrickemmanuel.studyzen.domain.removeEmptyBlock
import com.fredrickemmanuel.studyzen.domain.toPlainText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NoteBlocksTest {
    private var n = 0
    private fun id() = "b${n++}"
    private fun text(t: String, kind: BlockKind = BlockKind.Text) = NoteBlock(id(), kind, t)

    @Test fun oldNoteBecomesOneBlockPerLine() {
        val blocks = blocksFromBody("a\nb\n\nc") { id() }
        assertEquals(listOf("a", "b", "", "c"), blocks.map { it.text })
        assertEquals(1, blocksFromBody("") { id() }.size)
    }

    @Test fun plainTextCopy() {
        val blocks = listOf(
            text("Title", BlockKind.Heading), text("one", BlockKind.Bullet),
            text("buy", BlockKind.Check), NoteBlock(id(), BlockKind.Check, "done", checked = true),
            NoteBlock(id(), BlockKind.Image, media = "x.jpg"),
            NoteBlock(id(), BlockKind.Audio, media = "v.m4a", durationMs = 65_000),
        )
        assertEquals("Title\n• one\n☐ buy\n☑ done\n[Photo]\n[Voice note 1:05]", blocks.toPlainText())
        assertEquals("Title", blocks.firstLine())
    }

    @Test fun durationFormat() {
        assertEquals("0:07", formatDuration(7_400))
        assertEquals("12:00", formatDuration(720_000))
    }

    @Test fun emptyNote() {
        assertTrue(listOf(text("  ")).isEmptyNote())
        assertFalse(listOf(text(""), NoteBlock(id(), BlockKind.Image, media = "x")).isEmptyNote())
    }

    @Test fun enterSplitsAtCursorAndKeepsLists() {
        val list = listOf(text("milk", BlockKind.Check))
        val edit = applyTextEdit(list, 0, "mi\nlk") { id() }
        assertEquals(listOf("mi", "lk"), edit.blocks.map { it.text })
        assertEquals(BlockKind.Check, edit.blocks[1].kind)
        assertEquals(edit.blocks[1].id, edit.focusId)
    }

    @Test fun pastedLinesEachGetABlock() {
        val edit = applyTextEdit(listOf(text("x")), 0, "one\ntwo\nthree") { id() }
        assertEquals(listOf("one", "two", "three"), edit.blocks.map { it.text })
        assertEquals(edit.blocks[2].id, edit.focusId)
    }

    @Test fun enterOnHeadingContinuesAsText() {
        val edit = applyTextEdit(listOf(text("Intro", BlockKind.Heading)), 0, "Intro\n") { id() }
        assertEquals(BlockKind.Text, edit.blocks[1].kind)
    }

    @Test fun enterOnEmptyListItemEndsList() {
        val edit = applyTextEdit(listOf(text("", BlockKind.Bullet)), 0, "\n") { id() }
        assertEquals(1, edit.blocks.size)
        assertEquals(BlockKind.Text, edit.blocks[0].kind)
    }

    @Test fun backspaceOnEmptyBlock() {
        val blocks = listOf(text("a"), text(""))
        val edit = removeEmptyBlock(blocks, 1)
        assertEquals(1, edit.blocks.size)
        assertEquals(blocks[0].id, edit.focusId)
        // never removes the only block
        assertEquals(1, removeEmptyBlock(listOf(text("")), 0).blocks.size)
        // a list item turns into text first
        val step = removeEmptyBlock(listOf(text("a"), text("", BlockKind.Check)), 1)
        assertEquals(2, step.blocks.size)
        assertEquals(BlockKind.Text, step.blocks[1].kind)
    }

    @Test fun mediaIsInsertedWithSomewhereToType() {
        val photo = NoteBlock(id(), BlockKind.Image, media = "p.jpg")
        val replaced = insertMedia(listOf(text("a"), text("")), 1, photo) { id() }
        assertEquals(listOf(BlockKind.Text, BlockKind.Image, BlockKind.Text), replaced.blocks.map { it.kind })
        val after = insertMedia(listOf(text("hello")), 0, photo) { id() }
        assertEquals(listOf(BlockKind.Text, BlockKind.Image, BlockKind.Text), after.blocks.map { it.kind })
        assertEquals(after.blocks[2].id, after.focusId)
    }
}
