package com.appenheimer.dailyflow.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.appenheimer.dailyflow.data.DailyFlowStore
import com.appenheimer.dailyflow.model.DailyTask
import com.appenheimer.dailyflow.model.greetingForNow
import com.appenheimer.dailyflow.model.sortedTasks
import com.appenheimer.dailyflow.ui.AppSection
import com.appenheimer.dailyflow.ui.components.EmptyState
import com.appenheimer.dailyflow.ui.components.ScreenList
import com.appenheimer.dailyflow.ui.components.SectionCard
import com.appenheimer.dailyflow.ui.components.StatChip
import com.appenheimer.dailyflow.ui.components.TaskCompactRow

@Composable
fun HomeScreen(store: DailyFlowStore, navigate: (AppSection) -> Unit) {
    var taskEditor by remember { mutableStateOf(false) }
    var habitEditor by remember { mutableStateOf(false) }
    var noteEditor by remember { mutableStateOf(false) }

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
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(greetingForNow(), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Text("Today at a glance", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Keep the day clear: choose a few tasks, check in on habits, and capture the details worth saving.")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatChip("${store.activeTaskCount} active")
                        StatChip("${store.doneHabitsTodayCount}/${store.habits.size} habits")
                    }
                }
            }
        }
        item { WarningCard(store.dataWarning) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                MetricCard("Active", store.activeTaskCount.toString(), "tasks", Icons.Filled.CheckCircle, Modifier.weight(1f)) { navigate(AppSection.TASKS) }
                MetricCard("Completed", store.completedTaskCount.toString(), "tasks", Icons.Filled.Check, Modifier.weight(1f)) { navigate(AppSection.TASKS) }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                MetricCard("Habits", "${store.doneHabitsTodayCount}/${store.habits.size}", "today", Icons.Filled.Favorite, Modifier.weight(1f)) { navigate(AppSection.HABITS) }
                MetricCard("Notes", store.noteCount.toString(), "local", Icons.Filled.Edit, Modifier.weight(1f)) { navigate(AppSection.NOTES) }
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
                        Text("Habit")
                    }
                    OutlinedButton(onClick = { if (store.canAddNote()) noteEditor = true else store.notice = store.limitMessage("notes") }, modifier = Modifier.weight(1f)) {
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
                    EmptyState(Icons.Filled.Star, "Nothing urgent yet", "Add a high priority task to turn this into a focused daily plan.")
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
private fun MetricCard(title: String, value: String, subtitle: String, icon: ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    ElevatedCard(modifier = modifier.clickable(onClick = onClick), shape = RoundedCornerShape(8.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun TodayProgressCard(store: DailyFlowStore, onOpenHabits: () -> Unit) {
    val totalHabits = store.habits.size.coerceAtLeast(1)
    val progress = store.doneHabitsTodayCount.toFloat() / totalHabits.toFloat()
    SectionCard(title = "Daily habit progress", actionLabel = "Habits", onAction = onOpenHabits) {
        Text("${store.doneHabitsTodayCount} of ${store.habits.size} habits done")
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
        Text(
            if (store.habits.isEmpty()) "Add a habit to start seeing daily progress." else "Missed days reset the current streak, but best streaks are saved.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
