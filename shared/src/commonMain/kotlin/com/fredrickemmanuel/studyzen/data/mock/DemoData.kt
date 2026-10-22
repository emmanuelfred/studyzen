package com.fredrickemmanuel.studyzen.data.mock

import com.fredrickemmanuel.studyzen.core.Ids
import com.fredrickemmanuel.studyzen.core.time.DayOfWeek
import com.fredrickemmanuel.studyzen.core.time.todayEpochDay
import com.fredrickemmanuel.studyzen.data.StudyStore
import com.fredrickemmanuel.studyzen.domain.AssessmentResult
import com.fredrickemmanuel.studyzen.domain.KnowledgeLevel
import com.fredrickemmanuel.studyzen.domain.LearningModule
import com.fredrickemmanuel.studyzen.domain.Lesson
import com.fredrickemmanuel.studyzen.domain.Note
import com.fredrickemmanuel.studyzen.domain.NoteSource
import com.fredrickemmanuel.studyzen.domain.AssignmentCadence
import com.fredrickemmanuel.studyzen.domain.PlanGenerator
import com.fredrickemmanuel.studyzen.domain.TimeBlock
import com.fredrickemmanuel.studyzen.domain.Priority
import com.fredrickemmanuel.studyzen.domain.Reminder
import com.fredrickemmanuel.studyzen.domain.SessionKind
import com.fredrickemmanuel.studyzen.domain.SessionStatus
import com.fredrickemmanuel.studyzen.domain.Study
import com.fredrickemmanuel.studyzen.domain.StudyPlan
import com.fredrickemmanuel.studyzen.domain.StudySession
import com.fredrickemmanuel.studyzen.domain.StudyTopic
import com.fredrickemmanuel.studyzen.domain.Subject

/**
 * Sample content so every screen can be seen without typing data in. It is generated relative
 * to today's date. Turn it off in Profile > Developer options (or by clearing data).
 */
object DemoData {

    private fun lessons(done: Int, vararg titles: String): List<Lesson> =
        titles.mapIndexed { i, t -> Lesson(Ids.next("les"), t, "", completed = i < done) }

