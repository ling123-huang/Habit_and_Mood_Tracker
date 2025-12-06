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

    //private val recordedDates: MutableSet<String> = mutableSetOf()
    private val recordedDates: MutableMap<String, String> = mutableMapOf()
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private val emojiList = listOf("😢", "😔", "😐", "😊", "😍")//추가

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

                    // [수정] 기분 숫자(1~5)를 가져와서 이모티콘으로 변환하여 저장
                    val moodVal = doc.getLong("mood")?.toInt() ?: 0
                    if (moodVal in 1..5) {
                        recordedDates[dateStr] = emojiList[moodVal - 1]
                    }
                }
                    //recordedDates.add(dateStr)

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

        val adapter = CalendarDayAdapter(days, "") { day ->
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
            val existingMood = document.getLong("mood")?.toInt() ?:0  // 1~5
            //체크 안된 날은 다이어그램 표시 X
            if (existingMood !in 1..5) {
                return@addOnSuccessListener
            }
            val existingNote = document.getString("note") ?: ""
            val dialogView = layoutInflater.inflate(R.layout.dialog_view_mood, null)

            // View 찾기
            val tvDate = dialogView.findViewById<TextView>(R.id.tvDialogDate)
            val tvMood = dialogView.findViewById<TextView>(R.id.tvDialogMoodEmoji)
            val tvNote = dialogView.findViewById<TextView>(R.id.tvDialogNote)

            tvDate.text = dateKey // 날짜 표시

            tvMood.text = emojiList[existingMood - 1] // 위에서 검사했으므로 안전함
            tvNote.text = if (existingNote.isNotEmpty()) existingNote else "No note recorded."


            // 다이얼로그 생성 및 표시
            val builder = AlertDialog.Builder(this)
                .setView(dialogView) // 커스텀 레이아웃 설정

            val dialog = builder.create()
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

            dialog.show()
        }
    }
    private fun generateCalendarDays(): List<CalendarDay> {
        val days = mutableListOf<CalendarDay>()
        val tempCalendar = calendar.clone() as Calendar

        tempCalendar.set(Calendar.DAY_OF_MONTH, 1)

        val firstDayOfWeek = tempCalendar.get(Calendar.DAY_OF_WEEK) // 1(일) ~ 7(토)
        val maxDayOfMonth = tempCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        for (i in 1 until firstDayOfWeek) {
            days.add(CalendarDay(0,
                false,
                false,
                null,
                ""))
        }

        val dateFormatKey = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
        for (day in 1..maxDayOfMonth) {
            tempCalendar.set(Calendar.DAY_OF_MONTH, day)
            val dateKey = dateFormatKey.format(tempCalendar.time)
            val emoji = recordedDates[dateKey]
            val isRecorded = recordedDates.contains(dateKey)

            days.add(CalendarDay(
                dayNumber = day,
                isCurrentMonth = true,
                isChecked = isRecorded,
                date = dateKey,
                moodEmoji = emoji // 여기서 이모티콘을 어댑터로 보냄
            ))
        }

        val remainingDays = 42 - days.size
        for (i in 1..remainingDays) {
            days.add(CalendarDay(0,
                false,
                false,
                null,
                ""))
        }

        return days
    }
    override fun onResume() {
        super.onResume()
        // 其他页面返回时，保持 Stats tab 选中
        bottomNavigationView.selectedItemId = R.id.menu_stats
    }
}
