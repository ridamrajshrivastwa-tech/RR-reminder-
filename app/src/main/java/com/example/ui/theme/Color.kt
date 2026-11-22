package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Deep Navy & Obsidian Dark Palette
val NavyVoid = Color(0xFF070C17)
val NavyDeep = Color(0xFF0B1324)
val NavySurface = Color(0xFF111B30)
val NavyElevated = Color(0xFF18243E)
val NavyBorder = Color(0xFF263554)
val SlateMuted = Color(0xFF94A3B8)
val CloudText = Color(0xFFF1F5F9)

// Porcelain & Crisp Slate Light Palette
val PorcelainBg = Color(0xFFF4F7FC)
val FrostSurface = Color(0xFFFFFFFF)
val MistSurfaceVariant = Color(0xFFE8EEF7)
val LightBorder = Color(0xFFD5DFEE)
val InkNavy = Color(0xFF0F172A)
val InkMuted = Color(0xFF475569)

// Accent Options (10 curated premium palettes)
enum class AccentPalette(
    val label: String,
    val primaryDark: Color,
    val primaryLight: Color,
    val secondaryDark: Color,
    val secondaryLight: Color
) {
    SAPPHIRE(
        label = "Sapphire Blue",
        primaryDark = Color(0xFF4F8EF7),
        primaryLight = Color(0xFF2563EB),
        secondaryDark = Color(0xFF38BDF8),
        secondaryLight = Color(0xFF0284C7)
    ),
    CYAN(
        label = "Electric Cyan",
        primaryDark = Color(0xFF22D3EE),
        primaryLight = Color(0xFF0891B2),
        secondaryDark = Color(0xFF38BDF8),
        secondaryLight = Color(0xFF0284C7)
    ),
    INDIGO(
        label = "Midnight Indigo",
        primaryDark = Color(0xFF818CF8),
        primaryLight = Color(0xFF4F46E5),
        secondaryDark = Color(0xFFA5B4FC),
        secondaryLight = Color(0xFF6366F1)
    ),
    AMETHYST(
        label = "Royal Amethyst",
        primaryDark = Color(0xFFC084FC),
        primaryLight = Color(0xFF9333EA),
        secondaryDark = Color(0xFFE879F9),
        secondaryLight = Color(0xFFC026D3)
    ),
    EMERALD(
        label = "Arctic Teal",
        primaryDark = Color(0xFF2DD4BF),
        primaryLight = Color(0xFF0D9488),
        secondaryDark = Color(0xFF34D399),
        secondaryLight = Color(0xFF059669)
    ),
    AURORA(
        label = "Aurora Mint",
        primaryDark = Color(0xFF34D399),
        primaryLight = Color(0xFF059669),
        secondaryDark = Color(0xFF6EE7B7),
        secondaryLight = Color(0xFF10B981)
    ),
    AMBER(
        label = "Solar Amber",
        primaryDark = Color(0xFFFBBF24),
        primaryLight = Color(0xFFD97706),
        secondaryDark = Color(0xFFF59E0B),
        secondaryLight = Color(0xFFB45309)
    ),
    GOLD(
        label = "Champagne Gold",
        primaryDark = Color(0xFFFCD34D),
        primaryLight = Color(0xFFB45309),
        secondaryDark = Color(0xFFFBBF24),
        secondaryLight = Color(0xFFD97706)
    ),
    ROSE(
        label = "Crimson Coral",
        primaryDark = Color(0xFFFB7185),
        primaryLight = Color(0xFFE11D48),
        secondaryDark = Color(0xFFF472B6),
        secondaryLight = Color(0xFFDB2777)
    ),
    COBALT(
        label = "Neon Cobalt",
        primaryDark = Color(0xFF60A5FA),
        primaryLight = Color(0xFF1D4ED8),
        secondaryDark = Color(0xFFA78BFA),
        secondaryLight = Color(0xFF6D28D9)
    )
}

// Semantic Status Colors
val PriorityLowColor = Color(0xFF38BDF8)
val PriorityMediumColor = Color(0xFF3B82F6)
val PriorityHighColor = Color(0xFFF59E0B)
val PriorityUrgentColor = Color(0xFFEF4444)
val SuccessEmerald = Color(0xFF10B981)
val WarningAmber = Color(0xFFF59E0B)
val ErrorCrimson = Color(0xFFEF4444)

// Reminder Color Swatches
val ReminderColorOptions = listOf(
    0xFF3B82F6.toInt(), // Cobalt
    0xFF6366F1.toInt(), // Indigo
    0xFF14B8A6.toInt(), // Teal
    0xFF10B981.toInt(), // Emerald
    0xFFF59E0B.toInt(), // Amber
    0xFFEF4444.toInt(), // Crimson
    0xFFEC4899.toInt(), // Rose
    0xFF8B5CF6.toInt(), // Violet
    0xFF06B6D4.toInt()  // Cyan
)
