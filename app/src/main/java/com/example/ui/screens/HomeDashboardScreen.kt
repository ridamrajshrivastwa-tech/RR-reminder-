package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.HourglassBottom
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Watch
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.AlarmEntity
import com.example.data.local.CalendarEventEntity
import com.example.data.local.ReminderEntity
import com.example.data.local.RoutineEntity
import com.example.data.local.UserPreferences
import com.example.ui.components.EmptyStatePanel
import com.example.ui.components.HomeWatchFaceContent
import com.example.ui.components.PriorityBadge
import com.example.ui.components.RRCard
import com.example.ui.components.RRGlassCard
import com.example.ui.components.RRLogoMark
import com.example.ui.theme.AccentPalette
import com.example.ui.theme.ClockDesign
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SuccessEmerald
import com.example.ui.theme.ThemeMode
import com.example.ui.viewmodel.WeatherUiState
import com.example.util.DateTimeUtils
import java.util.Calendar
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeDashboardScreen(
    nowMillis: Long,
    preferences: UserPreferences,
    reminders: List<ReminderEntity>,
    alarms: List<AlarmEntity>,
    events: List<CalendarEventEntity>,
    routines: List<RoutineEntity>,
    weatherState: WeatherUiState,
    onSelectClockDesign: (ClockDesign) -> Unit,
    onSelectAccentPalette: (AccentPalette) -> Unit,
    onSelectThemeMode: (ThemeMode) -> Unit,
    onQuickAddReminder: () -> Unit,
    onToggleReminderComplete: (ReminderEntity) -> Unit,
    onNavigateToReminders: () -> Unit,
    onNavigateToClockTab: (Int) -> Unit,
    onNavigateToWeather: () -> Unit,
    onNavigateToCalendar: () -> Unit,
    onNavigateToRoutines: () -> Unit,
    onNavigateToFocus: () -> Unit,
    onNavigateToSleep: () -> Unit,
    onNavigateToAiAssistant: () -> Unit,
    onNavigateToStatistics: () -> Unit,
    onNavigateToNotifications: () -> Unit
) {
    var showColorQuickPicker by remember { mutableStateOf(false) }

    val currentHour = remember(nowMillis) {
        Calendar.getInstance().apply { timeInMillis = nowMillis }.get(Calendar.HOUR_OF_DAY)
    }
    val greeting = DateTimeUtils.greetingForHour(currentHour)
    val todayEpoch = remember(nowMillis) { DateTimeUtils.epochDayForMillis(nowMillis) }

    val activeReminders = remember(reminders) {
        reminders.filter { !it.archived }
    }
    val todayReminders = remember(activeReminders, nowMillis) {
        activeReminders.filter { !it.completed && DateTimeUtils.isSameLocalDay(it.triggerAtMillis, nowMillis) }
            .sortedBy { it.triggerAtMillis }
    }
    val completedTodayCount = remember(activeReminders, nowMillis) {
        activeReminders.count {
            it.completed && it.completedAtMillis != null && DateTimeUtils.isSameLocalDay(it.completedAtMillis, nowMillis)
        }
    }
    val totalDailyItems = (todayReminders.size + completedTodayCount + routines.size).coerceAtLeast(0)
    val completedRoutines = routines.count { it.completedToday }
    val doneDailyItems = completedTodayCount + completedRoutines
    val completionRatio = if (totalDailyItems > 0) doneDailyItems.toFloat() / totalDailyItems.toFloat() else 0f

    val nextAlarmPair = remember(alarms, nowMillis) {
        alarms.filter { it.enabled }.map { alarm ->
            val days = alarm.weekdaysCsv.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()
            alarm to DateTimeUtils.computeNextAlarmTriggerMillis(alarm.hour, alarm.minute, days, nowMillis)
        }.minByOrNull { it.second }
    }

    val upcomingEvents = remember(events, todayEpoch) {
        events.filter { it.dateEpochDay >= todayEpoch }.take(4)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Brand Header + Quick Action Icons
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RRLogoMark(size = 38.dp)
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "RR Reminder",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = greeting,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = { showColorQuickPicker = !showColorQuickPicker },
                    modifier = Modifier.testTag("home_palette_btn")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Palette,
                        contentDescription = "Customize Colors",
                        tint = if (showColorQuickPicker) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(
                    onClick = onNavigateToAiAssistant,
                    modifier = Modifier.testTag("home_ai_btn")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = "AI Smart Assistant",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(
                    onClick = onNavigateToNotifications,
                    modifier = Modifier.testTag("home_notifications_btn")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Notifications,
                        contentDescription = "Notification History"
                    )
                }
            }

            // Expandable Quick Color & Theme Switcher
            AnimatedVisibility(visible = showColorQuickPicker) {
                RRCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Theme & Accent Colors", style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = preferences.accentPalette.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AccentPalette.entries.forEach { palette ->
                            val selected = preferences.accentPalette == palette
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(palette.primaryDark)
                                    .then(
                                        if (selected) Modifier.border(2.5.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                        else Modifier
                                    )
                                    .clickable { onSelectAccentPalette(palette) }
                                    .testTag("home_swatch_${palette.name}")
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemeMode.entries.forEach { mode ->
                            FilterChip(
                                selected = preferences.themeMode == mode,
                                onClick = { onSelectThemeMode(mode) },
                                label = {
                                    Text(
                                        text = when (mode) {
                                            ThemeMode.SYSTEM -> "Auto"
                                            ThemeMode.DARK -> "Dark"
                                            ThemeMode.LIGHT -> "Light"
                                        }
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }

        // Selectable Watch/Clock Hero Glass Card
        item {
            RRGlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Watch,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = preferences.homeClockDesign.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        ClockDesign.entries.forEach { design ->
                            val active = preferences.homeClockDesign == design
                            Surface(
                                onClick = { onSelectClockDesign(design) },
                                shape = RoundedCornerShape(999.dp),
                                color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
                                border = BorderStroke(
                                    1.dp,
                                    if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                ),
                                modifier = Modifier.testTag("clock_design_pill_${design.name}")
                            ) {
                                Text(
                                    text = design.shortLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                HomeWatchFaceContent(
                    design = preferences.homeClockDesign,
                    nowMillis = nowMillis,
                    use24HourTime = preferences.use24HourTime,
                    doneDailyItems = doneDailyItems,
                    totalDailyItems = totalDailyItems,
                    completionRatio = completionRatio
                )

                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = onQuickAddReminder,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("home_quick_add_reminder_btn"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Quick Reminder")
                }
            }
        }

        // Next Alarm & Live Weather Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                RRCard(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("home_next_alarm_card"),
                    onClick = { onNavigateToClockTab(0) }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.Alarm,
                            contentDescription = "Next alarm",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Next Alarm", style = MaterialTheme.typography.labelLarge)
                    }
                    Spacer(Modifier.height(10.dp))
                    if (nextAlarmPair != null) {
                        val (alarm, triggerMs) = nextAlarmPair
                        Text(
                            text = DateTimeUtils.formatHourMinute(alarm.hour, alarm.minute, preferences.use24HourTime),
                            fontFamily = JetBrainsMonoFontFamily,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${alarm.label.ifBlank { "Alarm" }} • ${DateTimeUtils.formatCountdown(triggerMs, nowMillis)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else {
                        Text("No active alarm", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "Tap to schedule",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                RRCard(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("home_weather_card"),
                    onClick = onNavigateToWeather
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.WbSunny,
                            contentDescription = "Weather summary",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Weather", style = MaterialTheme.typography.labelLarge)
                    }
                    Spacer(Modifier.height(10.dp))
                    when (weatherState) {
                        is WeatherUiState.Success -> {
                            val snap = weatherState.snapshot
                            val tempDisplay = if (preferences.weatherUseFahrenheit) {
                                "${((snap.temperatureC * 9 / 5) + 32).roundToInt()}°F"
                            } else {
                                "${snap.temperatureC.roundToInt()}°C"
                            }
                            Text(
                                text = tempDisplay,
                                fontFamily = JetBrainsMonoFontFamily,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${snap.cityName} • ${snap.conditionLabel}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        is WeatherUiState.Loading -> {
                            Text("Updating…", style = MaterialTheme.typography.titleMedium)
                            Text("Fetching live forecast", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        is WeatherUiState.Error -> {
                            Text("Offline", style = MaterialTheme.typography.titleMedium)
                            Text("Tap to retry", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        }
                        WeatherUiState.NeedsLocationOrCity -> {
                            Text("Set City", style = MaterialTheme.typography.titleMedium)
                            Text("Tap for live forecast", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        // Quick Timer & Stopwatch + Productivity Tools Strip
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilledTonalButton(
                    onClick = { onNavigateToClockTab(2) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("home_quick_timer_btn"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Rounded.HourglassBottom, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Timer")
                }
                FilledTonalButton(
                    onClick = { onNavigateToClockTab(3) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("home_quick_stopwatch_btn"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Rounded.Timer, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Stopwatch")
                }
            }
        }

        // Productivity Tools Horizontal Strip
        item {
            Text("Productivity Tools", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(10.dp))
            val tools = listOf(
                Triple("Calendar", Icons.Rounded.CalendarMonth, onNavigateToCalendar),
                Triple("Routines", Icons.Rounded.Checklist, onNavigateToRoutines),
                Triple("Focus", Icons.Rounded.SelfImprovement, onNavigateToFocus),
                Triple("Sleep", Icons.Rounded.Bedtime, onNavigateToSleep),
                Triple("AI Assistant", Icons.Rounded.AutoAwesome, onNavigateToAiAssistant),
                Triple("Statistics", Icons.Rounded.BarChart, onNavigateToStatistics)
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(tools) { (title, icon, onClick) ->
                    QuickToolTile(title = title, icon = icon, onClick = onClick)
                }
            }
        }

        // Today's Upcoming Reminders
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Today's Upcoming Reminders", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "${todayReminders.size} due",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        if (todayReminders.isEmpty()) {
            item {
                EmptyStatePanel(
                    icon = Icons.Rounded.CheckCircle,
                    title = "Clear Schedule Today",
                    message = "No pending reminders left for today. Tap Quick Reminder or ask the AI Assistant to schedule one.",
                    actionButton = {
                        FilledTonalButton(onClick = onNavigateToReminders) {
                            Text("View All Reminders")
                        }
                    }
                )
            }
        } else {
            items(todayReminders.take(5), key = { it.id }) { reminder ->
                RRCard(onClick = onNavigateToReminders) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color(reminder.colorHex))
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = reminder.title,
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${DateTimeUtils.formatShortDateTime(reminder.triggerAtMillis, preferences.use24HourTime)} • ${reminder.category}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        PriorityBadge(reminder.priority)
                        IconButton(onClick = { onToggleReminderComplete(reminder) }) {
                            Icon(
                                imageVector = if (reminder.completed) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                                contentDescription = "Toggle reminder complete",
                                tint = if (reminder.completed) SuccessEmerald else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Upcoming Events
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Upcoming Events", style = MaterialTheme.typography.titleMedium)
                FilledTonalButton(onClick = onNavigateToCalendar, shape = RoundedCornerShape(12.dp)) {
                    Text("Open Calendar")
                }
            }
        }

        if (upcomingEvents.isEmpty()) {
            item {
                RRCard(onClick = onNavigateToCalendar) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.Event,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("No upcoming events", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "Tap to add meetings or blocks on your calendar",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        } else {
            items(upcomingEvents, key = { it.id }) { ev ->
                val dayMs = DateTimeUtils.millisForEpochDay(ev.dateEpochDay)
                RRCard(onClick = onNavigateToCalendar) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp, 36.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(ev.colorHex))
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(ev.title, style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = "${DateTimeUtils.formatRelativeDateLabel(dayMs)} • ${DateTimeUtils.formatMinutesOfDay(ev.startMinutes, preferences.use24HourTime)} - ${DateTimeUtils.formatMinutesOfDay(ev.endMinutes, preferences.use24HourTime)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Creator Credit Footer
        item {
            RRCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("creator_footer_card"),
                borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    RRLogoMark(size = 28.dp)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Created by Ridam Raj",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "RR Reminder • Precision Watch & Productivity",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun QuickToolTile(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp,
        modifier = Modifier
            .width(104.dp)
            .testTag("tool_tile_${title.lowercase().replace(" ", "_")}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = title, tint = MaterialTheme.colorScheme.primary)
            }
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
