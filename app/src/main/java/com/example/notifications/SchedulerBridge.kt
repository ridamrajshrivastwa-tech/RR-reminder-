package com.example.notifications

import android.content.Context
import com.example.data.local.AlarmEntity
import com.example.data.local.ReminderEntity
import com.example.data.local.TimerEntity

class SchedulerBridge(context: Context) {
    private val appContext = context.applicationContext

    fun scheduleReminder(reminder: ReminderEntity) = AlarmScheduler.scheduleReminder(appContext, reminder)
    fun cancelReminder(reminderId: String) = AlarmScheduler.cancelReminder(appContext, reminderId)
    fun scheduleAlarm(alarm: AlarmEntity) = AlarmScheduler.scheduleAlarm(appContext, alarm)
    fun cancelAlarm(alarmId: String) = AlarmScheduler.cancelAlarm(appContext, alarmId)
    fun scheduleTimer(timer: TimerEntity) = AlarmScheduler.scheduleTimer(appContext, timer)
    fun cancelTimer(timerId: String) = AlarmScheduler.cancelTimer(appContext, timerId)
    fun scheduleBedtimeReminder(hour: Int, minute: Int, enabled: Boolean) =
        AlarmScheduler.scheduleBedtimeReminder(appContext, hour, minute, enabled)
    fun configureAutoBackupJob(enabled: Boolean) = AlarmScheduler.configureAutoBackupJob(appContext, enabled)
}
