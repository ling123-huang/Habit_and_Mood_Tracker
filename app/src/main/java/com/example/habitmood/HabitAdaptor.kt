package com.example.habitmood

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import androidx.appcompat.app.AlertDialog

class HabitAdapter(private val habitList: MutableList<Habit>,private val onDelete: (Int) -> Unit) :
    RecyclerView.Adapter<HabitAdapter.HabitViewHolder>() {

    // 각 습관의 체크 상태와 누적 일수를 저장 (임시)
    private val checkedStates = mutableMapOf<Int, Boolean>()
    private val streakDays = mutableMapOf<Int, Int>()

    class HabitViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val btnHabit: MaterialButton = itemView.findViewById(R.id.btnHabit)
        val tvHabitName: TextView = itemView.findViewById(R.id.tvHabitName)
        val tvHabitDays: TextView = itemView.findViewById(R.id.tvHabitDays)
        val viewOuterCircle: View = itemView.findViewById(R.id.viewOuterCircle)
        val tvStreakDays: TextView = itemView.findViewById(R.id.tvStreakDays)
        val checkCircleFrame: View = itemView.findViewById(R.id.checkCircleFrame)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HabitViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.habit_item, parent, false)
        return HabitViewHolder(view)
    }

    override fun onBindViewHolder(holder: HabitViewHolder, position: Int) {
        val habit = habitList[position]
        val habitName = habit.name
        val isChecked = checkedStates[position] ?: false
        val streak = streakDays[position] ?: 0

        holder.tvHabitName.text = habitName
        holder.tvHabitDays.text = "2025.11.04 ~"

        // 누적 일수 표시
        holder.tvStreakDays.text = streak.toString()

        // 체크 상태에 따라 UI 업데이트
        updateCheckState(holder, isChecked, streak)

        // 원 클릭 시 체크 토글
        holder.checkCircleFrame.setOnClickListener {
            val newCheckedState = !isChecked
            checkedStates[position] = newCheckedState

            // 체크하면 누적 일수 증가, 해제하면 감소
            if (newCheckedState) {
                streakDays[position] = streak + 1
            } else {
                streakDays[position] = maxOf(0, streak - 1)
            }

            notifyItemChanged(position)
        }

        // Button 클릭 시 MonthlyDetail 화면으로 이동
        holder.btnHabit.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, MonthlyDetail::class.java)
            intent.putExtra("HABIT_NAME", habitName)
            context.startActivity(intent)
        }

        holder.btnHabit.setOnLongClickListener {
            val currentPos = holder.bindingAdapterPosition
            if (currentPos == RecyclerView.NO_POSITION) {
                return@setOnLongClickListener true
            }

            val context = holder.itemView.context

            AlertDialog.Builder(context)
                .setTitle("Delete habit")
                .setMessage("Are you sure you want to delete \"$habitName\"?")
                .setPositiveButton("Delete") { _, _ ->
                    onDelete(currentPos)
                }
                .setNegativeButton("Cancel", null)
                .show()

            true
        }

    }

    private fun updateCheckState(holder: HabitViewHolder, isChecked: Boolean, streak: Int) {
        if (isChecked) {
            // 체크된 상태: 두 원 사이가 연두색으로 채워짐
            holder.viewOuterCircle.setBackgroundResource(R.drawable.circle_outer_checked)
            holder.tvStreakDays.setTextColor(holder.itemView.context.getColor(R.color.light_green))
        } else {
            // 체크 안 된 상태: 회색 테두리
            holder.viewOuterCircle.setBackgroundResource(R.drawable.circle_outer_unchecked)
            holder.tvStreakDays.setTextColor(holder.itemView.context.getColor(android.R.color.darker_gray))
        }
    }

    override fun getItemCount(): Int = habitList.size
}