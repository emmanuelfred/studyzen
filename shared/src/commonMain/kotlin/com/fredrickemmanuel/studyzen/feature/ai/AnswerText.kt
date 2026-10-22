package com.fredrickemmanuel.studyzen.feature.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.fredrickemmanuel.studyzen.domain.MdBlock
import com.fredrickemmanuel.studyzen.domain.MdSpan
import com.fredrickemmanuel.studyzen.domain.parseMarkdown

/** Draws an AI answer (headings, numbered steps, bullets, bold, code) and lets the learner select text. */
@Composable
fun AnswerText(markdown: String, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val blocks = remember(markdown) { parseMarkdown(markdown) }
    val codeBackground = colors.surfaceVariant

    SelectionContainer {
        Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            blocks.forEach { block ->
                when (block) {
                    is MdBlock.Heading -> Text(
                        styled(block.spans, codeBackground),
                        style = if (block.level <= 2) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = colors.onSurface,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                    is MdBlock.Paragraph -> Text(
                        styled(block.spans, codeBackground),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurface,
                    )
                    is MdBlock.Bullet -> Row(Modifier.padding(start = (block.indent * 16).dp)) {
                        Text("•", style = MaterialTheme.typography.bodyMedium, color = colors.primary, modifier = Modifier.width(18.dp))
                        Text(
                            styled(block.spans, codeBackground),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    is MdBlock.Numbered -> Row(Modifier.padding(start = (block.indent * 16).dp)) {
                        Text(
                            "${block.number}.",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.primary,
                            modifier = Modifier.width(26.dp),
                        )
                        Text(
                            styled(block.spans, codeBackground),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    is MdBlock.Quote -> Row {
                        Box(Modifier.width(3.dp).height(20.dp).background(colors.primary))
                        Text(
                            styled(block.spans, codeBackground),
                            style = MaterialTheme.typography.bodyMedium,
                            fontStyle = FontStyle.Italic,
                            color = colors.onSurfaceVariant,
                            modifier = Modifier.padding(start = 10.dp),
                        )
                    }
                    is MdBlock.Code -> Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(codeBackground)
                            .horizontalScroll(rememberScrollState())
                            .padding(10.dp),
                    ) {
                        Text(
                            block.text,
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurface,
                        )
                    }
                    MdBlock.Rule -> Box(Modifier.fillMaxWidth().height(1.dp).background(colors.outlineVariant))
                }
            }
        }
    }
}

private fun styled(spans: List<MdSpan>, codeBackground: Color): AnnotatedString = buildAnnotatedString {
    spans.forEach { span ->
        val style = SpanStyle(
            fontWeight = if (span.bold) FontWeight.Bold else null,
            fontStyle = if (span.italic) FontStyle.Italic else null,
            fontFamily = if (span.code) FontFamily.Monospace else null,
            background = if (span.code) codeBackground else Color.Unspecified,
        )
        withStyle(style) { append(span.text) }
    }
}
