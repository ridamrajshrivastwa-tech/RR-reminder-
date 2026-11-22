package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class ReminderPriority(val label: String) {
    LOW("Low"),
    MEDIUM("Medium"),
    HIGH("High"),
    URGENT("Urgent")
}

enum class RepeatInterval(val label: String) {
    NONE("One-time"),
    DAILY("Daily"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly"),
    YEARLY("Yearly"),
    CUSTOM("Custom Days")
}

enum class RoutinePeriod(val label: String) {
    MORNING("Morning"),
    EVENING("Evening"),
    CUSTOM("Custom")
}

enum class TimerRunState {
    IDLE,
    RUNNING,
    PAUSED,
    FINISHED
}

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    val triggerAtMillis: Long,
    val repeatInterval: String = RepeatInterval.NONE.name,
    val customRepeatDays: Int = 1,
    val priority: String = ReminderPriority.MEDIUM.name,
    val category: String = "Personal",
    val colorHex: Int = 0xFF3B82F6.toInt(),
    val attachmentUri: String? = null,
    val attachmentName: String? = null,
    val completed: Boolean = false,
    val completedAtMillis: Long? = null,
    val archived: Boolean = false,
    val snoozedUntilMillis: Long = 0L,
    val soundName: String = "Default Chime",
    val vibrate: Boolean = true,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val label: String = "Alarm",
    val hour: Int,
    val minute: Int,
    val weekdaysCsv: String = "",
    val enabled: Boolean = true,
    val vibrate: Boolean = true,
    val gradualVolume: Boolean = true,
    val snoozeMinutes: Int = 10,
    val toneName: String = "Dawn Chime",
    val bedtimeAlarm: Boolean = false,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "calendar_events")
data class CalendarEventEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val notes: String = "",
    val dateEpochDay: Long,
    val startMinutes: Int = 540,
    val endMinutes: Int = 600,
    val colorHex: Int = 0xFF3B82F6.toInt(),
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "routines")
data class RoutineEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val period: String = RoutinePeriod.MORNING.name,
    val priority: String = ReminderPriority.MEDIUM.name,
    val recurringDaily: Boolean = true,
    val scheduledMinutesOfDay: Int = 480,
    val completedToday: Boolean = false,
    val lastCompletedEpochDay: Long = -1L,
    val streakCount: Int = 0,
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "focus_sessions")
data class FocusSessionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val label: String = "Focus Session",
    val durationMinutes: Int,
    val sessionType: String = "WORK", // WORK, SHORT_BREAK, LONG_BREAK
    val completedAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "stopwatch_sessions")
data class StopwatchSessionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val label: String,
    val totalMillis: Long,
    val lapTimesCsv: String,
    val bestLapMillis: Long,
    val slowestLapMillis: Long,
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "saved_cities")
data class SavedCityEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val cityName: String,
    val country: String,
    val timeZoneId: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val forWeather: Boolean = false,
    val forWorldClock: Boolean = true,
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "timers")
data class TimerEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val label: String = "Timer",
    val initialDurationMillis: Long,
    val remainingMillis: Long,
    val targetEndWallClockMillis: Long = 0L,
    val runState: String = TimerRunState.IDLE.name,
    val soundName: String = "Bell Pulse",
    val lastUsedMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "notification_logs")
data class NotificationLogEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val message: String,
    val sourceType: String, // REMINDER, ALARM, TIMER, SLEEP, MISSED
    val status: String, // TRIGGERED, COMPLETED, SNOOZED, DISMISSED, MISSED
    val timestampMillis: Long = System.currentTimeMillis()
)
