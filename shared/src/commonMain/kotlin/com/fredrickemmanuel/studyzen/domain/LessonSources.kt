package com.fredrickemmanuel.studyzen.domain

import com.fredrickemmanuel.studyzen.data.ai.WikiArticle
import com.fredrickemmanuel.studyzen.data.ai.urlEncode

/**
 * The sources shown under a lesson. Wikipedia articles are real pages that were fetched. YouTube and
 * web results are only search links the learner opens themselves: the app never invents a video or
 * page address.
 */
object LessonSources {

    fun youtubeSearch(query: String) = "https://www.youtube.com/results?search_query=${urlEncode(query)}"
    fun webSearch(query: String) = "https://www.google.com/search?q=${urlEncode(query)}"

    fun build(study: Study, lesson: Lesson, articles: List<WikiArticle>, newId: () -> String): List<LessonResource> {
        val out = mutableListOf<LessonResource>()
        articles.take(3).forEach { a ->
            out += LessonResource(newId(), a.title, a.url, ResourceSource.Wikipedia, a.extract.take(280).trimEnd())
        }
        val topic = lesson.title
        out += LessonResource(
            newId(), "Videos about “$topic” on YouTube", youtubeSearch("$topic ${study.title} explained"),
            ResourceSource.Search, "Opens a YouTube search. Pick a video you like.",
        )
        out += LessonResource(
            newId(), "Search the web for “$topic”", webSearch("$topic ${study.title}"),
            ResourceSource.Search, "Opens a web search for guides and worked examples.",
        )
        val extra = when (study.purpose) {
            StudyPurpose.Exam, StudyPurpose.Curriculum ->
                if (study.curriculum.isNotBlank()) "Past questions: ${study.curriculum} $topic" else null
            StudyPurpose.Interview -> "Interview questions about $topic ${study.curriculum}".trim()
            else -> null
        }
        if (extra != null) {
            val title = if (study.purpose == StudyPurpose.Interview) "Interview questions on this topic" else "Past questions for this topic"
            out += LessonResource(newId(), title, webSearch(extra), ResourceSource.Search, "Opens a web search.")
        }
        return out
    }
}
