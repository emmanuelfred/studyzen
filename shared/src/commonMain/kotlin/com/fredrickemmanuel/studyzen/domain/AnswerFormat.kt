package com.fredrickemmanuel.studyzen.domain

/** A run of text inside a line of an AI answer. */
data class MdSpan(
    val text: String,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val code: Boolean = false,
)

sealed interface MdBlock {
    data class Heading(val level: Int, val spans: List<MdSpan>) : MdBlock
    data class Paragraph(val spans: List<MdSpan>) : MdBlock
    data class Bullet(val spans: List<MdSpan>, val indent: Int) : MdBlock
    data class Numbered(val number: String, val spans: List<MdSpan>, val indent: Int) : MdBlock
    data class Quote(val spans: List<MdSpan>) : MdBlock
    data class Code(val text: String) : MdBlock
    data object Rule : MdBlock
}

fun List<MdSpan>.plain(): String = joinToString("") { it.text }

private val SUPER = mapOf(
    '0' to '⁰', '1' to '¹', '2' to '²', '3' to '³', '4' to '⁴', '5' to '⁵', '6' to '⁶',
    '7' to '⁷', '8' to '⁸', '9' to '⁹', '+' to '⁺', '-' to '⁻', 'n' to 'ⁿ',
)
private val SUB = mapOf(
    '0' to '₀', '1' to '₁', '2' to '₂', '3' to '₃', '4' to '₄',
    '5' to '₅', '6' to '₆', '7' to '₇', '8' to '₈', '9' to '₉',
)

private val SYMBOLS = mapOf(
    "times" to "×", "div" to "÷", "cdot" to "·", "pm" to "±", "mp" to "∓",
    "leq" to "≤", "le" to "≤", "geq" to "≥", "ge" to "≥", "neq" to "≠", "ne" to "≠",
    "approx" to "≈", "infty" to "∞", "pi" to "π", "theta" to "θ", "alpha" to "α", "beta" to "β",
    "gamma" to "γ", "delta" to "δ", "Delta" to "Δ", "lambda" to "λ", "mu" to "μ", "sigma" to "σ",
    "Sigma" to "Σ", "omega" to "ω", "Omega" to "Ω", "phi" to "φ", "epsilon" to "ε",
    "rightarrow" to "→", "to" to "→", "Rightarrow" to "⇒", "implies" to "⇒", "leftarrow" to "←",
    "sum" to "∑", "int" to "∫", "degree" to "°", "circ" to "°", "ldots" to "…", "dots" to "…",
    "left" to "", "right" to "", "quad" to " ", "qquad" to "  ", "displaystyle" to "",
    "sin" to "sin", "cos" to "cos", "tan" to "tan", "log" to "log", "ln" to "ln",
)

private val WRAPPERS = Regex("""\\(?:text|textbf|textit|mathrm|mathbf|mathit|boldsymbol|operatorname)\{([^{}]*)\}""")
private val FRAC = Regex("""\\[dt]?frac\{([^{}]*)\}\{([^{}]*)\}""")
private val SQRT = Regex("""\\sqrt\{([^{}]*)\}""")
private val CBRT = Regex("""\\sqrt\[3\]\{([^{}]*)\}""")
private val COMMAND = Regex("""\\([A-Za-z]+)""")
private val SUP_BRACED = Regex("""\^\{([^{}]*)\}""")
private val SUP_PLAIN = Regex("""\^(-?\d+|n)""")
private val SUB_BRACED = Regex("""([A-Za-z)\]])_\{(\d+)\}""")
private val SUB_PLAIN = Regex("""([A-Za-z)\]])_(\d+)""")
private val DOLLAR_MATH = Regex("""\$\$?([^$\n]+)\$\$?""")

private fun superscript(s: String): String? =
    if (s.isNotEmpty() && s.all { it in SUPER }) s.map { SUPER.getValue(it) }.joinToString("") else null

private fun looksLikeMath(inner: String) = inner.any { it == '\\' || it == '^' || it == '_' || it == '{' }

/**
 * AI models like to answer in LaTeX, which a phone cannot draw. This turns the common cases into
 * plain symbols (x², √(x), a/b, ×, ≤ ...) so maths reads naturally. Anything unknown is left as is.
 */
