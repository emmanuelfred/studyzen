package com.fredrickemmanuel.studyzen.core.designsystem.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.fredrickemmanuel.studyzen.core.time.CivilDate
import com.fredrickemmanuel.studyzen.core.time.formatClock

/** Date control: steps one day at a time. Works the same on every platform. */
@Composable
fun DateStepper(
    label: String,
    epochDay: Long,
    onChange: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Stepper(
        label = label,
        valueText = CivilDate.fromEpochDay(epochDay).shortLabel(),
        onMinus = { onChange(epochDay - 1) },
        onPlus = { onChange(epochDay + 1) },
        modifier = modifier,
    )
}

/** Time-of-day control: steps by [stepMinutes] and stays within 00:00..23:59. */
@Composable
fun TimeStepper(
    label: String,
    minuteOfDay: Int,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    stepMinutes: Int = 15,
) {
    Stepper(
        label = label,
        valueText = formatClock(minuteOfDay),
        onMinus = { onChange((minuteOfDay - stepMinutes).coerceAtLeast(0)) },
        onPlus = { onChange((minuteOfDay + stepMinutes).coerceAtMost(24 * 60 - stepMinutes)) },
        modifier = modifier,
    )
}
