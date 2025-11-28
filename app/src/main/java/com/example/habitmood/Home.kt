package com.example.habitmood

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.text.SimpleDateFormat
import java.util.*
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager

class Home: AppCompatActivity() {

    private lateinit var habitRecyclerView: RecyclerView
    private lateinit var fabAddHabit: FloatingActionButton
    private lateinit var habitAdapter: HabitAdapter
    private lateinit var tvDate: TextView

    // 초기 습관 목록(일시적)
    private val habitList = mutableListOf("Water Drinking", "Walking", "Meditation")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        habitRecyclerView = findViewById(R.id.habit_RecyclerView)
        fabAddHabit = findViewById(R.id.fabAddHabit)
        tvDate = findViewById(R.id.tvDate)

        setCurrentDate()

        habitAdapter = HabitAdapter(habitList)
        habitRecyclerView.layoutManager = LinearLayoutManager(this)
        habitRecyclerView.adapter = habitAdapter

        fabAddHabit.setOnClickListener {
            // AddHabitActivity로 이동
            val intent = Intent(this, AddHabitActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setCurrentDate() {
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("EEEE, dd MMM", Locale.ENGLISH)
        val currentDate = dateFormat.format(calendar.time)
        tvDate.text = currentDate
    }
}
