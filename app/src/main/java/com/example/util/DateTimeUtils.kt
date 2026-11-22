package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs

data class WorldCityOption(
    val cityName: String,
    val country: String,
    val timeZoneId: String
)

object DateTimeUtils {

    val POPULAR_WORLD_CITIES = listOf(
        WorldCityOption("New York", "United States", "America/New_York"),
        WorldCityOption("Los Angeles", "United States", "America/Los_Angeles"),
        WorldCityOption("Chicago", "United States", "America/Chicago"),
        WorldCityOption("Toronto", "Canada", "America/Toronto"),
        WorldCityOption("London", "United Kingdom", "Europe/London"),
        WorldCityOption("Paris", "France", "Europe/Paris"),
        WorldCityOption("Berlin", "Germany", "Europe/Berlin"),
        WorldCityOption("Zurich", "Switzerland", "Europe/Zurich"),
        WorldCityOption("Dubai", "UAE", "Asia/Dubai"),
        WorldCityOption("Mumbai", "India", "Asia/Kolkata"),
        WorldCityOption("Singapore", "Singapore", "Asia/Singapore"),
        WorldCityOption("Tokyo", "Japan", "Asia/Tokyo"),
        WorldCityOption("Seoul", "South Korea", "Asia/Seoul"),
        WorldCityOption("Sydney", "Australia", "Australia/Sydney"),
        WorldCityOption("Auckland", "New Zealand", "Pacific/Auckland"),
        WorldCityOption("Sao Paulo", "Brazil", "America/Sao_Paulo"),
        WorldCityOption("Cairo", "Egypt", "Africa/Cairo"),
        WorldCityOption("Johannesburg", "South Africa", "Africa/Johannesburg"),
        WorldCityOption("Bangkok", "Thailand", "Asia/Bangkok"),
        WorldCityOption("Hong Kong", "China", "Asia/Hong_Kong"),
        WorldCityOption("Istanbul", "Turkey", "Europe/Istanbul"),
        WorldCityOption("Mexico City", "Mexico", "America/Mexico_City"),
        WorldCityOption("Amsterdam", "Netherlands", "Europe/Amsterdam"),
        WorldCityOption("Stockholm", "Sweden", "Europe/Stockholm")
    )

    fun searchAllTimeZones(query: String): List<WorldCityOption> {
        val trimmed = query.trim().lowercase(Locale.getDefault())
        if (trimmed.isEmpty()) return POPULAR_WORLD_CITIES

        val popularMatches = POPULAR_WORLD_CITIES.filter {
            it.cityName.lowercase(Locale.getDefault()).contains(trimmed) ||
                it.country.lowercase(Locale.getDefault()).contains(trimmed) ||
                it.timeZoneId.lowercase(Locale.getDefault()).contains(trimmed)
        }
        val popularIds = popularMatches.map { it.timeZoneId }.toSet()

        val systemMatches = TimeZone.getAvailableIDs()
            .asSequence()
            .filter { id -> id.contains("/") && !id.startsWith("Etc/") && id !in popularIds }
            .filter { id -> id.lowercase(Locale.getDefault()).contains(trimmed) }
            .take(30)
            .map { id ->
                val parts = id.split("/")
                val city = parts.last().replace("_", " ")
                val region = parts.first().replace("_", " ")
                WorldCityOption(cityName = city, country = region, timeZoneId = id)
            }
            .toList()

        return (popularMatches + systemMatches).take(40)
    }

