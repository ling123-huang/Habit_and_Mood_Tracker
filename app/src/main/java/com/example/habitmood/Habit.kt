package com.example.habitmood

data class Habit(
    val id: String = "",             // Firestore 문서 ID
    val name: String = "",           // 습관 이름
    val isAlarmOn: Boolean = false,  // 알람 사용 여부
    val alarmHour: Int? = null,      // 알람 시 (24시간제, null이면 알람 없음)
    val alarmMinute: Int? = null,    // 알람 분
    val selectedDays: List<String> = emptyList(),  // 선택된 요일들 ("Everyday", "Mon" 등)
    val createdDate: String = ""
)


