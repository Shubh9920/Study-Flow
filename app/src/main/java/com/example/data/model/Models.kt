package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class Priority {
    LOW, MEDIUM, HIGH
}

enum class SessionType {
    FOCUS, SHORT_BREAK, LONG_BREAK
}

enum class ThemeMode {
    SYSTEM, DARK, LIGHT
}

enum class SyncState {
    IDLE, SYNCING, SYNCED, ERROR
}

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val subject: String = "General",
    val priority: Priority = Priority.MEDIUM,
    val dueDateMillis: Long? = null,
    val estimatedPomodoros: Int = 1,
    val completedPomodoros: Int = 0,
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
)

@Entity(tableName = "study_sessions")
data class StudySession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val taskId: Long? = null,
    val taskTitle: String? = null,
    val subject: String = "General",
    val durationMinutes: Int,
    val sessionType: SessionType = SessionType.FOCUS,
    val completedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
)

@Entity(tableName = "user_settings")
data class UserSettings(
    @PrimaryKey
    val id: Int = 1,
    val focusDurationMinutes: Int = 25,
    val shortBreakDurationMinutes: Int = 5,
    val longBreakDurationMinutes: Int = 15,
    val sessionsBeforeLongBreak: Int = 4,
    val autoStartBreaks: Boolean = false,
    val autoStartFocus: Boolean = false,
    val soundAlerts: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val dailyGoalMinutes: Int = 120,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val syncAccountId: String = "",
    val deviceName: String = "My Android Device",
    val lastSyncTimestamp: Long = 0L,
    val isSignedInWithGoogle: Boolean = false,
    val googleEmail: String? = null,
    val googleDisplayName: String? = null,
    val googlePhotoUrl: String? = null
)

data class StreakInfo(
    val currentStreakDays: Int = 0,
    val longestStreakDays: Int = 0,
    val totalStudyMinutes: Int = 0,
    val totalSessionsCount: Int = 0,
    val todayStudyMinutes: Int = 0,
    val dailyGoalMinutes: Int = 120,
    val isTodayGoalAchieved: Boolean = false
)

data class DayStudyRecord(
    val dayLabel: String, // "Mon", "Tue"
    val dateString: String, // "2026-09-27"
    val minutes: Int
)

data class SubjectStudyRecord(
    val subject: String,
    val minutes: Int,
    val percentage: Float
)

data class Milestone(
    val id: String,
    val title: String,
    val description: String,
    val icon: String,
    val isUnlocked: Boolean,
    val progress: Float
)
