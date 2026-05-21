package com.mameen.isomessage.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * ISO8583 POS Simulator — FinTech Dark Theme
 *
 * Always dark — payment terminals are typically used in various lighting conditions
 * and a dark theme reduces eye strain while giving a professional, premium appearance.
 * Dynamic color is disabled to maintain the intentional FinTech brand palette.
 */
private val PosColorScheme = darkColorScheme(
    primary            = PosCyan400,
    onPrimary          = PosNavy900,
    primaryContainer   = PosNavy600,
    onPrimaryContainer = PosCyan200,

    secondary            = PosGreen500,
    onSecondary          = PosNavy900,
    secondaryContainer   = PosNavy700,
    onSecondaryContainer = PosGreen300,

    tertiary            = PosAmber400,
    onTertiary          = PosNavy900,
    tertiaryContainer   = PosNavy700,
    onTertiaryContainer = PosAmber400,

    error            = PosRed500,
    onError          = PosTextPrimary,
    errorContainer   = Color(0xFF4A1010),
    onErrorContainer = PosRed300,

    background   = PosNavy900,
    onBackground = PosTextPrimary,

    surface          = PosSurface,
    onSurface        = PosTextPrimary,
    surfaceVariant   = PosCardBg,
    onSurfaceVariant = PosTextSecondary,

    outline        = PosTextHint,
    outlineVariant = PosNavy500,

    inverseSurface   = PosTextPrimary,
    inverseOnSurface = PosNavy800,
    inversePrimary   = PosNavy700
)

@Composable
fun ISOMessageTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = PosColorScheme,
        typography = Typography,
        content = content
    )
}
