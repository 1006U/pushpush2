package com.pushpush2.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.view.KeyEvent
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
    private var keyboardStage =
        currentStage.coerceIn(1, max(1, unlockedStages))

    init {
        isFocusable = true
        isFocusableInTouchMode = true
    }

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

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        val nextStage = when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_W ->
                keyboardStage - COLUMN_COUNT

            KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_S ->
                keyboardStage + COLUMN_COUNT

            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_A ->
                keyboardStage - 1

            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_D ->
                keyboardStage + 1

            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_NUMPAD_ENTER,
            KeyEvent.KEYCODE_SPACE,
            KeyEvent.KEYCODE_DPAD_CENTER -> {
                if (keyboardStage <= unlockedStages) {
                    onStageSelected(keyboardStage)
                }
                return true
            }

            else -> return super.onKeyDown(keyCode, event)
        }

        keyboardStage =
            nextStage.coerceIn(1, max(1, unlockedStages))
        invalidate()
        post { revealKeyboardSelection() }
        return true
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
                    keyboardStage = stage
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

    private fun revealKeyboardSelection() {
        if (cellHeight <= 0f) return

        val row = (keyboardStage - 1) / COLUMN_COUNT
        val top =
            paddingTop + (row * cellHeight).toInt()
        val bottom =
            top + cellHeight.toInt() + paddingBottom

        requestRectangleOnScreen(
            Rect(0, top, width, bottom),
            true
        )
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
        val keyboardSelected = stage == keyboardStage

        paint.style = Paint.Style.FILL
        paint.color = when {
            pressed && unlocked -> Color.rgb(91, 111, 132)
            keyboardSelected -> Color.rgb(75, 126, 91)
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
        paint.strokeWidth =
            if (keyboardSelected || current) dpF(2) else dpF(1)
        paint.color = when {
            keyboardSelected -> Color.WHITE
            current -> Color.rgb(236, 242, 248)
            else -> Color.rgb(74, 86, 101)
        }

        canvas.drawRoundRect(
            rect,
            dpF(9),
            dpF(9),
            paint
        )

        textPaint.textSize = dpF(14)
        textPaint.color = if (unlocked) {
            if (keyboardSelected || current) {
                Color.WHITE
            } else {
                Color.rgb(42, 52, 64)
            }
        } else {
            Color.rgb(91, 98, 106)
        }

        val baseline =
            rect.centerY() -
                (textPaint.descent() + textPaint.ascent()) / 2f

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
