package com.fredrickemmanuel.studyzen.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Android forgets every alarm when the phone restarts, and the app update replaces them too.
 * This puts them back from the list saved on the phone, without opening the app.
 */
class ReminderBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED",
            -> ReminderScheduler.armNext(context)
        }
    }
}
