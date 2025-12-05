package com.example.habitmood

import android.app.Activity
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.android.material.switchmaterial.SwitchMaterial
import java.util.Calendar

class AddHabitActivity : AppCompatActivity() {

    private lateinit var toolbar: Toolbar
    private lateinit var etHabitName: EditText
    private lateinit var switchAlarm: SwitchMaterial
    private lateinit var layoutAlarmDetails: LinearLayout
    private lateinit var tvTimePicker: TextView
    private lateinit var btnSaveHabit: Button

    // 선택된 알람 시간
    private var selectedHour = 9
    private var selectedMinute = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_habit)

        // View 초기화
        initViews()

        // Toolbar 설정
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        // 리스너 설정
        setupListeners()
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        etHabitName = findViewById(R.id.etHabitName)
        switchAlarm = findViewById(R.id.switchAlarm)
        layoutAlarmDetails = findViewById(R.id.layoutAlarmDetails)
        tvTimePicker = findViewById(R.id.tvTimePicker)
        btnSaveHabit = findViewById(R.id.btnSaveHabit)
    }

    private fun setupListeners() {
        // 알람 스위치 동작 (시간 설정 보이기/숨기기)
        switchAlarm.setOnCheckedChangeListener { _, isChecked ->
            layoutAlarmDetails.visibility = if (isChecked) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }

        // 시간 선택기
        tvTimePicker.setOnClickListener {
            showTimePicker()
        }

        // 저장 버튼
        btnSaveHabit.setOnClickListener {
            saveHabit()
        }
    }

    private fun showTimePicker() {
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)

        val timeDialog = TimePickerDialog(
            this,
            android.R.style.Theme_Holo_Light_Dialog_NoActionBar,
            { _, hourOfDay, minute ->
                selectedHour = hourOfDay
                selectedMinute = minute

                // 시간 텍스트 업데이트 (AM/PM 포맷)
                val amPm = if (hourOfDay < 12) "AM" else "PM"
                val hour12 = if (hourOfDay > 12) {
                    hourOfDay - 12
                } else if (hourOfDay == 0) {
                    12
                } else {
                    hourOfDay
                }
                val timeString = String.format("%02d:%02d %s", hour12, minute, amPm)
                tvTimePicker.text = timeString
            },
            currentHour,
            currentMinute,
            false // false = AM/PM 모드
        )

        timeDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        timeDialog.show()
    }

    private fun saveHabit() {
        val habitName = etHabitName.text.toString().trim()

        // 습관 이름 검증
        if (habitName.isEmpty()) {
            Toast.makeText(this, "Please enter habit name", Toast.LENGTH_SHORT).show()
            return
        }

        val resultIntent = Intent().apply {
            putExtra("NEW_HABIT_NAME", habitName)

            // 알람이 켜져있다면 추가 정보도 보냄
            if (switchAlarm.isChecked) {
                putExtra("IS_ALARM_ON", true)
                putExtra("ALARM_HOUR", selectedHour)
                putExtra("ALARM_MINUTE", selectedMinute)

                val daysList = ArrayList<String>()
                daysList.add("Everyday")
                putStringArrayListExtra("SELECTED_DAYS", daysList)
            } else {
                putExtra("IS_ALARM_ON", false)
            }
        }

        setResult(Activity.RESULT_OK, resultIntent)
        Toast.makeText(this, "Habit created!", Toast.LENGTH_SHORT).show()
        finish()
    }
}