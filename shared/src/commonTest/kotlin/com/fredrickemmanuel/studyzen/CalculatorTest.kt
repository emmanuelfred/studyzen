package com.fredrickemmanuel.studyzen

import com.fredrickemmanuel.studyzen.feature.tools.calculator.AngleMode
import com.fredrickemmanuel.studyzen.feature.tools.calculator.CalcFormat
import com.fredrickemmanuel.studyzen.feature.tools.calculator.CalcResult
import com.fredrickemmanuel.studyzen.feature.tools.calculator.CalculatorState
import com.fredrickemmanuel.studyzen.feature.tools.calculator.ScientificCalculator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CalculatorTest {
    private fun calc(expr: String, mode: AngleMode = AngleMode.Degrees, ans: Double = 0.0): String =
        when (val r = ScientificCalculator.evaluate(expr, mode, ans)) {
            is CalcResult.Value -> CalcFormat.format(r.value)
            is CalcResult.Error -> "ERR:${r.message}"
        }

    @Test
    fun basicArithmeticFollowsTheOrderOfOperations() {
        assertEquals("14", calc("2+3×4"))
        assertEquals("20", calc("(2+3)×4"))
        assertEquals("2.5", calc("10÷4"))
        assertEquals("-4", calc("-2^2"))
        assertEquals("512", calc("2^3^2"))
        assertEquals("0.125", calc("2^-3"))
        assertEquals("5", calc("2−−3"))
        assertEquals("0.3", calc("0.1+0.2"))
        assertEquals("0.333333333333", calc("1÷3"))
        assertEquals("1234567.891", calc("1234567.891"))
    }

    @Test
    fun implicitMultiplicationBracketsAndPercent() {
        assertEquals("6.28318530718", calc("2π"))
        assertEquals("21", calc("3(4+3)"))
        assertEquals("77", calc("(5+6)(3+4)"))
        assertEquals("0.5", calc("50%"))
        assertEquals("5", calc("(2+3")) // a missing closing bracket is closed for you
    }

    @Test
    fun trigonometryInDegreesAndRadians() {
        assertEquals("0.5", calc("sin(30)"))
        assertEquals("0", calc("sin(180)"))
        assertEquals("0", calc("cos(90)"))
        assertEquals("1", calc("tan(45)"))
        assertEquals("30", calc("asin(0.5)"))
        assertEquals("1", calc("sin(π÷2)", AngleMode.Radians))
        assertEquals("0", calc("cos(π÷2)", AngleMode.Radians))
        assertEquals("ERR:Undefined", calc("tan(90)"))
        assertEquals("ERR:Use a number from -1 to 1", calc("asin(2)"))
        assertEquals("1", calc("3sin(30)×2÷3"))
    }

    @Test
    fun logsRootsPowersAndFactorials() {
        assertEquals("1", calc("ln(e)"))
        assertEquals("3", calc("log(1000)"))
        assertEquals("3", calc("log2(8)"))
        assertEquals("1", calc("exp(0)"))
        assertEquals("12", calc("√144"))
        assertEquals("3", calc("√(9)"))
        assertEquals("3", calc("cbrt(27)"))
        assertEquals("-3", calc("cbrt(-27)"))
        assertEquals("5", calc("abs(-5)"))
        assertEquals("120", calc("5!"))
        assertEquals("1", calc("0!"))
        assertEquals("3628800", calc("10!"))
        assertEquals("1000", calc("1E3"))
        assertEquals("0.0000015", calc("1.5E-6"))
        assertEquals("2", calc("Ans×2", ans = 1.0))
    }

    @Test
    fun errorsAreShortAndNeverCrash() {
        assertEquals("ERR:Can't divide by zero", calc("5÷0"))
        assertEquals("ERR:Can't divide by zero", calc("0^-1"))
        assertEquals("ERR:No real square root", calc("√(-4)"))
        assertEquals("ERR:ln needs a number above 0", calc("ln(0)"))
        assertEquals("ERR:Factorial needs a whole number", calc("2.5!"))
        assertEquals("ERR:Result too large", calc("171!"))
        assertEquals("ERR:Result too large", calc("10^400"))
        assertEquals("ERR:Not a real number", calc("(-8)^0.5"))
        for (bad in listOf("2+", "×3", "()", "2)", "1.2.3", "abc", "sin", "2++", "")) {
            assertTrue(calc(bad).startsWith("ERR:"), "'$bad' should be an error but was ${calc(bad)}")
        }
    }

    @Test
    fun numbersAreFormattedWithoutNoise() {
        assertEquals("0", CalcFormat.format(0.0))
        assertEquals("100", CalcFormat.format(100.0))
        assertEquals("-2.5", CalcFormat.format(-2.5))
        assertEquals("1E12", CalcFormat.format(1e12))
        assertEquals("1.23456789012E14", CalcFormat.format(123456789012345.0))
        assertEquals("1E-7", CalcFormat.format(1e-7))
        assertEquals("0.000001", CalcFormat.format(1e-6))
        assertEquals("1000", CalcFormat.format(999.9999999999999))
        assertEquals("1.2E-10", CalcFormat.format(1.2e-10))
    }

    @Test
    fun keypadStateBehavesLikeACalculator() {
        val s = CalculatorState()
        "12+3".forEach { s.input(it.toString()) }
        assertEquals("12+3", s.expression)
        assertEquals("15", s.preview)
        s.calculate()
        assertEquals("15", s.expression)
        assertEquals(15.0, s.ans)
        assertNull(s.preview)

        s.input("×") // carries on from the answer
        s.input("2")
        s.calculate()
        assertEquals("30", s.expression)

        s.input("7") // a digit starts fresh
        assertEquals("7", s.expression)

        s.input("×"); s.input("÷") // a second operator replaces the first
        assertEquals("7÷", s.expression)
        s.input("−"); assertEquals("7÷−", s.expression) // but − can mean negative
        s.clear()
        s.input("+")
        assertEquals("Ans+", s.expression)

        s.clear(); s.input("sin("); s.input("3"); s.backspace(); s.backspace()
        assertEquals("", s.expression) // backspace removes "sin(" in one go
        assertEquals(2, s.history.size)
    }

    @Test
    fun memoryHistoryAndErrors() {
        val s = CalculatorState()
        s.input("5"); s.input("×"); s.input("5")
        s.memoryAdd(1)
        s.memoryAdd(1)
        s.clear(); s.memoryRecall()
        assertEquals("50", s.expression)
        s.memoryClear()
        assertNull(s.memory)

        s.clear(); s.input("5"); s.input("÷"); s.input("0"); s.calculate()
        assertEquals("Can't divide by zero", s.error)
        assertEquals("5÷0", s.expression)
        s.input("1")
        assertNull(s.error)
    }
}
