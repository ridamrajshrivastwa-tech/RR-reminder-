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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.HourglassBottom
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.StopwatchSessionEntity
import com.example.data.local.TimerEntity
import com.example.data.local.TimerRunState
import com.example.ui.components.EmptyStatePanel
import com.example.ui.components.RRCard
import com.example.ui.components.RRGlassCard
import com.example.ui.theme.ErrorCrimson
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SuccessEmerald
import com.example.ui.viewmodel.StopwatchUiState
import com.example.util.DateTimeUtils

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TimerTab(
    timers: List<TimerEntity>,
    nowMillis: Long,
    onCreateTimer: (String, Long, String, Boolean) -> Unit,
    onToggleTimer: (TimerEntity) -> Unit,
    onResetTimer: (TimerEntity) -> Unit,
    onDeleteTimer: (String) -> Unit
) {
    var label by remember { mutableStateOf("") }
    var hoursText by remember { mutableStateOf("0") }
    var minsText by remember { mutableStateOf("5") }
    var secsText by remember { mutableStateOf("0") }
    var selectedSound by remember { mutableStateOf("Bell Pulse") }

    val presets = listOf(
        "1m" to 60_000L,
        "3m" to 3 * 60_000L,
        "5m" to 5 * 60_000L,
        "10m" to 10 * 60_000L,
        "15m" to 15 * 60_000L,
        "25m" to 25 * 60_000L,
        "45m" to 45 * 60_000L
    )
    val sounds = listOf("Bell Pulse", "Digital Radar", "Soft Chime")

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            RRCard {
                Text("New Timer", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Timer Name (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = hoursText,
                        onValueChange = { hoursText = it.filter { c -> c.isDigit() }.take(2) },
                        label = { Text("Hours") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = minsText,
                        onValueChange = { minsText = it.filter { c -> c.isDigit() }.take(2) },
                        label = { Text("Min") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = secsText,
                        onValueChange = { secsText = it.filter { c -> c.isDigit() }.take(2) },
                        label = { Text("Sec") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(10.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    sounds.forEach { s ->
                        FilterChip(
                            selected = selectedSound == s,
                            onClick = { selectedSound = s },
                            label = { Text(s) }
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = {
                        val h = hoursText.toLongOrNull() ?: 0L
                        val m = minsText.toLongOrNull() ?: 0L
                        val s = secsText.toLongOrNull() ?: 0L
                        val totalMs = ((h * 3600L) + (m * 60L) + s) * 1000L
                        if (totalMs > 0L) {
                            onCreateTimer(label.ifBlank { "Custom Timer" }, totalMs, selectedSound, true)
                            label = ""
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("timer_start_custom_btn")
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Start Timer")
                }
            }
        }

        item {
            Text("Preset Timers", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                presets.forEach { (tag, durationMs) ->
                    FilledTonalButton(
                        onClick = { onCreateTimer("$tag Preset", durationMs, selectedSound, true) },
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(tag)
                    }
                }
            }
        }

        item {
            Text("Active & Recent Timers", style = MaterialTheme.typography.titleMedium)
        }

        if (timers.isEmpty()) {
            item {
                EmptyStatePanel(
                    icon = Icons.Rounded.HourglassBottom,
                    title = "No Timers Yet",
                    message = "Start a custom timer or tap any preset duration above."
                )
            }
        } else {
            items(timers, key = { it.id }) { timer ->
                val remaining = when (timer.runState) {
                    TimerRunState.RUNNING.name -> (timer.targetEndWallClockMillis - nowMillis).coerceAtLeast(0L)
                    TimerRunState.FINISHED.name -> 0L
                    else -> timer.remainingMillis
                }
                val progress = if (timer.initialDurationMillis > 0L) {
                    (remaining.toFloat() / timer.initialDurationMillis.toFloat()).coerceIn(0f, 1f)
                } else 0f

                RRGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(timer.label, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = DateTimeUtils.formatDurationMillis(remaining),
                                fontFamily = JetBrainsMonoFontFamily,
                                style = MaterialTheme.typography.displayMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${timer.soundName} • ${timer.runState}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row {
                            IconButton(onClick = { onToggleTimer(timer) }) {
                                Icon(
                                    imageVector = if (timer.runState == TimerRunState.RUNNING.name) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    contentDescription = "Start or pause timer"
                                )
                            }
                            IconButton(onClick = { onResetTimer(timer) }) {
                                Icon(Icons.Rounded.Refresh, contentDescription = "Reset timer")
                            }
                            IconButton(onClick = { onDeleteTimer(timer.id) }) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete timer", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        item { Spacer(Modifier.height(72.dp)) }
    }
}

@Composable
fun StopwatchTab(
    state: StopwatchUiState,
    savedSessions: List<StopwatchSessionEntity>,
    onToggle: () -> Unit,
    onLap: () -> Unit,
    onReset: () -> Unit,
    onSaveSession: (String) -> Unit,
    onDeleteSession: (String) -> Unit
) {
    var showSaveDialog by remember { mutableStateOf(false) }
    var sessionLabel by remember { mutableStateOf("Run / Session") }

    val bestLapMs = remember(state.laps) {
        if (state.laps.size >= 2) state.laps.minOfOrNull { it.lapDurationMs } else null
    }
    val slowestLapMs = remember(state.laps) {
        if (state.laps.size >= 2) state.laps.maxOfOrNull { it.lapDurationMs } else null
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            RRGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = DateTimeUtils.formatDurationMillis(state.elapsedMs, includeCentiseconds = true),
                        fontFamily = JetBrainsMonoFontFamily,
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onToggle,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("stopwatch_start_pause_btn")
                        ) {
                            Icon(
                                imageVector = if (state.running) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = null
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(if (state.running) "Pause" else if (state.elapsedMs > 0L) "Resume" else "Start")
                        }
                        FilledTonalButton(
                            onClick = onLap,
                            enabled = state.elapsedMs > 0L,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("stopwatch_lap_btn")
                        ) {
                            Icon(Icons.Rounded.Flag, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Lap")
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onReset,
                            enabled = state.elapsedMs > 0L,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.Refresh, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Reset")
                        }
                        OutlinedButton(
                            onClick = { showSaveDialog = true },
                            enabled = state.elapsedMs > 0L,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.BookmarkAdd, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Save")
                        }
                    }
                }
            }
        }

        if (state.laps.isNotEmpty()) {
            item {
                Text("Laps & Splits", style = MaterialTheme.typography.titleMedium)
            }
            items(state.laps, key = { it.lapNumber }) { lap ->
                val isBest = bestLapMs != null && lap.lapDurationMs == bestLapMs
                val isSlowest = slowestLapMs != null && lap.lapDurationMs == slowestLapMs && bestLapMs != slowestLapMs
                RRCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Lap ${lap.lapNumber}", style = MaterialTheme.typography.titleSmall)
                                if (isBest) {
                                    Text("BEST", style = MaterialTheme.typography.labelMedium, color = SuccessEmerald)
                                } else if (isSlowest) {
                                    Text("SLOWEST", style = MaterialTheme.typography.labelMedium, color = ErrorCrimson)
                                }
                            }
                            Text(
                                text = "Split: ${DateTimeUtils.formatDurationMillis(lap.splitTotalMs, true)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = DateTimeUtils.formatDurationMillis(lap.lapDurationMs, true),
                            fontFamily = JetBrainsMonoFontFamily,
                            style = MaterialTheme.typography.titleMedium,
                            color = when {
                                isBest -> SuccessEmerald
                                isSlowest -> ErrorCrimson
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                }
            }
        }

        if (savedSessions.isNotEmpty()) {
            item {
                Text("Saved Sessions", style = MaterialTheme.typography.titleMedium)
            }
            items(savedSessions, key = { it.id }) { session ->
                RRCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(session.label, style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = "Total: ${DateTimeUtils.formatDurationMillis(session.totalMillis, true)}",
                                fontFamily = JetBrainsMonoFontFamily,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Best: ${DateTimeUtils.formatDurationMillis(session.bestLapMillis, true)} • Slowest: ${DateTimeUtils.formatDurationMillis(session.slowestLapMillis, true)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { onDeleteSession(session.id) }) {
                            Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete session", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(72.dp)) }
    }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Save Stopwatch Session") },
            text = {
                OutlinedTextField(
                    value = sessionLabel,
                    onValueChange = { sessionLabel = it },
                    label = { Text("Session Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    onSaveSession(sessionLabel)
                    showSaveDialog = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) { Text("Cancel") }
            }
        )
    }
}