fun cleanMath(input: String): String {
    var s = input
    s = DOLLAR_MATH.replace(s) { m -> if (looksLikeMath(m.groupValues[1])) m.groupValues[1] else m.value }
    s = s.replace("\\[", "").replace("\\]", "").replace("\\(", "").replace("\\)", "")
    repeat(3) {
        s = WRAPPERS.replace(s) { it.groupValues[1] }
        s = CBRT.replace(s) { "∛(${it.groupValues[1]})" }
        s = SQRT.replace(s) { "√(${it.groupValues[1]})" }
        s = FRAC.replace(s) { m ->
            val a = m.groupValues[1].trim()
            val b = m.groupValues[2].trim()
            fun wrap(x: String) = if (x.all { it.isLetterOrDigit() || it == '.' }) x else "($x)"
            "${wrap(a)}/${wrap(b)}"
        }
    }
    s = SUP_BRACED.replace(s) { m -> superscript(m.groupValues[1]) ?: "^(${m.groupValues[1]})" }
    s = SUP_PLAIN.replace(s) { m -> superscript(m.groupValues[1]) ?: m.value }
    s = SUB_BRACED.replace(s) { m -> m.groupValues[1] + m.groupValues[2].map { SUB.getValue(it) }.joinToString("") }
    s = SUB_PLAIN.replace(s) { m -> m.groupValues[1] + m.groupValues[2].map { SUB.getValue(it) }.joinToString("") }
    s = COMMAND.replace(s) { m -> SYMBOLS[m.groupValues[1]] ?: m.value }
    s = s.replace("\\%", "%").replace("\\,", " ").replace("\\;", " ").replace("\\!", "")
    return s
}

private fun isWordChar(c: Char?) = c != null && (c.isLetterOrDigit())

/** Splits a line into bold / italic / code runs. Markers that are never closed stay as plain text. */
fun parseInline(text: String, bold: Boolean = false, italic: Boolean = false): List<MdSpan> {
    val out = mutableListOf<MdSpan>()
    val plain = StringBuilder()
    fun flush() {
        if (plain.isNotEmpty()) {
            out += MdSpan(plain.toString(), bold, italic)
            plain.clear()
        }
    }
    var i = 0
    while (i < text.length) {
        val c = text[i]
        if (c == '*' && text.startsWith("**", i)) {
            val end = text.indexOf("**", i + 2)
            if (end > i + 2 && text.substring(i + 2, end).isNotBlank()) {
                flush()
                out += parseInline(text.substring(i + 2, end), bold = true, italic = italic)
                i = end + 2
                continue
            }
        } else if (c == '*') {
            val next = text.getOrNull(i + 1)
            val before = text.getOrNull(i - 1)
            if (next != null && !next.isWhitespace() && next != '*' && !isWordChar(before)) {
                var end = i + 1
                var found = -1
                while (end < text.length) {
                    end = text.indexOf('*', end)
                    if (end < 0) break
                    if (!text[end - 1].isWhitespace() && !isWordChar(text.getOrNull(end + 1))) {
                        found = end
                        break
                    }
                    end++
                }
                if (found > i + 1) {
                    flush()
                    out += parseInline(text.substring(i + 1, found), bold = bold, italic = true)
                    i = found + 1
                    continue
                }
            }
        } else if (c == '`') {
            val end = text.indexOf('`', i + 1)
            if (end > i + 1) {
                flush()
                out += MdSpan(text.substring(i + 1, end), bold, italic, code = true)
                i = end + 1
                continue
            }
        }
        plain.append(c)
        i++
    }
    flush()
    return out
}

private val HEADING = Regex("""^(#{1,6})\s+(.*)$""")
private val RULE = Regex("""^(-{3,}|\*{3,}|_{3,})$""")
private val BULLET = Regex("""^(\s*)[-*•+]\s+(.*)$""")
private val NUMBERED = Regex("""^(\s*)(\d{1,3})[.)]\s+(.*)$""")
private val TABLE_SEPARATOR = Regex("""^\|?[\s:|-]+\|?$""")

