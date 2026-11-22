package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.local.NotificationLogEntity
import com.example.data.local.RRDatabase
import com.example.data.local.TimerRunState
import com.example.data.local.UserPreferencesRepository
import com.example.system.AlarmRingActivity
import com.example.util.DateTimeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ReminderAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val appContext = context.applicationContext
        val action = intent.action ?: run {
            pendingResult.finish()
            return
        }
        val itemId = intent.getStringExtra(EXTRA_ID).orEmpty()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = RRDatabase.getInstance(appContext)
                val prefs = UserPreferencesRepository(appContext).preferences.first()

                when (action) {
                    ACTION_REMINDER_TRIGGER -> {
                        val reminder = db.reminderDao().getById(itemId) ?: return@launch
                        if (reminder.completed || reminder.archived) return@launch

                        NotificationHelper.postReminderNotification(
                            context = appContext,
                            reminderId = reminder.id,
                            title = reminder.title,
                            notes = reminder.description,
                            hideDetailsOnLockScreen = prefs.privacyHideLockScreenDetails,
                            playSound = prefs.notificationSoundEnabled,
                            vibrate = prefs.notificationVibrationEnabled && reminder.vibrate
                        )
                        db.notificationLogDao().insert(
                            NotificationLogEntity(
                                title = reminder.title,
                                message = reminder.description.ifBlank { "Reminder triggered" },
                                sourceType = "REMINDER",
                                status = "TRIGGERED"
                            )
                        )

                        val nextMillis = DateTimeUtils.computeNextRecurringReminderMillis(
                            currentTriggerMillis = reminder.triggerAtMillis,
                            repeatInterval = reminder.repeatInterval,
                            customRepeatDays = reminder.customRepeatDays
                        )
                        if (nextMillis != null) {
                            val updated = reminder.copy(
                                triggerAtMillis = nextMillis,
                                snoozedUntilMillis = 0L,
                                updatedAtMillis = System.currentTimeMillis()
                            )
                            db.reminderDao().upsert(updated)
                            AlarmScheduler.scheduleReminder(appContext, updated)
                        }
                    }

                    ACTION_REMINDER_COMPLETE -> {
                        val reminder = db.reminderDao().getById(itemId) ?: return@launch
                        db.reminderDao().upsert(
                            reminder.copy(
                                completed = true,
                                completedAtMillis = System.currentTimeMillis(),
                                snoozedUntilMillis = 0L,
                                updatedAtMillis = System.currentTimeMillis()
                            )
                        )
                        AlarmScheduler.cancelReminder(appContext, itemId)
                        NotificationHelper.cancelNotification(appContext, itemId.hashCode())
                        db.notificationLogDao().insert(
                            NotificationLogEntity(
                                title = reminder.title,
                                message = "Marked complete from notification",
                                sourceType = "REMINDER",
                                status = "COMPLETED"
                            )
                        )
                    }

                    ACTION_REMINDER_SNOOZE -> {
                        val reminder = db.reminderDao().getById(itemId) ?: return@launch
                        val snoozeUntil = System.currentTimeMillis() + 10 * 60_000L
                        val updated = reminder.copy(
                            snoozedUntilMillis = snoozeUntil,
                            updatedAtMillis = System.currentTimeMillis()
                        )
                        db.reminderDao().upsert(updated)
                        AlarmScheduler.scheduleReminder(appContext, updated)
                        NotificationHelper.cancelNotification(appContext, itemId.hashCode())
                        db.notificationLogDao().insert(
                            NotificationLogEntity(
                                title = reminder.title,
                                message = "Snoozed for 10 minutes",
                                sourceType = "REMINDER",
                                status = "SNOOZED"
                            )
                        )
                    }

                    ACTION_ALARM_TRIGGER -> {
                        val alarm = db.alarmDao().getById(itemId) ?: return@launch
                        NotificationHelper.postAlarmNotification(
                            context = appContext,
                            alarmId = alarm.id,
                            label = alarm.label,
                            snoozeMinutes = alarm.snoozeMinutes,
                            gradualVolume = alarm.gradualVolume,
                            vibrate = alarm.vibrate && prefs.notificationVibrationEnabled,
                            toneName = alarm.toneName
                        )
                        db.notificationLogDao().insert(
                            NotificationLogEntity(
                                title = alarm.label.ifBlank { "Alarm" },
                                message = "Alarm rang at ${DateTimeUtils.formatHourMinute(alarm.hour, alarm.minute, prefs.use24HourTime)}",
                                sourceType = "ALARM",
                                status = "TRIGGERED"
                            )
                        )
                        if (alarm.weekdaysCsv.isBlank()) {
                            db.alarmDao().upsert(alarm.copy(enabled = false, updatedAtMillis = System.currentTimeMillis()))
                        } else {
                            AlarmScheduler.scheduleAlarm(appContext, alarm)
                        }
                    }

                    ACTION_ALARM_DISMISS -> {
                        NotificationHelper.cancelNotification(appContext, ("alarm_" + itemId).hashCode())
                        appContext.sendBroadcast(Intent(AlarmRingActivity.ACTION_CLOSE_ALARM_SCREEN).setPackage(appContext.packageName))
                        db.notificationLogDao().insert(
                            NotificationLogEntity(
                                title = "Alarm",
                                message = "Dismissed from notification",
                                sourceType = "ALARM",
                                status = "DISMISSED"
                            )
                        )
                    }

                    ACTION_ALARM_SNOOZE -> {
                        val snoozeMins = intent.getIntExtra(EXTRA_SNOOZE_MINUTES, 10)
                        NotificationHelper.cancelNotification(appContext, ("alarm_" + itemId).hashCode())
                        appContext.sendBroadcast(Intent(AlarmRingActivity.ACTION_CLOSE_ALARM_SCREEN).setPackage(appContext.packageName))
                        AlarmScheduler.scheduleSnoozedAlarm(appContext, itemId, snoozeMins)
                        db.notificationLogDao().insert(
                            NotificationLogEntity(
                                title = "Alarm",
                                message = "Snoozed for $snoozeMins minutes",
                                sourceType = "ALARM",
                                status = "SNOOZED"
                            )
                        )
                    }

                    ACTION_TIMER_TRIGGER -> {
                        val timer = db.timerDao().getById(itemId) ?: return@launch
                        db.timerDao().upsert(
                            timer.copy(
                                remainingMillis = 0L,
                                targetEndWallClockMillis = 0L,
                                runState = TimerRunState.FINISHED.name
                            )
                        )
                        NotificationHelper.postTimerCompletedNotification(appContext, timer.id, timer.label)
                        db.notificationLogDao().insert(
                            NotificationLogEntity(
                                title = timer.label,
                                message = "Timer completed",
                                sourceType = "TIMER",
                                status = "TRIGGERED"
                            )
                        )
                    }

                    ACTION_BEDTIME_TRIGGER -> {
                        NotificationHelper.postSleepReminderNotification(appContext)
                        db.notificationLogDao().insert(
                            NotificationLogEntity(
                                title = "Bedtime Reminder",
                                message = "Evening wind-down notification",
                                sourceType = "SLEEP",
                                status = "TRIGGERED"
                            )
                        )
                        AlarmScheduler.scheduleBedtimeReminder(
                            appContext,
                            prefs.bedtimeHour,
                            prefs.bedtimeMinute,
                            prefs.sleepReminderEnabled
                        )
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_REMINDER_TRIGGER = "com.example.action.REMINDER_TRIGGER"
        const val ACTION_REMINDER_COMPLETE = "com.example.action.REMINDER_COMPLETE"
        const val ACTION_REMINDER_SNOOZE = "com.example.action.REMINDER_SNOOZE"
        const val ACTION_ALARM_TRIGGER = "com.example.action.ALARM_TRIGGER"
        const val ACTION_ALARM_DISMISS = "com.example.action.ALARM_DISMISS"
        const val ACTION_ALARM_SNOOZE = "com.example.action.ALARM_SNOOZE"
        const val ACTION_TIMER_TRIGGER = "com.example.action.TIMER_TRIGGER"
        const val ACTION_BEDTIME_TRIGGER = "com.example.action.BEDTIME_TRIGGER"
        const val EXTRA_ID = "extra_id"
        const val EXTRA_SNOOZE_MINUTES = "extra_snooze_minutes"
    }
}
