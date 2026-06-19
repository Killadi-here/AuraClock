package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "alarms")
data class Alarm(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val hour: Int,
    val minute: Int,
    val label: String = "Alarm",
    val isEnabled: Boolean = true,
    val daysOfWeek: String = "", // Comma-separated indices "1,2,3..." 1=Mon, 7=Sun. Empty means one-time.
    val isVibrate: Boolean = true,
    val soundOption: String = "Default"
) : Serializable {
    val formattedTime: String
        get() {
            val displayHour = if (hour == 0 || hour == 12) 12 else hour % 12
            val amPm = if (hour >= 12) "PM" else "AM"
            return String.format("%02d:%02d %s", displayHour, minute, amPm)
        }

    val daysText: String
        get() {
            if (daysOfWeek.isEmpty()) return "Once"
            val parts = daysOfWeek.split(",").mapNotNull { it.toIntOrNull() }
            if (parts.size == 7) return "Every day"
            if (parts.size == 5 && !parts.contains(6) && !parts.contains(7)) return "Weekdays"
            if (parts.size == 2 && parts.contains(6) && parts.contains(7)) return "Weekends"
            
            val daysMap = mapOf(
                1 to "Mon", 2 to "Tue", 3 to "Wed", 4 to "Thu",
                5 to "Fri", 6 to "Sat", 7 to "Sun"
            )
            return parts.sorted().map { daysMap[it] ?: "" }.filter { it.isNotEmpty() }.joinToString(", ")
        }
}
