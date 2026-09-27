package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.StudySession
import kotlinx.coroutines.flow.Flow

@Dao
interface StudySessionDao {
    @Query("SELECT * FROM study_sessions ORDER BY completedAt DESC")
    fun getAllSessions(): Flow<List<StudySession>>

    @Query("SELECT * FROM study_sessions WHERE completedAt >= :startTime ORDER BY completedAt DESC")
    fun getSessionsSince(startTime: Long): Flow<List<StudySession>>

    @Query("SELECT * FROM study_sessions WHERE completedAt BETWEEN :startTime AND :endTime ORDER BY completedAt ASC")
    suspend fun getSessionsBetween(startTime: Long, endTime: Long): List<StudySession>

    @Query("SELECT SUM(durationMinutes) FROM study_sessions WHERE completedAt >= :startTime AND sessionType = 'FOCUS'")
    fun getFocusMinutesSince(startTime: Long): Flow<Int?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: StudySession): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessions(sessions: List<StudySession>)

    @Query("SELECT * FROM study_sessions WHERE isSynced = 0")
    suspend fun getUnsyncedSessions(): List<StudySession>

    @Query("UPDATE study_sessions SET isSynced = 1")
    suspend fun markAllSynced()
}
