package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.data.model.Priority
import com.example.data.model.Task
import com.example.ui.components.TaskDialog
import com.example.ui.theme.FocusAmber
import com.example.ui.theme.FocusCyan
import com.example.ui.theme.FocusEmerald
import com.example.ui.theme.FocusIndigo
import com.example.ui.theme.FocusRose
import com.example.ui.viewmodel.StudyViewModel

@Composable
fun TasksScreen(
    viewModel: StudyViewModel,
    onNavigateToTimer: () -> Unit
) {
    val allTasks by viewModel.allTasks.collectAsState()
    val filteredTasks by viewModel.filteredTasks.collectAsState()
    val searchQuery by viewModel.taskSearchQuery.collectAsState()
    val selectedSubject by viewModel.selectedSubjectFilter.collectAsState()
    val selectedStatus by viewModel.selectedStatusFilter.collectAsState()
    val timerState by viewModel.timerState.collectAsState()

    var showAddTaskDialog by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<Task?>(null) }

    val pendingCount = allTasks.count { !it.isCompleted }
    val completedCount = allTasks.count { it.isCompleted }
    val totalPomodoros = allTasks.sumOf { it.completedPomodoros }

    val subjects = remember(allTasks) {
        listOf("All") + allTasks.map { it.subject }.distinct().filter { it.isNotBlank() }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Statistics
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricStatCard(
                        title = "Pending",
                        value = "$pendingCount",
                        color = FocusIndigo,
                        modifier = Modifier.weight(1f)
                    )
                    MetricStatCard(
                        title = "Completed",
                        value = "$completedCount",
                        color = FocusEmerald,
                        modifier = Modifier.weight(1f)
                    )
                    MetricStatCard(
                        title = "Pomodoros",
                        value = "🍅 $totalPomodoros",
                        color = FocusRose,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setTaskSearchQuery(it) },
                    placeholder = { Text("Search assignments or topics...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setTaskSearchQuery("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("task_search_bar"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Status Filter Tabs
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("All", "Pending", "Completed").forEach { status ->
                        val selected = selectedStatus == status
                        FilterChip(
                            selected = selected,
                            onClick = { viewModel.setSelectedStatusFilter(status) },
                            label = { Text(status) },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("status_chip_$status")
                        )
                    }
                }
            }

            // Subject Filter Chips
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    subjects.forEach { subj ->
                        val isSelected = selectedSubject == subj
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setSelectedSubjectFilter(subj) },
                            label = { Text(subj) },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
            }

            // Empty state
            if (filteredTasks.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("📚", style = MaterialTheme.typography.displayMedium)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) "No assignments match your search" else "No assignments found",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tap the + button below to add your homework or study goals.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(filteredTasks, key = { it.id }) { task ->
                    val isCurrentlyActive = timerState.activeTask?.id == task.id
                    TaskItemCard(
                        task = task,
                        isActiveInTimer = isCurrentlyActive,
                        onToggleComplete = { viewModel.toggleTaskComplete(task) },
                        onStartFocus = {
                            viewModel.setActiveTask(task)
                            onNavigateToTimer()
                        },
                        onDelete = { viewModel.deleteTask(task) },
                        onClick = { taskToEdit = task }
                    )
                }
            }
        }

        // Add Task FAB
        FloatingActionButton(
            onClick = { showAddTaskDialog = true },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .testTag("add_task_fab")
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add new assignment")
        }
    }

    // Add Task Dialog
    if (showAddTaskDialog) {
        TaskDialog(
            onDismiss = { showAddTaskDialog = false },
            onConfirm = { title, desc, subj, priority, pomodoros ->
                viewModel.addTask(title, desc, subj, priority, pomodoros, null)
                showAddTaskDialog = false
            }
        )
    }

    // Edit Task Dialog
    taskToEdit?.let { task ->
        TaskDialog(
            taskToEdit = task,
            onDismiss = { taskToEdit = null },
            onConfirm = { title, desc, subj, priority, pomodoros ->
                viewModel.updateTask(
                    task.copy(
                        title = title,
                        description = desc,
                        subject = subj,
                        priority = priority,
                        estimatedPomodoros = pomodoros
                    )
                )
                taskToEdit = null
            }
        )
    }
}

@Composable
private fun MetricStatCard(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.12f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TaskItemCard(
    task: Task,
    isActiveInTimer: Boolean,
    onToggleComplete: () -> Unit,
    onStartFocus: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    val priorityColor = when (task.priority) {
        Priority.HIGH -> FocusRose
        Priority.MEDIUM -> FocusAmber
        Priority.LOW -> FocusEmerald
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("task_item_${task.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (isActiveInTimer) {
                FocusIndigo.copy(alpha = 0.12f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = task.isCompleted,
                    onCheckedChange = { onToggleComplete() },
                    colors = CheckboxDefaults.colors(checkedColor = FocusEmerald),
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                        color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                    )

                    if (task.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = task.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2
                        )
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete assignment",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Metadata Row: Subject, Priority, Pomodoros, and Focus Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Subject Chip
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = task.subject,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Priority Chip
                    Surface(
                        color = priorityColor.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = task.priority.name,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = priorityColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    // Pomodoro sessions completed
                    Text(
                        text = "🍅 ${task.completedPomodoros}/${task.estimatedPomodoros}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (!task.isCompleted) {
                    Button(
                        onClick = onStartFocus,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = if (isActiveInTimer) {
                            ButtonDefaults.buttonColors(containerColor = FocusIndigo)
                        } else {
                            ButtonDefaults.filledTonalButtonColors()
                        },
                        modifier = Modifier
                            .height(32.dp)
                            .testTag("focus_task_button_${task.id}")
                    ) {
                        Icon(
                            Icons.Default.PlayCircleFilled,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            if (isActiveInTimer) "Active" else "Focus",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }
    }
}
