package com.appenheimer.dailyflow.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DailyFlowColors = lightColorScheme(
    primary = Color(0xFF007C89),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB9F2F7),
    onPrimaryContainer = Color(0xFF002022),
    secondary = Color(0xFF2D6F8F),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFC8E7FF),
    onSecondaryContainer = Color(0xFF001E2D),
    tertiary = Color(0xFF5D63C8),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE2E0FF),
    onTertiaryContainer = Color(0xFF17134B),
    background = Color(0xFFF4FBFC),
    surface = Color(0xFFFCFEFF),
    surfaceVariant = Color(0xFFDCEFF2),
    outline = Color(0xFF6C7D80),
    error = Color(0xFFB3261E),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002)
)

@Composable
fun DailyFlowTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DailyFlowColors, content = content)
}
