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
    private lateinit var circularProgress: CircularProgressView

    private val calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("MMMM yyyy", Locale.ENGLISH)

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

        initViews()

        // RecyclerView Setup (7-Column Grid)
        calendarRecyclerView.layoutManager = GridLayoutManager(this, 7)

        // Get habit name & ID from Intent
        val habitName = intent.getStringExtra("HABIT_NAME") ?: "Habit"
        habitId = intent.getStringExtra("HABIT_ID")
        habitCreatedDate = intent.getStringExtra("HABIT_CREATED_DATE")
        tvHabitTitle.text = habitName

        // Load current month's check data + display calendar
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
        circularProgress = findViewById(R.id.circularProgress)
    }

    private fun setupListeners() {
        btnPrevMonth.setOnClickListener {
            calendar.add(Calendar.MONTH, -1)
            loadCheckedDatesForCurrentMonth()
        }

        btnNextMonth.setOnClickListener {
            calendar.add(Calendar.MONTH, 1)
            loadCheckedDatesForCurrentMonth()
        }

        btnBack.setOnClickListener {
            finish()
        }

        btnComplete.setOnClickListener {
            toggleCheckForDate(selectedDate)
        }
    }

    private fun loadCheckedDatesForCurrentMonth() {
        val user = auth.currentUser
        val localHabitId = habitId

        // reset the month/year display on the screen and the existing check status
        checkedDates.clear()
        updateCalendar()

        if (user == null || localHabitId == null) {
            updateStatistics()
            return
        }

        // Calculating the start/end date strings of this month
        val tempStart = calendar.clone() as Calendar
        tempStart.set(Calendar.DAY_OF_MONTH, 1)

        val tempEnd = calendar.clone() as Calendar
        tempEnd.set(Calendar.DAY_OF_MONTH, tempEnd.getActualMaximum(Calendar.DAY_OF_MONTH))

        val keyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
        val startKey = keyFormat.format(tempStart.time)
        val endKey = keyFormat.format(tempEnd.time)

        //Retrieve check-in data for the current month only
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
                updateCompleteButtonText()
            }

        // Separate query for total cumulative count
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
                tvTotalCount.text = "${totalCount}days"
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

        //Calculate percentage and limit it to 0~100%
        val percentage = if (targetDays > 0) {
            val raw = monthCheckCount.toFloat() / targetDays.toFloat() * 100f
            raw.coerceIn(0f, 100f).toInt()
        } else {
            0
        }
        circularProgress.setProgress(percentage, animate = true)

        tvMonthPercentage.text = "$percentage%"
        tvMonthCount.text = "${monthCheckCount}days"

        Log.d(
            "MonthlyDetail",
            "Stats - MonthCheckCount=$monthCheckCount, TargetDays=$targetDays, Percentage=$percentage%"
        )
    }


    // A function that toggles and saves a date in Firebase
    private fun toggleCheckForDate(dateKey: String) {
        val user = auth.currentUser
        val localHabitId = habitId

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
            docRef.delete()
                .addOnSuccessListener {
                    checkedDates.remove(dateKey)
                    updateCalendar()
                    updateStatistics()
                    loadTotalCheckCount()
                    updateCompleteButtonText()
                }
        } else {
            val data = hashMapOf(
                "date" to dateKey,
                "timestamp" to FieldValue.serverTimestamp()
            )
            docRef.set(data)
                .addOnSuccessListener {
                    checkedDates.add(dateKey)
                    updateCalendar()
                    updateStatistics()
                    loadTotalCheckCount()
                    updateCompleteButtonText()
                }
        }
    }

    // redraws the entire calendar
    private fun updateCalendar() {
        tvMonthYear.text = dateFormat.format(calendar.time)

        val days = generateCalendarDays()

        Log.d("MonthlyDetail", "Generated ${days.size} days")
        Log.d("MonthlyDetail", "First day: ${days.firstOrNull()}")

        val adapter = CalendarDayAdapter(days, selectedDate) { day ->
            if (day.date.isNotBlank() && day.isAvailable) {
                selectedDate = day.date
                updateCompleteButtonText()
                updateCalendar()
            }
        }
        calendarRecyclerView.adapter = adapter

        Log.d("MonthlyDetail", "Adapter set with ${adapter.itemCount} items")
    }

    private fun generateCalendarDays(): List<CalendarDay> {
        val days = mutableListOf<CalendarDay>()
        val tempCalendar = calendar.clone() as Calendar
        tempCalendar.set(Calendar.DAY_OF_MONTH, 1)

        val year = tempCalendar.get(Calendar.YEAR)
        val month = tempCalendar.get(Calendar.MONTH)
        val firstDayOfWeek = tempCalendar.get(Calendar.DAY_OF_WEEK)
        val maxDayOfMonth = tempCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        // Prepare today's date
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // Prepare habit creation date
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

        // Add blank from the previous month
        for (i in 1 until firstDayOfWeek) {
            days.add(CalendarDay(0, false, false, null, "", false, false))
        }

        // Add the date for this month
        val dateFormatKey = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
        for (day in 1..maxDayOfMonth) {
            tempCalendar.set(Calendar.DAY_OF_MONTH, day)

            // Normalize the current date to midnight
            val currentDayStart = tempCalendar.clone() as Calendar
            currentDayStart.set(Calendar.HOUR_OF_DAY, 0)
            currentDayStart.set(Calendar.MINUTE, 0)
            currentDayStart.set(Calendar.SECOND, 0)
            currentDayStart.set(Calendar.MILLISECOND, 0)

            // Check future date (after midnight today)
            val isFuture = currentDayStart.after(today)

            //Check the date before creation
            var isBeforeCreation = false
            if (createdCal != null) {
                isBeforeCreation = currentDayStart.before(createdCal)
            }

            // Availability: After creation date && before/today
            val isAvailable = !isFuture && !isBeforeCreation

            val dateKey = dateFormatKey.format(tempCalendar.time)
            val isChecked = checkedDates.contains(dateKey)

            // Also display dates before the creation date, but in light gray
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

        val remainingDays = 42 - days.size
        for (i in 1..remainingDays) {
            days.add(CalendarDay(0, false, false, null, "", false, false))
        }

        return days
    }

    private fun updateCompleteButtonText() {
        if (checkedDates.contains(selectedDate)) {
            // If the date is not checked, 'Undo' & gray button
            btnComplete.text = "Undo"
            btnComplete.backgroundTintList = getColorStateList(android.R.color.darker_gray)
        } else {
            // If the date is not checked, 'Complete' & the green button
            btnComplete.text = "Complete"
            btnComplete.backgroundTintList = getColorStateList(R.color.green)
        }
    }
}