package com.fredrickemmanuel.studyzen

import com.fredrickemmanuel.studyzen.domain.BlockKind
import com.fredrickemmanuel.studyzen.domain.MdBlock
import com.fredrickemmanuel.studyzen.domain.MdSpan
import com.fredrickemmanuel.studyzen.domain.answerToBlocks
import com.fredrickemmanuel.studyzen.domain.cleanMath
import com.fredrickemmanuel.studyzen.domain.parseInline
import com.fredrickemmanuel.studyzen.domain.parseMarkdown
import com.fredrickemmanuel.studyzen.domain.plain
import com.fredrickemmanuel.studyzen.domain.scanTitle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AnswerFormatTest {
    private var n = 0
    private fun id() = "b${n++}"

    @Test fun latexBecomesPlainSymbols() {
        assertEquals("x² + 3x = 0", cleanMath("x^2 + 3x = 0"))
        assertEquals("a/b", cleanMath("\\frac{a}{b}"))
        assertEquals("(x+1)/2", cleanMath("\\frac{x+1}{2}"))
        assertEquals("√(16) = 4", cleanMath("\\sqrt{16} = 4"))
        assertEquals("3 × 4 ≤ 12", cleanMath("3 \\times 4 \\leq 12"))
        assertEquals("x = 5", cleanMath("\\(x = 5\\)"))
        assertEquals("v₁ and 10⁻³", cleanMath("v_1 and 10^{-3}"))
        assertEquals("2¹⁰", cleanMath("2^10"))
    }

    @Test fun moneyIsLeftAlone() {
        assertEquals("It costs \$5 and \$6", cleanMath("It costs \$5 and \$6"))
        assertEquals("area = π r²", cleanMath("area = \$\\pi r^2\$"))
    }

    @Test fun inlineBoldItalicCode() {
        assertEquals(listOf(MdSpan("a "), MdSpan("b", bold = true), MdSpan(" c")), parseInline("a **b** c"))
        assertEquals(listOf(MdSpan("x "), MdSpan("y", italic = true)), parseInline("x *y*"))
        assertEquals(listOf(MdSpan("use "), MdSpan("f(x)", code = true)), parseInline("use `f(x)`"))
        // multiplication and unclosed markers stay as text
        assertEquals("2 * 3 * 4", parseInline("2 * 3 * 4").plain())
        assertEquals("2*3*4", parseInline("2*3*4").plain())
        assertEquals("**oops", parseInline("**oops").plain())
    }

    private val sample = """
        ## Solve
        Find x.

        1. Subtract 3
        2. Divide by **2**
          - sub point
        * item
        > note
        ---
        ```
        x = 4
        ```
        **Answer:** x = 4
    """.trimIndent()

    @Test fun markdownBlocks() {
        val blocks = parseMarkdown(sample)
        assertEquals(10, blocks.size)
        assertEquals(2, (blocks[0] as MdBlock.Heading).level)
        assertEquals("Find x.", (blocks[1] as MdBlock.Paragraph).spans.plain())
        assertEquals("1", (blocks[2] as MdBlock.Numbered).number)
        assertEquals("Divide by 2", (blocks[3] as MdBlock.Numbered).spans.plain())
        assertEquals(1, (blocks[4] as MdBlock.Bullet).indent)
        assertTrue(blocks[5] is MdBlock.Bullet)
        assertTrue(blocks[6] is MdBlock.Quote)
        assertTrue(blocks[7] is MdBlock.Rule)
        assertEquals("x = 4", (blocks[8] as MdBlock.Code).text)
        assertEquals(true, (blocks[9] as MdBlock.Paragraph).spans.first().bold)
    }

    @Test fun tablesAreKeptReadable() {
        val blocks = parseMarkdown("| x | y |\n|---|---|\n| 1 | 2 |\n\nafter")
        assertEquals("x  |  y\n1  |  2", (blocks[0] as MdBlock.Code).text)
        assertEquals("after", (blocks[1] as MdBlock.Paragraph).spans.plain())
    }

    @Test fun answerBecomesNoteBlocks() {
        val blocks = answerToBlocks("2x + 3 = 11", "## Steps\n- subtract\n1. divide\n**Answer:** x = 4") { id() }
        assertEquals(listOf(BlockKind.Text, BlockKind.Heading, BlockKind.Bullet, BlockKind.Text, BlockKind.Text), blocks.map { it.kind })
        assertEquals("Q: 2x + 3 = 11", blocks[0].text)
        assertEquals("1. divide", blocks[3].text)
        assertEquals("Answer: x = 4", blocks[4].text)
    }

    @Test fun titles() {
        assertEquals("Scan: 2x + 3 = 11", scanTitle("2x + 3 = 11", "x"))
        assertEquals("Scan: Photosynthesis", scanTitle(null, "## Photosynthesis\nPlants..."))
        assertEquals(true, scanTitle("a".repeat(80), "x").endsWith("…"))
    }
}
