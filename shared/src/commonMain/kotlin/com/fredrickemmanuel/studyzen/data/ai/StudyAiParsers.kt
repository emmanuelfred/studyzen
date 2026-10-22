package com.fredrickemmanuel.studyzen.data.ai

import com.fredrickemmanuel.studyzen.domain.AssessmentKind
import com.fredrickemmanuel.studyzen.domain.AssessmentQuestion
import com.fredrickemmanuel.studyzen.domain.LearningModule
import com.fredrickemmanuel.studyzen.domain.Lesson
import com.fredrickemmanuel.studyzen.domain.LessonKind

/** Turns what the AI wrote into the app's own models. Anything unreadable is dropped, never guessed. */

private const val MAX_MODULES = 12
private const val MAX_LESSONS_PER_MODULE = 14
private const val MAX_LESSONS = 60

private fun lessonKind(text: String?): LessonKind = when (text?.trim()?.lowercase()) {
    "practice", "exercise", "exercises" -> LessonKind.Practice
    "revision", "review", "revise" -> LessonKind.Revision
    "checkpoint", "assessment", "test", "quiz", "mock" -> LessonKind.Assessment
    else -> LessonKind.Learn
}

/**
 * `{"modules":[{"title":"..","lessons":[{"title":"..","summary":"..","objectives":[".."],"kind":"learn"}]}]}`.
 * A bare list of modules is accepted too. Returns null when no lessons could be read.
 */
fun parseStructure(text: String, newId: () -> String): List<LearningModule>? {
    val root = MiniJson.extract(text)
    val modulesRaw = root.asMap()?.get("modules").asList() ?: root.asList() ?: return null
    var count = 0
    val modules = modulesRaw.take(MAX_MODULES).mapNotNull { raw ->
        val m = raw.asMap() ?: return@mapNotNull null
        val title = m["title"].asText() ?: m["name"].asText() ?: return@mapNotNull null
        val lessons = (m["lessons"].asList() ?: emptyList()).take(MAX_LESSONS_PER_MODULE).mapNotNull inner@{ lr ->
            if (count >= MAX_LESSONS) return@inner null
            val l = lr.asMap() ?: return@inner null
            val lessonTitle = l["title"].asText() ?: l["name"].asText() ?: return@inner null
            count++
            Lesson(
                id = newId(),
                title = lessonTitle,
                summary = l["summary"].asText() ?: l["description"].asText() ?: "",
                objectives = l["objectives"].asTextList().take(5),
                kind = lessonKind(l["kind"].asText() ?: l["type"].asText()),
            )
        }
        if (lessons.isEmpty()) null else LearningModule(newId(), title, lessons)
    }
    return modules.ifEmpty { null }
}

/** Figures out which option is meant by 1, "B", "b)" or the option's own text. */
private fun answerIndex(raw: Any?, options: List<String>): Int {
    val text = raw.asText() ?: return -1
    val letter = text.trim().trimEnd(')', '.', ':').lowercase()
    if (letter.length == 1 && letter[0] in 'a'..'z') {
        val i = letter[0] - 'a'
        if (i in options.indices) return i
    }
    val number = raw.asInt()
    if (number != null && raw !is String) return if (number in options.indices) number else -1
    val byText = options.indexOfFirst { it.trim().equals(text.trim(), ignoreCase = true) }
    if (byText >= 0) return byText
    val asNumber = text.trim().toIntOrNull()
    return if (asNumber != null && asNumber in options.indices) asNumber else -1
}

/**
 * Multiple choice: `{"questions":[{"prompt":"..","options":[".."],"answer":1,"explanation":"..","topic":".."}]}`.
 * Written kinds: `{"questions":[{"prompt":"..","topic":"..","criteria":"..","model_answer":".."}]}`.
 * Multiple-choice questions without a readable correct answer are dropped, so nothing is marked wrongly.
 */
fun parseAssessment(text: String, kind: AssessmentKind, newId: () -> String): List<AssessmentQuestion>? {
    val root = MiniJson.extract(text)
    val raw = root.asMap()?.get("questions").asList() ?: root.asList() ?: return null
    val questions = raw.mapNotNull { qr ->
        val q = qr.asMap() ?: return@mapNotNull null
        val prompt = q["prompt"].asText() ?: q["question"].asText() ?: return@mapNotNull null
        val topic = q["topic"].asText() ?: ""
        if (kind.objective) {
            val options = q["options"].asTextList().ifEmpty { q["choices"].asTextList() }
            if (options.size < 2) return@mapNotNull null
            val index = answerIndex(q["answer"] ?: q["correct"] ?: q["correct_index"], options)
            if (index < 0) return@mapNotNull null
            AssessmentQuestion(newId(), prompt, topic, options, index, q["explanation"].asText() ?: "")
        } else {
            AssessmentQuestion(
                id = newId(), prompt = prompt, topic = topic,
                explanation = q["model_answer"].asText() ?: q["answer"].asText() ?: q["explanation"].asText() ?: "",
                criteria = q["criteria"].asText() ?: q["marking_criteria"].asText() ?: "",
            )
        }
    }
    return questions.ifEmpty { null }
}

/** Feedback on written answers. [feedback] is keyed by question id. */
data class ParsedFeedback(val overall: String, val weakTopics: List<String>, val feedback: Map<String, String>)

/**
 * `{"overall":"..","weak_topics":[".."],"feedback":[{"id":"q1","feedback":".."}]}`. When an entry has no
 * usable id, entries are matched to [questionIds] by position.
 */
fun parseFeedback(text: String, questionIds: List<String>): ParsedFeedback? {
    val root = MiniJson.extract(text).asMap() ?: return null
    val entries = root["feedback"].asList() ?: root["answers"].asList() ?: emptyList()
    val explicit = entries.mapNotNull { it.asMap()?.get("id").asText()?.takeIf { id -> id in questionIds } }.toSet()
    val free = questionIds.filter { it !in explicit }.toMutableList()
    val byId = mutableMapOf<String, String>()
    for (raw in entries) {
        val e = raw.asMap() ?: continue
        val body = e["feedback"].asText() ?: e["comment"].asText() ?: continue
        val id = e["id"].asText()?.takeIf { it in questionIds } ?: free.removeFirstOrNull() ?: continue
        byId[id] = body
    }
    val overall = root["overall"].asText() ?: root["summary"].asText() ?: ""
    if (byId.isEmpty() && overall.isEmpty()) return null
    return ParsedFeedback(overall, root["weak_topics"].asTextList().ifEmpty { root["revise"].asTextList() }.take(6), byId)
}
