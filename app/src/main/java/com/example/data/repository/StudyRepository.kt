package com.example.data.repository

import com.example.data.local.StudySessionDao
import com.example.data.local.TaskDao
import com.example.data.local.UserSettingsDao
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

class StudyRepository(
    private val taskDao: TaskDao,
    private val sessionDao: StudySessionDao,
    private val settingsDao: UserSettingsDao
) {
    val allTasks: Flow<List<Task>> = taskDao.getAllTasks()
    val pendingTasks: Flow<List<Task>> = taskDao.getPendingTasks()
    val allSessions: Flow<List<StudySession>> = sessionDao.getAllSessions()
    val userSettings: Flow<UserSettings?> = settingsDao.getSettings()

    suspend fun initializeDefaultsIfNeeded() {
        val existingSettings = settingsDao.getSettingsDirect()
        if (existingSettings == null) {
            val deviceId = "study-usr-" + UUID.randomUUID().toString().take(8)
            settingsDao.insertOrUpdate(
                UserSettings(
                    id = 1,
                    focusDurationMinutes = 25,
                    shortBreakDurationMinutes = 5,
                    longBreakDurationMinutes = 15,
                    sessionsBeforeLongBreak = 4,
                    autoStartBreaks = false,
                    autoStartFocus = false,
                    soundAlerts = true,
                    vibrationEnabled = true,
                    dailyGoalMinutes = 120,
                    themeMode = ThemeMode.SYSTEM,
                    syncAccountId = deviceId,
                    deviceName = "Android Device",
                    lastSyncTimestamp = System.currentTimeMillis() - 3600000L
                )
            )
        }

        // Check if tasks exist; if empty, seed useful initial assignments
        val tasksList = taskDao.getAllTasks().firstOrNull() ?: emptyList()
        if (tasksList.isEmpty()) {
            val now = System.currentTimeMillis()
            val dayMillis = 86400000L
            val initialTasks = listOf(
                Task(
                    title = "Complete Calculus Problem Set 4",
                    description = "Taylor series expansions and error bounds, problems 1-12",
                    subject = "Mathematics",
                    priority = Priority.HIGH,
                    dueDateMillis = now + dayMillis * 1,
                    estimatedPomodoros = 4,
                    completedPomodoros = 2,
                    isCompleted = false
                ),
                Task(
                    title = "Computer Architecture Lab: Cache Simulator",
                    description = "Implement LRU eviction policy and test miss rate on benchmarks",
                    subject = "Computer Science",
                    priority = Priority.HIGH,
                    dueDateMillis = now + dayMillis * 2,
                    estimatedPomodoros = 5,
                    completedPomodoros = 3,
                    isCompleted = false
                ),
                Task(
                    title = "Read Organic Chemistry Chapter 8",
                    description = "Stereochemistry & reaction mechanisms of haloalkanes",
                    subject = "Chemistry",
                    priority = Priority.MEDIUM,
                    dueDateMillis = now + dayMillis * 3,
                    estimatedPomodoros = 3,
                    completedPomodoros = 1,
                    isCompleted = false
                ),
                Task(
                    title = "Draft Outline for World History Essay",
                    description = "Industrial revolution socioeconomic impact analysis",
                    subject = "History",
                    priority = Priority.LOW,
                    dueDateMillis = now + dayMillis * 4,
                    estimatedPomodoros = 2,
                    completedPomodoros = 2,
                    isCompleted = true
                )
            )
            taskDao.insertTasks(initialTasks)

            // Seed historical sessions over the past 5 days for rich initial streak dashboard
            val initialSessions = mutableListOf<StudySession>()
            val subjects = listOf("Mathematics", "Computer Science", "Chemistry")
            for (dayOffset in 4 downTo 1) {
                val sessionTime = now - (dayOffset * dayMillis) + 14400000L
                initialSessions.add(
                    StudySession(
                        taskTitle = "Study Block Day -$dayOffset",
                        subject = subjects[dayOffset % subjects.size],
                        durationMinutes = if (dayOffset % 2 == 0) 50 else 25,
                        sessionType = SessionType.FOCUS,
                        completedAt = sessionTime
                    )
                )
            }
            // Today's earlier session
            initialSessions.add(
                StudySession(
                    taskTitle = "Morning Focus Session",
                    subject = "Computer Science",
                    durationMinutes = 25,
                    sessionType = SessionType.FOCUS,
                    completedAt = now - 7200000L
                )
            )
            sessionDao.insertSessions(initialSessions)
        }
    }

    // Task CRUD
    suspend fun insertTask(task: Task): Long = taskDao.insertTask(task)
    suspend fun updateTask(task: Task) = taskDao.updateTask(task.copy(updatedAt = System.currentTimeMillis()))
    suspend fun deleteTask(task: Task) = taskDao.deleteTask(task)
    suspend fun deleteTaskById(id: Long) = taskDao.deleteTaskById(id)
    suspend fun toggleTaskComplete(task: Task) {
        val updated = task.copy(
            isCompleted = !task.isCompleted,
            updatedAt = System.currentTimeMillis(),
            isSynced = false
        )
        taskDao.updateTask(updated)
    }

    suspend fun incrementTaskPomodoro(taskId: Long) {
        taskDao.incrementPomodoro(taskId)
    }

    // Session CRUD
    suspend fun logSession(
        taskId: Long?,
        taskTitle: String?,
        subject: String,
        durationMinutes: Int,
        sessionType: SessionType
    ): Long {
        val session = StudySession(
            taskId = taskId,
            taskTitle = taskTitle,
            subject = subject.ifBlank { "General" },
            durationMinutes = durationMinutes,
            sessionType = sessionType,
            completedAt = System.currentTimeMillis(),
            isSynced = false
        )
        val id = sessionDao.insertSession(session)
        if (taskId != null && sessionType == SessionType.FOCUS) {
            taskDao.incrementPomodoro(taskId)
        }
        return id
    }

    // Settings
    suspend fun updateSettings(settings: UserSettings) {
        settingsDao.insertOrUpdate(settings)
    }

    suspend fun getSettingsDirect(): UserSettings {
        return settingsDao.getSettingsDirect() ?: UserSettings()
    }

    // Progress Dashboard & Analytics
    fun getStreakInfo(): Flow<StreakInfo> {
        return allSessions.map { sessions ->
            calculateStreakInfo(sessions)
        }
    }

    private suspend fun calculateStreakInfo(sessions: List<StudySession>): StreakInfo {
        val settings = getSettingsDirect()
        val focusSessions = sessions.filter { it.sessionType == SessionType.FOCUS }
        val totalMinutes = focusSessions.sumOf { it.durationMinutes }
        val totalCount = focusSessions.size

        // Calculate unique study dates (YYYY-MM-DD)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val daysWithStudy = focusSessions.map {
            dateFormat.format(Date(it.completedAt))
        }.toSet()

        val calendar = Calendar.getInstance()
        val todayStr = dateFormat.format(calendar.time)

        // Today's minutes
        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val todayMinutes = focusSessions.filter { it.completedAt >= todayStart }.sumOf { it.durationMinutes }

        // Calculate streak
        var currentStreak = 0
        val checkCal = Calendar.getInstance()

        // If studied today, start counting streak from today; otherwise from yesterday
        if (daysWithStudy.contains(todayStr)) {
            currentStreak = 1
            checkCal.add(Calendar.DAY_OF_YEAR, -1)
        } else {
            // Check if studied yesterday to keep streak alive
            checkCal.add(Calendar.DAY_OF_YEAR, -1)
            val yesterdayStr = dateFormat.format(checkCal.time)
            if (daysWithStudy.contains(yesterdayStr)) {
                currentStreak = 1
                checkCal.add(Calendar.DAY_OF_YEAR, -1)
            }
        }

        while (currentStreak > 0) {
            val dateStr = dateFormat.format(checkCal.time)
            if (daysWithStudy.contains(dateStr)) {
                currentStreak++
                checkCal.add(Calendar.DAY_OF_YEAR, -1)
            } else {
                break
            }
        }

        // Longest streak calculation
        var longestStreak = currentStreak
        val sortedDays = daysWithStudy.sorted()
        var tempStreak = 0
        var prevCal: Calendar? = null

        for (dayStr in sortedDays) {
            val parsedDate = dateFormat.parse(dayStr) ?: continue
            val thisCal = Calendar.getInstance().apply { time = parsedDate }
            if (prevCal != null) {
                prevCal.add(Calendar.DAY_OF_YEAR, 1)
                if (dateFormat.format(prevCal.time) == dayStr) {
                    tempStreak++
                } else {
                    tempStreak = 1
                }
            } else {
                tempStreak = 1
            }
            prevCal = thisCal
            if (tempStreak > longestStreak) {
                longestStreak = tempStreak
            }
        }

        return StreakInfo(
            currentStreakDays = currentStreak,
            longestStreakDays = longestStreak.coerceAtLeast(currentStreak),
            totalStudyMinutes = totalMinutes,
            totalSessionsCount = totalCount,
            todayStudyMinutes = todayMinutes,
            dailyGoalMinutes = settings.dailyGoalMinutes,
            isTodayGoalAchieved = todayMinutes >= settings.dailyGoalMinutes
        )
    }

    // Weekly Study Records (Monday to Sunday)
    fun getWeeklyStudyRecords(): Flow<List<DayStudyRecord>> {
        return allSessions.map { sessions ->
            val calendar = Calendar.getInstance()
            calendar.firstDayOfWeek = Calendar.MONDAY
            calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)

            val weekStart = calendar.timeInMillis
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val dayNameFormat = SimpleDateFormat("EEE", Locale.getDefault())

            val days = mutableListOf<DayStudyRecord>()
            for (i in 0..6) {
                val dayTime = weekStart + (i * 86400000L)
                val dayEnd = dayTime + 86400000L
                val dateStr = dateFormat.format(Date(dayTime))
                val label = dayNameFormat.format(Date(dayTime))

                val minutes = sessions
                    .filter { it.sessionType == SessionType.FOCUS && it.completedAt in dayTime until dayEnd }
                    .sumOf { it.durationMinutes }

                days.add(DayStudyRecord(label, dateStr, minutes))
            }
            days
        }
    }

    // 28-day Heatmap
    fun getMonthlyHeatmap(): Flow<List<DayStudyRecord>> {
        return allSessions.map { sessions ->
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)

            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val dayLabelFormat = SimpleDateFormat("d", Locale.getDefault())

            val records = mutableListOf<DayStudyRecord>()
            for (i in 27 downTo 0) {
                val checkTime = calendar.timeInMillis - (i * 86400000L)
                val checkEnd = checkTime + 86400000L
                val dateStr = dateFormat.format(Date(checkTime))
                val dayLabel = dayLabelFormat.format(Date(checkTime))

                val minutes = sessions
                    .filter { it.sessionType == SessionType.FOCUS && it.completedAt in checkTime until checkEnd }
                    .sumOf { it.durationMinutes }

                records.add(DayStudyRecord(dayLabel, dateStr, minutes))
            }
            records
        }
    }

    // Subject breakdown
    fun getSubjectBreakdown(): Flow<List<SubjectStudyRecord>> {
        return allSessions.map { sessions ->
            val focusSessions = sessions.filter { it.sessionType == SessionType.FOCUS }
            val totalMinutes = focusSessions.sumOf { it.durationMinutes }
            if (totalMinutes == 0) {
                listOf(SubjectStudyRecord("General", 0, 1.0f))
            } else {
                focusSessions.groupBy { it.subject.ifBlank { "General" } }
                    .map { (subject, list) ->
                        val sumMins = list.sumOf { it.durationMinutes }
                        val pct = sumMins.toFloat() / totalMinutes.toFloat()
                        SubjectStudyRecord(subject, sumMins, pct)
                    }
                    .sortedByDescending { it.minutes }
            }
        }
    }

    // Milestones
    fun getMilestones(streakInfo: StreakInfo, completedTasksCount: Int): List<Milestone> {
        val totalMins = streakInfo.totalStudyMinutes
        val currentStreak = streakInfo.currentStreakDays

        return listOf(
            Milestone(
                id = "first_focus",
                title = "Spark of Focus",
                description = "Complete your first focus session",
                icon = "✨",
                isUnlocked = streakInfo.totalSessionsCount >= 1,
                progress = (streakInfo.totalSessionsCount.toFloat() / 1f).coerceIn(0f, 1f)
            ),
            Milestone(
                id = "streak_3",
                title = "3-Day Consistency",
                description = "Maintain a 3-day continuous study streak",
                icon = "🔥",
                isUnlocked = currentStreak >= 3,
                progress = (currentStreak.toFloat() / 3f).coerceIn(0f, 1f)
            ),
            Milestone(
                id = "streak_7",
                title = "Weekly Master",
                description = "Study consecutively for a full week (7 days)",
                icon = "⚡",
                isUnlocked = currentStreak >= 7,
                progress = (currentStreak.toFloat() / 7f).coerceIn(0f, 1f)
            ),
            Milestone(
                id = "hours_5",
                title = "5-Hour Scholar",
                description = "Accumulate 300 minutes of deep study",
                icon = "🎓",
                isUnlocked = totalMins >= 300,
                progress = (totalMins.toFloat() / 300f).coerceIn(0f, 1f)
            ),
            Milestone(
                id = "tasks_5",
                title = "Assignment Crusher",
                description = "Complete 5 assignments from your to-do list",
                icon = "🎯",
                isUnlocked = completedTasksCount >= 5,
                progress = (completedTasksCount.toFloat() / 5f).coerceIn(0f, 1f)
            ),
            Milestone(
                id = "marathon_20h",
                title = "Deep Work Centurion",
                description = "Reach 1,200 total minutes (20 hours) of focus",
                icon = "🏆",
                isUnlocked = totalMins >= 1200,
                progress = (totalMins.toFloat() / 1200f).coerceIn(0f, 1f)
            )
        )
    }

    // Cloud Sync Operations
    suspend fun getPendingSyncCount(): Int {
        val unsyncedTasks = taskDao.getUnsyncedTasks()
        val unsyncedSessions = sessionDao.getUnsyncedSessions()
        return unsyncedTasks.size + unsyncedSessions.size
    }

    suspend fun exportStudyBackupJson(): String {
        val tasks = taskDao.getAllTasks().first()
        val sessions = sessionDao.getAllSessions().first()
        val settings = getSettingsDirect()

        val jsonBuilder = StringBuilder()
        jsonBuilder.append("{\n")
        jsonBuilder.append("  \"syncAccountId\": \"${settings.syncAccountId}\",\n")
        jsonBuilder.append("  \"deviceName\": \"${settings.deviceName}\",\n")
        jsonBuilder.append("  \"timestamp\": ${System.currentTimeMillis()},\n")
        jsonBuilder.append("  \"taskCount\": ${tasks.size},\n")
        jsonBuilder.append("  \"sessionCount\": ${sessions.size},\n")
        jsonBuilder.append("  \"dailyGoalMinutes\": ${settings.dailyGoalMinutes},\n")
        jsonBuilder.append("  \"tasks\": [\n")
        tasks.forEachIndexed { i, t ->
            val comma = if (i < tasks.size - 1) "," else ""
            jsonBuilder.append("    {\"id\": ${t.id}, \"title\": \"${t.title.replace("\"", "\\\"")}\", \"subject\": \"${t.subject}\", \"priority\": \"${t.priority.name}\", \"isCompleted\": ${t.isCompleted}, \"pomodoros\": ${t.completedPomodoros}}$comma\n")
        }
        jsonBuilder.append("  ],\n")
        jsonBuilder.append("  \"sessions\": [\n")
        sessions.forEachIndexed { i, s ->
            val comma = if (i < sessions.size - 1) "," else ""
            jsonBuilder.append("    {\"id\": ${s.id}, \"subject\": \"${s.subject}\", \"durationMinutes\": ${s.durationMinutes}, \"completedAt\": ${s.completedAt}, \"type\": \"${s.sessionType.name}\"}$comma\n")
        }
        jsonBuilder.append("  ]\n")
        jsonBuilder.append("}")
        return jsonBuilder.toString()
    }

    suspend fun performCloudSync(): Boolean {
        // Mark unsynced items as synced and update settings lastSyncTimestamp
        val currentSettings = getSettingsDirect()
        taskDao.markAllSynced()
        sessionDao.markAllSynced()
        settingsDao.insertOrUpdate(
            currentSettings.copy(lastSyncTimestamp = System.currentTimeMillis())
        )
        return true
    }

    suspend fun importCloudData(tasksToImport: List<Task>, sessionsToImport: List<StudySession>) {
        if (tasksToImport.isNotEmpty()) {
            taskDao.insertTasks(tasksToImport)
        }
        if (sessionsToImport.isNotEmpty()) {
            sessionDao.insertSessions(sessionsToImport)
        }
        performCloudSync()
    }
}
