package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.data.firebase.GoogleSignInButton
import com.example.data.firebase.signOutGoogle
import com.example.data.local.UserPreferences
import com.example.system.SecurityUtils
import com.example.ui.components.RRCard
import com.example.ui.theme.AccentPalette
import com.example.ui.theme.ClockDesign
import com.example.ui.theme.ErrorCrimson
import com.example.ui.theme.SuccessEmerald
import com.example.ui.theme.ThemeMode
import com.example.ui.viewmodel.CloudActionUiState
import com.example.ui.viewmodel.CloudHistoryUiState
import com.example.util.DateTimeUtils
import kotlinx.coroutines.launch
import java.util.Calendar

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    preferences: UserPreferences,
    isSignedIn: Boolean,
    userEmail: String?,
    backupHistoryState: CloudHistoryUiState,
    cloudActionState: CloudActionUiState,
    onSetThemeMode: (ThemeMode) -> Unit,
    onSetAccentPalette: (AccentPalette) -> Unit,
    onSetHomeClockDesign: (ClockDesign) -> Unit,
    onSetUse24HourTime: (Boolean) -> Unit,
    onSetFirstDayOfWeek: (Int) -> Unit,
    onSetLanguageLabel: (String) -> Unit,
    onSetWeatherFahrenheit: (Boolean) -> Unit,
    onSetNotificationSound: (Boolean) -> Unit,
    onSetNotificationVibration: (Boolean) -> Unit,
    onSetPrivacyHideLockScreen: (Boolean) -> Unit,
    onConfigurePinLock: (Boolean, String?) -> Unit,
    onSetBiometricEnabled: (Boolean) -> Unit,
    onSetAutoLockMinutes: (Int) -> Unit,
    onLockNow: () -> Unit,
    onSetAutoCloudBackup: (Boolean) -> Unit,
    onTriggerCloudBackup: () -> Unit,
    onTriggerCloudRestore: () -> Unit,
    onExportJson: suspend () -> String,
    onImportJson: suspend (String) -> Result<Int>,
    onOpenNotificationsScreen: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var showPinDialog by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }
    var pinConfirm by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }

    var authError by remember { mutableStateOf<String?>(null) }
    var localStatusMessage by remember { mutableStateOf<String?>(null) }
    var showRestoreConfirmDialog by remember { mutableStateOf(false) }

    val biometricAvailable = remember(context) { SecurityUtils.isBiometricAvailable(context) }
    val hasPinConfigured = preferences.appLockPinHash.isNotBlank()

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val json = onExportJson()
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(json.toByteArray(Charsets.UTF_8))
                    }
                    localStatusMessage = "Local JSON backup exported successfully."
                } catch (e: Exception) {
                    localStatusMessage = "Export failed: ${e.localizedMessage}"
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val content = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                        ?: throw IllegalStateException("Could not read selected file")
                    onImportJson(content)
                        .onSuccess { count -> localStatusMessage = "Imported $count items from backup." }
                        .onFailure { err -> localStatusMessage = "Import failed: ${err.localizedMessage}" }
                } catch (e: Exception) {
                    localStatusMessage = "Import failed: ${e.localizedMessage}"
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Settings", style = MaterialTheme.typography.headlineLarge)
            Text(
                text = "Appearance, alerts, security, and cloud/local backups",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // 1. Appearance & Regional
        item {
            RRCard(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("Appearance & Regional", style = MaterialTheme.typography.titleMedium)
                }
                Spacer(Modifier.height(10.dp))

                Text("Theme", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(4.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeMode.entries.forEach { mode ->
                        FilterChip(
                            selected = preferences.themeMode == mode,
                            onClick = { onSetThemeMode(mode) },
                            label = { Text(mode.label) },
                            modifier = Modifier.testTag("theme_chip_${mode.name}")
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))
                Text("Home Watch / Clock Style", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(4.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ClockDesign.entries.forEach { design ->
                        FilterChip(
                            selected = preferences.homeClockDesign == design,
                            onClick = { onSetHomeClockDesign(design) },
                            label = { Text(design.label) },
                            modifier = Modifier.testTag("settings_clock_${design.name}")
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Accent Color", style = MaterialTheme.typography.labelLarge)
                    Text(
                        text = preferences.accentPalette.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AccentPalette.entries.forEach { palette ->
                        val selected = preferences.accentPalette == palette
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(palette.primaryDark)
                                .then(
                                    if (selected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    else Modifier
                                )
                                .clickable { onSetAccentPalette(palette) }
                                .testTag("accent_swatch_${palette.name}")
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                SettingToggleRow(
                    title = "24-hour time format",
                    subtitle = "Apply 24h display across clocks, alarms & reminders",
                    checked = preferences.use24HourTime,
                    onCheckedChange = onSetUse24HourTime
                )
                SettingToggleRow(
                    title = "Fahrenheit (°F) weather units",
                    subtitle = "Switch live temperature display between °C and °F",
                    checked = preferences.weatherUseFahrenheit,
                    onCheckedChange = onSetWeatherFahrenheit
                )

                Spacer(Modifier.height(8.dp))
                Text("First Day of Week (Calendar)", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        Calendar.MONDAY to "Monday",
                        Calendar.SUNDAY to "Sunday",
                        Calendar.SATURDAY to "Saturday"
                    ).forEach { (dow, label) ->
                        FilterChip(
                            selected = preferences.firstDayOfWeek == dow,
                            onClick = { onSetFirstDayOfWeek(dow) },
                            label = { Text(label) }
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
                Text("Display Locale Label", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(4.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("English (System)", "English (US)", "English (UK)").forEach { lang ->
                        FilterChip(
                            selected = preferences.languageLabel == lang,
                            onClick = { onSetLanguageLabel(lang) },
                            label = { Text(lang) }
                        )
                    }
                }
            }
        }

        // 2. Notification & Sound Settings
        item {
            RRCard(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("Notifications & Audio", style = MaterialTheme.typography.titleMedium)
                }
                Spacer(Modifier.height(8.dp))
                SettingToggleRow(
                    title = "Notification alert sound",
                    subtitle = "Play chime when reminders trigger",
                    checked = preferences.notificationSoundEnabled,
                    onCheckedChange = onSetNotificationSound
                )
                SettingToggleRow(
                    title = "Vibration alerts",
                    subtitle = "Vibrate on reminders and alarms",
                    checked = preferences.notificationVibrationEnabled,
                    onCheckedChange = onSetNotificationVibration
                )
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onOpenNotificationsScreen,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Open Permissions, Battery Status & Alert History")
                }
            }
        }

        // 3. Security & Privacy
        item {
            RRCard(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("Security & Privacy", style = MaterialTheme.typography.titleMedium)
                }
                Spacer(Modifier.height(8.dp))

                SettingToggleRow(
                    title = "Privacy mode on lock screen",
                    subtitle = "Hide reminder titles and notes in lock-screen notifications",
                    checked = preferences.privacyHideLockScreenDetails,
                    onCheckedChange = onSetPrivacyHideLockScreen
                )

                SettingToggleRow(
                    title = "PIN App Lock",
                    subtitle = if (hasPinConfigured) "4-digit PIN configured" else "Require PIN when opening RR Reminder",
                    checked = preferences.appLockEnabled,
                    onCheckedChange = { enable ->
                        if (enable && !hasPinConfigured) {
                            pinInput = ""
                            pinConfirm = ""
                            pinError = null
                            showPinDialog = true
                        } else if (!enable) {
                            onConfigurePinLock(false, null)
                        } else {
                            pinInput = ""
                            pinConfirm = ""
                            pinError = null
                            showPinDialog = true
                        }
                    }
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            pinInput = ""
                            pinConfirm = ""
                            pinError = null
                            showPinDialog = true
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (hasPinConfigured) "Change PIN" else "Set 4-Digit PIN")
                    }
                    if (preferences.appLockEnabled && hasPinConfigured) {
                        OutlinedButton(
                            onClick = onLockNow,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Lock Now")
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                SettingToggleRow(
                    title = "Biometric unlock",
                    subtitle = if (biometricAvailable) "Allow fingerprint or face unlock" else "Not configured on this device",
                    checked = preferences.biometricEnabled,
                    enabled = biometricAvailable && preferences.appLockEnabled && hasPinConfigured,
                    onCheckedChange = onSetBiometricEnabled
                )

                if (preferences.appLockEnabled) {
                    Spacer(Modifier.height(6.dp))
                    Text("Auto-Lock Timeout", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(4.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(0 to "Immediate", 1 to "1 min", 5 to "5 min", 15 to "15 min").forEach { (mins, label) ->
                            FilterChip(
                                selected = preferences.autoLockMinutes == mins,
                                onClick = { onSetAutoLockMinutes(mins) },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            }
        }

        // 4. Cloud Backup & Restore + Local JSON Export/Import
        item {
            RRCard(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.CloudDone, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("Backup & Restore", style = MaterialTheme.typography.titleMedium)
                }
                Spacer(Modifier.height(8.dp))

                if (isSignedIn) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Google Cloud Sync Connected", style = MaterialTheme.typography.labelLarge)
                                Text(
                                    text = userEmail ?: "Signed-in Google Account",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            TextButton(
                                onClick = {
                                    signOutGoogle(credentialManager, scope)
                                },
                                modifier = Modifier.testTag("settings_sign_out_btn")
                            ) {
                                Text("Sign Out")
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onTriggerCloudBackup,
                            enabled = cloudActionState !is CloudActionUiState.InProgress,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("cloud_backup_now_btn")
                        ) {
                            Icon(Icons.Rounded.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Backup Now")
                        }
                        OutlinedButton(
                            onClick = { showRestoreConfirmDialog = true },
                            enabled = cloudActionState !is CloudActionUiState.InProgress,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("cloud_restore_btn")
                        ) {
                            Icon(Icons.Rounded.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Restore")
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    SettingToggleRow(
                        title = "Scheduled daily cloud backup",
                        subtitle = "Uses Android JobScheduler when on network",
                        checked = preferences.autoCloudBackupEnabled,
                        onCheckedChange = onSetAutoCloudBackup
                    )

                    when (cloudActionState) {
                        CloudActionUiState.InProgress -> {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 6.dp)) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                                Text("Syncing with cloud…", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        is CloudActionUiState.Message -> {
                            Text(
                                text = cloudActionState.text,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (cloudActionState.isError) ErrorCrimson else SuccessEmerald,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                        CloudActionUiState.Idle -> Unit
                    }

                    Spacer(Modifier.height(8.dp))
                    Text("Cloud Backup History", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(4.dp))
                    when (backupHistoryState) {
                        CloudHistoryUiState.Loading -> Text("Loading cloud history…", style = MaterialTheme.typography.bodySmall)
                        is CloudHistoryUiState.Error -> Text(
                            text = backupHistoryState.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = ErrorCrimson
                        )
                        is CloudHistoryUiState.Success -> {
                            if (backupHistoryState.records.isEmpty()) {
                                Text("No cloud snapshots yet.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            } else {
                                backupHistoryState.records.take(5).forEach { rec ->
                                    val timeStr = rec.createdAt?.toDate()?.time?.let {
                                        DateTimeUtils.formatShortDateTime(it, preferences.use24HourTime)
                                    } ?: "Pending server timestamp"
                                    Text(
                                        text = "• $timeStr (${rec.deviceLabel}): ${rec.reminderCount} reminders, ${rec.alarmCount} alarms, ${rec.eventCount} events",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Text(
                        text = "Sign in with Google to back up and restore your reminders, alarms, and calendar events across devices.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))
                    GoogleSignInButton(
                        onAuthSuccess = { authError = null },
                        onAuthError = { authError = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (!authError.isNullOrBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(authError.orEmpty(), style = MaterialTheme.typography.bodySmall, color = ErrorCrimson)
                    }
                }

                Spacer(Modifier.height(12.dp))
                Text("Local JSON Export & Import", style = MaterialTheme.typography.labelLarge)
                if (preferences.lastBackupMillis > 0L) {
                    Text(
                        text = "Last backup: ${DateTimeUtils.formatShortDateTime(preferences.lastBackupMillis, preferences.use24HourTime)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { exportLauncher.launch("rr_reminder_backup.json") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("export_json_btn")
                    ) {
                        Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Export JSON")
                    }
                    OutlinedButton(
                        onClick = { importLauncher.launch(arrayOf("application/json", "text/plain")) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("import_json_btn")
                    ) {
                        Icon(Icons.Rounded.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Import JSON")
                    }
                }
                if (!localStatusMessage.isNullOrBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = localStatusMessage.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // 5. About RR Reminder
        item {
            RRCard(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("About RR Reminder", style = MaterialTheme.typography.titleMedium)
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "RR Reminder v1.0 — Created by Ridam Raj. Unified reminder scheduling, smart alarm clock, live Open-Meteo weather forecasts, world clock, multi-timer, monotonic stopwatch, calendar planner, routines, focus timer, and Gemini AI assistant.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item { Spacer(Modifier.height(72.dp)) }
    }

    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = { Text("Configure 4-Digit Security PIN") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { if (it.length <= 4) pinInput = it.filter { c -> c.isDigit() } },
                        label = { Text("Enter 4-digit PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = pinConfirm,
                        onValueChange = { if (it.length <= 4) pinConfirm = it.filter { c -> c.isDigit() } },
                        label = { Text("Confirm 4-digit PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (!pinError.isNullOrBlank()) {
                        Text(pinError.orEmpty(), style = MaterialTheme.typography.bodySmall, color = ErrorCrimson)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (pinInput.length != 4) {
                        pinError = "PIN must be exactly 4 digits"
                        return@Button
                    }
                    if (pinInput != pinConfirm) {
                        pinError = "PIN entries do not match"
                        return@Button
                    }
                    onConfigurePinLock(true, pinInput)
                    showPinDialog = false
                }) { Text("Save PIN") }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showRestoreConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirmDialog = false },
            title = { Text("Restore from Cloud?") },
            text = { Text("This will merge and update your device reminders, alarms, and calendar events with your latest cloud snapshot.") },
            confirmButton = {
                Button(onClick = {
                    showRestoreConfirmDialog = false
                    onTriggerCloudRestore()
                }) { Text("Restore") }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirmDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}
