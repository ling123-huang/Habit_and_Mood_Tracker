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
    val date: String, // "2025-11-04" 형식
    val isFutureDate: Boolean = false, //미래 날짜 여부
    val isAvailable: Boolean = true //선택 및 체크 가능 여부 (생성일 이전, 미래 날짜는 false)
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
                // MoodStatistics (기분 이모지 표시)
                holder.tvDayNumber.text = day.moodEmoji
                holder.tvDayNumber.textSize = 22f
                holder.viewDayBackground.visibility = View.INVISIBLE
                holder.tvDayNumber.setTextColor(holder.itemView.context.getColor(R.color.black))
                holder.itemView.setOnClickListener(null) // 이모지 날짜는 MonthlyDetail에서 클릭 불가
                holder.itemView.setOnClickListener {
                    onDayClick(day)
                }
            } else {
                // MonthlyDetail (습관 체크)
                holder.tvDayNumber.text = day.dayNumber.toString()
                holder.tvDayNumber.textSize = 14f
                holder.viewDayBackground.visibility = View.VISIBLE

                // 미래 날짜 또는 생성일 이전 날짜 처리
                if (!day.isAvailable) {
                    holder.viewDayBackground.visibility = View.VISIBLE

                    // 습관 생성일 이전 날짜
                    holder.viewDayBackground.setBackgroundResource(R.drawable.day_circle_default)
                    holder.tvDayNumber.setTextColor(holder.itemView.context.getColor(R.color.gray))
                    //선택 및 체크 불가
                    holder.itemView.setOnClickListener(null)
                    holder.itemView.isClickable = false

                } else {
                    holder.tvDayNumber.setTypeface(null, android.graphics.Typeface.BOLD)
                    // 체크 여부에 따른 초록색/회색 처리
                    if (day.date == selectedDate) {
                        // 1순위: 내가 지금 '선택'한 날짜 (회색)
                        holder.viewDayBackground.setBackgroundResource(R.drawable.day_circle_selected)
                        holder.tvDayNumber.setTextColor(holder.itemView.context.getColor(R.color.white)) // 회색 배경엔 흰 글씨
                    }
                    else if (day.isChecked) {
                        // 2순위: 이미 '완료'한 날짜 (초록색)
                        holder.viewDayBackground.setBackgroundResource(R.drawable.day_circle_checked)
                        holder.tvDayNumber.setTextColor(holder.itemView.context.getColor(R.color.white)) // 초록 배경엔 흰 글씨
                    }
                    else {
                        // 3순위: 기본 (투명/테두리)
                        holder.viewDayBackground.setBackgroundResource(R.drawable.day_circle_default)
                        holder.tvDayNumber.setTextColor(holder.itemView.context.getColor(R.color.black)) // 기본 배경엔 검은 글씨
                    }

                    // 클릭 활성화
                    holder.itemView.setOnClickListener {
                        onDayClick(day)
                    }
                }
            }

        } else {
            // 다른 달의 날짜 및 생성일 이전 날짜는 숨김
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