package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.firebase.AiAssistantRepository
import com.example.data.firebase.CloudBackupRepository
import com.example.data.firebase.authStateFlow
import com.example.data.firebase.configureAppCheck
import com.example.data.local.RRDatabase
import com.example.data.local.ReminderEntity
import com.example.data.local.ReminderPriority
import com.example.data.local.RepeatInterval
import com.example.data.local.UserPreferencesRepository
import com.example.data.weather.WeatherRepository
import com.example.notifications.NotificationHelper
import com.example.notifications.SchedulerBridge
import com.example.ui.components.RRLogoMark
import com.example.ui.screens.AiAssistantScreen
import com.example.ui.screens.AppLockScreen
import com.example.ui.screens.CalendarScreen
import com.example.ui.screens.ClockScreen
import com.example.ui.screens.FocusScreen
import com.example.ui.screens.HomeDashboardScreen
import com.example.ui.screens.NotificationHistoryScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.ReminderEditorDialog
import com.example.ui.screens.RemindersScreen
import com.example.ui.screens.RoutinesScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SleepScreen
import com.example.ui.screens.StatisticsScreen
import com.example.ui.screens.WeatherScreen
import com.example.ui.theme.RRReminderTheme
import com.example.ui.theme.ReminderColorOptions
import com.example.ui.viewmodel.AiAndCloudViewModel
import com.example.ui.viewmodel.RRViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

private object NavRoutes {
    const val HOME = "home"
    const val REMINDERS = "reminders"
    const val CLOCK = "clock"
    const val WEATHER = "weather"
    const val SETTINGS = "settings"

    const val CALENDAR = "calendar"
    const val ROUTINES = "routines"
    const val FOCUS = "focus"
    const val SLEEP = "sleep"
    const val AI_ASSISTANT = "ai_assistant"
    const val STATISTICS = "statistics"
    const val NOTIFICATIONS = "notifications"
}

private data class TopLevelDestination(
    val route: String,
    val label: String,
    val icon: ImageVector
)

