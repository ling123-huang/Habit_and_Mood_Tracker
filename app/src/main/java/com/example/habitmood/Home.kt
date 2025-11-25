package com.example.habitmood

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class Home: AppCompatActivity() {

    private lateinit var habitRecyclerView: RecyclerView
    private lateinit var fabAddHabit: FloatingActionButton
    private lateinit var habitAdapter: HabitAdapter

    // 초기 습관 목록
    private val habitList = mutableListOf("Water Drinking", "Walking", "Meditation")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        habitRecyclerView = findViewById(R.id.habit_RecyclerView)
        fabAddHabit = findViewById(R.id.fabAddHabit)

        habitAdapter = HabitAdapter(habitList)
        habitRecyclerView.adapter = habitAdapter

        fabAddHabit.setOnClickListener {
            val newHabit = "New Habit " + (habitList.size + 1)
            habitList.add(newHabit)
            habitAdapter.notifyItemInserted(habitList.size - 1)
        }
    }
}
