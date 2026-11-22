package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.AiAssistantRepository
import com.example.data.firebase.AiParsedReminderDraft
import com.example.data.firebase.BackupRecord
import com.example.data.firebase.CloudBackupRepository
import com.example.data.firebase.CloudRestorePayload
import com.example.data.local.AlarmEntity
import com.example.data.local.CalendarEventEntity
import com.example.data.local.ReminderEntity
import com.example.data.local.UserPreferencesRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface AiParseUiState {
    data object Idle : AiParseUiState
    data object Loading : AiParseUiState
    data class DraftReady(val draft: AiParsedReminderDraft) : AiParseUiState
    data class Error(val message: String) : AiParseUiState
}

sealed interface AiInsightUiState {
    data object Idle : AiInsightUiState
    data object Loading : AiInsightUiState
    data class Streaming(val text: String) : AiInsightUiState
    data class Success(val text: String) : AiInsightUiState
    data class Error(val message: String) : AiInsightUiState
}

sealed interface CloudHistoryUiState {
    data object Loading : CloudHistoryUiState
    data class Success(val records: List<BackupRecord>) : CloudHistoryUiState
    data class Error(val message: String) : CloudHistoryUiState
}

sealed interface CloudActionUiState {
    data object Idle : CloudActionUiState
    data object InProgress : CloudActionUiState
    data class Message(val text: String, val isError: Boolean = false) : CloudActionUiState
}

class AiAndCloudViewModel(
    private val aiRepo: AiAssistantRepository,
    private val cloudRepo: CloudBackupRepository,
    private val prefsRepo: UserPreferencesRepository,
    private val currentUserId: String?
) : ViewModel() {

    private val _parseState = MutableStateFlow<AiParseUiState>(AiParseUiState.Idle)
    val parseState: StateFlow<AiParseUiState> = _parseState.asStateFlow()

    private val _insightState = MutableStateFlow<AiInsightUiState>(AiInsightUiState.Idle)
    val insightState: StateFlow<AiInsightUiState> = _insightState.asStateFlow()

    private val _cloudActionState = MutableStateFlow<CloudActionUiState>(CloudActionUiState.Idle)
    val cloudActionState: StateFlow<CloudActionUiState> = _cloudActionState.asStateFlow()

    val backupHistoryState: StateFlow<CloudHistoryUiState> = if (!currentUserId.isNullOrBlank()) {
        cloudRepo.observeBackupHistory(currentUserId)
            .map<List<BackupRecord>, CloudHistoryUiState> { CloudHistoryUiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing backup history", rootError)
                emit(CloudHistoryUiState.Error(rootError.message ?: "Failed to load cloud backup history"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = CloudHistoryUiState.Loading
            )
    } else {
        MutableStateFlow<CloudHistoryUiState>(CloudHistoryUiState.Success(emptyList())).asStateFlow()
    }

    fun parseNaturalLanguageReminder(input: String) {
        if (input.isBlank()) return
        viewModelScope.launch {
            _parseState.value = AiParseUiState.Loading
            aiRepo.parseReminderFromText(input.trim())
                .onSuccess { draft -> _parseState.value = AiParseUiState.DraftReady(draft) }
                .onFailure { err ->
                    _parseState.value = AiParseUiState.Error(err.localizedMessage ?: "Failed to parse reminder.")
                }
        }
    }

    fun dismissDraft() {
        _parseState.value = AiParseUiState.Idle
    }

    fun runProductivityPrompt(prompt: String) {
        if (prompt.isBlank()) return
        viewModelScope.launch {
            _insightState.value = AiInsightUiState.Loading
            var lastText = ""
            try {
                aiRepo.streamProductivityResponse(prompt).collect { partial ->
                    lastText = partial
                    _insightState.value = AiInsightUiState.Streaming(partial)
                }
                _insightState.value = AiInsightUiState.Success(lastText)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "AI insight streaming failed", e)
                _insightState.value = AiInsightUiState.Error(e.localizedMessage ?: "AI Assistant unavailable.")
            }
        }
    }

    fun triggerCloudBackup(
        reminders: List<ReminderEntity>,
        alarms: List<AlarmEntity>,
        events: List<CalendarEventEntity>
    ) {
        viewModelScope.launch {
            _cloudActionState.value = CloudActionUiState.InProgress
            cloudRepo.backupAll(reminders, alarms, events)
                .onSuccess { record ->
                    prefsRepo.setLastBackupMillis(System.currentTimeMillis())
                    _cloudActionState.value = CloudActionUiState.Message(
                        "Backed up ${record.reminderCount} reminders, ${record.alarmCount} alarms, ${record.eventCount} events."
                    )
                }
                .onFailure { err ->
                    _cloudActionState.value = CloudActionUiState.Message(
                        err.localizedMessage ?: "Cloud backup failed.",
                        isError = true
                    )
                }
        }
    }

    fun triggerCloudRestore(onApply: suspend (CloudRestorePayload) -> Unit) {
        viewModelScope.launch {
            _cloudActionState.value = CloudActionUiState.InProgress
            cloudRepo.restoreFromCloud()
                .onSuccess { payload ->
                    onApply(payload)
                    _cloudActionState.value = CloudActionUiState.Message(
                        "Restored ${payload.reminders.size} reminders, ${payload.alarms.size} alarms, ${payload.events.size} events."
                    )
                }
                .onFailure { err ->
                    _cloudActionState.value = CloudActionUiState.Message(
                        err.localizedMessage ?: "Cloud restore failed.",
                        isError = true
                    )
                }
        }
    }

    fun clearCloudMessage() {
        _cloudActionState.value = CloudActionUiState.Idle
    }

    companion object {
        private const val TAG = "AiAndCloudVM"
    }
}
