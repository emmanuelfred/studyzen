package com.fredrickemmanuel.studyzen.core.time

import java.util.TimeZone

actual fun currentTimeMillis(): Long = System.currentTimeMillis()

actual fun utcOffsetMinutes(): Int =
    TimeZone.getDefault().getOffset(System.currentTimeMillis()) / 60_000
