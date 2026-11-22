package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

enum class ThemeMode(val label: String) {
    SYSTEM("System Default"),
    DARK("Deep Navy Dark"),
    LIGHT("Porcelain Light")
}

enum class ClockDesign(val label: String, val shortLabel: String) {
    CHRONO_DIGITAL("Chrono Digital", "Digital"),
    CLASSIC_ANALOG("Analog Watch", "Analog"),
    FLIP_DECK("Flip Deck", "Flip"),
    RADIAL_GAUGE("Orbital Ring", "Orbital")
}

val RRShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp)
)

@Composable
fun RRReminderTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    accentPalette: AccentPalette = AccentPalette.SAPPHIRE,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }

    val colorScheme = if (isDark) {
        darkColorScheme(
            primary = accentPalette.primaryDark,
            onPrimary = NavyVoid,
            primaryContainer = accentPalette.primaryDark.copy(alpha = 0.18f),
            onPrimaryContainer = CloudText,
            secondary = accentPalette.secondaryDark,
            onSecondary = NavyVoid,
            secondaryContainer = NavyElevated,
            onSecondaryContainer = CloudText,
            tertiary = SuccessEmerald,
            background = NavyVoid,
            onBackground = CloudText,
            surface = NavyDeep,
            onSurface = CloudText,
            surfaceVariant = NavySurface,
            onSurfaceVariant = SlateMuted,
            surfaceContainerLowest = NavyVoid,
            surfaceContainerLow = NavyDeep,
            surfaceContainer = NavySurface,
            surfaceContainerHigh = NavyElevated,
            outline = NavyBorder,
            outlineVariant = NavyBorder.copy(alpha = 0.5f),
            error = ErrorCrimson,
            onError = Color.White
        )
    } else {
        lightColorScheme(
            primary = accentPalette.primaryLight,
            onPrimary = Color.White,
            primaryContainer = accentPalette.primaryLight.copy(alpha = 0.12f),
            onPrimaryContainer = InkNavy,
            secondary = accentPalette.secondaryLight,
            onSecondary = Color.White,
            secondaryContainer = MistSurfaceVariant,
            onSecondaryContainer = InkNavy,
            tertiary = SuccessEmerald,
            background = PorcelainBg,
            onBackground = InkNavy,
            surface = FrostSurface,
            onSurface = InkNavy,
            surfaceVariant = MistSurfaceVariant,
            onSurfaceVariant = InkMuted,
            surfaceContainerLowest = FrostSurface,
            surfaceContainerLow = PorcelainBg,
            surfaceContainer = FrostSurface,
            surfaceContainerHigh = MistSurfaceVariant,
            outline = LightBorder,
            outlineVariant = LightBorder.copy(alpha = 0.6f),
            error = ErrorCrimson,
            onError = Color.White
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDark
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !isDark
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = RRTypography,
        shapes = RRShapes,
        content = content
    )
}
