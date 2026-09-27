package com.example.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.Priority
import com.example.data.model.Task
import com.example.ui.theme.FocusAmber
import com.example.ui.theme.FocusEmerald
import com.example.ui.theme.FocusRose

@Composable
fun TaskDialog(
    taskToEdit: Task? = null,
    onDismiss: () -> Unit,
    onConfirm: (
        title: String,
        description: String,
        subject: String,
        priority: Priority,
        estimatedPomodoros: Int
    ) -> Unit
) {
    var title by remember { mutableStateOf(taskToEdit?.title ?: "") }
    var description by remember { mutableStateOf(taskToEdit?.description ?: "") }
    var subject by remember { mutableStateOf(taskToEdit?.subject ?: "Mathematics") }
    var priority by remember { mutableStateOf(taskToEdit?.priority ?: Priority.MEDIUM) }
    var estimatedPomodoros by remember { mutableIntStateOf(taskToEdit?.estimatedPomodoros ?: 2) }

    val commonSubjects = listOf("Mathematics", "Computer Science", "Physics", "Chemistry", "Biology", "History", "Literature", "General")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (taskToEdit == null) "New Assignment" else "Edit Assignment")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Assignment Title *") },
                    placeholder = { Text("e.g. Calculus Problem Set 3") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("task_title_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description & Notes (Optional)") },
                    placeholder = { Text("Chapters, exercises, or objectives") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("task_description_input"),
                    maxLines = 3
                )

                // Subject Selector
                Column {
                    Text("Subject / Course", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        commonSubjects.forEach { subj ->
                            val selected = subject == subj
                            FilterChip(
                                selected = selected,
                                onClick = { subject = subj },
                                label = { Text(subj, style = MaterialTheme.typography.bodySmall) }
                            )
                        }
                    }
                }

                // Priority Selector
                Column {
                    Text("Priority", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Priority.entries.forEach { p ->
                            val selected = priority == p
                            val color = when (p) {
                                Priority.HIGH -> FocusRose
                                Priority.MEDIUM -> FocusAmber
                                Priority.LOW -> FocusEmerald
                            }
                            FilterChip(
                                selected = selected,
                                onClick = { priority = p },
                                label = { Text(p.name) },
                                modifier = Modifier.weight(1f),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = color.copy(alpha = 0.25f),
                                    selectedLabelColor = color
                                )
                            )
                        }
                    }
                }

                // Estimated Pomodoro Sessions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Est. Pomodoros", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "🍅 ${estimatedPomodoros * 25} mins focus",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { if (estimatedPomodoros > 1) estimatedPomodoros-- },
                            enabled = estimatedPomodoros > 1
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Decrease pomodoros")
                        }
                        Text(
                            "$estimatedPomodoros",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        IconButton(
                            onClick = { if (estimatedPomodoros < 20) estimatedPomodoros++ }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Increase pomodoros")
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onConfirm(title, description, subject, priority, estimatedPomodoros)
                    }
                },
                enabled = title.isNotBlank(),
                modifier = Modifier.testTag("save_task_button")
            ) {
                Text(if (taskToEdit == null) "Create" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
