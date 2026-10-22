package com.fredrickemmanuel.studyzen.core.time

import kotlin.js.Date

actual fun currentTimeMillis(): Long = Date.now().toLong()

actual fun utcOffsetMinutes(): Int = -Date().getTimezoneOffset().toInt()
