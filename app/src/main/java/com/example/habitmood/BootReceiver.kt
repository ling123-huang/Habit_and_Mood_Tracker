package com.example.habitmood

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Locale


class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        Log.d("BootReceiver", "BOOT_COMPLETED received, rescheduling alarms")

        try {
            FirebaseApp.initializeApp(context)
        } catch (_: Exception) {
        }

        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser

        if (user == null) {
            Log.d("BootReceiver", "No logged-in user, skip rescheduling")
            return
        }

        val db = FirebaseFirestore.getInstance()

        db.collection("users")
            .document(user.uid)
            .collection("habits")
            .whereEqualTo("isAlarmOn", true)   // 只恢复开着提醒的习惯
            .get()
            .addOnSuccessListener { snapshot ->
                for (doc in snapshot) {
                    val habitId = doc.id
                    val habitName = doc.getString("name") ?: "Habit"

                    val alarmHour = doc.getLong("alarmHour")?.toInt()
                    val alarmMinute = doc.getLong("alarmMinute")?.toInt()

                    // ★ 读出 createdAt → 转成 "yyyy.MM.dd"，和 Home / MonthlyDetail 用的一样
                    val createdAtTs = doc.getTimestamp("createdAt")
                    val createdDate = createdAtTs?.let {
                        val df = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault())
                        df.format(it.toDate())
                    } ?: ""

                    if (alarmHour != null && alarmMinute != null) {
                        scheduleHabitReminder(
                            context,
                            habitId,
                            habitName,
                            alarmHour,
                            alarmMinute,
                            createdDate                 // ★ 传给 scheduleHabitReminder
                        )
                    }
                }

            }
            .addOnFailureListener { e ->
                Log.e("BootReceiver", "Failed to reload habits after boot", e)
            }
    }

    private fun scheduleHabitReminder(
        context: Context,
        habitId: String,
        habitName: String,
        hour: Int,
        minute: Int,
        habitCreatedDate: String
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("HABIT_ID", habitId)
            putExtra("HABIT_NAME", habitName)
            putExtra("HABIT_MESSAGE", "Time for \"$habitName\" 🙌")
            putExtra("HABIT_CREATED_DATE", habitCreatedDate)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            habitId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }

        alarmManager.cancel(pendingIntent)

        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            cal.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }
}

