package com.example.habitmood

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
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
    private val recordedDates = mutableSetOf(
        "2025-11-04",
        "2025-11-05",
        "2025-11-10",
        "2025-11-15"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mood_statistics)

        Log.d("MoodStatistics", "onCreate started")

        initViews()
        setupBottomNavigation()

        // RecyclerView 설정 (7열 그리드)
        calendarRecyclerView.layoutManager = GridLayoutManager(this, 7)

        updateCalendar()
    }

    private fun initViews() {
        calendarRecyclerView = findViewById(R.id.calendarRecyclerView)

        tvCurrentMonth = findViewById(R.id.tvCurrentMonth)
        btnPrevMonth = findViewById(R.id.btnPrevMonth)
        btnNextMonth = findViewById(R.id.btnNextMonth)
        bottomNavigationView = findViewById(R.id.bottomAppBar)

        btnPrevMonth.setOnClickListener {
            calendar.add(Calendar.MONTH, -1)
            updateCalendar()
        }

        btnNextMonth.setOnClickListener {
            calendar.add(Calendar.MONTH, 1)
            updateCalendar()
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

    private fun updateCalendar() {
        // 월/년 표시 업데이트
        tvCurrentMonth.text = dateFormat.format(calendar.time)

        // 달력 날짜 데이터 생성
        val days = generateCalendarDays()

        Log.d("MoodStatistics", "Generated ${days.size} days")

        val adapter = CalendarDayAdapter(days) { day ->
            Log.d("MoodStatistics", "Day clicked: ${day.date}")
            // 날짜 클릭 시 해당 날짜의 기분 메모 띄우기
        }
        calendarRecyclerView.adapter = adapter
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