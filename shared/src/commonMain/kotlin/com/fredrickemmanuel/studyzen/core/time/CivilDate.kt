package com.fredrickemmanuel.studyzen.core.time

@kotlinx.serialization.Serializable
enum class DayOfWeek(val label: String, val short: String, val letter: String) {
    Monday("Monday", "Mon", "M"),
    Tuesday("Tuesday", "Tue", "T"),
    Wednesday("Wednesday", "Wed", "W"),
    Thursday("Thursday", "Thu", "T"),
    Friday("Friday", "Fri", "F"),
    Saturday("Saturday", "Sat", "S"),
    Sunday("Sunday", "Sun", "S"),
}

val MonthNames = listOf(
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December",
)

/**
 * A calendar date without a time zone. Small on purpose, so the app needs no date library.
 * Conversions use the well-known "days from civil" algorithm and are covered by unit tests.
 */
data class CivilDate(val year: Int, val month: Int, val day: Int) : Comparable<CivilDate> {

    /** Days since 1970-01-01. */
    val epochDay: Long
        get() {
            val y = (if (month <= 2) year - 1 else year).toLong()
            val era = (if (y >= 0) y else y - 399) / 400
            val yoe = y - era * 400
            val mp = (if (month > 2) month - 3 else month + 9).toLong()
            val doy = (153 * mp + 2) / 5 + day - 1
            val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
            return era * 146097 + doe - 719468
        }

    val dayOfWeek: DayOfWeek
        get() = DayOfWeek.entries[(((epochDay + 3) % 7 + 7) % 7).toInt()]

    val daysInMonth: Int get() = daysInMonth(year, month)

    val monthName: String get() = MonthNames[month - 1]

    fun plusDays(days: Long): CivilDate = fromEpochDay(epochDay + days)

    fun firstOfMonth(): CivilDate = CivilDate(year, month, 1)

    fun plusMonths(months: Int): CivilDate {
        val index = year * 12 + (month - 1) + months
        val newYear = index.floorDiv(12)
        val newMonth = index.mod(12) + 1
        return CivilDate(newYear, newMonth, minOf(day, daysInMonth(newYear, newMonth)))
    }

    override fun compareTo(other: CivilDate): Int = epochDay.compareTo(other.epochDay)

    /** "Tue, 20 Oct" */
    fun shortLabel(): String = "${dayOfWeek.short}, $day ${monthName.take(3)}"

    companion object {
        fun fromEpochDay(epochDay: Long): CivilDate {
            val z = epochDay + 719468
            val era = (if (z >= 0) z else z - 146096) / 146097
            val doe = z - era * 146097
            val yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365
            val y = yoe + era * 400
            val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
            val mp = (5 * doy + 2) / 153
            val d = (doy - (153 * mp + 2) / 5 + 1).toInt()
            val m = (if (mp < 10) mp + 3 else mp - 9).toInt()
            return CivilDate((if (m <= 2) y + 1 else y).toInt(), m, d)
        }

        fun isLeapYear(year: Int): Boolean = (year % 4 == 0 && year % 100 != 0) || year % 400 == 0

        fun daysInMonth(year: Int, month: Int): Int = when (month) {
            1, 3, 5, 7, 8, 10, 12 -> 31
            4, 6, 9, 11 -> 30
            else -> if (isLeapYear(year)) 29 else 28
        }

        /** Today's date on this device. */
        fun today(): CivilDate = fromEpochDay(todayEpochDay())
    }
}

private const val MillisPerDay = 86_400_000L

private fun localMillis(): Long = currentTimeMillis() + utcOffsetMinutes() * 60_000L

fun todayEpochDay(): Long = localMillis().floorDiv(MillisPerDay)

/** Minutes since local midnight right now (0..1439). */
fun nowMinuteOfDay(): Int = (localMillis().mod(MillisPerDay) / 60_000L).toInt()

/** 19:05 -> "7:05 PM". */
fun formatClock(minuteOfDay: Int): String {
    val m = minuteOfDay.mod(1440)
    val hour24 = m / 60
    val minute = m % 60
    val hour12 = when (val h = hour24 % 12) { 0 -> 12 else -> h }
    return "$hour12:${minute.toString().padStart(2, '0')} ${if (hour24 < 12) "AM" else "PM"}"
}

/** 90 -> "1h 30m", 45 -> "45m", 120 -> "2h". */
fun formatMinutes(totalMinutes: Int): String {
    val h = totalMinutes / 60
    val m = totalMinutes % 60
    return when {
        h == 0 -> "${m}m"
        m == 0 -> "${h}h"
        else -> "${h}h ${m}m"
    }
}

/** 3725 seconds -> "1:02:05", 125 -> "02:05". */
fun formatTimer(totalSeconds: Long): String {
    val s = totalSeconds.coerceAtLeast(0)
    val h = s / 3600
    val m = (s % 3600) / 60
    val sec = s % 60
    val mm = m.toString().padStart(2, '0')
    val ss = sec.toString().padStart(2, '0')
    return if (h > 0) "$h:$mm:$ss" else "$mm:$ss"
}
