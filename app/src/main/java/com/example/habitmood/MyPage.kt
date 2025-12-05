package com.example.habitmood

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class MyPage : AppCompatActivity() {

    private lateinit var tvUserName: TextView
    private lateinit var tvUserEmail: TextView
    private lateinit var tvMemberDays: TextView
    private lateinit var tvBestHabitName: TextView
    private lateinit var tvBestHabitRate: TextView
    private lateinit var tvReminderTime: TextView
    private lateinit var tvThemeValue: TextView
    private lateinit var switchDailyReminder: SwitchMaterial
    private lateinit var btnChangeTime: MaterialButton
    private lateinit var btnLogout: MaterialButton
    private lateinit var layoutTheme: View
    private lateinit var bottomNavigationView: BottomNavigationView

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var prefs: SharedPreferences

    private var reminderHour = 21 // 기본 9:00 PM
    private var reminderMinute = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_page)

        // Firebase 초기화
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        prefs = getSharedPreferences("MyPagePrefs", Context.MODE_PRIVATE)

        initViews()
        loadSettings()
        loadUserData()
        loadMemberDays()
        loadBestHabit()
        setupListeners()
        setupBottomNavigation()
    }

    private fun initViews() {
        tvUserName = findViewById(R.id.tvUserName)
        tvUserEmail = findViewById(R.id.tvUserEmail)
        tvMemberDays = findViewById(R.id.tvMemberDays)
        tvBestHabitName = findViewById(R.id.tvBestHabitName)
        tvBestHabitRate = findViewById(R.id.tvBestHabitRate)
        tvReminderTime = findViewById(R.id.tvReminderTime)
        switchDailyReminder = findViewById(R.id.switchDailyReminder)
        btnChangeTime = findViewById(R.id.btnChangeTime)
        btnLogout = findViewById(R.id.btnLogout)
        //layoutTheme = findViewById(R.id.layoutTheme)
        bottomNavigationView = findViewById(R.id.bottomAppBar)
    }

    private fun setupListeners() {
        // 알림 스위치
        switchDailyReminder.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("reminder_enabled", isChecked).apply()
            if (isChecked) {
                scheduleDailyReminder()
                Toast.makeText(this, "Daily reminder enabled", Toast.LENGTH_SHORT).show()
            } else {
                cancelDailyReminder()
                Toast.makeText(this, "Daily reminder disabled", Toast.LENGTH_SHORT).show()
            }
        }

        // 시간 변경 버튼
        btnChangeTime.setOnClickListener {
            showTimePickerDialog()
        }

        // 로그아웃 버튼
        btnLogout.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout") { _, _ ->
                    auth.signOut()
                    Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show()
                    // 로그인 화면으로 이동
                    val intent = Intent(this, Login::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun loadSettings() {
        // 저장된 알림 시간 불러오기
        reminderHour = prefs.getInt("reminder_hour", 21)
        reminderMinute = prefs.getInt("reminder_minute", 0)

        val amPm = if (reminderHour < 12) "AM" else "PM"
        val hour12 = if (reminderHour > 12) reminderHour - 12
        else if (reminderHour == 0) 12
        else reminderHour
        tvReminderTime.text = String.format("%d:%02d %s", hour12, reminderMinute, amPm)

        // 알림 활성화 상태
        val isReminderEnabled = prefs.getBoolean("reminder_enabled", false)
        switchDailyReminder.isChecked = isReminderEnabled

        // 테마
        val theme = prefs.getString("theme", "Light") ?: "Light"
        tvThemeValue.text = theme
    }

    private fun showTimePickerDialog() {
        val timeDialog = TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                reminderHour = hourOfDay
                reminderMinute = minute

                // 저장
                prefs.edit()
                    .putInt("reminder_hour", reminderHour)
                    .putInt("reminder_minute", reminderMinute)
                    .apply()

                // UI 업데이트
                val amPm = if (hourOfDay < 12) "AM" else "PM"
                val hour12 = if (hourOfDay > 12) hourOfDay - 12
                else if (hourOfDay == 0) 12
                else hourOfDay
                val timeString = String.format("%d:%02d %s", hour12, minute, amPm)
                tvReminderTime.text = timeString

                // 알림 재설정
                if (switchDailyReminder.isChecked) {
                    scheduleDailyReminder()
                    Toast.makeText(this, "Reminder time updated", Toast.LENGTH_SHORT).show()
                }
            },
            reminderHour,
            reminderMinute,
            false
        )
        timeDialog.show()
    }


    private fun scheduleDailyReminder() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(this, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            9999, // 고유 ID
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, reminderHour)
            set(Calendar.MINUTE, reminderMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            // 오늘 시간이 지났으면 내일로
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }

        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }

    private fun cancelDailyReminder() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            9999,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    private fun setupBottomNavigation() {
        bottomNavigationView.selectedItemId = R.id.menu_profile

        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.menu_stats -> {
                    val intent = Intent(this, MoodStatistics::class.java)
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.home_page -> {
                    val intent = Intent(this, Home::class.java)
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                R.id.menu_profile -> true
                else -> false
            }
        }
    }

    private fun loadUserData() {
        val user = auth.currentUser
        if (user != null) {
            tvUserName.text = user.displayName ?: "User"
            tvUserEmail.text = user.email ?: "user@example.com"
        } else {
            tvUserName.text = "Guest"
            tvUserEmail.text = "Not logged in"
        }
    }

    private fun loadMemberDays() {
        val user = auth.currentUser ?: return

        db.collection("users")
            .document(user.uid)
            .get()
            .addOnSuccessListener { document ->
                val createdAt = document.getTimestamp("createdAt")
                if (createdAt != null) {
                    val days = TimeUnit.MILLISECONDS.toDays(
                        System.currentTimeMillis() - createdAt.toDate().time
                    )
                    tvMemberDays.text = "$days days"
                } else {
                    tvMemberDays.text = "New member"
                }
            }
            .addOnFailureListener {
                tvMemberDays.text = "-"
            }
    }

    private fun loadBestHabit() {
        val user = auth.currentUser ?: return

        val calendar = Calendar.getInstance()
        val currentMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(calendar.time)

        db.collection("users")
            .document(user.uid)
            .collection("habits")
            .get()
            .addOnSuccessListener { habitsSnapshot ->
                var bestHabit: String? = null
                var bestRate = 0

                for (habitDoc in habitsSnapshot) {
                    val habitName = habitDoc.getString("name") ?: continue
                    val habitId = habitDoc.id

                    // 이번 달 체크인 개수 확인 (비동기이므로 복잡함, 간단히 처리)
                    db.collection("users")
                        .document(user.uid)
                        .collection("habits")
                        .document(habitId)
                        .collection("checkins")
                        .get()
                        .addOnSuccessListener { checkinsSnapshot ->
                            val thisMonthCheckins = checkinsSnapshot.documents.count {
                                it.getString("date")?.startsWith(currentMonth) == true
                            }

                            val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
                            val rate = (thisMonthCheckins * 100) / daysInMonth

                            if (rate > bestRate) {
                                bestRate = rate
                                bestHabit = habitName
                                tvBestHabitName.text = bestHabit ?: "No habits"
                                tvBestHabitRate.text = "$bestRate% completion"
                            }
                        }
                }

                if (habitsSnapshot.isEmpty) {
                    tvBestHabitName.text = "No habits yet"
                    tvBestHabitRate.text = "Start tracking!"
                }
            }
    }

    override fun onResume() {
        super.onResume()
        bottomNavigationView.selectedItemId = R.id.menu_profile
    }
}