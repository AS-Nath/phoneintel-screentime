package com.phoneintel.app.util

import java.text.SimpleDateFormat
import java.util.*

object DateUtil {

    fun startOfDay(epochMs: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = epochMs
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    fun daysAgo(days: Int): Long = startOfDay(System.currentTimeMillis() - days * 86_400_000L)

    fun startOfYear(year: Int): Long {
        val cal = Calendar.getInstance().apply {
            set(year, Calendar.JANUARY, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    fun endOfYear(year: Int): Long {
        val cal = Calendar.getInstance().apply {
            set(year, Calendar.DECEMBER, 31, 23, 59, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return cal.timeInMillis
    }

    fun currentYear(): Int = Calendar.getInstance().get(Calendar.YEAR)

    fun formatDuration(ms: Long): String {
        val totalMinutes = ms / 60_000
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m"
            else -> "<1m"
        }
    }

    fun formatBytes(bytes: Long): String = when {
        bytes >= 1_073_741_824L -> String.format("%.1f GB", bytes / 1_073_741_824.0)
        bytes >= 1_048_576L -> String.format("%.1f MB", bytes / 1_048_576.0)
        bytes >= 1_024L -> String.format("%.1f KB", bytes / 1_024.0)
        else -> "$bytes B"
    }

    fun formatDate(epochMs: Long, pattern: String = "MMM d"): String =
        SimpleDateFormat(pattern, Locale.getDefault()).format(Date(epochMs))

    fun monthName(epochMs: Long): String =
        SimpleDateFormat("MMMM", Locale.getDefault()).format(Date(epochMs))

    fun isToday(epochMs: Long): Boolean = startOfDay(epochMs) == startOfDay()
}
