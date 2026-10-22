package com.fredrickemmanuel.studyzen.feature.reminders

import androidx.compose.runtime.staticCompositionLocalOf
import com.fredrickemmanuel.studyzen.domain.ReminderAlarm

/** What the phone currently allows. Read it in composables so the screen updates when it changes. */
data class ReminderStatus(
    /** False on platforms without background alarms (desktop, web, and iOS for now). */
    val supported: Boolean,
    val notificationsAllowed: Boolean,
    /** False means alarms may arrive a few minutes late (Android "Alarms & reminders" is off). */
    val exactAlarmsAllowed: Boolean,
) {
    val ready: Boolean get() = supported && notificationsAllowed
}

/**
 * The part of reminders that needs the operating system. The shared code decides WHICH alarms
 * exist; each platform stores them on the device and rings them, with or without internet.
 */
interface ReminderPlatform {
    val status: ReminderStatus

    /** Replaces every saved alarm with [alarms]. */
    fun sync(alarms: List<ReminderAlarm>)

    /** Asks the learner for notification permission; [onResult] says whether it was granted. */
    fun requestNotificationPermission(onResult: (Boolean) -> Unit)

    fun openExactAlarmSettings()
    fun openNotificationSettings()

    /** Rings a test reminder in about 10 seconds, to check the phone allows it. */
    fun sendTestReminder() {}
}

object NoReminderPlatform : ReminderPlatform {
    override val status = ReminderStatus(supported = false, notificationsAllowed = false, exactAlarmsAllowed = false)
    override fun sync(alarms: List<ReminderAlarm>) = Unit
    override fun requestNotificationPermission(onResult: (Boolean) -> Unit) = onResult(false)
    override fun openExactAlarmSettings() = Unit
    override fun openNotificationSettings() = Unit
}

val LocalReminderPlatform = staticCompositionLocalOf<ReminderPlatform> { NoReminderPlatform }
