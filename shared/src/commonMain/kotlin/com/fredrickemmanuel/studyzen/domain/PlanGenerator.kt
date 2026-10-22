package com.fredrickemmanuel.studyzen.domain

import com.fredrickemmanuel.studyzen.core.time.CivilDate

/** One place in the calendar a plan wants a session. */
data class PlanSlot(val epochDay: Long, val block: TimeBlock)

/**
 * Turns a plan into dated sessions. The learner always reviews the result before it is saved.
 *
 *  - Only the plan's study days are used, each with that day's own times.
 *  - Runs from the later of the plan's start and [fromEpochDay], to the plan's end date.
 *  - Slots that overlap an existing session are skipped, so nothing ever overlaps.
 *  - Topics rotate (round robin), so no subject is studied alone all week.
 *  - With revision on, the first session of every third study day is a review.
 *  - Assignments: after every session, or on the last session of each week.
 */
object PlanGenerator {
    const val MAX_DAYS = 366

    /** Every slot the plan asks for, before clashes are removed. */
    fun slots(plan: StudyPlan, fromEpochDay: Long, firstDayAfterMinute: Int = 0): List<PlanSlot> {
        val first = maxOf(plan.startEpochDay, fromEpochDay)
        val last = minOf(plan.endEpochDay, first + MAX_DAYS - 1)
        val result = mutableListOf<PlanSlot>()
        var day = first
        while (day <= last) {
            val blocks = plan.weeklyBlocks[CivilDate.fromEpochDay(day).dayOfWeek].orEmpty()
            for (block in blocks.sortedBy { it.startMinute }) {
                if (day == fromEpochDay && block.startMinute < firstDayAfterMinute) continue
                result += PlanSlot(day, block)
            }
            day++
        }
        return result
    }

    fun suggest(
        plan: StudyPlan,
        topics: List<StudyTopic>,
        fromEpochDay: Long,
        existing: List<StudySession>,
        firstDayAfterMinute: Int = 0,
        newId: () -> String,
    ): List<StudySession> {
        if (topics.isEmpty()) return emptyList()

        val result = mutableListOf<StudySession>()
        var topicIndex = 0
        var studyDayCount = 0
        var lastDay = Long.MIN_VALUE
        var firstOfDay = true

        for (slot in slots(plan, fromEpochDay, firstDayAfterMinute)) {
            if (slot.epochDay != lastDay) {
                if (lastDay != Long.MIN_VALUE) studyDayCount++
                lastDay = slot.epochDay
                firstOfDay = true
            }
            val taken = ScheduleRules.clash(existing, slot.epochDay, slot.block) != null ||
                ScheduleRules.clash(result, slot.epochDay, slot.block) != null
            if (taken) { firstOfDay = false; continue }

            val topic = topics[topicIndex % topics.size]
            topicIndex++
            val isReview = plan.includeRevision && firstOfDay && studyDayCount % 3 == 2
            firstOfDay = false
            result += StudySession(
                id = newId(),
                title = topic.title,
                studyId = topic.studyId,
                subject = topic.subject,
                epochDay = slot.epochDay,
                startMinute = slot.block.startMinute,
                durationMinutes = slot.block.minutes,
                kind = if (isReview) SessionKind.Review else SessionKind.Study,
                priority = plan.priority,
                planId = plan.id,
                reminder = plan.reminder.takeIf { it.enabled },
            )
        }
        return withAssignments(result, plan.assignment)
    }

    private fun withAssignments(sessions: List<StudySession>, cadence: AssignmentCadence): List<StudySession> =
        when (cadence) {
            AssignmentCadence.None -> sessions
            AssignmentCadence.EverySession -> sessions.map { it.copy(hasAssignment = true) }
            AssignmentCadence.Weekly -> {
                val lastIdOfWeek = sessions
                    .groupBy { Stats.weekStart(it.epochDay) }
                    .mapValues { (_, week) -> week.maxWith(compareBy({ it.epochDay }, { it.startMinute })).id }
                    .values.toSet()
                sessions.map { if (it.id in lastIdOfWeek) it.copy(hasAssignment = true) else it }
            }
        }
}

/** A topic a plan can schedule: either an existing Study or a free-text subject. */
data class StudyTopic(
    val title: String,
    val studyId: String?,
    val subject: Subject,
)
