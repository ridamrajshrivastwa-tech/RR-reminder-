package com.example.system

import android.content.Context
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import android.util.Base64
import androidx.core.content.ContextCompat
import java.security.MessageDigest
import java.security.SecureRandom

object SecurityUtils {
    fun generateSalt(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    fun hashPin(pin: String, salt: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest("$salt:$pin".toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(digest, Base64.NO_WRAP)
    }

    fun verifyPin(pin: String, expectedHash: String, salt: String): Boolean {
        if (expectedHash.isBlank() || salt.isBlank()) return false
        return hashPin(pin, salt) == expectedHash
    }

    fun isBiometricAvailable(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val bm = context.getSystemService(BiometricManager::class.java) ?: return false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                bm.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL) == BiometricManager.BIOMETRIC_SUCCESS
            } else {
                @Suppress("DEPRECATION")
                bm.canAuthenticate() == BiometricManager.BIOMETRIC_SUCCESS
            }
        } else {
            false
        }
    }

    fun promptBiometricUnlock(
        context: Context,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            onError("Biometric unlock requires Android 9 or higher. Use your PIN instead.")
            return
        }
        val executor = ContextCompat.getMainExecutor(context)
        val builder = BiometricPrompt.Builder(context)
            .setTitle("Unlock RR Reminder")
            .setSubtitle("Verify your identity to access reminders")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            builder.setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_WEAK or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
        } else {
            builder.setNegativeButton("Use PIN", executor) { _, _ -> }
        }

        val prompt = builder.build()
        prompt.authenticate(
            CancellationSignal(),
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                    onError(errString?.toString() ?: "Biometric authentication unavailable")
                }

                override fun onAuthenticationFailed() {
                    onError("Biometric not recognized. Try again or use PIN.")
                }
            }
        )
    }
}
