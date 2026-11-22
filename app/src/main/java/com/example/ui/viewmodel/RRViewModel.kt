package com.example.ui.viewmodel

import android.annotation.SuppressLint
import android.content.Context
import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AlarmEntity
import com.example.data.local.CalendarEventEntity
import com.example.data.local.FocusSessionEntity
import com.example.data.local.NotificationLogEntity
import com.example.data.local.RRDatabase
import com.example.data.local.ReminderEntity
import com.example.data.local.ReminderPriority
import com.example.data.local.RepeatInterval
import com.example.data.local.RoutineEntity
import com.example.data.local.SavedCityEntity
import com.example.data.local.StopwatchSessionEntity
import com.example.data.local.TimerEntity
import com.example.data.local.TimerRunState
import com.example.data.local.UserPreferences
import com.example.data.local.UserPreferencesRepository
import com.example.data.weather.CitySearchResult
import com.example.data.weather.WeatherRepository
import com.example.data.weather.WeatherSnapshot
import com.example.notifications.SchedulerBridge
import com.example.system.BackupJsonSerializer
import com.example.system.LocalBackupBundle
import com.example.system.SecurityUtils
import com.example.ui.theme.AccentPalette
import com.example.ui.theme.ClockDesign
import com.example.ui.theme.ThemeMode
import com.example.util.DateTimeUtils
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.UUID

sealed interface WeatherUiState {
    data object NeedsLocationOrCity : WeatherUiState
    data object Loading : WeatherUiState
    data class Success(val snapshot: WeatherSnapshot) : WeatherUiState
    data class Error(val message: String) : WeatherUiState
}

enum class PomodoroPhase(val label: String) {
    WORK("Focus"),
    SHORT_BREAK("Short Break"),
    LONG_BREAK("Long Break")
}

data class StopwatchLap(
    val lapNumber: Int,
    val lapDurationMs: Long,
    val splitTotalMs: Long
)

data class StopwatchUiState(
    val running: Boolean = false,
    val elapsedMs: Long = 0L,
    val laps: List<StopwatchLap> = emptyList()
)

data class PomodoroUiState(
    val phase: PomodoroPhase = PomodoroPhase.WORK,
    val running: Boolean = false,
    val remainingMs: Long = 25 * 60_000L,
    val completedCycles: Int = 0
)

