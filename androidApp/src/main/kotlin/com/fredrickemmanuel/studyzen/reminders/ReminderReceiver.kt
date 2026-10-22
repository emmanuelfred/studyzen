package com.fredrickemmanuel.studyzen.reminders

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.fredrickemmanuel.studyzen.MainActivity
import com.fredrickemmanuel.studyzen.domain.ReminderAlarm

/** Runs when an alarm time arrives: shows the reminder, then arms the next alarms. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.data?.lastPathSegment
        if (intent.action == ReminderScheduler.ACTION_STOP) {
            // "Stop" on the notification: silence the ringing and remove it
            if (id != null) NotificationManagerCompat.from(context).cancel(id.hashCode())
            return
        }
        if (intent.action != ReminderScheduler.ACTION_REMIND) return
        val alarm = id?.let { ReminderScheduler.find(context, it) }
        if (alarm != null) show(context, alarm)
        ReminderScheduler.armNext(context)
    }

    private fun show(context: Context, alarm: ReminderAlarm) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        val channel = ensureChannel(context, alarm.silent, alarm.vibrate)
        val open = PendingIntent.getActivity(
            context,
            alarm.id.hashCode(),
            Intent(context, MainActivity::class.java)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .setData(Uri.parse("studyzen://session/${Uri.encode(alarm.sessionId)}")),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val stopIntent = PendingIntent.getBroadcast(
            context,
            alarm.id.hashCode(),
            Intent(context, ReminderReceiver::class.java)
                .setAction(ReminderScheduler.ACTION_STOP)
                .setData(Uri.parse("studyzen://reminder/${Uri.encode(alarm.id)}")),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(alarm.title)
            .setContentText(alarm.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(alarm.body))
            .setCategory(if (alarm.silent) NotificationCompat.CATEGORY_REMINDER else NotificationCompat.CATEGORY_ALARM)
            .setPriority(if (alarm.silent) NotificationCompat.PRIORITY_DEFAULT else NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(open)
            .setAutoCancel(true)
            .setWhen(System.currentTimeMillis())
            // a ringing reminder stops by itself after a minute if nobody answers
            .setTimeoutAfter(if (alarm.silent) 0L else 60_000L)
            .apply {
                if (!alarm.silent) addAction(0, "Stop", stopIntent)
                // Android 8+ takes sound and vibration from the channel; older versions from here.
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                    if (!alarm.silent) setSound(alarmSound(), AudioManager.STREAM_ALARM)
                    if (alarm.vibrate && !alarm.silent) setVibrate(VIBRATION)
                    if (alarm.silent) setSilent(true)
                }
            }
            .build()
        // keep ringing until the learner answers (or the timeout above), like an alarm clock
        if (!alarm.silent) notification.flags = notification.flags or Notification.FLAG_INSISTENT
        try {
            NotificationManagerCompat.from(context).notify(alarm.id.hashCode(), notification)
        } catch (e: SecurityException) {
            // notification permission was removed while the alarm was waiting
        }
    }

    private fun alarmSound(): Uri =
        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

    /**
     * Channels cannot be changed once created, so the ringing ones have new names ("alarm_v2"): phones that
     * already had the old quiet channel get the new loud one.
     */
    private fun ensureChannel(context: Context, silent: Boolean, vibrate: Boolean): String {
        val id = if (silent) "reminders_silent_v2" else "alarm_v2_${if (vibrate) "vibrate" else "still"}"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)
            if (manager.getNotificationChannel(id) == null) {
                val channel = NotificationChannel(
                    id,
                    if (silent) "Session reminders (silent)" else "Session alarms" + if (vibrate) " (with vibration)" else "",
                    if (silent) NotificationManager.IMPORTANCE_DEFAULT else NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    description = "Reminders before your study sessions"
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                    if (silent) {
                        setSound(null, null)
                        enableVibration(false)
                    } else {
                        setSound(
                            alarmSound(),
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_ALARM)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .build(),
                        )
                        enableVibration(vibrate)
                        if (vibrate) vibrationPattern = VIBRATION
                    }
                }
                manager.createNotificationChannel(channel)
            }
        }
        return id
    }

    private companion object {
        val VIBRATION = longArrayOf(0, 500, 250, 500, 250, 500)
    }
}
