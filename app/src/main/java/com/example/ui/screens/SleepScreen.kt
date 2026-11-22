package com.example.ui.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.WbTwilight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.UserPreferences
import com.example.ui.components.RRCard
import com.example.ui.components.RRGlassCard
import com.example.ui.components.SubScreenTopBar
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SuccessEmerald
import com.example.util.DateTimeUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepScreen(
    preferences: UserPreferences,
    onBack: () -> Unit,
    onUpdateSleepSchedule: (Int, Int, Int, Int, Boolean, Boolean) -> Unit,
    onUpdateChecklist: (List<String>) -> Unit
) {
    var pickingBedtime by remember { mutableStateOf(false) }
    var pickingWake by remember { mutableStateOf(false) }
    var newStepText by remember { mutableStateOf("") }
    var checkedSteps by remember { mutableStateOf(setOf<String>()) }

    val checklist = remember(preferences.bedtimeChecklistCsv) {
        preferences.bedtimeChecklistCsv.split("|").map { it.trim() }.filter { it.isNotEmpty() }
    }

    val bedTotalMins = preferences.bedtimeHour * 60 + preferences.bedtimeMinute
    val wakeTotalMins = preferences.wakeUpHour * 60 + preferences.wakeUpMinute
    val sleepDurationMins = ((wakeTotalMins - bedTotalMins) + 1440) % 1440
    val sleepHours = sleepDurationMins / 60
    val sleepRemMins = sleepDurationMins % 60

    Column(modifier = Modifier.fillMaxSize()) {
        SubScreenTopBar(
            title = "Sleep Mode",
            subtitle = "Planned window: ${sleepHours}h ${sleepRemMins}m",
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
                        .height(150.dp)
                        .clip(RoundedCornerShape(22.dp))
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_sleep_serenity),
                        contentDescription = "Sleep mode serenity illustration",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.72f))))
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp)
                    ) {
                        Text("Rest & Wind-Down Schedule", style = MaterialTheme.typography.titleLarge, color = Color.White)
                        Text(
                            "Consistent bedtime cues and gentle wake-up alarms",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }
            }

            item {
                RRGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Bedtime, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(6.dp))
                                Text("Bedtime", style = MaterialTheme.typography.labelLarge)
                            }
                            Text(
                                text = DateTimeUtils.formatHourMinute(preferences.bedtimeHour, preferences.bedtimeMinute, preferences.use24HourTime),
                                fontFamily = JetBrainsMonoFontFamily,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                            OutlinedButton(onClick = { pickingBedtime = true }) { Text("Change") }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.WbTwilight, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                                Spacer(Modifier.width(6.dp))
                                Text("Wake Up", style = MaterialTheme.typography.labelLarge)
                            }
                            Text(
                                text = DateTimeUtils.formatHourMinute(preferences.wakeUpHour, preferences.wakeUpMinute, preferences.use24HourTime),
                                fontFamily = JetBrainsMonoFontFamily,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                            OutlinedButton(onClick = { pickingWake = true }) { Text("Change") }
                        }
                    }
                }
            }

            item {
                RRCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Enable Sleep Schedule & Wake Alarm", style = MaterialTheme.typography.titleSmall)
                            Text("Schedules daily bedtime reminder and syncs wake-up alarm", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = preferences.sleepReminderEnabled,
                            onCheckedChange = { enabled ->
                                onUpdateSleepSchedule(
                                    preferences.bedtimeHour,
                                    preferences.bedtimeMinute,
                                    preferences.wakeUpHour,
                                    preferences.wakeUpMinute,
                                    enabled,
                                    preferences.gentleWakeAlarmEnabled
                                )
                            }
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Gentle Wake Fade-In", style = MaterialTheme.typography.titleSmall)
                            Text("Gradually ramps alarm volume over 12 seconds", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = preferences.gentleWakeAlarmEnabled,
                            onCheckedChange = { gentle ->
                                onUpdateSleepSchedule(
                                    preferences.bedtimeHour,
                                    preferences.bedtimeMinute,
                                    preferences.wakeUpHour,
                                    preferences.wakeUpMinute,
                                    preferences.sleepReminderEnabled,
                                    gentle
                                )
                            }
                        )
                    }
                }
            }

            item {
                RRCard {
                    Text("Custom Bedtime Routine", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    checklist.forEach { step ->
                        val checked = step in checkedSteps
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = {
                                checkedSteps = if (checked) checkedSteps - step else checkedSteps + step
                            }) {
                                Icon(
                                    imageVector = if (checked) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                                    contentDescription = "Toggle step",
                                    tint = if (checked) SuccessEmerald else MaterialTheme.colorScheme.primary
                                )
                            }
                            Text(step, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                            IconButton(onClick = {
                                onUpdateChecklist(checklist.filterNot { it == step })
                            }) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Remove step", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = newStepText,
                            onValueChange = { newStepText = it },
                            placeholder = { Text("Add bedtime step…") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Button(onClick = {
                            if (newStepText.isNotBlank()) {
                                onUpdateChecklist(checklist + newStepText.trim())
                                newStepText = ""
                            }
                        }) {
                            Icon(Icons.Rounded.Add, contentDescription = "Add step")
                        }
                    }
                }
            }

            item {
                RRCard {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(Icons.Rounded.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "Note: Sleep Mode tracks your planned sleep schedule, wind-down checklist, and gentle wake alarm. It does not measure physiological sleep stages without connected health integrations.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(48.dp)) }
        }
    }

    if (pickingBedtime || pickingWake) {
        val initH = if (pickingBedtime) preferences.bedtimeHour else preferences.wakeUpHour
        val initM = if (pickingBedtime) preferences.bedtimeMinute else preferences.wakeUpMinute
        val tpState = rememberTimePickerState(initH, initM, preferences.use24HourTime)
        AlertDialog(
            onDismissRequest = { pickingBedtime = false; pickingWake = false },
            title = { Text(if (pickingBedtime) "Set Bedtime" else "Set Wake-Up Time") },
            text = { TimePicker(state = tpState) },
            confirmButton = {
                TextButton(onClick = {
                    if (pickingBedtime) {
                        onUpdateSleepSchedule(
                            tpState.hour,
                            tpState.minute,
                            preferences.wakeUpHour,
                            preferences.wakeUpMinute,
                            preferences.sleepReminderEnabled,
                            preferences.gentleWakeAlarmEnabled
                        )
                    } else {
                        onUpdateSleepSchedule(
                            preferences.bedtimeHour,
                            preferences.bedtimeMinute,
                            tpState.hour,
                            tpState.minute,
                            preferences.sleepReminderEnabled,
                            preferences.gentleWakeAlarmEnabled
                        )
                    }
                    pickingBedtime = false
                    pickingWake = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { pickingBedtime = false; pickingWake = false }) { Text("Cancel") }
            }
        )
    }
}
