package com.fredrickemmanuel.studyzen.data.dictionary

import com.fredrickemmanuel.studyzen.domain.FavoriteWord

data class WordDefinition(val text: String, val example: String? = null)

data class WordMeaning(
    val partOfSpeech: String,
    val definitions: List<WordDefinition>,
    val synonyms: List<String> = emptyList(),
)

/** One dictionary entry, merged from everything the dictionary returned for the word. */
data class DictionaryEntry(
    val word: String,
    val phonetic: String?,
    val meanings: List<WordMeaning>,
    /** A recording of the word being said, when the dictionary has one. */
    val audioUrl: String? = null,
) {
    /** What is kept when the learner adds the word to their favourites: the main meaning. */
    fun toFavorite(savedAtEpochDay: Long): FavoriteWord? {
        val meaning = meanings.firstOrNull { it.definitions.isNotEmpty() } ?: return null
        val definition = meaning.definitions.first()
        return FavoriteWord(
            id = word.trim().lowercase(),
            word = word,
            phonetic = phonetic,
            partOfSpeech = meaning.partOfSpeech,
            definition = definition.text,
            example = definition.example,
            savedAtEpochDay = savedAtEpochDay,
        )
    }

    /** Plain text for saving into a note. */
    fun toNoteText(): String = buildString {
        phonetic?.let { appendLine(it) }
        meanings.forEach { m ->
            appendLine()
            appendLine(m.partOfSpeech.ifBlank { "meaning" })
            m.definitions.take(3).forEachIndexed { i, d ->
                appendLine("${i + 1}. ${d.text}")
                d.example?.let { appendLine("   \"$it\"") }
            }
        }
    }.trim()
}

sealed interface LookupResult {
    data class Found(val entry: DictionaryEntry) : LookupResult
    data object NotFound : LookupResult
    /** The dictionary could not be reached (offline) or answered with an error. */
    data class Failed(val message: String, val offline: Boolean) : LookupResult
}
