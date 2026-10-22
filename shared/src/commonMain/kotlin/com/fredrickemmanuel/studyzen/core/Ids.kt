package com.fredrickemmanuel.studyzen.core

import com.fredrickemmanuel.studyzen.core.time.currentTimeMillis

/** Creates unique, stable ids for locally created items (the server will issue its own later). */
object Ids {
    private var counter = 0L

    fun next(prefix: String): String {
        counter++
        return "$prefix-${currentTimeMillis().toString(36)}-$counter"
    }
}
