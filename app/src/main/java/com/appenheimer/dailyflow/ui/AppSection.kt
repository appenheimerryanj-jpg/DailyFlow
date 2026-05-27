package com.appenheimer.dailyflow.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.vector.ImageVector

enum class AppSection(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Filled.Home),
    TASKS("Tasks", Icons.Filled.CheckCircle),
    HABITS("Habits", Icons.Filled.Favorite),
    NOTES("Notes", Icons.Filled.Edit),
    PREMIUM("Premium", Icons.Filled.Star),
    SETTINGS("Settings", Icons.Filled.Settings)
}
