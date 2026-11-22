package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders ORDER BY triggerAtMillis ASC")
    fun observeAllReminders(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders ORDER BY triggerAtMillis ASC")
    suspend fun getAllRemindersOnce(): List<ReminderEntity>

    @Query("SELECT * FROM reminders WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ReminderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(reminder: ReminderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(reminders: List<ReminderEntity>)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM reminders")
    suspend fun deleteAll()
}

@Dao
interface AlarmDao {
    @Query("SELECT * FROM alarms ORDER BY hour ASC, minute ASC")
    fun observeAllAlarms(): Flow<List<AlarmEntity>>

    @Query("SELECT * FROM alarms ORDER BY hour ASC, minute ASC")
    suspend fun getAllAlarmsOnce(): List<AlarmEntity>

    @Query("SELECT * FROM alarms WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): AlarmEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(alarm: AlarmEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(alarms: List<AlarmEntity>)

    @Query("DELETE FROM alarms WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface CalendarEventDao {
    @Query("SELECT * FROM calendar_events ORDER BY dateEpochDay ASC, startMinutes ASC")
    fun observeAllEvents(): Flow<List<CalendarEventEntity>>

    @Query("SELECT * FROM calendar_events ORDER BY dateEpochDay ASC, startMinutes ASC")
    suspend fun getAllEventsOnce(): List<CalendarEventEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(event: CalendarEventEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(events: List<CalendarEventEntity>)

    @Query("DELETE FROM calendar_events WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface RoutineDao {
    @Query("SELECT * FROM routines ORDER BY scheduledMinutesOfDay ASC")
    fun observeAllRoutines(): Flow<List<RoutineEntity>>

    @Query("SELECT * FROM routines")
    suspend fun getAllRoutinesOnce(): List<RoutineEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(routine: RoutineEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(routines: List<RoutineEntity>)

    @Query("DELETE FROM routines WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface FocusSessionDao {
    @Query("SELECT * FROM focus_sessions ORDER BY completedAtMillis DESC")
    fun observeAllSessions(): Flow<List<FocusSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: FocusSessionEntity)
}

@Dao
interface StopwatchSessionDao {
    @Query("SELECT * FROM stopwatch_sessions ORDER BY createdAtMillis DESC")
    fun observeAllSessions(): Flow<List<StopwatchSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: StopwatchSessionEntity)

    @Query("DELETE FROM stopwatch_sessions WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface SavedCityDao {
    @Query("SELECT * FROM saved_cities ORDER BY createdAtMillis ASC")
    fun observeAllCities(): Flow<List<SavedCityEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(city: SavedCityEntity)

    @Query("DELETE FROM saved_cities WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface TimerDao {
    @Query("SELECT * FROM timers ORDER BY lastUsedMillis DESC")
    fun observeAllTimers(): Flow<List<TimerEntity>>

    @Query("SELECT * FROM timers")
    suspend fun getAllTimersOnce(): List<TimerEntity>

    @Query("SELECT * FROM timers WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): TimerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(timer: TimerEntity)

    @Query("DELETE FROM timers WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface NotificationLogDao {
    @Query("SELECT * FROM notification_logs ORDER BY timestampMillis DESC LIMIT 200")
    fun observeRecentLogs(): Flow<List<NotificationLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: NotificationLogEntity)

    @Query("DELETE FROM notification_logs")
    suspend fun clearAll()
}