class RRViewModel(
    private val db: RRDatabase,
    private val prefsRepo: UserPreferencesRepository,
    private val weatherRepo: WeatherRepository,
    private val scheduler: SchedulerBridge
) : ViewModel() {

    val preferences: StateFlow<UserPreferences> = prefsRepo.preferences.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000L),
        UserPreferences()
    )

    val reminders: StateFlow<List<ReminderEntity>> = db.reminderDao().observeAllReminders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val alarms: StateFlow<List<AlarmEntity>> = db.alarmDao().observeAllAlarms()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val events: StateFlow<List<CalendarEventEntity>> = db.calendarEventDao().observeAllEvents()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val routines: StateFlow<List<RoutineEntity>> = db.routineDao().observeAllRoutines()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val focusSessions: StateFlow<List<FocusSessionEntity>> = db.focusSessionDao().observeAllSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val stopwatchSessions: StateFlow<List<StopwatchSessionEntity>> = db.stopwatchSessionDao().observeAllSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val savedCities: StateFlow<List<SavedCityEntity>> = db.savedCityDao().observeAllCities()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val timers: StateFlow<List<TimerEntity>> = db.timerDao().observeAllTimers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val notificationLogs: StateFlow<List<NotificationLogEntity>> = db.notificationLogDao().observeRecentLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val _nowMillis = MutableStateFlow(System.currentTimeMillis())
    val nowMillis: StateFlow<Long> = _nowMillis.asStateFlow()

    private val _weatherState = MutableStateFlow<WeatherUiState>(WeatherUiState.NeedsLocationOrCity)
    val weatherState: StateFlow<WeatherUiState> = _weatherState.asStateFlow()

    private val _citySearchResults = MutableStateFlow<List<CitySearchResult>>(emptyList())
    val citySearchResults: StateFlow<List<CitySearchResult>> = _citySearchResults.asStateFlow()

    private val _citySearchError = MutableStateFlow<String?>(null)
    val citySearchError: StateFlow<String?> = _citySearchError.asStateFlow()

    private val _isLocked = MutableStateFlow(false)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    private var lastBackgroundedElapsedMs: Long = 0L

    // Monotonic Stopwatch state
    private val _stopwatchState = MutableStateFlow(StopwatchUiState())
    val stopwatchState: StateFlow<StopwatchUiState> = _stopwatchState.asStateFlow()
    private var swBaseElapsedRealtime = 0L
    private var swAccumulatedMs = 0L
    private var swLastLapSplitMs = 0L
    private var swTickerJob: Job? = null

    // Pomodoro state
    private val _pomodoroState = MutableStateFlow(PomodoroUiState())
    val pomodoroState: StateFlow<PomodoroUiState> = _pomodoroState.asStateFlow()
    private var pomoTargetElapsedRealtime = 0L
    private var pomoTickerJob: Job? = null

    init {
        viewModelScope.launch {
            while (true) {
                _nowMillis.value = System.currentTimeMillis()
                delay(1000L)
            }
        }
        viewModelScope.launch {
            val prefs = prefsRepo.preferences.first()
            if (prefs.appLockEnabled && prefs.appLockPinHash.isNotBlank()) {
                _isLocked.value = true
            }
            if (prefs.selectedWeatherCityName.isNotBlank()) {
                loadWeather(prefs.selectedWeatherCityName, prefs.selectedWeatherLat, prefs.selectedWeatherLon)
            }
            resetDailyRoutinesIfNeeded()
        }
    }

    private suspend fun resetDailyRoutinesIfNeeded() {
        val todayEpoch = DateTimeUtils.epochDayForMillis(System.currentTimeMillis())
        db.routineDao().getAllRoutinesOnce().forEach { rt ->
            if (rt.recurringDaily && rt.completedToday && rt.lastCompletedEpochDay < todayEpoch) {
                db.routineDao().upsert(rt.copy(completedToday = false))
            }
        }
    }

    // --- App Lock ---
    fun onAppPaused() {
        if (preferences.value.appLockEnabled) {
            lastBackgroundedElapsedMs = SystemClock.elapsedRealtime()
        }
    }

    fun onAppResumed() {
        val prefs = preferences.value
        if (prefs.appLockEnabled && prefs.appLockPinHash.isNotBlank() && lastBackgroundedElapsedMs > 0L) {
            val awayMs = SystemClock.elapsedRealtime() - lastBackgroundedElapsedMs
            val thresholdMs = prefs.autoLockMinutes.coerceAtLeast(0) * 60_000L
            if (awayMs >= thresholdMs) {
                _isLocked.value = true
            }
        }
    }

    fun unlockWithPin(pin: String): Boolean {
        val prefs = preferences.value
        val valid = SecurityUtils.verifyPin(pin, prefs.appLockPinHash, prefs.appLockPinSalt)
        if (valid) _isLocked.value = false
        return valid
    }

    fun unlockWithBiometric() {
        _isLocked.value = false
    }

    fun lockNow() {
        if (preferences.value.appLockEnabled && preferences.value.appLockPinHash.isNotBlank()) {
            _isLocked.value = true
        }
    }

    fun configurePinLock(enabled: Boolean, newPin: String?) {
        viewModelScope.launch {
            if (!enabled) {
                prefsRepo.setAppLock(false)
                prefsRepo.setBiometricEnabled(false)
                _isLocked.value = false
            } else if (!newPin.isNullOrBlank() && newPin.length >= 4) {
                val salt = SecurityUtils.generateSalt()
                val hash = SecurityUtils.hashPin(newPin, salt)
                prefsRepo.setAppLock(true, hash, salt)
            }
        }
    }

    // --- Onboarding & Preferences ---
    fun completeOnboarding() {
        viewModelScope.launch { prefsRepo.setOnboardingCompleted(true) }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { prefsRepo.setThemeMode(mode) }
    }

    fun setAccentPalette(palette: AccentPalette) {
        viewModelScope.launch { prefsRepo.setAccentPalette(palette) }
    }

    fun setHomeClockDesign(design: ClockDesign) {
        viewModelScope.launch { prefsRepo.setHomeClockDesign(design) }
    }

    fun setUse24HourTime(use24h: Boolean) {
        viewModelScope.launch { prefsRepo.setUse24HourTime(use24h) }
    }

    fun setFirstDayOfWeek(day: Int) {
        viewModelScope.launch { prefsRepo.setFirstDayOfWeek(day) }
    }

    fun setLanguageLabel(label: String) {
        viewModelScope.launch { prefsRepo.setLanguageLabel(label) }
    }

    fun setNotificationSound(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setNotificationSoundEnabled(enabled) }
    }

    fun setNotificationVibration(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setNotificationVibrationEnabled(enabled) }
    }

    fun setPrivacyHideLockScreen(hide: Boolean) {
        viewModelScope.launch { prefsRepo.setPrivacyHideLockScreenDetails(hide) }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setBiometricEnabled(enabled) }
    }

    fun setAutoLockMinutes(minutes: Int) {
        viewModelScope.launch { prefsRepo.setAutoLockMinutes(minutes) }
    }

    fun setWeatherUseFahrenheit(useFahrenheit: Boolean) {
        viewModelScope.launch { prefsRepo.setWeatherUseFahrenheit(useFahrenheit) }
    }

    fun setAutoCloudBackup(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepo.setAutoCloudBackupEnabled(enabled)
            scheduler.configureAutoBackupJob(enabled)
        }
    }

    // --- Reminders CRUD ---
    fun saveReminder(
        existingId: String? = null,
        title: String,
        description: String,
        triggerAtMillis: Long,
        repeatInterval: RepeatInterval,
        customRepeatDays: Int,
        priority: ReminderPriority,
        category: String,
        colorHex: Int,
        attachmentUri: String? = null,
        attachmentName: String? = null,
        vibrate: Boolean = true
    ) {
        viewModelScope.launch {
            val current = existingId?.let { db.reminderDao().getById(it) }
            val entity = if (current != null) {
                current.copy(
                    title = title.trim(),
                    description = description.trim(),
                    triggerAtMillis = triggerAtMillis,
                    repeatInterval = repeatInterval.name,
                    customRepeatDays = customRepeatDays.coerceAtLeast(1),
                    priority = priority.name,
                    category = category.trim().ifBlank { "Personal" },
                    colorHex = colorHex,
                    attachmentUri = attachmentUri,
                    attachmentName = attachmentName,
                    snoozedUntilMillis = 0L,
                    vibrate = vibrate,
                    updatedAtMillis = System.currentTimeMillis()
                )
            } else {
                ReminderEntity(
                    title = title.trim(),
                    description = description.trim(),
                    triggerAtMillis = triggerAtMillis,
                    repeatInterval = repeatInterval.name,
                    customRepeatDays = customRepeatDays.coerceAtLeast(1),
                    priority = priority.name,
                    category = category.trim().ifBlank { "Personal" },
                    colorHex = colorHex,
                    attachmentUri = attachmentUri,
                    attachmentName = attachmentName,
                    vibrate = vibrate
                )
            }
            db.reminderDao().upsert(entity)
            scheduler.scheduleReminder(entity)
        }
    }

    fun toggleReminderCompleted(reminder: ReminderEntity) {
        viewModelScope.launch {
            val nowCompleted = !reminder.completed
            if (nowCompleted && reminder.repeatInterval != RepeatInterval.NONE.name) {
                val nextMillis = DateTimeUtils.computeNextRecurringReminderMillis(
                    reminder.triggerAtMillis,
                    reminder.repeatInterval,
                    reminder.customRepeatDays
                )
                if (nextMillis != null) {
                    db.notificationLogDao().insert(
                        NotificationLogEntity(
                            title = reminder.title,
                            message = "Completed cycle; next occurrence scheduled",
                            sourceType = "REMINDER",
                            status = "COMPLETED"
                        )
                    )
                    val nextEntity = reminder.copy(
                        triggerAtMillis = nextMillis,
                        snoozedUntilMillis = 0L,
                        completed = false,
                        completedAtMillis = System.currentTimeMillis(),
                        updatedAtMillis = System.currentTimeMillis()
                    )
                    db.reminderDao().upsert(nextEntity)
                    scheduler.scheduleReminder(nextEntity)
                    return@launch
                }
            }

            val updated = reminder.copy(
                completed = nowCompleted,
                completedAtMillis = if (nowCompleted) System.currentTimeMillis() else null,
                snoozedUntilMillis = 0L,
                updatedAtMillis = System.currentTimeMillis()
            )
            db.reminderDao().upsert(updated)
            if (nowCompleted) {
                scheduler.cancelReminder(updated.id)
            } else {
                scheduler.scheduleReminder(updated)
            }
        }
    }

    fun snoozeReminder(reminder: ReminderEntity, minutes: Int) {
        viewModelScope.launch {
            val until = System.currentTimeMillis() + minutes * 60_000L
            val updated = reminder.copy(
                snoozedUntilMillis = until,
                updatedAtMillis = System.currentTimeMillis()
            )
            db.reminderDao().upsert(updated)
            scheduler.scheduleReminder(updated)
            db.notificationLogDao().insert(
                NotificationLogEntity(
                    title = reminder.title,
                    message = "Snoozed for $minutes minutes",
                    sourceType = "REMINDER",
                    status = "SNOOZED"
                )
            )
        }
    }

    fun toggleArchiveReminder(reminder: ReminderEntity) {
        viewModelScope.launch {
            val updated = reminder.copy(
                archived = !reminder.archived,
                updatedAtMillis = System.currentTimeMillis()
            )
            db.reminderDao().upsert(updated)
            if (updated.archived) {
                scheduler.cancelReminder(updated.id)
            } else {
                scheduler.scheduleReminder(updated)
            }
        }
    }

    fun deleteReminder(reminderId: String) {
        viewModelScope.launch {
            scheduler.cancelReminder(reminderId)
            db.reminderDao().deleteById(reminderId)
        }
    }

    fun clearNotificationLogs() {
        viewModelScope.launch { db.notificationLogDao().clearAll() }
    }

    // --- Alarms CRUD ---
    fun saveAlarm(
        existingId: String? = null,
        label: String,
        hour: Int,
        minute: Int,
        weekdays: Set<Int>,
        vibrate: Boolean,
        gradualVolume: Boolean,
        snoozeMinutes: Int,
        toneName: String,
        bedtimeAlarm: Boolean = false
    ) {
        viewModelScope.launch {
            val csv = weekdays.sorted().joinToString(",")
            val entity = AlarmEntity(
                id = existingId ?: UUID.randomUUID().toString(),
                label = label.trim().ifBlank { "Alarm" },
                hour = hour.coerceIn(0, 23),
                minute = minute.coerceIn(0, 59),
                weekdaysCsv = csv,
                enabled = true,
                vibrate = vibrate,
                gradualVolume = gradualVolume,
                snoozeMinutes = snoozeMinutes.coerceIn(1, 60),
                toneName = toneName,
                bedtimeAlarm = bedtimeAlarm,
                updatedAtMillis = System.currentTimeMillis()
            )
            db.alarmDao().upsert(entity)
            scheduler.scheduleAlarm(entity)
        }
    }

    fun toggleAlarmEnabled(alarm: AlarmEntity) {
        viewModelScope.launch {
            val updated = alarm.copy(enabled = !alarm.enabled, updatedAtMillis = System.currentTimeMillis())
            db.alarmDao().upsert(updated)
            if (updated.enabled) {
                scheduler.scheduleAlarm(updated)
            } else {
                scheduler.cancelAlarm(updated.id)
            }
        }
    }

    fun deleteAlarm(alarmId: String) {
        viewModelScope.launch {
            scheduler.cancelAlarm(alarmId)
            db.alarmDao().deleteById(alarmId)
        }
    }

    // --- World Clock Cities ---
    fun addWorldClockCity(cityName: String, country: String, timeZoneId: String) {
        viewModelScope.launch {
            val existing = savedCities.value.firstOrNull { it.timeZoneId == timeZoneId && it.forWorldClock }
            if (existing == null) {
                db.savedCityDao().upsert(
                    SavedCityEntity(
                        cityName = cityName,
                        country = country,
                        timeZoneId = timeZoneId,
                        forWorldClock = true,
                        forWeather = false
                    )
                )
            }
        }
    }

    fun removeSavedCity(cityId: String) {
        viewModelScope.launch { db.savedCityDao().deleteById(cityId) }
    }

    // --- Multi-Timer ---
    fun createTimer(label: String, durationMillis: Long, soundName: String = "Bell Pulse", autoStart: Boolean = true) {
        if (durationMillis <= 0L) return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val entity = TimerEntity(
                label = label.trim().ifBlank { "Timer" },
                initialDurationMillis = durationMillis,
                remainingMillis = durationMillis,
                targetEndWallClockMillis = if (autoStart) now + durationMillis else 0L,
                runState = if (autoStart) TimerRunState.RUNNING.name else TimerRunState.IDLE.name,
                soundName = soundName,
                lastUsedMillis = now
            )
            db.timerDao().upsert(entity)
            if (autoStart) scheduler.scheduleTimer(entity)
        }
    }

    fun toggleTimerRunPause(timer: TimerEntity) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            when (timer.runState) {
                TimerRunState.RUNNING.name -> {
                    val left = (timer.targetEndWallClockMillis - now).coerceAtLeast(0L)
                    val paused = timer.copy(
                        remainingMillis = left,
                        targetEndWallClockMillis = 0L,
                        runState = TimerRunState.PAUSED.name
                    )
                    db.timerDao().upsert(paused)
                    scheduler.cancelTimer(paused.id)
                }
                TimerRunState.PAUSED.name, TimerRunState.IDLE.name -> {
                    val left = timer.remainingMillis.coerceAtLeast(1000L)
                    val running = timer.copy(
                        targetEndWallClockMillis = now + left,
                        runState = TimerRunState.RUNNING.name,
                        lastUsedMillis = now
                    )
                    db.timerDao().upsert(running)
                    scheduler.scheduleTimer(running)
                }
                TimerRunState.FINISHED.name -> {
                    val reset = timer.copy(
                        remainingMillis = timer.initialDurationMillis,
                        targetEndWallClockMillis = now + timer.initialDurationMillis,
                        runState = TimerRunState.RUNNING.name,
                        lastUsedMillis = now
                    )
                    db.timerDao().upsert(reset)
                    scheduler.scheduleTimer(reset)
                }
            }
        }
    }

    fun resetTimer(timer: TimerEntity) {
        viewModelScope.launch {
            scheduler.cancelTimer(timer.id)
            db.timerDao().upsert(
                timer.copy(
                    remainingMillis = timer.initialDurationMillis,
                    targetEndWallClockMillis = 0L,
                    runState = TimerRunState.IDLE.name
                )
            )
        }
    }

    fun deleteTimer(timerId: String) {
        viewModelScope.launch {
            scheduler.cancelTimer(timerId)
            db.timerDao().deleteById(timerId)
        }
    }

    // --- Monotonic Stopwatch ---
    fun toggleStopwatch() {
        val current = _stopwatchState.value
        if (current.running) {
            swAccumulatedMs += (SystemClock.elapsedRealtime() - swBaseElapsedRealtime).coerceAtLeast(0L)
            swTickerJob?.cancel()
            _stopwatchState.value = current.copy(running = false, elapsedMs = swAccumulatedMs)
        } else {
            swBaseElapsedRealtime = SystemClock.elapsedRealtime()
            _stopwatchState.value = current.copy(running = true)
            swTickerJob?.cancel()
            swTickerJob = viewModelScope.launch {
                while (true) {
                    val total = swAccumulatedMs + (SystemClock.elapsedRealtime() - swBaseElapsedRealtime).coerceAtLeast(0L)
                    _stopwatchState.value = _stopwatchState.value.copy(elapsedMs = total)
                    delay(33L)
                }
            }
        }
    }

    fun recordStopwatchLap() {
        val state = _stopwatchState.value
        val total = if (state.running) {
            swAccumulatedMs + (SystemClock.elapsedRealtime() - swBaseElapsedRealtime).coerceAtLeast(0L)
        } else {
            state.elapsedMs
        }
        if (total <= 0L) return
        val lapDuration = (total - swLastLapSplitMs).coerceAtLeast(0L)
        swLastLapSplitMs = total
        val newLap = StopwatchLap(
            lapNumber = state.laps.size + 1,
            lapDurationMs = lapDuration,
            splitTotalMs = total
        )
        _stopwatchState.value = state.copy(elapsedMs = total, laps = listOf(newLap) + state.laps)
    }

    fun resetStopwatch() {
        swTickerJob?.cancel()
        swAccumulatedMs = 0L
        swLastLapSplitMs = 0L
        _stopwatchState.value = StopwatchUiState()
    }

    fun saveStopwatchSession(label: String) {
        val state = _stopwatchState.value
        if (state.elapsedMs <= 0L) return
        viewModelScope.launch {
            val lapsAsc = state.laps.reversed().map { it.lapDurationMs }
            val best = lapsAsc.minOrNull() ?: state.elapsedMs
            val slowest = lapsAsc.maxOrNull() ?: state.elapsedMs
            db.stopwatchSessionDao().insert(
                StopwatchSessionEntity(
                    label = label.trim().ifBlank { "Stopwatch Session" },
                    totalMillis = state.elapsedMs,
                    lapTimesCsv = lapsAsc.joinToString(","),
                    bestLapMillis = best,
                    slowestLapMillis = slowest
                )
            )
        }
    }

    fun deleteStopwatchSession(id: String) {
        viewModelScope.launch { db.stopwatchSessionDao().deleteById(id) }
    }

    // --- Calendar Events ---
    fun saveCalendarEvent(
        existingId: String? = null,
        title: String,
        notes: String,
        dateEpochDay: Long,
        startMinutes: Int,
        endMinutes: Int,
        colorHex: Int
    ) {
        viewModelScope.launch {
            val entity = CalendarEventEntity(
                id = existingId ?: UUID.randomUUID().toString(),
                title = title.trim(),
                notes = notes.trim(),
                dateEpochDay = dateEpochDay,
                startMinutes = startMinutes.coerceIn(0, 1439),
                endMinutes = endMinutes.coerceIn(startMinutes, 1439),
                colorHex = colorHex,
                updatedAtMillis = System.currentTimeMillis()
            )
            db.calendarEventDao().upsert(entity)
        }
    }

    fun deleteCalendarEvent(id: String) {
        viewModelScope.launch { db.calendarEventDao().deleteById(id) }
    }

    // --- Routines ---
    fun saveRoutine(title: String, period: String, priority: ReminderPriority, recurringDaily: Boolean, scheduledMinutes: Int) {
        viewModelScope.launch {
            db.routineDao().upsert(
                RoutineEntity(
                    title = title.trim(),
                    period = period,
                    priority = priority.name,
                    recurringDaily = recurringDaily,
                    scheduledMinutesOfDay = scheduledMinutes.coerceIn(0, 1439)
                )
            )
        }
    }

    fun toggleRoutineCompleted(routine: RoutineEntity) {
        viewModelScope.launch {
            val today = DateTimeUtils.epochDayForMillis(System.currentTimeMillis())
            val nextDone = !routine.completedToday
            val nextStreak = if (nextDone) routine.streakCount + 1 else (routine.streakCount - 1).coerceAtLeast(0)
            db.routineDao().upsert(
                routine.copy(
                    completedToday = nextDone,
                    lastCompletedEpochDay = if (nextDone) today else routine.lastCompletedEpochDay,
                    streakCount = nextStreak
                )
            )
        }
    }

    fun deleteRoutine(id: String) {
        viewModelScope.launch { db.routineDao().deleteById(id) }
    }

    // --- Focus Mode (Pomodoro) ---
    fun selectPomodoroPhase(phase: PomodoroPhase) {
        pomoTickerJob?.cancel()
        val prefs = preferences.value
        val mins = when (phase) {
            PomodoroPhase.WORK -> prefs.pomodoroWorkMinutes
            PomodoroPhase.SHORT_BREAK -> prefs.pomodoroShortBreakMinutes
            PomodoroPhase.LONG_BREAK -> prefs.pomodoroLongBreakMinutes
        }
        _pomodoroState.value = _pomodoroState.value.copy(
            phase = phase,
            running = false,
            remainingMs = mins * 60_000L
        )
    }

    fun updatePomodoroDurations(work: Int, shortBreak: Int, longBreak: Int) {
        viewModelScope.launch {
            prefsRepo.setPomodoroDurations(work, shortBreak, longBreak)
            if (!_pomodoroState.value.running) {
                val mins = when (_pomodoroState.value.phase) {
                    PomodoroPhase.WORK -> work
                    PomodoroPhase.SHORT_BREAK -> shortBreak
                    PomodoroPhase.LONG_BREAK -> longBreak
                }
                _pomodoroState.value = _pomodoroState.value.copy(remainingMs = mins * 60_000L)
            }
        }
    }

    fun togglePomodoro() {
        val current = _pomodoroState.value
        if (current.running) {
            pomoTickerJob?.cancel()
            val left = (pomoTargetElapsedRealtime - SystemClock.elapsedRealtime()).coerceAtLeast(0L)
            _pomodoroState.value = current.copy(running = false, remainingMs = left)
        } else {
            pomoTargetElapsedRealtime = SystemClock.elapsedRealtime() + current.remainingMs.coerceAtLeast(1000L)
            _pomodoroState.value = current.copy(running = true)
            pomoTickerJob?.cancel()
            pomoTickerJob = viewModelScope.launch {
                while (true) {
                    val left = (pomoTargetElapsedRealtime - SystemClock.elapsedRealtime()).coerceAtLeast(0L)
                    if (left <= 0L) {
                        onPomodoroCompleted()
                        break
                    }
                    _pomodoroState.value = _pomodoroState.value.copy(remainingMs = left)
                    delay(250L)
                }
            }
        }
    }

    fun resetPomodoro() {
        selectPomodoroPhase(_pomodoroState.value.phase)
    }

    private suspend fun onPomodoroCompleted() {
        val finishedPhase = _pomodoroState.value.phase
        val prefs = preferences.value
        val durationMins = when (finishedPhase) {
            PomodoroPhase.WORK -> prefs.pomodoroWorkMinutes
            PomodoroPhase.SHORT_BREAK -> prefs.pomodoroShortBreakMinutes
            PomodoroPhase.LONG_BREAK -> prefs.pomodoroLongBreakMinutes
        }
        db.focusSessionDao().insert(
            FocusSessionEntity(
                label = finishedPhase.label,
                durationMinutes = durationMins,
                sessionType = finishedPhase.name
            )
        )
        val nextCycles = if (finishedPhase == PomodoroPhase.WORK) {
            _pomodoroState.value.completedCycles + 1
        } else {
            _pomodoroState.value.completedCycles
        }
        val nextPhase = when (finishedPhase) {
            PomodoroPhase.WORK -> if (nextCycles % 4 == 0) PomodoroPhase.LONG_BREAK else PomodoroPhase.SHORT_BREAK
            PomodoroPhase.SHORT_BREAK, PomodoroPhase.LONG_BREAK -> PomodoroPhase.WORK
        }
        val nextMins = when (nextPhase) {
            PomodoroPhase.WORK -> prefs.pomodoroWorkMinutes
            PomodoroPhase.SHORT_BREAK -> prefs.pomodoroShortBreakMinutes
            PomodoroPhase.LONG_BREAK -> prefs.pomodoroLongBreakMinutes
        }
        _pomodoroState.value = PomodoroUiState(
            phase = nextPhase,
            running = false,
            remainingMs = nextMins * 60_000L,
            completedCycles = nextCycles
        )
    }

    // --- Sleep Mode ---
    fun updateSleepSchedule(
        bedtimeHour: Int,
        bedtimeMinute: Int,
        wakeHour: Int,
        wakeMinute: Int,
        sleepReminderEnabled: Boolean,
        gentleWakeEnabled: Boolean
    ) {
        viewModelScope.launch {
            prefsRepo.updateSleepSchedule(
                bedtimeHour,
                bedtimeMinute,
                wakeHour,
                wakeMinute,
                sleepReminderEnabled,
                gentleWakeEnabled
            )
            scheduler.scheduleBedtimeReminder(bedtimeHour, bedtimeMinute, sleepReminderEnabled)

            // Sync dedicated wake-up alarm
            val existingBedtimeAlarm = alarms.value.firstOrNull { it.bedtimeAlarm }
            val allWeekdays = (Calendar.SUNDAY..Calendar.SATURDAY).joinToString(",")
            val wakeAlarm = AlarmEntity(
                id = existingBedtimeAlarm?.id ?: "bedtime_wake_alarm",
                label = "Wake Up (Sleep Schedule)",
                hour = wakeHour,
                minute = wakeMinute,
                weekdaysCsv = allWeekdays,
                enabled = sleepReminderEnabled,
                vibrate = true,
                gradualVolume = gentleWakeEnabled,
                snoozeMinutes = 10,
                toneName = "Gentle Sunrise",
                bedtimeAlarm = true
            )
            db.alarmDao().upsert(wakeAlarm)
            if (wakeAlarm.enabled) scheduler.scheduleAlarm(wakeAlarm) else scheduler.cancelAlarm(wakeAlarm.id)
        }
    }

    fun updateBedtimeChecklist(items: List<String>) {
        viewModelScope.launch {
            prefsRepo.setBedtimeChecklistCsv(items.joinToString("|"))
        }
    }

    // --- Weather ---
    fun searchWeatherCities(query: String) {
        viewModelScope.launch {
            _citySearchError.value = null
            weatherRepo.searchCities(query).onSuccess {
                _citySearchResults.value = it
            }.onFailure {
                _citySearchError.value = it.localizedMessage ?: "City search failed"
            }
        }
    }

    fun selectWeatherCity(name: String, country: String, latitude: Double, longitude: Double, timeZoneId: String) {
        viewModelScope.launch {
            prefsRepo.setSelectedWeatherLocation(name, latitude, longitude)
            val alreadySaved = savedCities.value.any {
                it.forWeather && it.cityName.equals(name, ignoreCase = true)
            }
            if (!alreadySaved) {
                db.savedCityDao().upsert(
                    SavedCityEntity(
                        cityName = name,
                        country = country,
                        timeZoneId = timeZoneId,
                        latitude = latitude,
                        longitude = longitude,
                        forWeather = true,
                        forWorldClock = false
                    )
                )
            }
            loadWeather(name, latitude, longitude)
        }
    }

    fun loadWeather(cityName: String, latitude: Double, longitude: Double) {
        viewModelScope.launch {
            _weatherState.value = WeatherUiState.Loading
            weatherRepo.fetchWeather(cityName, latitude, longitude)
                .onSuccess { _weatherState.value = WeatherUiState.Success(it) }
                .onFailure { err ->
                    _weatherState.value = WeatherUiState.Error(
                        err.localizedMessage ?: "Unable to fetch live weather. Check network connectivity."
                    )
                }
        }
    }

    @SuppressLint("MissingPermission")
    fun fetchWeatherFromDeviceLocation(context: Context) {
        viewModelScope.launch {
            _weatherState.value = WeatherUiState.Loading
            try {
                val client = LocationServices.getFusedLocationProviderClient(context.applicationContext)
                val location = client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null).await()
                    ?: client.lastLocation.await()
                if (location == null) {
                    _weatherState.value = WeatherUiState.Error("Device location unavailable right now. Search for a city manually.")
                    return@launch
                }
                prefsRepo.setSelectedWeatherLocation("Current Location", location.latitude, location.longitude)
                loadWeather("Current Location", location.latitude, location.longitude)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _weatherState.value = WeatherUiState.Error(e.localizedMessage ?: "Location lookup failed. Try selecting a city.")
            }
        }
    }

    // --- Local JSON Export/Import ---
    suspend fun exportLocalBackupJson(): String {
        val bundle = LocalBackupBundle(
            reminders = db.reminderDao().getAllRemindersOnce(),
            alarms = db.alarmDao().getAllAlarmsOnce(),
            events = db.calendarEventDao().getAllEventsOnce(),
            routines = db.routineDao().getAllRoutinesOnce()
        )
        return BackupJsonSerializer.exportToJson(bundle)
    }

    suspend fun importLocalBackupJson(rawJson: String): Result<Int> {
        return try {
            val bundle = BackupJsonSerializer.importFromJson(rawJson)
            db.reminderDao().upsertAll(bundle.reminders)
            db.alarmDao().upsertAll(bundle.alarms)
            db.calendarEventDao().upsertAll(bundle.events)
            db.routineDao().upsertAll(bundle.routines)
            bundle.reminders.forEach { scheduler.scheduleReminder(it) }
            bundle.alarms.forEach { scheduler.scheduleAlarm(it) }
            prefsRepo.setLastBackupMillis(System.currentTimeMillis())
            Result.success(bundle.reminders.size + bundle.alarms.size + bundle.events.size + bundle.routines.size)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun applyCloudRestore(
        remindersToRestore: List<ReminderEntity>,
        alarmsToRestore: List<AlarmEntity>,
        eventsToRestore: List<CalendarEventEntity>
    ) {
        db.reminderDao().upsertAll(remindersToRestore)
        db.alarmDao().upsertAll(alarmsToRestore)
        db.calendarEventDao().upsertAll(eventsToRestore)
        remindersToRestore.forEach { scheduler.scheduleReminder(it) }
        alarmsToRestore.forEach { scheduler.scheduleAlarm(it) }
        prefsRepo.setLastBackupMillis(System.currentTimeMillis())
    }
}
