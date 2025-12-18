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
import android.app.AlarmManager
import java.util.Calendar

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val habitName = intent.getStringExtra("HABIT_NAME") ?: "Your habit"
        val habitId = intent.getStringExtra("HABIT_ID") ?: ""
        val habitMessage = intent.getStringExtra("HABIT_MESSAGE")
            ?: "It's time to do your habit! ✅"
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

        //Go to Home screen when notification is clicked
        val openIntent = if (habitId == "MY_PAGE_DAILY") {
            Intent(context, Home::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
        } else {
            Intent(context, MonthlyDetail::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("HABIT_ID", habitId)
                putExtra("HABIT_NAME", habitName)
                putExtra("HABIT_CREATED_DATE", intent.getStringExtra("HABIT_CREATED_DATE"))
            }
        }


        val contentPendingIntent = PendingIntent.getActivity(
            context,
            (habitId.ifBlank { "MY_PAGE_DAILY" }).hashCode(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val title = if (habitId == "MY_PAGE_DAILY") "Daily Reminder" else habitName

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(habitMessage)
            .setStyle(NotificationCompat.BigTextStyle().bigText(habitMessage))
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()


        manager.notify(habitId.hashCode(), notification)

        if (habitId == "MY_PAGE_DAILY") {
            rescheduleMyPageNextDay(context)
        }
    }
}
private fun rescheduleMyPageNextDay(context: Context) {
    val prefs = context.getSharedPreferences("MyPagePrefs", Context.MODE_PRIVATE)
    val enabled = prefs.getBoolean("reminder_enabled", false)
    if (!enabled) return

    val hour = prefs.getInt("reminder_hour", 21)
    val minute = prefs.getInt("reminder_minute", 0)

    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    val i = Intent(context, ReminderReceiver::class.java).apply {
        putExtra("HABIT_ID", "MY_PAGE_DAILY")
        putExtra("HABIT_NAME", "Daily Reminder")
        putExtra("HABIT_MESSAGE", "Time to check your habits today ✅")
    }

    val pi = PendingIntent.getBroadcast(
        context,
        "MY_PAGE_DAILY".hashCode(),
        i,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val cal = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    alarmManager.cancel(pi)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
    } else {
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
    }
}
