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

    private var pressedDirection: Direction? = null
    private var pressedSoftKey: SoftKey? = null
    private var repeating = false

    private val repeatRunnable = object : Runnable {
        override fun run() {
            val direction = pressedDirection ?: return

            onDirection?.invoke(direction)
            repeating = true
            postDelayed(this, REPEAT_INTERVAL_MS)
        }
    }

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

        drawSoftKey(
            canvas,
            stageRect,
            "STAGE",
            pressedSoftKey == SoftKey.STAGE
        )
        drawSoftKey(
            canvas,
            retryRect,
            "RETRY",
            pressedSoftKey == SoftKey.RETRY
        )

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

        drawPressedWedge(canvas)

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
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)

                when {
                    stageRect.contains(event.x, event.y) -> {
                        pressedSoftKey = SoftKey.STAGE
                        invalidate()
                    }

                    retryRect.contains(event.x, event.y) -> {
                        pressedSoftKey = SoftKey.RETRY
                        invalidate()
                    }

                    else -> {
                        val direction = directionAt(event.x, event.y)
                        if (direction != null) {
                            pressDirection(direction)
                        }
                    }
                }

                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (pressedSoftKey != null) {
                    val stillInside = when (pressedSoftKey) {
                        SoftKey.STAGE -> stageRect.contains(event.x, event.y)
                        SoftKey.RETRY -> retryRect.contains(event.x, event.y)
                        null -> false
                    }

                    if (!stillInside) {
                        pressedSoftKey = null
                        invalidate()
                    }
                    return true
                }

                val direction = directionAt(event.x, event.y)

                if (direction != pressedDirection) {
                    cancelRepeat()

                    if (direction != null) {
                        pressDirection(direction)
                    } else {
                        pressedDirection = null
                        invalidate()
                    }
                }

                return true
            }

            MotionEvent.ACTION_UP -> {
                parent?.requestDisallowInterceptTouchEvent(false)

                when (pressedSoftKey) {
                    SoftKey.STAGE -> {
                        if (stageRect.contains(event.x, event.y)) {
                            performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            onStageClick?.invoke()
                        }
                    }

                    SoftKey.RETRY -> {
                        if (retryRect.contains(event.x, event.y)) {
                            performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            onRetryClick?.invoke()
                        }
                    }

                    null -> Unit
                }

                pressedSoftKey = null
                releaseDirection()
                performClick()
                invalidate()
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                parent?.requestDisallowInterceptTouchEvent(false)
                pressedSoftKey = null
                releaseDirection()
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

    override fun onDetachedFromWindow() {
        cancelRepeat()
        super.onDetachedFromWindow()
    }

    private fun pressDirection(direction: Direction) {
        if (pressedDirection == direction) {
            return
        }

        pressedDirection = direction
        repeating = false

        performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        onDirection?.invoke(direction)

        removeCallbacks(repeatRunnable)
        postDelayed(repeatRunnable, INITIAL_REPEAT_DELAY_MS)

        invalidate()
    }

    private fun releaseDirection() {
        cancelRepeat()
        pressedDirection = null
        repeating = false
    }

    private fun cancelRepeat() {
        removeCallbacks(repeatRunnable)
    }

    private fun directionAt(
        x: Float,
        y: Float
    ): Direction? {
        val dx = x - dpadCx
        val dy = y - dpadCy
        val distanceSquared = dx * dx + dy * dy

        if (distanceSquared > dpadRadius * dpadRadius) {
            return null
        }

        if (distanceSquared < centerRadius * centerRadius) {
            return null
        }

        return if (abs(dx) > abs(dy)) {
            if (dx < 0f) Direction.LEFT else Direction.RIGHT
        } else {
            if (dy < 0f) Direction.UP else Direction.DOWN
        }
    }

    private fun drawSoftKey(
        canvas: Canvas,
        rect: RectF,
        label: String,
        pressed: Boolean
    ) {
        paint.style = Paint.Style.FILL
        paint.color = if (pressed) {
            Color.rgb(166, 176, 188)
        } else {
            Color.rgb(211, 217, 224)
        }

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

    private fun drawPressedWedge(canvas: Canvas) {
        val direction = pressedDirection ?: return

        val startAngle = when (direction) {
            Direction.UP -> 225f
            Direction.RIGHT -> 315f
            Direction.DOWN -> 45f
            Direction.LEFT -> 135f
        }

        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(54, 65, 77)

        val rect = RectF(
            dpadCx - dpadRadius,
            dpadCy - dpadRadius,
            dpadCx + dpadRadius,
            dpadCy + dpadRadius
        )

        canvas.drawArc(rect, startAngle, 90f, true, paint)
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
        paint.color = if (pressedDirection == direction) {
            Color.WHITE
        } else {
            Color.rgb(225, 231, 237)
        }

        canvas.drawPath(path, paint)
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun dpF(value: Int): Float =
        value * resources.displayMetrics.density

    private fun dpF(value: Float): Float =
        value * resources.displayMetrics.density

    private enum class SoftKey {
        STAGE,
        RETRY
    }

    private companion object {
        const val INITIAL_REPEAT_DELAY_MS = 280L
        const val REPEAT_INTERVAL_MS = 110L
    }
}
