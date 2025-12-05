package com.example.habitmood

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import android.widget.LinearLayout
import android.view.Gravity

import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.SetOptions
import java.text.SimpleDateFormat
import java.util.*

class MoodStatistics : AppCompatActivity() {

    private lateinit var calendarRecyclerView: RecyclerView
    private lateinit var tvCurrentMonth: TextView // XML ID: tvCurrentMonth
    private lateinit var btnPrevMonth: ImageButton
    private lateinit var btnNextMonth: ImageButton
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var tvVeryGoodPercent: TextView
    private lateinit var tvGoodPercent: TextView
    private lateinit var tvNeutralPercent: TextView
    private lateinit var tvSadPercent: TextView
    private lateinit var tvBadPercent: TextView

    private lateinit var progressVeryGood: ProgressBar
    private lateinit var progressGood: ProgressBar
    private lateinit var progressNeutral: ProgressBar
    private lateinit var progressSad: ProgressBar
    private lateinit var progressBad: ProgressBar
    private val calendar = Calendar.getInstance()
    private val monthTitleFormat = SimpleDateFormat("MMMM yyyy", Locale.ENGLISH)

    // 임시: 기분이 기록된 날짜들 (나중에 DB에서 가져올 데이터)
    private val recordedDates: MutableSet<String> = mutableSetOf()

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

        updateMonthTitle()

        // RecyclerView 설정 (7열 그리드)
        calendarRecyclerView.layoutManager = GridLayoutManager(this, 7)

