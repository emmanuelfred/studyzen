package com.fredrickemmanuel.studyzen

import com.fredrickemmanuel.studyzen.data.ai.MiniJson
import com.fredrickemmanuel.studyzen.data.ai.WikiArticle
import com.fredrickemmanuel.studyzen.data.ai.parseAssessment
import com.fredrickemmanuel.studyzen.data.ai.parseFeedback
import com.fredrickemmanuel.studyzen.data.ai.parseStructure
import com.fredrickemmanuel.studyzen.data.ai.parseWikipediaSearch
import com.fredrickemmanuel.studyzen.data.ai.urlEncode
import com.fredrickemmanuel.studyzen.domain.AssessmentKind
import com.fredrickemmanuel.studyzen.domain.KnowledgeLevel
import com.fredrickemmanuel.studyzen.domain.LessonKind
import com.fredrickemmanuel.studyzen.domain.LessonSources
import com.fredrickemmanuel.studyzen.domain.LearningModule
import com.fredrickemmanuel.studyzen.domain.Lesson
import com.fredrickemmanuel.studyzen.domain.ResourceSource
import com.fredrickemmanuel.studyzen.domain.Study
import com.fredrickemmanuel.studyzen.domain.StudyPurpose
import com.fredrickemmanuel.studyzen.domain.Subject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StudyAiParsersTest {
    private var n = 0
    private fun id() = "i${n++}"

    @Test fun jsonInsideFencesAndChatterIsFound() {
        val text = "Sure! Here you go:\n```json\n{\"a\": [1, 2.5, \"x\\n\\\"y\\\" \\u00e9\", true, null,], \"b\": {\"c\": -3e2}, }\n```\nHope it helps"
        val map = MiniJson.extract(text) as Map<*, *>
        assertEquals(listOf(1.0, 2.5, "x\n\"y\" é", true, null), map["a"])
        assertEquals(-300.0, (map["b"] as Map<*, *>)["c"])
        assertNull(MiniJson.extract("no json here"))
        assertNull(MiniJson.extract("{\"broken\": "))
    }

    private val structure = """
        ```json
        {"modules":[
          {"title":"Foundations","lessons":[
            {"title":"What is a cell?","summary":"Basics","objectives":["Name the parts","Explain function"],"kind":"learn"},
            {"title":"Cell practice","kind":"Practice"},
            {"summary":"no title, dropped"}
          ]},
          {"title":"Empty module","lessons":[]},
          {"title":"Check","lessons":[{"title":"Mock test","kind":"mock"}]}
        ]}
        ```
    """

    @Test fun structureIsReadAndTidied() {
        val modules = parseStructure(structure) { id() }!!
        assertEquals(listOf("Foundations", "Check"), modules.map { it.title }) // the empty module is dropped
        val lessons = modules.flatMap { it.lessons }
        assertEquals(listOf("What is a cell?", "Cell practice", "Mock test"), lessons.map { it.title })
        assertEquals(listOf("Name the parts", "Explain function"), lessons[0].objectives)
        assertEquals(listOf(LessonKind.Learn, LessonKind.Practice, LessonKind.Assessment), lessons.map { it.kind })
        assertEquals(lessons.size + modules.size, (lessons.map { it.id } + modules.map { it.id }).toSet().size)
        assertNull(parseStructure("{\"modules\":[]}") { id() })
        assertNull(parseStructure("sorry, I cannot") { id() })
        // a bare list works too
        assertEquals(1, parseStructure("[{\"title\":\"A\",\"lessons\":[{\"title\":\"x\"}]}]") { id() }?.size)
    }

    @Test fun multipleChoiceAnswersAreUnderstoodInSeveralShapes() {
        val text = """{"questions":[
          {"prompt":"2+2?","options":["3","4","5"],"answer":1,"explanation":"Basic","topic":"Arithmetic"},
          {"prompt":"Capital of France?","options":["Rome","Paris"],"answer":"B"},
          {"prompt":"Colour of sky?","options":["Blue","Green"],"answer":"Blue"},
          {"prompt":"Bad","options":["x","y"],"answer":"zzz"},
          {"prompt":"One option","options":["x"],"answer":0}
        ]}"""
        val qs = parseAssessment(text, AssessmentKind.MultipleChoice) { id() }!!
        assertEquals(listOf("2+2?", "Capital of France?", "Colour of sky?"), qs.map { it.prompt })
        assertEquals(listOf(1, 1, 0), qs.map { it.correctIndex })
        assertEquals("Arithmetic", qs[0].topic)
        assertTrue(qs.all { it.isObjective })
    }

    @Test fun writtenQuestionsKeepCriteriaAndModelAnswer() {
        val text = """{"questions":[{"prompt":"Explain osmosis","topic":"Cells","criteria":"Mention membrane","model_answer":"Water moves..."}]}"""
        val q = parseAssessment(text, AssessmentKind.ShortAnswer) { id() }!!.single()
        assertEquals("Mention membrane", q.criteria)
        assertEquals("Water moves...", q.explanation)
        assertTrue(!q.isObjective)
        assertNull(parseAssessment("{\"questions\":[]}", AssessmentKind.Written) { id() })
    }

    @Test fun feedbackIsMatchedByIdOrPosition() {
        val text = """{"overall":"Good effort","weak_topics":["Osmosis","Diffusion"],"feedback":[{"id":"q2","feedback":"Add detail"},{"feedback":"Nice"}]}"""
        val f = parseFeedback(text, listOf("q1", "q2"))!!
        assertEquals("Good effort", f.overall)
        assertEquals(listOf("Osmosis", "Diffusion"), f.weakTopics)
        assertEquals("Add detail", f.feedback["q2"])
        assertEquals("Nice", f.feedback["q1"]) // an entry without an id takes the next free question
        assertNull(parseFeedback("{}", listOf("q1")))
    }

    @Test fun wikipediaAnswerIsReadInRankOrder() {
        val body = """{"batchcomplete":"","query":{"pages":{
          "22":{"pageid":22,"title":"Second","index":2,"extract":"Second   text\n here","fullurl":"https://en.wikipedia.org/wiki/Second"},
          "11":{"pageid":11,"title":"First","index":1,"extract":"First text","fullurl":"https://en.wikipedia.org/wiki/First"},
          "33":{"pageid":33,"title":"No text","index":3,"fullurl":"https://en.wikipedia.org/wiki/No_text"}}}}"""
        val articles = parseWikipediaSearch(body)
        assertEquals(listOf("First", "Second"), articles.map { it.title })
        assertEquals("Second text here", articles[1].extract)
        assertEquals(emptyList<WikiArticle>(), parseWikipediaSearch("{\"batchcomplete\":\"\"}"))
        assertEquals(emptyList<WikiArticle>(), parseWikipediaSearch("not json"))
    }

    @Test fun linksAreEncoded() {
        assertEquals("a%20b%26c", urlEncode("a b&c"))
        assertEquals("caf%C3%A9", urlEncode("café"))
        assertEquals("x-y_z.~", urlEncode("x-y_z.~"))
    }

    @Test fun sourcesNeverContainInventedVideos() {
        val study = Study(
            "s", "Biology", "", "", Subject.Science, KnowledgeLevel.Beginner, createdAtEpochDay = 0,
            purpose = StudyPurpose.Exam, curriculum = "WAEC",
            modules = listOf(LearningModule("m", "M", listOf(Lesson("l", "Cell structure", "")))),
        )
        val articles = listOf(WikiArticle("Cell (biology)", "https://en.wikipedia.org/wiki/Cell_(biology)", "A cell is..."))
        val sources = LessonSources.build(study, study.allLessons.first(), articles) { id() }
        assertEquals(listOf(ResourceSource.Wikipedia, ResourceSource.Search, ResourceSource.Search, ResourceSource.Search), sources.map { it.source })
        assertTrue(sources.filter { it.source == ResourceSource.Search }.all {
            it.url.startsWith("https://www.youtube.com/results?search_query=") || it.url.startsWith("https://www.google.com/search?q=")
        })
        assertTrue(sources.last().url.contains("WAEC"))
    }
}
