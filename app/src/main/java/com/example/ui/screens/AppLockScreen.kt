package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.system.SecurityUtils

@Composable
fun AppLockScreen(
    biometricEnabled: Boolean,
    onVerifyPin: (String) -> Boolean,
    onBiometricSuccess: () -> Unit
) {
    val context = LocalContext.current
    var pinInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val biometricAvailable = remember(context) { SecurityUtils.isBiometricAvailable(context) }

    LaunchedEffect(biometricEnabled, biometricAvailable) {
        if (biometricEnabled && biometricAvailable) {
            SecurityUtils.promptBiometricUnlock(
                context = context,
                onSuccess = onBiometricSuccess,
                onError = { errorMessage = it }
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.widthIn(max = 360.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Lock,
                    contentDescription = "App Locked",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(30.dp)
                )
            }
            Text("RR Reminder Locked", style = MaterialTheme.typography.headlineMedium)
            Text(
                text = "Enter your security PIN to continue",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(4) { index ->
                    val filled = index < pinInput.length
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(
                                if (filled) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline
                            )
                    )
                }
            }

            if (!errorMessage.isNullOrBlank()) {
                Text(
                    text = errorMessage.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            val rows = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("BIO", "0", "DEL")
            )

            rows.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    row.forEach { key ->
                        when (key) {
                            "BIO" -> {
                                if (biometricEnabled && biometricAvailable) {
                                    OutlinedButton(
                                        onClick = {
                                            SecurityUtils.promptBiometricUnlock(
                                                context = context,
                                                onSuccess = onBiometricSuccess,
                                                onError = { errorMessage = it }
                                            )
                                        },
                                        modifier = Modifier.size(72.dp),
                                        shape = CircleShape
                                    ) {
                                        Icon(Icons.Rounded.Fingerprint, contentDescription = "Biometric unlock")
                                    }
                                } else {
                                    Spacer(Modifier.size(72.dp))
                                }
                            }
                            "DEL" -> {
                                TextButton(
                                    onClick = {
                                        if (pinInput.isNotEmpty()) pinInput = pinInput.dropLast(1)
                                    },
                                    modifier = Modifier.size(72.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Rounded.Backspace, contentDescription = "Delete digit")
                                }
                            }
                            else -> {
                                FilledTonalButton(
                                    onClick = {
                                        if (pinInput.length < 4) {
                                            val updated = pinInput + key
                                            pinInput = updated
                                            errorMessage = null
                                            if (updated.length == 4) {
                                                if (!onVerifyPin(updated)) {
                                                    errorMessage = "Incorrect PIN. Try again."
                                                    pinInput = ""
                                                }
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .size(72.dp)
                                        .testTag("pin_key_$key"),
                                    shape = CircleShape
                                ) {
                                    Text(key, style = MaterialTheme.typography.titleLarge)
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
