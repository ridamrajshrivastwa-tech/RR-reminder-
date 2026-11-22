package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AlarmEntity
import com.example.data.local.CalendarEventEntity
import com.example.data.local.ReminderEntity
import com.example.data.local.RoutineEntity
import com.example.system.BackupJsonSerializer
import com.example.system.LocalBackupBundle
import com.example.system.SecurityUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun appNameMatchesRRReminder() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("RR Reminder", appName)
    }

    @Test
    fun pinHashingAndVerificationWorks() {
        val salt = SecurityUtils.generateSalt()
        val hash = SecurityUtils.hashPin("4829", salt)
        assertTrue(SecurityUtils.verifyPin("4829", hash, salt))
        assertFalse(SecurityUtils.verifyPin("1234", hash, salt))
    }

    @Test
    fun backupJsonRoundTripPreservesAllCollections() {
        val bundle = LocalBackupBundle(
            reminders = listOf(
                ReminderEntity(
                    id = "r-1",
                    title = "Submit quarterly report",
                    triggerAtMillis = 1_800_000_000_000L,
                    repeatInterval = "WEEKLY",
                    priority = "HIGH",
                    category = "Work"
                )
            ),
            alarms = listOf(
                AlarmEntity(
                    id = "a-1",
                    label = "Morning Run",
                    hour = 6,
                    minute = 30,
                    weekdaysCsv = "2,4,6"
                )
            ),
            events = listOf(
                CalendarEventEntity(
                    id = "e-1",
                    title = "Design Sync",
                    dateEpochDay = 20_500L,
                    startMinutes = 600,
                    endMinutes = 660
                )
            ),
            routines = listOf(
                RoutineEntity(
                    id = "rt-1",
                    title = "Hydrate",
                    streakCount = 5
                )
            )
        )

        val json = BackupJsonSerializer.exportToJson(bundle)
        val restored = BackupJsonSerializer.importFromJson(json)

        assertEquals(1, restored.reminders.size)
        assertEquals("Submit quarterly report", restored.reminders.first().title)
        assertEquals(1, restored.alarms.size)
        assertEquals(6, restored.alarms.first().hour)
        assertEquals(1, restored.events.size)
        assertEquals("Design Sync", restored.events.first().title)
        assertEquals(1, restored.routines.size)
        assertEquals(5, restored.routines.first().streakCount)
    }
}
