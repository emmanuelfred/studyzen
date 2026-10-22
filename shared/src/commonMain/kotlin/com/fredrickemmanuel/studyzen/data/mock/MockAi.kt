package com.fredrickemmanuel.studyzen.data.mock

import com.fredrickemmanuel.studyzen.core.Ids
import com.fredrickemmanuel.studyzen.domain.Finding
import com.fredrickemmanuel.studyzen.domain.KnowledgeLevel
import com.fredrickemmanuel.studyzen.domain.LearningModule
import com.fredrickemmanuel.studyzen.domain.Lesson
import com.fredrickemmanuel.studyzen.domain.QuizDifficulty
import com.fredrickemmanuel.studyzen.domain.QuizQuestion
import com.fredrickemmanuel.studyzen.domain.Study
import com.fredrickemmanuel.studyzen.domain.Subject

/** What the learner can ask the assistant to do. */
enum class AssistantAction(val label: String) {
    Explain("Explain simply"),
    Example("Give an example"),
    Analogy("Use an analogy"),
    Quiz("Quiz me"),
    Outline("Lesson outline"),
    Summary("Summarize"),
    Related("Related concepts"),
}

/**
 * Stand-in for the AI backend. Everything it returns is sample text built from templates, so
 * screens can be designed and tested now. The UI labels this content clearly ("Sample AI
 * response") and it must be replaced by real API calls later.
 */
object MockAi {

    fun reply(action: AssistantAction?, studyTitle: String, question: String?): String {
        val topic = studyTitle.ifBlank { "this topic" }
        return when (action) {
            AssistantAction.Explain ->
                "Here is a simple way to think about $topic.\n\n" +
                    "• Start with the big idea: what problem does it solve?\n" +
                    "• Break it into two or three small parts.\n" +
                    "• Learn one part at a time and test yourself after each one.\n\n" +
                    "Tell me which part feels unclear and I will go deeper."
            AssistantAction.Example ->
                "Example for $topic:\n\n" +
                    "1. Pick a small, concrete case you already know.\n" +
                    "2. Walk through it step by step, writing each step down.\n" +
                    "3. Change one detail and predict what happens.\n\n" +
                    "Want a worked example on a specific lesson?"
            AssistantAction.Analogy ->
                "Analogy for $topic: think of it like organising a kitchen. " +
                    "Each part has a place, and knowing where things live makes everything faster. " +
                    "If you tell me what you are comparing it to, I can make the analogy sharper."
            AssistantAction.Quiz ->
                "Quick check on $topic:\n\n" +
                    "1. In your own words, what is the main idea?\n" +
                    "2. Name one situation where you would use it.\n" +
                    "3. What is the most common mistake beginners make?\n\n" +
                    "Reply with your answers and I will give feedback. You can also open Practice for a full quiz."
            AssistantAction.Outline ->
                "Lesson outline for $topic:\n\n" +
                    "1. Introduction and goals\n2. Key terms\n3. Core ideas with examples\n" +
                    "4. Practice problems\n5. Review and next steps"
            AssistantAction.Summary ->
                "Summary of $topic:\n\n" +
                    "• Focus on the core idea first.\n• Link new terms to examples.\n" +
                    "• Review within 24 hours to remember more.\n\nSave this to your notes if it helps."
            AssistantAction.Related ->
                "Concepts related to $topic: prerequisites you may want to refresh, " +
                    "neighbouring ideas that use the same tools, and the next topic to study after this one."
            null ->
                if (question.isNullOrBlank()) {
                    "Ask me anything about $topic."
                } else {
                    "Good question about $topic: \"${question.trim()}\".\n\n" +
                        "Here is how I would approach it: define the key terms, look at one small example, " +
                        "then try a similar problem yourself. Tell me where you get stuck and I will adapt."
                }
        }
    }

    // --- Learning paths ---------------------------------------------------------------------

    fun learningPath(title: String, subject: Subject, @Suppress("UNUSED_PARAMETER") level: KnowledgeLevel): List<LearningModule> {
        val names = when {
            title.contains("python", ignoreCase = true) -> listOf(
                "Introduction and setup", "Variables and data types", "Conditions and loops",
                "Functions", "Collections", "Files and exceptions", "Practical projects",
            )
            subject == Subject.Programming -> listOf(
                "Introduction and setup", "Core concepts", "Building blocks", "Putting it together", "Practical project",
            )
            subject == Subject.Mathematics -> listOf(
                "Foundations", "Key definitions and rules", "Worked examples", "Problem-solving techniques", "Review and practice",
            )
            else -> listOf("Introduction", "Core concepts", "Applying what you learned", "Review")
        }
        return names.map { name ->
            LearningModule(
                id = Ids.next("mod"),
                title = name,
                lessons = listOf(
                    Lesson(Ids.next("les"), "$name: key ideas", "Understand the main ideas and vocabulary."),
                    Lesson(Ids.next("les"), "$name: worked examples", "See the ideas applied step by step."),
                    Lesson(Ids.next("les"), "$name: practice", "Try exercises and check your answers."),
                ),
            )
        }
    }

    fun lessonBody(studyTitle: String, lessonTitle: String): String =
        "$lessonTitle\n\n" +
            "This is sample lesson text for \"$studyTitle\". When the backend is connected, the AI will write " +
            "the full lesson here, with explanations, examples and exercises.\n\n" +
            "For now, use this space to:\n" +
            "• Write what you already know about the topic.\n" +
            "• Ask the assistant a question about it.\n" +
            "• Save a note, then mark the lesson complete."

    // --- Research ---------------------------------------------------------------------------

