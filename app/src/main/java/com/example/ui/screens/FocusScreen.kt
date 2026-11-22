package com.example.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DoNotDisturbOn
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.FocusSessionEntity
import com.example.data.local.UserPreferences
import com.example.ui.components.EmptyStatePanel
import com.example.ui.components.RRCard
import com.example.ui.components.SubScreenTopBar
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.viewmodel.PomodoroPhase
import com.example.ui.viewmodel.PomodoroUiState
import com.example.util.DateTimeUtils
import kotlin.math.roundToInt

@Composable
fun FocusScreen(
    pomodoroState: PomodoroUiState,
    preferences: UserPreferences,
    focusSessions: List<FocusSessionEntity>,
    onBack: () -> Unit,
    onSelectPhase: (PomodoroPhase) -> Unit,
    onToggleRun: () -> Unit,
    onReset: () -> Unit,
    onUpdateDurations: (Int, Int, Int) -> Unit
) {
    val context = LocalContext.current
    val totalPhaseMs = when (pomodoroState.phase) {
        PomodoroPhase.WORK -> preferences.pomodoroWorkMinutes * 60_000L
        PomodoroPhase.SHORT_BREAK -> preferences.pomodoroShortBreakMinutes * 60_000L
        PomodoroPhase.LONG_BREAK -> preferences.pomodoroLongBreakMinutes * 60_000L
    }.coerceAtLeast(1L)
    val progress = (pomodoroState.remainingMs.toFloat() / totalPhaseMs.toFloat()).coerceIn(0f, 1f)

    val todayMinutes = remember(focusSessions) {
        val now = System.currentTimeMillis()
        focusSessions.filter {
            it.sessionType == PomodoroPhase.WORK.name && DateTimeUtils.isSameLocalDay(it.completedAtMillis, now)
        }.sumOf { it.durationMinutes }
    }

    var workSlider by remember(preferences.pomodoroWorkMinutes) { mutableFloatStateOf(preferences.pomodoroWorkMinutes.toFloat()) }
    var shortSlider by remember(preferences.pomodoroShortBreakMinutes) { mutableFloatStateOf(preferences.pomodoroShortBreakMinutes.toFloat()) }
    var longSlider by remember(preferences.pomodoroLongBreakMinutes) { mutableFloatStateOf(preferences.pomodoroLongBreakMinutes.toFloat()) }

    Column(modifier = Modifier.fillMaxSize()) {
        SubScreenTopBar(
            title = "Focus Mode",
            subtitle = "$todayMinutes focus min today • ${pomodoroState.completedCycles} cycles",
            onBack = onBack
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(22.dp))
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_focus_zen),
                        contentDescription = "Focus mode studio illustration",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))))
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp)
                    ) {
                        Text("Deep Work Pomodoro", style = MaterialTheme.typography.titleLarge, color = Color.White)
                        Text("Eliminate distractions and track focused sprints", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f))
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PomodoroPhase.entries.forEach { phase ->
                        FilterChip(
                            selected = pomodoroState.phase == phase,
                            onClick = { onSelectPhase(phase) },
                            label = { Text(phase.label) }
                        )
                    }
                }
            }

            item {
                RRCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val ringColor = MaterialTheme.colorScheme.primary
                        val trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                        Box(
                            modifier = Modifier.size(210.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.size(196.dp)) {
                                val stroke = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
                                drawArc(trackColor, 0f, 360f, false, style = stroke)
                                drawArc(ringColor, -90f, 360f * progress, false, style = stroke)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(pomodoroState.phase.label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                                Text(
                                    text = DateTimeUtils.formatDurationMillis(pomodoroState.remainingMs),
                                    fontFamily = JetBrainsMonoFontFamily,
                                    style = MaterialTheme.typography.displayMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = onToggleRun,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("focus_start_pause_btn")
                            ) {
                                Icon(if (pomodoroState.running) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text(if (pomodoroState.running) "Pause" else "Start")
                            }
                            OutlinedButton(
                                onClick = onReset,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Rounded.Refresh, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Reset")
                            }
                        }
                    }
                }
            }

            item {
                RRCard {
                    Text("Custom Timer Intervals", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text("Focus Duration: ${workSlider.roundToInt()} min", style = MaterialTheme.typography.bodySmall)
                    Slider(
                        value = workSlider,
                        onValueChange = { workSlider = it },
                        valueRange = 5f..90f,
                        onValueChangeFinished = {
                            onUpdateDurations(workSlider.roundToInt(), shortSlider.roundToInt(), longSlider.roundToInt())
                        }
                    )
                    Text("Short Break: ${shortSlider.roundToInt()} min", style = MaterialTheme.typography.bodySmall)
                    Slider(
                        value = shortSlider,
                        onValueChange = { shortSlider = it },
                        valueRange = 1f..20f,
                        onValueChangeFinished = {
                            onUpdateDurations(workSlider.roundToInt(), shortSlider.roundToInt(), longSlider.roundToInt())
                        }
                    )
                    Text("Long Break: ${longSlider.roundToInt()} min", style = MaterialTheme.typography.bodySmall)
                    Slider(
                        value = longSlider,
                        onValueChange = { longSlider = it },
                        valueRange = 5f..45f,
                        onValueChangeFinished = {
                            onUpdateDurations(workSlider.roundToInt(), shortSlider.roundToInt(), longSlider.roundToInt())
                        }
                    )
                }
            }

            item {
                RRCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.DoNotDisturbOn, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Notification Blocking Guidance", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "Enable Android Do Not Disturb during focus sprints to silence distracting notifications.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = {
                        try {
                            context.startActivity(
                                Intent(Settings.ACTION_ZEN_MODE_PRIORITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        } catch (_: Exception) {
                            context.startActivity(
                                Intent(Settings.ACTION_SOUND_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                    }) {
                        Text("Open Do Not Disturb Settings")
                    }
                }
            }

            item {
                Text("Focus Session History", style = MaterialTheme.typography.titleMedium)
            }

            if (focusSessions.isEmpty()) {
                item {
                    EmptyStatePanel(
                        icon = Icons.Rounded.SelfImprovement,
                        title = "No Completed Sessions",
                        message = "Complete a focus sprint to record your daily focus history."
                    )
                }
            } else {
                items(focusSessions.take(15), key = { it.id }) { s ->
                    RRCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(s.label, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    DateTimeUtils.formatShortDateTime(s.completedAtMillis, preferences.use24HourTime),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text("${s.durationMinutes} min", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(48.dp)) }
        }
    }
}
