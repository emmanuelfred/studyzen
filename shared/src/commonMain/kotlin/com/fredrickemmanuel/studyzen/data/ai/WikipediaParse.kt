package com.fredrickemmanuel.studyzen.data.ai

/** An article Wikipedia returned for a search. [extract] is the plain-text introduction. */
data class WikiArticle(val title: String, val url: String, val extract: String)

/** Builds a safe link for [text] inside a URL (UTF-8 percent-encoding). */
fun urlEncode(text: String): String = buildString {
    for (b in text.encodeToByteArray()) {
        val c = b.toInt() and 0xFF
        val ch = c.toChar()
        if (ch.isLetterOrDigit() && c < 128 || ch in "-_.~") append(ch)
        else if (ch == ' ') append("%20")
        else {
            append('%')
            append("0123456789ABCDEF"[c shr 4])
            append("0123456789ABCDEF"[c and 15])
        }
    }
}

/**
 * Reads Wikipedia's `action=query&generator=search&prop=extracts|info` answer. Articles come back in
 * search-rank order; ones without text or a link are dropped.
 */
fun parseWikipediaSearch(body: String): List<WikiArticle> {
    val pages = MiniJson.extract(body).asMap()?.get("query").asMap()?.get("pages").asMap() ?: return emptyList()
    return pages.values.mapNotNull { page ->
        val p = page.asMap() ?: return@mapNotNull null
        val title = p["title"].asText() ?: return@mapNotNull null
        val url = p["fullurl"].asText() ?: return@mapNotNull null
        val extract = p["extract"].asText() ?: return@mapNotNull null
        (p["index"].asInt() ?: Int.MAX_VALUE) to WikiArticle(title, url, extract.replace(Regex("\\s+"), " "))
    }.sortedBy { it.first }.map { it.second }
}
