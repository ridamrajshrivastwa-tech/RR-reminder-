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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.data.local.ReminderEntity
import com.example.data.local.ReminderPriority
import com.example.data.local.RoutineEntity
import com.example.data.local.RoutinePeriod
import com.example.ui.components.EmptyStatePanel
import com.example.ui.components.PriorityBadge
import com.example.ui.components.RRCard
import com.example.ui.components.RRGlassCard
import com.example.ui.components.SubScreenTopBar
import com.example.ui.theme.ErrorCrimson
import com.example.ui.theme.SuccessEmerald
import com.example.ui.theme.WarningAmber
import com.example.util.DateTimeUtils

@Composable
fun RoutinesScreen(
    routines: List<RoutineEntity>,
    overdueReminders: List<ReminderEntity>,
    use24HourTime: Boolean,
    onBack: () -> Unit,
    onAddRoutine: (String, String, ReminderPriority, Boolean, Int) -> Unit,
    onToggleRoutine: (RoutineEntity) -> Unit,
    onDeleteRoutine: (String) -> Unit,
    onCompleteOverdueReminder: (ReminderEntity) -> Unit,
    onSnoozeOverdueReminder: (ReminderEntity, Int) -> Unit
) {
    var selectedPeriod by remember { mutableStateOf("ALL") }
    var showAddDialog by remember { mutableStateOf(false) }

    val filteredRoutines = remember(routines, selectedPeriod) {
        if (selectedPeriod == "ALL") routines else routines.filter { it.period == selectedPeriod }
    }
    val doneCount = routines.count { it.completedToday }
    val ratio = if (routines.isNotEmpty()) doneCount.toFloat() / routines.size.toFloat() else 0f

    Column(modifier = Modifier.fillMaxSize()) {
        SubScreenTopBar(
            title = "Tasks & Routines",
            subtitle = "$doneCount of ${routines.size} completed today",
            onBack = onBack,
            trailingAction = {
                Button(
                    onClick = { showAddDialog = true },
                    modifier = Modifier.testTag("routine_add_btn")
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Routine")
                }
            }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                RRGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text("Daily Routine Progress", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(progress = { ratio }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "${(ratio * 100).toInt()}% completed • Morning, Evening & Custom habits",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (overdueReminders.isNotEmpty()) {
                item {
                    Text(
                        "Overdue Tasks (${overdueReminders.size})",
                        style = MaterialTheme.typography.titleMedium,
                        color = ErrorCrimson
                    )
                }
                items(overdueReminders.take(5), key = { "ov_${it.id}" }) { rem ->
                    RRCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(rem.title, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    DateTimeUtils.formatShortDateTime(rem.triggerAtMillis, use24HourTime),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ErrorCrimson
                                )
                            }
                            TextButton(onClick = { onSnoozeOverdueReminder(rem, 60) }) { Text("+1h") }
                            Button(onClick = { onCompleteOverdueReminder(rem) }) { Text("Done") }
                        }
                    }
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("ALL" to "All", "MORNING" to "Morning", "EVENING" to "Evening", "CUSTOM" to "Custom").forEach { (key, label) ->
                        FilterChip(
                            selected = selectedPeriod == key,
                            onClick = { selectedPeriod = key },
                            label = { Text(label) }
                        )
                    }
                }
            }

            if (filteredRoutines.isEmpty()) {
                item {
                    EmptyStatePanel(
                        icon = Icons.Rounded.Checklist,
                        title = "No Routines Added",
                        message = "Build Morning, Evening, or Custom daily routines to track habits and streaks.",
                        actionButton = {
                            Button(onClick = { showAddDialog = true }) { Text("Create Routine") }
                        }
                    )
                }
            } else {
                items(filteredRoutines, key = { it.id }) { rt ->
                    RRCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { onToggleRoutine(rt) }) {
                                Icon(
                                    imageVector = if (rt.completedToday) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                                    contentDescription = "Complete routine",
                                    tint = if (rt.completedToday) SuccessEmerald else MaterialTheme.colorScheme.primary
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = rt.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    textDecoration = if (rt.completedToday) TextDecoration.LineThrough else null
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${rt.period.lowercase().replaceFirstChar { it.uppercase() }} • ${DateTimeUtils.formatMinutesOfDay(rt.scheduledMinutesOfDay, use24HourTime)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (rt.streakCount > 0) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Rounded.LocalFireDepartment, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(14.dp))
                                            Text("${rt.streakCount}d", style = MaterialTheme.typography.labelSmall, color = WarningAmber)
                                        }
                                    }
                                }
                            }
                            PriorityBadge(rt.priority)
                            IconButton(onClick = { onDeleteRoutine(rt.id) }) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete routine", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(48.dp)) }
        }
    }

    if (showAddDialog) {
        AddRoutineDialog(
            onDismiss = { showAddDialog = false },
            onSave = { title, period, priority, recurring, mins ->
                onAddRoutine(title, period, priority, recurring, mins)
                showAddDialog = false
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddRoutineDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, ReminderPriority, Boolean, Int) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var period by remember { mutableStateOf(RoutinePeriod.MORNING) }
    var priority by remember { mutableStateOf(ReminderPriority.MEDIUM) }
    var recurringDaily by remember { mutableStateOf(true) }
    var scheduledMinutes by remember { mutableIntStateOf(450) } // 07:30

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Routine / Daily Task") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Routine Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Routine Type", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    RoutinePeriod.entries.forEach { p ->
                        FilterChip(
                            selected = period == p,
                            onClick = {
                                period = p
                                scheduledMinutes = when (p) {
                                    RoutinePeriod.MORNING -> 450
                                    RoutinePeriod.EVENING -> 1260
                                    RoutinePeriod.CUSTOM -> 720
                                }
                            },
                            label = { Text(p.label) }
                        )
                    }
                }
                Text("Priority", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ReminderPriority.entries.forEach { pr ->
                        FilterChip(
                            selected = priority == pr,
                            onClick = { priority = pr },
                            label = { Text(pr.label) }
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Recurring daily")
                    Switch(checked = recurringDaily, onCheckedChange = { recurringDaily = it })
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (title.isNotBlank()) {
                    onSave(title, period.name, priority, recurringDaily, scheduledMinutes)
                }
            }) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
