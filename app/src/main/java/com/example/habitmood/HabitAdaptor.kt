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

class HabitAdapter(
    private val habitList: MutableList<Habit>,
    private val onDelete: (Int) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_HABIT = 0
        private const val VIEW_TYPE_DIVIDER = 1
    }

    inner class HabitViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvHabitName: TextView = itemView.findViewById(R.id.tvHabitName)
        val tvHabitDays: TextView = itemView.findViewById(R.id.tvHabitDays)
        val tvStreakDays: TextView = itemView.findViewById(R.id.tvStreakDays)
        val viewOuterCircle: View = itemView.findViewById(R.id.viewOuterCircle)
        val btnHabit: MaterialButton = itemView.findViewById(R.id.btnHabit)
    }

    inner class DividerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvDivider: TextView = itemView.findViewById(R.id.tvDivider)
    }

    override fun getItemViewType(position: Int): Int {
        return if (habitList[position].id == "DIVIDER") {
            VIEW_TYPE_DIVIDER
        } else {
            VIEW_TYPE_HABIT
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_DIVIDER) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_habit_divider, parent, false)
            DividerViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.habit_item, parent, false)
            HabitViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val habit = habitList[position]

        if (holder is DividerViewHolder) {
            holder.tvDivider.text = habit.name
        } else if (holder is HabitViewHolder) {
            holder.tvHabitName.text = habit.name

            holder.tvHabitDays.text =
                if (habit.createdDate.isNotEmpty()) "Started ${habit.createdDate}" else "Started"

            holder.tvStreakDays.text = "0%"
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

                val todayKey = todayKey()
                val isTodayChecked = checkinDates.contains(todayKey)

                if (habit.createdDate.isEmpty()) {
                    val done = checkinDates.size
                    val percent = if (done > 0) 100 else 0
                    callback("${percent}%", isTodayChecked)
                    return@addOnSuccessListener
                }

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

                val now = Calendar.getInstance()
                val currentYear = now.get(Calendar.YEAR)
                val currentMonth = now.get(Calendar.MONTH)

                val monthStart = Calendar.getInstance().apply {
                    set(Calendar.YEAR, currentYear)
                    set(Calendar.MONTH, currentMonth)
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                val createdCal = Calendar.getInstance().apply { time = created }

                if (createdCal.after(monthStart)) {
                    monthStart.time = createdCal.time
                }

                val monthEnd = Calendar.getInstance().apply {
                    set(Calendar.YEAR, currentYear)
                    set(Calendar.MONTH, currentMonth)
                    set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }

                if (created.after(monthEnd.time)) {
                    callback("0%", isTodayChecked)
                    return@addOnSuccessListener
                }

                val scheduledDays = habit.selectedDays.map { it.lowercase(Locale.getDefault()) }
                val treatAsEveryday =
                    scheduledDays.isEmpty() || scheduledDays.contains("everyday")

                val cal = monthStart.clone() as Calendar
                val keyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

                var totalDays = 0
                var doneDays = 0

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

    private fun todayKey(): String {
        val df = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return df.format(Date())
    }

    private fun updateCheckState(holder: HabitViewHolder, isTodayChecked: Boolean) {
        if (isTodayChecked) {
            holder.viewOuterCircle.setBackgroundResource(R.drawable.circle_outer_checked)
            holder.tvStreakDays.setTextColor(
                holder.itemView.context.getColor(R.color.green)
            )
        } else {
            holder.viewOuterCircle.setBackgroundResource(R.drawable.circle_outer_unchecked)
            holder.tvStreakDays.setTextColor(
                holder.itemView.context.getColor(android.R.color.darker_gray)
            )
        }
    }
}