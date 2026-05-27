package com.appenheimer.dailyflow.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.appenheimer.dailyflow.data.DailyFlowStore
import com.appenheimer.dailyflow.model.DailyTask
import com.appenheimer.dailyflow.model.greetingForNow
import com.appenheimer.dailyflow.model.sortedTasks
import com.appenheimer.dailyflow.ui.AppSection
import com.appenheimer.dailyflow.ui.components.AnimatedProgressBar
import com.appenheimer.dailyflow.ui.components.EmptyState
import com.appenheimer.dailyflow.ui.components.FlowMascot
import com.appenheimer.dailyflow.ui.components.FlowPose
import com.appenheimer.dailyflow.ui.components.ScreenList
import com.appenheimer.dailyflow.ui.components.SectionCard
import com.appenheimer.dailyflow.ui.components.SparkleBurst
import com.appenheimer.dailyflow.ui.components.StatChip
import com.appenheimer.dailyflow.ui.components.TaskCompactRow
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(store: DailyFlowStore, navigate: (AppSection) -> Unit) {
    var taskEditor by remember { mutableStateOf(false) }
    var habitEditor by remember { mutableStateOf(false) }
    var noteEditor by remember { mutableStateOf(false) }
    val flowMessages = remember {
        listOf(
            "Flow says: start with one clear win.",
            "Flow says: progress counts, even when it is quiet.",
            "Flow says: keep today light enough to finish.",
            "Flow says: small plans make real momentum."
        )
    }
    var flowMessageIndex by remember {
        mutableIntStateOf(((System.currentTimeMillis() / 86_400_000L) % flowMessages.size).toInt())
    }
    LaunchedEffect(Unit) {
        while (true) {
            delay(7_000)
            flowMessageIndex = (flowMessageIndex + 1) % flowMessages.size
        }
    }
    val flowMessage = flowMessages[flowMessageIndex]

    if (taskEditor) {
        TaskEditorDialog(
            task = null,
            onDismiss = { taskEditor = false },
            onSave = { text, priority, dueDate ->
                if (store.upsertTask(null, text, priority, dueDate)) taskEditor = false
            }
        )
    }
    if (habitEditor) {
        HabitEditorDialog(
            habit = null,
            onDismiss = { habitEditor = false },
            onSave = { name ->
                if (store.upsertHabit(null, name)) habitEditor = false
            }
        )
    }
    if (noteEditor) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { noteEditor = false },
            title = { Text("Add note") },
            text = {
                NoteEditor(
                    note = null,
                    onCancel = { noteEditor = false },
                    onSave = { title, body ->
                        if (store.upsertNote(null, title, body)) noteEditor = false
                    }
                )
            },
            confirmButton = {},
            dismissButton = {}
        )
    }

    ScreenList {
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(greetingForNow(), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        Text("Flow is ready for today", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text("Plan better. Build momentum.")
                        AnimatedContent(
                            targetState = flowMessage,
                            transitionSpec = {
                                (fadeIn(tween(220)) + slideInVertically(tween(220)) { it / 3 }) togetherWith
                                    (fadeOut(tween(180)) + slideOutVertically(tween(180)) { -it / 3 })
                            },
                            label = "FlowMessage"
                        ) { message ->
                            Text(message, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatChip("${store.activeTaskCount} active")
                            StatChip("${store.doneHabitsTodayCount}/${store.habits.size} habits")
                        }
                    }
                    FlowMascot(FlowPose.HAPPY, modifier = Modifier.size(104.dp))
                }
            }
        }
        item { WarningCard(store.dataWarning) }
        item {
            SectionCard(title = "Today's Flow") {
                val activeFocus = sortedTasks(store.tasks.filterNot { it.done }).take(4).size
                FlowStatRow("Tasks completed", store.completedTaskCount.toString(), Icons.Filled.Check) { navigate(AppSection.TASKS) }
                FlowStatRow("Active focus tasks", activeFocus.toString(), Icons.Filled.Star) { navigate(AppSection.TASKS) }
                FlowStatRow("Habits completed today", "${store.doneHabitsTodayCount}/${store.habits.size}", Icons.Filled.Favorite) { navigate(AppSection.HABITS) }
                FlowStatRow("Notes captured", store.noteCount.toString(), Icons.Filled.Edit) { navigate(AppSection.NOTES) }
                val totalTasks = (store.activeTaskCount + store.completedTaskCount).coerceAtLeast(1)
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    AnimatedProgressBar(store.completedTaskCount.toFloat() / totalTasks.toFloat(), Modifier.fillMaxWidth())
                    SparkleBurst(
                        trigger = (store.completedTaskCount + store.doneHabitsTodayCount).toLong(),
                        modifier = Modifier.size(64.dp)
                    )
                }
            }
        }
        item {
            SectionCard(title = "Quick actions") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(onClick = { if (store.canAddTask()) taskEditor = true else store.notice = store.limitMessage("tasks") }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Task")
                    }
                    OutlinedButton(onClick = { if (store.canAddHabit()) habitEditor = true else store.notice = store.limitMessage("habits") }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.Favorite, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Habit")
                    }
                    OutlinedButton(onClick = { if (store.canAddNote()) noteEditor = true else store.notice = store.limitMessage("notes") }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Note")
                    }
                }
            }
        }
        item {
            TodayProgressCard(store) { navigate(AppSection.HABITS) }
        }
        item {
            SectionCard(title = "Focus for today", actionLabel = "Tasks", onAction = { navigate(AppSection.TASKS) }) {
                val focusTasks: List<DailyTask> = sortedTasks(store.tasks.filterNot { it.done }).take(4)
                if (focusTasks.isEmpty()) {
                    EmptyState(Icons.Filled.Star, "Nothing urgent yet", "Flow will surface high priority active tasks here when you add them.", FlowPose.EMPTY)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        focusTasks.forEach { task -> TaskCompactRow(task) { navigate(AppSection.TASKS) } }
                    }
                }
            }
        }
    }
}

@Composable
private fun WarningCard(message: String?) {
    if (message.isNullOrBlank()) return
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            Spacer(Modifier.width(12.dp))
            Text(message, color = MaterialTheme.colorScheme.onErrorContainer)
        }
    }
}

@Composable
private fun FlowStatRow(title: String, value: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(12.dp))
        Text(title, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun TodayProgressCard(store: DailyFlowStore, onOpenHabits: () -> Unit) {
    val totalHabits = store.habits.size.coerceAtLeast(1)
    val progress = store.doneHabitsTodayCount.toFloat() / totalHabits.toFloat()
    SectionCard(title = "Daily habit progress", actionLabel = "Habits", onAction = onOpenHabits) {
        Text("${store.doneHabitsTodayCount} of ${store.habits.size} habits done")
        Spacer(Modifier.height(8.dp))
        AnimatedProgressBar(progress = progress, modifier = Modifier.fillMaxWidth())
        Text(
            if (store.habits.isEmpty()) "Flow will show daily progress as soon as you add a habit." else "Missed days reset the current streak, but best streaks are saved.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
