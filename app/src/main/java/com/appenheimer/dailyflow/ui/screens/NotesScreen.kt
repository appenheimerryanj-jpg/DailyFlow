package com.appenheimer.dailyflow.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.appenheimer.dailyflow.data.DailyFlowStore
import com.appenheimer.dailyflow.model.FREE_NOTE_LIMIT
import com.appenheimer.dailyflow.model.Note
import com.appenheimer.dailyflow.model.formatTimestamp
import com.appenheimer.dailyflow.model.noteBody
import com.appenheimer.dailyflow.model.noteTitle
import com.appenheimer.dailyflow.ui.components.EmptyState
import com.appenheimer.dailyflow.ui.components.FlowPose
import com.appenheimer.dailyflow.ui.components.LimitCard
import com.appenheimer.dailyflow.ui.components.ScreenHeader
import com.appenheimer.dailyflow.ui.components.ScreenList
import com.appenheimer.dailyflow.ui.components.SectionCard

@Composable
fun NotesScreen(store: DailyFlowStore) {
    var query by rememberSaveable { mutableStateOf("") }
    var editingNote by remember { mutableStateOf<Note?>(null) }
    var creatingNote by remember { mutableStateOf(false) }
    var deleteNote by remember { mutableStateOf<Note?>(null) }

    deleteNote?.let { note ->
        ConfirmDialog("Delete note?", "This removes \"${note.noteTitle()}\" from local storage.", "Delete", { deleteNote = null }) {
            store.deleteNote(note.id)
            deleteNote = null
        }
    }

    if (creatingNote || editingNote != null) {
        val note = editingNote
        ScreenList {
            item {
                ScreenHeader(
                    title = if (note == null) "New note" else "Edit note",
                    subtitle = "Titles and body text are searchable and stored locally."
                )
            }
            item {
                SectionCard(title = "Note editor") {
                    NoteEditor(
                        note = note,
                        onCancel = {
                            creatingNote = false
                            editingNote = null
                        },
                        onSave = { title, body ->
                            if (store.upsertNote(note?.id, title, body)) {
                                creatingNote = false
                                editingNote = null
                            }
                        }
                    )
                }
            }
        }
        return
    }

    val filteredNotes = store.notes.filter { note ->
        val needle = query.trim()
        needle.isBlank() ||
            note.noteTitle().contains(needle, ignoreCase = true) ||
            note.noteBody().contains(needle, ignoreCase = true)
    }

    ScreenList {
        item {
            ScreenHeader(
                title = "Notes",
                subtitle = "Searchable local notes for details you do not want to lose.",
                actionText = "Add note",
                actionIcon = Icons.Filled.Add,
                onAction = {
                    if (store.canAddNote()) creatingNote = true else store.notice = store.limitMessage("notes")
                }
            )
        }
        item { LimitCard("Free note limit", store.notes.size, FREE_NOTE_LIMIT, store.premium, store.limitMessage("notes")) }
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotBlank()) {
                        IconButton(onClick = { query = "" }) { Icon(Icons.Filled.Close, contentDescription = "Clear search") }
                    }
                },
                label = { Text("Search title or body") }
            )
        }
        if (filteredNotes.isEmpty()) {
            item {
                EmptyState(
                    Icons.Filled.Edit,
                    if (query.isBlank()) "No notes yet" else "No notes found",
                    if (query.isBlank()) {
                        "Flow can hold meeting details, shopping lists, ideas, and thoughts you want searchable later."
                    } else {
                        "Flow could not find that phrase. Try another word from the title or body."
                    },
                    if (query.isBlank()) FlowPose.NOTE else FlowPose.THINKING
                )
            }
        } else {
            items(filteredNotes, key = { it.id ?: it.hashCode().toString() }) { note ->
                NoteCard(
                    note = note,
                    onEdit = { editingNote = note },
                    onDelete = { deleteNote = note }
                )
            }
        }
    }
}

@Composable
private fun NoteCard(note: Note, onEdit: () -> Unit, onDelete: () -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onEdit),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(note.noteTitle(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Updated ${formatTimestamp(note.updatedAt)}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                }
                IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, contentDescription = "Edit note") }
                IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = "Delete note") }
            }
            Text(note.noteBody().ifBlank { "No body text" }, maxLines = 3, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
