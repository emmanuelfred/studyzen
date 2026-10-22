package com.fredrickemmanuel.studyzen.core.time

private fun jsNow(): Double = js("Date.now()")

private fun jsOffsetMinutes(): Double = js("-new Date().getTimezoneOffset()")

actual fun currentTimeMillis(): Long = jsNow().toLong()

actual fun utcOffsetMinutes(): Int = jsOffsetMinutes().toInt()
