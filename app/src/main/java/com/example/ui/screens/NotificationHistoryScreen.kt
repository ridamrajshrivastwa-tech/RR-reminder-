package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.local.NotificationLogEntity
import com.example.notifications.NotificationHelper
import com.example.ui.components.EmptyStatePanel
import com.example.ui.components.RRCard
import com.example.ui.components.SubScreenTopBar
import com.example.ui.theme.ErrorCrimson
import com.example.ui.theme.SuccessEmerald
import com.example.ui.theme.WarningAmber
import com.example.util.DateTimeUtils

@Composable
fun NotificationHistoryScreen(
    logs: List<NotificationLogEntity>,
    use24HourTime: Boolean,
    onBack: () -> Unit,
    onClearLogs: () -> Unit
) {
    val context = LocalContext.current
    var hasNotifPerm by remember { mutableStateOf(NotificationHelper.hasNotificationPermission(context)) }
    val exactAlarmAllowed = NotificationHelper.canScheduleExactAlarms(context)
    val batteryUnrestricted = NotificationHelper.isIgnoringBatteryOptimizations(context)

    val notifPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotifPerm = granted || NotificationHelper.hasNotificationPermission(context)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        SubScreenTopBar(
            title = "Notifications & Reliability",
            subtitle = "Alert history & Android permission checks",
            onBack = onBack,
            trailingAction = {
                if (logs.isNotEmpty()) {
                    IconButton(
                        onClick = onClearLogs,
                        modifier = Modifier.testTag("clear_notif_logs_btn")
                    ) {
                        Icon(Icons.Rounded.DeleteSweep, contentDescription = "Clear notification history")
                    }
                }
            }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                RRCard(modifier = Modifier.fillMaxWidth()) {
                    Text("System Reliability Status", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))

                    // 1. Notification permission
                    StatusRow(
                        ok = hasNotifPerm,
                        title = if (hasNotifPerm) "Notifications Permitted" else "Notifications Blocked",
                        detail = if (hasNotifPerm) "Reminders and alarms can post alerts."
                        else "Enable notification permission so scheduled alerts appear.",
                        actionLabel = if (!hasNotifPerm) "Grant" else null,
                        onAction = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                NotificationHelper.openNotificationSettings(context)
                            }
                        }
                    )

                    Spacer(Modifier.height(10.dp))

                    // 2. Exact alarms
                    StatusRow(
                        ok = exactAlarmAllowed,
                        title = if (exactAlarmAllowed) "Exact Alarm Timing Active" else "Exact Alarms Restricted",
                        detail = if (exactAlarmAllowed) "Alarms and reminders trigger on exact minute."
                        else "Android is limiting exact timing; fallback scheduling may drift during Doze mode.",
                        actionLabel = if (!exactAlarmAllowed) "Settings" else null,
                        onAction = { NotificationHelper.openExactAlarmSettings(context) }
                    )

                    Spacer(Modifier.height(10.dp))

                    // 3. Battery optimization transparency
                    StatusRow(
                        ok = batteryUnrestricted,
                        title = if (batteryUnrestricted) "Battery Optimization Unrestricted" else "Battery Optimization Active",
                        detail = if (batteryUnrestricted) "Background execution is not throttled by standard battery optimization."
                        else "Aggressive Android or OEM battery savers may delay background alerts. Even with permissions granted, OS power policies govern execution.",
                        actionLabel = "Battery",
                        onAction = { NotificationHelper.openBatterySettings(context) }
                    )
                }
            }

            item {
                Text("Notification Activity Log (${logs.size})", style = MaterialTheme.typography.titleMedium)
            }

            if (logs.isEmpty()) {
                item {
                    EmptyStatePanel(
                        icon = Icons.Rounded.NotificationsOff,
                        title = "No Notification Events Yet",
                        message = "Triggered, snoozed, completed, and missed reminder alerts will appear here."
                    )
                }
            } else {
                items(logs, key = { it.id }) { log ->
                    val statusColor = when (log.status) {
                        "COMPLETED" -> SuccessEmerald
                        "SNOOZED" -> WarningAmber
                        "MISSED", "DISMISSED" -> ErrorCrimson
                        else -> MaterialTheme.colorScheme.primary
                    }
                    RRCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(log.title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                            Surface(
                                color = statusColor.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(999.dp)
                            ) {
                                Text(
                                    text = log.status,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = statusColor,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = log.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "${log.sourceType} • ${DateTimeUtils.formatShortDateTime(log.timestampMillis, use24HourTime)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun StatusRow(
    ok: Boolean,
    title: String,
    detail: String,
    actionLabel: String?,
    onAction: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (ok) Icons.Rounded.CheckCircle else Icons.Rounded.Warning,
            contentDescription = null,
            tint = if (ok) SuccessEmerald else WarningAmber
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (actionLabel != null) {
            Spacer(Modifier.width(8.dp))
            OutlinedButton(onClick = onAction) {
                Text(actionLabel)
            }
        }
    }
}
