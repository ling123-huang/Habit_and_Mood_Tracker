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

    // UI 변수
    private lateinit var tvUserName: TextView
    private lateinit var tvUserEmail: TextView

    // Best Habit 관련
    private lateinit var tvBestHabitName: TextView
    private lateinit var tvBestHabitRate: TextView

    // Mood of the Month 관련
    private lateinit var tvTopMoodName: TextView
    private lateinit var tvTopMoodCount: TextView
    private lateinit var tvTopMoodEmoji: TextView

    // 설정 관련
    private lateinit var tvReminderTime: TextView
    private lateinit var switchDailyReminder: SwitchMaterial
    private lateinit var btnChangeTime: MaterialButton
    private lateinit var btnLogout: MaterialButton
    private lateinit var tvTotalHabits: TextView
    // 하단 네비게이션
    private lateinit var bottomNavigationView: BottomNavigationView

    // Firebase 및 저장소
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
        loadBestHabit()
        loadMoodOfTheMonth()
        setupListeners()
        setupBottomNavigation()
    }

    private fun initViews() {
        tvTotalHabits = findViewById(R.id.tvTotalHabits)
        // 프로필
        tvUserName = findViewById(R.id.tvUserName)
        tvUserEmail = findViewById(R.id.tvUserEmail)

        // Best Habit (XML ID와 일치시킴)
        tvBestHabitName = findViewById(R.id.tvBestHabitName)
        tvBestHabitRate = findViewById(R.id.tvBestHabitRate)

        // Mood of the Month
        tvTopMoodName = findViewById(R.id.tvTopMoodName)
        tvTopMoodCount = findViewById(R.id.tvTopMoodCount)
        tvTopMoodEmoji = findViewById(R.id.tvTopMoodEmoji)

        // 설정
        tvReminderTime = findViewById(R.id.tvReminderTime)
        switchDailyReminder = findViewById(R.id.switchDailyReminder)
        btnChangeTime = findViewById(R.id.btnChangeTime)
        btnLogout = findViewById(R.id.btnLogout)

        // 네비게이션
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

        updateTimeUI(reminderHour, reminderMinute)

        // 알림 활성화 상태
        val isReminderEnabled = prefs.getBoolean("reminder_enabled", false)
        switchDailyReminder.isChecked = isReminderEnabled
    }

    private fun updateTimeUI(hour: Int, minute: Int) {
        val amPm = if (hour < 12) "AM" else "PM"
        val hour12 = if (hour > 12) hour - 12
        else if (hour == 0) 12
        else hour
        tvReminderTime.text = String.format("%d:%02d %s", hour12, minute, amPm)
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
                updateTimeUI(hourOfDay, minute)

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
            9999,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, reminderHour)
            set(Calendar.MINUTE, reminderMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

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

    private fun loadBestHabit() {
        val user = auth.currentUser ?: return
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH)     // 0 = Jan
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        db.collection("users")
            .document(user.uid)
            .collection("habits")
            .get()
            .addOnSuccessListener { habitsSnapshot ->
                val totalCount = habitsSnapshot.size()
                tvTotalHabits.text = totalCount.toString()

                var bestHabit: String? = null
                var bestRate = 0
                var processedCount = 0
                val totalHabits = habitsSnapshot.size()

                if (totalHabits == 0) {
                    tvBestHabitName.text = "No habits yet"
                    tvBestHabitRate.text = "Start tracking!"
                    return@addOnSuccessListener
                }

                for (habitDoc in habitsSnapshot) {
                    val habitName = habitDoc.getString("name") ?: continue
                    val habitId = habitDoc.id

                    // ① 读取创建时间 createdAt
                    val createdTs = habitDoc.getTimestamp("createdAt")
                    val createdCal = createdTs?.let {
                        Calendar.getInstance().apply {
                            time = it.toDate()
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                    }

                    // ② 计算这个习惯在“当前月份”的有效天数 targetDays
                    var targetDays = daysInMonth

                    if (createdCal != null) {
                        val createdYear = createdCal.get(Calendar.YEAR)
                        val createdMonth = createdCal.get(Calendar.MONTH)
                        val createdDay = createdCal.get(Calendar.DAY_OF_MONTH)

                        targetDays = when {
                            // 当前月份在创建月份之前 → 还没开始，分母 0
                            currentYear < createdYear ||
                                    (currentYear == createdYear && currentMonth < createdMonth) -> 0

                            // 当前月份 = 创建月份 → 分母 = 从创建日到月底
                            currentYear == createdYear && currentMonth == createdMonth ->
                                daysInMonth - (createdDay - 1)

                            // 当前月份在创建月份之后 → 用整个月天数
                            else -> daysInMonth
                        }
                    }

                    val targetDaysFinal = targetDays  // 闭包里用的 final 变量

                    db.collection("users")
                        .document(user.uid)
                        .collection("habits")
                        .document(habitId)
                        .collection("checkins")
                        .get()
                        .addOnSuccessListener { checkinsSnapshot ->
                            // ③ 只统计本月的打卡次数
                            val currentMonthPrefix = String.format(
                                Locale.getDefault(),
                                "%04d-%02d",
                                currentYear,
                                currentMonth + 1 // Calendar.MONTH 是 0~11
                            )

                            val thisMonthCheckins = checkinsSnapshot.documents.count {
                                val d = it.getString("date") ?: it.id
                                d.startsWith(currentMonthPrefix)
                            }

                            // ④ 计算百分比：本月完成天数 / targetDays
                            val rate = if (targetDaysFinal > 0) {
                                (thisMonthCheckins * 100) / targetDaysFinal
                            } else {
                                0
                            }

                            if (rate >= bestRate) {
                                bestRate = rate
                                bestHabit = habitName
                            }

                            processedCount++
                            if (processedCount == totalHabits) {
                                if (bestHabit != null && bestRate > 0) {
                                    tvBestHabitName.text = bestHabit
                                    tvBestHabitRate.text = "$bestRate% completion"
                                } else {
                                    tvBestHabitName.text = bestHabit ?: "Keep going!"
                                    tvBestHabitRate.text = "0% completion"
                                }
                            }
                        }
                }
            }
    }

    private fun loadMoodOfTheMonth() {
        val user = auth.currentUser ?: return

        // 현재 년/월 구하기
        val currentCal = Calendar.getInstance()
        val yearNow = currentCal.get(Calendar.YEAR)
        val monthNow = currentCal.get(Calendar.MONTH)

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        db.collection("users")
            .document(user.uid)
            .collection("moods")
            .get()
            .addOnSuccessListener { snapshot ->
                // index 0:Bad(1) ~ 4:VeryGood(5)
                val counts = IntArray(5)

                for (doc in snapshot) {
                    val dateStr = doc.getString("date") ?: continue
                    val mood = doc.getLong("mood")?.toInt() ?: continue

                    if (mood !in 1..5) continue

                    val date = try {
                        sdf.parse(dateStr)
                    } catch (e: Exception) {
                        null
                    } ?: continue

                    val c = Calendar.getInstance().apply { time = date }

                    // 이번 달 데이터만 집계
                    if (c.get(Calendar.YEAR) == yearNow && c.get(Calendar.MONTH) == monthNow) {
                        counts[mood - 1]++
                    }
                }

                // 제일 많이 나온 기분 찾기
                var maxIndex = -1
                var maxCount = 0

                for (i in counts.indices) {
                    if (counts[i] > maxCount) {
                        maxCount = counts[i]
                        maxIndex = i
                    }
                }

                if (maxIndex != -1) {
                    // MoodStatistics와 동일한 이모지/이름
                    val emojiList = listOf("\uD83D\uDE2D", "😔", "😐", "😊", "😍")
                    val nameList = listOf("Bad", "Sad", "Neutral", "Good", "Very Good")

                    tvTopMoodName.text = nameList[maxIndex]
                    tvTopMoodCount.text = "$maxCount times"
                    tvTopMoodEmoji.text = emojiList[maxIndex]
                } else {
                    tvTopMoodName.text = "No Data"
                    tvTopMoodCount.text = "Track your mood!"
                    tvTopMoodEmoji.text = "🫥"
                }
            }
            .addOnFailureListener {
                tvTopMoodName.text = "Error"
                tvTopMoodCount.text = "-"
            }
    }

    override fun onResume() {
        super.onResume()
        bottomNavigationView.selectedItemId = R.id.menu_profile
    }
}