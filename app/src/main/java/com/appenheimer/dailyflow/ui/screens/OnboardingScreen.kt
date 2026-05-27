package com.appenheimer.dailyflow.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.appenheimer.dailyflow.ui.components.FlowMascot
import com.appenheimer.dailyflow.ui.components.FlowPose
import com.appenheimer.dailyflow.ui.theme.DailyFlowTheme

private data class OnboardingPage(val title: String, val body: String, val pose: FlowPose)

@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val pages = listOf(
        OnboardingPage("Plan your day", "Flow helps you capture tasks, choose priorities, and keep due date notes close.", FlowPose.CHECKLIST),
        OnboardingPage("Build better habits", "Check in daily, protect streaks, and let small wins build momentum.", FlowPose.CELEBRATE),
        OnboardingPage("Capture quick thoughts", "Save searchable notes with titles and body text. Everything stays local for now.", FlowPose.NOTE)
    )
    var page by remember { mutableIntStateOf(0) }
    val current = pages[page]

    DailyFlowTheme {
        Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
                Column(verticalArrangement = Arrangement.spacedBy(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(
                            Modifier.fillMaxWidth().padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            FlowMascot(current.pose, modifier = Modifier.size(128.dp))
                            Text("DailyFlow", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            Text(current.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                            Text("Plan better. Build momentum.", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(current.body, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        pages.indices.forEach { index ->
                            Surface(
                                color = if (index == page) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.size(width = if (index == page) 28.dp else 10.dp, height = 10.dp),
                                content = {}
                            )
                        }
                    }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = onFinish) { Text("Skip") }
                        Spacer(Modifier.weight(1f))
                        Button(onClick = { if (page == pages.lastIndex) onFinish() else page += 1 }) {
                            Text(if (page == pages.lastIndex) "Get Started" else "Next")
                        }
                    }
                }
            }
        }
    }
}
