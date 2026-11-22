package com.example.data.firebase

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class AiParsedReminderDraft(
    val title: String,
    val description: String,
    val daysFromToday: Int,
    val hour24: Int,
    val minute: Int,
    val repeatInterval: String,
    val priority: String,
    val category: String,
    val rationale: String
)

class AiAssistantRepository {
    private val reminderSchema = Schema.obj(
        properties = mapOf(
            "title" to Schema.string(description = "Concise reminder title, max 80 chars"),
            "description" to Schema.string(description = "Optional extra notes from user input"),
            "daysFromToday" to Schema.integer(description = "Integer days from today (0 = today, 1 = tomorrow, 7 = next week)"),
            "hour24" to Schema.integer(description = "24-hour clock hour from 0 to 23"),
            "minute" to Schema.integer(description = "Minute from 0 to 59"),
            "repeatInterval" to Schema.enumeration(
                values = listOf("NONE", "DAILY", "WEEKLY", "MONTHLY", "YEARLY"),
                description = "Recurrence pattern"
            ),
            "priority" to Schema.enumeration(
                values = listOf("LOW", "MEDIUM", "HIGH", "URGENT"),
                description = "Inferred priority level"
            ),
            "category" to Schema.string(description = "Short category like Study, Work, Health, Finance, Personal"),
            "rationale" to Schema.string(description = "Short 1-sentence explanation of suggested time and priority")
        ),
        optionalProperties = listOf("description")
    )

    private val structuredModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = reminderSchema
            }
        )
    }

    private val chatModel by lazy {
        Firebase.ai.generativeModel(modelName = "gemini-3.8-flash")
    }

    suspend fun parseReminderFromText(userPrompt: String): Result<AiParsedReminderDraft> {
        return try {
            val nowFormatted = SimpleDateFormat("EEEE, yyyy-MM-dd HH:mm", Locale.US).format(Date())
            val fullPrompt = """
                Current local date & time: $nowFormatted.
                Convert the user's request or note into a structured reminder schedule.
                If no explicit time is mentioned, suggest a practical hour (e.g., 9:00 or 18:00) and explain why in rationale.
                User input: "$userPrompt"
            """.trimIndent()

            val response = withTimeout(60_000L) {
                structuredModel.generateContent(fullPrompt)
            }
            val jsonText = response.text
                ?: return Result.failure(IllegalStateException("AI model returned an empty response."))
            val json = JSONObject(jsonText)
            Result.success(
                AiParsedReminderDraft(
                    title = json.optString("title", userPrompt.take(60)).ifBlank { "Reminder" },
                    description = json.optString("description", ""),
                    daysFromToday = json.optInt("daysFromToday", 0).coerceIn(0, 365),
                    hour24 = json.optInt("hour24", 9).coerceIn(0, 23),
                    minute = json.optInt("minute", 0).coerceIn(0, 59),
                    repeatInterval = json.optString("repeatInterval", "NONE"),
                    priority = json.optString("priority", "MEDIUM"),
                    category = json.optString("category", "Personal").ifBlank { "Personal" },
                    rationale = json.optString("rationale", "Scheduled based on your request.")
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "AI reminder parsing timed out", e)
            Result.failure(Exception("AI request timed out after 60 seconds. Please try again."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AI reminder parsing failed", e)
            Result.failure(e)
        }
    }

    fun streamProductivityResponse(prompt: String): Flow<String> = flow {
        val accumulated = StringBuilder()
        try {
            withTimeout(60_000L) {
                chatModel.generateContentStream(prompt).collect { chunk ->
                    val piece = chunk.text.orEmpty()
                    if (piece.isNotEmpty()) {
                        accumulated.append(piece)
                        emit(accumulated.toString())
                    }
                }
            }
            if (accumulated.isEmpty()) {
                throw IllegalStateException("AI model returned an empty response.")
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "AI streaming timed out", e)
            throw Exception("AI response timed out after 60 seconds. Please try again.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AI streaming failed", e)
            throw e
        }
    }

    companion object {
        private const val TAG = "AiAssistantRepo"
    }
}
