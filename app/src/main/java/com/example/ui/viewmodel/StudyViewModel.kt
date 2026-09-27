package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AmbientAudioEngine
import com.example.audio.AmbientSoundType
import com.example.data.local.AppDatabase
import com.example.data.model.DayStudyRecord
import com.example.data.model.Milestone
import com.example.data.model.Priority
import com.example.data.model.SessionType
import com.example.data.model.StreakInfo
import com.example.data.model.StudySession
import com.example.data.model.SubjectStudyRecord
import com.example.data.model.Task
import com.example.data.model.ThemeMode
import com.example.data.model.UserSettings
import com.example.data.repository.StudyRepository
import com.example.sync.CloudSyncManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TimerUiState(
    val sessionType: SessionType = SessionType.FOCUS,
    val isRunning: Boolean = false,
    val remainingSeconds: Int = 25 * 60,
    val totalSeconds: Int = 25 * 60,
    val completedSessionsInCycle: Int = 0,
    val totalSessionsInCycle: Int = 4,
    val activeTask: Task? = null,
    val presetName: String = "Pomodoro (25/5)"
)

class StudyViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    val repository = StudyRepository(
        database.taskDao(),
        database.studySessionDao(),
        database.userSettingsDao()
    )
    val cloudSyncManager = CloudSyncManager(application, repository)
    val audioEngine = AmbientAudioEngine()
    val googleAuthHelper = com.example.auth.GoogleAuthHelper(application)

    // Timer State
    private val _timerState = MutableStateFlow(TimerUiState())
    val timerState: StateFlow<TimerUiState> = _timerState.asStateFlow()

    private var timerJob: Job? = null

    // Tasks and Filter State
    val allTasks = repository.allTasks.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    private val _taskSearchQuery = MutableStateFlow("")
    val taskSearchQuery: StateFlow<String> = _taskSearchQuery.asStateFlow()

    private val _selectedSubjectFilter = MutableStateFlow("All")
    val selectedSubjectFilter: StateFlow<String> = _selectedSubjectFilter.asStateFlow()

    private val _selectedStatusFilter = MutableStateFlow("All") // "All", "Pending", "Completed"
    val selectedStatusFilter: StateFlow<String> = _selectedStatusFilter.asStateFlow()

    val filteredTasks: StateFlow<List<Task>> = combine(
        allTasks,
        _taskSearchQuery,
        _selectedSubjectFilter,
        _selectedStatusFilter
    ) { tasks, query, subject, status ->
        tasks.filter { task ->
            val matchesQuery = query.isBlank() || task.title.contains(query, ignoreCase = true) ||
                    task.subject.contains(query, ignoreCase = true)
            val matchesSubject = subject == "All" || task.subject.equals(subject, ignoreCase = true)
            val matchesStatus = when (status) {
                "Pending" -> !task.isCompleted
                "Completed" -> task.isCompleted
                else -> true
            }
            matchesQuery && matchesSubject && matchesStatus
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Ambient Tracks state counter to trigger recomposition when volume/active state changes
    private val _ambientStateVersion = MutableStateFlow(0)
    val ambientStateVersion: StateFlow<Int> = _ambientStateVersion.asStateFlow()

    // Daily Motivational Quote State
    private var quoteOffset = 0
    private val _dailyQuote = MutableStateFlow(com.example.data.model.QuoteLibrary.getTodayQuote(0))
    val dailyQuote: StateFlow<com.example.data.model.MotivationalQuote> = _dailyQuote.asStateFlow()

    fun nextMotivationalQuote() {
        quoteOffset++
        _dailyQuote.value = com.example.data.model.QuoteLibrary.getTodayQuote(quoteOffset)
    }

    // Analytics / Streaks State
    val streakInfo: StateFlow<StreakInfo> = repository.getStreakInfo().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        StreakInfo()
    )

    val weeklyRecords: StateFlow<List<DayStudyRecord>> = repository.getWeeklyStudyRecords().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val heatmapRecords: StateFlow<List<DayStudyRecord>> = repository.getMonthlyHeatmap().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val subjectBreakdown: StateFlow<List<SubjectStudyRecord>> = repository.getSubjectBreakdown().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val userSettings: StateFlow<UserSettings> = repository.userSettings.combine(
        MutableStateFlow(Unit)
    ) { settings, _ ->
        settings ?: UserSettings()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserSettings())

    init {
        viewModelScope.launch {
            repository.initializeDefaultsIfNeeded()
            val initialSettings = repository.getSettingsDirect()
            val focusSeconds = initialSettings.focusDurationMinutes * 60
            _timerState.value = _timerState.value.copy(
                remainingSeconds = focusSeconds,
                totalSeconds = focusSeconds,
                totalSessionsInCycle = initialSettings.sessionsBeforeLongBreak
            )
        }
    }

    // Timer Logic
    fun startTimer() {
        if (_timerState.value.isRunning) return
        _timerState.value = _timerState.value.copy(isRunning = true)

        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_timerState.value.remainingSeconds > 0 && _timerState.value.isRunning) {
                delay(1000)
                val newRemaining = _timerState.value.remainingSeconds - 1
                _timerState.value = _timerState.value.copy(remainingSeconds = newRemaining)
            }

            if (_timerState.value.remainingSeconds <= 0 && _timerState.value.isRunning) {
                handleSessionCompleted()
            }
        }
    }

    fun pauseTimer() {
        _timerState.value = _timerState.value.copy(isRunning = false)
        timerJob?.cancel()
    }

    fun resetTimer() {
        pauseTimer()
        val settings = userSettings.value
        val durationMins = when (_timerState.value.sessionType) {
            SessionType.FOCUS -> settings.focusDurationMinutes
            SessionType.SHORT_BREAK -> settings.shortBreakDurationMinutes
            SessionType.LONG_BREAK -> settings.longBreakDurationMinutes
        }
        val seconds = durationMins * 60
        _timerState.value = _timerState.value.copy(
            remainingSeconds = seconds,
            totalSeconds = seconds
        )
    }

    fun skipCurrentPhase() {
        pauseTimer()
        transitionToNextPhase(autoStart = false)
    }

    private suspend fun handleSessionCompleted() {
        val current = _timerState.value
        val settings = userSettings.value

        vibrateAlert()

        // Log session in repository
        val durationMins = current.totalSeconds / 60
        repository.logSession(
            taskId = current.activeTask?.id,
            taskTitle = current.activeTask?.title,
            subject = current.activeTask?.subject ?: "General",
            durationMinutes = durationMins.coerceAtLeast(1),
            sessionType = current.sessionType
        )

        cloudSyncManager.refreshStatus()
        transitionToNextPhase(
            autoStart = if (current.sessionType == SessionType.FOCUS) settings.autoStartBreaks else settings.autoStartFocus
        )
    }

    private fun transitionToNextPhase(autoStart: Boolean) {
        val current = _timerState.value
        val settings = userSettings.value

        val nextType: SessionType
        var nextCompletedCycles = current.completedSessionsInCycle

        if (current.sessionType == SessionType.FOCUS) {
            nextCompletedCycles++
            nextType = if (nextCompletedCycles >= settings.sessionsBeforeLongBreak) {
                SessionType.LONG_BREAK
            } else {
                SessionType.SHORT_BREAK
            }
        } else {
            if (current.sessionType == SessionType.LONG_BREAK) {
                nextCompletedCycles = 0
            }
            nextType = SessionType.FOCUS
        }

        val nextDurationMins = when (nextType) {
            SessionType.FOCUS -> settings.focusDurationMinutes
            SessionType.SHORT_BREAK -> settings.shortBreakDurationMinutes
            SessionType.LONG_BREAK -> settings.longBreakDurationMinutes
        }
        val nextSeconds = nextDurationMins * 60

        _timerState.value = current.copy(
            sessionType = nextType,
            isRunning = false,
            remainingSeconds = nextSeconds,
            totalSeconds = nextSeconds,
            completedSessionsInCycle = nextCompletedCycles,
            totalSessionsInCycle = settings.sessionsBeforeLongBreak
        )

        if (autoStart) {
            startTimer()
        }
    }

    fun setActiveTask(task: Task?) {
        _timerState.value = _timerState.value.copy(activeTask = task)
    }

    fun applyPreset(presetName: String, focusMinutes: Int, shortBreakMinutes: Int, longBreakMinutes: Int) {
        pauseTimer()
        val updatedSettings = userSettings.value.copy(
            focusDurationMinutes = focusMinutes,
            shortBreakDurationMinutes = shortBreakMinutes,
            longBreakDurationMinutes = longBreakMinutes
        )
        viewModelScope.launch {
            repository.updateSettings(updatedSettings)
        }
        val focusSeconds = focusMinutes * 60
        _timerState.value = _timerState.value.copy(
            presetName = presetName,
            sessionType = SessionType.FOCUS,
            remainingSeconds = focusSeconds,
            totalSeconds = focusSeconds
        )
    }

    fun setCustomIntervals(
        focusMin: Int,
        shortBreakMin: Int,
        longBreakMin: Int,
        cycles: Int
    ) {
        pauseTimer()
        val updated = userSettings.value.copy(
            focusDurationMinutes = focusMin.coerceIn(1, 180),
            shortBreakDurationMinutes = shortBreakMin.coerceIn(1, 60),
            longBreakDurationMinutes = longBreakMin.coerceIn(1, 60),
            sessionsBeforeLongBreak = cycles.coerceIn(1, 12)
        )
        viewModelScope.launch {
            repository.updateSettings(updated)
        }
        val focusSeconds = updated.focusDurationMinutes * 60
        _timerState.value = _timerState.value.copy(
            presetName = "Custom ($focusMin/${shortBreakMin}m)",
            sessionType = SessionType.FOCUS,
            remainingSeconds = focusSeconds,
            totalSeconds = focusSeconds,
            totalSessionsInCycle = updated.sessionsBeforeLongBreak
        )
    }

    private fun vibrateAlert() {
        if (!userSettings.value.vibrationEnabled) return
        val context = getApplication<Application>()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createWaveform(longArrayOf(0, 300, 200, 400), -1)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(
                        VibrationEffect.createWaveform(longArrayOf(0, 300, 200, 400), -1)
                    )
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(500)
                }
            }
        } catch (_: Exception) {}
    }

    // Ambient Sound Controls
    fun toggleAmbientTrack(type: AmbientSoundType) {
        audioEngine.toggleTrack(type) {
            _ambientStateVersion.value++
        }
        _ambientStateVersion.value++
    }

    fun setAmbientTrackVolume(type: AmbientSoundType, volume: Float) {
        audioEngine.setTrackVolume(type, volume)
        _ambientStateVersion.value++
    }

    fun setAmbientMasterVolume(volume: Float) {
        audioEngine.setMasterVolume(volume)
        _ambientStateVersion.value++
    }

    fun stopAllAmbientTracks() {
        audioEngine.stopAll {
            _ambientStateVersion.value++
        }
        _ambientStateVersion.value++
    }

    // Task Management
    fun setTaskSearchQuery(query: String) {
        _taskSearchQuery.value = query
    }

    fun setSelectedSubjectFilter(subject: String) {
        _selectedSubjectFilter.value = subject
    }

    fun setSelectedStatusFilter(status: String) {
        _selectedStatusFilter.value = status
    }

    fun addTask(
        title: String,
        description: String,
        subject: String,
        priority: Priority,
        estimatedPomodoros: Int,
        dueDateMillis: Long?
    ) {
        viewModelScope.launch {
            val task = Task(
                title = title.trim(),
                description = description.trim(),
                subject = subject.trim().ifBlank { "General" },
                priority = priority,
                estimatedPomodoros = estimatedPomodoros.coerceAtLeast(1),
                dueDateMillis = dueDateMillis
            )
            repository.insertTask(task)
            cloudSyncManager.refreshStatus()
        }
    }

    fun updateTask(task: Task) {
        viewModelScope.launch {
            repository.updateTask(task)
            cloudSyncManager.refreshStatus()
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            if (_timerState.value.activeTask?.id == task.id) {
                _timerState.value = _timerState.value.copy(activeTask = null)
            }
            repository.deleteTask(task)
            cloudSyncManager.refreshStatus()
        }
    }

    fun toggleTaskComplete(task: Task) {
        viewModelScope.launch {
            repository.toggleTaskComplete(task)
            cloudSyncManager.refreshStatus()
        }
    }

    // Settings Updates
    fun updateThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            val updated = userSettings.value.copy(themeMode = mode)
            repository.updateSettings(updated)
        }
    }

    fun updateUserSettings(settings: UserSettings) {
        viewModelScope.launch {
            repository.updateSettings(settings)
        }
    }

    fun getMilestonesList(): List<Milestone> {
        val completedCount = allTasks.value.count { it.isCompleted }
        return repository.getMilestones(streakInfo.value, completedCount)
    }

    // Google Sign-In Actions
    fun signInWithGoogle(
        serverClientId: String? = null,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            googleAuthHelper.signInWithGoogle(
                serverClientId = serverClientId,
                onSuccess = { userData ->
                    viewModelScope.launch {
                        val current = userSettings.value
                        val updated = current.copy(
                            isSignedInWithGoogle = true,
                            googleEmail = userData.email,
                            googleDisplayName = userData.displayName,
                            googlePhotoUrl = userData.photoUrl,
                            syncAccountId = userData.email
                        )
                        repository.updateSettings(updated)
                        cloudSyncManager.refreshStatus()
                        onSuccess()
                    }
                },
                onError = { err ->
                    onError(err)
                }
            )
        }
    }

    fun connectGoogleAccountDirect(email: String, displayName: String) {
        viewModelScope.launch {
            val current = userSettings.value
            val updated = current.copy(
                isSignedInWithGoogle = true,
                googleEmail = email.trim(),
                googleDisplayName = displayName.trim().ifBlank { email.substringBefore("@") },
                syncAccountId = email.trim()
            )
            repository.updateSettings(updated)
            cloudSyncManager.refreshStatus()
        }
    }

    fun signOutGoogle(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            googleAuthHelper.signOut {
                viewModelScope.launch {
                    val current = userSettings.value
                    val updated = current.copy(
                        isSignedInWithGoogle = false,
                        googleEmail = null,
                        googleDisplayName = null,
                        googlePhotoUrl = null
                    )
                    repository.updateSettings(updated)
                    cloudSyncManager.refreshStatus()
                    onComplete()
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        audioEngine.stopAll()
    }
}
