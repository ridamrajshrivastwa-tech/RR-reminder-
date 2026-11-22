package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.local.NotificationLogEntity
import com.example.data.local.RRDatabase
import com.example.data.local.TimerRunState
import com.example.data.local.UserPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action !in setOf(
                Intent.ACTION_BOOT_COMPLETED,
                Intent.ACTION_TIME_CHANGED,
                Intent.ACTION_TIMEZONE_CHANGED,
                Intent.ACTION_MY_PACKAGE_REPLACED
            )
        ) return

        val pendingResult = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = RRDatabase.getInstance(appContext)
                val prefs = UserPreferencesRepository(appContext).preferences.first()
                val now = System.currentTimeMillis()

                var missedCount = 0
                db.reminderDao().getAllRemindersOnce().forEach { reminder ->
                    if (!reminder.completed && !reminder.archived) {
                        val effectiveTime = if (reminder.snoozedUntilMillis > 0L) reminder.snoozedUntilMillis else reminder.triggerAtMillis
                        if (effectiveTime > now) {
                            AlarmScheduler.scheduleReminder(appContext, reminder)
                        } else {
                            missedCount++
                        }
                    }
                }
                if (missedCount > 0 && action == Intent.ACTION_BOOT_COMPLETED) {
                    NotificationHelper.postMissedRemindersNotification(appContext, missedCount)
                    db.notificationLogDao().insert(
                        NotificationLogEntity(
                            title = "Missed Reminders",
                            message = "$missedCount overdue reminders detected after restart",
                            sourceType = "MISSED",
                            status = "MISSED"
                        )
                    )
                }

                db.alarmDao().getAllAlarmsOnce().forEach { alarm ->
                    if (alarm.enabled) {
                        AlarmScheduler.scheduleAlarm(appContext, alarm)
                    }
                }

                db.timerDao().getAllTimersOnce().forEach { timer ->
                    if (timer.runState == TimerRunState.RUNNING.name) {
                        if (timer.targetEndWallClockMillis > now) {
                            AlarmScheduler.scheduleTimer(appContext, timer)
                        } else {
                            db.timerDao().upsert(
                                timer.copy(
                                    remainingMillis = 0L,
                                    targetEndWallClockMillis = 0L,
                                    runState = TimerRunState.FINISHED.name
                                )
                            )
                            NotificationHelper.postTimerCompletedNotification(appContext, timer.id, timer.label)
                        }
                    }
                }

                AlarmScheduler.scheduleBedtimeReminder(
                    appContext,
                    prefs.bedtimeHour,
                    prefs.bedtimeMinute,
                    prefs.sleepReminderEnabled
                )
            } finally {
                pendingResult.finish()
            }
        }
    }
}
