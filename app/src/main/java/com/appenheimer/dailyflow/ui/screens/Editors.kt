package com.appenheimer.dailyflow.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.appenheimer.dailyflow.model.DailyTask
import com.appenheimer.dailyflow.model.Habit
import com.appenheimer.dailyflow.model.Note
import com.appenheimer.dailyflow.model.TaskPriority
import com.appenheimer.dailyflow.model.noteBody
import com.appenheimer.dailyflow.model.safeName
import com.appenheimer.dailyflow.model.safePriority
import com.appenheimer.dailyflow.model.safeText

@Composable
fun TaskEditorDialog(
    task: DailyTask?,
    onDismiss: () -> Unit,
    onSave: (String, TaskPriority, String) -> Unit
) {
    var text by remember(task?.id) { mutableStateOf(task?.safeText().orEmpty()) }
    var dueDate by remember(task?.id) { mutableStateOf(task?.dueDate.orEmpty()) }
    var priority by remember(task?.id) { mutableStateOf(task?.safePriority() ?: TaskPriority.MEDIUM) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (task == null) "Add task" else "Edit task") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = text, onValueChange = { text = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Task") }, singleLine = true)
                OutlinedTextField(value = dueDate, onValueChange = { dueDate = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Due date text") }, placeholder = { Text("Today, Friday, Jun 12...") }, singleLine = true)
                Text("Priority", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TaskPriority.entries.forEach { item ->
                        FilterChip(selected = priority == item, onClick = { priority = item }, label = { Text(item.label) })
                    }
                }
            }
        },
        confirmButton = { Button(onClick = { onSave(text, priority, dueDate) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun HabitEditorDialog(habit: Habit?, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var name by remember(habit?.id) { mutableStateOf(habit?.safeName().orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (habit == null) "Add habit" else "Edit habit") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Habit") },
                placeholder = { Text("Read, walk, stretch...") },
                singleLine = true
            )
        },
        confirmButton = { Button(onClick = { onSave(name) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun NoteEditor(
    note: Note?,
    onCancel: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var title by remember(note?.id) { mutableStateOf(note?.title?.trim().orEmpty()) }
    var body by remember(note?.id) { mutableStateOf(note?.noteBody().orEmpty()) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(value = title, onValueChange = { title = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Title") }, singleLine = true)
        OutlinedTextField(value = body, onValueChange = { body = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Note") }, minLines = 8)
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("Cancel") }
            Button(onClick = { onSave(title, body) }, modifier = Modifier.weight(1f)) { Text("Save note") }
        }
    }
}
