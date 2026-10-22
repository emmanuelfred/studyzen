package com.fredrickemmanuel.studyzen

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationManagerCompat
import com.fredrickemmanuel.studyzen.domain.ReminderAlarm
import com.fredrickemmanuel.studyzen.feature.reminders.ReminderPlatform
import com.fredrickemmanuel.studyzen.feature.reminders.ReminderStatus
import com.fredrickemmanuel.studyzen.reminders.ReminderScheduler

/** Android's side of reminders: saves alarms on the phone and rings them through AlarmManager. */
class AndroidReminderPlatform(private val activity: ComponentActivity) : ReminderPlatform {

    private var current by mutableStateOf(read())
    override val status: ReminderStatus get() = current

    private var waiting: ((Boolean) -> Unit)? = null

    // must be created before the activity starts, so this class is built in onCreate
    private val permissionLauncher =
        activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            refresh()
            waiting?.invoke(granted)
            waiting = null
        }

    /** Call when the app comes back to the front: the learner may have changed a setting. */
    fun refresh() {
        current = read()
        ReminderScheduler.armNext(activity)
    }

    override fun sync(alarms: List<ReminderAlarm>) {
        ReminderScheduler.replaceAll(activity, alarms)
    }

    override fun requestNotificationPermission(onResult: (Boolean) -> Unit) {
        val needsAsk = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            activity.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        if (!needsAsk) {
            refresh()
            onResult(current.notificationsAllowed)
            return
        }
        waiting = onResult
        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    override fun openExactAlarmSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            startSafely(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${activity.packageName}")))
        }
    }

    override fun sendTestReminder() {
        ReminderScheduler.scheduleTest(activity)
    }

    override fun openNotificationSettings() {
        startSafely(
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, activity.packageName),
        )
    }

    private fun startSafely(intent: Intent) {
        try {
            activity.startActivity(intent)
        } catch (e: Exception) {
            activity.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${activity.packageName}")),
            )
        }
    }

    private fun read() = ReminderStatus(
        supported = true,
        notificationsAllowed = NotificationManagerCompat.from(activity as Context).areNotificationsEnabled(),
        exactAlarmsAllowed = ReminderScheduler.canScheduleExact(activity),
    )
}
