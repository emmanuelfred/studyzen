package com.fredrickemmanuel.studyzen.domain

import com.fredrickemmanuel.studyzen.core.time.formatClock

/** The rules every study schedule must follow, shared by the plan builder and the session editor. */
object ScheduleRules {
    const val MIN_BLOCK_MINUTES = 15
    const val DEFAULT_BLOCK_MINUTES = 60
    const val DEFAULT_GAP_MINUTES = 15
    const val LAST_START_MINUTE = 24 * 60 - MIN_BLOCK_MINUTES

    /** An error message for one session time, or null when it is fine. */
    fun blockError(block: TimeBlock): String? = when {
        block.startMinute < 0 || block.endMinute > 24 * 60 -> "Times must be within the same day"
        block.minutes < MIN_BLOCK_MINUTES -> "A session must be at least $MIN_BLOCK_MINUTES minutes"
        else -> null
    }

    /** The first problem in one day's sessions (including overlaps), or null when the day is fine. */
    fun dayError(blocks: List<TimeBlock>): String? {
        blocks.forEach { blockError(it)?.let { message -> return message } }
        val sorted = blocks.sortedBy { it.startMinute }
        for (i in 0 until sorted.size - 1) {
            val a = sorted[i]
            val b = sorted[i + 1]
            if (a.overlaps(b)) {
                return "${formatClock(a.startMinute)}–${formatClock(a.endMinute)} overlaps " +
                    "${formatClock(b.startMinute)}–${formatClock(b.endMinute)}"
            }
        }
        return null
    }

    /** A sensible next session after the last one of the day, or null when the day is full. */
    fun nextBlock(existing: List<TimeBlock>, defaultStart: Int = 18 * 60): TimeBlock? {
        val start = if (existing.isEmpty()) defaultStart else existing.maxOf { it.endMinute } + DEFAULT_GAP_MINUTES
        if (start > LAST_START_MINUTE) return null
        val end = minOf(start + DEFAULT_BLOCK_MINUTES, 24 * 60)
        return TimeBlock(start, end).takeIf { it.minutes >= MIN_BLOCK_MINUTES }
    }

    /** The session already booked at that time, if any (ignoring [ignoreId], the one being edited). */
    fun clash(sessions: List<StudySession>, epochDay: Long, block: TimeBlock, ignoreId: String? = null): StudySession? =
        sessions.firstOrNull {
            it.id != ignoreId && it.epochDay == epochDay && it.status != SessionStatus.Skipped &&
                TimeBlock(it.startMinute, it.endMinute).overlaps(block)
        }
}
