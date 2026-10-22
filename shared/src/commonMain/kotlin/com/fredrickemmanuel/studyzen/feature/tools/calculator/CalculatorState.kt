package com.fredrickemmanuel.studyzen.feature.tools.calculator

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

data class HistoryItem(val expression: String, val result: String)

/** Everything the calculator remembers while it is open: what was typed, memory, angle mode, history. */
@Stable
class CalculatorState {
    var expression by mutableStateOf("")
        private set
    var lastExpression by mutableStateOf("")
        private set
    var mode by mutableStateOf(AngleMode.Degrees)
    var second by mutableStateOf(false)
    var memory by mutableStateOf<Double?>(null)
        private set
    var ans by mutableStateOf(0.0)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    /** True right after "=", when the display shows a result rather than something being typed. */
    var showingResult by mutableStateOf(false)
        private set

    val history = mutableStateListOf<HistoryItem>()

    /** The answer so far, shown in grey while typing. Null when it would just repeat what is typed. */
    val preview: String?
        get() {
            if (showingResult || expression.isBlank()) return null
            val plain = expression.removePrefix("-").all { it.isDigit() || it == '.' }
            if (plain) return null
            val r = ScientificCalculator.evaluate(expression, mode, ans)
            return if (r is CalcResult.Value) CalcFormat.format(r.value) else null
        }

    fun input(token: String) {
        error = null
        if (showingResult) {
            // an operator carries on from the answer; anything else starts a new calculation
            if (token !in CONTINUING) expression = ""
            showingResult = false
        }
        val binary = token in BINARY
        if (expression.isEmpty() && binary) {
            expression = "Ans$token"
            return
        }
        // typing a second operator replaces the first, except "−", which can start a negative number (2×−3)
        if (binary && token != "−" && expression.isNotEmpty() && expression.last().toString() in BINARY) {
            expression = expression.dropLast(1) + token
            return
        }
        expression += token
    }

    fun backspace() {
        error = null
        showingResult = false
        val tail = TOKENS.firstOrNull { expression.endsWith(it) }
        expression = if (tail != null) expression.dropLast(tail.length) else expression.dropLast(1)
    }

    fun clear() {
        expression = ""
        lastExpression = ""
        error = null
        showingResult = false
    }

    fun calculate() {
        if (expression.isBlank()) return
        when (val r = ScientificCalculator.evaluate(expression, mode, ans)) {
            is CalcResult.Value -> {
                val text = CalcFormat.format(r.value)
                history.add(0, HistoryItem(expression, text))
                while (history.size > MAX_HISTORY) history.removeAt(history.size - 1)
                lastExpression = expression
                ans = r.value
                expression = text
                showingResult = true
                error = null
            }
            is CalcResult.Error -> error = r.message
        }
    }

    fun recall(item: HistoryItem) {
        expression = item.result
        showingResult = false
        error = null
    }

    fun clearHistory() = history.clear()

    // --- Memory --------------------------------------------------------------------------------

    private fun currentValue(): Double? {
        if (expression.isBlank()) return ans
        return (ScientificCalculator.evaluate(expression, mode, ans) as? CalcResult.Value)?.value
    }

    fun memoryAdd(sign: Int) {
        val v = currentValue() ?: run { error = "Finish the calculation first"; return }
        memory = (memory ?: 0.0) + sign * v
    }

    fun memoryRecall() {
        memory?.let { input(CalcFormat.format(it)) }
    }

    fun memoryClear() { memory = null }

    private companion object {
        const val MAX_HISTORY = 30
        val BINARY = setOf("+", "−", "×", "÷", "^")
        val CONTINUING = BINARY + setOf("!", "%")

        /** Multi-character pieces that Backspace removes in one go. */
        val TOKENS = listOf(
            "asin(", "acos(", "atan(", "sinh(", "cosh(", "tanh(", "cbrt(", "log2(",
            "sin(", "cos(", "tan(", "log(", "exp(", "ln(", "√(", "Ans",
        )
    }
}
