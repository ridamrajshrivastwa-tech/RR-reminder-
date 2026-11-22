package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.ui.theme.AccentPalette
import com.example.ui.theme.ClockDesign
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "rr_preferences")

data class UserPreferences(
    val onboardingCompleted: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.DARK,
    val accentPalette: AccentPalette = AccentPalette.SAPPHIRE,
    val homeClockDesign: ClockDesign = ClockDesign.CHRONO_DIGITAL,
    val use24HourTime: Boolean = false,
    val firstDayOfWeek: Int = Calendar.MONDAY,
    val languageLabel: String = "English (System)",
    val notificationSoundEnabled: Boolean = true,
    val notificationVibrationEnabled: Boolean = true,
    val privacyHideLockScreenDetails: Boolean = false,
    val appLockEnabled: Boolean = false,
    val appLockPinHash: String = "",
    val appLockPinSalt: String = "",
    val biometricEnabled: Boolean = false,
    val autoLockMinutes: Int = 1,
    val bedtimeHour: Int = 22,
    val bedtimeMinute: Int = 30,
    val wakeUpHour: Int = 6,
    val wakeUpMinute: Int = 30,
    val sleepReminderEnabled: Boolean = false,
    val gentleWakeAlarmEnabled: Boolean = true,
    val bedtimeChecklistCsv: String = "Dim lights|Set phone aside|Prepare tomorrow's top 3 tasks|Hydrate",
    val autoCloudBackupEnabled: Boolean = false,
    val lastBackupMillis: Long = 0L,
    val pomodoroWorkMinutes: Int = 25,
    val pomodoroShortBreakMinutes: Int = 5,
    val pomodoroLongBreakMinutes: Int = 15,
    val weatherUseFahrenheit: Boolean = false,
    val selectedWeatherCityName: String = "",
    val selectedWeatherLat: Double = 0.0,
    val selectedWeatherLon: Double = 0.0
)

class UserPreferencesRepository(private val context: Context) {
    private object Keys {
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val ACCENT_PALETTE = stringPreferencesKey("accent_palette")
        val HOME_CLOCK_DESIGN = stringPreferencesKey("home_clock_design")
        val USE_24H = booleanPreferencesKey("use_24h")
        val FIRST_DAY_OF_WEEK = intPreferencesKey("first_day_of_week")
        val LANGUAGE_LABEL = stringPreferencesKey("language_label")
        val NOTIF_SOUND = booleanPreferencesKey("notif_sound")
        val NOTIF_VIBRATION = booleanPreferencesKey("notif_vibration")
        val PRIVACY_HIDE_LOCK = booleanPreferencesKey("privacy_hide_lock")
        val APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
        val PIN_HASH = stringPreferencesKey("pin_hash")
        val PIN_SALT = stringPreferencesKey("pin_salt")
        val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        val AUTO_LOCK_MINUTES = intPreferencesKey("auto_lock_minutes")
        val BEDTIME_HOUR = intPreferencesKey("bedtime_hour")
        val BEDTIME_MINUTE = intPreferencesKey("bedtime_minute")
        val WAKE_HOUR = intPreferencesKey("wake_hour")
        val WAKE_MINUTE = intPreferencesKey("wake_minute")
        val SLEEP_REMINDER_ENABLED = booleanPreferencesKey("sleep_reminder_enabled")
        val GENTLE_WAKE = booleanPreferencesKey("gentle_wake")
        val BEDTIME_CHECKLIST = stringPreferencesKey("bedtime_checklist")
        val AUTO_BACKUP = booleanPreferencesKey("auto_backup")
        val LAST_BACKUP_MILLIS = longPreferencesKey("last_backup_millis")
        val POMODORO_WORK = intPreferencesKey("pomodoro_work")
        val POMODORO_SHORT = intPreferencesKey("pomodoro_short")
        val POMODORO_LONG = intPreferencesKey("pomodoro_long")
        val WEATHER_FAHRENHEIT = booleanPreferencesKey("weather_fahrenheit")
        val WEATHER_CITY_NAME = stringPreferencesKey("weather_city_name")
        val WEATHER_LAT = stringPreferencesKey("weather_lat")
        val WEATHER_LON = stringPreferencesKey("weather_lon")
    }

