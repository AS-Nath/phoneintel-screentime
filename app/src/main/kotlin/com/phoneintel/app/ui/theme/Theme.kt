package com.phoneintel.app.ui.theme

import android.app.Activity
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ─── Figma Design System ──────────────────────────────────────────────────────
// Dark forest green palette — not black, not indigo. Everything is intentional.

// Backgrounds
val BgDeep       = Color(0xFF081510)   // deepest background
val BgBase       = Color(0xFF0A1A12)   // main background
val BgCard       = Color(0xFF0F2218)   // card surface
val BgCardLight  = Color(0xFF152E20)   // elevated card / selected state

// Accent — mint green, the single brand colour
val Mint         = Color(0xFF3DEBA8)   // primary CTA, scores, highlights
val MintDim      = Color(0xFF1A7A54)   // muted mint for secondary elements
val MintSubtle   = Color(0xFF0F3D28)   // mint tint backgrounds

// Text
val TextPrimary   = Color(0xFFEEF2EE)  // near-white
val TextSecondary = Color(0xFF7A9A84)  // muted green-grey
val TextDim       = Color(0xFF3D5C47)  // very muted, labels

// Semantic
val CoralAccent  = Color(0xFFFF6B6B)   // alert / destructive
val AmberAccent  = Color(0xFFFFB547)   // warning
val ChartAmber   = Color(0xFFFFB547)
val ChartGreen   = Mint
val ChartCoral   = CoralAccent
val ChartPurple  = Color(0xFFAB7FE8)
val ChartBlue    = Color(0xFF5BA4CF)
val ChartTeal    = Mint

// Legacy aliases so existing screens that import these don't break
val IndigoBase   = Color(0xFF3DEBA8)
val IndigoDark   = Color(0xFF1A7A54)
val IndigoLight  = Color(0xFF7DCCA8)
val TealAccent   = Mint

// ─── Color Scheme ─────────────────────────────────────────────────────────────

private val PhoneIntelColorScheme = darkColorScheme(
    primary             = Mint,
    onPrimary           = BgDeep,
    primaryContainer    = MintSubtle,
    onPrimaryContainer  = Mint,
    secondary           = MintDim,
    onSecondary         = TextPrimary,
    tertiary            = CoralAccent,
    background          = BgBase,
    surface             = BgCard,
    surfaceVariant      = BgCardLight,
    onBackground        = TextPrimary,
    onSurface           = TextPrimary,
    onSurfaceVariant    = TextSecondary,
    outline             = TextDim
)

@Composable
fun PhoneIntelTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = BgBase.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }
    MaterialTheme(
        colorScheme = PhoneIntelColorScheme,
        typography = Typography,
        content = content
    )
}