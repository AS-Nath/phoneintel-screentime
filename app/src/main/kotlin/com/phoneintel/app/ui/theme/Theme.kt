package com.phoneintel.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ─── Brand Colors ─────────────────────────────────────────────────────────────

val IndigoBase = Color(0xFF5C6BC0)
val IndigoDark = Color(0xFF3949AB)
val IndigoLight = Color(0xFF9FA8DA)
val TealAccent = Color(0xFF26C6DA)
val CoralAccent = Color(0xFFFF7043)
val AmberAccent = Color(0xFFFFCA28)
val SurfaceDark = Color(0xFF121212)
val SurfaceVariantDark = Color(0xFF1E1E2E)
val CardDark = Color(0xFF252535)

// Chart colors
val ChartBlue = Color(0xFF5C6BC0)
val ChartTeal = Color(0xFF26C6DA)
val ChartCoral = Color(0xFFFF7043)
val ChartAmber = Color(0xFFFFCA28)
val ChartGreen = Color(0xFF66BB6A)
val ChartPurple = Color(0xFFAB47BC)

private val DarkColorScheme = darkColorScheme(
    primary = IndigoBase,
    onPrimary = Color.White,
    primaryContainer = IndigoDark,
    onPrimaryContainer = IndigoLight,
    secondary = TealAccent,
    onSecondary = Color.Black,
    tertiary = CoralAccent,
    background = SurfaceDark,
    surface = SurfaceVariantDark,
    surfaceVariant = CardDark,
    onBackground = Color(0xFFE8E8F0),
    onSurface = Color(0xFFE8E8F0),
    onSurfaceVariant = Color(0xFFB0B0C8)
)

private val LightColorScheme = lightColorScheme(
    primary = IndigoDark,
    onPrimary = Color.White,
    primaryContainer = IndigoLight,
    onPrimaryContainer = IndigoDark,
    secondary = Color(0xFF0097A7),
    onSecondary = Color.White,
    tertiary = CoralAccent,
    background = Color(0xFFF5F5FA),
    surface = Color.White,
    surfaceVariant = Color(0xFFEEEEF8),
    onBackground = Color(0xFF1A1A2E),
    onSurface = Color(0xFF1A1A2E),
    onSurfaceVariant = Color(0xFF4A4A6A)
)

@Composable
fun PhoneIntelTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            // Intentionally disabled — we want consistent branding
            if (darkTheme) DarkColorScheme else LightColorScheme
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
