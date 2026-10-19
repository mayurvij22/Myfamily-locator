package com.familytracker.util

import java.util.Locale
import kotlin.math.roundToInt

object Format {

    /** "850 m" under 1 km, else "12.4 km". */
    fun distance(meters: Float): String =
        if (meters < 1000f) "${meters.roundToInt()} m"
        else String.format(Locale.US, "%.1f km", meters / 1000f)

    /** "3 h 10 min", "45 min", "2 h". */
    fun duration(ms: Long): String {
        val totalMin = (ms / 60_000L).coerceAtLeast(0)
        val h = totalMin / 60
        val m = totalMin % 60
        return when {
            h == 0L -> "$m min"
            m == 0L -> "$h h"
            else -> "$h h $m min"
        }
    }

    /** "Just now", "2 min ago", "3 h ago", "2 d ago". */
    fun ago(ms: Long): String {
        val min = (ms / 60_000L).coerceAtLeast(0)
        return when {
            min < 1 -> "Just now"
            min < 60 -> "$min min ago"
            min < 48 * 60 -> "${min / 60} h ago"
            else -> "${min / (24 * 60)} d ago"
        }
    }

    fun latLng(lat: Double, lng: Double): String = String.format(Locale.US, "%.4f, %.4f", lat, lng)

    fun initials(name: String): String {
        val parts = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        return when {
            parts.isEmpty() -> "?"
            parts.size == 1 -> parts[0].take(2).uppercase()
            else -> "${parts[0].first()}${parts[1].first()}".uppercase()
        }
    }
}
