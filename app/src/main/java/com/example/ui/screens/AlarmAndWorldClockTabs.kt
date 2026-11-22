package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.AlarmEntity
import com.example.data.local.SavedCityEntity
import com.example.ui.components.EmptyStatePanel
import com.example.ui.components.RRCard
import com.example.ui.components.RRGlassCard
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.util.DateTimeUtils
import java.util.Calendar
import java.util.TimeZone

@Composable
fun AlarmTab(
    alarms: List<AlarmEntity>,
    nowMillis: Long,
    use24HourTime: Boolean,
    onSaveAlarm: (String?, String, Int, Int, Set<Int>, Boolean, Boolean, Int, String) -> Unit,
    onToggleAlarm: (AlarmEntity) -> Unit,
    onDeleteAlarm: (String) -> Unit
) {
    var editingAlarm by remember { mutableStateOf<AlarmEntity?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }

    val nextTrigger = remember(alarms, nowMillis) {
        alarms.filter { it.enabled }.map { a ->
            val days = a.weekdaysCsv.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()
            a to DateTimeUtils.computeNextAlarmTriggerMillis(a.hour, a.minute, days, nowMillis)
        }.minByOrNull { it.second }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            RRGlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (nextTrigger != null) {
                                "Next alarm ${DateTimeUtils.formatCountdown(nextTrigger.second, nowMillis)}"
                            } else {
                                "No active alarms"
                            },
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = if (nextTrigger != null) {
                                "${nextTrigger.first.label.ifBlank { "Alarm" }} at ${DateTimeUtils.formatHourMinute(nextTrigger.first.hour, nextTrigger.first.minute, use24HourTime)}"
                            } else {
                                "Create unlimited alarms with custom tones and fade-in volume"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = { showCreateDialog = true },
                        modifier = Modifier.testTag("alarm_add_btn")
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Alarm")
                    }
                }
            }
        }

        if (alarms.isEmpty()) {
            item {
                EmptyStatePanel(
                    icon = Icons.Rounded.Alarm,
                    title = "No Alarms Configured",
                    message = "Tap + Alarm to schedule wake-up or daily alarms with weekday repeats."
                )
            }
        } else {
            items(alarms, key = { it.id }) { alarm ->
                val daySet = alarm.weekdaysCsv.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()
                val daySummary = if (daySet.isEmpty()) "One-time" else daySet.sorted().joinToString(" ") {
                    DateTimeUtils.weekdayShortName(it)
                }
                RRCard(onClick = { editingAlarm = alarm }) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = DateTimeUtils.formatHourMinute(alarm.hour, alarm.minute, use24HourTime),
                                fontFamily = JetBrainsMonoFontFamily,
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${alarm.label.ifBlank { "Alarm" }} • $daySummary",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "${alarm.toneName} • Snooze ${alarm.snoozeMinutes}m${if (alarm.gradualVolume) " • Gradual vol" else ""}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = alarm.enabled,
                            onCheckedChange = { onToggleAlarm(alarm) }
                        )
                        IconButton(onClick = { onDeleteAlarm(alarm.id) }) {
                            Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete alarm", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(72.dp)) }
    }

    if (showCreateDialog || editingAlarm != null) {
        AlarmEditorDialog(
            initialAlarm = editingAlarm,
            use24HourTime = use24HourTime,
            onDismiss = {
                showCreateDialog = false
                editingAlarm = null
            },
            onSave = { label, hour, min, days, vib, gradual, snooze, tone ->
                onSaveAlarm(editingAlarm?.id, label, hour, min, days, vib, gradual, snooze, tone)
                showCreateDialog = false
                editingAlarm = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun AlarmEditorDialog(
    initialAlarm: AlarmEntity?,
    use24HourTime: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, Int, Int, Set<Int>, Boolean, Boolean, Int, String) -> Unit
) {
    val timeState = rememberTimePickerState(
        initialHour = initialAlarm?.hour ?: 7,
        initialMinute = initialAlarm?.minute ?: 0,
        is24Hour = use24HourTime
    )
    var label by remember { mutableStateOf(initialAlarm?.label ?: "Morning Alarm") }
    var weekdays by remember {
        mutableStateOf(
            initialAlarm?.weekdaysCsv?.split(",")?.mapNotNull { it.trim().toIntOrNull() }?.toSet() ?: emptySet()
        )
    }
    var vibrate by remember { mutableStateOf(initialAlarm?.vibrate ?: true) }
    var gradualVolume by remember { mutableStateOf(initialAlarm?.gradualVolume ?: true) }
    var snoozeMinutes by remember { mutableIntStateOf(initialAlarm?.snoozeMinutes ?: 10) }
    var toneName by remember { mutableStateOf(initialAlarm?.toneName ?: "Dawn Chime") }

    val allDays = listOf(
        Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
        Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY
    )
    val tones = listOf("Dawn Chime", "Crystal Pulse", "Soft Horizon", "Classic Bell")
    val snoozeChoices = listOf(5, 10, 15, 20, 30)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialAlarm == null) "New Alarm" else "Edit Alarm") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TimePicker(state = timeState)

                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Alarm Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Repeat Days", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    allDays.forEach { dow ->
                        val active = dow in weekdays
                        FilterChip(
                            selected = active,
                            onClick = {
                                weekdays = if (active) weekdays - dow else weekdays + dow
                            },
                            label = { Text(DateTimeUtils.weekdayShortName(dow)) }
                        )
                    }
                }

                Text("Alarm Tone", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    tones.forEach { tone ->
                        FilterChip(
                            selected = toneName == tone,
                            onClick = { toneName = tone },
                            label = { Text(tone) }
                        )
                    }
                }

                Text("Snooze Duration", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    snoozeChoices.forEach { mins ->
                        FilterChip(
                            selected = snoozeMinutes == mins,
                            onClick = { snoozeMinutes = mins },
                            label = { Text("${mins}m") }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Gradually increase volume", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = gradualVolume, onCheckedChange = { gradualVolume = it })
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Vibrate", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = vibrate, onCheckedChange = { vibrate = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        label,
                        timeState.hour,
                        timeState.minute,
                        weekdays,
                        vibrate,
                        gradualVolume,
                        snoozeMinutes,
                        toneName
                    )
                },
                modifier = Modifier.testTag("alarm_save_btn")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun WorldClockTab(
    savedCities: List<SavedCityEntity>,
    nowMillis: Long,
    use24HourTime: Boolean,
    onAddCity: (String, String, String) -> Unit,
    onRemoveCity: (String) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    val worldCities = remember(savedCities) { savedCities.filter { it.forWorldClock } }
    val localZone = TimeZone.getDefault()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            RRGlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Local Time (${localZone.id})", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        Text(
                            text = DateTimeUtils.formatDigitalTime(nowMillis, use24HourTime, showSeconds = true, localZone),
                            fontFamily = JetBrainsMonoFontFamily,
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(DateTimeUtils.formatFullDate(nowMillis, localZone), style = MaterialTheme.typography.bodySmall)
                    }
                    Button(
                        onClick = { showAddDialog = true },
                        modifier = Modifier.testTag("world_clock_add_btn")
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("City")
                    }
                }
            }
        }

        if (worldCities.isEmpty()) {
            item {
                EmptyStatePanel(
                    icon = Icons.Rounded.Public,
                    title = "No World Clocks Added",
                    message = "Add global cities to track live time differences, day/night status, and date offsets.",
                    actionButton = {
                        Button(onClick = { showAddDialog = true }) { Text("Add City") }
                    }
                )
            }
        } else {
            items(worldCities, key = { it.id }) { city ->
                val tz = remember(city.timeZoneId) { TimeZone.getTimeZone(city.timeZoneId) }
                val isDay = DateTimeUtils.isDaytimeInZone(nowMillis, tz)
                RRCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isDay) Icons.Rounded.WbSunny else Icons.Rounded.NightsStay,
                            contentDescription = if (isDay) "Daytime" else "Nighttime",
                            tint = if (isDay) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${city.cityName}, ${city.country}", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = DateTimeUtils.formatTimeZoneDifference(nowMillis, tz),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = tz.displayName,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = DateTimeUtils.formatDigitalTime(nowMillis, use24HourTime, showSeconds = false, tz),
                            fontFamily = JetBrainsMonoFontFamily,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { onRemoveCity(city.id) }) {
                            Icon(Icons.Rounded.DeleteOutline, contentDescription = "Remove city", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(72.dp)) }
    }

    if (showAddDialog) {
        var query by remember { mutableStateOf("") }
        val results = remember(query) { DateTimeUtils.searchAllTimeZones(query) }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add World Clock City") },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text("Search city or timezone…") },
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(10.dp))
                    LazyColumn(modifier = Modifier.height(280.dp)) {
                        items(results) { opt ->
                            val tz = TimeZone.getTimeZone(opt.timeZoneId)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${opt.cityName}, ${opt.country}", style = MaterialTheme.typography.titleSmall)
                                    Text(opt.timeZoneId, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(
                                    DateTimeUtils.formatDigitalTime(nowMillis, use24HourTime, showSeconds = false, tz),
                                    fontFamily = JetBrainsMonoFontFamily,
                                    style = MaterialTheme.typography.labelLarge
                                )
                                TextButton(onClick = {
                                    onAddCity(opt.cityName, opt.country, opt.timeZoneId)
                                    showAddDialog = false
                                }) {
                                    Text("Add")
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Close") }
            }
        )
    }
}
