package com.fredrickemmanuel.studyzen

import com.fredrickemmanuel.studyzen.core.time.CivilDate
import com.fredrickemmanuel.studyzen.core.time.DayOfWeek
import com.fredrickemmanuel.studyzen.data.MemoryKeyValueStore
import com.fredrickemmanuel.studyzen.data.StorePersistence
import com.fredrickemmanuel.studyzen.data.StudyStore
import com.fredrickemmanuel.studyzen.domain.AssignmentCadence
import com.fredrickemmanuel.studyzen.domain.Reminder
import com.fredrickemmanuel.studyzen.domain.ReminderPlanner
import com.fredrickemmanuel.studyzen.domain.SessionStatus
import com.fredrickemmanuel.studyzen.domain.StudyPlan
import com.fredrickemmanuel.studyzen.domain.StudySession
import com.fredrickemmanuel.studyzen.domain.Subject
import com.fredrickemmanuel.studyzen.domain.TimeBlock
import com.fredrickemmanuel.studyzen.domain.UserProfile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RemindersTest {
    private val day = CivilDate(2026, 10, 26).epochDay

    private fun session(
        id: String = "s1",
        start: Int = 18 * 60,
        reminder: Reminder? = Reminder(minutesBefore = 10),
        status: SessionStatus = SessionStatus.Scheduled,
        epochDay: Long = day,
        assignment: Boolean = false,
    ) = StudySession(id, "Algebra", null, Subject.Mathematics, epochDay, start, 60, status = status, reminder = reminder, hasAssignment = assignment)

    @Test
    fun alarmRingsTheChosenMinutesBeforeStart() {
        val alarm = ReminderPlanner.plan(listOf(session()), UserProfile(), day).single()
        assertEquals(day * 1440 + 18 * 60 - 10, alarm.localEpochMinute)
        assertEquals(day, alarm.epochDay)
        assertEquals(17 * 60 + 50, alarm.minuteOfDay)
        assertTrue(alarm.body.contains("10 min"))
        assertFalse(alarm.silent)
    }

    @Test
    fun onlyScheduledFutureSessionsWithRemindersGetAlarms() {
        val sessions = listOf(
            session("a"),
            session("done", status = SessionStatus.Completed),
            session("skipped", status = SessionStatus.Skipped),
            session("off", reminder = Reminder(enabled = false)),
            session("none", reminder = null),
            session("past", epochDay = day - 1),
        )
        assertEquals(listOf("a"), ReminderPlanner.plan(sessions, UserProfile(), day).map { it.id })
        assertTrue(ReminderPlanner.plan(sessions, UserProfile(remindersEnabled = false), day).isEmpty())
    }

    @Test
    fun alarmsAreSortedAndMarkAssignments() {
        val alarms = ReminderPlanner.plan(
            listOf(session("late", start = 20 * 60), session("early", start = 8 * 60, assignment = true)),
            UserProfile(), day,
        )
        assertEquals(listOf("early", "late"), alarms.map { it.id })
        assertTrue(alarms.first().body.contains("Assignment due"))
    }

    @Test
    fun quietHoursAndSilentSoundMakeAlarmsSilent() {
        val quiet = UserProfile(quietHoursEnabled = true, quietStartMinute = 22 * 60, quietEndMinute = 7 * 60)
        val early = ReminderPlanner.plan(listOf(session(start = 6 * 60 + 30)), quiet, day).single()
        assertTrue(early.silent)
        val evening = ReminderPlanner.plan(listOf(session(start = 18 * 60)), quiet, day).single()
        assertFalse(evening.silent)
        val silentSound = ReminderPlanner.plan(listOf(session(reminder = Reminder(sound = "Silent"))), UserProfile(), day).single()
        assertTrue(silentSound.silent)

        assertTrue(ReminderPlanner.inWindow(23 * 60, 22 * 60, 7 * 60))
        assertTrue(ReminderPlanner.inWindow(60, 22 * 60, 7 * 60))
        assertFalse(ReminderPlanner.inWindow(12 * 60, 22 * 60, 7 * 60))
        assertFalse(ReminderPlanner.inWindow(12 * 60, 9 * 60, 9 * 60))
    }

    @Test
    fun reminderBeforeMidnightStartStillLandsOnTheRightDay() {
        val alarm = ReminderPlanner.plan(listOf(session(start = 5, reminder = Reminder(minutesBefore = 30))), UserProfile(), day).single()
        assertEquals(day - 1, alarm.epochDay)
        assertEquals(23 * 60 + 35, alarm.minuteOfDay)
    }

    @Test
    fun learnersDataSurvivesRestart() {
        val storage = MemoryKeyValueStore()
        val first = StudyStore(StorePersistence(storage))
        first.clearAll()
        val plan = StudyPlan(
            id = "p", name = "Exams", goal = "Pass", studyIds = listOf("x"), startEpochDay = day, endEpochDay = day + 13,
            weeklyBlocks = mapOf(DayOfWeek.Monday to listOf(TimeBlock(18 * 60, 19 * 60), TimeBlock(20 * 60, 21 * 60))),
            assignment = AssignmentCadence.Weekly,
            reminder = Reminder(minutesBefore = 15, sound = "Silent", vibrate = false),
        )
        first.savePlan(plan)
        first.saveSession(session("kept", assignment = true))
        first.profile = first.profile.copy(name = "Ada", quietHoursEnabled = true)
        first.saveNow()

        val second = StudyStore(StorePersistence(storage))
        assertEquals(listOf(plan), second.plans.toList())
        assertEquals("kept", second.sessions.single().id)
        assertTrue(second.sessions.single().hasAssignment)
        assertEquals("Ada", second.profile.name)
        assertTrue(second.profile.quietHoursEnabled)
        assertFalse(second.demoDataLoaded)
    }

    @Test
    fun withoutSavedDataTheSampleDataLoadsAndBrokenDataIsIgnored() {
        val fresh = StudyStore(StorePersistence(MemoryKeyValueStore()))
        assertTrue(fresh.demoDataLoaded)

        val broken = MemoryKeyValueStore().also { it.put("store.snapshot.v1", "{not json") }
        assertNotNull(StudyStore(StorePersistence(broken)).studies)
        assertTrue(StudyStore(StorePersistence(broken)).demoDataLoaded)
    }
}
