package com.example.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.system.AlarmRingActivity

object NotificationHelper {
    const val CHANNEL_REMINDERS = "rr_reminders_high"
    const val CHANNEL_ALARMS = "rr_alarms_critical"
    const val CHANNEL_TIMERS = "rr_timers"
    const val CHANNEL_SLEEP = "rr_sleep_routines"

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val reminderChannel = NotificationChannel(
            CHANNEL_REMINDERS,
            "Reminders",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Scheduled reminder alerts with Complete and Snooze actions"
            enableVibration(true)
            enableLights(true)
        }

        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val alarmChannel = NotificationChannel(
            CHANNEL_ALARMS,
            "Alarms",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Wake-up and scheduled alarm alerts"
            enableVibration(true)
            setBypassDnd(true)
            setSound(
                alarmSound,
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
        }

        val timerChannel = NotificationChannel(
            CHANNEL_TIMERS,
            "Timers",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Countdown timer completion alerts"
            enableVibration(true)
        }

        val sleepChannel = NotificationChannel(
            CHANNEL_SLEEP,
            "Sleep & Routines",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Bedtime and daily routine reminders"
        }

        nm.createNotificationChannels(
            listOf(reminderChannel, alarmChannel, timerChannel, sleepChannel)
        )
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun canScheduleExactAlarms(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            am.canScheduleExactAlarms()
        } else {
            true
        }
    }

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun openExactAlarmSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    fun openNotificationSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun openBatterySettings(context: Context) {
        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun postReminderNotification(
        context: Context,
        reminderId: String,
        title: String,
        notes: String,
        hideDetailsOnLockScreen: Boolean,
        playSound: Boolean,
        vibrate: Boolean
    ) {
        if (!hasNotificationPermission(context)) return
        ensureChannels(context)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notifId = reminderId.hashCode()

        val openIntent = PendingIntent.getActivity(
            context,
            notifId,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val completeIntent = PendingIntent.getBroadcast(
            context,
            (reminderId + "_complete").hashCode(),
            Intent(context, ReminderAlarmReceiver::class.java).apply {
                action = ReminderAlarmReceiver.ACTION_REMINDER_COMPLETE
                putExtra(ReminderAlarmReceiver.EXTRA_ID, reminderId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = PendingIntent.getBroadcast(
            context,
            (reminderId + "_snooze").hashCode(),
            Intent(context, ReminderAlarmReceiver::class.java).apply {
                action = ReminderAlarmReceiver.ACTION_REMINDER_SNOOZE
                putExtra(ReminderAlarmReceiver.EXTRA_ID, reminderId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val publicFallback = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("RR Reminder")
            .setContentText("You have a scheduled reminder.")
            .build()

        val builder = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(notes.ifBlank { "Scheduled reminder due now" })
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(openIntent)
            .addAction(0, "Complete", completeIntent)
            .addAction(0, "Snooze 10m", snoozeIntent)

        if (hideDetailsOnLockScreen) {
            builder.setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            builder.setPublicVersion(publicFallback)
        } else {
            builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        }

        if (!playSound) builder.setSilent(true)
        if (!vibrate) builder.setVibrate(longArrayOf(0L))

        nm.notify(notifId, builder.build())
    }

    fun postAlarmNotification(
        context: Context,
        alarmId: String,
        label: String,
        snoozeMinutes: Int,
        gradualVolume: Boolean,
        vibrate: Boolean,
        toneName: String
    ) {
        if (!hasNotificationPermission(context)) return
        ensureChannels(context)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notifId = ("alarm_" + alarmId).hashCode()

        val ringIntent = Intent(context, AlarmRingActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(AlarmRingActivity.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmRingActivity.EXTRA_ALARM_LABEL, label)
            putExtra(AlarmRingActivity.EXTRA_SNOOZE_MINUTES, snoozeMinutes)
            putExtra(AlarmRingActivity.EXTRA_GRADUAL_VOLUME, gradualVolume)
            putExtra(AlarmRingActivity.EXTRA_VIBRATE, vibrate)
            putExtra(AlarmRingActivity.EXTRA_TONE_NAME, toneName)
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            notifId,
            ringIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = PendingIntent.getBroadcast(
            context,
            ("alarm_dismiss_" + alarmId).hashCode(),
            Intent(context, ReminderAlarmReceiver::class.java).apply {
                action = ReminderAlarmReceiver.ACTION_ALARM_DISMISS
                putExtra(ReminderAlarmReceiver.EXTRA_ID, alarmId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = PendingIntent.getBroadcast(
            context,
            ("alarm_snooze_" + alarmId).hashCode(),
            Intent(context, ReminderAlarmReceiver::class.java).apply {
                action = ReminderAlarmReceiver.ACTION_ALARM_SNOOZE
                putExtra(ReminderAlarmReceiver.EXTRA_ID, alarmId)
                putExtra(ReminderAlarmReceiver.EXTRA_SNOOZE_MINUTES, snoozeMinutes)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ALARMS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(label.ifBlank { "RR Reminder Alarm" })
            .setContentText("Tap to open alarm controls")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .setAutoCancel(false)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .addAction(0, "Dismiss", dismissIntent)
            .addAction(0, "Snooze ${snoozeMinutes}m", snoozeIntent)
            .build()

        nm.notify(notifId, notification)
    }

    fun postTimerCompletedNotification(context: Context, timerId: String, label: String) {
        if (!hasNotificationPermission(context)) return
        ensureChannels(context)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val openIntent = PendingIntent.getActivity(
            context,
            ("timer_" + timerId).hashCode(),
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_TIMERS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Timer Finished")
            .setContentText("$label has completed.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(openIntent)
            .build()
        nm.notify(("timer_" + timerId).hashCode(), notification)
    }

    fun postSleepReminderNotification(context: Context) {
        if (!hasNotificationPermission(context)) return
        ensureChannels(context)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(context, CHANNEL_SLEEP)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Bedtime Wind-Down")
            .setContentText("It's time for your evening wind-down routine.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        nm.notify(88001, notification)
    }

    fun postMissedRemindersNotification(context: Context, count: Int) {
        if (!hasNotificationPermission(context) || count <= 0) return
        ensureChannels(context)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val openIntent = PendingIntent.getActivity(
            context,
            88002,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Missed Reminders")
            .setContentText("You have $count overdue reminder${if (count > 1) "s" else ""} while the device was off.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openIntent)
            .build()
        nm.notify(88002, notification)
    }

    fun cancelNotification(context: Context, notifId: Int) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(notifId)
    }
}
