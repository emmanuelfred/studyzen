package com.fredrickemmanuel.studyzen.core.time

/** Milliseconds since 1970-01-01 UTC. */
expect fun currentTimeMillis(): Long

/** The device's current offset from UTC in minutes (e.g. +60 for Lagos). */
expect fun utcOffsetMinutes(): Int
