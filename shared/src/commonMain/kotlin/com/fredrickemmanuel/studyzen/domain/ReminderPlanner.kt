package com.fredrickemmanuel.studyzen.domain

import com.fredrickemmanuel.studyzen.core.time.formatClock
import com.fredrickemmanuel.studyzen.core.time.formatMinutes

/**
 * One alarm to ring. Times are the learner's wall-clock time (minutes since 1970-01-01 00:00 local),
 * so an alarm for "6:00 PM" stays at 6:00 PM even if the phone changes time zone.
 */
data class ReminderAlarm(
    val id: String,
    val sessionId: String,
    val localEpochMinute: Long,
    val title: String,
    val body: String,
    /** Rings with the phone's notification sound unless this is true. */
    val silent: Boolean,
    val vibrate: Boolean,
) {
    val epochDay: Long get() = localEpochMinute.floorDiv(1440L)
    val minuteOfDay: Int get() = localEpochMinute.mod(1440L).toInt()
}

/** Decides which alarms the phone should hold, from the sessions and notification settings. */
object ReminderPlanner {
    const val MAX_ALARMS = 200

    fun plan(sessions: List<StudySession>, profile: UserProfile, todayEpochDay: Long): List<ReminderAlarm> {
        if (!profile.remindersEnabled) return emptyList()
        return sessions
            .asSequence()
            .filter { !it.isSample && it.status == SessionStatus.Scheduled && it.epochDay >= todayEpochDay }
            .mapNotNull { session ->
                val reminder = session.reminder?.takeIf { it.enabled } ?: return@mapNotNull null
                val at = session.epochDay * 1440L + session.startMinute - reminder.minutesBefore
                val minuteOfDay = at.mod(1440L).toInt()
                ReminderAlarm(
                    id = session.id,
                    sessionId = session.id,
                    localEpochMinute = at,
                    title = session.title,
                    body = bodyFor(session, reminder),
                    silent = reminder.sound == "Silent" ||
                        (profile.quietHoursEnabled && inWindow(minuteOfDay, profile.quietStartMinute, profile.quietEndMinute)),
                    vibrate = reminder.vibrate,
                )
            }
            .sortedBy { it.localEpochMinute }
            .take(MAX_ALARMS)
            .toList()
    }

    private fun bodyFor(session: StudySession, reminder: Reminder): String {
        val whenText = if (reminder.minutesBefore == 0) "Starting now" else "Starts in ${reminder.minutesBefore} min"
        val parts = mutableListOf(
            "$whenText · ${formatClock(session.startMinute)}",
            formatMinutes(session.durationMinutes),
        )
        if (session.hasAssignment) parts += "Assignment due"
        return parts.joinToString(" · ")
    }

    /** True when [minute] falls in the window, which may wrap past midnight (22:00 to 07:00). */
    fun inWindow(minute: Int, start: Int, end: Int): Boolean = when {
        start == end -> false
        start < end -> minute in start until end
        else -> minute >= start || minute < end
    }
}
