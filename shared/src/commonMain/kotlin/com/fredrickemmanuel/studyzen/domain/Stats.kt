package com.fredrickemmanuel.studyzen.domain

import com.fredrickemmanuel.studyzen.core.time.CivilDate
import com.fredrickemmanuel.studyzen.core.time.formatClock

/**
 * Statistics computed only from recorded sessions. Nothing here is estimated or invented:
 * if nothing was recorded, the results are zero or empty and the UI shows an empty state.
 */
object Stats {

    enum class Range(val label: String) { Day("Day"), Week("Week"), Month("Month"), AllTime("All Time") }

    /** Monday of the week containing [epochDay]. */
    fun weekStart(epochDay: Long): Long {
        val offset = CivilDate.fromEpochDay(epochDay).dayOfWeek.ordinal
        return epochDay - offset
    }

    /** Inclusive epoch-day range covered by [range] ending today. */
    fun daysIn(range: Range, today: Long, firstSessionDay: Long?): LongRange = when (range) {
        Range.Day -> today..today
        Range.Week -> weekStart(today)..(weekStart(today) + 6)
        Range.Month -> {
            val first = CivilDate.fromEpochDay(today).firstOfMonth()
            first.epochDay..(first.epochDay + first.daysInMonth - 1)
        }
        Range.AllTime -> (firstSessionDay ?: today)..today
    }

    fun completed(sessions: List<StudySession>, days: LongRange): List<StudySession> =
        sessions.filter { it.status == SessionStatus.Completed && it.epochDay in days }

    fun totalMinutes(sessions: List<StudySession>, days: LongRange): Int =
        completed(sessions, days).sumOf { it.actualMinutes }

    /**
     * Share of sessions that were finished, among those that are no longer upcoming
     * (completed or skipped, plus scheduled ones whose day has passed). Null when there are none.
     */
    fun completionRate(sessions: List<StudySession>, days: LongRange, today: Long): Int? {
        val past = sessions.filter {
            it.epochDay in days &&
                (it.status != SessionStatus.Scheduled || it.epochDay < today)
        }
        if (past.isEmpty()) return null
        return past.count { it.status == SessionStatus.Completed } * 100 / past.size
    }

    /** Consecutive days, ending today or yesterday, with at least one completed session. */
    fun streak(sessions: List<StudySession>, today: Long): Int {
        val days = sessions.filter { it.status == SessionStatus.Completed }.map { it.epochDay }.toSet()
        var day = if (today in days) today else today - 1
        var count = 0
        while (day in days) {
            count++
            day--
        }
        return count
    }

    /** Study time per study title within [days], largest first. */
    fun minutesByStudy(sessions: List<StudySession>, days: LongRange, titleOf: (String?) -> String): List<Pair<String, Int>> =
        completed(sessions, days)
            .groupBy { titleOf(it.studyId).ifBlank { it.title } }
            .map { (title, list) -> title to list.sumOf { s -> s.actualMinutes } }
            .sortedByDescending { it.second }

    /** The start hour (0-23) when the learner has studied the most, or null with less than 3 sessions. */
    fun mostProductiveHour(sessions: List<StudySession>): Int? {
        val done = sessions.filter { it.status == SessionStatus.Completed }
        if (done.size < 3) return null
        return done.groupBy { it.startMinute / 60 }
            .maxByOrNull { (_, list) -> list.sumOf { it.actualMinutes } }
            ?.key
    }

    /** Hours of study per bar for the chart, plus the labels under the bars. */
    fun chartSeries(range: Range, sessions: List<StudySession>, today: Long, firstSessionDay: Long?): Pair<List<Float>, List<String>> {
        fun hours(minutes: Int) = minutes / 60f
        return when (range) {
            Range.Day -> {
                val todays = sessions.filter { it.epochDay == today }.sortedBy { it.startMinute }
                todays.map { if (it.status == SessionStatus.Completed) hours(it.actualMinutes) else 0f } to
                    todays.map { formatClock(it.startMinute).replace(":00", "") }
            }
            Range.Week -> {
                val start = weekStart(today)
                val values = (0L..6L).map { hours(totalMinutes(sessions, (start + it)..(start + it))) }
                values to com.fredrickemmanuel.studyzen.core.time.DayOfWeek.entries.map { it.short }
            }
            Range.Month -> {
                val first = CivilDate.fromEpochDay(today).firstOfMonth()
                val weeks = (first.daysInMonth + 6) / 7
                val values = (0 until weeks).map { w ->
                    val from = first.epochDay + w * 7
                    val to = minOf(from + 6, first.epochDay + first.daysInMonth - 1)
                    hours(totalMinutes(sessions, from..to))
                }
                values to (1..weeks).map { "W$it" }
            }
            Range.AllTime -> {
                val now = CivilDate.fromEpochDay(today)
                val months = (5 downTo 0).map { now.firstOfMonth().plusMonths(-it) }
                val values = months.map { m ->
                    hours(totalMinutes(sessions, m.epochDay..(m.epochDay + m.daysInMonth - 1)))
                }
                values to months.map { it.monthName.take(3) }
            }
        }
    }

    fun greeting(minuteOfDay: Int): String = when {
        minuteOfDay < 12 * 60 -> "Good morning!"
        minuteOfDay < 17 * 60 -> "Good afternoon!"
        else -> "Good evening!"
    }
}