    /** No sources are invented: findings are labelled as AI interpretation until a real source exists. */
    fun research(question: String): List<Finding> = listOf(
        Finding("A short, plain-language answer to \"${question.trim()}\" would appear here."),
        Finding("Key points and trade-offs would be listed here, separate from any sourced facts."),
    )

    // --- Quizzes ----------------------------------------------------------------------------

    fun quiz(study: Study, count: Int, difficulty: QuizDifficulty): List<QuizQuestion> {
        val bank = when (study.subject) {
            Subject.Programming -> programmingBank
            Subject.Mathematics -> mathBank
            Subject.Science -> scienceBank
            else -> lessonBank(study)
        }
        if (bank.isEmpty()) return emptyList()
        // Harder quizzes start from the later questions in the bank.
        val offset = when (difficulty) { QuizDifficulty.Easy -> 0; QuizDifficulty.Medium -> 2; QuizDifficulty.Hard -> 4 }
        val rotated = bank.drop(offset % bank.size) + bank.take(offset % bank.size)
        return rotated.take(count.coerceAtMost(bank.size))
    }

    private fun q(topic: String, prompt: String, options: List<String>, correct: Int, why: String) =
        QuizQuestion(Ids.next("q"), topic, prompt, options, correct, why)

    private val programmingBank: List<QuizQuestion>
        get() = listOf(
            q("Data structures", "Which data structure follows Last In, First Out (LIFO)?",
                listOf("Queue", "Stack", "Array", "Hash table"), 1,
                "A stack removes the most recently added item first, like a pile of plates."),
            q("Data structures", "Which structure follows First In, First Out (FIFO)?",
                listOf("Stack", "Queue", "Tree", "Graph"), 1,
                "A queue serves items in the order they arrived, like people waiting in line."),
            q("Algorithms", "Binary search requires the data to be...",
                listOf("Sorted", "Unique", "Numeric", "Stored in a linked list"), 0,
                "Binary search halves the range each step, which only works when the data is in order."),
            q("Algorithms", "What is the worst-case time complexity of linear search on n items?",
                listOf("O(1)", "O(log n)", "O(n)", "O(n²)"), 2,
                "In the worst case every item is checked once, so the work grows with n."),
            q("Python", "What does len([4, 8, 15]) return in Python?",
                listOf("2", "3", "4", "15"), 1,
                "len counts the items in the list. There are three: 4, 8 and 15."),
            q("Python", "Which keyword defines a function in Python?",
                listOf("func", "def", "function", "define"), 1,
                "Python uses def, followed by the function name and parentheses."),
            q("Data structures", "In a singly linked list, each node stores a value and...",
                listOf("A reference to the next node", "Its index", "The list length", "A hash"), 0,
                "Each node points to the next one, which is how the list is chained together."),
            q("Data structures", "On average, looking up a key in a hash table takes...",
                listOf("O(1)", "O(n)", "O(log n)", "O(n²)"), 0,
                "A hash function jumps straight to the right bucket, so lookup is constant time on average."),
        )

    private val mathBank: List<QuizQuestion>
        get() = listOf(
            q("Sets", "How many elements does the set {2, 4, 6, 8} have?",
                listOf("3", "4", "5", "8"), 1, "Count the distinct members: 2, 4, 6 and 8 make four."),
            q("Counting", "What is 5! (5 factorial)?",
                listOf("25", "60", "120", "720"), 2, "5! = 5 × 4 × 3 × 2 × 1 = 120."),
            q("Logic", "If p is true and q is false, what is (p AND q)?",
                listOf("True", "False", "Unknown", "Both"), 1, "AND is true only when both parts are true."),
            q("Algebra", "Solve for x: 2x + 6 = 14",
                listOf("x = 3", "x = 4", "x = 5", "x = 10"), 1, "Subtract 6 to get 2x = 8, then divide by 2: x = 4."),
            q("Counting", "In how many ways can 3 different books be arranged on a shelf?",
                listOf("3", "6", "9", "27"), 1, "3! = 3 × 2 × 1 = 6 arrangements."),
            q("Number theory", "Which of these numbers is prime?",
                listOf("21", "27", "29", "33"), 2, "29 has no divisors except 1 and itself. The others are divisible by 3."),
        )

    private val scienceBank: List<QuizQuestion>
        get() = listOf(
            q("Chemistry", "What is the chemical formula for water?",
                listOf("H2O", "CO2", "O2", "NaCl"), 0, "Water has two hydrogen atoms and one oxygen atom: H2O."),
            q("Physics", "Which force pulls objects toward the Earth?",
                listOf("Friction", "Gravity", "Magnetism", "Tension"), 1, "Gravity attracts objects with mass toward each other."),
            q("Biology", "Plants make food using which process?",
                listOf("Respiration", "Photosynthesis", "Digestion", "Fermentation"), 1,
                "Photosynthesis turns light, water and carbon dioxide into sugar and oxygen."),
        )

    /** For subjects without a ready bank: questions built from the Study's own lessons. */
    private fun lessonBank(study: Study): List<QuizQuestion> {
        val lessons = study.allLessons
        if (lessons.size < 2) return emptyList()
        return lessons.take(6).mapIndexed { i, lesson ->
            val others = lessons.filter { it.id != lesson.id }.take(3).map { it.title }
            val options = (others + lesson.title).take(4)
            q(study.title, "Which of these is a lesson in \"${study.title}\"?", options, options.indexOf(lesson.title).coerceAtLeast(0),
                "\"${lesson.title}\" is lesson ${i + 1} of your learning path.")
        }
    }
}
