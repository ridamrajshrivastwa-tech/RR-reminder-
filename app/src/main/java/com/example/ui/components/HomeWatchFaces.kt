package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ClockDesign
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.util.DateTimeUtils
import java.util.Calendar
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun HomeWatchFaceContent(
    design: ClockDesign,
    nowMillis: Long,
    use24HourTime: Boolean,
    doneDailyItems: Int,
    totalDailyItems: Int,
    completionRatio: Float
) {
    val cal = remember(nowMillis) {
        Calendar.getInstance().apply { timeInMillis = nowMillis }
    }
    val hour24 = cal.get(Calendar.HOUR_OF_DAY)
    val hour12 = cal.get(Calendar.HOUR).let { if (it == 0) 12 else it }
    val minute = cal.get(Calendar.MINUTE)
    val second = cal.get(Calendar.SECOND)
    val amPm = if (cal.get(Calendar.AM_PM) == Calendar.AM) "AM" else "PM"

    when (design) {
        ClockDesign.CHRONO_DIGITAL -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = DateTimeUtils.formatDigitalTime(nowMillis, use24HourTime, showSeconds = true),
                        fontFamily = JetBrainsMonoFontFamily,
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = DateTimeUtils.formatFullDate(nowMillis),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                DailyCompletionRing(doneDailyItems, totalDailyItems, completionRatio)
            }
        }

        ClockDesign.CLASSIC_ANALOG -> {
            val primary = MaterialTheme.colorScheme.primary
            val secondary = MaterialTheme.colorScheme.secondary
            val onSurface = MaterialTheme.colorScheme.onSurface
            val track = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier.size(124.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(120.dp)) {
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val radius = size.minDimension * 0.41f

                        // Watch strap lugs (top & bottom)
                        val lugW = radius * 0.66f
                        val lugH = size.minDimension * 0.08f
                        drawRoundRect(
                            color = primary.copy(alpha = 0.55f),
                            topLeft = Offset(cx - lugW / 2f, cy - radius - lugH * 0.72f),
                            size = Size(lugW, lugH),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
                        )
                        drawRoundRect(
                            color = primary.copy(alpha = 0.55f),
                            topLeft = Offset(cx - lugW / 2f, cy + radius - lugH * 0.28f),
                            size = Size(lugW, lugH),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
                        )
                        // Right crown
                        drawRoundRect(
                            color = secondary,
                            topLeft = Offset(cx + radius + 2.dp.toPx(), cy - radius * 0.16f),
                            size = Size(5.dp.toPx(), radius * 0.32f),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
                        )

                        // Watch outer bezel
                        drawCircle(
                            color = primary.copy(alpha = 0.08f),
                            radius = radius,
                            center = Offset(cx, cy)
                        )
                        drawCircle(
                            brush = Brush.sweepGradient(listOf(primary, secondary, primary)),
                            radius = radius,
                            center = Offset(cx, cy),
                            style = Stroke(width = 3.5.dp.toPx())
                        )

                        // 12 hour markers & 60 minute sub-ticks
                        for (i in 0 until 60) {
                            val angleRad = (i * 6.0 - 90.0) * (PI / 180.0)
                            val isHour = i % 5 == 0
                            val outerR = radius - 5.dp.toPx()
                            val innerR = outerR - if (isHour) 7.dp.toPx() else 3.dp.toPx()
                            drawLine(
                                color = if (isHour) onSurface.copy(alpha = 0.85f) else track,
                                start = Offset(
                                    cx + (cos(angleRad) * innerR).toFloat(),
                                    cy + (sin(angleRad) * innerR).toFloat()
                                ),
                                end = Offset(
                                    cx + (cos(angleRad) * outerR).toFloat(),
                                    cy + (sin(angleRad) * outerR).toFloat()
                                ),
                                strokeWidth = if (isHour) 2.dp.toPx() else 1.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        }

                        // Hour hand
                        val hourAngle = ((hour12 % 12) * 30.0 + minute * 0.5 - 90.0) * (PI / 180.0)
                        drawLine(
                            color = onSurface,
                            start = Offset(cx, cy),
                            end = Offset(
                                cx + (cos(hourAngle) * radius * 0.52f).toFloat(),
                                cy + (sin(hourAngle) * radius * 0.52f).toFloat()
                            ),
                            strokeWidth = 3.6.dp.toPx(),
                            cap = StrokeCap.Round
                        )

                        // Minute hand
                        val minAngle = (minute * 6.0 + second * 0.1 - 90.0) * (PI / 180.0)
                        drawLine(
                            color = primary,
                            start = Offset(cx, cy),
                            end = Offset(
                                cx + (cos(minAngle) * radius * 0.74f).toFloat(),
                                cy + (sin(minAngle) * radius * 0.74f).toFloat()
                            ),
                            strokeWidth = 2.6.dp.toPx(),
                            cap = StrokeCap.Round
                        )

                        // Second hand
                        val secAngle = (second * 6.0 - 90.0) * (PI / 180.0)
                        drawLine(
                            color = secondary,
                            start = Offset(cx, cy),
                            end = Offset(
                                cx + (cos(secAngle) * radius * 0.82f).toFloat(),
                                cy + (sin(secAngle) * radius * 0.82f).toFloat()
                            ),
                            strokeWidth = 1.5.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                        drawCircle(color = secondary, radius = 3.5.dp.toPx(), center = Offset(cx, cy))
                    }
                    Text(
                        text = "RR",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = 34.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = DateTimeUtils.formatDigitalTime(nowMillis, use24HourTime, showSeconds = false),
                        fontFamily = JetBrainsMonoFontFamily,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = DateTimeUtils.formatFullDate(nowMillis),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "Daily progress ${(completionRatio * 100).roundToInt()}% ($doneDailyItems/$totalDailyItems)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        ClockDesign.FLIP_DECK -> {
            val hhText = String.format(Locale.US, "%02d", if (use24HourTime) hour24 else hour12)
            val mmText = String.format(Locale.US, "%02d", minute)
            val ssText = String.format(Locale.US, "%02d", second)

            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FlipDigitCard(digits = hhText, label = if (use24HourTime) "24H" else amPm)
                    Text(
                        text = ":",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    FlipDigitCard(digits = mmText, label = "${ssText}s")
                    Spacer(Modifier.weight(1f))
                    DailyCompletionRing(doneDailyItems, totalDailyItems, completionRatio)
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = DateTimeUtils.formatFullDate(nowMillis),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        ClockDesign.RADIAL_GAUGE -> {
            val primary = MaterialTheme.colorScheme.primary
            val secondary = MaterialTheme.colorScheme.secondary
            val tertiary = MaterialTheme.colorScheme.tertiary
            val track = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier.size(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(116.dp)) {
                        val s1 = 7.dp.toPx()
                        val s2 = 6.dp.toPx()
                        val s3 = 5.dp.toPx()

                        // Outer ring: seconds (0..59)
                        drawArc(track, 0f, 360f, false, style = Stroke(s1, cap = StrokeCap.Round))
                        drawArc(
                            color = secondary,
                            startAngle = -90f,
                            sweepAngle = (second / 60f) * 360f,
                            useCenter = false,
                            style = Stroke(s1, cap = StrokeCap.Round)
                        )

                        // Middle ring: minutes (0..59)
                        val pad2 = 12.dp.toPx()
                        drawArc(
                            track, 0f, 360f, false,
                            topLeft = Offset(pad2, pad2),
                            size = Size(size.width - pad2 * 2, size.height - pad2 * 2),
                            style = Stroke(s2, cap = StrokeCap.Round)
                        )
                        drawArc(
                            color = primary,
                            startAngle = -90f,
                            sweepAngle = (minute / 60f) * 360f,
                            useCenter = false,
                            topLeft = Offset(pad2, pad2),
                            size = Size(size.width - pad2 * 2, size.height - pad2 * 2),
                            style = Stroke(s2, cap = StrokeCap.Round)
                        )

                        // Inner ring: daily tasks completion
                        val pad3 = 23.dp.toPx()
                        drawArc(
                            track, 0f, 360f, false,
                            topLeft = Offset(pad3, pad3),
                            size = Size(size.width - pad3 * 2, size.height - pad3 * 2),
                            style = Stroke(s3, cap = StrokeCap.Round)
                        )
                        drawArc(
                            color = tertiary,
                            startAngle = -90f,
                            sweepAngle = completionRatio.coerceIn(0f, 1f) * 360f,
                            useCenter = false,
                            topLeft = Offset(pad3, pad3),
                            size = Size(size.width - pad3 * 2, size.height - pad3 * 2),
                            style = Stroke(s3, cap = StrokeCap.Round)
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = String.format(Locale.US, "%02d", second),
                            fontFamily = JetBrainsMonoFontFamily,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text("SEC", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = DateTimeUtils.formatDigitalTime(nowMillis, use24HourTime, showSeconds = false),
                        fontFamily = JetBrainsMonoFontFamily,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = DateTimeUtils.formatFullDate(nowMillis),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Outer: Seconds • Mid: Minutes • Inner: Goals ($doneDailyItems/$totalDailyItems)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun FlipDigitCard(digits: String, label: String) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
        tonalElevation = 4.dp
    ) {
        Box(
            modifier = Modifier
                .width(74.dp)
                .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = digits,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            // Split-flap center hairline
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                    .align(Alignment.Center)
            )
        }
    }
}

@Composable
fun DailyCompletionRing(
    doneDailyItems: Int,
    totalDailyItems: Int,
    completionRatio: Float
) {
    val ringColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    Box(
        modifier = Modifier.size(84.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(80.dp)) {
            val stroke = Stroke(width = 8.5.dp.toPx(), cap = StrokeCap.Round)
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = stroke
            )
            drawArc(
                color = ringColor,
                startAngle = -90f,
                sweepAngle = 360f * completionRatio.coerceIn(0f, 1f),
                useCenter = false,
                style = stroke
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${(completionRatio * 100).roundToInt()}%",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "$doneDailyItems/$totalDailyItems",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
