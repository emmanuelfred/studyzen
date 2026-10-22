package com.fredrickemmanuel.studyzen.domain

/** Picks a sensible kind of assessment for where the learner is, so "let the AI recommend" has an answer. */
fun recommendKind(study: Study, lesson: Lesson?): AssessmentKind = when {
    study.purpose == StudyPurpose.Interview -> if (study.subject == Subject.Programming) AssessmentKind.Coding else AssessmentKind.Interview
    lesson?.kind == LessonKind.Practice -> if (study.subject == Subject.Programming) AssessmentKind.Coding else AssessmentKind.Practice
    lesson?.kind == LessonKind.Revision -> AssessmentKind.ShortAnswer
    study.purpose == StudyPurpose.Coursework -> AssessmentKind.Written
    else -> AssessmentKind.MultipleChoice
}

/** Marks the objective questions and lists the topics of the ones that were missed (or left blank). */
fun weakTopicsOf(assessment: Assessment): List<String> =
    assessment.questions
        .filter { q -> q.isObjective && assessment.answerFor(q.id)?.choice != q.correctIndex }
        .map { it.topic.trim() }
        .filter { it.isNotEmpty() }
        .distinct()
        .take(6)

/** A short title such as "Quiz: Cell structure". */
fun assessmentTitle(kind: AssessmentKind, lesson: Lesson?, study: Study): String {
    val what = lesson?.title ?: study.title
    val noun = when (kind) {
        AssessmentKind.MultipleChoice -> "Quiz"
        AssessmentKind.ShortAnswer -> "Short answers"
        AssessmentKind.Practice -> "Practice"
        AssessmentKind.Written -> "Assignment"
        AssessmentKind.Interview -> "Interview practice"
        AssessmentKind.Coding -> "Coding challenge"
    }
    return "$noun: $what"
}

/** Whether every question has some answer, so Submit can be allowed. */
fun isComplete(assessment: Assessment): Boolean = assessment.questions.all { q ->
    val a = assessment.answerFor(q.id)
    if (q.isObjective) a != null && a.choice in q.options.indices else a != null && a.text.isNotBlank()
}

/** Replaces or adds one answer. */
fun Assessment.withAnswer(answer: AssessmentAnswer): Assessment =
    copy(answers = answers.filterNot { it.questionId == answer.questionId } + answer)
