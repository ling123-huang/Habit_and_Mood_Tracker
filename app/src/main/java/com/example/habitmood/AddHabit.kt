package com.example.habitmood

import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.util.Calendar

class AddHabitActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_habit) // activity_add_habit.xml 레이아웃 연결

        val cbAlarm = findViewById<CheckBox>(R.id.cbAlarm)
        val tvTimePicker = findViewById<TextView>(R.id.tvTimePicker)
        val btnSave = findViewById<Button>(R.id.btnSaveHabit)

        //시간 선택 화면
        cbAlarm.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                tvTimePicker.visibility = View.VISIBLE
            } else {
                tvTimePicker.visibility = View.GONE
            }
        }

        // 시간 선택기 (Spinner 스타일)
        tvTimePicker.setOnClickListener {
            val calendar = Calendar.getInstance()
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)

            val timeDialog = TimePickerDialog(
                this,
                android.R.style.Theme_Holo_Light_Dialog_NoActionBar,
                { _, selectedHour, selectedMinute ->
                    val amPm = if (selectedHour < 12) "AM" else "PM"
                    val hour12 = if (selectedHour % 12 == 0) 12 else selectedHour % 12
                    val timeString = String.format("%02d:%02d %s", hour12, selectedMinute, amPm)

                    tvTimePicker.text = timeString
                },
                hour,
                minute,
                false
            )
            timeDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
            timeDialog.show()
        }

        // 'Add Habit' 버튼 클릭 이벤트
        btnSave.setOnClickListener {
            Toast.makeText(this, "Habit Added!", Toast.LENGTH_SHORT).show()

            finish() // Home 화면으로
        }
    }
}