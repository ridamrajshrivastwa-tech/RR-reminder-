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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.firebase.AiParsedReminderDraft
import com.example.data.local.AlarmEntity
import com.example.data.local.CalendarEventEntity
import com.example.data.local.ReminderEntity
import com.example.data.local.RoutineEntity
import com.example.ui.components.PriorityBadge
import com.example.ui.components.RRCard
import com.example.ui.components.RRGlassCard
import com.example.ui.components.SubScreenTopBar
import com.example.ui.viewmodel.AiInsightUiState
import com.example.ui.viewmodel.AiParseUiState
import com.example.util.DateTimeUtils
import java.util.Calendar

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiAssistantScreen(
    parseState: AiParseUiState,
    insightState: AiInsightUiState,
    use24HourTime: Boolean,
    reminders: List<ReminderEntity>,
    alarms: List<AlarmEntity>,
    events: List<CalendarEventEntity>,
    routines: List<RoutineEntity>,
    onBack: () -> Unit,
    onParseReminder: (String) -> Unit,
    onConfirmCreateReminder: (AiParsedReminderDraft, Long) -> Unit,
    onDismissDraft: () -> Unit,
    onRunProductivityPrompt: (String) -> Unit
) {
    var naturalInput by remember { mutableStateOf("") }
    var assistantPrompt by remember { mutableStateOf("") }
    var createdConfirmation by remember { mutableStateOf<String?>(null) }

    val scheduleSummaryContext = remember(reminders, alarms, events, routines) {
        val pending = reminders.filter { !it.completed && !it.archived }.take(12)
        val pendingLines = pending.joinToString("\n") {
            "- ${it.title} [${it.priority}, ${it.category}] at ${DateTimeUtils.formatShortDateTime(it.triggerAtMillis, use24HourTime)}"
        }
        val activeAlarms = alarms.filter { it.enabled }.joinToString(", ") {
            "${it.label} (${DateTimeUtils.formatHourMinute(it.hour, it.minute, use24HourTime)})"
        }
        val todayRoutines = routines.joinToString(", ") { "${it.title} (${if (it.completedToday) "done" else "pending"})" }
        """
        User Schedule Context:
        Pending reminders (${pending.size}):
        ${pendingLines.ifBlank { "None" }}
        Active alarms: ${activeAlarms.ifBlank { "None" }}
        Upcoming events count: ${events.size}
        Daily routines: ${todayRoutines.ifBlank { "None" }}
        """.trimIndent()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        SubScreenTopBar(
            title = "AI Smart Assistant",
            subtitle = "Natural language parsing & schedule insights",
            onBack = onBack
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 1: Natural Language Reminder Parser
            item {
                RRGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Natural Language Reminder Creator", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Type a sentence or paste quick notes. AI drafts the date, time, priority, and repeat cycle for you to confirm.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))

                    OutlinedTextField(
                        value = naturalInput,
                        onValueChange = {
                            naturalInput = it
                            createdConfirmation = null
                        },
                        label = { Text("e.g., Remind me to study mathematics tomorrow at 6 PM") },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ai_parse_input")
                    )

                    Spacer(Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(
                            "Study mathematics tomorrow at 6 PM",
                            "Pay rent on the 1st monthly at 9 AM",
                            "Morning vitamins daily at 8:30 AM"
                        ).forEach { suggestion ->
                            AssistChip(
                                onClick = { naturalInput = suggestion },
                                label = { Text(suggestion, maxLines = 1) }
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = { onParseReminder(naturalInput) },
                        enabled = naturalInput.isNotBlank() && parseState !is AiParseUiState.Loading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ai_parse_submit_btn"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        if (parseState is AiParseUiState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Parsing Request…")
                        } else {
                            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Draft Reminder with AI")
                        }
                    }

                    if (!createdConfirmation.isNullOrBlank()) {
                        Spacer(Modifier.height(10.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = createdConfirmation.orEmpty(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }

            // Draft Confirmation Card (requires explicit user confirmation before writing)
            when (parseState) {
                is AiParseUiState.DraftReady -> {
                    item {
                        val draft = parseState.draft
                        val triggerMillis = remember(draft) {
                            Calendar.getInstance().apply {
                                add(Calendar.DAY_OF_YEAR, draft.daysFromToday)
                                set(Calendar.HOUR_OF_DAY, draft.hour24)
                                set(Calendar.MINUTE, draft.minute)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }.timeInMillis
                        }

                        RRCard(
                            modifier = Modifier.fillMaxWidth(),
                            borderColor = MaterialTheme.colorScheme.primary
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Review AI Reminder Draft", style = MaterialTheme.typography.titleMedium)
                                PriorityBadge(draft.priority)
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = draft.title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            if (draft.description.isNotBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = draft.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "Scheduled: ${DateTimeUtils.formatShortDateTime(triggerMillis, use24HourTime)} • Repeat: ${draft.repeatInterval} • Category: ${draft.category}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = draft.rationale,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedButton(
                                    onClick = onDismissDraft,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Rounded.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Discard")
                                }
                                Button(
                                    onClick = {
                                        onConfirmCreateReminder(draft, triggerMillis)
                                        createdConfirmation = "Reminder \"${draft.title}\" scheduled!"
                                        naturalInput = ""
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("ai_confirm_create_btn")
                                ) {
                                    Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Confirm & Save")
                                }
                            }
                        }
                    }
                }
                is AiParseUiState.Error -> {
                    item {
                        RRCard(borderColor = MaterialTheme.colorScheme.error) {
                            Text(
                                text = parseState.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
                else -> Unit
            }

            // Section 2: Productivity Insights & Task Organizer
            item {
                RRCard(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.Lightbulb,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Productivity Briefing & Organizer", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "Daily Briefing" to "Give me a concise, actionable daily productivity briefing and top 3 priorities based on my current schedule.",
                            "Organize Tasks" to "Organize my pending reminders into Morning, Afternoon, and Evening time blocks with suggested focus tips.",
                            "Focus Plan" to "Recommend an optimal Pomodoro focus & break plan for completing my highest-priority tasks today."
                        ).forEach { (label, promptBody) ->
                            FilledTonalButton(
                                onClick = {
                                    onRunProductivityPrompt("$scheduleSummaryContext\n\nTask: $promptBody")
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(label)
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = assistantPrompt,
                            onValueChange = { assistantPrompt = it },
                            label = { Text("Ask about your schedule or convert notes…") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("ai_insight_input")
                        )
                        Button(
                            onClick = {
                                if (assistantPrompt.isNotBlank()) {
                                    onRunProductivityPrompt("$scheduleSummaryContext\n\nUser Question: $assistantPrompt")
                                }
                            },
                            enabled = assistantPrompt.isNotBlank() && insightState !is AiInsightUiState.Loading,
                            modifier = Modifier.testTag("ai_insight_send_btn")
                        ) {
                            Icon(Icons.Rounded.Send, contentDescription = "Send prompt")
                        }
                    }
                }
            }

            when (insightState) {
                is AiInsightUiState.Loading -> {
                    item {
                        RRCard {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(12.dp))
                                Text("Analyzing your schedule…", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
                is AiInsightUiState.Streaming -> {
                    item {
                        RRCard(borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)) {
                            Text("AI Assistant (Streaming…)", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(6.dp))
                            Text(insightState.text, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                is AiInsightUiState.Success -> {
                    item {
                        RRCard {
                            Text("AI Assistant Response", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(6.dp))
                            Text(insightState.text, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                is AiInsightUiState.Error -> {
                    item {
                        RRCard(borderColor = MaterialTheme.colorScheme.error) {
                            Text(
                                text = insightState.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
                AiInsightUiState.Idle -> Unit
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}
