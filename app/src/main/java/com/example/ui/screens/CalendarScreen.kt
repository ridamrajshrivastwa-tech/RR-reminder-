package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.local.CalendarEventEntity
import com.example.data.local.ReminderEntity
import com.example.ui.components.EmptyStatePanel
import com.example.ui.components.RRCard
import com.example.ui.components.SubScreenTopBar
import com.example.ui.theme.ReminderColorOptions
import com.example.util.DateTimeUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun CalendarScreen(
    reminders: List<ReminderEntity>,
    events: List<CalendarEventEntity>,
    use24HourTime: Boolean,
    firstDayOfWeek: Int,
    onBack: () -> Unit,
    onSaveEvent: (String?, String, String, Long, Int, Int, Int) -> Unit,
    onDeleteEvent: (String) -> Unit
) {
    val todayEpoch = remember { DateTimeUtils.epochDayForMillis(System.currentTimeMillis()) }
    var selectedEpochDay by remember { mutableLongStateOf(todayEpoch) }
    var monthOffset by remember { mutableIntStateOf(0) }
    var weeklyView by remember { mutableStateOf(false) }
    var editingEvent by remember { mutableStateOf<CalendarEventEntity?>(null) }
    var showCreateEvent by remember { mutableStateOf(false) }

    val displayCal = remember(monthOffset) {
        Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            add(Calendar.MONTH, monthOffset)
        }
    }
    val monthTitle = remember(monthOffset) {
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(displayCal.time)
    }

    val reminderDays = remember(reminders) {
        reminders.filter { !it.archived }.map { DateTimeUtils.epochDayForMillis(it.triggerAtMillis) }.toSet()
    }
    val eventDays = remember(events) {
        events.map { it.dateEpochDay }.toSet()
    }

    val dayEvents = remember(events, selectedEpochDay) {
        events.filter { it.dateEpochDay == selectedEpochDay }.sortedBy { it.startMinutes }
    }
    val dayReminders = remember(reminders, selectedEpochDay) {
        reminders.filter {
            !it.archived && DateTimeUtils.epochDayForMillis(it.triggerAtMillis) == selectedEpochDay
        }.sortedBy { it.triggerAtMillis }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        SubScreenTopBar(
            title = "Calendar",
            subtitle = monthTitle,
            onBack = onBack,
            trailingAction = {
                Button(
                    onClick = { showCreateEvent = true },
                    modifier = Modifier.testTag("calendar_add_event_btn")
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Event")
                }
            }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(selected = !weeklyView, onClick = { weeklyView = false }, label = { Text("Month") })
                        FilterChip(selected = weeklyView, onClick = { weeklyView = true }, label = { Text("Week") })
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { monthOffset-- }) {
                            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "Previous month")
                        }
                        TextButton(onClick = {
                            monthOffset = 0
                            selectedEpochDay = todayEpoch
                        }) {
                            Text("Today")
                        }
                        IconButton(onClick = { monthOffset++ }) {
                            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "Next month")
                        }
                    }
                }
            }

            item {
                RRCard {
                    val orderedHeaders = remember(firstDayOfWeek) {
                        (0..6).map { offset ->
                            val dow = ((firstDayOfWeek - 1 + offset) % 7) + 1
                            DateTimeUtils.weekdayShortName(dow)
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth()) {
                        orderedHeaders.forEach { label ->
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))

                    if (weeklyView) {
                        val weekStart = selectedEpochDay - 3
                        Row(modifier = Modifier.fillMaxWidth()) {
                            for (d in 0..6) {
                                val epoch = weekStart + d
                                val cal = Calendar.getInstance().apply {
                                    timeInMillis = DateTimeUtils.millisForEpochDay(epoch)
                                }
                                DayCell(
                                    dayNumber = cal.get(Calendar.DAY_OF_MONTH),
                                    isSelected = epoch == selectedEpochDay,
                                    isToday = epoch == todayEpoch,
                                    hasMarker = epoch in reminderDays || epoch in eventDays,
                                    onClick = { selectedEpochDay = epoch },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    } else {
                        val monthCal = (displayCal.clone() as Calendar)
                        val daysInMonth = monthCal.getActualMaximum(Calendar.DAY_OF_MONTH)
                        val firstDow = monthCal.get(Calendar.DAY_OF_WEEK)
                        val leadingEmpty = ((firstDow - firstDayOfWeek) + 7) % 7
                        val totalCells = ((leadingEmpty + daysInMonth + 6) / 7) * 7

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            for (rowStart in 0 until totalCells step 7) {
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    for (col in 0..6) {
                                        val cellIdx = rowStart + col
                                        val dayNum = cellIdx - leadingEmpty + 1
                                        if (dayNum in 1..daysInMonth) {
                                            val cellCal = (monthCal.clone() as Calendar).apply {
                                                set(Calendar.DAY_OF_MONTH, dayNum)
                                            }
                                            val epoch = DateTimeUtils.epochDayForMillis(cellCal.timeInMillis)
                                            DayCell(
                                                dayNumber = dayNum,
                                                isSelected = epoch == selectedEpochDay,
                                                isToday = epoch == todayEpoch,
                                                hasMarker = epoch in reminderDays || epoch in eventDays,
                                                onClick = { selectedEpochDay = epoch },
                                                modifier = Modifier.weight(1f)
                                            )
                                        } else {
                                            Spacer(Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                val selMillis = DateTimeUtils.millisForEpochDay(selectedEpochDay)
                Text(
                    text = "Schedule • ${DateTimeUtils.formatRelativeDateLabel(selMillis)} (${DateTimeUtils.formatFullDate(selMillis)})",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            if (dayEvents.isEmpty() && dayReminders.isEmpty()) {
                item {
                    EmptyStatePanel(
                        icon = Icons.Rounded.Event,
                        title = "Nothing Scheduled",
                        message = "No events or reminders on this date. Tap + Event to block time."
                    )
                }
            } else {
                items(dayEvents, key = { "ev_${it.id}" }) { ev ->
                    RRCard(onClick = { editingEvent = ev }) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(5.dp)
                                    .height(40.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(ev.colorHex))
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(ev.title, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    text = "${DateTimeUtils.formatMinutesOfDay(ev.startMinutes, use24HourTime)} - ${DateTimeUtils.formatMinutesOfDay(ev.endMinutes, use24HourTime)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                if (ev.notes.isNotBlank()) {
                                    Text(ev.notes, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            IconButton(onClick = { onDeleteEvent(ev.id) }) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete event", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }

                items(dayReminders, key = { "rem_${it.id}" }) { rem ->
                    RRCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(Color(rem.colorHex))
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Reminder: ${rem.title}", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    text = "${DateTimeUtils.formatShortDateTime(rem.triggerAtMillis, use24HourTime)} • ${rem.priority}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(48.dp)) }
        }
    }

    if (showCreateEvent || editingEvent != null) {
        EventEditorDialog(
            initialEvent = editingEvent,
            defaultEpochDay = selectedEpochDay,
            use24HourTime = use24HourTime,
            onDismiss = {
                showCreateEvent = false
                editingEvent = null
            },
            onSave = { title, notes, startMins, endMins, colorHex ->
                onSaveEvent(
                    editingEvent?.id,
                    title,
                    notes,
                    editingEvent?.dateEpochDay ?: selectedEpochDay,
                    startMins,
                    endMins,
                    colorHex
                )
                showCreateEvent = false
                editingEvent = null
            }
        )
    }
}

@Composable
private fun DayCell(
    dayNumber: Int,
    isSelected: Boolean,
    isToday: Boolean,
    hasMarker: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(CircleShape)
            .background(
                when {
                    isSelected -> MaterialTheme.colorScheme.primary
                    isToday -> MaterialTheme.colorScheme.primaryContainer
                    else -> Color.Transparent
                }
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = dayNumber.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
            )
            if (hasMarker) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.secondary)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventEditorDialog(
    initialEvent: CalendarEventEntity?,
    defaultEpochDay: Long,
    use24HourTime: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String, Int, Int, Int) -> Unit
) {
    var title by remember { mutableStateOf(initialEvent?.title ?: "") }
    var notes by remember { mutableStateOf(initialEvent?.notes ?: "") }
    var startMinutes by remember { mutableIntStateOf(initialEvent?.startMinutes ?: 540) }
    var endMinutes by remember { mutableIntStateOf(initialEvent?.endMinutes ?: 600) }
    var colorHex by remember { mutableIntStateOf(initialEvent?.colorHex ?: ReminderColorOptions.first()) }
    var pickingStart by remember { mutableStateOf(false) }
    var pickingEnd by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialEvent == null) "New Event" else "Edit Event") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Event Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { pickingStart = true }, modifier = Modifier.weight(1f)) {
                        Text("Start: ${DateTimeUtils.formatMinutesOfDay(startMinutes, use24HourTime)}")
                    }
                    OutlinedButton(onClick = { pickingEnd = true }, modifier = Modifier.weight(1f)) {
                        Text("End: ${DateTimeUtils.formatMinutesOfDay(endMinutes, use24HourTime)}")
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ReminderColorOptions.forEach { argb ->
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(argb))
                                .then(
                                    if (colorHex == argb) Modifier.border(2.5.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    else Modifier
                                )
                                .clickable { colorHex = argb }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onSave(title, notes, startMinutes, endMinutes.coerceAtLeast(startMinutes), colorHex)
                    }
                }
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )

    if (pickingStart || pickingEnd) {
        val currentMins = if (pickingStart) startMinutes else endMinutes
        val tpState = rememberTimePickerState(currentMins / 60, currentMins % 60, use24HourTime)
        AlertDialog(
            onDismissRequest = { pickingStart = false; pickingEnd = false },
            title = { Text(if (pickingStart) "Start Time" else "End Time") },
            text = { TimePicker(state = tpState) },
            confirmButton = {
                TextButton(onClick = {
                    val total = tpState.hour * 60 + tpState.minute
                    if (pickingStart) {
                        startMinutes = total
                        if (endMinutes < startMinutes) endMinutes = (startMinutes + 30).coerceAtMost(1439)
                    } else {
                        endMinutes = total.coerceAtLeast(startMinutes)
                    }
                    pickingStart = false
                    pickingEnd = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { pickingStart = false; pickingEnd = false }) { Text("Cancel") }
            }
        )
    }
}
