package com.jipzeongit.arcsync.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.jipzeongit.arcsync.data.SettingsRepository

private val DarkColors = darkColorScheme(
    primary = Color(0xFF4DD0E1),
    onPrimary = Color(0xFF00363A),
    secondary = Color(0xFF80CBC4),
    onSecondary = Color(0xFF00332E),
    background = Color(0xFF0B0F12),
    onBackground = Color(0xFFE6EDF3),
    surface = Color(0xFF121820),
    onSurface = Color(0xFFE6EDF3),
    surfaceVariant = Color(0xFF1D2733),
    onSurfaceVariant = Color(0xFFB6C2CF)
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF006D75),
    onPrimary = Color.White,
    secondary = Color(0xFF4F6367),
    onSecondary = Color.White,
    background = Color(0xFFF4F6F8),
    onBackground = Color(0xFF0E1116),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0E1116),
    surfaceVariant = Color(0xFFE3E7EB),
    onSurfaceVariant = Color(0xFF3E4A54)
)

@Composable
fun ArcSyncTheme(mode: SettingsRepository.ThemeMode, content: @Composable () -> Unit) {
    val useDark = when (mode) {
        SettingsRepository.ThemeMode.ON -> true
        SettingsRepository.ThemeMode.OFF -> false
        SettingsRepository.ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val colors = if (useDark) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colors,
        typography = ArcSyncTypography,
        content = content
    )
}
