package com.example

import com.example.util.DateTimeUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class ExampleUnitTest {
    @Test
    fun nextAlarmTriggerAdvancesPastNow() {
        val now = Calendar.getInstance().apply {
            set(2026, Calendar.MAY, 10, 8, 30, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val next = DateTimeUtils.computeNextAlarmTriggerMillis(
            hour = 7,
            minute = 0,
            weekdays = emptySet(),
            nowMillis = now
        )
        assertTrue(next > now)
    }

    @Test
    fun recurringReminderComputesFutureOccurrence() {
        val now = System.currentTimeMillis()
        val dailyNext = DateTimeUtils.computeNextRecurringReminderMillis(now - 10_000L, "DAILY", 1)
        assertNotNull(dailyNext)
        assertTrue(dailyNext!! > now)

        val oneTime = DateTimeUtils.computeNextRecurringReminderMillis(now, "NONE", 1)
        assertNull(oneTime)
    }

    @Test
    fun durationFormattingHandlesMinutesAndHours() {
        assertEquals("05:09", DateTimeUtils.formatDurationMillis(309_000L, includeCentiseconds = false))
        assertEquals("01:02:03.45", DateTimeUtils.formatDurationMillis(3_723_450L, includeCentiseconds = true))
    }
}
