package com.fredrickemmanuel.studyzen.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.fredrickemmanuel.studyzen.MainActivity
import com.fredrickemmanuel.studyzen.domain.ReminderAlarm
import java.util.TimeZone

/** Hands the soonest alarms to Android's AlarmManager, which rings them with no internet needed. */
internal object ReminderScheduler {
    const val TEST_ID = "test"
    const val ACTION_REMIND = "com.fredrickemmanuel.studyzen.REMIND"
    const val ACTION_STOP = "com.fredrickemmanuel.studyzen.STOP_REMINDER"

    /** Android limits how many alarms an app may hold, so only the next few are armed at a time. */
    private const val ARM_LIMIT = 40

    /** Replaces everything saved with [alarms] (what the app wants) and arms the soonest ones. */
    fun replaceAll(context: Context, alarms: List<ReminderAlarm>) {
        ReminderStore.save(context, alarms)
        armNext(context)
    }

    /**
     * Cancels the armed alarms and arms the next ones from the saved list. Safe to call any time:
     * after the app opens, a reminder rings, the phone restarts, or the clock or time zone changes.
     */
    fun armNext(context: Context) {
        val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        ReminderStore.armedIds(context).forEach { manager.cancel(pendingIntent(context, it)) }

        val now = System.currentTimeMillis()
        val upcoming = ReminderStore.load(context)
            .map { it to triggerMillis(it) }
            .filter { it.second > now }
            .sortedBy { it.second }

        // forget alarms that have passed, so the list never grows without limit
        ReminderStore.save(context, upcoming.map { it.first })

        val armed = mutableListOf<String>()
        for ((alarm, at) in upcoming.take(ARM_LIMIT)) {
            arm(context, manager, alarm.id, at)
            armed += alarm.id
        }
        ReminderStore.saveArmedIds(context, armed)
    }

    /** One alarm 10 seconds from now, to prove the phone lets StudyZen ring. */
    fun scheduleTest(context: Context) {
        val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        arm(context, manager, TEST_ID, System.currentTimeMillis() + 10_000L)
    }

    /**
     * Uses an "alarm clock" alarm: Android fires it on the dot even when the phone is idle or in battery saver,
     * and it does not need the "Alarms & reminders" permission. (Plain exact alarms are often delayed or
     * refused on newer Android versions, which is why reminders used to arrive late or not ring.)
     */
    private fun arm(context: Context, manager: AlarmManager, id: String, at: Long) {
        val show = PendingIntent.getActivity(
            context, 1,
            Intent(context, MainActivity::class.java).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        try {
            manager.setAlarmClock(AlarmManager.AlarmClockInfo(at, show), pendingIntent(context, id))
        } catch (e: SecurityException) {
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pendingIntent(context, id))
        }
    }

    fun find(context: Context, id: String): ReminderAlarm? =
        if (id == TEST_ID) {
            ReminderAlarm(TEST_ID, TEST_ID, 0L, "Test reminder", "It works. Your study reminders will ring like this.", silent = false, vibrate = true)
        } else {
            ReminderStore.load(context).firstOrNull { it.id == id }
        }

    /** Alarm-clock alarms never need the special permission, so this is always fine. */
    @Suppress("UNUSED_PARAMETER")
    fun canScheduleExact(context: Context): Boolean = true

    /** Wall-clock minutes to a real moment, using the phone's current time zone. */
    fun triggerMillis(alarm: ReminderAlarm): Long {
        val localMillis = alarm.localEpochMinute * 60_000L
        val zone = TimeZone.getDefault()
        return localMillis - zone.getOffset(localMillis - zone.rawOffset)
    }

    internal fun pendingIntent(context: Context, id: String): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java)
            .setAction(ACTION_REMIND)
            .setData(Uri.parse("studyzen://reminder/${Uri.encode(id)}"))
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }
}
