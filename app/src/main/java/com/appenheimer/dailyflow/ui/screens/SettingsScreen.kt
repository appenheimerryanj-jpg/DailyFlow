package com.appenheimer.dailyflow.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.appenheimer.dailyflow.BuildConfig
import com.appenheimer.dailyflow.data.DailyFlowStore
import com.appenheimer.dailyflow.model.FocusVibe
import com.appenheimer.dailyflow.ui.components.FlowMascot
import com.appenheimer.dailyflow.ui.components.FlowPose
import com.appenheimer.dailyflow.ui.components.ScreenHeader
import com.appenheimer.dailyflow.ui.components.ScreenList
import com.appenheimer.dailyflow.ui.components.SectionCard

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(store: DailyFlowStore) {
    var showResetDialog by remember { mutableStateOf(false) }

    if (showResetDialog) {
        ConfirmDialog(
            title = "Reset all local data?",
            text = "This permanently clears tasks, habits, and notes stored on this device. Premium status is kept.",
            confirmText = "Reset all",
            onDismiss = { showResetDialog = false },
            onConfirm = {
                store.resetData()
                showResetDialog = false
            }
        )
    }

    ScreenList {
        item { ScreenHeader("Settings", "App details, local data controls, and privacy.") }
        item {
            SectionCard(title = "Local status") {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FlowMascot(if (store.premium) FlowPose.CELEBRATE else FlowPose.HAPPY, modifier = Modifier.size(72.dp), reducedMotion = store.reducedMotion)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("DailyFlow is running locally on this device.", fontWeight = FontWeight.SemiBold)
                        Text("${if (store.premium) "Premium active" else "Free plan active"} - ${store.totalMomentumPoints} total XP", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                SettingsRow("Name", "DailyFlow")
                SettingsRow("Version", BuildConfig.VERSION_NAME)
                SettingsRow("Premium", if (store.premium) "Active" else "Free plan")
            }
        }
        item {
            SectionCard(title = "Sound and motion") {
                SettingsToggleRow(
                    title = "Sound effects",
                    value = "Short local sounds for wins, limits, and focus starts.",
                    checked = store.soundEffectsEnabled,
                    onCheckedChange = { store.updateSoundEffectsEnabled(it) }
                )
                SettingsToggleRow(
                    title = "Celebration effects",
                    value = "Flow reward cards, sparkles, and extra success motion.",
                    checked = store.celebrationEffectsEnabled,
                    onCheckedChange = { store.updateCelebrationEffectsEnabled(it) }
                )
                SettingsToggleRow(
                    title = "Reduced motion",
                    value = "Keeps Flow expressive but tones down looping movement.",
                    checked = store.reducedMotion,
                    onCheckedChange = { store.updateReducedMotion(it) }
                )
                OutlinedButton(onClick = { store.previewSound() }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Preview sound")
                }
            }
        }
        item {
            SectionCard(title = "Music profile") {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FlowMascot(FlowPose.MUSIC, modifier = Modifier.size(68.dp), reducedMotion = store.reducedMotion)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Focus vibe", fontWeight = FontWeight.Bold)
                        Text(store.selectedFocusVibe.description, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FocusVibe.entries.forEach { vibe ->
                        FilterChip(
                            selected = store.selectedFocusVibe == vibe,
                            onClick = { store.setFocusVibe(vibe) },
                            label = { Text(vibe.label) }
                        )
                    }
                }
                Row(verticalAlignment = Alignment.Top) {
                    Icon(Icons.Filled.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Text(store.musicProfileMessage(), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            SectionCard(title = "About DailyFlow") {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(Icons.Filled.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Text("DailyFlow is an offline-first day planner with Flow, a calm companion for tasks, habit streaks, and quick notes. Plan better. Build momentum.")
                }
            }
        }
        item {
            SectionCard(title = "Privacy") {
                Text("Data is stored locally on this device unless a future sync feature is added.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            SectionCard(title = "Backup placeholders") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { store.exportPlaceholder() }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.Info, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Export data")
                    }
                    OutlinedButton(onClick = { store.importPlaceholder() }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Import data")
                    }
                }
            }
        }
        item {
            SectionCard(title = "Data") {
                Text("Use reset only when you want a completely fresh local workspace.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedButton(onClick = { showResetDialog = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Reset all data")
                }
            }
        }
    }
}

@Composable
private fun SettingsRow(title: String, value: String) {
    ListItem(
        headlineContent = { Text(title, fontWeight = FontWeight.SemiBold) },
        supportingContent = { Text(value) }
    )
}

@Composable
private fun SettingsToggleRow(title: String, value: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.Waves, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(value, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
