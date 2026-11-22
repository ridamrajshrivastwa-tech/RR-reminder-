package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Snooze
import androidx.compose.material.icons.rounded.Unarchive
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.ReminderEntity
import com.example.data.local.RepeatInterval
import com.example.ui.components.EmptyStatePanel
import com.example.ui.components.PriorityBadge
import com.example.ui.components.RRCard
import com.example.ui.theme.ErrorCrimson
import com.example.ui.theme.SuccessEmerald
import com.example.util.DateTimeUtils

enum class ReminderFilter(val label: String) {
    ACTIVE("Active"),
    TODAY("Today"),
    OVERDUE("Overdue"),
    COMPLETED("Completed"),
    ARCHIVED("Archived"),
    ALL("All")
}

@Composable
fun RemindersScreen(
    reminders: List<ReminderEntity>,
    nowMillis: Long,
    use24HourTime: Boolean,
    onCreateNew: () -> Unit,
    onEditReminder: (ReminderEntity) -> Unit,
    onToggleComplete: (ReminderEntity) -> Unit,
    onSnooze: (ReminderEntity, Int) -> Unit,
    onToggleArchive: (ReminderEntity) -> Unit,
    onDelete: (String) -> Unit,
    onOpenAiAssistant: () -> Unit,
    onOpenNotificationHistory: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var activeFilter by remember { mutableStateOf(ReminderFilter.ACTIVE) }
    var selectedCategory by remember { mutableStateOf("All") }

    val categories = remember(reminders) {
        listOf("All") + reminders.map { it.category }.distinct().sorted()
    }

    val filtered = remember(reminders, searchQuery, activeFilter, selectedCategory, nowMillis) {
        reminders.filter { r ->
            val matchesQuery = searchQuery.isBlank() ||
                r.title.contains(searchQuery, ignoreCase = true) ||
                r.description.contains(searchQuery, ignoreCase = true) ||
                r.category.contains(searchQuery, ignoreCase = true)
            val matchesCat = selectedCategory == "All" || r.category == selectedCategory
            val effectiveTrigger = if (r.snoozedUntilMillis > 0L) r.snoozedUntilMillis else r.triggerAtMillis
            val matchesFilter = when (activeFilter) {
                ReminderFilter.ACTIVE -> !r.completed && !r.archived
                ReminderFilter.TODAY -> !r.archived && DateTimeUtils.isSameLocalDay(effectiveTrigger, nowMillis)
                ReminderFilter.OVERDUE -> !r.completed && !r.archived && effectiveTrigger < nowMillis
                ReminderFilter.COMPLETED -> r.completed && !r.archived
                ReminderFilter.ARCHIVED -> r.archived
                ReminderFilter.ALL -> true
            }
            matchesQuery && matchesCat && matchesFilter
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Reminders", style = MaterialTheme.typography.headlineLarge)
                Text(
                    text = "${reminders.count { !it.completed && !it.archived }} active • ${reminders.count { it.completed }} completed",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = onOpenAiAssistant,
                modifier = Modifier.testTag("reminders_ai_btn")
            ) {
                Icon(Icons.Rounded.AutoAwesome, contentDescription = "AI Reminder Creator", tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(
                onClick = onOpenNotificationHistory,
                modifier = Modifier.testTag("reminders_history_btn")
            ) {
                Icon(Icons.Rounded.History, contentDescription = "Notification History")
            }
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search reminders, notes, categories…") },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .testTag("reminders_search_input")
        )

        Spacer(Modifier.height(10.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(ReminderFilter.entries) { filter ->
                FilterChip(
                    selected = activeFilter == filter,
                    onClick = { activeFilter = filter },
                    label = { Text(filter.label) }
                )
            }
        }

        if (categories.size > 2) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat) }
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (filtered.isEmpty()) {
                item {
                    EmptyStatePanel(
                        icon = Icons.Rounded.NotificationsActive,
                        title = "No matching reminders",
                        message = "Tap + to create a reminder with recurrence, priority, and custom alerts.",
                        actionButton = {
                            androidx.compose.material3.Button(onClick = onCreateNew) {
                                Text("Create Reminder")
                            }
                        }
                    )
                }
            } else {
                items(filtered, key = { it.id }) { reminder ->
                    ReminderRowCard(
                        reminder = reminder,
                        nowMillis = nowMillis,
                        use24HourTime = use24HourTime,
                        onToggleComplete = { onToggleComplete(reminder) },
                        onEdit = { onEditReminder(reminder) },
                        onSnooze = { mins -> onSnooze(reminder, mins) },
                        onToggleArchive = { onToggleArchive(reminder) },
                        onDelete = { onDelete(reminder.id) }
                    )
                }
            }
            item { Spacer(Modifier.height(72.dp)) }
        }
    }
}

