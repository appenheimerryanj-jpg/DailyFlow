package com.appenheimer.dailyflow.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DailyFlowColors = lightColorScheme(
    primary = Color(0xFF176B5B),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD8F1E9),
    onPrimaryContainer = Color(0xFF08251F),
    secondary = Color(0xFF59628D),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0E4FF),
    onSecondaryContainer = Color(0xFF171B35),
    tertiary = Color(0xFFB65C00),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDCC1),
    onTertiaryContainer = Color(0xFF341100),
    background = Color(0xFFF8FAF7),
    surface = Color(0xFFFEFFFC),
    surfaceVariant = Color(0xFFE4EAE4),
    outline = Color(0xFF727971),
    error = Color(0xFFB3261E),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002)
)

@Composable
fun DailyFlowTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DailyFlowColors, content = content)
}
