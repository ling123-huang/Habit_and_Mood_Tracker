package com.example.habitmood

import android.app.Activity
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import android.widget.*
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.util.Calendar
import android.widget.EditText
import androidx.appcompat.widget.Toolbar
import com.google.android.material.switchmaterial.SwitchMaterial

class AddHabitActivity : AppCompatActivity() {

    // 나중에 데이터를 저장할 때 쓸 변수들
    private var selectedHour = 9
    private var selectedMinute = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_habit)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        val switchAlarm = findViewById<SwitchMaterial>(R.id.switchAlarm)
        val tvTimePicker = findViewById<TextView>(R.id.tvTimePicker)
        val layoutAlarmDetails = findViewById<LinearLayout>(R.id.layoutAlarmDetails)
        val radioGroupFrequency = findViewById<RadioGroup>(R.id.radioGroupFrequency)
        val chipGroupDays = findViewById<ChipGroup>(R.id.chipGroupDays)
        val etHabitName = findViewById<EditText>(R.id.etHabitName)
        val btnSave = findViewById<Button>(R.id.btnSaveHabit)


        // 뒤로가기
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        // 3. 알람 스위치 동작 (시간 및 요일 설정 보이기/숨기기)
        switchAlarm.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                layoutAlarmDetails.visibility = View.VISIBLE
            } else {
                layoutAlarmDetails.visibility = View.GONE
            }
        }

        // 시간 선택기
        tvTimePicker.setOnClickListener {
            val calendar = Calendar.getInstance()
            // 현재 시간으로 초기값 설정
            val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
            val currentMinute = calendar.get(Calendar.MINUTE)

            val timeDialog = TimePickerDialog(
                this,
                android.R.style.Theme_Holo_Light_Dialog_NoActionBar, // 원하시는 스피너 스타일
                { _, hourOfDay, minute ->
                    selectedHour = hourOfDay
                    selectedMinute = minute

                    // 시간 텍스트 업데이트 (AM/PM 포맷)
                    val amPm = if (hourOfDay < 12) "AM" else "PM"
                    val hour12 = if (hourOfDay > 12) hourOfDay - 12 else if (hourOfDay == 0) 12 else hourOfDay
                    val timeString = String.format("%02d:%02d %s", hour12, minute, amPm)
                    tvTimePicker.text = timeString
                },
                currentHour,
                currentMinute,
                false // false = AM/PM 모드
            )
            // 배경 투명하게 (둥근 모서리 다이얼로그를 위해)
            timeDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
            timeDialog.show()
        }
        // 5. 빈도 선택
        radioGroupFrequency.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id.rbSpecificDays) {
                chipGroupDays.visibility = View.VISIBLE
            } else {
                chipGroupDays.visibility = View.GONE
            }
        }

        btnSave.setOnClickListener {
            val habitName = etHabitName.text.toString().trim()

            if (habitName.isEmpty()) {
                Toast.makeText(this, "Please enter habit name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val resultIntent = Intent().apply {
                putExtra("NEW_HABIT_NAME", habitName)

                // 알람이 켜져있다면 추가 정보도 보냄
                if (switchAlarm.isChecked) {
                    putExtra("IS_ALARM_ON", true)
                    putExtra("ALARM_HOUR", selectedHour)
                    putExtra("ALARM_MINUTE", selectedMinute)

                    // 요일 정보 수집
                    val daysList = ArrayList<String>()
                    if (radioGroupFrequency.checkedRadioButtonId == R.id.rbEveryday) {
                        daysList.add("Everyday")
                    } else {
                        // 선택된 Chip들의 텍스트를 가져옴
                        for (id in chipGroupDays.checkedChipIds) {
                            val chip = findViewById<Chip>(id)
                            daysList.add(chip.text.toString())
                        }
                    }
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
}