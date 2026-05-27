package com.appenheimer.dailyflow.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.appenheimer.dailyflow.data.DailyFlowStore
import com.appenheimer.dailyflow.model.FREE_HABIT_LIMIT
import com.appenheimer.dailyflow.model.Habit
import com.appenheimer.dailyflow.model.doneToday
import com.appenheimer.dailyflow.model.safeName
import com.appenheimer.dailyflow.ui.components.EmptyState
import com.appenheimer.dailyflow.ui.components.LimitCard
import com.appenheimer.dailyflow.ui.components.ScreenHeader
import com.appenheimer.dailyflow.ui.components.ScreenList
import com.appenheimer.dailyflow.ui.components.SectionCard

@Composable
fun HabitsScreen(store: DailyFlowStore) {
    var showEditor by remember { mutableStateOf(false) }
    var editingHabit by remember { mutableStateOf<Habit?>(null) }
    var deleteHabit by remember { mutableStateOf<Habit?>(null) }
    var resetHabit by remember { mutableStateOf<Habit?>(null) }

    if (showEditor) {
        HabitEditorDialog(
            habit = editingHabit,
            onDismiss = { showEditor = false },
            onSave = { name -> if (store.upsertHabit(editingHabit?.id, name)) showEditor = false }
        )
    }
    deleteHabit?.let { habit ->
        ConfirmDialog("Delete habit?", "This removes \"${habit.safeName()}\" and its streak data.", "Delete", { deleteHabit = null }) {
            store.deleteHabit(habit.id)
            deleteHabit = null
        }
    }
    resetHabit?.let { habit ->
        ConfirmDialog("Reset current streak?", "The current streak goes to 0, but the best streak stays for history.", "Reset", { resetHabit = null }) {
            store.resetHabit(habit)
            resetHabit = null
        }
    }

    ScreenList {
        item {
            ScreenHeader(
                title = "Habits",
                subtitle = "Check in daily. Missing more than a day resets the current streak.",
                actionText = "Add habit",
                actionIcon = Icons.Filled.Add,
                onAction = {
                    if (store.canAddHabit()) {
                        editingHabit = null
                        showEditor = true
                    } else {
                        store.notice = store.limitMessage("habits")
                    }
                }
            )
        }
        item { LimitCard("Free habit limit", store.habits.size, FREE_HABIT_LIMIT, store.premium, store.limitMessage("habits")) }
        item {
            SectionCard(title = "Today") {
                val total = store.habits.size.coerceAtLeast(1)
                val progress = store.doneHabitsTodayCount.toFloat() / total.toFloat()
                Text("${store.doneHabitsTodayCount} of ${store.habits.size} habits complete")
                LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                Text("Check in once per habit per day. If yesterday was missed, the next check-in starts a new streak.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (store.habits.isEmpty()) {
            item { EmptyState(Icons.Filled.Favorite, "No habits yet", "Start with one easy habit that you can complete today.") }
        } else {
            items(store.habits, key = { it.id ?: it.hashCode().toString() }) { habit ->
                HabitCard(
                    habit = habit,
                    onComplete = { store.completeHabit(habit) },
                    onReset = { resetHabit = habit },
                    onEdit = {
                        editingHabit = habit
                        showEditor = true
                    },
                    onDelete = { deleteHabit = habit }
                )
            }
        }
    }
}

@Composable
private fun HabitCard(habit: Habit, onComplete: () -> Unit, onReset: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    val doneToday = habit.doneToday()
    ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(habit.safeName(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(if (doneToday) "Done today" else "Open for today's check-in", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Badge(containerColor = if (doneToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant) {
                    Text(if (doneToday) "Done" else "Today")
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                StreakBox("Current", habit.streak.toString(), Modifier.weight(1f))
                StreakBox("Best", habit.bestStreak.toString(), Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = onComplete, enabled = !doneToday, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (doneToday) "Checked in" else "Mark done")
                }
                OutlinedButton(onClick = onReset) { Text("Reset") }
                IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, contentDescription = "Edit habit") }
                IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = "Delete habit") }
            }
        }
    }
}

@Composable
private fun StreakBox(label: String, value: String, modifier: Modifier = Modifier) {
    ElevatedCard(modifier = modifier, shape = RoundedCornerShape(8.dp)) {
        Column(Modifier.fillMaxWidth().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