        loadMoodsForAllTimeThenUpdate()
    }

    private fun initViews() {
        calendarRecyclerView = findViewById(R.id.calendarRecyclerView)

        tvCurrentMonth = findViewById(R.id.tvCurrentMonth)
        btnPrevMonth = findViewById(R.id.btnPrevMonth)
        btnNextMonth = findViewById(R.id.btnNextMonth)
        bottomNavigationView = findViewById(R.id.bottomAppBar)

        tvVeryGoodPercent = findViewById(R.id.tvVeryGoodPercent)
        tvGoodPercent = findViewById(R.id.tvGoodPercent)
        tvNeutralPercent = findViewById(R.id.tvNeutralPercent)
        tvSadPercent = findViewById(R.id.tvSadPercent)
        tvBadPercent = findViewById(R.id.tvBadPercent)

        progressVeryGood = findViewById(R.id.progressVeryGood)
        progressGood = findViewById(R.id.progressGood)
        progressNeutral = findViewById(R.id.progressNeutral)
        progressSad = findViewById(R.id.progressSad)
        progressBad = findViewById(R.id.progressBad)

        btnPrevMonth.setOnClickListener {
            calendar.add(Calendar.MONTH, -1)
            updateMonthTitle()
            updateUiWithCachedMoods()
        }

        btnNextMonth.setOnClickListener {
            calendar.add(Calendar.MONTH, 1)
            updateMonthTitle()
            updateUiWithCachedMoods()
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
    private fun updateMonthTitle() {
        tvCurrentMonth.text = monthTitleFormat.format(calendar.time)
    }

    private fun loadMoodsForAllTimeThenUpdate() {
        val user = auth.currentUser
        if (user == null) {
            recordedDates.clear()
            updateCalendar(emptyList())
            updateMoodOverview(null)
            return
        }

        db.collection("users")
            .document(user.uid)
            .collection("moods")
            .get()
            .addOnSuccessListener { snapshot ->
                recordedDates.clear()
                for (doc in snapshot) {
                    val dateStr = doc.getString("date") ?: continue
                    recordedDates.add(dateStr)
                }

                updateCalendar(snapshot.documents)
                updateMoodOverview(snapshot)
            }
    }

    private fun updateUiWithCachedMoods() {
        loadMoodsForAllTimeThenUpdate()
    }

    private fun updateMoodOverview(snapshot: QuerySnapshot?) {
        if (snapshot == null) {
            setOverviewZero()
            return
        }

        // counts[0] = bad(1), [1] = sad(2), [2] = neutral(3), [3] = good(4), [4] = very good(5)
        val counts = IntArray(5)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        val yearNow = calendar.get(Calendar.YEAR)
        val monthNow = calendar.get(Calendar.MONTH)

        for (doc in snapshot.documents) {
            val dateStr = doc.getString("date") ?: continue
            val mood = doc.getLong("mood")?.toInt() ?: continue
            if (mood !in 1..5) continue

            val date = try {
                sdf.parse(dateStr)
            } catch (e: Exception) {
                null
            } ?: continue

            val c = Calendar.getInstance().apply { time = date }

            if (c.get(Calendar.YEAR) == yearNow && c.get(Calendar.MONTH) == monthNow) {
                counts[mood - 1]++
            }
        }

        val total = counts.sum()
        if (total == 0) {
            setOverviewZero()
            return
        }

        fun percent(v: Int) = (v * 100f / total).toInt()

        val veryGood = percent(counts[4])
        val good = percent(counts[3])
        val neutral = percent(counts[2])
        val sad = percent(counts[1])
        val bad = percent(counts[0])

        tvVeryGoodPercent.text = "${veryGood}%"
        tvGoodPercent.text = "${good}%"
        tvNeutralPercent.text = "${neutral}%"
        tvSadPercent.text = "${sad}%"
        tvBadPercent.text = "${bad}%"

        progressVeryGood.progress = veryGood
        progressGood.progress = good
        progressNeutral.progress = neutral
        progressSad.progress = sad
        progressBad.progress = bad
    }
    private fun setOverviewZero() {
        tvVeryGoodPercent.text = "0%"
        tvGoodPercent.text = "0%"
        tvNeutralPercent.text = "0%"
        tvSadPercent.text = "0%"
        tvBadPercent.text = "0%"

        progressVeryGood.progress = 0
        progressGood.progress = 0
        progressNeutral.progress = 0
        progressSad.progress = 0
        progressBad.progress = 0
    }

    private fun updateCalendar(docs: List<com.google.firebase.firestore.DocumentSnapshot>?) {
        // 更新顶部月份文字
        updateMonthTitle()

        val days = generateCalendarDays()

        val adapter = CalendarDayAdapter(days) { day ->
            if (day.isCurrentMonth && day.dayNumber != 0) {
                onDayClicked(day.date)
            }
        }
        calendarRecyclerView.adapter = adapter
    }

    private fun onDayClicked(dateKey: String) {
        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show()
            return
        }


        val docRef = db.collection("users")
            .document(user.uid)
            .collection("moods")
            .document(dateKey)

        docRef.get().addOnSuccessListener { document ->
            val existingNote = document.getString("note") ?: ""
            val existingMood = document.getLong("mood")?.toInt()  // 1~5

            // 默认心情：如果以前没选过，就用 3 = Neutral
            var selectedMood = existingMood ?: 0

            // 动态创建一个竖直布局，里面放一排 emoji + 一个 EditText
            val padding = (16 * resources.displayMetrics.density).toInt()

            val container = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(padding, padding, padding, 0)
            }

            // 一排 emoji（Bad → Very Good）
            val moodRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
            }

            val emojis = listOf("😢", "😔", "😐", "😊", "😍")  // 1~5
            val moodViews = mutableListOf<TextView>()

            fun updateMoodHighlight() {
                moodViews.forEachIndexed { index, tv ->
                    if (index + 1 == selectedMood) {
                        tv.alpha = 1.0f
                        tv.scaleX = 1.2f
                        tv.scaleY = 1.2f
                    } else {
                        tv.alpha = 0.4f
                        tv.scaleX = 1.0f
                        tv.scaleY = 1.0f
                    }
                }
            }

            emojis.forEachIndexed { index, emoji ->
                val tv = TextView(this).apply {
                    text = emoji
                    textSize = 28f
                    setPadding(padding / 2, padding / 2, padding / 2, padding / 2)
                    isClickable = true
                    isFocusable = true
                    setOnClickListener {
                        selectedMood = index + 1      // 1~5
                        updateMoodHighlight()
                    }
                }
                moodViews.add(tv)
                moodRow.addView(tv)
            }

            // 初始高亮
            updateMoodHighlight()

            // 笔记输入框
            val noteEditText = EditText(this).apply {
                hint = "Add a note"
                setText(existingNote)
            }

            container.addView(moodRow)
            container.addView(noteEditText)

            val builder = AlertDialog.Builder(this)
                .setTitle(dateKey)
                .setView(container)

            builder.setPositiveButton("Save") { _, _ ->
                if (selectedMood == 0) {
                    Toast.makeText(this, "Please select a mood", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val data = hashMapOf(
                    "date" to dateKey,
                    "mood" to selectedMood,                         // 保存/修改当天心情
                    "note" to noteEditText.text.toString(),
                    "timestamp" to FieldValue.serverTimestamp()
                )
                // mood 字段在 Home 页面存，这里用 merge() 只改 note
                docRef.set(data, SetOptions.merge())
                    .addOnSuccessListener {
                        recordedDates.add(dateKey)
                        updateUiWithCachedMoods()
                    }
            }

            if (document.exists()) {
                builder.setNeutralButton("Delete") { _, _ ->
                    docRef.delete()
                        .addOnSuccessListener {
                            recordedDates.remove(dateKey)
                            updateUiWithCachedMoods()
                        }
                }
            }

            builder.setNegativeButton("Cancel", null)
            builder.show()
        }
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
    override fun onResume() {
        super.onResume()
        // 其他页面返回时，保持 Stats tab 选中
        bottomNavigationView.selectedItemId = R.id.menu_stats
    }
}
