package com.fredrickemmanuel.studyzen.data.ai

import com.fredrickemmanuel.studyzen.core.Ids
import com.fredrickemmanuel.studyzen.core.time.todayEpochDay
import com.fredrickemmanuel.studyzen.domain.Assessment
import com.fredrickemmanuel.studyzen.domain.AssessmentKind
import com.fredrickemmanuel.studyzen.domain.AssessmentQuestion
import com.fredrickemmanuel.studyzen.domain.LearningModule
import com.fredrickemmanuel.studyzen.domain.Lesson
import com.fredrickemmanuel.studyzen.domain.LessonMaterial
import com.fredrickemmanuel.studyzen.domain.LessonSources
import com.fredrickemmanuel.studyzen.domain.Study

sealed interface StudyAiOutcome<out T> {
    data class Ok<T>(val value: T, val provider: String) : StudyAiOutcome<T>
    data class Err(val message: String, val kind: AiFailure) : StudyAiOutcome<Nothing>
}

/**
 * What came back for a lesson's materials. The sources can arrive even when the AI could not answer
 * (and the other way round), so each part reports its own problem.
 */
data class MaterialResult(
    val material: LessonMaterial,
    /** Set when the AI explanation could not be written. */
    val aiError: String?,
    /** True when Wikipedia could not be reached. */
    val sourcesOffline: Boolean,
)

/** The Study Space's AI jobs, built on the learner's own key (AiService) and real Wikipedia sources. */
class StudyAi(
    private val service: AiService = AiService(),
    private val wiki: WikipediaClient = WikipediaClient(),
) {
    /** Asks the AI for modules and lessons. The learner reviews them before anything is saved. */
    suspend fun proposeStructure(settings: AiSettings, brief: StudyBrief): StudyAiOutcome<List<LearningModule>> {
        val ask = AiMessage(AiRole.User, StudyAiPrompts.structureRequest(brief))
        val one = service.ask(settings, StudyAiPrompts.STRUCTURE_SYSTEM, listOf(ask))
        if (one is AiResult.Failure) return StudyAiOutcome.Err(one.message, one.kind)
        one as AiResult.Success
        parseStructure(one.text) { Ids.next("les") }?.let { return StudyAiOutcome.Ok(it, one.provider.label) }

        // the answer was not usable JSON: ask once more, firmly
        val two = service.ask(
            settings, StudyAiPrompts.STRUCTURE_SYSTEM,
            listOf(ask, AiMessage(AiRole.Assistant, one.text), AiMessage(AiRole.User, StudyAiPrompts.STRUCTURE_RETRY)),
        )
        return when (two) {
            is AiResult.Failure -> StudyAiOutcome.Err(two.message, two.kind)
            is AiResult.Success -> parseStructure(two.text) { Ids.next("les") }
                ?.let { StudyAiOutcome.Ok(it, two.provider.label) }
                ?: StudyAiOutcome.Err("The AI answered in a format that could not be read. Please try again.", AiFailure.Empty)
        }
    }

    /** Fetches real sources and writes an explanation grounded in them. */
    suspend fun lessonMaterial(settings: AiSettings, study: Study, lesson: Lesson): MaterialResult {
        // sources first: the explanation can then lean on the retrieved text
        var articles = wiki.search(lesson.title)
        if (articles != null && articles.isEmpty()) articles = wiki.search(study.title)
        val sourcesOffline = articles == null
        val found = articles.orEmpty()
        val resources = LessonSources.build(study, lesson, found) { Ids.next("res") }

        val result = service.ask(
            settings,
            StudyAiPrompts.lessonSystem(study),
            listOf(AiMessage(AiRole.User, StudyAiPrompts.lessonRequest(study, lesson, found))),
        )
        val explanation = (result as? AiResult.Success)
        val material = LessonMaterial(
            explanation = explanation?.text.orEmpty(),
            explanationBy = explanation?.provider?.label.orEmpty(),
            resources = resources,
            generatedAtEpochDay = todayEpochDay(),
        )
        return MaterialResult(material, (result as? AiResult.Failure)?.message, sourcesOffline)
    }

    /** Writes questions for one lesson (or the whole study when [lesson] is null). */
    suspend fun generateAssessment(
        settings: AiSettings, study: Study, lesson: Lesson?, kind: AssessmentKind, count: Int,
    ): StudyAiOutcome<List<AssessmentQuestion>> {
        val ask = AiMessage(AiRole.User, StudyAiPrompts.assessmentRequest(study, lesson, kind, count))
        val system = StudyAiPrompts.assessmentSystem(study)
        val one = service.ask(settings, system, listOf(ask))
        if (one is AiResult.Failure) return StudyAiOutcome.Err(one.message, one.kind)
        one as AiResult.Success
        parseAssessment(one.text, kind) { Ids.next("q") }?.let { return StudyAiOutcome.Ok(it, one.provider.label) }
        val two = service.ask(
            settings, system,
            listOf(ask, AiMessage(AiRole.Assistant, one.text), AiMessage(AiRole.User, StudyAiPrompts.STRUCTURE_RETRY)),
        )
        return when (two) {
            is AiResult.Failure -> StudyAiOutcome.Err(two.message, two.kind)
            is AiResult.Success -> parseAssessment(two.text, kind) { Ids.next("q") }
                ?.let { StudyAiOutcome.Ok(it, two.provider.label) }
                ?: StudyAiOutcome.Err("The AI answered in a format that could not be read. Please try again.", AiFailure.Empty)
        }
    }

    /** Feedback on written answers. It is guidance, never an official grade. */
    suspend fun feedback(settings: AiSettings, assessment: Assessment): StudyAiOutcome<ParsedFeedback> {
        val ask = AiMessage(AiRole.User, StudyAiPrompts.feedbackRequest(assessment))
        val result = service.ask(settings, StudyAiPrompts.FEEDBACK_SYSTEM, listOf(ask))
        if (result is AiResult.Failure) return StudyAiOutcome.Err(result.message, result.kind)
        result as AiResult.Success
        val parsed = parseFeedback(result.text, assessment.questions.map { it.id })
            ?: return StudyAiOutcome.Err("The feedback could not be read. Please try again.", AiFailure.Empty)
        return StudyAiOutcome.Ok(parsed, result.provider.label)
    }

    /** A summary that may only use the retrieved [articles]. Null text means the AI could not be reached. */
    suspend fun summarise(settings: AiSettings, question: String, articles: List<WikiArticle>): StudyAiOutcome<String> {
        val result = service.ask(settings, StudyAiPrompts.RESEARCH_SYSTEM, listOf(AiMessage(AiRole.User, StudyAiPrompts.researchRequest(question, articles))))
        return when (result) {
            is AiResult.Failure -> StudyAiOutcome.Err(result.message, result.kind)
            is AiResult.Success -> StudyAiOutcome.Ok(result.text, result.provider.label)
        }
    }

    /** Plain search of Wikipedia. Null means it could not be reached. */
    suspend fun searchWikipedia(query: String, limit: Int = 5): List<WikiArticle>? = wiki.search(query, limit)
}
