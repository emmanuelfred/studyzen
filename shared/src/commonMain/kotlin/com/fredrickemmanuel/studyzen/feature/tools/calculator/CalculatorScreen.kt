package com.fredrickemmanuel.studyzen.feature.tools.calculator

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fredrickemmanuel.studyzen.core.Ids
import com.fredrickemmanuel.studyzen.domain.LogKind
import com.fredrickemmanuel.studyzen.domain.StudyLogEntry
import androidx.compose.runtime.LaunchedEffect
import com.fredrickemmanuel.studyzen.core.designsystem.components.ScreenScaffold
import com.fredrickemmanuel.studyzen.core.designsystem.components.StudyZenCard
import com.fredrickemmanuel.studyzen.core.designsystem.components.StudyZenTextButton
import com.fredrickemmanuel.studyzen.core.time.todayEpochDay
import com.fredrickemmanuel.studyzen.data.LocalStudyStore
import com.fredrickemmanuel.studyzen.domain.Note
import com.fredrickemmanuel.studyzen.domain.NoteSource
import com.fredrickemmanuel.studyzen.navigation.LocalAppNavigator

private enum class KeyStyle { Digit, Operator, Function, Action, Equals }

private class Key(val label: String, val style: KeyStyle, val selected: Boolean = false, val onClick: () -> Unit)

