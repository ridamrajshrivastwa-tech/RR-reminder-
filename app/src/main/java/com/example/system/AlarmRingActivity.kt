package com.example.system

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.AlarmOff
import androidx.compose.material.icons.rounded.Snooze
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.notifications.ReminderAlarmReceiver
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.NavyDeep
import com.example.ui.theme.NavyVoid
import com.example.ui.theme.RRReminderTheme
import com.example.util.DateTimeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AlarmRingActivity : ComponentActivity() {
    private var mediaPlayer: MediaPlayer? = null
    private var volumeJob: Job? = null
    private var closeReceiver: BroadcastReceiver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val alarmId = intent.getStringExtra(EXTRA_ALARM_ID).orEmpty()
        val label = intent.getStringExtra(EXTRA_ALARM_LABEL).orEmpty().ifBlank { "Alarm" }
        val snoozeMins = intent.getIntExtra(EXTRA_SNOOZE_MINUTES, 10)
        val gradual = intent.getBooleanExtra(EXTRA_GRADUAL_VOLUME, true)
        val vibrate = intent.getBooleanExtra(EXTRA_VIBRATE, true)
        val toneName = intent.getStringExtra(EXTRA_TONE_NAME).orEmpty().ifBlank { "Dawn Chime" }

        startAlarmMedia(gradual, vibrate)

        closeReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                finish()
            }
        }
        ContextCompat.registerReceiver(
            this,
            closeReceiver,
            IntentFilter(ACTION_CLOSE_ALARM_SCREEN),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        setContent {
            RRReminderTheme {
                var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
                val pulseTransition = rememberInfiniteTransition(label = "alarm_pulse")
                val pulseScale by pulseTransition.animateFloat(
                    initialValue = 0.94f,
                    targetValue = 1.12f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(900),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "pulse_scale"
                )

                androidx.compose.runtime.LaunchedEffect(Unit) {
                    while (true) {
                        nowMillis = System.currentTimeMillis()
                        delay(1000L)
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.verticalGradient(listOf(NavyVoid, NavyDeep, Color(0xFF0E1E40))))
                        .safeDrawingPadding()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(24.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .scale(pulseScale)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.22f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Alarm,
                                contentDescription = "Active alarm",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(58.dp)
                            )
                        }

                        Text(
                            text = DateTimeUtils.formatDigitalTime(nowMillis, use24Hour = false, showSeconds = false),
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Text(
                            text = label,
                            style = MaterialTheme.typography.headlineLarge,
                            color = Color.White
                        )
                        Text(
                            text = "$toneName • ${if (gradual) "Gentle fade-in" else "Full volume"}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(Modifier.height(16.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedButton(
                                onClick = {
                                    sendBroadcast(
                                        Intent(this@AlarmRingActivity, ReminderAlarmReceiver::class.java).apply {
                                            action = ReminderAlarmReceiver.ACTION_ALARM_SNOOZE
                                            putExtra(ReminderAlarmReceiver.EXTRA_ID, alarmId)
                                            putExtra(ReminderAlarmReceiver.EXTRA_SNOOZE_MINUTES, snoozeMins)
                                        }
                                    )
                                    finish()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp)
                                    .testTag("alarm_ring_snooze_btn")
                            ) {
                                Icon(Icons.Rounded.Snooze, contentDescription = null)
                                Spacer(Modifier.size(8.dp))
                                Text("Snooze ${snoozeMins}m")
                            }

                            Button(
                                onClick = {
                                    sendBroadcast(
                                        Intent(this@AlarmRingActivity, ReminderAlarmReceiver::class.java).apply {
                                            action = ReminderAlarmReceiver.ACTION_ALARM_DISMISS
                                            putExtra(ReminderAlarmReceiver.EXTRA_ID, alarmId)
                                        }
                                    )
                                    finish()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp)
                                    .testTag("alarm_ring_dismiss_btn")
                            ) {
                                Icon(Icons.Rounded.AlarmOff, contentDescription = null)
                                Spacer(Modifier.size(8.dp))
                                Text("Dismiss")
                            }
                        }
                    }
                }
            }
        }
    }

    private fun startAlarmMedia(gradualVolume: Boolean, vibrate: Boolean) {
        try {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            if (uri != null) {
                mediaPlayer = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    setDataSource(this@AlarmRingActivity, uri)
                    isLooping = true
                    val startVol = if (gradualVolume) 0.1f else 1.0f
                    setVolume(startVol, startVol)
                    prepare()
                    start()
                }
                if (gradualVolume) {
                    volumeJob = CoroutineScope(Dispatchers.Main).launch {
                        for (step in 1..10) {
                            delay(1200L)
                            val vol = (step / 10f).coerceIn(0.1f, 1.0f)
                            mediaPlayer?.setVolume(vol, vol)
                        }
                    }
                }
            }
        } catch (_: Exception) {
            // Audio fallback silently handled if emulator has no alarm audio device
        }

        if (vibrate) {
            try {
                val pattern = longArrayOf(0, 600, 400, 600)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                    vm.defaultVibrator.vibrate(VibrationEffect.createWaveform(pattern, 0))
                } else {
                    @Suppress("DEPRECATION")
                    val vib = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vib.vibrate(VibrationEffect.createWaveform(pattern, 0))
                    } else {
                        @Suppress("DEPRECATION")
                        vib.vibrate(pattern, 0)
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    override fun onDestroy() {
        volumeJob?.cancel()
        mediaPlayer?.run {
            try {
                if (isPlaying) stop()
                release()
            } catch (_: Exception) {
            }
        }
        mediaPlayer = null
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vm.defaultVibrator.cancel()
            } else {
                @Suppress("DEPRECATION")
                (getSystemService(Context.VIBRATOR_SERVICE) as Vibrator).cancel()
            }
        } catch (_: Exception) {
        }
        closeReceiver?.let { unregisterReceiver(it) }
        super.onDestroy()
    }

    companion object {
        const val EXTRA_ALARM_ID = "alarm_id"
        const val EXTRA_ALARM_LABEL = "alarm_label"
        const val EXTRA_SNOOZE_MINUTES = "snooze_minutes"
        const val EXTRA_GRADUAL_VOLUME = "gradual_volume"
        const val EXTRA_VIBRATE = "vibrate"
        const val EXTRA_TONE_NAME = "tone_name"
        const val ACTION_CLOSE_ALARM_SCREEN = "com.example.action.CLOSE_ALARM_SCREEN"
    }
}