    val preferences: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        UserPreferences(
            onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: false,
            themeMode = ThemeMode.entries.firstOrNull { it.name == prefs[Keys.THEME_MODE] } ?: ThemeMode.DARK,
            accentPalette = AccentPalette.entries.firstOrNull { it.name == prefs[Keys.ACCENT_PALETTE] } ?: AccentPalette.SAPPHIRE,
            homeClockDesign = ClockDesign.entries.firstOrNull { it.name == prefs[Keys.HOME_CLOCK_DESIGN] } ?: ClockDesign.CHRONO_DIGITAL,
            use24HourTime = prefs[Keys.USE_24H] ?: false,
            firstDayOfWeek = prefs[Keys.FIRST_DAY_OF_WEEK] ?: Calendar.MONDAY,
            languageLabel = prefs[Keys.LANGUAGE_LABEL] ?: "English (System)",
            notificationSoundEnabled = prefs[Keys.NOTIF_SOUND] ?: true,
            notificationVibrationEnabled = prefs[Keys.NOTIF_VIBRATION] ?: true,
            privacyHideLockScreenDetails = prefs[Keys.PRIVACY_HIDE_LOCK] ?: false,
            appLockEnabled = prefs[Keys.APP_LOCK_ENABLED] ?: false,
            appLockPinHash = prefs[Keys.PIN_HASH] ?: "",
            appLockPinSalt = prefs[Keys.PIN_SALT] ?: "",
            biometricEnabled = prefs[Keys.BIOMETRIC_ENABLED] ?: false,
            autoLockMinutes = prefs[Keys.AUTO_LOCK_MINUTES] ?: 1,
            bedtimeHour = prefs[Keys.BEDTIME_HOUR] ?: 22,
            bedtimeMinute = prefs[Keys.BEDTIME_MINUTE] ?: 30,
            wakeUpHour = prefs[Keys.WAKE_HOUR] ?: 6,
            wakeUpMinute = prefs[Keys.WAKE_MINUTE] ?: 30,
            sleepReminderEnabled = prefs[Keys.SLEEP_REMINDER_ENABLED] ?: false,
            gentleWakeAlarmEnabled = prefs[Keys.GENTLE_WAKE] ?: true,
            bedtimeChecklistCsv = prefs[Keys.BEDTIME_CHECKLIST]
                ?: "Dim lights|Set phone aside|Prepare tomorrow's top 3 tasks|Hydrate",
            autoCloudBackupEnabled = prefs[Keys.AUTO_BACKUP] ?: false,
            lastBackupMillis = prefs[Keys.LAST_BACKUP_MILLIS] ?: 0L,
            pomodoroWorkMinutes = prefs[Keys.POMODORO_WORK] ?: 25,
            pomodoroShortBreakMinutes = prefs[Keys.POMODORO_SHORT] ?: 5,
            pomodoroLongBreakMinutes = prefs[Keys.POMODORO_LONG] ?: 15,
            weatherUseFahrenheit = prefs[Keys.WEATHER_FAHRENHEIT] ?: false,
            selectedWeatherCityName = prefs[Keys.WEATHER_CITY_NAME] ?: "",
            selectedWeatherLat = prefs[Keys.WEATHER_LAT]?.toDoubleOrNull() ?: 0.0,
            selectedWeatherLon = prefs[Keys.WEATHER_LON]?.toDoubleOrNull() ?: 0.0
        )
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { it[Keys.ONBOARDING_COMPLETED] = completed }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setAccentPalette(palette: AccentPalette) {
        context.dataStore.edit { it[Keys.ACCENT_PALETTE] = palette.name }
    }

    suspend fun setHomeClockDesign(design: ClockDesign) {
        context.dataStore.edit { it[Keys.HOME_CLOCK_DESIGN] = design.name }
    }

    suspend fun setUse24HourTime(use24h: Boolean) {
        context.dataStore.edit { it[Keys.USE_24H] = use24h }
    }

    suspend fun setFirstDayOfWeek(day: Int) {
        context.dataStore.edit { it[Keys.FIRST_DAY_OF_WEEK] = day }
    }

    suspend fun setLanguageLabel(label: String) {
        context.dataStore.edit { it[Keys.LANGUAGE_LABEL] = label }
    }

    suspend fun setNotificationSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.NOTIF_SOUND] = enabled }
    }

    suspend fun setNotificationVibrationEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.NOTIF_VIBRATION] = enabled }
    }

    suspend fun setPrivacyHideLockScreenDetails(hide: Boolean) {
        context.dataStore.edit { it[Keys.PRIVACY_HIDE_LOCK] = hide }
    }

    suspend fun setAppLock(enabled: Boolean, pinHash: String? = null, pinSalt: String? = null) {
        context.dataStore.edit {
            it[Keys.APP_LOCK_ENABLED] = enabled
            if (pinHash != null) it[Keys.PIN_HASH] = pinHash
            if (pinSalt != null) it[Keys.PIN_SALT] = pinSalt
        }
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.BIOMETRIC_ENABLED] = enabled }
    }

    suspend fun setAutoLockMinutes(minutes: Int) {
        context.dataStore.edit { it[Keys.AUTO_LOCK_MINUTES] = minutes }
    }

    suspend fun updateSleepSchedule(
        bedtimeHour: Int,
        bedtimeMinute: Int,
        wakeHour: Int,
        wakeMinute: Int,
        sleepReminderEnabled: Boolean,
        gentleWakeEnabled: Boolean
    ) {
        context.dataStore.edit {
            it[Keys.BEDTIME_HOUR] = bedtimeHour
            it[Keys.BEDTIME_MINUTE] = bedtimeMinute
            it[Keys.WAKE_HOUR] = wakeHour
            it[Keys.WAKE_MINUTE] = wakeMinute
            it[Keys.SLEEP_REMINDER_ENABLED] = sleepReminderEnabled
            it[Keys.GENTLE_WAKE] = gentleWakeEnabled
        }
    }

    suspend fun setBedtimeChecklistCsv(csv: String) {
        context.dataStore.edit { it[Keys.BEDTIME_CHECKLIST] = csv }
    }

    suspend fun setAutoCloudBackupEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_BACKUP] = enabled }
    }

    suspend fun setLastBackupMillis(millis: Long) {
        context.dataStore.edit { it[Keys.LAST_BACKUP_MILLIS] = millis }
    }

    suspend fun setPomodoroDurations(work: Int, shortBreak: Int, longBreak: Int) {
        context.dataStore.edit {
            it[Keys.POMODORO_WORK] = work.coerceIn(5, 120)
            it[Keys.POMODORO_SHORT] = shortBreak.coerceIn(1, 30)
            it[Keys.POMODORO_LONG] = longBreak.coerceIn(5, 60)
        }
    }

    suspend fun setWeatherUseFahrenheit(useFahrenheit: Boolean) {
        context.dataStore.edit { it[Keys.WEATHER_FAHRENHEIT] = useFahrenheit }
    }

    suspend fun setSelectedWeatherLocation(name: String, lat: Double, lon: Double) {
        context.dataStore.edit {
            it[Keys.WEATHER_CITY_NAME] = name
            it[Keys.WEATHER_LAT] = lat.toString()
            it[Keys.WEATHER_LON] = lon.toString()
        }
    }
}