private val TOP_DESTINATIONS = listOf(
    TopLevelDestination(NavRoutes.HOME, "Home", Icons.Rounded.Dashboard),
    TopLevelDestination(NavRoutes.REMINDERS, "Reminders", Icons.Rounded.NotificationsActive),
    TopLevelDestination(NavRoutes.CLOCK, "Clock", Icons.Rounded.AccessTime),
    TopLevelDestination(NavRoutes.WEATHER, "Weather", Icons.Rounded.WbSunny),
    TopLevelDestination(NavRoutes.SETTINGS, "Settings", Icons.Rounded.Settings)
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureAppCheck(this, intent)
        enableEdgeToEdge()
        NotificationHelper.ensureChannels(this)

        val appContext = applicationContext
        val rrFactory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return RRViewModel(
                    db = RRDatabase.getInstance(appContext),
                    prefsRepo = UserPreferencesRepository(appContext),
                    weatherRepo = WeatherRepository(),
                    scheduler = SchedulerBridge(appContext)
                ) as T
            }
        }

        setContent {
            val rrViewModel: RRViewModel = viewModel(factory = rrFactory)
            val preferences by rrViewModel.preferences.collectAsStateWithLifecycle()
            val isLocked by rrViewModel.isLocked.collectAsStateWithLifecycle()
            val currentUser by Firebase.auth.authStateFlow()
                .collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)

            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner, rrViewModel) {
                val observer = LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_PAUSE -> rrViewModel.onAppPaused()
                        Lifecycle.Event.ON_RESUME -> rrViewModel.onAppResumed()
                        else -> Unit
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }

            RRReminderTheme(
                themeMode = preferences.themeMode,
                accentPalette = preferences.accentPalette
            ) {
                when {
                    isLocked -> {
                        AppLockScreen(
                            biometricEnabled = preferences.biometricEnabled,
                            onVerifyPin = { pin -> rrViewModel.unlockWithPin(pin) },
                            onBiometricSuccess = { rrViewModel.unlockWithBiometric() }
                        )
                    }

                    !preferences.onboardingCompleted -> {
                        OnboardingScreen(
                            isSignedIn = currentUser != null,
                            userEmail = currentUser?.email ?: currentUser?.displayName,
                            onContinue = { rrViewModel.completeOnboarding() }
                        )
                    }

                    else -> {
                        val uid = currentUser?.uid
                        key(uid) {
                            val aiCloudFactory = remember(uid, appContext) {
                                object : ViewModelProvider.Factory {
                                    @Suppress("UNCHECKED_CAST")
                                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                                        return AiAndCloudViewModel(
                                            aiRepo = AiAssistantRepository(),
                                            cloudRepo = CloudBackupRepository(appContext),
                                            prefsRepo = UserPreferencesRepository(appContext),
                                            currentUserId = uid
                                        ) as T
                                    }
                                }
                            }
                            val aiCloudViewModel: AiAndCloudViewModel = viewModel(
                                key = uid ?: "guest",
                                factory = aiCloudFactory
                            )

                            RRAppShell(
                                rrViewModel = rrViewModel,
                                aiCloudViewModel = aiCloudViewModel,
                                isSignedIn = currentUser != null,
                                userEmail = currentUser?.email ?: currentUser?.displayName
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RRAppShell(
    rrViewModel: RRViewModel,
    aiCloudViewModel: AiAndCloudViewModel,
    isSignedIn: Boolean,
    userEmail: String?
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: NavRoutes.HOME

    var clockTab by rememberSaveable { mutableIntStateOf(0) }
    var showReminderEditor by remember { mutableStateOf(false) }
    var editingReminder by remember { mutableStateOf<ReminderEntity?>(null) }
    var askedNotifOnce by rememberSaveable { mutableStateOf(false) }

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(Unit) {
        if (!askedNotifOnce &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !NotificationHelper.hasNotificationPermission(context)
        ) {
            askedNotifOnce = true
            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val preferences by rrViewModel.preferences.collectAsStateWithLifecycle()
    val nowMillis by rrViewModel.nowMillis.collectAsStateWithLifecycle()
    val reminders by rrViewModel.reminders.collectAsStateWithLifecycle()
    val alarms by rrViewModel.alarms.collectAsStateWithLifecycle()
    val events by rrViewModel.events.collectAsStateWithLifecycle()
    val routines by rrViewModel.routines.collectAsStateWithLifecycle()
    val focusSessions by rrViewModel.focusSessions.collectAsStateWithLifecycle()
    val stopwatchSessions by rrViewModel.stopwatchSessions.collectAsStateWithLifecycle()
    val savedCities by rrViewModel.savedCities.collectAsStateWithLifecycle()
    val timers by rrViewModel.timers.collectAsStateWithLifecycle()
    val notificationLogs by rrViewModel.notificationLogs.collectAsStateWithLifecycle()
    val weatherState by rrViewModel.weatherState.collectAsStateWithLifecycle()
    val citySearchResults by rrViewModel.citySearchResults.collectAsStateWithLifecycle()
    val citySearchError by rrViewModel.citySearchError.collectAsStateWithLifecycle()
    val stopwatchState by rrViewModel.stopwatchState.collectAsStateWithLifecycle()
    val pomodoroState by rrViewModel.pomodoroState.collectAsStateWithLifecycle()

    val aiParseState by aiCloudViewModel.parseState.collectAsStateWithLifecycle()
    val aiInsightState by aiCloudViewModel.insightState.collectAsStateWithLifecycle()
    val backupHistoryState by aiCloudViewModel.backupHistoryState.collectAsStateWithLifecycle()
    val cloudActionState by aiCloudViewModel.cloudActionState.collectAsStateWithLifecycle()

    val isTopRoute = currentRoute in TOP_DESTINATIONS.map { it.route }

    fun navigateTopLevel(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Row(modifier = Modifier.fillMaxSize()) {
            if (useRail && isTopRoute) {
                NavigationRail(
                    modifier = Modifier.fillMaxHeight(),
                    header = {
                        Box(modifier = Modifier.padding(vertical = 12.dp)) {
                            RRLogoMark(size = 34.dp)
                        }
                    }
                ) {
                    TOP_DESTINATIONS.forEach { dest ->
                        NavigationRailItem(
                            selected = currentRoute == dest.route,
                            onClick = { navigateTopLevel(dest.route) },
                            icon = { Icon(dest.icon, contentDescription = dest.label) },
                            label = { Text(dest.label) },
                            modifier = Modifier.testTag("nav_rail_${dest.route}")
                        )
                    }
                }
            }

            Scaffold(
                modifier = Modifier.weight(1f),
                contentWindowInsets = WindowInsets.safeDrawing,
                containerColor = MaterialTheme.colorScheme.background,
                bottomBar = {
                    if (!useRail && isTopRoute) {
                        NavigationBar {
                            TOP_DESTINATIONS.forEach { dest ->
                                NavigationBarItem(
                                    selected = currentRoute == dest.route,
                                    onClick = { navigateTopLevel(dest.route) },
                                    icon = { Icon(dest.icon, contentDescription = dest.label) },
                                    label = { Text(dest.label, maxLines = 1) },
                                    modifier = Modifier.testTag("nav_bar_${dest.route}")
                                )
                            }
                        }
                    }
                },
                floatingActionButton = {
                    if (currentRoute == NavRoutes.HOME || currentRoute == NavRoutes.REMINDERS) {
                        FloatingActionButton(
                            onClick = {
                                editingReminder = null
                                showReminderEditor = true
                            },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.testTag("global_add_reminder_fab")
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = "Add reminder")
                        }
                    }
                }
            ) { innerPadding ->
                NavHost(
                    navController = navController,
                    startDestination = NavRoutes.HOME,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    composable(NavRoutes.HOME) {
                        HomeDashboardScreen(
                            nowMillis = nowMillis,
                            preferences = preferences,
                            reminders = reminders,
                            alarms = alarms,
                            events = events,
                            routines = routines,
                            weatherState = weatherState,
                            onSelectClockDesign = { rrViewModel.setHomeClockDesign(it) },
                            onSelectAccentPalette = { rrViewModel.setAccentPalette(it) },
                            onSelectThemeMode = { rrViewModel.setThemeMode(it) },
                            onQuickAddReminder = {
                                editingReminder = null
                                showReminderEditor = true
                            },
                            onToggleReminderComplete = { rrViewModel.toggleReminderCompleted(it) },
                            onNavigateToReminders = { navigateTopLevel(NavRoutes.REMINDERS) },
                            onNavigateToClockTab = { tab ->
                                clockTab = tab
                                navigateTopLevel(NavRoutes.CLOCK)
                            },
                            onNavigateToWeather = { navigateTopLevel(NavRoutes.WEATHER) },
                            onNavigateToCalendar = { navController.navigate(NavRoutes.CALENDAR) },
                            onNavigateToRoutines = { navController.navigate(NavRoutes.ROUTINES) },
                            onNavigateToFocus = { navController.navigate(NavRoutes.FOCUS) },
                            onNavigateToSleep = { navController.navigate(NavRoutes.SLEEP) },
                            onNavigateToAiAssistant = { navController.navigate(NavRoutes.AI_ASSISTANT) },
                            onNavigateToStatistics = { navController.navigate(NavRoutes.STATISTICS) },
                            onNavigateToNotifications = { navController.navigate(NavRoutes.NOTIFICATIONS) }
                        )
                    }

                    composable(NavRoutes.REMINDERS) {
                        RemindersScreen(
                            reminders = reminders,
                            nowMillis = nowMillis,
                            use24HourTime = preferences.use24HourTime,
                            onCreateNew = {
                                editingReminder = null
                                showReminderEditor = true
                            },
                            onEditReminder = { r ->
                                editingReminder = r
                                showReminderEditor = true
                            },
                            onToggleComplete = { rrViewModel.toggleReminderCompleted(it) },
                            onSnooze = { r, mins -> rrViewModel.snoozeReminder(r, mins) },
                            onToggleArchive = { rrViewModel.toggleArchiveReminder(it) },
                            onDelete = { id -> rrViewModel.deleteReminder(id) },
                            onOpenAiAssistant = { navController.navigate(NavRoutes.AI_ASSISTANT) },
                            onOpenNotificationHistory = { navController.navigate(NavRoutes.NOTIFICATIONS) }
                        )
                    }

                    composable(NavRoutes.CLOCK) {
                        ClockScreen(
                            selectedTab = clockTab,
                            onSelectTab = { clockTab = it },
                            nowMillis = nowMillis,
                            use24HourTime = preferences.use24HourTime,
                            alarms = alarms,
                            savedCities = savedCities,
                            timers = timers,
                            stopwatchState = stopwatchState,
                            stopwatchSessions = stopwatchSessions,
                            onSaveAlarm = { id, label, h, m, days, vib, grad, snooze, tone ->
                                rrViewModel.saveAlarm(
                                    existingId = id,
                                    label = label,
                                    hour = h,
                                    minute = m,
                                    weekdays = days,
                                    vibrate = vib,
                                    gradualVolume = grad,
                                    snoozeMinutes = snooze,
                                    toneName = tone
                                )
                            },
                            onToggleAlarm = { rrViewModel.toggleAlarmEnabled(it) },
                            onDeleteAlarm = { rrViewModel.deleteAlarm(it) },
                            onAddWorldCity = { name, country, tz -> rrViewModel.addWorldClockCity(name, country, tz) },
                            onRemoveCity = { rrViewModel.removeSavedCity(it) },
                            onCreateTimer = { label, ms, sound, auto -> rrViewModel.createTimer(label, ms, sound, auto) },
                            onToggleTimer = { rrViewModel.toggleTimerRunPause(it) },
                            onResetTimer = { rrViewModel.resetTimer(it) },
                            onDeleteTimer = { rrViewModel.deleteTimer(it) },
                            onToggleStopwatch = { rrViewModel.toggleStopwatch() },
                            onLapStopwatch = { rrViewModel.recordStopwatchLap() },
                            onResetStopwatch = { rrViewModel.resetStopwatch() },
                            onSaveStopwatchSession = { rrViewModel.saveStopwatchSession(it) },
                            onDeleteStopwatchSession = { rrViewModel.deleteStopwatchSession(it) }
                        )
                    }

                    composable(NavRoutes.WEATHER) {
                        WeatherScreen(
                            weatherState = weatherState,
                            savedCities = savedCities,
                            citySearchResults = citySearchResults,
                            citySearchError = citySearchError,
                            useFahrenheit = preferences.weatherUseFahrenheit,
                            use24HourTime = preferences.use24HourTime,
                            onSearchCities = { rrViewModel.searchWeatherCities(it) },
                            onSelectSearchCity = { city ->
                                rrViewModel.selectWeatherCity(
                                    city.name,
                                    city.country,
                                    city.latitude,
                                    city.longitude,
                                    city.timezone
                                )
                            },
                            onSelectSavedCity = { city ->
                                if (city.latitude != null && city.longitude != null) {
                                    rrViewModel.selectWeatherCity(
                                        city.cityName,
                                        city.country,
                                        city.latitude,
                                        city.longitude,
                                        city.timeZoneId
                                    )
                                }
                            },
                            onRemoveSavedCity = { rrViewModel.removeSavedCity(it) },
                            onUseDeviceLocation = { rrViewModel.fetchWeatherFromDeviceLocation(context) },
                            onRetry = {
                                if (preferences.selectedWeatherCityName.isNotBlank()) {
                                    rrViewModel.loadWeather(
                                        preferences.selectedWeatherCityName,
                                        preferences.selectedWeatherLat,
                                        preferences.selectedWeatherLon
                                    )
                                }
                            },
                            onToggleUnit = { rrViewModel.setWeatherUseFahrenheit(it) }
                        )
                    }

                    composable(NavRoutes.SETTINGS) {
                        SettingsScreen(
                            preferences = preferences,
                            isSignedIn = isSignedIn,
                            userEmail = userEmail,
                            backupHistoryState = backupHistoryState,
                            cloudActionState = cloudActionState,
                            onSetThemeMode = { rrViewModel.setThemeMode(it) },
                            onSetAccentPalette = { rrViewModel.setAccentPalette(it) },
                            onSetHomeClockDesign = { rrViewModel.setHomeClockDesign(it) },
                            onSetUse24HourTime = { rrViewModel.setUse24HourTime(it) },
                            onSetFirstDayOfWeek = { rrViewModel.setFirstDayOfWeek(it) },
                            onSetLanguageLabel = { rrViewModel.setLanguageLabel(it) },
                            onSetWeatherFahrenheit = { rrViewModel.setWeatherUseFahrenheit(it) },
                            onSetNotificationSound = { rrViewModel.setNotificationSound(it) },
                            onSetNotificationVibration = { rrViewModel.setNotificationVibration(it) },
                            onSetPrivacyHideLockScreen = { rrViewModel.setPrivacyHideLockScreen(it) },
                            onConfigurePinLock = { en, pin -> rrViewModel.configurePinLock(en, pin) },
                            onSetBiometricEnabled = { rrViewModel.setBiometricEnabled(it) },
                            onSetAutoLockMinutes = { rrViewModel.setAutoLockMinutes(it) },
                            onLockNow = { rrViewModel.lockNow() },
                            onSetAutoCloudBackup = { rrViewModel.setAutoCloudBackup(it) },
                            onTriggerCloudBackup = {
                                aiCloudViewModel.triggerCloudBackup(reminders, alarms, events)
                            },
                            onTriggerCloudRestore = {
                                aiCloudViewModel.triggerCloudRestore { payload ->
                                    rrViewModel.applyCloudRestore(payload.reminders, payload.alarms, payload.events)
                                }
                            },
                            onExportJson = { rrViewModel.exportLocalBackupJson() },
                            onImportJson = { json -> rrViewModel.importLocalBackupJson(json) },
                            onOpenNotificationsScreen = { navController.navigate(NavRoutes.NOTIFICATIONS) }
                        )
                    }

                    composable(NavRoutes.CALENDAR) {
                        CalendarScreen(
                            reminders = reminders,
                            events = events,
                            use24HourTime = preferences.use24HourTime,
                            firstDayOfWeek = preferences.firstDayOfWeek,
                            onBack = { navController.popBackStack() },
                            onSaveEvent = { id, title, notes, day, start, end, color ->
                                rrViewModel.saveCalendarEvent(id, title, notes, day, start, end, color)
                            },
                            onDeleteEvent = { rrViewModel.deleteCalendarEvent(it) }
                        )
                    }

                    composable(NavRoutes.ROUTINES) {
                        val overdue = remember(reminders, nowMillis) {
                            reminders.filter { !it.completed && !it.archived && it.triggerAtMillis < nowMillis }
                        }
                        RoutinesScreen(
                            routines = routines,
                            overdueReminders = overdue,
                            use24HourTime = preferences.use24HourTime,
                            onBack = { navController.popBackStack() },
                            onAddRoutine = { title, period, prio, daily, mins ->
                                rrViewModel.saveRoutine(title, period, prio, daily, mins)
                            },
                            onToggleRoutine = { rrViewModel.toggleRoutineCompleted(it) },
                            onDeleteRoutine = { rrViewModel.deleteRoutine(it) },
                            onCompleteOverdueReminder = { rrViewModel.toggleReminderCompleted(it) },
                            onSnoozeOverdueReminder = { r, mins -> rrViewModel.snoozeReminder(r, mins) }
                        )
                    }

                    composable(NavRoutes.FOCUS) {
                        FocusScreen(
                            pomodoroState = pomodoroState,
                            preferences = preferences,
                            focusSessions = focusSessions,
                            onBack = { navController.popBackStack() },
                            onSelectPhase = { rrViewModel.selectPomodoroPhase(it) },
                            onToggleRun = { rrViewModel.togglePomodoro() },
                            onReset = { rrViewModel.resetPomodoro() },
                            onUpdateDurations = { w, s, l -> rrViewModel.updatePomodoroDurations(w, s, l) }
                        )
                    }

                    composable(NavRoutes.SLEEP) {
                        SleepScreen(
                            preferences = preferences,
                            onBack = { navController.popBackStack() },
                            onUpdateSleepSchedule = { bh, bm, wh, wm, en, gw ->
                                rrViewModel.updateSleepSchedule(bh, bm, wh, wm, en, gw)
                            },
                            onUpdateChecklist = { rrViewModel.updateBedtimeChecklist(it) }
                        )
                    }

                    composable(NavRoutes.AI_ASSISTANT) {
                        AiAssistantScreen(
                            parseState = aiParseState,
                            insightState = aiInsightState,
                            use24HourTime = preferences.use24HourTime,
                            reminders = reminders,
                            alarms = alarms,
                            events = events,
                            routines = routines,
                            onBack = { navController.popBackStack() },
                            onParseReminder = { aiCloudViewModel.parseNaturalLanguageReminder(it) },
                            onConfirmCreateReminder = { draft, triggerMs ->
                                val repeat = RepeatInterval.entries.firstOrNull { it.name == draft.repeatInterval }
                                    ?: RepeatInterval.NONE
                                val prio = ReminderPriority.entries.firstOrNull { it.name == draft.priority }
                                    ?: ReminderPriority.MEDIUM
                                rrViewModel.saveReminder(
                                    title = draft.title,
                                    description = draft.description,
                                    triggerAtMillis = triggerMs,
                                    repeatInterval = repeat,
                                    customRepeatDays = 1,
                                    priority = prio,
                                    category = draft.category,
                                    colorHex = ReminderColorOptions.first()
                                )
                                aiCloudViewModel.dismissDraft()
                            },
                            onDismissDraft = { aiCloudViewModel.dismissDraft() },
                            onRunProductivityPrompt = { aiCloudViewModel.runProductivityPrompt(it) }
                        )
                    }

                    composable(NavRoutes.STATISTICS) {
                        StatisticsScreen(
                            reminders = reminders,
                            routines = routines,
                            focusSessions = focusSessions,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(NavRoutes.NOTIFICATIONS) {
                        NotificationHistoryScreen(
                            logs = notificationLogs,
                            use24HourTime = preferences.use24HourTime,
                            onBack = { navController.popBackStack() },
                            onClearLogs = { rrViewModel.clearNotificationLogs() }
                        )
                    }
                }
            }
        }
    }

    if (showReminderEditor) {
        ReminderEditorDialog(
            initialReminder = editingReminder,
            use24HourTime = preferences.use24HourTime,
            onDismiss = { showReminderEditor = false },
            onSave = { title, desc, triggerMs, repeat, customDays, priority, category, colorHex, attUri, attName, vibrate ->
                rrViewModel.saveReminder(
                    existingId = editingReminder?.id,
                    title = title,
                    description = desc,
                    triggerAtMillis = triggerMs,
                    repeatInterval = repeat,
                    customRepeatDays = customDays,
                    priority = priority,
                    category = category,
                    colorHex = colorHex,
                    attachmentUri = attUri,
                    attachmentName = attName,
                    vibrate = vibrate
                )
                showReminderEditor = false
            }
        )
    }
}