    fun greetingForHour(hour24: Int): String = when (hour24) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..20 -> "Good evening"
        else -> "Good night"
    }

    fun formatDigitalTime(millis: Long, use24Hour: Boolean, showSeconds: Boolean = true, timeZone: TimeZone = TimeZone.getDefault()): String {
        val pattern = when {
            use24Hour && showSeconds -> "HH:mm:ss"
            use24Hour && !showSeconds -> "HH:mm"
            !use24Hour && showSeconds -> "hh:mm:ss a"
            else -> "h:mm a"
        }
        val sdf = SimpleDateFormat(pattern, Locale.getDefault())
        sdf.timeZone = timeZone
        return sdf.format(Date(millis))
    }

    fun formatHourMinute(hour24: Int, minute: Int, use24Hour: Boolean): String {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour24)
            set(Calendar.MINUTE, minute)
        }
        val pattern = if (use24Hour) "HH:mm" else "h:mm a"
        return SimpleDateFormat(pattern, Locale.getDefault()).format(cal.time)
    }

    fun formatFullDate(millis: Long, timeZone: TimeZone = TimeZone.getDefault()): String {
        val sdf = SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault())
        sdf.timeZone = timeZone
        return sdf.format(Date(millis))
    }

    fun formatShortDateTime(millis: Long, use24Hour: Boolean): String {
        val pattern = if (use24Hour) "MMM d, HH:mm" else "MMM d, h:mm a"
        return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(millis))
    }

    fun formatRelativeDateLabel(targetMillis: Long, nowMillis: Long = System.currentTimeMillis()): String {
        val targetDay = epochDayForMillis(targetMillis)
        val today = epochDayForMillis(nowMillis)
        return when (targetDay - today) {
            0L -> "Today"
            1L -> "Tomorrow"
            -1L -> "Yesterday"
            else -> SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(Date(targetMillis))
        }
    }

    fun epochDayForMillis(millis: Long, timeZone: TimeZone = TimeZone.getDefault()): Long {
        val cal = Calendar.getInstance(timeZone).apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val localOffset = timeZone.getOffset(cal.timeInMillis)
        return (cal.timeInMillis + localOffset) / 86_400_000L
    }

    fun millisForEpochDay(epochDay: Long, hour: Int = 9, minute: Int = 0): Long {
        val utcMidnight = epochDay * 86_400_000L
        val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = utcMidnight
        }
        return Calendar.getInstance().apply {
            set(Calendar.YEAR, utcCal.get(Calendar.YEAR))
            set(Calendar.MONTH, utcCal.get(Calendar.MONTH))
            set(Calendar.DAY_OF_MONTH, utcCal.get(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    fun isSameLocalDay(millisA: Long, millisB: Long): Boolean {
        val calA = Calendar.getInstance().apply { timeInMillis = millisA }
        val calB = Calendar.getInstance().apply { timeInMillis = millisB }
        return calA.get(Calendar.YEAR) == calB.get(Calendar.YEAR) &&
            calA.get(Calendar.DAY_OF_YEAR) == calB.get(Calendar.DAY_OF_YEAR)
    }

    fun isDaytimeInZone(nowMillis: Long, timeZone: TimeZone): Boolean {
        val cal = Calendar.getInstance(timeZone).apply { timeInMillis = nowMillis }
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        return hour in 6..18
    }

    fun formatTimeZoneDifference(nowMillis: Long, targetZone: TimeZone): String {
        val localZone = TimeZone.getDefault()
        val diffMs = targetZone.getOffset(nowMillis) - localZone.getOffset(nowMillis)
        val totalMinutes = diffMs / 60_000
        val sign = if (totalMinutes >= 0) "+" else "-"
        val absMinutes = abs(totalMinutes)
        val hours = absMinutes / 60
        val mins = absMinutes % 60
        val offsetText = when {
            totalMinutes == 0 -> "Same time"
            mins == 0 -> "$sign${hours}h"
            else -> "$sign${hours}h ${mins}m"
        }

        val targetDay = epochDayForMillis(nowMillis, targetZone)
        val localDay = epochDayForMillis(nowMillis, localZone)
        val dayLabel = when (targetDay - localDay) {
            0L -> "Today"
            1L -> "Tomorrow"
            -1L -> "Yesterday"
            else -> SimpleDateFormat("MMM d", Locale.getDefault()).apply { timeZone = targetZone }.format(Date(nowMillis))
        }
        return "$dayLabel • $offsetText"
    }

    fun formatDurationMillis(millis: Long, includeCentiseconds: Boolean = false): String {
        val clamped = millis.coerceAtLeast(0L)
        val totalSeconds = clamped / 1000L
        val hours = totalSeconds / 3600L
        val minutes = (totalSeconds % 3600L) / 60L
        val seconds = totalSeconds % 60L
        val centis = (clamped % 1000L) / 10L
        return if (includeCentiseconds) {
            if (hours > 0) {
                String.format(Locale.US, "%02d:%02d:%02d.%02d", hours, minutes, seconds, centis)
            } else {
                String.format(Locale.US, "%02d:%02d.%02d", minutes, seconds, centis)
            }
        } else {
            if (hours > 0) {
                String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format(Locale.US, "%02d:%02d", minutes, seconds)
            }
        }
    }

    fun computeNextAlarmTriggerMillis(
        hour: Int,
        minute: Int,
        weekdays: Set<Int>, // Calendar.SUNDAY..Calendar.SATURDAY (1..7)
        nowMillis: Long = System.currentTimeMillis()
    ): Long {
        val candidate = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (weekdays.isEmpty()) {
            if (candidate.timeInMillis <= nowMillis) {
                candidate.add(Calendar.DAY_OF_YEAR, 1)
            }
            return candidate.timeInMillis
        }
        for (offset in 0..7) {
            val check = (candidate.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, offset)
            }
            val dow = check.get(Calendar.DAY_OF_WEEK)
            if (dow in weekdays && check.timeInMillis > nowMillis) {
                return check.timeInMillis
            }
        }
        candidate.add(Calendar.DAY_OF_YEAR, 1)
        return candidate.timeInMillis
    }

    fun formatCountdown(targetMillis: Long, nowMillis: Long = System.currentTimeMillis()): String {
        val diff = (targetMillis - nowMillis).coerceAtLeast(0L)
        val totalMinutes = diff / 60_000L
        val days = totalMinutes / (60 * 24)
        val hours = (totalMinutes % (60 * 24)) / 60
        val mins = totalMinutes % 60
        return when {
            days > 0 -> "in ${days}d ${hours}h ${mins}m"
            hours > 0 -> "in ${hours}h ${mins}m"
            mins > 0 -> "in ${mins}m"
            else -> "in < 1m"
        }
    }

    fun computeNextRecurringReminderMillis(
        currentTriggerMillis: Long,
        repeatInterval: String,
        customRepeatDays: Int
    ): Long? {
        if (repeatInterval == "NONE") return null
        val cal = Calendar.getInstance().apply { timeInMillis = currentTriggerMillis }
        val now = System.currentTimeMillis()
        var guard = 0
        do {
            when (repeatInterval) {
                "DAILY" -> cal.add(Calendar.DAY_OF_YEAR, 1)
                "WEEKLY" -> cal.add(Calendar.WEEK_OF_YEAR, 1)
                "MONTHLY" -> cal.add(Calendar.MONTH, 1)
                "YEARLY" -> cal.add(Calendar.YEAR, 1)
                "CUSTOM" -> cal.add(Calendar.DAY_OF_YEAR, customRepeatDays.coerceAtLeast(1))
                else -> return null
            }
            guard++
        } while (cal.timeInMillis <= now && guard < 1000)
        return cal.timeInMillis
    }

    fun weekdayShortName(calendarDow: Int): String = when (calendarDow) {
        Calendar.SUNDAY -> "Sun"
        Calendar.MONDAY -> "Mon"
        Calendar.TUESDAY -> "Tue"
        Calendar.WEDNESDAY -> "Wed"
        Calendar.THURSDAY -> "Thu"
        Calendar.FRIDAY -> "Fri"
        Calendar.SATURDAY -> "Sat"
        else -> ""
    }

    fun formatMinutesOfDay(minutesOfDay: Int, use24Hour: Boolean): String {
        val clamped = minutesOfDay.coerceIn(0, 1439)
        return formatHourMinute(clamped / 60, clamped % 60, use24Hour)
    }
}
