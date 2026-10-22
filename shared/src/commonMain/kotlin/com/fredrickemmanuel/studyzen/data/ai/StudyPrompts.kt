package com.fredrickemmanuel.studyzen.data.ai

/** A one-tap instruction in the scanner. [instruction] is what is sent to the AI. */
data class ScanPreset(val id: String, val label: String, val instruction: String)

object StudyPrompts {
    val presets: List<ScanPreset> = listOf(
        ScanPreset("solve", "Solve it", "Solve this step by step. Show every step of the working, then give the final answer."),
        ScanPreset("explain", "Explain it", "Explain this clearly, as if teaching it to me for the first time. Use simple words and one example."),
        ScanPreset("hint", "Give me a hint", "Do not give the full solution. Give me a hint for the next step only, so I can work it out myself."),
        ScanPreset("check", "Check my work", "Check my working. Say whether it is correct, point out any mistakes, and show how to fix them."),
        ScanPreset("summary", "Summarise", "Summarise the key points in a short, well-organised list."),
        ScanPreset("terms", "Define the terms", "List the important terms here and explain each one in simple words."),
        ScanPreset("practice", "Practice questions", "Write 5 practice questions like this, from easier to harder, with the answers at the end."),
    )

    /** Rules for every answer. [study] is the learner's current Study, when they scanned from inside one. */
    fun system(study: String? = null): String = buildString {
        append("You are the study helper inside StudyZen, an app for students. ")
        append("Give accurate, well-organised answers a student can learn from. ")
        append("For a maths, science or other problem: say briefly what is being asked, solve it in numbered steps ")
        append("showing the formula and the working, and end with a line that starts with **Answer:**. ")
        append("For explanations: use plain words, short paragraphs and an example. ")
        append("Write in Markdown (## headings, numbered lists, bullet lists, **bold** for key results). ")
        append("Do not use LaTeX or dollar signs: write maths in plain text with symbols such as × ÷ √ ² ³ π ≤ ≥ and fractions like a/b. ")
        append("If the photo is unclear or the question is ambiguous, say what you could read and what you assumed. ")
        append("If you are not sure about something, say so instead of guessing, and never invent facts or sources.")
        if (!study.isNullOrBlank()) append(" The student is currently studying: $study.")
    }

    /** Sent by the "Test" button in AI settings. */
    const val TEST_SYSTEM = "You are a connection test. Reply with the single word: OK"
    const val TEST_MESSAGE = "Hello"
}
