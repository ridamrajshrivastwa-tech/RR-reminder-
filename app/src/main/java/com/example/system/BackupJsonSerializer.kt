package com.example.system

import com.example.data.local.AlarmEntity
import com.example.data.local.CalendarEventEntity
import com.example.data.local.ReminderEntity
import com.example.data.local.RoutineEntity
import org.json.JSONArray
import org.json.JSONObject

data class LocalBackupBundle(
    val reminders: List<ReminderEntity>,
    val alarms: List<AlarmEntity>,
    val events: List<CalendarEventEntity>,
    val routines: List<RoutineEntity>
)

object BackupJsonSerializer {
    fun exportToJson(bundle: LocalBackupBundle): String {
        val root = JSONObject()
        root.put("app", "RR Reminder")
        root.put("version", 1)
        root.put("exportedAtMillis", System.currentTimeMillis())

        val remArray = JSONArray()
        bundle.reminders.forEach { r ->
            remArray.put(JSONObject().apply {
                put("id", r.id)
                put("title", r.title)
                put("description", r.description)
                put("triggerAtMillis", r.triggerAtMillis)
                put("repeatInterval", r.repeatInterval)
                put("customRepeatDays", r.customRepeatDays)
                put("priority", r.priority)
                put("category", r.category)
                put("colorHex", r.colorHex)
                put("completed", r.completed)
                put("archived", r.archived)
                put("snoozedUntilMillis", r.snoozedUntilMillis)
                put("soundName", r.soundName)
                put("vibrate", r.vibrate)
            })
        }
        root.put("reminders", remArray)

        val alarmArray = JSONArray()
        bundle.alarms.forEach { a ->
            alarmArray.put(JSONObject().apply {
                put("id", a.id)
                put("label", a.label)
                put("hour", a.hour)
                put("minute", a.minute)
                put("weekdaysCsv", a.weekdaysCsv)
                put("enabled", a.enabled)
                put("vibrate", a.vibrate)
                put("gradualVolume", a.gradualVolume)
                put("snoozeMinutes", a.snoozeMinutes)
                put("toneName", a.toneName)
                put("bedtimeAlarm", a.bedtimeAlarm)
            })
        }
        root.put("alarms", alarmArray)

        val eventArray = JSONArray()
        bundle.events.forEach { e ->
            eventArray.put(JSONObject().apply {
                put("id", e.id)
                put("title", e.title)
                put("notes", e.notes)
                put("dateEpochDay", e.dateEpochDay)
                put("startMinutes", e.startMinutes)
                put("endMinutes", e.endMinutes)
                put("colorHex", e.colorHex)
            })
        }
        root.put("events", eventArray)

        val routineArray = JSONArray()
        bundle.routines.forEach { rt ->
            routineArray.put(JSONObject().apply {
                put("id", rt.id)
                put("title", rt.title)
                put("period", rt.period)
                put("priority", rt.priority)
                put("recurringDaily", rt.recurringDaily)
                put("scheduledMinutesOfDay", rt.scheduledMinutesOfDay)
                put("completedToday", rt.completedToday)
                put("lastCompletedEpochDay", rt.lastCompletedEpochDay)
                put("streakCount", rt.streakCount)
            })
        }
        root.put("routines", routineArray)

        return root.toString(2)
    }

    fun importFromJson(rawJson: String): LocalBackupBundle {
        val root = JSONObject(rawJson)
        val remArray = root.optJSONArray("reminders") ?: JSONArray()
        val reminders = (0 until remArray.length()).mapNotNull { i ->
            val o = remArray.optJSONObject(i) ?: return@mapNotNull null
            ReminderEntity(
                id = o.optString("id").ifBlank { return@mapNotNull null },
                title = o.optString("title", "Reminder"),
                description = o.optString("description", ""),
                triggerAtMillis = o.optLong("triggerAtMillis", System.currentTimeMillis()),
                repeatInterval = o.optString("repeatInterval", "NONE"),
                customRepeatDays = o.optInt("customRepeatDays", 1),
                priority = o.optString("priority", "MEDIUM"),
                category = o.optString("category", "Personal"),
                colorHex = o.optInt("colorHex", 0xFF3B82F6.toInt()),
                completed = o.optBoolean("completed", false),
                archived = o.optBoolean("archived", false),
                snoozedUntilMillis = o.optLong("snoozedUntilMillis", 0L),
                soundName = o.optString("soundName", "Default Chime"),
                vibrate = o.optBoolean("vibrate", true)
            )
        }

        val alarmArray = root.optJSONArray("alarms") ?: JSONArray()
        val alarms = (0 until alarmArray.length()).mapNotNull { i ->
            val o = alarmArray.optJSONObject(i) ?: return@mapNotNull null
            AlarmEntity(
                id = o.optString("id").ifBlank { return@mapNotNull null },
                label = o.optString("label", "Alarm"),
                hour = o.optInt("hour", 7).coerceIn(0, 23),
                minute = o.optInt("minute", 0).coerceIn(0, 59),
                weekdaysCsv = o.optString("weekdaysCsv", ""),
                enabled = o.optBoolean("enabled", true),
                vibrate = o.optBoolean("vibrate", true),
                gradualVolume = o.optBoolean("gradualVolume", true),
                snoozeMinutes = o.optInt("snoozeMinutes", 10).coerceIn(1, 60),
                toneName = o.optString("toneName", "Dawn Chime"),
                bedtimeAlarm = o.optBoolean("bedtimeAlarm", false)
            )
        }

        val eventArray = root.optJSONArray("events") ?: JSONArray()
        val events = (0 until eventArray.length()).mapNotNull { i ->
            val o = eventArray.optJSONObject(i) ?: return@mapNotNull null
            CalendarEventEntity(
                id = o.optString("id").ifBlank { return@mapNotNull null },
                title = o.optString("title", "Event"),
                notes = o.optString("notes", ""),
                dateEpochDay = o.optLong("dateEpochDay", 0L),
                startMinutes = o.optInt("startMinutes", 540).coerceIn(0, 1439),
                endMinutes = o.optInt("endMinutes", 600).coerceIn(0, 1439),
                colorHex = o.optInt("colorHex", 0xFF3B82F6.toInt())
            )
        }

        val routineArray = root.optJSONArray("routines") ?: JSONArray()
        val routines = (0 until routineArray.length()).mapNotNull { i ->
            val o = routineArray.optJSONObject(i) ?: return@mapNotNull null
            RoutineEntity(
                id = o.optString("id").ifBlank { return@mapNotNull null },
                title = o.optString("title", "Routine"),
                period = o.optString("period", "MORNING"),
                priority = o.optString("priority", "MEDIUM"),
                recurringDaily = o.optBoolean("recurringDaily", true),
                scheduledMinutesOfDay = o.optInt("scheduledMinutesOfDay", 480),
                completedToday = o.optBoolean("completedToday", false),
                lastCompletedEpochDay = o.optLong("lastCompletedEpochDay", -1L),
                streakCount = o.optInt("streakCount", 0)
            )
        }

        return LocalBackupBundle(reminders, alarms, events, routines)
    }
}
