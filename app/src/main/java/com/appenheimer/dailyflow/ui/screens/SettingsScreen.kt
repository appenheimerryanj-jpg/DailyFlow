package com.appenheimer.dailyflow.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.appenheimer.dailyflow.ui.components.ScreenHeader
import com.appenheimer.dailyflow.ui.components.ScreenList
import com.appenheimer.dailyflow.ui.components.SectionCard

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
            SectionCard(title = "App") {
                SettingsRow("Name", "DailyFlow")
                SettingsRow("Version", BuildConfig.VERSION_NAME)
                SettingsRow("Premium", if (store.premium) "Active" else "Free plan")
            }
        }
        item {
            SectionCard(title = "About DailyFlow") {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(Icons.Filled.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Text("DailyFlow is an offline-first day planner for practical tasks, habit streaks, and quick notes.")
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
