package com.pushpush2.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import com.pushpush2.game.Direction
import kotlin.math.abs
import kotlin.math.min

class RetroControlsView(context: Context) : View(context) {

    var onDirection: ((Direction) -> Unit)? = null
    var onStageClick: (() -> Unit)? = null
    var onRetryClick: (() -> Unit)? = null

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(44, 54, 66)
        textAlign = Paint.Align.CENTER
        typeface = android.graphics.Typeface.create(
            android.graphics.Typeface.MONOSPACE,
            android.graphics.Typeface.BOLD
        )
    }

    private val stageRect = RectF()
    private val retryRect = RectF()

    private var dpadCx = 0f
    private var dpadCy = 0f
    private var dpadRadius = 0f
    private var centerRadius = 0f

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = resolveSize(dp(216), heightMeasureSpec)
        setMeasuredDimension(width, height)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val sidePadding = dpF(24)
        val top = dpF(8)
        val softHeight = dpF(38)
        val softGap = dpF(18)
        val softWidth = min(
            dpF(112),
            (w - sidePadding * 2f - softGap) / 2f
        )

        stageRect.set(
            w / 2f - softGap / 2f - softWidth,
            top,
            w / 2f - softGap / 2f,
            top + softHeight
        )
        retryRect.set(
            w / 2f + softGap / 2f,
            top,
            w / 2f + softGap / 2f + softWidth,
            top + softHeight
        )

        drawSoftKey(canvas, stageRect, "STAGE")
        drawSoftKey(canvas, retryRect, "RETRY")

        dpadRadius = min(dpF(72), (w - dpF(72)) / 2f)
        centerRadius = dpadRadius * 0.34f
        dpadCx = w / 2f
        dpadCy = top + softHeight + dpF(18) + dpadRadius

        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(190, 199, 208)
        canvas.drawCircle(
            dpadCx,
            dpadCy + dpF(3),
            dpadRadius + dpF(8),
            paint
        )

        paint.color = Color.rgb(71, 82, 95)
        canvas.drawCircle(dpadCx, dpadCy, dpadRadius, paint)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = dpF(1.5f)
        paint.color = Color.rgb(30, 39, 49)
        canvas.drawCircle(dpadCx, dpadCy, dpadRadius, paint)

        drawDivider(canvas, 45f)
        drawDivider(canvas, 135f)

        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(137, 149, 162)
        canvas.drawCircle(dpadCx, dpadCy, centerRadius, paint)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = dpF(1.5f)
        paint.color = Color.rgb(44, 54, 66)
        canvas.drawCircle(dpadCx, dpadCy, centerRadius, paint)

        drawArrow(canvas, Direction.UP)
        drawArrow(canvas, Direction.DOWN)
        drawArrow(canvas, Direction.LEFT)
        drawArrow(canvas, Direction.RIGHT)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action != MotionEvent.ACTION_DOWN) {
            return true
        }

        if (stageRect.contains(event.x, event.y)) {
            performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            onStageClick?.invoke()
            return true
        }

        if (retryRect.contains(event.x, event.y)) {
            performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            onRetryClick?.invoke()
            return true
        }

        val dx = event.x - dpadCx
        val dy = event.y - dpadCy
        val distanceSquared = dx * dx + dy * dy

        if (distanceSquared > dpadRadius * dpadRadius) {
            return true
        }

        if (distanceSquared < centerRadius * centerRadius) {
            performClick()
            return true
        }

        val direction = if (abs(dx) > abs(dy)) {
            if (dx < 0f) Direction.LEFT else Direction.RIGHT
        } else {
            if (dy < 0f) Direction.UP else Direction.DOWN
        }

        performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        onDirection?.invoke(direction)
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    private fun drawSoftKey(
        canvas: Canvas,
        rect: RectF,
        label: String
    ) {
        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(211, 217, 224)
        canvas.drawRoundRect(rect, dpF(19), dpF(19), paint)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = dpF(1.2f)
        paint.color = Color.rgb(91, 102, 115)
        canvas.drawRoundRect(rect, dpF(19), dpF(19), paint)

        textPaint.textSize = dpF(12)
        val baseline =
            rect.centerY() - (textPaint.descent() + textPaint.ascent()) / 2f

        canvas.drawText(label, rect.centerX(), baseline, textPaint)
    }

    private fun drawDivider(
        canvas: Canvas,
        angleDegrees: Float
    ) {
        val radians = Math.toRadians(angleDegrees.toDouble())
        val x = kotlin.math.cos(radians).toFloat() * dpadRadius
        val y = kotlin.math.sin(radians).toFloat() * dpadRadius

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = dpF(1.1f)
        paint.color = Color.rgb(35, 44, 55)

        canvas.drawLine(
            dpadCx - x,
            dpadCy - y,
            dpadCx + x,
            dpadCy + y,
            paint
        )
    }

    private fun drawArrow(
        canvas: Canvas,
        direction: Direction
    ) {
        val distance = dpadRadius * 0.62f
        val size = dpadRadius * 0.13f

        val cx = when (direction) {
            Direction.LEFT -> dpadCx - distance
            Direction.RIGHT -> dpadCx + distance
            else -> dpadCx
        }

        val cy = when (direction) {
            Direction.UP -> dpadCy - distance
            Direction.DOWN -> dpadCy + distance
            else -> dpadCy
        }

        val path = Path()

        when (direction) {
            Direction.UP -> {
                path.moveTo(cx, cy - size)
                path.lineTo(cx - size, cy + size)
                path.lineTo(cx + size, cy + size)
            }
            Direction.DOWN -> {
                path.moveTo(cx, cy + size)
                path.lineTo(cx - size, cy - size)
                path.lineTo(cx + size, cy - size)
            }
            Direction.LEFT -> {
                path.moveTo(cx - size, cy)
                path.lineTo(cx + size, cy - size)
                path.lineTo(cx + size, cy + size)
            }
            Direction.RIGHT -> {
                path.moveTo(cx + size, cy)
                path.lineTo(cx - size, cy - size)
                path.lineTo(cx - size, cy + size)
            }
        }

        path.close()

        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(225, 231, 237)
        canvas.drawPath(path, paint)
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun dpF(value: Int): Float =
        value * resources.displayMetrics.density

    private fun dpF(value: Float): Float =
        value * resources.displayMetrics.density
}