    fun populate(store: StudyStore) {
        val today = todayEpochDay()

        val dataStructures = Study(
            id = "demo-ds",
            title = "Data Structures",
            description = "Arrays, linked lists, stacks, queues and hash tables.",
            objective = "Choose the right data structure for any problem.",
            subject = Subject.Programming,
            level = KnowledgeLevel.Intermediate,
            modules = listOf(
                LearningModule(
                    "demo-ds-m1", "Foundations",
                    lessons(2, "Arrays and memory", "Big-O basics"),
                ),
                LearningModule(
                    "demo-ds-m2", "Linear structures",
                    lessons(1, "Linked lists", "Stacks", "Queues"),
                ),
            ),
            createdAtEpochDay = today - 12,
        )
        val discrete = Study(
            id = "demo-dm",
            title = "Discrete mathematics",
            description = "Logic, sets and counting techniques.",
            objective = "Reason formally and count possibilities.",
            subject = Subject.Mathematics,
            level = KnowledgeLevel.Beginner,
            modules = listOf(
                LearningModule("demo-dm-m1", "Logic and sets", lessons(0, "Propositions", "Sets and operations")),
                LearningModule("demo-dm-m2", "Counting", lessons(0, "Permutations", "Combinations")),
            ),
            createdAtEpochDay = today - 8,
        )
        val problemSolving = Study(
            id = "demo-ps",
            title = "Problem Solving",
            description = "Strategies for breaking down hard problems.",
            objective = "Solve unfamiliar problems with a repeatable method.",
            subject = Subject.Programming,
            level = KnowledgeLevel.Intermediate,
            modules = listOf(
                LearningModule(
                    "demo-ps-m1", "Strategies",
                    lessons(3, "Understand the problem", "Plan an approach", "Work backwards", "Find patterns"),
                ),
                LearningModule(
                    "demo-ps-m2", "Practice sets",
                    lessons(0, "Warm-up set", "Arrays and strings", "Recursion", "Greedy methods", "Dynamic programming", "Mixed review"),
                ),
            ),
            createdAtEpochDay = today - 5,
        )
        store.studies.addAll(listOf(dataStructures, discrete, problemSolving))

        // Today
        fun session(
            day: Long, start: Int, minutes: Int, study: Study, kind: SessionKind,
            status: SessionStatus = SessionStatus.Scheduled, actual: Int = 0, planId: String? = null,
        ) = StudySession(
            id = Ids.next("ses"), title = study.title, studyId = study.id, subject = study.subject,
            epochDay = day, startMinute = start, durationMinutes = minutes, kind = kind,
            priority = Priority.Medium, status = status, actualMinutes = actual, planId = planId,
            reminder = if (status == SessionStatus.Scheduled) Reminder() else null,
        )

        store.sessions.addAll(
            listOf(
                session(today, 16 * 60, 90, dataStructures, SessionKind.Study),
                session(today, 18 * 60, 45, discrete, SessionKind.Review),
                session(today, 19 * 60 + 30, 60, problemSolving, SessionKind.Practice),
            ),
        )

        // The last week, for the Progress charts
        val history = listOf(
            Triple(-6, dataStructures, 75), Triple(-5, discrete, 40), Triple(-5, problemSolving, 55),
            Triple(-4, dataStructures, 90), Triple(-3, problemSolving, 60), Triple(-2, discrete, 45),
            Triple(-2, dataStructures, 80), Triple(-1, problemSolving, 70),
        )
        history.forEach { (offset, study, minutes) ->
            store.sessions.add(
                session(today + offset, 18 * 60, minutes, study, SessionKind.Study, SessionStatus.Completed, minutes),
            )
        }

        // A plan with upcoming sessions
        val plan = StudyPlan(
            id = "demo-plan",
            name = "Semester prep",
            goal = "Finish all three courses before exams.",
            studyIds = listOf(dataStructures.id, discrete.id, problemSolving.id),
            startEpochDay = today + 1,
            endEpochDay = today + 28,
            weeklyBlocks = listOf(
                DayOfWeek.Monday, DayOfWeek.Tuesday, DayOfWeek.Wednesday, DayOfWeek.Thursday, DayOfWeek.Friday,
            ).associateWith { listOf(TimeBlock(16 * 60, 17 * 60), TimeBlock(18 * 60, 19 * 60)) },
            assignment = AssignmentCadence.Weekly,
            includeRevision = true,
        )
        store.plans.add(plan)
        store.sessions.addAll(
            PlanGenerator.suggest(
                plan = plan,
                topics = listOf(dataStructures, discrete, problemSolving).map { StudyTopic(it.title, it.id, it.subject) },
                fromEpochDay = today + 1,
                existing = store.sessions.toList(),
                newId = { Ids.next("ses") },
            ),
        )

        store.notes.addAll(
            listOf(
                Note(
                    "demo-n1", dataStructures.id, "Stack vs queue",
                    "Stack: last in, first out (undo history).\nQueue: first in, first out (print jobs).",
                    NoteSource.Manual, today - 3,
                ),
                Note(
                    "demo-n2", dataStructures.id, "Why arrays are fast",
                    "Items sit next to each other in memory, so index lookup is a single calculation: O(1).",
                    NoteSource.Ai, today - 1,
                ),
            ),
        )
        store.results.addAll(
            listOf(
                AssessmentResult("demo-r1", dataStructures.id, "Data Structures quiz", 4, 5, today - 4, listOf("Stacks")),
                AssessmentResult("demo-r2", discrete.id, "Discrete mathematics quiz", 3, 5, today - 2, listOf("Counting", "Logic")),
            ),
        )

        // sample sessions are only for showing the app, so they never ring the phone
        for (i in store.sessions.indices) store.sessions[i] = store.sessions[i].copy(isSample = true)

        store.profile = store.profile.copy(
            name = "Student",
            interests = setOf(Subject.Programming, Subject.Mathematics),
            personalized = true,
        )
    }
}
