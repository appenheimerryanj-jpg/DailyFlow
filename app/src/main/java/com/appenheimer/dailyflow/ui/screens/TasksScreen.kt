package com.appenheimer.dailyflow.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.appenheimer.dailyflow.data.DailyFlowStore
import com.appenheimer.dailyflow.model.DailyTask
import com.appenheimer.dailyflow.model.FREE_ACTIVE_TASK_LIMIT
import com.appenheimer.dailyflow.model.TaskFilter
import com.appenheimer.dailyflow.model.safePriority
import com.appenheimer.dailyflow.model.safeText
import com.appenheimer.dailyflow.model.sortedTasks
import com.appenheimer.dailyflow.ui.components.EmptyState
import com.appenheimer.dailyflow.ui.components.LimitCard
import com.appenheimer.dailyflow.ui.components.PriorityBadge
import com.appenheimer.dailyflow.ui.components.ScreenHeader
import com.appenheimer.dailyflow.ui.components.ScreenList

@Composable
fun TasksScreen(store: DailyFlowStore) {
    var filterName by rememberSaveable { mutableStateOf(TaskFilter.ALL.name) }
    var showEditor by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<DailyTask?>(null) }
    var deleteTask by remember { mutableStateOf<DailyTask?>(null) }
    var confirmClearCompleted by remember { mutableStateOf(false) }
    val filter = TaskFilter.valueOf(filterName)
    val filteredTasks = when (filter) {
        TaskFilter.ALL -> store.tasks
        TaskFilter.ACTIVE -> store.tasks.filterNot { it.done }
        TaskFilter.COMPLETED -> store.tasks.filter { it.done }
    }.let { sortedTasks(it) }

    if (showEditor) {
        TaskEditorDialog(
            task = editingTask,
            onDismiss = { showEditor = false },
            onSave = { text, priority, dueDate ->
                if (store.upsertTask(editingTask?.id, text, priority, dueDate)) showEditor = false
            }
        )
    }
    deleteTask?.let { task ->
        ConfirmDialog(
            title = "Delete task?",
            text = "This removes \"${task.safeText()}\" from this device.",
            confirmText = "Delete",
            onDismiss = { deleteTask = null },
            onConfirm = {
                store.deleteTask(task.id)
                deleteTask = null
            }
        )
    }
    if (confirmClearCompleted) {
        ConfirmDialog(
            title = "Clear completed tasks?",
            text = "Completed tasks will be removed from local storage.",
            confirmText = "Clear",
            onDismiss = { confirmClearCompleted = false },
            onConfirm = {
                store.clearCompletedTasks()
                confirmClearCompleted = false
            }
        )
    }

    ScreenList {
        item {
            ScreenHeader(
                title = "Tasks",
                subtitle = "Sorted by status, priority, and due date text.",
                actionText = "Add task",
                actionIcon = Icons.Filled.Add,
                onAction = {
                    if (store.canAddTask()) {
                        editingTask = null
                        showEditor = true
                    } else {
                        store.notice = store.limitMessage("tasks")
                    }
                }
            )
        }
        item {
            LimitCard("Free active task limit", store.activeTaskCount, FREE_ACTIVE_TASK_LIMIT, store.premium, store.limitMessage("tasks"))
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                TaskFilter.entries.forEach { item ->
                    FilterChip(selected = filter == item, onClick = { filterName = item.name }, label = { Text(item.label) })
                }
            }
        }
        item {
            OutlinedButton(
                onClick = { confirmClearCompleted = true },
                enabled = store.completedTaskCount > 0,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Clear completed")
            }
        }
        if (filteredTasks.isEmpty()) {
            item {
                EmptyState(
                    Icons.Filled.CheckCircle,
                    when (filter) {
                        TaskFilter.ALL -> "No tasks yet"
                        TaskFilter.ACTIVE -> "No active tasks"
                        TaskFilter.COMPLETED -> "No completed tasks"
                    },
                    "Add a clear next action, then give it a priority and optional due date text."
                )
            }
        } else {
            items(filteredTasks, key = { it.id ?: it.hashCode().toString() }) { task ->
                TaskCard(
                    task = task,
                    onToggle = { store.toggleTask(task) },
                    onEdit = {
                        editingTask = task
                        showEditor = true
                    },
                    onDelete = { deleteTask = task }
                )
            }
        }
    }
}

@Composable
private fun TaskCard(task: DailyTask, onToggle: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = task.done, onCheckedChange = { onToggle() })
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    task.safeText(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = if (task.done) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (task.done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    PriorityBadge(task.safePriority())
                    if (!task.dueDate.isNullOrBlank()) {
                        Icon(Icons.Filled.Today, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary)
                        Text(task.dueDate.orEmpty(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                    }
                }
            }
            IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, contentDescription = "Edit task") }
            IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = "Delete task") }
        }
    }
}

@Composable
fun ConfirmDialog(title: String, text: String, confirmText: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = { Button(onClick = onConfirm) { Text(confirmText) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
