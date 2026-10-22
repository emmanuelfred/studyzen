package com.fredrickemmanuel.studyzen.core.time

import platform.CoreFoundation.CFAbsoluteTimeGetCurrent
import platform.Foundation.NSTimeZone
import platform.Foundation.localTimeZone

// CFAbsoluteTime counts seconds from 2001-01-01; 978307200 s separates it from the 1970 epoch.
actual fun currentTimeMillis(): Long = ((CFAbsoluteTimeGetCurrent() + 978307200.0) * 1000.0).toLong()

actual fun utcOffsetMinutes(): Int = (NSTimeZone.localTimeZone.secondsFromGMT / 60).toInt()
