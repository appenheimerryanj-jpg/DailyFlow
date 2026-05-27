package com.appenheimer.dailyflow.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.appenheimer.dailyflow.BuildConfig
import com.appenheimer.dailyflow.data.DailyFlowStore
import com.appenheimer.dailyflow.model.FREE_ACTIVE_TASK_LIMIT
import com.appenheimer.dailyflow.model.FREE_HABIT_LIMIT
import com.appenheimer.dailyflow.model.FREE_NOTE_LIMIT
import com.appenheimer.dailyflow.model.PREMIUM_PRODUCT_ID
import com.appenheimer.dailyflow.ui.components.FeatureLine
import com.appenheimer.dailyflow.ui.components.ScreenHeader
import com.appenheimer.dailyflow.ui.components.ScreenList
import com.appenheimer.dailyflow.ui.components.SectionCard

@Composable
fun PremiumScreen(store: DailyFlowStore) {
    ScreenList {
        item {
            ScreenHeader(
                title = "DailyFlow Premium",
                subtitle = "A one-time lifetime unlock for people who want more room."
            )
        }
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = if (store.premium) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(
                        if (store.premium) Icons.Filled.WorkspacePremium else Icons.Filled.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(44.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(if (store.premium) "Premium is active" else "Unlock unlimited DailyFlow", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(
                        if (store.premium) "Unlimited local tasks, habits, and notes are active on this device." else "Free is enough to start. Premium removes limits and supports future development.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    AssistChip(onClick = {}, enabled = false, label = { Text(if (store.billingLoading) "Billing connecting" else if (store.billingReady) "Billing ready" else "Billing unavailable") })
                }
            }
        }
        item {
            SectionCard(title = "Free vs Premium") {
                FeatureLine("Free: $FREE_ACTIVE_TASK_LIMIT active tasks, $FREE_HABIT_LIMIT habits, $FREE_NOTE_LIMIT notes")
                FeatureLine("Premium: unlimited tasks, habits, and notes")
                FeatureLine("All data stays offline-first on this device")
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                BenefitCard("Unlimited tasks", "Plan without active task caps.", Icons.Filled.CheckCircle, Modifier.weight(1f))
                BenefitCard("Unlimited habits", "Track more routines and streaks.", Icons.Filled.Favorite, Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                BenefitCard("Unlimited notes", "Capture more searchable notes.", Icons.Filled.Edit, Modifier.weight(1f))
                BenefitCard("Future support", "Help fund improvements.", Icons.Filled.Star, Modifier.weight(1f))
            }
        }
        item {
            SectionCard(title = "Unlock") {
                Text(store.purchaseStatus, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(onClick = { store.buyPremium() }, enabled = !store.premium, modifier = Modifier.fillMaxWidth()) {
                    Text(if (store.premium) "Premium active" else "Unlock Premium")
                }
                OutlinedButton(onClick = { store.restorePurchases() }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Restore purchases")
                }
                if (BuildConfig.DEBUG) {
                    OutlinedButton(onClick = { store.devUnlock() }, modifier = Modifier.fillMaxWidth()) {
                        Text("Debug/testing only: unlock premium")
                    }
                }
                Text("Product ID: $PREMIUM_PRODUCT_ID", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}

@Composable
private fun BenefitCard(title: String, body: String, icon: ImageVector, modifier: Modifier = Modifier) {
    ElevatedCard(modifier = modifier, shape = RoundedCornerShape(8.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(title, fontWeight = FontWeight.Bold)
            Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
