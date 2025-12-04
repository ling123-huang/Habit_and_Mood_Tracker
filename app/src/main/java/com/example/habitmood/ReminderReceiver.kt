package com.example.habitmood

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import java.util.*

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val habitName = intent.getStringExtra("HABIT_NAME") ?: "Your habit"
        val habitId = intent.getStringExtra("HABIT_ID") ?: ""
        val selectedDays =
            intent.getStringArrayListExtra("SELECTED_DAYS") ?: arrayListOf()

        if (selectedDays.isNotEmpty() && !isTodayInSelectedDays(selectedDays)) {
            return
        }

        val channelId = "habit_reminders"
        val manager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // 2. Notification Channel 생성 (Android 8.0 이상 필수)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Habit Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders to do your habits"
                enableLights(true)
                lightColor = Color.GREEN
            }
            manager.createNotificationChannel(channel)
        }

        // 3. 알림 클릭 시 Home 화면으로 이동
        val openIntent = Intent(context, Home::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            habitId.hashCode(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)              // 앱 기본 아이콘 사용
            .setContentTitle("Time for your habit")          // 알림 제목
            .setContentText(habitName)                       // 알림 내용
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        manager.notify(habitId.hashCode(), notification)
    }

    private fun isTodayInSelectedDays(selectedDays: List<String>): Boolean {
        val lowerList = selectedDays.map { it.trim().lowercase(Locale.getDefault()) }
        if (lowerList.isEmpty()) return true           // 아무 요일도 없으면 매일
        if (lowerList.any { it.contains("every") || it.contains("매일") || it.contains("everyday") }) {
            return true
        }

        val cal = Calendar.getInstance()
        val dow = cal.get(Calendar.DAY_OF_WEEK) // 1: Sun ~ 7: Sat
        val todayKeys = when (dow) {
            Calendar.SUNDAY -> listOf("sun", "sunday", "일")
            Calendar.MONDAY -> listOf("mon", "monday", "월")
            Calendar.TUESDAY -> listOf("tue", "tuesday", "화")
            Calendar.WEDNESDAY -> listOf("wed", "wednesday", "수")
            Calendar.THURSDAY -> listOf("thu", "thursday", "목")
            Calendar.FRIDAY -> listOf("fri", "friday", "금")
            Calendar.SATURDAY -> listOf("sat", "saturday", "토")
            else -> emptyList()
        }

        // selectedDays 안의 문자열이 오늘 요일 키워드를 하나라도 포함하면 true
        return lowerList.any { dayStr ->
            todayKeys.any { key -> dayStr.contains(key) }
        }
    }
}

