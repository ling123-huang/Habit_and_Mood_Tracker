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
            if (day.moodEmoji != null) {
                // ▶ CASE 1: 기분 통계 화면 (MoodStatistics)
                // 1. 숫자 대신 이모티콘 출력
                holder.tvDayNumber.text = day.moodEmoji
                // 2. 글씨(이모티콘) 크기 키움
                holder.tvDayNumber.textSize = 22f
                // 3. 배경 동그라미는 숨김 (이모티콘만 깔끔하게 나오도록)
                holder.viewDayBackground.visibility = View.INVISIBLE

                // (선택사항) 텍스트 색상 원복
                holder.tvDayNumber.setTextColor(holder.itemView.context.getColor(R.color.black))

            } else {
                // ▶ CASE 2: 습관 체크 화면 (MonthlyDetail) - 기존 로직 유지
                // 1. 날짜 숫자 출력
                holder.tvDayNumber.text = day.dayNumber.toString()
                // 2. 글씨 크기 원래대로
                holder.tvDayNumber.textSize = 14f
                // 3. 배경 동그라미 보이기
                holder.viewDayBackground.visibility = View.VISIBLE

                // 체크 여부에 따른 초록색/회색 처리
                if (day.isChecked) {
                    holder.viewDayBackground.setBackgroundResource(R.drawable.day_circle_checked)
                    holder.tvDayNumber.setTextColor(holder.itemView.context.getColor(R.color.black)) // 혹은 WHITE 등 디자인에 맞춰
                } else {
                    holder.viewDayBackground.setBackgroundResource(R.drawable.day_circle_default)
                    holder.tvDayNumber.setTextColor(holder.itemView.context.getColor(R.color.black))
                }
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
