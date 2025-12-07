package com.example.habitmood

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.core.content.ContextCompat

class CircularProgressView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var progress = 0f
    private val maxProgress = 100f

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.light_gray)
        style = Paint.Style.STROKE
        strokeWidth = 24f
        strokeCap = Paint.Cap.ROUND
    }

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.green)
        style = Paint.Style.STROKE
        strokeWidth = 24f
        strokeCap = Paint.Cap.ROUND
    }

    private val rect = RectF()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val diameter = Math.min(width, height).toFloat()
        val radius = diameter / 2f
        val padding = progressPaint.strokeWidth / 2

        rect.set(
            padding,
            padding,
            diameter - padding,
            diameter - padding
        )

        // 배경 원 그리기
        canvas.drawCircle(
            width / 2f,
            height / 2f,
            radius - padding,
            backgroundPaint
        )

        // 진행률 원호 그리기
        val sweepAngle = (progress / maxProgress) * 360f
        canvas.drawArc(
            rect,
            -90f, // 12시 방향부터 시작
            sweepAngle,
            false,
            progressPaint
        )
    }

    fun setProgress(newProgress: Int, animate: Boolean = true) {
        val targetProgress = newProgress.toFloat().coerceIn(0f, 100f)

        if (animate) {
            val animator = ValueAnimator.ofFloat(progress, targetProgress)
            animator.duration = 1000
            animator.interpolator = DecelerateInterpolator()
            animator.addUpdateListener { animation ->
                progress = animation.animatedValue as Float
                invalidate()
            }
            animator.start()
        } else {
            progress = targetProgress
            invalidate()
        }
    }
}