package com.example.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.local.AlarmEntity
import com.example.data.local.SavedCityEntity
import com.example.data.local.StopwatchSessionEntity
import com.example.data.local.TimerEntity
import com.example.ui.viewmodel.StopwatchUiState

@Composable
fun ClockScreen(
    selectedTab: Int,
    onSelectTab: (Int) -> Unit,
    nowMillis: Long,
    use24HourTime: Boolean,
    alarms: List<AlarmEntity>,
    savedCities: List<SavedCityEntity>,
    timers: List<TimerEntity>,
    stopwatchState: StopwatchUiState,
    stopwatchSessions: List<StopwatchSessionEntity>,
    onSaveAlarm: (String?, String, Int, Int, Set<Int>, Boolean, Boolean, Int, String) -> Unit,
    onToggleAlarm: (AlarmEntity) -> Unit,
    onDeleteAlarm: (String) -> Unit,
    onAddWorldCity: (String, String, String) -> Unit,
    onRemoveCity: (String) -> Unit,
    onCreateTimer: (String, Long, String, Boolean) -> Unit,
    onToggleTimer: (TimerEntity) -> Unit,
    onResetTimer: (TimerEntity) -> Unit,
    onDeleteTimer: (String) -> Unit,
    onToggleStopwatch: () -> Unit,
    onLapStopwatch: () -> Unit,
    onResetStopwatch: () -> Unit,
    onSaveStopwatchSession: (String) -> Unit,
    onDeleteStopwatchSession: (String) -> Unit
) {
    val tabs = listOf("Alarm", "World Clock", "Timer", "Stopwatch")

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Smart Clock",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(start = 16.dp, top = 14.dp, bottom = 8.dp)
        )

        TabRow(
            selectedTabIndex = selectedTab.coerceIn(0, 3),
            modifier = Modifier.fillMaxWidth()
        ) {
            tabs.forEachIndexed { idx, title ->
                Tab(
                    selected = selectedTab == idx,
                    onClick = { onSelectTab(idx) },
                    text = { Text(title, maxLines = 1) },
                    modifier = Modifier.testTag("clock_tab_$idx")
                )
            }
        }

        when (selectedTab) {
            0 -> AlarmTab(
                alarms = alarms,
                nowMillis = nowMillis,
                use24HourTime = use24HourTime,
                onSaveAlarm = onSaveAlarm,
                onToggleAlarm = onToggleAlarm,
                onDeleteAlarm = onDeleteAlarm
            )
            1 -> WorldClockTab(
                savedCities = savedCities,
                nowMillis = nowMillis,
                use24HourTime = use24HourTime,
                onAddCity = onAddWorldCity,
                onRemoveCity = onRemoveCity
            )
            2 -> TimerTab(
                timers = timers,
                nowMillis = nowMillis,
                onCreateTimer = onCreateTimer,
                onToggleTimer = onToggleTimer,
                onResetTimer = onResetTimer,
                onDeleteTimer = onDeleteTimer
            )
            3 -> StopwatchTab(
                state = stopwatchState,
                savedSessions = stopwatchSessions,
                onToggle = onToggleStopwatch,
                onLap = onLapStopwatch,
                onReset = onResetStopwatch,
                onSaveSession = onSaveStopwatchSession,
                onDeleteSession = onDeleteStopwatchSession
            )
        }
    }
}
