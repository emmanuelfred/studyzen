package com.fredrickemmanuel.studyzen.reminders

import android.content.Context
import com.fredrickemmanuel.studyzen.domain.ReminderAlarm
import org.json.JSONArray
import org.json.JSONObject

/**
 * The alarms saved on this phone. They live here, not only in the app's memory, so the phone can
 * ring them after a restart and while the app is closed or offline.
 */
internal object ReminderStore {
    private const val PREFS = "studyzen_reminders"
    private const val ALARMS = "alarms"
    private const val ARMED = "armed"

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun load(context: Context): List<ReminderAlarm> {
        val text = prefs(context).getString(ALARMS, null) ?: return emptyList()
        return try {
            val array = JSONArray(text)
            (0 until array.length()).map { i ->
                val o = array.getJSONObject(i)
                ReminderAlarm(
                    id = o.getString("id"),
                    sessionId = o.getString("sessionId"),
                    localEpochMinute = o.getLong("at"),
                    title = o.getString("title"),
                    body = o.getString("body"),
                    silent = o.getBoolean("silent"),
                    vibrate = o.getBoolean("vibrate"),
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun save(context: Context, alarms: List<ReminderAlarm>) {
        val array = JSONArray()
        alarms.forEach {
            array.put(
                JSONObject()
                    .put("id", it.id).put("sessionId", it.sessionId).put("at", it.localEpochMinute)
                    .put("title", it.title).put("body", it.body)
                    .put("silent", it.silent).put("vibrate", it.vibrate),
            )
        }
        prefs(context).edit().putString(ALARMS, array.toString()).apply()
    }

    fun armedIds(context: Context): List<String> {
        val text = prefs(context).getString(ARMED, null) ?: return emptyList()
        return try {
            val array = JSONArray(text)
            (0 until array.length()).map { array.getString(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveArmedIds(context: Context, ids: List<String>) {
        prefs(context).edit().putString(ARMED, JSONArray(ids).toString()).apply()
    }
}
