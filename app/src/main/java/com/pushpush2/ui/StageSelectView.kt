package com.pushpush2.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import kotlin.math.ceil
import kotlin.math.max

class StageSelectView(
    context: Context,
    private val totalStages: Int,
    private val unlockedStages: Int,
    private val currentStage: Int,
    private val onStageSelected: (Int) -> Unit
) : View(context) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = android.graphics.Typeface.create(
            android.graphics.Typeface.MONOSPACE,
            android.graphics.Typeface.BOLD
        )
    }

    private var cellWidth = 0f
    private var cellHeight = 0f
    private var pressedStage: Int? = null

    override fun onMeasure(
        widthMeasureSpec: Int,
        heightMeasureSpec: Int
    ) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val usableWidth = max(1, width - paddingLeft - paddingRight)

        cellWidth = usableWidth / COLUMN_COUNT.toFloat()
        cellHeight = max(dpF(52), cellWidth * 0.82f)

        val rows = ceil(totalStages / COLUMN_COUNT.toFloat()).toInt()
        val desiredHeight =
            paddingTop + paddingBottom + (rows * cellHeight).toInt()

        setMeasuredDimension(
            width,
            resolveSize(desiredHeight, heightMeasureSpec)
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        paint.style = Paint.Style.FILL
        paint.color = BACKGROUND
        canvas.drawRect(
            0f,
            0f,
            width.toFloat(),
            height.toFloat(),
            paint
        )

        for (stage in 1..totalStages) {
            drawStageCell(canvas, stage)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN,
            MotionEvent.ACTION_MOVE -> {
                pressedStage = stageAt(event.x, event.y)
                invalidate()
                return true
            }

            MotionEvent.ACTION_UP -> {
                val stage = stageAt(event.x, event.y)
                val pressed = pressedStage

                pressedStage = null
                invalidate()
                performClick()

                if (
                    stage != null &&
                    stage == pressed &&
                    stage <= unlockedStages
                ) {
                    onStageSelected(stage)
                }

                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                pressedStage = null
                invalidate()
                return true
            }
        }

        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    private fun drawStageCell(
        canvas: Canvas,
        stage: Int
    ) {
        val index = stage - 1
        val column = index % COLUMN_COUNT
        val row = index / COLUMN_COUNT

        val left =
            paddingLeft + column * cellWidth + dpF(4)
        val top =
            paddingTop + row * cellHeight + dpF(4)

        val rect = RectF(
            left,
            top,
            left + cellWidth - dpF(8),
            top + cellHeight - dpF(8)
        )

        val unlocked = stage <= unlockedStages
        val current = stage == currentStage
        val pressed = stage == pressedStage

        paint.style = Paint.Style.FILL
        paint.color = when {
            pressed && unlocked -> Color.rgb(91, 111, 132)
            current -> Color.rgb(109, 131, 154)
            unlocked -> Color.rgb(211, 217, 224)
            else -> Color.rgb(154, 162, 171)
        }

        canvas.drawRoundRect(
            rect,
            dpF(9),
            dpF(9),
            paint
        )

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = if (current) dpF(2) else dpF(1)
        paint.color = if (current) {
            Color.rgb(236, 242, 248)
        } else {
            Color.rgb(74, 86, 101)
        }

        canvas.drawRoundRect(
            rect,
            dpF(9),
            dpF(9),
            paint
        )

        textPaint.textSize = dpF(14)
        textPaint.color = if (unlocked) {
            if (current) Color.WHITE else Color.rgb(42, 52, 64)
        } else {
            Color.rgb(91, 98, 106)
        }

        val baseline =
            rect.centerY() - (textPaint.descent() + textPaint.ascent()) / 2f

        val label = if (unlocked) {
            "%02d".format(stage)
        } else {
            "·"
        }

        canvas.drawText(
            label,
            rect.centerX(),
            baseline,
            textPaint
        )
    }

    private fun stageAt(
        x: Float,
        y: Float
    ): Int? {
        if (
            x < paddingLeft ||
            x >= width - paddingRight ||
            y < paddingTop
        ) {
            return null
        }

        val column =
            ((x - paddingLeft) / cellWidth).toInt()

        val row =
            ((y - paddingTop) / cellHeight).toInt()

        if (column !in 0 until COLUMN_COUNT || row < 0) {
            return null
        }

        val stage =
            row * COLUMN_COUNT + column + 1

        return stage.takeIf { it in 1..totalStages }
    }

    private fun dpF(value: Int): Float =
        value * resources.displayMetrics.density

    private companion object {
        const val COLUMN_COUNT = 6

        val BACKGROUND: Int =
            Color.rgb(184, 193, 202)
    }
}
