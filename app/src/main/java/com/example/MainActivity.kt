package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.ThemeMode
import com.example.ui.screens.AmbientScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.SyncScreen
import com.example.ui.screens.TasksScreen
import com.example.ui.screens.TimerScreen
import com.example.ui.theme.FocusAmber
import com.example.ui.theme.FocusCyan
import com.example.ui.theme.FocusEmerald
import com.example.ui.theme.FocusIndigo
import com.example.ui.theme.StudyFlowTheme
import com.example.ui.viewmodel.StudyViewModel

enum class StudyScreenDestination(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    TIMER("timer", "Timer", Icons.Filled.Timer, Icons.Outlined.Timer),
    TASKS("tasks", "Tasks", Icons.AutoMirrored.Filled.ListAlt, Icons.AutoMirrored.Filled.ListAlt),
    AMBIENT("ambient", "Ambient", Icons.Filled.GraphicEq, Icons.Outlined.GraphicEq),
    STREAKS("streaks", "Streaks", Icons.Filled.LocalFireDepartment, Icons.Outlined.LocalFireDepartment),
    SYNC("sync", "Cloud Sync", Icons.Filled.CloudSync, Icons.Outlined.CloudSync)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val studyViewModel: StudyViewModel = viewModel()
            val userSettings by studyViewModel.userSettings.collectAsState()

            StudyFlowTheme(themeMode = userSettings.themeMode) {
                MainStudyApp(viewModel = studyViewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainStudyApp(viewModel: StudyViewModel) {
    var currentScreen by remember { mutableStateOf(StudyScreenDestination.TIMER) }
    val userSettings by viewModel.userSettings.collectAsState()
    val isOnline = viewModel.cloudSyncManager.isOnline()

    // Handle back button to return to Timer screen if on sub-screen
    BackHandler(enabled = currentScreen != StudyScreenDestination.TIMER) {
        currentScreen = StudyScreenDestination.TIMER
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 600.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.HourglassTop,
                                contentDescription = null,
                                tint = FocusCyan,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "StudyFlow",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    },
                    navigationIcon = {
                        // Cloud sync online/offline indicator badge
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = if (isOnline) FocusEmerald.copy(alpha = 0.15f) else FocusAmber.copy(alpha = 0.15f),
                            modifier = Modifier
                                .padding(start = 12.dp)
                                .testTag("top_sync_indicator")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isOnline) Icons.Default.CloudDone else Icons.Default.CloudOff,
                                    contentDescription = if (isOnline) "Cloud connected" else "Offline mode",
                                    tint = if (isOnline) FocusEmerald else FocusAmber,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    if (isOnline) "Sync On" else "Offline",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isOnline) FocusEmerald else FocusAmber
                                )
                            }
                        }
                    },
                    actions = {
                        // Google Account button / Avatar
                        IconButton(
                            onClick = { currentScreen = StudyScreenDestination.SYNC },
                            modifier = Modifier
                                .testTag("top_google_account_button")
                        ) {
                            if (userSettings.isSignedInWithGoogle) {
                                Surface(
                                    shape = androidx.compose.foundation.shape.CircleShape,
                                    color = FocusIndigo,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            (userSettings.googleDisplayName?.take(1) ?: "G").uppercase(),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = androidx.compose.ui.graphics.Color.White
                                        )
                                    }
                                }
                            } else {
                                Icon(
                                    painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_google_logo),
                                    contentDescription = "Sign in with Google",
                                    tint = androidx.compose.ui.graphics.Color.Unspecified,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Quick theme toggle button in TopAppBar
                        IconButton(
                            onClick = {
                                val nextTheme = when (userSettings.themeMode) {
                                    ThemeMode.SYSTEM -> ThemeMode.DARK
                                    ThemeMode.DARK -> ThemeMode.LIGHT
                                    ThemeMode.LIGHT -> ThemeMode.SYSTEM
                                }
                                viewModel.updateThemeMode(nextTheme)
                            },
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .testTag("quick_theme_toggle")
                        ) {
                            Icon(
                                imageVector = when (userSettings.themeMode) {
                                    ThemeMode.SYSTEM -> Icons.Default.SettingsBrightness
                                    ThemeMode.DARK -> Icons.Default.DarkMode
                                    ThemeMode.LIGHT -> Icons.Default.LightMode
                                },
                                contentDescription = "Toggle theme mode"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            },
            bottomBar = {
                if (!isWideScreen) {
                    NavigationBar(
                        modifier = Modifier.testTag("bottom_nav_bar")
                    ) {
                        StudyScreenDestination.entries.forEach { screen ->
                            val selected = currentScreen == screen
                            NavigationBarItem(
                                selected = selected,
                                onClick = { currentScreen = screen },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) screen.selectedIcon else screen.unselectedIcon,
                                        contentDescription = screen.title
                                    )
                                },
                                label = { Text(screen.title) },
                                modifier = Modifier.testTag("nav_item_${screen.route}")
                            )
                        }
                    }
                }
            }
        ) { paddingValues ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                if (isWideScreen) {
                    NavigationRail(
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        StudyScreenDestination.entries.forEach { screen ->
                            val selected = currentScreen == screen
                            NavigationRailItem(
                                selected = selected,
                                onClick = { currentScreen = screen },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) screen.selectedIcon else screen.unselectedIcon,
                                        contentDescription = screen.title
                                    )
                                },
                                label = { Text(screen.title) },
                                modifier = Modifier.testTag("rail_item_${screen.route}")
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 700.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    when (currentScreen) {
                        StudyScreenDestination.TIMER -> TimerScreen(
                            viewModel = viewModel,
                            onNavigateToTasks = { currentScreen = StudyScreenDestination.TASKS },
                            onNavigateToAmbient = { currentScreen = StudyScreenDestination.AMBIENT }
                        )
                        StudyScreenDestination.TASKS -> TasksScreen(
                            viewModel = viewModel,
                            onNavigateToTimer = { currentScreen = StudyScreenDestination.TIMER }
                        )
                        StudyScreenDestination.AMBIENT -> AmbientScreen(
                            viewModel = viewModel
                        )
                        StudyScreenDestination.STREAKS -> DashboardScreen(
                            viewModel = viewModel
                        )
                        StudyScreenDestination.SYNC -> SyncScreen(
                            viewModel = viewModel
                        )
                    }
                }
            }
        }
    }
}
