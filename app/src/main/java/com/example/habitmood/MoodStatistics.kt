package com.example.habitmood

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class MoodStatistics : AppCompatActivity() {

    private lateinit var calendarRecyclerView: RecyclerView
    private lateinit var tvCurrentMonth: TextView // XML ID: tvCurrentMonth
    private lateinit var btnPrevMonth: ImageButton
    private lateinit var btnNextMonth: ImageButton
    private lateinit var bottomNavigationView: BottomNavigationView

    private val calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("MMMM yyyy", Locale.ENGLISH)

    // 임시: 기분이 기록된 날짜들 (나중에 DB에서 가져올 데이터)
    private val recordedDates = mutableSetOf<String>()

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mood_statistics)

        Log.d("MoodStatistics", "onCreate started")

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        initViews()
        setupBottomNavigation()

        // RecyclerView 설정 (7열 그리드)
        calendarRecyclerView.layoutManager = GridLayoutManager(this, 7)

        loadRecordedDatesForCurrentMonth()
    }

    private fun initViews() {
        calendarRecyclerView = findViewById(R.id.calendarRecyclerView)

        tvCurrentMonth = findViewById(R.id.tvCurrentMonth)
        btnPrevMonth = findViewById(R.id.btnPrevMonth)
        btnNextMonth = findViewById(R.id.btnNextMonth)
        bottomNavigationView = findViewById(R.id.bottomAppBar)

        btnPrevMonth.setOnClickListener {
            calendar.add(Calendar.MONTH, -1)
            loadRecordedDatesForCurrentMonth()
        }

        btnNextMonth.setOnClickListener {
            calendar.add(Calendar.MONTH, 1)
            loadRecordedDatesForCurrentMonth()
        }
    }

    private fun setupBottomNavigation() {
        bottomNavigationView.selectedItemId = R.id.menu_stats

        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.menu_stats -> true
                R.id.home_page -> {
                    val intent = Intent(this, Home::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.menu_profile -> {
                    val intent = Intent(this, MyPage::class.java)
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                    true
                }
                else -> false
            }
        }
    }

    private fun loadRecordedDatesForCurrentMonth() {
        val user = auth.currentUser

        recordedDates.clear()
        updateCalendar() // 먼저 화면만 갱신

        if (user == null) {
            return
        }

        val tempStart = calendar.clone() as Calendar
        tempStart.set(Calendar.DAY_OF_MONTH, 1)

        val tempEnd = calendar.clone() as Calendar
        tempEnd.set(Calendar.DAY_OF_MONTH, tempEnd.getActualMaximum(Calendar.DAY_OF_MONTH))

        val keyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
        val startKey = keyFormat.format(tempStart.time)
        val endKey = keyFormat.format(tempEnd.time)

        db.collection("users")
            .document(user.uid)
            .collection("moods")
            .whereGreaterThanOrEqualTo("date", startKey)
            .whereLessThanOrEqualTo("date", endKey)
            .get()
            .addOnSuccessListener { snapshot ->
                for (doc in snapshot) {
                    val dateStr = doc.getString("date") ?: doc.id
                    recordedDates.add(dateStr)
                }
                updateCalendar()
            }
    }

    // 전체 달력 갱신
    private fun updateCalendar() {
        tvCurrentMonth.text = dateFormat.format(calendar.time)

        val days = generateCalendarDays()
        Log.d("MoodStatistics", "Generated ${days.size} days")

        val adapter = CalendarDayAdapter(days) { day ->
            Log.d("MoodStatistics", "Day clicked: ${day.date}")
            if (day.date.isNotBlank()) {
                onDayClicked(day.date)
            }
        }
        calendarRecyclerView.adapter = adapter
    }

    // 날짜 클릭 시 기분 메모 입력/수정/삭제
    private fun onDayClicked(dateKey: String) {
        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show()
            return
        }

        val editText = EditText(this)
        editText.hint = "How was your mood today?"

        val builder = AlertDialog.Builder(this)
            .setTitle(dateKey)
            .setView(editText)

        val docRef = db.collection("users")
            .document(user.uid)
            .collection("moods")
            .document(dateKey)

        // 먼저 기존 데이터를 한 번 읽어서 editText에 채워 넣을 수도 있지만,
        // 간단히 하기 위해 여기서는 새로 입력/수정만 처리합니다.
        builder.setPositiveButton("Save") { _, _ ->
            val note = editText.text.toString()
            val data = hashMapOf(
                "date" to dateKey,
                "note" to note,
                "timestamp" to FieldValue.serverTimestamp()
            )
            docRef.set(data)
                .addOnSuccessListener {
                    recordedDates.add(dateKey)
                    updateCalendar()
                }
        }

        if (recordedDates.contains(dateKey)) {
            // 이미 기록이 있는 날이면 삭제 버튼도 보여줌
            builder.setNeutralButton("Delete") { _, _ ->
                docRef.delete()
                    .addOnSuccessListener {
                        recordedDates.remove(dateKey)
                        updateCalendar()
                    }
            }
        }

        builder.setNegativeButton("Cancel", null)
        builder.show()
    }

    private fun generateCalendarDays(): List<CalendarDay> {
        val days = mutableListOf<CalendarDay>()
        val tempCalendar = calendar.clone() as Calendar

        tempCalendar.set(Calendar.DAY_OF_MONTH, 1)

        val firstDayOfWeek = tempCalendar.get(Calendar.DAY_OF_WEEK) // 1(일) ~ 7(토)
        val maxDayOfMonth = tempCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        for (i in 1 until firstDayOfWeek) {
            days.add(CalendarDay(0, false, false, ""))
        }

        val dateFormatKey = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
        for (day in 1..maxDayOfMonth) {
            tempCalendar.set(Calendar.DAY_OF_MONTH, day)
            val dateKey = dateFormatKey.format(tempCalendar.time)
            val isRecorded = recordedDates.contains(dateKey)

            days.add(CalendarDay(day, true, isRecorded, dateKey))
        }

        val remainingDays = 42 - days.size
        for (i in 1..remainingDays) {
            days.add(CalendarDay(0, false, false, ""))
        }

        return days
    }
}