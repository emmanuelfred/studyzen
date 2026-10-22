package com.fredrickemmanuel.studyzen.feature.tools.calculator

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.cosh
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.sin
import kotlin.math.sinh
import kotlin.math.sqrt
import kotlin.math.tan
import kotlin.math.tanh

enum class AngleMode(val label: String) { Degrees("DEG"), Radians("RAD") }

sealed interface CalcResult {
    data class Value(val value: Double) : CalcResult
    data class Error(val message: String) : CalcResult
}

private class CalcException(message: String) : Exception(message)

/**
 * Evaluates what the learner types: + - × ÷ ^ ( ) % !, constants π and e, Ans, and the functions
 * sin cos tan asin acos atan sinh cosh tanh ln log log2 √ cbrt abs exp.
 *
 * Multiplication may be left out ("2π", "3sin(30)", "(1+2)(3+4)"), a missing ")" at the end is
 * closed automatically, and "2^3^2" means 2^(3^2). Every failure becomes a short message.
 */
object ScientificCalculator {
    fun evaluate(expression: String, mode: AngleMode = AngleMode.Degrees, ans: Double = 0.0): CalcResult {
        if (expression.isBlank()) return CalcResult.Error("Enter a calculation")
        return try {
            val value = Parser(expression, mode, ans).parse()
            when {
                value.isNaN() -> CalcResult.Error("Not a real number")
                value.isInfinite() -> CalcResult.Error("Result too large")
                else -> CalcResult.Value(roundSig(value, 14))
            }
        } catch (e: CalcException) {
            CalcResult.Error(e.message.orEmpty())
        }
    }

    /** Rounds away floating-point noise: 0.30000000000000004 becomes 0.3. */
    internal fun roundSig(x: Double, digits: Int): Double {
        if (x == 0.0 || x.isNaN() || x.isInfinite()) return x
        val e = floor(log10(abs(x))).toInt()
        val shift = digits - 1 - e
        if (shift > 300 || shift < -300) return x
        val scale = 10.0.pow(shift)
        return round(x * scale) / scale
    }

    private class Parser(private val s: String, private val mode: AngleMode, private val ans: Double) {
        private var pos = 0

        fun parse(): Double {
            val value = expr()
            val next = peek()
            if (next == ')') throw CalcException("Extra closing bracket")
            if (next != null) throw CalcException("Check the expression")
            return value
        }

        private fun peek(): Char? {
            while (pos < s.length && s[pos] == ' ') pos++
            return s.getOrNull(pos)
        }

        private fun isAsciiLetter(c: Char) = c in 'a'..'z' || c in 'A'..'Z'

        private fun expr(): Double {
            var value = term()
            while (true) {
                when (peek()) {
                    '+' -> { pos++; value += term() }
                    '-', '−' -> { pos++; value -= term() }
                    else -> return value
                }
            }
        }

        private fun term(): Double {
            var value = unary()
            while (true) {
                val c = peek()
                when {
                    c == '*' || c == '×' || c == '·' -> { pos++; value *= unary() }
                    c == '/' || c == '÷' -> {
                        pos++
                        val divisor = unary()
                        if (divisor == 0.0) throw CalcException("Can't divide by zero")
                        value /= divisor
                    }
                    c != null && startsOperand(c) -> value *= unary() // multiplication left out: 2π, 3(4)
                    else -> return value
                }
            }
        }

        private fun startsOperand(c: Char) = c.isDigit() || c == '.' || c == '(' || c == 'π' || c == '√' || isAsciiLetter(c)

        private fun unary(): Double = when (peek()) {
            '-', '−' -> { pos++; -unary() }
            '+' -> { pos++; unary() }
            else -> power()
        }

        private fun power(): Double {
            val base = postfix()
            if (peek() == '^') {
                pos++
                val exponent = unary() // also makes ^ right-to-left and allows 2^-3
                if (base == 0.0 && exponent < 0) throw CalcException("Can't divide by zero")
                return base.pow(exponent)
            }
            return base
        }

        private fun postfix(): Double {
            var value = primary()
            while (true) {
                when (peek()) {
                    '!' -> { pos++; value = factorial(value) }
                    '%' -> { pos++; value /= 100.0 }
                    else -> return value
                }
            }
        }

        private fun primary(): Double {
            val c = peek() ?: throw CalcException("Check the expression")
            return when {
                c.isDigit() || c == '.' -> number()
                c == '(' -> { pos++; bracketed() }
                c == 'π' -> { pos++; PI }
                c == '√' -> { pos++; call("sqrt", primary()) }
                isAsciiLetter(c) -> word()
                else -> throw CalcException("Check the expression")
            }
        }

        /** After an opening bracket: the inside, then ")" (or the end of the text, which closes it). */
        private fun bracketed(): Double {
            val value = expr()
            if (peek() == ')') pos++
            return value
        }

