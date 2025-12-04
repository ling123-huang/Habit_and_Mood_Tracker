package com.example.habitmood

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class HabitAdapter(private val habitList: MutableList<Habit>,private val onDelete: (Int) -> Unit) :
    RecyclerView.Adapter<HabitAdapter.HabitViewHolder>() {

    // 각 습관의 체크 상태와 누적 일수를 저장 (임시)
    private val checkedStates = mutableMapOf<Int, Boolean>()
    private val streakDays = mutableMapOf<Int, Int>()

    inner class HabitViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvHabitName: TextView = itemView.findViewById(R.id.tvHabitName)    // 습관 이름
        val tvHabitDays: TextView = itemView.findViewById(R.id.tvHabitDays)    // "Started 날짜"
        val tvStreakDays: TextView = itemView.findViewById(R.id.tvStreakDays)  // 연속 일수 숫자

        val viewOuterCircle: View = itemView.findViewById(R.id.viewOuterCircle) // 동그라미 테두리
        val btnHabit: MaterialButton = itemView.findViewById(R.id.btnHabit)     // 카드 전체 버튼
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HabitViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.habit_item, parent, false)
        return HabitViewHolder(view)
    }


    override fun onBindViewHolder(holder: HabitViewHolder, position: Int) {
        val habit = habitList[position]

        holder.tvHabitName.text = habit.name

        holder.tvHabitDays.text =
            if (habit.createdDate.isNotEmpty()) "Started ${habit.createdDate}" else "Started"

        holder.tvStreakDays.text = "0/0"
        updateCheckState(holder, false)

        calculateProgressForHabit(habit) { displayText, isTodayChecked ->

            if (holder.adapterPosition == position) {
                holder.tvStreakDays.text = displayText
                updateCheckState(holder, isTodayChecked)
            }
        }

        holder.btnHabit.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, MonthlyDetail::class.java).apply {
                putExtra("HABIT_ID", habit.id)
                putExtra("HABIT_NAME", habit.name)
            }
            context.startActivity(intent)
        }

        holder.btnHabit.setOnLongClickListener {
            val context = holder.itemView.context
            AlertDialog.Builder(context)
                .setTitle("Delete habit")
                .setMessage("Delete \"${habit.name}\"?")
                .setPositiveButton("Delete") { _, _ ->
                    onDelete(holder.adapterPosition)
                }
                .setNegativeButton("Cancel", null)
                .show()
            true
        }
    }

    override fun getItemCount(): Int = habitList.size

    private fun calculateProgressForHabit(
        habit: Habit,
        callback: (displayText: String, isTodayChecked: Boolean) -> Unit
    ) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            callback("0/0", false)
            return
        }

        val db = FirebaseFirestore.getInstance()

        // checkins 전체 가져오기
        db.collection("users")
            .document(user.uid)
            .collection("habits")
            .document(habit.id)
            .collection("checkins")
            .get()
            .addOnSuccessListener { snapshot ->
                val checkinDates: Set<String> = snapshot.documents.map { it.id }.toSet()
                if (habit.createdDate.isEmpty()) {
                    // 생성 날짜를 모르면 단순히 완료 횟수/완료 횟수 로 표시
                    val done = checkinDates.size
                    val text = "$done/$done"
                    val todayKey = todayKey()
                    val isTodayChecked = checkinDates.contains(todayKey)
                    callback(text, isTodayChecked)
                    return@addOnSuccessListener
                }

                // 1) createdDate 문자열("yyyy.MM.dd") → Date
                val created = try {
                    SimpleDateFormat("yyyy.MM.dd", Locale.getDefault())
                        .parse(habit.createdDate)
                } catch (e: Exception) {
                    null
                }

                if (created == null) {
                    val done = checkinDates.size
                    val text = "$done/$done"
                    val todayKey = todayKey()
                    val isTodayChecked = checkinDates.contains(todayKey)
                    callback(text, isTodayChecked)
                    return@addOnSuccessListener
                }

                // 2) 스케줄 요일 정보
                val scheduledDays = habit.selectedDays.map { it.lowercase(Locale.getDefault()) }
                val treatAsEveryday =
                    scheduledDays.isEmpty() || scheduledDays.contains("everyday")

                // 3) createdDate ~ 오늘까지 하루씩 증가하면서
                //    그날이 "스케줄 요일"이면 total++,
                //    그리고 checkins 안에 있으면 done++.
                val cal = Calendar.getInstance()
                val endDate = cal.time   // 오늘
                cal.time = created

                val keyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                var total = 0
                var done = 0

                while (!cal.time.after(endDate)) {
                    val dow = cal.get(Calendar.DAY_OF_WEEK) // 1: Sun ~ 7: Sat
                    val weekdayKey = when (dow) {
                        Calendar.SUNDAY -> "sun"
                        Calendar.MONDAY -> "mon"
                        Calendar.TUESDAY -> "tue"
                        Calendar.WEDNESDAY -> "wed"
                        Calendar.THURSDAY -> "thu"
                        Calendar.FRIDAY -> "fri"
                        Calendar.SATURDAY -> "sat"
                        else -> ""
                    }

                    val isScheduled =
                        treatAsEveryday || scheduledDays.contains(weekdayKey)

                    if (isScheduled) {
                        total++
                        val dateKey = keyFormat.format(cal.time)
                        if (checkinDates.contains(dateKey)) {
                            done++
                        }
                    }

                    cal.add(Calendar.DAY_OF_MONTH, 1)
                }

                val todayKey = keyFormat.format(endDate)
                val isTodayChecked = checkinDates.contains(todayKey)

                val display = if (total > 0) "$done/$total" else "0/0"
                callback(display, isTodayChecked)
            }
            .addOnFailureListener {
                callback("0/0", false)
            }
    }
    // 오늘 날짜 key("yyyy-MM-dd")
    private fun todayKey(): String {
        val df = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return df.format(Date())
    }


    private fun updateCheckState(holder: HabitViewHolder, isTodayChecked: Boolean) {
        if (isTodayChecked) {
            // 오늘 완료: 연두색 테두리 + 글씨색
            holder.viewOuterCircle.setBackgroundResource(R.drawable.circle_outer_checked)
            holder.tvStreakDays.setTextColor(
                holder.itemView.context.getColor(R.color.light_green)
            )
        } else {
            // 오늘 미완료: 회색 테두리
            holder.viewOuterCircle.setBackgroundResource(R.drawable.circle_outer_unchecked)
            holder.tvStreakDays.setTextColor(
                holder.itemView.context.getColor(android.R.color.darker_gray)
            )
        }
    }
}