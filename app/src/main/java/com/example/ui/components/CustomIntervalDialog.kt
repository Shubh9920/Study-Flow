package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun CustomIntervalDialog(
    initialFocusMin: Int,
    initialShortBreakMin: Int,
    initialLongBreakMin: Int,
    initialCycles: Int,
    onDismiss: () -> Unit,
    onConfirm: (focus: Int, shortBreak: Int, longBreak: Int, cycles: Int) -> Unit
) {
    var focusMin by remember { mutableFloatStateOf(initialFocusMin.toFloat()) }
    var shortBreakMin by remember { mutableFloatStateOf(initialShortBreakMin.toFloat()) }
    var longBreakMin by remember { mutableFloatStateOf(initialLongBreakMin.toFloat()) }
    var cycles by remember { mutableIntStateOf(initialCycles) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Custom Timer Intervals",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Focus duration
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Focus Session", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "${focusMin.roundToInt()} min",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = focusMin,
                        onValueChange = { focusMin = it },
                        valueRange = 5f..120f,
                        steps = 22,
                        modifier = Modifier.testTag("custom_focus_slider")
                    )
                }

                // Short break duration
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Short Break", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "${shortBreakMin.roundToInt()} min",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    Slider(
                        value = shortBreakMin,
                        onValueChange = { shortBreakMin = it },
                        valueRange = 1f..30f,
                        steps = 28,
                        modifier = Modifier.testTag("custom_short_break_slider")
                    )
                }

                // Long break duration
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Long Break", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "${longBreakMin.roundToInt()} min",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                    Slider(
                        value = longBreakMin,
                        onValueChange = { longBreakMin = it },
                        valueRange = 5f..45f,
                        steps = 7,
                        modifier = Modifier.testTag("custom_long_break_slider")
                    )
                }

                // Sessions before long break
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Long Break Interval", style = MaterialTheme.typography.bodyMedium)
                        Text("Every $cycles sessions", style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(2, 3, 4, 5).forEach { count ->
                            val isSelected = cycles == count
                            if (isSelected) {
                                Button(
                                    onClick = { cycles = count },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("$count")
                                }
                            } else {
                                FilledTonalButton(
                                    onClick = { cycles = count },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("$count")
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        focusMin.roundToInt(),
                        shortBreakMin.roundToInt(),
                        longBreakMin.roundToInt(),
                        cycles
                    )
                },
                modifier = Modifier.testTag("confirm_custom_intervals")
            ) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
