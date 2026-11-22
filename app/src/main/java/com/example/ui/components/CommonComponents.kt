package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.local.ReminderPriority
import com.example.ui.theme.PriorityHighColor
import com.example.ui.theme.PriorityLowColor
import com.example.ui.theme.PriorityMediumColor
import com.example.ui.theme.PriorityUrgentColor

@Composable
fun RRLogoMark(
    modifier: Modifier = Modifier,
    size: Dp = 40.dp
) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val onSurface = MaterialTheme.colorScheme.onSurface
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(primary.copy(alpha = 0.20f), secondary.copy(alpha = 0.12f))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size * 0.86f)) {
            val w = this.size.width
            val h = this.size.height
            val cx = w / 2f
            val cy = h / 2f
            val bezelRadius = this.size.minDimension * 0.36f
            val bezelStroke = this.size.minDimension * 0.065f

            // Top and bottom watch strap lugs connecting the watch to the emblem
            val lugW = bezelRadius * 0.88f
            val lugH = this.size.minDimension * 0.12f
            drawRoundRect(
                color = primary.copy(alpha = 0.72f),
                topLeft = Offset(cx - lugW / 2f, cy - bezelRadius - lugH * 0.78f),
                size = androidx.compose.ui.geometry.Size(lugW, lugH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f, 3f)
            )
            drawRoundRect(
                color = primary.copy(alpha = 0.72f),
                topLeft = Offset(cx - lugW / 2f, cy + bezelRadius - lugH * 0.22f),
                size = androidx.compose.ui.geometry.Size(lugW, lugH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f, 3f)
            )

            // Side watch crown
            drawRoundRect(
                color = secondary,
                topLeft = Offset(cx + bezelRadius + bezelStroke * 0.35f, cy - bezelRadius * 0.18f),
                size = androidx.compose.ui.geometry.Size(this.size.minDimension * 0.08f, bezelRadius * 0.36f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f, 3f)
            )

            // Dial fill & precision metallic sweep bezel
            drawCircle(
                color = primary.copy(alpha = 0.12f),
                radius = bezelRadius,
                center = Offset(cx, cy)
            )
            drawCircle(
                brush = Brush.sweepGradient(listOf(primary, secondary, primary)),
                radius = bezelRadius,
                center = Offset(cx, cy),
                style = Stroke(width = bezelStroke)
            )

            // 4 cardinal watch ticks
            val tickLen = bezelRadius * 0.16f
            listOf(
                Offset(cx, cy - bezelRadius + bezelStroke) to Offset(cx, cy - bezelRadius + bezelStroke + tickLen),
                Offset(cx, cy + bezelRadius - bezelStroke) to Offset(cx, cy + bezelRadius - bezelStroke - tickLen),
                Offset(cx - bezelRadius + bezelStroke, cy) to Offset(cx - bezelRadius + bezelStroke + tickLen, cy),
                Offset(cx + bezelRadius - bezelStroke, cy) to Offset(cx + bezelRadius - bezelStroke - tickLen, cy)
            ).forEach { (start, end) ->
                drawLine(
                    color = secondary.copy(alpha = 0.65f),
                    start = start,
                    end = end,
                    strokeWidth = bezelStroke * 0.55f,
                    cap = StrokeCap.Round
                )
            }

            // Geometric interlocking "RR" inside the watch dial
            val letterStroke = this.size.minDimension * 0.068f
            val rH = bezelRadius * 0.76f
            val rTop = cy - rH / 2f
            val rBot = cy + rH / 2f
            val rMid = cy + rH * 0.02f

            fun drawR(leftX: Float, rightX: Float, color: Color) {
                val loopMid = (rTop + rMid) / 2f
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(leftX, rBot)
                    lineTo(leftX, rTop)
                    lineTo(rightX - (rightX - leftX) * 0.15f, rTop)
                    quadraticTo(rightX + (rightX - leftX) * 0.12f, loopMid, rightX - (rightX - leftX) * 0.15f, rMid)
                    lineTo(leftX, rMid)
                    moveTo((leftX + rightX) * 0.52f, rMid)
                    lineTo(rightX, rBot)
                }
                drawPath(
                    path = path,
                    color = color,
                    style = Stroke(width = letterStroke, cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round)
                )
            }

            val rWidth = bezelRadius * 0.44f
            val gap = bezelRadius * 0.10f
            drawR(cx - gap / 2f - rWidth, cx - gap / 2f, onSurface)
            drawR(cx + gap / 2f, cx + gap / 2f + rWidth, primary)

            // Watch hands connecting center pin out to dial
            drawLine(
                color = secondary,
                start = Offset(cx, cy),
                end = Offset(cx + bezelRadius * 0.52f, cy - bezelRadius * 0.48f),
                strokeWidth = bezelStroke * 0.65f,
                cap = StrokeCap.Round
            )
            drawCircle(color = secondary, radius = bezelStroke * 0.55f, center = Offset(cx, cy))
        }
    }
}

@Composable
fun RRCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    borderColor: Color = MaterialTheme.colorScheme.outlineVariant,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    val baseMod = modifier
        .clip(shape)
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)

    Surface(
        modifier = baseMod,
        shape = shape,
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

@Composable
fun RRGlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val shape = RoundedCornerShape(24.dp)
    Surface(
        modifier = modifier
            .clip(shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = shape,
        color = Color.Transparent,
        border = BorderStroke(1.dp, primary.copy(alpha = 0.28f))
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            primary.copy(alpha = 0.16f),
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
                            secondary.copy(alpha = 0.08f)
                        )
                    )
                )
                .padding(18.dp),
            content = content
        )
    }
}

@Composable
fun PriorityBadge(priorityName: String, modifier: Modifier = Modifier) {
    val priority = ReminderPriority.entries.firstOrNull { it.name == priorityName } ?: ReminderPriority.MEDIUM
    val color = when (priority) {
        ReminderPriority.LOW -> PriorityLowColor
        ReminderPriority.MEDIUM -> PriorityMediumColor
        ReminderPriority.HIGH -> PriorityHighColor
        ReminderPriority.URGENT -> PriorityUrgentColor
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = color.copy(alpha = 0.16f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.4f))
    ) {
        Text(
            text = priority.label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = color,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun SubScreenTopBar(
    title: String,
    subtitle: String? = null,
    onBack: () -> Unit,
    trailingAction: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.testTag("subscreen_back_btn")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Navigate back"
            )
        }
        Spacer(Modifier.width(4.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (trailingAction != null) {
            trailingAction()
        }
    }
}

@Composable
fun EmptyStatePanel(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionButton: @Composable (() -> Unit)? = null
) {
    RRCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (actionButton != null) {
                Spacer(Modifier.size(4.dp))
                actionButton()
            }
        }
    }
}