/** A full scientific calculator. [studyId] links anything saved to notes to that Study. */
@Composable
fun CalculatorScreen(studyId: String?, modifier: Modifier = Modifier, initialExpression: String? = null) {
    val nav = LocalAppNavigator.current
    val store = LocalStudyStore.current
    val colors = MaterialTheme.colorScheme
    val calc = remember {
        CalculatorState().also { c ->
            if (!initialExpression.isNullOrBlank()) c.recall(HistoryItem("", initialExpression))
        }
    }
    // keep each finished calculation in the study's history
    val newest = calc.history.firstOrNull()
    LaunchedEffect(newest) {
        if (studyId != null && newest != null) {
            store.addLog(StudyLogEntry(Ids.next("log"), studyId, LogKind.Calculator, "${newest.expression} = ${newest.result}", newest.expression, todayEpochDay()))
        }
    }
    var showHistory by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(false) }

    fun press(token: String) { saved = false; calc.input(token) }

    val s = calc.second
    ScreenScaffold(title = "Calculator", onBack = nav::back, modifier = modifier) {
        // --- Display
        StudyZenCard {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(calc.mode.label, style = MaterialTheme.typography.labelMedium, color = colors.primary)
                if (calc.second) Text("2nd", style = MaterialTheme.typography.labelMedium, color = colors.primary)
                if (calc.memory != null) Text("M", style = MaterialTheme.typography.labelMedium, color = colors.primary)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                if (calc.lastExpression.isNotEmpty() && calc.showingResult) "${calc.lastExpression} =" else " ",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.End,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
            val text = calc.expression.ifEmpty { "0" }
            Text(
                text,
                fontSize = when {
                    text.length <= 12 -> 40.sp
                    text.length <= 20 -> 30.sp
                    else -> 22.sp
                },
                color = colors.onSurface,
                textAlign = TextAlign.End,
                maxLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )
            val error = calc.error
            val preview = calc.preview
            Text(
                text = when {
                    error != null -> error
                    preview != null -> "= $preview"
                    else -> " "
                },
                style = MaterialTheme.typography.titleMedium,
                color = if (error != null) colors.error else colors.onSurfaceVariant,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            )
        }

        if (calc.showingResult && calc.lastExpression.isNotEmpty()) {
            StudyZenTextButton(
                if (saved) "Saved to your notes" else "Save to notes",
                enabled = !saved,
                onClick = {
                    store.saveNote(
                        Note(
                            id = Ids.next("note"),
                            studyId = studyId,
                            title = "Calculation: ${calc.lastExpression}",
                            body = "${calc.lastExpression} = ${calc.expression}",
                            source = NoteSource.Solver,
                            updatedAtEpochDay = todayEpochDay(),
                        ),
                    )
                    saved = true
                },
            )
        }

        // --- History
        if (showHistory) {
            Spacer(Modifier.height(8.dp))
            StudyZenCard {
                if (calc.history.isEmpty()) {
                    Text("Your calculations will appear here.", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                } else {
                    calc.history.take(8).forEach { item ->
                        Column(
                            Modifier.fillMaxWidth().clickable { calc.recall(item) }.padding(vertical = 6.dp),
                        ) {
                            Text(item.expression, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("= ${item.result}", style = MaterialTheme.typography.titleSmall, color = colors.onSurface)
                        }
                    }
                    StudyZenTextButton("Clear history", onClick = calc::clearHistory)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // --- Keys
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            KeyRow(
                Key("MC", KeyStyle.Action) { calc.memoryClear() },
                Key("MR", KeyStyle.Action) { saved = false; calc.memoryRecall() },
                Key("M+", KeyStyle.Action) { calc.memoryAdd(1) },
                Key("M−", KeyStyle.Action) { calc.memoryAdd(-1) },
                Key("History", KeyStyle.Action, selected = showHistory) { showHistory = !showHistory },
            )
            KeyRow(
                Key("2nd", KeyStyle.Function, selected = s) { calc.second = !calc.second },
                Key(calc.mode.label, KeyStyle.Function) {
                    calc.mode = if (calc.mode == AngleMode.Degrees) AngleMode.Radians else AngleMode.Degrees
                },
                Key("π", KeyStyle.Function) { press("π") },
                Key("e", KeyStyle.Function) { press("e") },
                Key("Ans", KeyStyle.Function) { press("Ans") },
            )
            KeyRow(
                Key(if (s) "sin⁻¹" else "sin", KeyStyle.Function) { press(if (s) "asin(" else "sin(") },
                Key(if (s) "cos⁻¹" else "cos", KeyStyle.Function) { press(if (s) "acos(" else "cos(") },
                Key(if (s) "tan⁻¹" else "tan", KeyStyle.Function) { press(if (s) "atan(" else "tan(") },
                Key(if (s) "eˣ" else "ln", KeyStyle.Function) { press(if (s) "exp(" else "ln(") },
                Key(if (s) "10ˣ" else "log", KeyStyle.Function) { press(if (s) "10^" else "log(") },
            )
            KeyRow(
                Key(if (s) "x³" else "x²", KeyStyle.Function) { press(if (s) "^3" else "^2") },
                Key("xʸ", KeyStyle.Function) { press("^") },
                Key(if (s) "∛x" else "√x", KeyStyle.Function) { press(if (s) "cbrt(" else "√(") },
                Key("1/x", KeyStyle.Function) { press("1÷(") },
                Key("n!", KeyStyle.Function) { press("!") },
            )
            KeyRow(
                Key("(", KeyStyle.Operator) { press("(") },
                Key(")", KeyStyle.Operator) { press(")") },
                Key("%", KeyStyle.Operator) { press("%") },
                Key("⌫", KeyStyle.Action) { saved = false; calc.backspace() },
                Key("AC", KeyStyle.Action) { saved = false; calc.clear() },
            )
            KeyRow(
                Key("7", KeyStyle.Digit) { press("7") }, Key("8", KeyStyle.Digit) { press("8") },
                Key("9", KeyStyle.Digit) { press("9") }, Key("÷", KeyStyle.Operator) { press("÷") },
            )
            KeyRow(
                Key("4", KeyStyle.Digit) { press("4") }, Key("5", KeyStyle.Digit) { press("5") },
                Key("6", KeyStyle.Digit) { press("6") }, Key("×", KeyStyle.Operator) { press("×") },
            )
            KeyRow(
                Key("1", KeyStyle.Digit) { press("1") }, Key("2", KeyStyle.Digit) { press("2") },
                Key("3", KeyStyle.Digit) { press("3") }, Key("−", KeyStyle.Operator) { press("−") },
            )
            KeyRow(
                Key("0", KeyStyle.Digit) { press("0") }, Key(".", KeyStyle.Digit) { press(".") },
                Key("EE", KeyStyle.Function) { press("E") }, Key("+", KeyStyle.Operator) { press("+") },
            )
            KeyRow(Key("=", KeyStyle.Equals) { saved = false; calc.calculate() })
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun KeyRow(vararg keys: Key) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        keys.forEach { CalcKey(it) }
    }
}

@Composable
private fun RowScope.CalcKey(key: Key) {
    val colors = MaterialTheme.colorScheme
    val (container, content) = when (key.style) {
        KeyStyle.Digit -> colors.surface to colors.onSurface
        KeyStyle.Operator -> colors.primaryContainer to colors.onPrimaryContainer
        KeyStyle.Function -> colors.secondaryContainer to colors.onSecondaryContainer
        KeyStyle.Action -> colors.surfaceVariant to colors.onSurfaceVariant
        KeyStyle.Equals -> colors.primary to colors.onPrimary
    }
    Box(
        modifier = Modifier
            .weight(1f)
            .height(if (key.style == KeyStyle.Equals) 52.dp else 46.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (key.selected) colors.primary else container)
            .clickable(role = Role.Button, onClick = key.onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            key.label,
            fontSize = if (key.label.length > 3) 13.sp else 17.sp,
            color = if (key.selected) colors.onPrimary else content,
            maxLines = 1,
        )
    }
}
