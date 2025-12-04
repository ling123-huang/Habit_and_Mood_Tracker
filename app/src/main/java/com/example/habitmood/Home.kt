package com.example.habitmood

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.text.SimpleDateFormat
import java.util.*
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.os.Build

class Home: AppCompatActivity() {

    private lateinit var habitRecyclerView: RecyclerView
    private lateinit var fabAddHabit: FloatingActionButton
    private lateinit var habitAdapter: HabitAdapter
    private lateinit var tvDate: TextView
    private lateinit var tvUserName: TextView
    private lateinit var tvTotalCount: TextView
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore


    // 초기 습관 목록(일시적)
    private val habitList = mutableListOf<Habit>()

    companion object {
        private const val REQUEST_ADD_HABIT = 1001
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!granted) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }

        }

        habitRecyclerView = findViewById(R.id.habit_RecyclerView)
        fabAddHabit = findViewById(R.id.fabAddHabit)
        tvDate = findViewById(R.id.tvDate)
        tvTotalCount = findViewById(R.id.habit_count)
        tvUserName = findViewById(R.id.tvUserName)
        bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomAppBar)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        setCurrentDate()
        setUserName()

        habitAdapter = HabitAdapter(habitList) { position ->
            if (position !in habitList.indices) return@HabitAdapter

            val user = auth.currentUser ?: return@HabitAdapter
            val habit = habitList[position]

            db.collection("users")
                .document(user.uid)
                .collection("habits")
                .document(habit.id)
                .delete()
                .addOnSuccessListener {
                    habitList.removeAt(position)
                    habitAdapter.notifyItemRemoved(position)
                    updateTotalCount()
                }
        }
        habitRecyclerView.layoutManager = LinearLayoutManager(this)
        habitRecyclerView.adapter = habitAdapter

        fabAddHabit.setOnClickListener {
            // AddHabitActivity로 이동
            val intent = Intent(this, AddHabitActivity::class.java)
            startActivityForResult(intent, REQUEST_ADD_HABIT)
        }
        setupBottomNavigation()
        loadHabitsFromFirestore()

    }

    private fun setUserName() {
        val user = auth.currentUser
        val name = user?.displayName ?: "User"
        tvUserName.text = "Hello, $name 👋"
    }

    private fun setupBottomNavigation() {
        // 현재 페이지를 Home으로 설정
        bottomNavigationView.selectedItemId = R.id.home_page

        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.menu_stats -> {
                    val intent = Intent(this, MoodStatistics::class.java)
                    startActivity(intent)
                    true
                }
                R.id.home_page -> {
                    true
                }
                R.id.menu_profile -> {
                    // MyActivity로 이동
                    val intent = Intent(this, MyPage::class.java)
                    startActivity(intent)
                    overridePendingTransition(0, 0) // 애니메이션 없이 전환
                    true
                }
                else -> false
            }
        }
    }

    private fun setCurrentDate() {
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("EEEE, dd MMM", Locale.ENGLISH)
        val currentDate = dateFormat.format(calendar.time)
        tvDate.text = currentDate
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_ADD_HABIT && resultCode == Activity.RESULT_OK) {

            val newHabitName = data?.getStringExtra("NEW_HABIT_NAME") ?: return
            val user = auth.currentUser ?: return

            // 알람 관련 정보도 함께 가져오기
            val isAlarmOn = data.getBooleanExtra("IS_ALARM_ON", false)
            val alarmHour = if (isAlarmOn) data.getIntExtra("ALARM_HOUR", 9) else null
            val alarmMinute = if (isAlarmOn) data.getIntExtra("ALARM_MINUTE", 0) else null
            val selectedDays =
                data.getStringArrayListExtra("SELECTED_DAYS") ?: arrayListOf<String>()

            // Firestore에 저장할 데이터 맵
            val habitData = hashMapOf(
                "name" to newHabitName,
                "createdAt" to FieldValue.serverTimestamp(),
                "isAlarmOn" to isAlarmOn,
                "selectedDays" to selectedDays
            )



            // null 이면 필드 생략, 있으면 저장
            alarmHour?.let { habitData["alarmHour"] = it }
            alarmMinute?.let { habitData["alarmMinute"] = it }

            db.collection("users")
                .document(user.uid)
                .collection("habits")
                .add(habitData)
                .addOnSuccessListener {
                    loadHabitsFromFirestore()
                }
        }
    }
    private fun updateTotalCount() {
        tvTotalCount.text = "Total: ${habitList.size}"
    }
    private fun loadHabitsFromFirestore() {
        val user = auth.currentUser ?: return

        db.collection("users")
            .document(user.uid)
            .collection("habits")
            .orderBy("createdAt")
            .get()
            .addOnSuccessListener { snapshot ->
                habitList.clear()
                val dateFormat = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault())
                for (doc in snapshot) {
                    val name = doc.getString("name") ?: ""
                    val isAlarmOn = doc.getBoolean("isAlarmOn") ?: false
                    val alarmHour = doc.getLong("alarmHour")?.toInt()
                    val alarmMinute = doc.getLong("alarmMinute")?.toInt()
                    val selectedDays =
                        (doc.get("selectedDays") as? List<*>)?.filterIsInstance<String>()
                            ?: emptyList()
                    val createdAtTs = doc.getTimestamp("createdAt")
                    val createdDate = createdAtTs?.let { ts ->
                        dateFormat.format(ts.toDate())
                    } ?: ""

                    val habit = Habit(
                        id = doc.id,
                        name = name,
                        isAlarmOn = isAlarmOn,
                        alarmHour = alarmHour,
                        alarmMinute = alarmMinute,
                        selectedDays = selectedDays,
                        createdDate = createdDate

                    )
                    habitList.add(habit)
                    if (isAlarmOn && alarmHour != null && alarmMinute != null) {
                        scheduleHabitReminder(habit.id, habit.name, alarmHour, alarmMinute, selectedDays)
                    }

                }
                habitAdapter.notifyDataSetChanged()
                updateTotalCount()
            }
    }

    private fun scheduleHabitReminder(
        habitId: String,
        habitName: String,
        hour: Int,
        minute: Int,
        selectedDays: List<String>
    ) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(this, ReminderReceiver::class.java).apply {
            putExtra("HABIT_ID", habitId)
            putExtra("HABIT_NAME", habitName)
            putStringArrayListExtra("SELECTED_DAYS", ArrayList(selectedDays))
        }

        val pendingIntent = PendingIntent.getBroadcast(
            this,
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

        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            cal.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }

    override fun onResume() {
        super.onResume()
        loadHabitsFromFirestore()
    }

}

