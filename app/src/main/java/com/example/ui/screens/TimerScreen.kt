package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.LibraryBooks
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AmbientSoundType
import com.example.data.model.SessionType
import com.example.ui.components.CustomIntervalDialog
import com.example.ui.theme.FocusAmber
import com.example.ui.theme.FocusCyan
import com.example.ui.theme.FocusEmerald
import com.example.ui.theme.FocusIndigo
import com.example.ui.theme.FocusRose
import com.example.ui.viewmodel.StudyViewModel
import java.util.Locale

@Composable
fun TimerScreen(
    viewModel: StudyViewModel,
    onNavigateToTasks: () -> Unit,
    onNavigateToAmbient: () -> Unit
) {
    val timerState by viewModel.timerState.collectAsState()
    val settings by viewModel.userSettings.collectAsState()
    val ambientVersion by viewModel.ambientStateVersion.collectAsState()

    var showCustomIntervalDialog by remember { mutableStateOf(false) }

    val phaseColor by animateColorAsState(
        targetValue = when (timerState.sessionType) {
            SessionType.FOCUS -> FocusIndigo
            SessionType.SHORT_BREAK -> FocusEmerald
            SessionType.LONG_BREAK -> FocusCyan
        },
        animationSpec = tween(400),
        label = "phaseColor"
    )

    val progress = if (timerState.totalSeconds > 0) {
        (timerState.remainingSeconds.toFloat() / timerState.totalSeconds.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(500),
        label = "progress"
    )

    val minutes = timerState.remainingSeconds / 60
    val seconds = timerState.remainingSeconds % 60
    val timeFormatted = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Preset Interval Selection Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AssistChip(
                onClick = { viewModel.applyPreset("Pomodoro (25/5)", 25, 5, 15) },
                label = { Text("25/5 Pomodoro") },
                leadingIcon = { Text("🍅") },
                modifier = Modifier.testTag("preset_pomodoro")
            )
            AssistChip(
                onClick = { viewModel.applyPreset("Deep Work (50/10)", 50, 10, 20) },
                label = { Text("50/10 Deep") },
                leadingIcon = { Text("⚡") },
                modifier = Modifier.testTag("preset_deep_work")
            )
            AssistChip(
                onClick = { viewModel.applyPreset("Ultra (90/20)", 90, 20, 30) },
                label = { Text("90/20 Ultra") },
                leadingIcon = { Text("🚀") },
                modifier = Modifier.testTag("preset_ultra")
            )
            AssistChip(
                onClick = { viewModel.applyPreset("Sprint (15/3)", 15, 3, 10) },
                label = { Text("15/3 Sprint") },
                leadingIcon = { Text("⏱️") }
            )
            AssistChip(
                onClick = { showCustomIntervalDialog = true },
                label = { Text("Custom...") },
                leadingIcon = { Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp)) },
                modifier = Modifier.testTag("open_custom_intervals")
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Timer Dial
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(270.dp)
                .padding(12.dp)
        ) {
            val strokeTrackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 14.dp.toPx()
                // Track arc
                drawCircle(
                    color = strokeTrackColor,
                    style = Stroke(width = strokeWidth)
                )

                // Progress arc
                drawArc(
                    brush = Brush.sweepGradient(
                        listOf(
                            phaseColor.copy(alpha = 0.7f),
                            phaseColor
                        )
                    ),
                    startAngle = -90f,
                    sweepAngle = 360f * animatedProgress,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Phase Badge
                Surface(
                    color = phaseColor.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.padding(bottom = 6.dp)
                ) {
                    Text(
                        text = when (timerState.sessionType) {
                            SessionType.FOCUS -> "FOCUS SESSION"
                            SessionType.SHORT_BREAK -> "SHORT BREAK"
                            SessionType.LONG_BREAK -> "LONG BREAK"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = phaseColor,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }

                Text(
                    text = timeFormatted,
                    fontSize = 52.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    letterSpacing = (-1).sp
                )

                Text(
                    text = "Interval ${timerState.completedSessionsInCycle + 1} of ${timerState.totalSessionsInCycle}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Play/Pause / Skip / Reset Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.resetTimer() },
                modifier = Modifier
                    .size(48.dp)
                    .testTag("reset_timer_button")
            ) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = "Reset timer",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(20.dp))

            FloatingActionButton(
                onClick = {
                    if (timerState.isRunning) {
                        viewModel.pauseTimer()
                    } else {
                        viewModel.startTimer()
                    }
                },
                containerColor = phaseColor,
                contentColor = Color.White,
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(6.dp),
                modifier = Modifier
                    .size(76.dp)
                    .testTag("toggle_timer_button")
            ) {
                Icon(
                    imageVector = if (timerState.isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (timerState.isRunning) "Pause timer" else "Start timer",
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.width(20.dp))

            IconButton(
                onClick = { viewModel.skipCurrentPhase() },
                modifier = Modifier
                    .size(48.dp)
                    .testTag("skip_timer_button")
            ) {
                Icon(
                    Icons.Default.SkipNext,
                    contentDescription = "Skip phase",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Daily Motivational Quote Card
        val dailyQuote by viewModel.dailyQuote.collectAsState()
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("daily_quote_card"),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("✨", fontSize = 15.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "DAILY MOTIVATION",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(
                        onClick = { viewModel.nextMotivationalQuote() },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Shuffle daily quote",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "“${dailyQuote.text}”",
                    style = MaterialTheme.typography.bodyMedium,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    lineHeight = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "— ${dailyQuote.author} (${dailyQuote.category})",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Active Assignment Card
        val activeTask = timerState.activeTask
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("active_task_card"),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.LibraryBooks,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "CURRENT ASSIGNMENT",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (activeTask != null) {
                        IconButton(
                            onClick = { viewModel.setActiveTask(null) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Outlined.Close,
                                contentDescription = "Unlink task",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                if (activeTask != null) {
                    Text(
                        text = activeTask.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Subject: ${activeTask.subject}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "🍅 ${activeTask.completedPomodoros} / ${activeTask.estimatedPomodoros} Sessions",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = FocusRose
                        )
                    }
                } else {
                    Text(
                        text = "No assignment linked to this session",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onNavigateToTasks,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("select_task_button")
                    ) {
                        Text("Select Assignment to Study")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Ambient Sound Quick Bar
        val isSoundPlaying = viewModel.audioEngine.isAnyPlaying()
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateToAmbient() }
                .testTag("ambient_sound_quick_card"),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = if (isSoundPlaying) FocusCyan.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.GraphicEq,
                                contentDescription = "Ambient sound",
                                tint = if (isSoundPlaying) FocusCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            "Background Ambient Sound",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            if (isSoundPlaying) "Tracks playing • Tap to mix" else "Offline ambient generator • Tap to choose",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isSoundPlaying) {
                        IconButton(
                            onClick = { viewModel.stopAllAmbientTracks() }
                        ) {
                            Icon(
                                Icons.Default.Pause,
                                contentDescription = "Stop sound",
                                tint = FocusCyan
                            )
                        }
                    } else {
                        IconButton(
                            onClick = { viewModel.toggleAmbientTrack(AmbientSoundType.RAIN) }
                        ) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = "Quick start rain",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCustomIntervalDialog) {
        CustomIntervalDialog(
            initialFocusMin = settings.focusDurationMinutes,
            initialShortBreakMin = settings.shortBreakDurationMinutes,
            initialLongBreakMin = settings.longBreakDurationMinutes,
            initialCycles = settings.sessionsBeforeLongBreak,
            onDismiss = { showCustomIntervalDialog = false },
            onConfirm = { focus, shortBreak, longBreak, cycles ->
                viewModel.setCustomIntervals(focus, shortBreak, longBreak, cycles)
                showCustomIntervalDialog = false
            }
        )
    }
}
