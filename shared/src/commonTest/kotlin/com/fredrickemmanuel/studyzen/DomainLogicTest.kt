package com.fredrickemmanuel.studyzen

import com.fredrickemmanuel.studyzen.core.time.CivilDate
import com.fredrickemmanuel.studyzen.core.time.DayOfWeek
import com.fredrickemmanuel.studyzen.core.time.formatClock
import com.fredrickemmanuel.studyzen.core.time.formatMinutes
import com.fredrickemmanuel.studyzen.core.time.formatTimer
import com.fredrickemmanuel.studyzen.domain.PlanGenerator
import com.fredrickemmanuel.studyzen.domain.TimeBlock
import com.fredrickemmanuel.studyzen.domain.ScheduleRules
import com.fredrickemmanuel.studyzen.domain.AssignmentCadence
import com.fredrickemmanuel.studyzen.domain.SessionKind
import com.fredrickemmanuel.studyzen.domain.SessionStatus
import com.fredrickemmanuel.studyzen.domain.Stats
import com.fredrickemmanuel.studyzen.domain.StudyPlan
import com.fredrickemmanuel.studyzen.domain.StudySession
import com.fredrickemmanuel.studyzen.domain.StudyTopic
import com.fredrickemmanuel.studyzen.domain.Subject
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DomainLogicTest {

    @Test
    fun civilDateKnownValues() {
        assertEquals(0L, CivilDate(1970, 1, 1).epochDay)
        assertEquals(DayOfWeek.Thursday, CivilDate(1970, 1, 1).dayOfWeek)
        assertEquals(11_017L, CivilDate(2000, 3, 1).epochDay)
        assertEquals(DayOfWeek.Thursday, CivilDate(2024, 2, 29).dayOfWeek)
        assertEquals(DayOfWeek.Tuesday, CivilDate(2026, 10, 20).dayOfWeek)
    }

    @Test
    fun civilDateRoundTripsAndLeapYears() {
        for (day in -800L..30_000L step 37L) {
            assertEquals(day, CivilDate.fromEpochDay(day).epochDay)
        }
        assertEquals(29, CivilDate.daysInMonth(2024, 2))
        assertEquals(28, CivilDate.daysInMonth(2100, 2))
        assertEquals(CivilDate(2025, 2, 28), CivilDate(2025, 1, 31).plusMonths(1))
    }

    @Test
    fun formatting() {
        assertEquals("12:00 AM", formatClock(0))
        assertEquals("7:05 PM", formatClock(19 * 60 + 5))
        assertEquals("12:30 PM", formatClock(12 * 60 + 30))
        assertEquals("1h 30m", formatMinutes(90))
        assertEquals("45m", formatMinutes(45))
        assertEquals("2h", formatMinutes(120))
        assertEquals("02:05", formatTimer(125))
        assertEquals("1:02:05", formatTimer(3725))
    }

    private fun blocks(vararg b: Pair<Int, Int>) = b.map { TimeBlock(it.first * 60, it.second * 60) }

    private fun plan(
        days: Map<DayOfWeek, List<TimeBlock>>,
        start: Long,
        end: Long,
        revision: Boolean = false,
        assignment: AssignmentCadence = AssignmentCadence.None,
    ) = StudyPlan(
        id = "p", name = "Plan", goal = "", studyIds = emptyList(),
        startEpochDay = start, endEpochDay = end, weeklyBlocks = days,
        assignment = assignment, includeRevision = revision,
    )

    private val topics = listOf(
        StudyTopic("A", null, Subject.Programming),
        StudyTopic("B", null, Subject.Mathematics),
    )
    private val monday = CivilDate(2026, 10, 19).epochDay

    @Test
    fun generatorUsesEachDaysOwnTimesAndRotatesTopics() {
        var n = 0
        val p = plan(
            mapOf(DayOfWeek.Monday to blocks(18 to 19, 20 to 21), DayOfWeek.Wednesday to blocks(7 to 8)),
            monday, monday + 13,
        )
        val result = PlanGenerator.suggest(p, topics, monday, emptyList()) { "s${n++}" }

        assertEquals(6, result.size) // 2 Mondays x 2 + 2 Wednesdays x 1
        val mondays = result.filter { CivilDate.fromEpochDay(it.epochDay).dayOfWeek == DayOfWeek.Monday }
        assertEquals(listOf(18 * 60, 20 * 60, 18 * 60, 20 * 60), mondays.map { it.startMinute })
        assertTrue(result.filter { CivilDate.fromEpochDay(it.epochDay).dayOfWeek == DayOfWeek.Wednesday }.all { it.startMinute == 7 * 60 })
        assertEquals(listOf("A", "B"), result.take(2).map { it.title })
        assertEquals(result.size, result.map { it.id }.toSet().size)
        assertTrue(result.all { it.epochDay in monday..(monday + 13) })
    }

    @Test
    fun generatorNeverOverlapsExistingSessionsAndHandlesEmptyInput() {
        val busy = StudySession("x", "Busy", null, Subject.Other, monday, 18 * 60 + 30, 60)
        var n = 0
        val p = plan(mapOf(DayOfWeek.Monday to blocks(18 to 19, 20 to 21)), monday, monday)
        val result = PlanGenerator.suggest(p, topics, monday, listOf(busy)) { "s${n++}" }
        assertEquals(listOf(20 * 60), result.map { it.startMinute })

        assertTrue(PlanGenerator.suggest(plan(emptyMap(), monday, monday + 6), topics, monday, emptyList()) { "i" }.isEmpty())
        assertTrue(PlanGenerator.suggest(p, emptyList(), monday, emptyList()) { "i" }.isEmpty())
    }

    @Test
    fun generatorSkipsSlotsAlreadyPastToday() {
        val p = plan(mapOf(DayOfWeek.Monday to blocks(8 to 9, 18 to 19)), monday, monday)
        val result = PlanGenerator.suggest(p, topics, monday, emptyList(), firstDayAfterMinute = 12 * 60) { "n" }
        assertEquals(listOf(18 * 60), result.map { it.startMinute })
    }

    @Test
    fun generatorAddsReviewsAndAssignments() {
        var n = 0
        val everyDay = DayOfWeek.entries.associateWith { blocks(18 to 19) }
        val reviews = PlanGenerator.suggest(plan(everyDay, monday, monday + 6, revision = true), topics, monday, emptyList()) { "s${n++}" }
        assertTrue(reviews.any { it.kind == SessionKind.Review })

        val every = PlanGenerator.suggest(plan(everyDay, monday, monday + 6, assignment = AssignmentCadence.EverySession), topics, monday, emptyList()) { "e${n++}" }
        assertTrue(every.all { it.hasAssignment })

        val weekly = PlanGenerator.suggest(plan(everyDay, monday, monday + 13, assignment = AssignmentCadence.Weekly), topics, monday, emptyList()) { "w${n++}" }
        val flagged = weekly.filter { it.hasAssignment }
        assertEquals(2, flagged.size)
        assertEquals(listOf(monday + 6, monday + 13), flagged.map { it.epochDay }) // Sundays: last session of each week
    }

    @Test
    fun scheduleRulesCatchOverlapsAndShortSessions() {
        assertNull(ScheduleRules.dayError(blocks(18 to 19, 19 to 20))) // touching is fine
        assertNull(ScheduleRules.dayError(emptyList()))
        assertNotNull(ScheduleRules.dayError(blocks(18 to 20, 19 to 21)))
        assertNotNull(ScheduleRules.dayError(listOf(TimeBlock(600, 610))))
        assertNotNull(ScheduleRules.dayError(listOf(TimeBlock(600, 600))))

        assertEquals(TimeBlock(18 * 60, 19 * 60), ScheduleRules.nextBlock(emptyList()))
        assertEquals(TimeBlock(19 * 60 + 15, 20 * 60 + 15), ScheduleRules.nextBlock(blocks(18 to 19)))
        assertNull(ScheduleRules.nextBlock(listOf(TimeBlock(22 * 60, 24 * 60))))

        val s = StudySession("a", "A", null, Subject.Other, monday, 600, 60)
        assertNotNull(ScheduleRules.clash(listOf(s), monday, TimeBlock(630, 700)))
        assertNull(ScheduleRules.clash(listOf(s), monday, TimeBlock(660, 700)))
        assertNull(ScheduleRules.clash(listOf(s), monday, TimeBlock(630, 700), ignoreId = "a"))
        assertNull(ScheduleRules.clash(listOf(s.copy(status = SessionStatus.Skipped)), monday, TimeBlock(630, 700)))
    }

    private fun done(day: Long, minutes: Int, hour: Int = 18) = StudySession(
        "d$day$hour", "T", "st", Subject.Programming, day, hour * 60, minutes,
        status = SessionStatus.Completed, actualMinutes = minutes,
    )

    @Test
    fun statsCountOnlyRecordedWork() {
        val today = CivilDate(2026, 10, 20).epochDay
        val sessions = listOf(done(today, 30), done(today - 1, 45), done(today - 2, 60), done(today - 4, 20))
        assertEquals(3, Stats.streak(sessions, today))
        assertEquals(155, Stats.totalMinutes(sessions, (today - 10)..today))
        assertEquals(0, Stats.streak(emptyList(), today))
        assertNull(Stats.completionRate(emptyList(), today..today, today))
        assertNull(Stats.mostProductiveHour(sessions.take(2)))
        assertEquals(18, Stats.mostProductiveHour(sessions))
        assertEquals(today - 1, Stats.weekStart(today)) // Monday 19 Oct
    }
}
