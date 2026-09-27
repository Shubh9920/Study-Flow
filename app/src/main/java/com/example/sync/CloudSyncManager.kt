package com.example.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.data.model.Priority
import com.example.data.model.SessionType
import com.example.data.model.StudySession
import com.example.data.model.SyncState
import com.example.data.model.Task
import com.example.data.repository.StudyRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CloudSyncManager(
    private val context: Context,
    private val repository: StudyRepository
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _syncState = MutableStateFlow(SyncState.IDLE)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val _pendingChanges = MutableStateFlow(0)
    val pendingChanges: StateFlow<Int> = _pendingChanges.asStateFlow()

    private val _lastSyncDisplay = MutableStateFlow("Never")
    val lastSyncDisplay: StateFlow<String> = _lastSyncDisplay.asStateFlow()

    init {
        refreshStatus()
    }

    fun isOnline(): Boolean {
        return try {
            val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val network = connectivityManager?.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))
        } catch (_: Exception) {
            false
        }
    }

    fun refreshStatus() {
        scope.launch {
            val count = repository.getPendingSyncCount()
            _pendingChanges.value = count
            val settings = repository.getSettingsDirect()
            if (settings.lastSyncTimestamp > 0) {
                val sdf = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
                _lastSyncDisplay.value = sdf.format(Date(settings.lastSyncTimestamp))
            } else {
                _lastSyncDisplay.value = "Never synced"
            }
        }
    }

    fun syncNow(onComplete: (Boolean, String) -> Unit) {
        scope.launch {
            if (_syncState.value == SyncState.SYNCING) return@launch
            _syncState.value = SyncState.SYNCING

            val online = isOnline()
            // Even if offline, we ensure local changes are cleanly cached and prepared
            delay(1200) // Simulating network handshake with multi-device cloud endpoint

            try {
                repository.performCloudSync()
                _syncState.value = SyncState.SYNCED
                refreshStatus()
                if (online) {
                    onComplete(true, "Cloud synchronization successful! All devices up to date.")
                } else {
                    onComplete(true, "Local changes saved offline. Ready to sync when online.")
                }
            } catch (e: Exception) {
                _syncState.value = SyncState.ERROR
                onComplete(false, "Sync error: ${e.message}")
            }
        }
    }

    suspend fun createExportBackup(): String {
        return repository.exportStudyBackupJson()
    }

    fun importBackup(jsonString: String, onResult: (Boolean, String) -> Unit) {
        scope.launch {
            try {
                val json = JSONObject(jsonString)
                val tasksJson = json.optJSONArray("tasks")
                val sessionsJson = json.optJSONArray("sessions")

                val parsedTasks = mutableListOf<Task>()
                if (tasksJson != null) {
                    for (i in 0 until tasksJson.length()) {
                        val item = tasksJson.getJSONObject(i)
                        parsedTasks.add(
                            Task(
                                title = item.optString("title", "Imported Task"),
                                subject = item.optString("subject", "General"),
                                priority = runCatching {
                                    Priority.valueOf(item.optString("priority", "MEDIUM"))
                                }.getOrDefault(Priority.MEDIUM),
                                isCompleted = item.optBoolean("isCompleted", false),
                                completedPomodoros = item.optInt("pomodoros", 0),
                                isSynced = true
                            )
                        )
                    }
                }

                val parsedSessions = mutableListOf<StudySession>()
                if (sessionsJson != null) {
                    for (i in 0 until sessionsJson.length()) {
                        val item = sessionsJson.getJSONObject(i)
                        parsedSessions.add(
                            StudySession(
                                subject = item.optString("subject", "General"),
                                durationMinutes = item.optInt("durationMinutes", 25),
                                completedAt = item.optLong("completedAt", System.currentTimeMillis()),
                                sessionType = runCatching {
                                    SessionType.valueOf(item.optString("type", "FOCUS"))
                                }.getOrDefault(SessionType.FOCUS),
                                isSynced = true
                            )
                        )
                    }
                }

                repository.importCloudData(parsedTasks, parsedSessions)
                refreshStatus()
                onResult(true, "Successfully imported ${parsedTasks.size} tasks and ${parsedSessions.size} sessions!")
            } catch (e: Exception) {
                onResult(false, "Failed to parse backup data: ${e.message}")
            }
        }
    }
}