/** Reads the Markdown an AI usually writes: headings, lists, bold/italic, code, quotes, simple tables. */
fun parseMarkdown(text: String): List<MdBlock> {
    val blocks = mutableListOf<MdBlock>()
    var inCode = false
    val code = mutableListOf<String>()
    val table = mutableListOf<String>()

    fun flushTable() {
        if (table.isNotEmpty()) {
            blocks += MdBlock.Code(table.joinToString("\n"))
            table.clear()
        }
    }

    for (raw in text.lines()) {
        val line = raw.trimEnd()
        val trimmed = line.trimStart()
        if (trimmed.startsWith("```")) {
            flushTable()
            if (inCode) {
                blocks += MdBlock.Code(code.joinToString("\n"))
                code.clear()
            }
            inCode = !inCode
            continue
        }
        if (inCode) {
            code += raw
            continue
        }
        if (trimmed.startsWith("|")) {
            if (!TABLE_SEPARATOR.matches(trimmed)) {
                val cells = trimmed.trim('|').split('|').map { cleanMath(it.trim()) }
                table += cells.joinToString("  |  ")
            }
            continue
        }
        flushTable()
        if (line.isBlank()) continue

        val clean = cleanMath(line)
        val cleanTrimmed = clean.trimStart()
        HEADING.matchEntire(cleanTrimmed)?.let {
            blocks += MdBlock.Heading(it.groupValues[1].length, parseInline(it.groupValues[2].trim()))
            continue
        }
        if (RULE.matches(cleanTrimmed)) {
            blocks += MdBlock.Rule
            continue
        }
        if (cleanTrimmed.startsWith(">")) {
            blocks += MdBlock.Quote(parseInline(cleanTrimmed.removePrefix(">").trim()))
            continue
        }
        BULLET.matchEntire(clean)?.let {
            blocks += MdBlock.Bullet(parseInline(it.groupValues[2]), it.groupValues[1].length / 2)
            continue
        }
        NUMBERED.matchEntire(clean)?.let {
            blocks += MdBlock.Numbered(it.groupValues[2], parseInline(it.groupValues[3]), it.groupValues[1].length / 2)
            continue
        }
        blocks += MdBlock.Paragraph(parseInline(cleanTrimmed))
    }
    flushTable()
    if (inCode && code.isNotEmpty()) blocks += MdBlock.Code(code.joinToString("\n"))
    return blocks
}

/** Turns an AI answer into note blocks, so it can be saved and edited like any other note. */
fun answerToBlocks(question: String?, answer: String, newId: () -> String): List<NoteBlock> {
    val out = mutableListOf<NoteBlock>()
    if (!question.isNullOrBlank()) {
        out += NoteBlock(newId(), BlockKind.Text, "Q: ${question.trim()}", italic = true)
    }
    for (block in parseMarkdown(answer)) {
        when (block) {
            is MdBlock.Heading -> out += NoteBlock(newId(), BlockKind.Heading, block.spans.plain())
            is MdBlock.Paragraph -> out += NoteBlock(
                newId(), BlockKind.Text, block.spans.plain(),
                bold = block.spans.isNotEmpty() && block.spans.all { it.bold },
            )
            is MdBlock.Bullet -> out += NoteBlock(newId(), BlockKind.Bullet, block.spans.plain())
            is MdBlock.Numbered -> out += NoteBlock(newId(), BlockKind.Text, "${block.number}. ${block.spans.plain()}")
            is MdBlock.Quote -> out += NoteBlock(newId(), BlockKind.Text, block.spans.plain(), italic = true)
            is MdBlock.Code -> block.text.lines().forEach { out += NoteBlock(newId(), BlockKind.Text, it) }
            MdBlock.Rule -> Unit
        }
    }
    if (out.isEmpty()) out += NoteBlock(newId(), BlockKind.Text, answer.trim())
    return out
}

/** A short title for a note made from a scan. */
fun scanTitle(question: String?, answer: String): String {
    val base = question?.trim()?.takeIf { it.isNotEmpty() }
        ?: parseMarkdown(answer).firstNotNullOfOrNull { b ->
            when (b) {
                is MdBlock.Heading -> b.spans.plain()
                is MdBlock.Paragraph -> b.spans.plain()
                else -> null
            }
        }
        ?: "Scanned problem"
    val oneLine = base.replace('\n', ' ').trim()
    return "Scan: " + if (oneLine.length > 40) oneLine.take(40).trimEnd() + "…" else oneLine
}
