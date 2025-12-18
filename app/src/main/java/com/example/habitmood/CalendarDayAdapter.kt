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
    val moodEmoji: String? = null,//mood
    val date: String, // "2025-11-04" format
    val isFutureDate: Boolean = false, //future date
    val isAvailable: Boolean = true //Selectable and checkable (false for dates before creation or future dates)
)

class CalendarDayAdapter(
    private val days: List<CalendarDay>,
    private val selectedDate: String,
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

            if (day.moodEmoji != null) {
                // MoodStatistics (Mood emoji display)
                holder.tvDayNumber.text = day.moodEmoji
                holder.tvDayNumber.textSize = 22f
                holder.viewDayBackground.visibility = View.INVISIBLE
                holder.tvDayNumber.setTextColor(holder.itemView.context.getColor(R.color.black))
                holder.itemView.setOnClickListener(null) // Emoji dates cannot be clicked in MonthlyDetail
                holder.itemView.setOnClickListener {
                    onDayClick(day)
                }
            } else {
                // MonthlyDetail (Habit Check)
                holder.tvDayNumber.text = day.dayNumber.toString()
                holder.tvDayNumber.textSize = 14f
                holder.viewDayBackground.visibility = View.VISIBLE

                // Handling future dates or dates before the creation date
                if (!day.isAvailable) {
                    holder.viewDayBackground.visibility = View.VISIBLE

                    //Date before habit creation
                    holder.viewDayBackground.setBackgroundResource(R.drawable.day_circle_default)
                    holder.tvDayNumber.setTextColor(holder.itemView.context.getColor(R.color.gray))
                    //Cannot select or check
                    holder.itemView.setOnClickListener(null)
                    holder.itemView.isClickable = false

                } else {
                    holder.tvDayNumber.setTypeface(null, android.graphics.Typeface.BOLD)
                    // Green/gray display depending on whether it is checked
                    if (day.date == selectedDate) {
                        //Priority 1: The date I have 'selected' now (gray)
                        holder.viewDayBackground.setBackgroundResource(R.drawable.day_circle_selected)
                        holder.tvDayNumber.setTextColor(holder.itemView.context.getColor(R.color.white))
                    }
                    else if (day.isChecked) {
                        // Second priority: Dates already 'completed' (green)
                        holder.viewDayBackground.setBackgroundResource(R.drawable.day_circle_checked)
                        holder.tvDayNumber.setTextColor(holder.itemView.context.getColor(R.color.white))
                    }
                    else {
                        // 3rd Priority: Basic (Transparent/Border)
                        holder.viewDayBackground.setBackgroundResource(R.drawable.day_circle_default)
                        holder.tvDayNumber.setTextColor(holder.itemView.context.getColor(R.color.black))
                    }

                    // Click to activate
                    holder.itemView.setOnClickListener {
                        onDayClick(day)
                    }
                }
            }

        } else {
            // Hide dates from other months and dates before the creation date
            holder.tvDayNumber.visibility = View.INVISIBLE
            holder.viewDayBackground.visibility = View.INVISIBLE
            holder.itemView.setOnClickListener(null)
            holder.itemView.isClickable = false
        }
    }

    override fun getItemCount(): Int {
        Log.d("CalendarAdapter", "getItemCount: ${days.size}")
        return days.size
    }
}