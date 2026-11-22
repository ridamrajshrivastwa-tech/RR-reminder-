package com.example.data.firebase

import android.content.Context
import android.os.Build
import com.example.R
import com.example.data.local.AlarmEntity
import com.example.data.local.CalendarEventEntity
import com.example.data.local.ReminderEntity
import com.example.data.local.ReminderPriority
import com.example.data.local.RepeatInterval
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

data class CloudRestorePayload(
    val reminders: List<ReminderEntity>,
    val alarms: List<AlarmEntity>,
    val events: List<CalendarEventEntity>
)

class CloudBackupRepository(private val db: FirebaseFirestore) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            throw IllegalStateException("User must be signed in with Google before accessing cloud backup.")
        }
        return uid
    }

    private fun sanitizeDocId(raw: String): String {
        val cleaned = raw.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
        return cleaned.take(128).ifEmpty { UUID.randomUUID().toString() }
    }

    fun observeBackupHistory(userId: String): Flow<List<BackupRecord>> = flow {
        if (userId.isBlank()) {
            emit(emptyList())
            return@flow
        }
        val path = "users/$userId/backup_history"
        emitAll(
            db.collection("users").document(userId).collection("backup_history")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        BackupRecord::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun backupAll(
        reminders: List<ReminderEntity>,
        alarms: List<AlarmEntity>,
        events: List<CalendarEventEntity>
    ): Result<BackupRecord> {
        var currentPath = "users/unauthenticated"
        return try {
            val uid = requireUserId()
            val userDoc = db.collection("users").document(uid)

            reminders.forEach { r ->
                val docId = sanitizeDocId(r.id)
                val ref = userDoc.collection("reminders").document(docId)
                currentPath = ref.path
                val safePriority = ReminderPriority.entries.firstOrNull { it.name == r.priority }?.name ?: "MEDIUM"
                val safeRepeat = RepeatInterval.entries.firstOrNull { it.name == r.repeatInterval }?.name ?: "NONE"
                val payload = mapOf(
                    "userId" to uid,
                    "title" to r.title.trim().ifEmpty { "Reminder" }.take(120),
                    "description" to r.description.take(1000),
                    "triggerAtMillis" to r.triggerAtMillis.coerceAtLeast(1L),
                    "repeatInterval" to safeRepeat,
                    "customRepeatDays" to r.customRepeatDays.coerceIn(0, 3650),
                    "priority" to safePriority,
                    "category" to r.category.trim().ifEmpty { "Personal" }.take(40),
                    "colorHex" to r.colorHex,
                    "completed" to r.completed,
                    "archived" to r.archived,
                    "snoozedUntilMillis" to r.snoozedUntilMillis.coerceAtLeast(0L),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                ref.set(payload).await()
            }

            alarms.forEach { a ->
                val docId = sanitizeDocId(a.id)
                val ref = userDoc.collection("alarms").document(docId)
                currentPath = ref.path
                val payload = mapOf(
                    "userId" to uid,
                    "label" to a.label.take(80),
                    "hour" to a.hour.coerceIn(0, 23),
                    "minute" to a.minute.coerceIn(0, 59),
                    "enabled" to a.enabled,
                    "vibrate" to a.vibrate,
                    "gradualVolume" to a.gradualVolume,
                    "snoozeMinutes" to a.snoozeMinutes.coerceIn(1, 60),
                    "toneName" to a.toneName.ifBlank { "Dawn Chime" }.take(60),
                    "weekdaysCsv" to a.weekdaysCsv.take(30),
                    "bedtimeAlarm" to a.bedtimeAlarm,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                ref.set(payload).await()
            }

            events.forEach { e ->
                val docId = sanitizeDocId(e.id)
                val ref = userDoc.collection("events").document(docId)
                currentPath = ref.path
                val payload = mapOf(
                    "userId" to uid,
                    "title" to e.title.trim().ifEmpty { "Event" }.take(120),
                    "notes" to e.notes.take(1000),
                    "dateEpochDay" to e.dateEpochDay,
                    "startMinutes" to e.startMinutes.coerceIn(0, 1439),
                    "endMinutes" to e.endMinutes.coerceIn(0, 1439),
                    "colorHex" to e.colorHex,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                ref.set(payload).await()
            }

            val historyId = "bkp_${System.currentTimeMillis()}"
            val historyRef = userDoc.collection("backup_history").document(historyId)
            currentPath = historyRef.path
            val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}".trim().ifBlank { "Android" }.take(80)
            val historyMap = mapOf(
                "userId" to uid,
                "reminderCount" to reminders.size.coerceIn(0, 10000),
                "alarmCount" to alarms.size.coerceIn(0, 10000),
                "eventCount" to events.size.coerceIn(0, 10000),
                "status" to "SUCCESS",
                "deviceLabel" to deviceName,
                "createdAt" to FieldValue.serverTimestamp()
            )
            historyRef.set(historyMap).await()

            Result.success(
                BackupRecord(
                    id = historyId,
                    userId = uid,
                    reminderCount = reminders.size,
                    alarmCount = alarms.size,
                    eventCount = events.size,
                    status = "SUCCESS",
                    deviceLabel = deviceName
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, currentPath)
            Result.failure(e)
        }
    }

    suspend fun restoreFromCloud(): Result<CloudRestorePayload> {
        var currentPath = "users/unauthenticated"
        return try {
            val uid = requireUserId()
            val userDoc = db.collection("users").document(uid)

            currentPath = "users/$uid/reminders"
            val remSnap = userDoc.collection("reminders").get().await()
            val reminders = remSnap.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                ReminderEntity(
                    id = doc.id,
                    title = (data["title"] as? String).orEmpty().ifBlank { "Reminder" },
                    description = (data["description"] as? String).orEmpty(),
                    triggerAtMillis = (data["triggerAtMillis"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                    repeatInterval = (data["repeatInterval"] as? String) ?: RepeatInterval.NONE.name,
                    customRepeatDays = (data["customRepeatDays"] as? Number)?.toInt() ?: 1,
                    priority = (data["priority"] as? String) ?: ReminderPriority.MEDIUM.name,
                    category = (data["category"] as? String) ?: "Personal",
                    colorHex = (data["colorHex"] as? Number)?.toInt() ?: 0xFF3B82F6.toInt(),
                    completed = (data["completed"] as? Boolean) ?: false,
                    archived = (data["archived"] as? Boolean) ?: false,
                    snoozedUntilMillis = (data["snoozedUntilMillis"] as? Number)?.toLong() ?: 0L
                )
            }

            currentPath = "users/$uid/alarms"
            val alarmSnap = userDoc.collection("alarms").get().await()
            val alarms = alarmSnap.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                AlarmEntity(
                    id = doc.id,
                    label = (data["label"] as? String).orEmpty(),
                    hour = ((data["hour"] as? Number)?.toInt() ?: 7).coerceIn(0, 23),
                    minute = ((data["minute"] as? Number)?.toInt() ?: 0).coerceIn(0, 59),
                    enabled = (data["enabled"] as? Boolean) ?: true,
                    vibrate = (data["vibrate"] as? Boolean) ?: true,
                    gradualVolume = (data["gradualVolume"] as? Boolean) ?: true,
                    snoozeMinutes = ((data["snoozeMinutes"] as? Number)?.toInt() ?: 10).coerceIn(1, 60),
                    toneName = (data["toneName"] as? String) ?: "Dawn Chime",
                    weekdaysCsv = (data["weekdaysCsv"] as? String).orEmpty(),
                    bedtimeAlarm = (data["bedtimeAlarm"] as? Boolean) ?: false
                )
            }

            currentPath = "users/$uid/events"
            val eventSnap = userDoc.collection("events").get().await()
            val events = eventSnap.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                CalendarEventEntity(
                    id = doc.id,
                    title = (data["title"] as? String).orEmpty().ifBlank { "Event" },
                    notes = (data["notes"] as? String).orEmpty(),
                    dateEpochDay = (data["dateEpochDay"] as? Number)?.toLong() ?: 0L,
                    startMinutes = ((data["startMinutes"] as? Number)?.toInt() ?: 540).coerceIn(0, 1439),
                    endMinutes = ((data["endMinutes"] as? Number)?.toInt() ?: 600).coerceIn(0, 1439),
                    colorHex = (data["colorHex"] as? Number)?.toInt() ?: 0xFF3B82F6.toInt()
                )
            }

            val historyId = "rst_${System.currentTimeMillis()}"
            val historyRef = userDoc.collection("backup_history").document(historyId)
            currentPath = historyRef.path
            val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}".trim().ifBlank { "Android" }.take(80)
            historyRef.set(
                mapOf(
                    "userId" to uid,
                    "reminderCount" to reminders.size,
                    "alarmCount" to alarms.size,
                    "eventCount" to events.size,
                    "status" to "RESTORED",
                    "deviceLabel" to deviceName,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()

            Result.success(CloudRestorePayload(reminders, alarms, events))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, currentPath)
            Result.failure(e)
        }
    }
}
