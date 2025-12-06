package com.example.habitmood

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class MonthlyDetail : AppCompatActivity() {

    private lateinit var calendarRecyclerView: RecyclerView
    private lateinit var tvMonthYear: TextView
    private lateinit var tvMonthPercentage: TextView
    private lateinit var tvMonthCount: TextView
    private lateinit var tvTotalCount: TextView
    private lateinit var btnPrevMonth: ImageButton
    private lateinit var btnNextMonth: ImageButton
    private lateinit var btnBack: ImageButton
    private lateinit var tvHabitTitle: TextView
    private lateinit var btnComplete: Button

    private val calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("MMMM yyyy", Locale.ENGLISH)

    // 체크된 날짜들 (Firebase에서 가져올 데이터)
    private val checkedDates = mutableSetOf<String>()
    private var selectedDate: String = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(Date())

    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private var habitId: String? = null
    private var habitCreatedDate: String? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_monthly_detail)

        Log.d("MonthlyDetail", "onCreate started")

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // View 초기화
        initViews()

        //Log.d("MonthlyDetail", "Views initialized")

        // RecyclerView 설정 (7열 그리드)
        calendarRecyclerView.layoutManager = GridLayoutManager(this, 7)

        // Intent에서 습관 이름 & ID 받기
        val habitName = intent.getStringExtra("HABIT_NAME") ?: "Habit"
        habitId = intent.getStringExtra("HABIT_ID")
        habitCreatedDate = intent.getStringExtra("HABIT_CREATED_DATE")   // 新增
        tvHabitTitle.text = habitName


        //Log.d("MonthlyDetail", "Habit name: $habitName")
        //오늘 날짜로 초기화
        updateCompleteButtonText()
        // 현재 달의 체크 데이터 로드 + 달력 표시
        loadCheckedDatesForCurrentMonth()
        setupListeners()
    }

    private fun initViews() {
        calendarRecyclerView = findViewById(R.id.calendarRecyclerView)
        tvMonthYear = findViewById(R.id.tvMonthYear)
        tvMonthPercentage = findViewById(R.id.tvMonthPercentage)
        tvMonthCount = findViewById(R.id.tvMonthCount)
        tvTotalCount = findViewById(R.id.tvTotalCount)
        btnPrevMonth = findViewById(R.id.btnPrevMonth)
        btnNextMonth = findViewById(R.id.btnNextMonth)
        btnBack = findViewById(R.id.btnBack)
        tvHabitTitle = findViewById(R.id.tvHabitTitle)
        btnComplete = findViewById(R.id.btnComplete)
    }

    private fun setupListeners() {
        // 이전 달 버튼
        btnPrevMonth.setOnClickListener {
            calendar.add(Calendar.MONTH, -1)
            loadCheckedDatesForCurrentMonth()
        }

        // 다음 달 버튼
        btnNextMonth.setOnClickListener {
            calendar.add(Calendar.MONTH, 1)
            loadCheckedDatesForCurrentMonth()
        }

        // 뒤로가기 버튼
        btnBack.setOnClickListener {
            finish()
        }

        // Complete 버튼: 오늘 날짜 토글
        btnComplete.setOnClickListener {
            toggleCheckForDate(selectedDate)
        }
    }

    //사용자가 뭘 선택했는지 불러오기
    private fun updateCompleteButtonText() {
        btnComplete.text = "Check/Uncheck ($selectedDate)"
    }

    private fun loadCheckedDatesForCurrentMonth() {
        val user = auth.currentUser
        val localHabitId = habitId

        // 우선 화면의 월/년 표시 및 기존 체크 상태 초기화
        checkedDates.clear()
        updateCalendar()

        if (user == null || localHabitId == null) {
            // 로그인 안 되어 있으면 로컬 표시만
            updateStatistics()
            return
        }

        // 이번 달의 시작/끝 날짜 문자열 계산
        val tempStart = calendar.clone() as Calendar
        tempStart.set(Calendar.DAY_OF_MONTH, 1)

        val tempEnd = calendar.clone() as Calendar
        tempEnd.set(Calendar.DAY_OF_MONTH, tempEnd.getActualMaximum(Calendar.DAY_OF_MONTH))

        val keyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
        val startKey = keyFormat.format(tempStart.time)
        val endKey = keyFormat.format(tempEnd.time)

        // 현재 월의 체크인 데이터만 가져오기
        db.collection("users")
            .document(user.uid)
            .collection("habits")
            .document(localHabitId)
            .collection("checkins")
            .whereGreaterThanOrEqualTo("date", startKey)
            .whereLessThanOrEqualTo("date", endKey)
            .get()
            .addOnSuccessListener { snapshot ->
                for (doc in snapshot) {
                    val dateStr = doc.getString("date") ?: doc.id
                    checkedDates.add(dateStr)
                }
                updateCalendar()
                updateStatistics()
            }

        // 전체 누적 횟수를 위한 별도 쿼리
        loadTotalCheckCount()
    }

    private fun loadTotalCheckCount() {
        val user = auth.currentUser
        val localHabitId = habitId

        if (user == null || localHabitId == null) {
            return
        }

        db.collection("users")
            .document(user.uid)
            .collection("habits")
            .document(localHabitId)
            .collection("checkins")
            .get()
            .addOnSuccessListener { snapshot ->
                val totalCount = snapshot.size()
                tvTotalCount.text = "${totalCount}회"
            }
    }

    private fun updateStatistics() {
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        var targetDays = daysInMonth
        val createdStr = habitCreatedDate
        var createdCal: Calendar? = null

        if (!createdStr.isNullOrEmpty()) {
            try {
                val sdfCreated = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault())
                val createdDate = sdfCreated.parse(createdStr)
                if (createdDate != null) {
                    createdCal = Calendar.getInstance().apply { time = createdDate }

                    val createdYear = createdCal.get(Calendar.YEAR)
                    val createdMonth = createdCal.get(Calendar.MONTH)
                    val createdDay = createdCal.get(Calendar.DAY_OF_MONTH)

                    val currentYear = calendar.get(Calendar.YEAR)
                    val currentMonth = calendar.get(Calendar.MONTH)

                    targetDays = when {
                        currentYear < createdYear ||
                                (currentYear == createdYear && currentMonth < createdMonth) -> 0

                        currentYear == createdYear && currentMonth == createdMonth -> {
                            daysInMonth - (createdDay - 1)
                        }

                        else -> daysInMonth
                    }
                }
            } catch (_: Exception) {
            }
        }

        val sdfKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH)
        var monthCheckCount = 0

        for (dateStr in checkedDates) {
            try {
                val date = sdfKey.parse(dateStr) ?: continue
                val c = Calendar.getInstance().apply { time = date }
                val y = c.get(Calendar.YEAR)
                val m = c.get(Calendar.MONTH)
                val d = c.get(Calendar.DAY_OF_MONTH)

                if (y != currentYear || m != currentMonth) continue

                if (createdCal != null &&
                    y == createdCal.get(Calendar.YEAR) &&
                    m == createdCal.get(Calendar.MONTH) &&
                    d < createdCal.get(Calendar.DAY_OF_MONTH)
                ) {
                    continue
                }

                monthCheckCount++
            } catch (_: Exception) {
            }
        }

        val percentage = if (targetDays > 0) {
            val raw = monthCheckCount.toFloat() / targetDays.toFloat() * 100f
            raw.coerceIn(0f, 100f).toInt()
        } else {
            0
        }

        tvMonthPercentage.text = "$percentage%"
        tvMonthCount.text = "${monthCheckCount}회"

        Log.d(
            "MonthlyDetail",
            "Stats - MonthCheckCount=$monthCheckCount, TargetDays=$targetDays, Percentage=$percentage%"
        )
    }

    // 날짜 하나를 Firebase에 토글 저장하는 함수
    private fun toggleCheckForDate(dateKey: String) {
        val user = auth.currentUser
        val localHabitId = habitId

        // 로그인/ID 없으면 로컬 세트만 토글
        if (user == null || localHabitId == null) {
            if (checkedDates.contains(dateKey)) {
                checkedDates.remove(dateKey)
            } else {
                checkedDates.add(dateKey)
            }
            updateCalendar()
            updateStatistics()
            return
        }

        val docRef = db.collection("users")
            .document(user.uid)
            .collection("habits")
            .document(localHabitId)
            .collection("checkins")
            .document(dateKey)

        if (checkedDates.contains(dateKey)) {
            // 이미 체크되어 있으면 삭제
            docRef.delete()
                .addOnSuccessListener {
                    checkedDates.remove(dateKey)
                    updateCalendar()
                    updateStatistics()
                    loadTotalCheckCount() // 총 누적도 업데이트
                }
        } else {
            // 없으면 새로 생성
            val data = hashMapOf(
                "date" to dateKey,
                "timestamp" to FieldValue.serverTimestamp()
            )
            docRef.set(data)
                .addOnSuccessListener {
                    checkedDates.add(dateKey)
                    updateCalendar()
                    updateStatistics()
                    loadTotalCheckCount() // 총 누적도 업데이트
                }
        }
    }

    // MonthlyDetail.kt
    // 달력 전체를 다시 그리는 함수
    private fun updateCalendar() {
        // 월/년 표시 업데이트
        tvMonthYear.text = dateFormat.format(calendar.time)

        // 달력 날짜 생성
        val days = generateCalendarDays()

        Log.d("MonthlyDetail", "Generated ${days.size} days")
        Log.d("MonthlyDetail", "First day: ${days.firstOrNull()}")

        // 어댑터 설정
        val adapter = CalendarDayAdapter(days, selectedDate) { day ->
            // [수정] isAvailable 조건 추가: 미래 날짜나 생성일 이전 날짜는 선택 불가
            if (day.date.isNotBlank() && day.isAvailable) {
                selectedDate = day.date
                updateCompleteButtonText() // [추가] 선택된 날짜로 버튼 텍스트 업데이트
                updateCalendar()
            }
        }
        calendarRecyclerView.adapter = adapter

        Log.d("MonthlyDetail", "Adapter set with ${adapter.itemCount} items")
    }

    // MonthlyDetail.kt
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

        // 오늘 날짜 준비
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // 습관 생성일 준비
        var createdCal: Calendar? = null
        val createdStr = habitCreatedDate // "yyyy.MM.dd" format
        if (!createdStr.isNullOrEmpty()) {
            try {
                val sdfCreated = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault())
                val createdDate = sdfCreated.parse(createdStr)
                if (createdDate != null) {
                    createdCal = Calendar.getInstance().apply {
                        time = createdDate
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                }
            } catch (_: Exception) {
            }
        }

        Log.d(
            "MonthlyDetail",
            "Year: $year, Month: $month, FirstDay: $firstDayOfWeek, MaxDay: $maxDayOfMonth"
        )

        // 이전 달의 빈 칸 추가
        for (i in 1 until firstDayOfWeek) {
            days.add(CalendarDay(0, false, false, null, "", false, false))
        }

        // 이번 달의 날짜 추가
        val dateFormatKey = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
        for (day in 1..maxDayOfMonth) {
            tempCalendar.set(Calendar.DAY_OF_MONTH, day)

            // 현재 날짜를 자정 기준으로 정규화
            val currentDayStart = tempCalendar.clone() as Calendar
            currentDayStart.set(Calendar.HOUR_OF_DAY, 0)
            currentDayStart.set(Calendar.MINUTE, 0)
            currentDayStart.set(Calendar.SECOND, 0)
            currentDayStart.set(Calendar.MILLISECOND, 0)

            // 미래 날짜 확인 (오늘 자정 이후)
            val isFuture = currentDayStart.after(today)

            // 생성일 이전 날짜 확인
            var isBeforeCreation = false
            if (createdCal != null) {
                isBeforeCreation = currentDayStart.before(createdCal)
            }

            // 이용 가능 여부: 생성일 이후 && 오늘 이전/오늘
            val isAvailable = !isFuture && !isBeforeCreation

            val dateKey = dateFormatKey.format(tempCalendar.time)
            val isChecked = checkedDates.contains(dateKey)

            // 생성일 이전 날짜도 표시하되 연한 회색으로
            days.add(
                CalendarDay(
                    dayNumber = day,
                    isCurrentMonth = true,
                    isChecked = isChecked,
                    moodEmoji = null,
                    date = dateKey,
                    isFutureDate = isFuture,
                    isAvailable = isAvailable
                )
            )
        }

        // 다음 달의 빈 칸 추가 (6주 채우기)
        val remainingDays = 42 - days.size // 6주 * 7일 = 42칸
        for (i in 1..remainingDays) {
            days.add(CalendarDay(0, false, false, null, "", false, false))
        }

        return days
    }
}