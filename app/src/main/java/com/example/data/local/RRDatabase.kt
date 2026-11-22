package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ReminderEntity::class,
        AlarmEntity::class,
        CalendarEventEntity::class,
        RoutineEntity::class,
        FocusSessionEntity::class,
        StopwatchSessionEntity::class,
        SavedCityEntity::class,
        TimerEntity::class,
        NotificationLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class RRDatabase : RoomDatabase() {
    abstract fun reminderDao(): ReminderDao
    abstract fun alarmDao(): AlarmDao
    abstract fun calendarEventDao(): CalendarEventDao
    abstract fun routineDao(): RoutineDao
    abstract fun focusSessionDao(): FocusSessionDao
    abstract fun stopwatchSessionDao(): StopwatchSessionDao
    abstract fun savedCityDao(): SavedCityDao
    abstract fun timerDao(): TimerDao
    abstract fun notificationLogDao(): NotificationLogDao

    companion object {
        @Volatile
        private var INSTANCE: RRDatabase? = null

        fun getInstance(context: Context): RRDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    RRDatabase::class.java,
                    "rr_reminder_db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
