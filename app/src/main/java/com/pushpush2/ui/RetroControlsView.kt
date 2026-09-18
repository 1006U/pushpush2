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
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

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
    private var ringInnerRadius = 0f

    private var pressedDirection: Direction? = null
    private var pressedSoftKey: SoftKey? = null

    private val repeatRunnable = object : Runnable {
        override fun run() {
            val direction = pressedDirection ?: return
            onDirection?.invoke(direction)
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
            canvas = canvas,
            rect = stageRect,
            label = "STAGE",
            pressed = pressedSoftKey == SoftKey.STAGE
        )
        drawSoftKey(
            canvas = canvas,
            rect = retryRect,
            label = "RETRY",
            pressed = pressedSoftKey == SoftKey.RETRY
        )

        dpadRadius = min(dpF(72), (w - dpF(72)) / 2f)
        centerRadius = dpadRadius * 0.28f
        ringInnerRadius = dpadRadius * 0.47f
        dpadCx = w / 2f
        dpadCy = top + softHeight + dpF(18) + dpadRadius

        drawAnycallDpad(canvas)
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
                        directionAt(event.x, event.y)?.let(::pressDirection)
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
                    pressedDirection = null

                    if (direction != null) {
                        pressDirection(direction)
                    } else {
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
        if (pressedDirection == direction) return

        pressedDirection = direction

        performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        onDirection?.invoke(direction)

        removeCallbacks(repeatRunnable)
        postDelayed(repeatRunnable, INITIAL_REPEAT_DELAY_MS)

        invalidate()
    }

    private fun releaseDirection() {
        cancelRepeat()
        pressedDirection = null
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
        if (pressed) {
            drawLedGlowRoundRect(canvas, rect)
        }

        paint.style = Paint.Style.FILL
        paint.color = if (pressed) {
            LED_BLUE_DARK
        } else {
            SOFT_KEY_NORMAL
        }
        canvas.drawRoundRect(rect, dpF(19), dpF(19), paint)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = if (pressed) dpF(2.2f) else dpF(1.2f)
        paint.color = if (pressed) LED_BLUE_BRIGHT else SOFT_KEY_BORDER
        canvas.drawRoundRect(rect, dpF(19), dpF(19), paint)

        textPaint.textSize = dpF(12)
        textPaint.color = if (pressed) Color.WHITE else TEXT_NORMAL

        val baseline =
            rect.centerY() - (textPaint.descent() + textPaint.ascent()) / 2f

        canvas.drawText(label, rect.centerX(), baseline, textPaint)
    }

    private fun drawLedGlowRoundRect(
        canvas: Canvas,
        rect: RectF
    ) {
        paint.style = Paint.Style.STROKE

        paint.strokeWidth = dpF(8)
        paint.color = Color.argb(42, 0, 126, 255)
        canvas.drawRoundRect(
            rect,
            dpF(19),
            dpF(19),
            paint
        )

        paint.strokeWidth = dpF(4)
        paint.color = Color.argb(86, 0, 151, 255)
        canvas.drawRoundRect(
            rect,
            dpF(19),
            dpF(19),
            paint
        )
    }

    private fun drawAnycallDpad(canvas: Canvas) {
        // 애니콜 네비게이션 키처럼 바깥쪽 금속 링을 먼저 그린다.
        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(188, 197, 207)
        canvas.drawCircle(
            dpadCx,
            dpadCy + dpF(3),
            dpadRadius + dpF(8),
            paint
        )

        paint.color = Color.rgb(91, 103, 116)
        canvas.drawCircle(
            dpadCx,
            dpadCy,
            dpadRadius + dpF(3),
            paint
        )

        // 눌린 방향 뒤로 파란 LED halo가 퍼지는 느낌.
        pressedDirection?.let { direction ->
            drawLedHalo(canvas, direction)
        }

        Direction.entries.forEach { direction ->
            drawRingSegment(
                canvas = canvas,
                direction = direction,
                pressed = pressedDirection == direction
            )
        }

        // 중심 확인키.
        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(142, 153, 166)
        canvas.drawCircle(
            dpadCx,
            dpadCy,
            centerRadius,
            paint
        )

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = dpF(1.6f)
        paint.color = Color.rgb(43, 54, 66)
        canvas.drawCircle(
            dpadCx,
            dpadCy,
            centerRadius,
            paint
        )

        // 원본 애니콜 다이얼의 작은 중심 점 느낌.
        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(93, 107, 122)
        canvas.drawCircle(
            dpadCx,
            dpadCy,
            centerRadius * 0.16f,
            paint
        )

        Direction.entries.forEach { direction ->
            drawArrow(
                canvas = canvas,
                direction = direction,
                pressed = pressedDirection == direction
            )
        }

        // 외곽선.
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = dpF(1.6f)
        paint.color = Color.rgb(27, 35, 44)
        canvas.drawCircle(
            dpadCx,
            dpadCy,
            dpadRadius,
            paint
        )
    }

    private fun drawRingSegment(
        canvas: Canvas,
        direction: Direction,
        pressed: Boolean
    ) {
        val startAngle = when (direction) {
            Direction.UP -> 233f
            Direction.RIGHT -> 323f
            Direction.DOWN -> 53f
            Direction.LEFT -> 143f
        }

        val sweep = 74f

        val outerRect = RectF(
            dpadCx - dpadRadius,
            dpadCy - dpadRadius,
            dpadCx + dpadRadius,
            dpadCy + dpadRadius
        )

        val innerRect = RectF(
            dpadCx - ringInnerRadius,
            dpadCy - ringInnerRadius,
            dpadCx + ringInnerRadius,
            dpadCy + ringInnerRadius
        )

        val path = Path().apply {
            arcTo(outerRect, startAngle, sweep)
            arcTo(innerRect, startAngle + sweep, -sweep)
            close()
        }

        paint.style = Paint.Style.FILL
        paint.color = if (pressed) {
            LED_BLUE_DARK
        } else {
            DIRECTION_NORMAL
        }
        canvas.drawPath(path, paint)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = if (pressed) dpF(2f) else dpF(1f)
        paint.color = if (pressed) {
            LED_BLUE_BRIGHT
        } else {
            Color.rgb(35, 44, 55)
        }
        canvas.drawPath(path, paint)

        if (pressed) {
            // 안쪽에 한 번 더 밝은 선을 넣어 실제 LED가 켜진 듯 보이게 한다.
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = dpF(1.2f)
            paint.color = LED_BLUE_PALE
            canvas.drawArc(
                RectF(
                    dpadCx - dpadRadius * 0.86f,
                    dpadCy - dpadRadius * 0.86f,
                    dpadCx + dpadRadius * 0.86f,
                    dpadCy + dpadRadius * 0.86f
                ),
                startAngle + 5f,
                sweep - 10f,
                false,
                paint
            )
        }
    }

    private fun drawLedHalo(
        canvas: Canvas,
        direction: Direction
    ) {
        val angle = when (direction) {
            Direction.UP -> -90f
            Direction.RIGHT -> 0f
            Direction.DOWN -> 90f
            Direction.LEFT -> 180f
        }

        val radians = Math.toRadians(angle.toDouble())
        val glowDistance = dpadRadius * 0.63f
        val gx = dpadCx + cos(radians).toFloat() * glowDistance
        val gy = dpadCy + sin(radians).toFloat() * glowDistance

        paint.style = Paint.Style.FILL
        paint.color = Color.argb(35, 0, 119, 255)
        canvas.drawCircle(
            gx,
            gy,
            dpadRadius * 0.46f,
            paint
        )

        paint.color = Color.argb(58, 0, 153, 255)
        canvas.drawCircle(
            gx,
            gy,
            dpadRadius * 0.33f,
            paint
        )
    }

    private fun drawArrow(
        canvas: Canvas,
        direction: Direction,
        pressed: Boolean
    ) {
        val distance = dpadRadius * 0.70f
        val size = dpadRadius * 0.105f

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
        paint.color = if (pressed) {
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

        val DIRECTION_NORMAL: Int =
            Color.rgb(71, 82, 95)

        val SOFT_KEY_NORMAL: Int =
            Color.rgb(211, 217, 224)
        val SOFT_KEY_BORDER: Int =
            Color.rgb(91, 102, 115)
        val TEXT_NORMAL: Int =
            Color.rgb(44, 54, 66)

        val LED_BLUE_DARK: Int =
            Color.rgb(18, 70, 170)
        val LED_BLUE_BRIGHT: Int =
            Color.rgb(0, 145, 255)
        val LED_BLUE_PALE: Int =
            Color.rgb(105, 205, 255)
    }
}
