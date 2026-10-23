package com.example.fantasmandroid.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val FantasmColorScheme = darkColorScheme(
    primary = AccentGreen,
    onPrimary = Ink,
    secondary = AccentCyan,
    onSecondary = Ink,
    tertiary = Amber,
    background = Ink,
    onBackground = IceWhite,
    surface = Ink,
    onSurface = IceWhite,
    surfaceVariant = PanelBlack,
    onSurfaceVariant = TextDim,
    outline = Hairline,
    error = WarmRed
)

@Composable
fun FantasmAndroidTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = FantasmColorScheme,
        typography = FantasmTypography,
        content = content
    )
}
