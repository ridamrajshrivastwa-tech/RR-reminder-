package com.example.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.example.data.local.AlarmEntity
import com.example.data.local.ReminderEntity
import com.example.data.local.TimerEntity
import com.example.data.local.TimerRunState
import com.example.system.AutoBackupJobService
import com.example.util.DateTimeUtils

object AlarmScheduler {
    private const val BACKUP_JOB_ID = 7001

    private fun scheduleExactOrInexact(context: Context, triggerAtMillis: Long, pendingIntent: PendingIntent) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val safeTime = triggerAtMillis.coerceAtLeast(System.currentTimeMillis() + 500L)
        if (NotificationHelper.canScheduleExactAlarms(context)) {
            try {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, safeTime, pendingIntent)
                return
            } catch (_: SecurityException) {
                // Fall back to inexact scheduling if revoked
            }
        }
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, safeTime, pendingIntent)
    }

    fun scheduleReminder(context: Context, reminder: ReminderEntity) {
        cancelReminder(context, reminder.id)
        if (reminder.completed || reminder.archived) return
        val effectiveTrigger = if (reminder.snoozedUntilMillis > System.currentTimeMillis()) {
            reminder.snoozedUntilMillis
        } else {
            reminder.triggerAtMillis
        }
        if (effectiveTrigger <= System.currentTimeMillis()) return

        val pi = PendingIntent.getBroadcast(
            context,
            ("rem_" + reminder.id).hashCode(),
            Intent(context, ReminderAlarmReceiver::class.java).apply {
                action = ReminderAlarmReceiver.ACTION_REMINDER_TRIGGER
                putExtra(ReminderAlarmReceiver.EXTRA_ID, reminder.id)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        scheduleExactOrInexact(context, effectiveTrigger, pi)
    }

    fun cancelReminder(context: Context, reminderId: String) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = PendingIntent.getBroadcast(
            context,
            ("rem_" + reminderId).hashCode(),
            Intent(context, ReminderAlarmReceiver::class.java).apply {
                action = ReminderAlarmReceiver.ACTION_REMINDER_TRIGGER
                putExtra(ReminderAlarmReceiver.EXTRA_ID, reminderId)
            },
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pi != null) {
            am.cancel(pi)
            pi.cancel()
        }
    }

    fun scheduleAlarm(context: Context, alarm: AlarmEntity) {
        cancelAlarm(context, alarm.id)
        if (!alarm.enabled) return
        val weekdays = alarm.weekdaysCsv.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()
        val nextTrigger = DateTimeUtils.computeNextAlarmTriggerMillis(alarm.hour, alarm.minute, weekdays)
        val pi = PendingIntent.getBroadcast(
            context,
            ("alm_" + alarm.id).hashCode(),
            Intent(context, ReminderAlarmReceiver::class.java).apply {
                action = ReminderAlarmReceiver.ACTION_ALARM_TRIGGER
                putExtra(ReminderAlarmReceiver.EXTRA_ID, alarm.id)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        scheduleExactOrInexact(context, nextTrigger, pi)
    }

    fun scheduleSnoozedAlarm(context: Context, alarmId: String, snoozeMinutes: Int) {
        val trigger = System.currentTimeMillis() + snoozeMinutes.coerceIn(1, 60) * 60_000L
        val pi = PendingIntent.getBroadcast(
            context,
            ("alm_" + alarmId).hashCode(),
            Intent(context, ReminderAlarmReceiver::class.java).apply {
                action = ReminderAlarmReceiver.ACTION_ALARM_TRIGGER
                putExtra(ReminderAlarmReceiver.EXTRA_ID, alarmId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        scheduleExactOrInexact(context, trigger, pi)
    }

    fun cancelAlarm(context: Context, alarmId: String) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = PendingIntent.getBroadcast(
            context,
            ("alm_" + alarmId).hashCode(),
            Intent(context, ReminderAlarmReceiver::class.java).apply {
                action = ReminderAlarmReceiver.ACTION_ALARM_TRIGGER
                putExtra(ReminderAlarmReceiver.EXTRA_ID, alarmId)
            },
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pi != null) {
            am.cancel(pi)
            pi.cancel()
        }
    }

    fun scheduleTimer(context: Context, timer: TimerEntity) {
        cancelTimer(context, timer.id)
        if (timer.runState != TimerRunState.RUNNING.name || timer.targetEndWallClockMillis <= System.currentTimeMillis()) return
        val pi = PendingIntent.getBroadcast(
            context,
            ("tmr_" + timer.id).hashCode(),
            Intent(context, ReminderAlarmReceiver::class.java).apply {
                action = ReminderAlarmReceiver.ACTION_TIMER_TRIGGER
                putExtra(ReminderAlarmReceiver.EXTRA_ID, timer.id)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        scheduleExactOrInexact(context, timer.targetEndWallClockMillis, pi)
    }

    fun cancelTimer(context: Context, timerId: String) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = PendingIntent.getBroadcast(
            context,
            ("tmr_" + timerId).hashCode(),
            Intent(context, ReminderAlarmReceiver::class.java).apply {
                action = ReminderAlarmReceiver.ACTION_TIMER_TRIGGER
                putExtra(ReminderAlarmReceiver.EXTRA_ID, timerId)
            },
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pi != null) {
            am.cancel(pi)
            pi.cancel()
        }
    }

    fun scheduleBedtimeReminder(context: Context, hour: Int, minute: Int, enabled: Boolean) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = PendingIntent.getBroadcast(
            context,
            88001,
            Intent(context, ReminderAlarmReceiver::class.java).apply {
                action = ReminderAlarmReceiver.ACTION_BEDTIME_TRIGGER
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        am.cancel(pi)
        if (!enabled) return
        val next = DateTimeUtils.computeNextAlarmTriggerMillis(hour, minute, emptySet())
        scheduleExactOrInexact(context, next, pi)
    }

    fun configureAutoBackupJob(context: Context, enabled: Boolean) {
        val scheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
        scheduler.cancel(BACKUP_JOB_ID)
        if (!enabled) return
        val job = JobInfo.Builder(
            BACKUP_JOB_ID,
            ComponentName(context, AutoBackupJobService::class.java)
        )
            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
            .setPeriodic(24 * 60 * 60 * 1000L)
            .setPersisted(true)
            .build()
        scheduler.schedule(job)
    }
}
