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
import kotlin.math.roundToInt
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

        holder.tvStreakDays.text = "0%"
        updateCheckState(holder, false)

        calculateProgressForHabit(habit) { displayText, isTodayChecked ->
            if (holder.adapterPosition == position) {
                holder.tvStreakDays.text = displayText  // 这里现在会收到 "xx%"
                updateCheckState(holder, isTodayChecked)
            }
        }

        holder.btnHabit.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, MonthlyDetail::class.java).apply {
                putExtra("HABIT_ID", habit.id)
                putExtra("HABIT_NAME", habit.name)
                putExtra("HABIT_CREATED_DATE", habit.createdDate)
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
            callback("0%", false)
            return
        }

        val db = FirebaseFirestore.getInstance()

        db.collection("users")
            .document(user.uid)
            .collection("habits")
            .document(habit.id)
            .collection("checkins")
            .get()
            .addOnSuccessListener { snapshot ->
                val checkinDates: Set<String> = snapshot.documents.map { it.id }.toSet()

                // 今天有没有打卡 -> 用来决定圆圈颜色
                val todayKey = todayKey()
                val isTodayChecked = checkinDates.contains(todayKey)

                // 如果没有 createdDate，就没办法精确算，从简处理：
                if (habit.createdDate.isEmpty()) {
                    val done = checkinDates.size
                    val percent = if (done > 0) 100 else 0
                    callback("${percent}%", isTodayChecked)
                    return@addOnSuccessListener
                }

                // "yyyy.MM.dd" -> Date
                val created = try {
                    SimpleDateFormat("yyyy.MM.dd", Locale.getDefault())
                        .parse(habit.createdDate)
                } catch (e: Exception) {
                    null
                }

                if (created == null) {
                    val done = checkinDates.size
                    val percent = if (done > 0) 100 else 0
                    callback("${percent}%", isTodayChecked)
                    return@addOnSuccessListener
                }

                // 当前年月
                val now = Calendar.getInstance()
                val currentYear = now.get(Calendar.YEAR)
                val currentMonth = now.get(Calendar.MONTH)

                // 本月 1 号
                val monthStart = Calendar.getInstance().apply {
                    set(Calendar.YEAR, currentYear)
                    set(Calendar.MONTH, currentMonth)
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                // 创建日期
                val createdCal = Calendar.getInstance().apply { time = created }

                // 从 “max(本月 1 日, 创建日)” 开始算
                if (createdCal.after(monthStart)) {
                    monthStart.time = createdCal.time
                }

                // 本月最后一天
                val monthEnd = Calendar.getInstance().apply {
                    set(Calendar.YEAR, currentYear)
                    set(Calendar.MONTH, currentMonth)
                    set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }

                // 如果习惯是在这个月之后才创建的（未来），直接 0%
                if (created.after(monthEnd.time)) {
                    callback("0%", isTodayChecked)
                    return@addOnSuccessListener
                }

                // 安排的星期信息
                val scheduledDays = habit.selectedDays.map { it.lowercase(Locale.getDefault()) }
                val treatAsEveryday =
                    scheduledDays.isEmpty() || scheduledDays.contains("everyday")

                val cal = monthStart.clone() as Calendar
                val keyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

                var totalDays = 0   // 从创建日到本月最后一天，符合排程的天数
                var doneDays = 0    // 其中打卡的天数

                while (!cal.after(monthEnd)) {
                    val dow = cal.get(Calendar.DAY_OF_WEEK)
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

                    val isScheduled = treatAsEveryday || scheduledDays.contains(weekdayKey)

                    if (isScheduled) {
                        totalDays++
                        val dateKey = keyFormat.format(cal.time)
                        if (checkinDates.contains(dateKey)) {
                            doneDays++
                        }
                    }

                    cal.add(Calendar.DAY_OF_MONTH, 1)
                }

                val percent = if (totalDays > 0) {
                    (doneDays.toFloat() / totalDays * 100).toInt()
                } else {
                    0
                }

                callback("${percent}%", isTodayChecked)
            }
            .addOnFailureListener {
                callback("0%", false)
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