@Composable
private fun ReminderRowCard(
    reminder: ReminderEntity,
    nowMillis: Long,
    use24HourTime: Boolean,
    onToggleComplete: () -> Unit,
    onEdit: () -> Unit,
    onSnooze: (Int) -> Unit,
    onToggleArchive: () -> Unit,
    onDelete: () -> Unit
) {
    var showSnoozeMenu by remember { mutableStateOf(false) }
    val effectiveTrigger = if (reminder.snoozedUntilMillis > 0L) reminder.snoozedUntilMillis else reminder.triggerAtMillis
    val isOverdue = !reminder.completed && !reminder.archived && effectiveTrigger < nowMillis

    RRCard(onClick = onEdit) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .height(54.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(reminder.colorHex))
            )
            Spacer(Modifier.width(10.dp))
            IconButton(
                onClick = onToggleComplete,
                modifier = Modifier.testTag("reminder_complete_toggle_${reminder.id}")
            ) {
                Icon(
                    imageVector = if (reminder.completed) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                    contentDescription = "Mark reminder completed",
                    tint = if (reminder.completed) SuccessEmerald else MaterialTheme.colorScheme.primary
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = reminder.title,
                        style = MaterialTheme.typography.titleMedium,
                        textDecoration = if (reminder.completed) TextDecoration.LineThrough else null,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    PriorityBadge(reminder.priority)
                }

                if (reminder.description.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = reminder.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = DateTimeUtils.formatShortDateTime(effectiveTrigger, use24HourTime),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isOverdue) ErrorCrimson else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = reminder.category,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (reminder.repeatInterval != RepeatInterval.NONE.name) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Repeat, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.secondary)
                            Spacer(Modifier.width(3.dp))
                            Text(
                                text = reminder.repeatInterval.lowercase().replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                    if (!reminder.attachmentName.isNullOrBlank()) {
                        Icon(
                            Icons.Rounded.AttachFile,
                            contentDescription = "Has attachment",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Box {
                IconButton(onClick = { showSnoozeMenu = true }) {
                    Icon(Icons.Rounded.Snooze, contentDescription = "Snooze reminder")
                }
                DropdownMenu(expanded = showSnoozeMenu, onDismissRequest = { showSnoozeMenu = false }) {
                    DropdownMenuItem(text = { Text("Snooze 10 minutes") }, onClick = { showSnoozeMenu = false; onSnooze(10) })
                    DropdownMenuItem(text = { Text("Snooze 30 minutes") }, onClick = { showSnoozeMenu = false; onSnooze(30) })
                    DropdownMenuItem(text = { Text("Snooze 1 hour") }, onClick = { showSnoozeMenu = false; onSnooze(60) })
                    DropdownMenuItem(text = { Text("Snooze 3 hours") }, onClick = { showSnoozeMenu = false; onSnooze(180) })
                }
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Rounded.Edit, contentDescription = "Edit reminder")
            }
            IconButton(onClick = onToggleArchive) {
                Icon(
                    imageVector = if (reminder.archived) Icons.Rounded.Unarchive else Icons.Rounded.Archive,
                    contentDescription = if (reminder.archived) "Unarchive" else "Archive"
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete reminder", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
