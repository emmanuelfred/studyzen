package com.fredrickemmanuel.studyzen.data.dictionary

import com.fredrickemmanuel.studyzen.domain.FavoriteWord

/** A word that ships with the app, so Word of the Day works without internet. Written for StudyZen. */
data class BankWord(
    val word: String,
    val partOfSpeech: String,
    val definition: String,
    val example: String,
) {
    fun toFavorite(savedAtEpochDay: Long) = FavoriteWord(
        id = word.lowercase(),
        word = word,
        partOfSpeech = partOfSpeech,
        definition = definition,
        example = example,
        savedAtEpochDay = savedAtEpochDay,
    )
}

object WordBank {
    /** The word for a given day: the same all day, a new one tomorrow, cycling through the whole list. */
    fun wordOfTheDay(epochDay: Long): BankWord = words[epochDay.mod(words.size.toLong()).toInt()]

    val words: List<BankWord> = listOf(
        BankWord("ephemeral", "adjective", "Lasting for a very short time.", "The ephemeral beauty of the cherry blossoms drew crowds every spring."),
        BankWord("ubiquitous", "adjective", "Present or found everywhere.", "Smartphones have become ubiquitous in classrooms."),
        BankWord("meticulous", "adjective", "Showing great attention to detail; very careful and precise.", "She kept meticulous notes of every experiment."),
        BankWord("resilient", "adjective", "Able to recover quickly from difficulty.", "A resilient student treats a bad grade as feedback, not failure."),
        BankWord("eloquent", "adjective", "Fluent and persuasive in speaking or writing.", "His eloquent speech convinced the whole class."),
        BankWord("pragmatic", "adjective", "Dealing with things sensibly and realistically, based on what works in practice.", "We took a pragmatic approach and fixed the biggest problem first."),
        BankWord("ambiguous", "adjective", "Open to more than one meaning; not clear.", "The question was ambiguous, so I asked the teacher what it meant."),
        BankWord("diligent", "adjective", "Showing steady, careful effort in your work.", "A diligent learner reviews a little every day."),
        BankWord("hypothesis", "noun", "A proposed explanation that is used as a starting point for investigation and can be tested.", "Our hypothesis was that plants grow faster with more light."),
        BankWord("empirical", "adjective", "Based on observation or experiment rather than on theory alone.", "The study gave empirical evidence that sleep improves memory."),
        BankWord("paradigm", "noun", "A typical example or pattern of something; a model that shapes how a subject is understood.", "The discovery created a new paradigm in physics."),
        BankWord("scrutinize", "verb", "To examine something very closely and carefully.", "Scrutinize each step of your solution before submitting it."),
        BankWord("coherent", "adjective", "Logical and consistent; easy to follow and understand.", "Write a coherent essay where each paragraph leads to the next."),
        BankWord("analogous", "adjective", "Similar in a way that makes comparison possible.", "The heart is analogous to a pump."),
        BankWord("benevolent", "adjective", "Kind, generous and well-meaning.", "The benevolent teacher stayed late to help everyone."),
        BankWord("candid", "adjective", "Honest and straightforward in what you say.", "I appreciate your candid feedback on my draft."),
        BankWord("concise", "adjective", "Giving the needed information clearly in few words.", "Keep your summary concise: three sentences at most."),
        BankWord("deduce", "verb", "To reach a conclusion by reasoning from known facts.", "From the clues, we can deduce who solved the problem first."),
        BankWord("eminent", "adjective", "Famous and respected in a particular field.", "An eminent scientist visited the school."),
        BankWord("fallacy", "noun", "A mistaken belief, especially one based on faulty reasoning.", "It is a fallacy that you must study all night to do well."),
        BankWord("gregarious", "adjective", "Fond of company; sociable.", "A gregarious person often enjoys group study."),
        BankWord("inevitable", "adjective", "Certain to happen; unavoidable.", "Mistakes are inevitable when you are learning something new."),
        BankWord("juxtapose", "verb", "To place two things side by side to compare or contrast them.", "The teacher juxtaposed two poems to show their different moods."),
        BankWord("lucid", "adjective", "Clearly expressed and easy to understand.", "Her lucid explanation made the topic simple."),
        BankWord("mitigate", "verb", "To make something less severe, serious or painful.", "Planning ahead helps mitigate exam stress."),
        BankWord("nuance", "noun", "A subtle difference in meaning, expression or tone.", "Translators must catch every nuance of the original."),
        BankWord("obsolete", "adjective", "No longer produced or used; out of date.", "Floppy disks are obsolete."),
        BankWord("perseverance", "noun", "Continued effort to do something despite difficulty or delay.", "Perseverance carried her through the hardest chapter."),
        BankWord("quantify", "verb", "To express or measure the amount of something.", "It is hard to quantify how much you have learned."),
        BankWord("rigorous", "adjective", "Extremely thorough, exact and careful.", "The experiment followed a rigorous method."),
        BankWord("substantiate", "verb", "To provide evidence that supports or proves a claim.", "You must substantiate your argument with examples."),
        BankWord("tenacious", "adjective", "Holding firmly to a purpose; persistent.", "A tenacious student keeps trying until the problem is solved."),
        BankWord("unprecedented", "adjective", "Never done or known before.", "The school saw an unprecedented number of applications."),
        BankWord("validate", "verb", "To check or prove that something is true, accurate or acceptable.", "Run the tests to validate your answer."),
        BankWord("verbose", "adjective", "Using more words than needed.", "The report was verbose; half of it could be cut."),
        BankWord("viable", "adjective", "Capable of working successfully; workable.", "Is this a viable plan for finishing before the deadline?"),
        BankWord("wary", "adjective", "Cautious about possible dangers or problems.", "Be wary of sources that give no evidence."),
        BankWord("zealous", "adjective", "Showing great energy and enthusiasm for a cause or goal.", "She was a zealous supporter of the science club."),
        BankWord("abstract", "adjective", "Existing as an idea or concept rather than as a physical thing.", "Justice is an abstract idea."),
        BankWord("catalyst", "noun", "Something that causes or speeds up change; in chemistry, a substance that speeds up a reaction without being used up.", "The trip was a catalyst for her interest in geology."),
        BankWord("synthesis", "noun", "The combining of separate parts or ideas to form a connected whole.", "The essay is a synthesis of three different sources."),
        BankWord("correlation", "noun", "A relationship in which two things tend to change together. It does not by itself prove that one causes the other.", "There is a correlation between hours studied and test scores."),
        BankWord("variable", "noun", "Something that can change; in maths and programming, a name or symbol that stands for a value that can vary.", "In the equation y = 2x, x is the variable."),
        BankWord("algorithm", "noun", "A step-by-step set of instructions for solving a problem or completing a task.", "Follow the algorithm one step at a time."),
        BankWord("inference", "noun", "A conclusion reached from evidence and reasoning.", "What inference can you draw from this graph?"),
        BankWord("criterion", "noun", "A standard or principle used to judge something. The plural is criteria.", "Clear writing is one criterion on the marking scheme."),
        BankWord("bias", "noun", "An unfair tendency to favour or oppose something, often without noticing; in statistics, a systematic error.", "Check the article for bias before you trust it."),
        BankWord("context", "noun", "The circumstances or surrounding words that help explain the meaning of something.", "You can often guess a new word from its context."),
        BankWord("concede", "verb", "To admit that something is true or valid after first resisting it.", "He had to concede that his first answer was wrong."),
        BankWord("elaborate", "verb", "To explain something in more detail.", "Could you elaborate on your second point?"),
        BankWord("facilitate", "verb", "To make an action or process easier.", "Good notes facilitate revision."),
        BankWord("integrity", "noun", "The quality of being honest and having strong moral principles.", "Academic integrity means doing your own work."),
        BankWord("inherent", "adjective", "Existing as a natural and permanent part of something.", "There is an inherent risk in guessing."),
        BankWord("metaphor", "noun", "A figure of speech that describes something by saying it is something else, without using \"like\" or \"as\".", "\"Time is a thief\" is a metaphor."),
        BankWord("plausible", "adjective", "Seeming reasonable or likely to be true.", "That is a plausible explanation for the result."),
        BankWord("refute", "verb", "To prove that a statement or theory is wrong.", "The new data refute the old theory."),
        BankWord("speculate", "verb", "To form a theory or guess without firm evidence.", "Scientists speculate about life on other planets."),
        BankWord("tangible", "adjective", "Able to be touched; real and clear enough to be seen or measured.", "Practice brings tangible improvement."),
        BankWord("trivial", "adjective", "Of little value or importance.", "Don't lose marks over trivial mistakes."),
        BankWord("coalesce", "verb", "To come together and form one whole.", "The scattered ideas began to coalesce into a clear plan."),
    )
}
