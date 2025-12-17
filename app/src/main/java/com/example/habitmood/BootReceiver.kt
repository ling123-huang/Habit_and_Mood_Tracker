package com.example.habitmood

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val ACTION_RETRY_RESTORE = "com.example.habitmood.ACTION_RETRY_RESTORE"
        private const val PREF_RETRY = "BootRetryPrefs"
        private const val KEY_RETRY_COUNT = "retry_count"
        private const val MAX_RETRY = 10
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != Intent.ACTION_BOOT_COMPLETED && action != ACTION_RETRY_RESTORE) return

        Log.d("BootReceiver", "onReceive action=$action")

        try { FirebaseApp.initializeApp(context) } catch (_: Exception) {}

        restoreMyPageReminder(context)

        restoreHabitRemindersWithRetry(context)
    }

    // MyPage Reminder
    private fun restoreMyPageReminder(context: Context) {
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
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_MONTH, 1)
        }

        alarmManager.cancel(pi)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
        }

        Log.d("BootReceiver", "MyPage reminder restored: $hour:$minute")
    }

    // Habit Reminders
    private fun restoreHabitRemindersWithRetry(context: Context) {
        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser

        if (user == null) {
            scheduleRetry(context)
            return
        }

        context.getSharedPreferences(PREF_RETRY, Context.MODE_PRIVATE)
            .edit().putInt(KEY_RETRY_COUNT, 0).apply()

        val db = FirebaseFirestore.getInstance()

        db.collection("users")
            .document(user.uid)
            .collection("habits")
            .whereEqualTo("isAlarmOn", true)
            .get()
            .addOnSuccessListener { snapshot ->
                for (doc in snapshot) {
                    val habitId = doc.id
                    val habitName = doc.getString("name") ?: "Habit"
                    val alarmHour = doc.getLong("alarmHour")?.toInt()
                    val alarmMinute = doc.getLong("alarmMinute")?.toInt()

                    val createdAtTs = doc.getTimestamp("createdAt")
                    val createdDate = createdAtTs?.let {
                        SimpleDateFormat("yyyy.MM.dd", Locale.getDefault()).format(it.toDate())
                    } ?: ""

                    if (alarmHour != null && alarmMinute != null) {
                        scheduleHabitRepeating(
                            context,
                            habitId,
                            habitName,
                            alarmHour,
                            alarmMinute,
                            createdDate
                        )
                    }
                }
                Log.d("BootReceiver", "Habit reminders restored from Firestore: ${snapshot.size()}")
            }
            .addOnFailureListener { e ->
                Log.e("BootReceiver", "Failed to restore habits; will retry", e)
                scheduleRetry(context)
            }
    }

    private fun scheduleRetry(context: Context) {
        val prefs = context.getSharedPreferences(PREF_RETRY, Context.MODE_PRIVATE)
        val count = prefs.getInt(KEY_RETRY_COUNT, 0)

        if (count >= MAX_RETRY) {
            Log.d("BootReceiver", "Retry reached MAX_RETRY=$MAX_RETRY, stop retrying")
            return
        }

        prefs.edit().putInt(KEY_RETRY_COUNT, count + 1).apply()

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val i = Intent(context, BootReceiver::class.java).apply {
            action = ACTION_RETRY_RESTORE
        }

        val pi = PendingIntent.getBroadcast(
            context,
            7788,
            i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerAt = System.currentTimeMillis() + 60_000L
        alarmManager.cancel(pi)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }

        Log.d("BootReceiver", "Scheduled retry #${count + 1} in 60s")
    }

    private fun scheduleHabitRepeating(
        context: Context,
        habitId: String,
        habitName: String,
        hour: Int,
        minute: Int,
        habitCreatedDate: String
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val i = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("HABIT_ID", habitId)
            putExtra("HABIT_NAME", habitName)
            putExtra("HABIT_MESSAGE", "It's time to do your habit! ✅")
            putExtra("HABIT_CREATED_DATE", habitCreatedDate)
        }

        val pi = PendingIntent.getBroadcast(
            context,
            habitId.hashCode(),
            i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_MONTH, 1)
        }

        alarmManager.cancel(pi)

        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            cal.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pi
        )
    }
}

