package com.example.data.firebase

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class CloudReminder(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val description: String = "",
    val triggerAtMillis: Long = 0L,
    val repeatInterval: String = "NONE",
    val customRepeatDays: Int = 0,
    val priority: String = "MEDIUM",
    val category: String = "Personal",
    val colorHex: Int = 0xFF3B82F6.toInt(),
    val completed: Boolean = false,
    val archived: Boolean = false,
    val snoozedUntilMillis: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CloudAlarm(
    @DocumentId val id: String = "",
    val userId: String = "",
    val label: String = "",
    val hour: Int = 7,
    val minute: Int = 0,
    val enabled: Boolean = true,
    val vibrate: Boolean = true,
    val gradualVolume: Boolean = true,
    val snoozeMinutes: Int = 10,
    val toneName: String = "Dawn Chime",
    val weekdaysCsv: String = "",
    val bedtimeAlarm: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CloudEvent(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val notes: String = "",
    val dateEpochDay: Long = 0L,
    val startMinutes: Int = 540,
    val endMinutes: Int = 600,
    val colorHex: Int = 0xFF3B82F6.toInt(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class BackupRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val reminderCount: Int = 0,
    val alarmCount: Int = 0,
    val eventCount: Int = 0,
    val status: String = "SUCCESS",
    val deviceLabel: String = "Android",
    @ServerTimestamp val createdAt: Timestamp? = null
)
