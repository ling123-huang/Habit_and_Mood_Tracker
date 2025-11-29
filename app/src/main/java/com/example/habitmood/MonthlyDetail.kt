package com.example.habitmood

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class MonthlyDetail : AppCompatActivity() {

    private lateinit var calendarRecyclerView: RecyclerView
    private lateinit var tvMonthYear: TextView
    private lateinit var btnPrevMonth: ImageButton
    private lateinit var btnNextMonth: ImageButton
    private lateinit var btnBack: ImageButton
    private lateinit var tvHabitTitle: TextView
    private lateinit var btnComplete: Button

    private val calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("MMMM yyyy", Locale.ENGLISH)

    // 임시: 체크된 날짜들 (백엔드에서 가져올 데이터)
    private val checkedDates = mutableSetOf(
        "2025-11-04",
        "2025-11-05",
        "2025-11-10",
        "2025-11-15"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_monthly_detail)

        Log.d("MonthlyDetail", "onCreate started")

        // View 초기화
        calendarRecyclerView = findViewById(R.id.calendarRecyclerView)
        tvMonthYear = findViewById(R.id.tvMonthYear)
        btnPrevMonth = findViewById(R.id.btnPrevMonth)
        btnNextMonth = findViewById(R.id.btnNextMonth)
        btnBack = findViewById(R.id.btnBack)
        tvHabitTitle = findViewById(R.id.tvHabitTitle)
        btnComplete = findViewById(R.id.btnComplete)

        Log.d("MonthlyDetail", "Views initialized")

        // RecyclerView 설정 (7열 그리드)
        calendarRecyclerView.layoutManager = GridLayoutManager(this, 7)

        // Intent에서 습관 이름 받기
        val habitName = intent.getStringExtra("HABIT_NAME") ?: "Habit"
        tvHabitTitle.text = habitName

        Log.d("MonthlyDetail", "Habit name: $habitName")

        // 달력 표시
        updateCalendar()

        // 이전 달 버튼
        btnPrevMonth.setOnClickListener {
            calendar.add(Calendar.MONTH, -1)
            updateCalendar()
        }

        // 다음 달 버튼
        btnNextMonth.setOnClickListener {
            calendar.add(Calendar.MONTH, 1)
            updateCalendar()
        }

        // 뒤로가기 버튼
        btnBack.setOnClickListener {
            finish()
        }

        // Complete 버튼
        btnComplete.setOnClickListener {
            // 오늘 날짜 체크 추가
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(Date())
            if (checkedDates.contains(today)) {
                checkedDates.remove(today)
            } else {
                checkedDates.add(today)
            }
            updateCalendar()
        }
    }

    private fun updateCalendar() {
        // 월/년 표시 업데이트
        tvMonthYear.text = dateFormat.format(calendar.time)

        // 달력 날짜 생성
        val days = generateCalendarDays()

        Log.d("MonthlyDetail", "Generated ${days.size} days")
        Log.d("MonthlyDetail", "First day: ${days.firstOrNull()}")

        // 어댑터 설정
        val adapter = CalendarDayAdapter(days) { day ->
            Log.d("MonthlyDetail", "Day clicked: ${day.date}")
            // 날짜 클릭 시 체크 토글
            val dateKey = day.date
            if (checkedDates.contains(dateKey)) {
                checkedDates.remove(dateKey)
            } else {
                checkedDates.add(dateKey)
            }
            updateCalendar() // 달력 새로고침
        }
        calendarRecyclerView.adapter = adapter

        Log.d("MonthlyDetail", "Adapter set with ${adapter.itemCount} items")
    }

    private fun generateCalendarDays(): List<CalendarDay> {
        val days = mutableListOf<CalendarDay>()

        // 현재 달의 1일로 설정
        val tempCalendar = calendar.clone() as Calendar
        tempCalendar.set(Calendar.DAY_OF_MONTH, 1)

        val year = tempCalendar.get(Calendar.YEAR)
        val month = tempCalendar.get(Calendar.MONTH)

        // 이번 달의 첫 날 요일 (일요일=1, 월요일=2, ...)
        val firstDayOfWeek = tempCalendar.get(Calendar.DAY_OF_WEEK)

        // 이번 달의 마지막 날
        val maxDayOfMonth = tempCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        Log.d("MonthlyDetail", "Year: $year, Month: $month, FirstDay: $firstDayOfWeek, MaxDay: $maxDayOfMonth")

        // 이전 달의 빈 칸 추가
        for (i in 1 until firstDayOfWeek) {
            days.add(CalendarDay(0, false, false, ""))
        }

        // 이번 달의 날짜 추가
        val dateFormatKey = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
        for (day in 1..maxDayOfMonth) {
            tempCalendar.set(Calendar.DAY_OF_MONTH, day)
            val dateKey = dateFormatKey.format(tempCalendar.time)
            val isChecked = checkedDates.contains(dateKey)

            days.add(CalendarDay(day, true, isChecked, dateKey))
        }

        // 다음 달의 빈 칸 추가 (6주 채우기)
        val remainingDays = 42 - days.size // 6주 * 7일 = 42칸
        for (i in 1..remainingDays) {
            days.add(CalendarDay(0, false, false, ""))
        }

        return days
    }
}