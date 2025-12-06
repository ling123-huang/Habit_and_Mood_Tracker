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

        val channelId = "habit_reminders"
        val manager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Notification Channel
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

        //알림 클릭 시 Home 화면으로 이동
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
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Time for your habit")
            .setContentText(habitName)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        manager.notify(habitId.hashCode(), notification)
    }
}