        private fun number(): Double {
            val start = pos
            while (pos < s.length && (s[pos].isDigit() || s[pos] == '.')) pos++
            if (pos < s.length && s[pos] == 'E') {
                val a = s.getOrNull(pos + 1)
                val b = s.getOrNull(pos + 2)
                if (a != null && (a.isDigit() || ((a == '+' || a == '-') && b != null && b.isDigit()))) {
                    pos += 2
                    while (pos < s.length && s[pos].isDigit()) pos++
                }
            }
            return s.substring(start, pos).toDoubleOrNull() ?: throw CalcException("Check the number")
        }

        private fun word(): Double {
            val start = pos
            while (pos < s.length && isAsciiLetter(s[pos])) pos++
            var name = s.substring(start, pos).lowercase()
            if (name == "log" && s.getOrNull(pos) == '2') { pos++; name = "log2" }
            when (name) {
                "pi" -> return PI
                "e" -> return kotlin.math.E
                "ans" -> return ans
            }
            if (peek() != '(') throw CalcException("Use brackets, like $name(30)")
            pos++
            return call(name, bracketed())
        }

        private fun toRadians(x: Double) = if (mode == AngleMode.Degrees) x * PI / 180.0 else x
        private fun fromRadians(x: Double) = clean(if (mode == AngleMode.Degrees) x * 180.0 / PI else x)

        /** Removes rounding noise and turns tiny leftovers (sin 180° = 1.2e-16) into 0. */
        private fun clean(x: Double): Double {
            val r = roundSig(x, 14)
            return if (abs(r) < 1e-14) 0.0 else r
        }

        private fun call(name: String, x: Double): Double = when (name) {
            "sin" -> clean(sin(toRadians(x)))
            "cos" -> clean(cos(toRadians(x)))
            "tan" -> {
                if (abs(cos(toRadians(x))) < 1e-12) throw CalcException("Undefined")
                clean(tan(toRadians(x)))
            }
            "asin" -> { if (x < -1 || x > 1) throw CalcException("Use a number from -1 to 1"); fromRadians(asin(x)) }
            "acos" -> { if (x < -1 || x > 1) throw CalcException("Use a number from -1 to 1"); fromRadians(acos(x)) }
            "atan" -> fromRadians(atan(x))
            "sinh" -> sinh(x)
            "cosh" -> cosh(x)
            "tanh" -> tanh(x)
            "ln" -> { if (x <= 0) throw CalcException("ln needs a number above 0"); ln(x) }
            "log" -> { if (x <= 0) throw CalcException("log needs a number above 0"); roundSig(log10(x), 14) }
            "log2" -> { if (x <= 0) throw CalcException("log needs a number above 0"); roundSig(log2(x), 14) }
            "sqrt" -> { if (x < 0) throw CalcException("No real square root"); sqrt(x) }
            "cbrt" -> if (x < 0) -(-x).pow(1.0 / 3.0) else x.pow(1.0 / 3.0)
            "abs" -> abs(x)
            "exp" -> exp(x)
            else -> throw CalcException("Unknown function \"$name\"")
        }

        private fun factorial(x: Double): Double {
            if (x < 0 || x != floor(x)) throw CalcException("Factorial needs a whole number")
            if (x > 170) throw CalcException("Result too large")
            var result = 1.0
            for (i in 2..x.toInt()) result *= i
            return result
        }
    }
}

/** Turns a result into the text shown on screen, without ever printing floating-point noise. */
object CalcFormat {
    private const val SIG = 12
    private const val LIMIT = 1_000_000_000_000L // 10^SIG

    fun format(value: Double): String {
        if (value.isNaN()) return "Error"
        if (value.isInfinite()) return if (value > 0) "∞" else "-∞"
        if (value == 0.0) return "0"

        val negative = value < 0
        val a = abs(value)
        var exponent = floor(log10(a)).toInt()
        val total = SIG - 1 - exponent
        val half = total / 2
        var mantissa = round(a * 10.0.pow(half) * 10.0.pow(total - half)).toLong()
        while (mantissa >= LIMIT) { mantissa /= 10; exponent++ }
        while (mantissa in 1 until LIMIT / 10) { mantissa *= 10; exponent-- }

        val digits = mantissa.toString() // exactly SIG digits
        val trimmed = digits.trimEnd('0').ifEmpty { "0" }
        val text = if (exponent in -6..(SIG - 1)) {
            if (exponent >= 0) {
                val whole = digits.take(exponent + 1)
                val fraction = digits.drop(exponent + 1).trimEnd('0')
                if (fraction.isEmpty()) whole else "$whole.$fraction"
            } else {
                "0." + "0".repeat(-exponent - 1) + trimmed
            }
        } else {
            val lead = trimmed.take(1)
            val rest = trimmed.drop(1)
            (if (rest.isEmpty()) lead else "$lead.$rest") + "E" + exponent
        }
        return if (negative) "-$text" else text
    }
}
