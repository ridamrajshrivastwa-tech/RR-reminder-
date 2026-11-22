package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.FocusSessionEntity
import com.example.data.local.ReminderEntity
import com.example.data.local.RoutineEntity
import com.example.ui.components.RRCard
import com.example.ui.components.RRGlassCard
import com.example.ui.components.SubScreenTopBar
import com.example.ui.theme.ErrorCrimson
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SuccessEmerald
import com.example.ui.theme.WarningAmber
import com.example.util.DateTimeUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun StatisticsScreen(
    reminders: List<ReminderEntity>,
    routines: List<RoutineEntity>,
    focusSessions: List<FocusSessionEntity>,
    onBack: () -> Unit
) {
    val now = remember { System.currentTimeMillis() }
    val todayEpoch = remember(now) { DateTimeUtils.epochDayForMillis(now) }

    val totalCount = reminders.size
    val completedCount = reminders.count { it.completed }
    val pendingCount = reminders.count { !it.completed && !it.archived }
    val overdueCount = reminders.count { !it.completed && !it.archived && it.triggerAtMillis < now }
    val completionRate = if (totalCount > 0) (completedCount * 100f / totalCount).roundToInt() else 0

    // Last 7 days completed counts
    val sevenDayBars = remember(reminders, todayEpoch) {
        val sdf = SimpleDateFormat("EEE", Locale.getDefault())
        (6 downTo 0).map { offset ->
            val dayEpoch = todayEpoch - offset
            val dayMillis = DateTimeUtils.millisForEpochDay(dayEpoch)
            val label = sdf.format(dayMillis).take(2)
            val count = reminders.count { r ->
                r.completed && r.completedAtMillis != null &&
                    DateTimeUtils.epochDayForMillis(r.completedAtMillis) == dayEpoch
            }
            label to count
        }
    }

    val weeklyCompleted = remember(reminders, todayEpoch) {
        reminders.count { r ->
            r.completed && r.completedAtMillis != null &&
                DateTimeUtils.epochDayForMillis(r.completedAtMillis) >= todayEpoch - 6
        }
    }
    val monthlyCompleted = remember(reminders, todayEpoch) {
        reminders.count { r ->
            r.completed && r.completedAtMillis != null &&
                DateTimeUtils.epochDayForMillis(r.completedAtMillis) >= todayEpoch - 29
        }
    }

    val categories = remember(reminders) {
        reminders.groupBy { it.category }
            .mapValues { (_, list) -> list.size to list.count { it.completed } }
            .toList()
            .sortedByDescending { it.second.first }
    }

    val totalFocusMinutes = remember(focusSessions) {
        focusSessions.filter { it.sessionType == "WORK" }.sumOf { it.durationMinutes }
    }
    val topStreak = remember(routines) { routines.maxOfOrNull { it.streakCount } ?: 0 }

    Column(modifier = Modifier.fillMaxSize()) {
        SubScreenTopBar(
            title = "Productivity Statistics",
            subtitle = "Completion trends, streaks & focus time",
            onBack = onBack
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                RRGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text("Overall Completion Rate", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "$completionRate%",
                        fontFamily = JetBrainsMonoFontFamily,
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { (completionRate / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "Weekly completed: $weeklyCompleted  •  30-day completed: $monthlyCompleted",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KpiStatCard("Total", totalCount.toString(), MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                    KpiStatCard("Done", completedCount.toString(), SuccessEmerald, Modifier.weight(1f))
                    KpiStatCard("Pending", pendingCount.toString(), WarningAmber, Modifier.weight(1f))
                    KpiStatCard("Overdue", overdueCount.toString(), ErrorCrimson, Modifier.weight(1f))
                }
            }

            // Custom Canvas 7-day productivity bar chart
            item {
                RRCard(modifier = Modifier.fillMaxWidth()) {
                    Text("7-Day Completion Activity", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(12.dp))
                    val barColor = MaterialTheme.colorScheme.primary
                    val trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    val maxBar = (sevenDayBars.maxOfOrNull { it.second } ?: 1).coerceAtLeast(1)

                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                    ) {
                        val slotWidth = size.width / sevenDayBars.size.toFloat()
                        val barWidth = (slotWidth * 0.45f).coerceAtMost(36.dp.toPx())
                        sevenDayBars.forEachIndexed { idx, (_, count) ->
                            val x = idx * slotWidth + (slotWidth - barWidth) / 2f
                            drawRoundRect(
                                color = trackColor,
                                topLeft = Offset(x, 0f),
                                size = Size(barWidth, size.height),
                                cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                            )
                            val fillHeight = (count.toFloat() / maxBar.toFloat()) * size.height
                            if (fillHeight > 0f) {
                                drawRoundRect(
                                    color = barColor,
                                    topLeft = Offset(x, size.height - fillHeight),
                                    size = Size(barWidth, fillHeight),
                                    cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                        sevenDayBars.forEach { (label, count) ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(label, style = MaterialTheme.typography.labelSmall)
                                Text(
                                    text = count.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            // Focus & Routines summary
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    RRCard(modifier = Modifier.weight(1f)) {
                        Text("Focus Minutes", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "${totalFocusMinutes}m",
                            fontFamily = JetBrainsMonoFontFamily,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text("${focusSessions.count { it.sessionType == "WORK" }} sessions", style = MaterialTheme.typography.bodySmall)
                    }
                    RRCard(modifier = Modifier.weight(1f)) {
                        Text("Best Routine Streak", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "$topStreak days",
                            fontFamily = JetBrainsMonoFontFamily,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text("${routines.count { it.completedToday }}/${routines.size} done today", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            // Category Breakdown
            item {
                RRCard(modifier = Modifier.fillMaxWidth()) {
                    Text("Category Breakdown", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(10.dp))
                    if (categories.isEmpty()) {
                        Text(
                            text = "No reminder categories recorded yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        categories.forEach { (category, pair) ->
                            val (catTotal, catDone) = pair
                            val pct = if (catTotal > 0) catDone.toFloat() / catTotal.toFloat() else 0f
                            Column(modifier = Modifier.padding(vertical = 6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(category, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        text = "$catDone / $catTotal",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { pct },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun KpiStatCard(
    label: String,
    value: String,
    accent: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    RRCard(modifier = modifier) {
        Text(
            text = value,
            fontFamily = JetBrainsMonoFontFamily,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = accent
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
