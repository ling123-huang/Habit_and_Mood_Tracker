package com.example.habitmood

data class Habit(
    val id: String = "",             // Firestore document ID
    val name: String = "",           // habit name
    val isAlarmOn: Boolean = false,  // Alarm usage
    val alarmHour: Int? = null,      // Alarm time (24-hour format, no alarm if null)
    val alarmMinute: Int? = null,    // Alarm minute
    val selectedDays: List<String> = emptyList(),
    val createdDate: String = ""
)


