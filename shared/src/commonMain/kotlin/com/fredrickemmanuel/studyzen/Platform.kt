package com.fredrickemmanuel.studyzen

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform