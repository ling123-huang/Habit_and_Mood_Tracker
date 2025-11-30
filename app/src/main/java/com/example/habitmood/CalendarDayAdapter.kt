package com.example.habitmood

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class CalendarDay(
    val dayNumber: Int,
    val isCurrentMonth: Boolean,
    val isChecked: Boolean,
    val date: String // "2025-11-04" 형식
)

class CalendarDayAdapter(
    private val days: List<CalendarDay>,
    private val onDayClick: (CalendarDay) -> Unit
) : RecyclerView.Adapter<CalendarDayAdapter.DayViewHolder>() {

    class DayViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvDayNumber: TextView = itemView.findViewById(R.id.tvDayNumber)
        val viewDayBackground: View = itemView.findViewById(R.id.viewDayBackground)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.calendar_day_item, parent, false)
        Log.d("CalendarAdapter", "onCreateViewHolder called")
        return DayViewHolder(view)
    }

    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        val day = days[position]

        Log.d("CalendarAdapter", "Binding position $position: day=${day.dayNumber}, isCurrentMonth=${day.isCurrentMonth}")

        if (day.isCurrentMonth) {
            holder.tvDayNumber.text = day.dayNumber.toString()
            holder.tvDayNumber.visibility = View.VISIBLE
            holder.viewDayBackground.visibility = View.VISIBLE

            // 체크된 날짜는 초록 원, 아니면 회색 원
            if (day.isChecked) {
                holder.viewDayBackground.setBackgroundResource(R.drawable.day_circle_checked)
                holder.tvDayNumber.setTextColor(holder.itemView.context.getColor(R.color.black))
            } else {
                holder.viewDayBackground.setBackgroundResource(R.drawable.day_circle_default)
                holder.tvDayNumber.setTextColor(holder.itemView.context.getColor(R.color.black))
            }

            holder.itemView.setOnClickListener {
                onDayClick(day)
            }
        } else {
            // 다른 달의 날짜는 숨김
            holder.tvDayNumber.visibility = View.INVISIBLE
            holder.viewDayBackground.visibility = View.INVISIBLE
        }
    }

    override fun getItemCount(): Int {
        Log.d("CalendarAdapter", "getItemCount: ${days.size}")
        return days.size
    }
}