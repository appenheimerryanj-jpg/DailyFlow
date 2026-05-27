package com.appenheimer.dailyflow.ui

import android.app.Activity
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.appenheimer.dailyflow.data.DailyFlowStore
import com.appenheimer.dailyflow.ui.components.FlowDelightOverlay
import com.appenheimer.dailyflow.ui.components.FlowMascot
import com.appenheimer.dailyflow.ui.components.FlowMotion
import com.appenheimer.dailyflow.ui.components.FlowPose
import com.appenheimer.dailyflow.ui.screens.HabitsScreen
import com.appenheimer.dailyflow.ui.screens.HomeScreen
import com.appenheimer.dailyflow.ui.screens.NotesScreen
import com.appenheimer.dailyflow.ui.screens.OnboardingScreen
import com.appenheimer.dailyflow.ui.screens.PremiumScreen
import com.appenheimer.dailyflow.ui.screens.SettingsScreen
import com.appenheimer.dailyflow.ui.screens.TasksScreen
import com.appenheimer.dailyflow.ui.theme.DailyFlowTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyFlowApp() {
    val activity = LocalContext.current as Activity
    val store = remember { DailyFlowStore(activity) }
    val snackbarHostState = remember { SnackbarHostState() }
    var sectionName by rememberSaveable { mutableStateOf(AppSection.HOME.name) }
    val currentSection = AppSection.valueOf(sectionName)

    LaunchedEffect(store.notice) {
        val message = store.notice
        if (!message.isNullOrBlank()) {
            snackbarHostState.showSnackbar(message)
            store.notice = null
        }
    }

    DisposableEffect(store) {
        onDispose { store.close() }
    }

    DailyFlowTheme {
        if (!store.onboardingCompleted) {
            OnboardingScreen(onFinish = { store.completeOnboarding() })
            return@DailyFlowTheme
        }

        Box(Modifier.fillMaxSize()) {
            Scaffold(
                snackbarHost = {
                    SnackbarHost(snackbarHostState) { data ->
                        Snackbar(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                FlowMascot(FlowPose.HAPPY, modifier = Modifier.size(36.dp), motion = FlowMotion.HAPPY)
                                Spacer(Modifier.width(10.dp))
                                Text(data.visuals.message, color = androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }
                    }
                },
                topBar = {
                    TopAppBar(
                        title = {
                            Column {
                                Text("DailyFlow", fontWeight = FontWeight.Bold)
                                Text(currentSection.label, style = androidx.compose.material3.MaterialTheme.typography.labelMedium)
                            }
                        },
                        actions = {
                            AssistChip(
                                onClick = { sectionName = AppSection.PREMIUM.name },
                                label = { Text(if (store.premium) "Premium" else "Free") },
                                leadingIcon = {
                                    Icon(
                                        if (store.premium) Icons.Filled.WorkspacePremium else Icons.Filled.Lock,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface
                        )
                    )
                },
                bottomBar = {
                    NavigationBar(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface) {
                        AppSection.entries.forEach { item ->
                            val selected = currentSection == item
                            val iconScale by animateFloatAsState(
                                targetValue = if (selected) 1.16f else 1f,
                                animationSpec = tween(180),
                                label = "BottomNavIcon"
                            )
                            NavigationBarItem(
                                selected = selected,
                                onClick = { sectionName = item.name },
                                icon = {
                                    Icon(
                                        item.icon,
                                        contentDescription = item.label,
                                        modifier = Modifier.graphicsLayer {
                                            scaleX = iconScale
                                            scaleY = iconScale
                                        }
                                    )
                                },
                                label = { Text(item.label) }
                            )
                        }
                    }
                }
            ) { padding ->
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.background
                ) {
                    when (currentSection) {
                        AppSection.HOME -> HomeScreen(store) { sectionName = it.name }
                        AppSection.TASKS -> TasksScreen(store)
                        AppSection.HABITS -> HabitsScreen(store)
                        AppSection.NOTES -> NotesScreen(store)
                        AppSection.PREMIUM -> PremiumScreen(store)
                        AppSection.SETTINGS -> SettingsScreen(store)
                    }
                }
            }
            FlowDelightOverlay(event = store.delight, onFinished = { store.delight = null })
        }
    }
}
