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
    var onCenterClick: (() -> Unit)? = null

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
    private var dpadRx = 0f
    private var dpadRy = 0f
    private var centerRx = 0f
    private var centerRy = 0f
    private var ringInnerRx = 0f
    private var ringInnerRy = 0f

    private var pressedDirection: Direction? = null
    private var pressedSoftKey: SoftKey? = null
    private var pressedCenter = false

    private val repeatRunnable = object : Runnable {
        override fun run() {
            val direction = pressedDirection ?: return
            onDirection?.invoke(direction)
            postDelayed(this, REPEAT_INTERVAL_MS)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = resolveSize(dp(248), heightMeasureSpec)
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

        // Use more of the available lower screen area while keeping the
        // Anycall-style horizontal shape. Width is always capped to the view.
        dpadRadius = min(dpF(84), (w - dpF(48)) / 2f)

        dpadRx = min(
            dpadRadius * 1.90f,
            (w - dpF(8)) / 2f
        )
        dpadRy = dpadRadius * 0.90f
        centerRx = dpadRx * 0.25f
        centerRy = dpadRy * 0.23f
        ringInnerRx = dpadRx * 0.51f
        ringInnerRy = dpadRy * 0.51f

        dpadCx = w / 2f
        dpadCy = top + softHeight + dpF(18) + dpadRy

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

                    centerAt(event.x, event.y) -> {
                        pressedCenter = true
                        performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        invalidate()
                    }

                    else -> {
                        directionAt(event.x, event.y)?.let(::pressDirection)
                    }
                }

                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (pressedCenter) {
                    pressedCenter = centerAt(event.x, event.y)
                    invalidate()
                    return true
                }

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

                if (pressedCenter && centerAt(event.x, event.y)) {
                    onCenterClick?.invoke()
                }

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
                pressedCenter = false
                releaseDirection()
                performClick()
                invalidate()
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                parent?.requestDisallowInterceptTouchEvent(false)
                pressedSoftKey = null
                pressedCenter = false
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

    private fun centerAt(
        x: Float,
        y: Float
    ): Boolean {
        if (centerRx <= 0f || centerRy <= 0f) return false

        val dx = x - dpadCx
        val dy = y - dpadCy

        return (dx * dx) / (centerRx * centerRx) +
            (dy * dy) / (centerRy * centerRy) <= 1f
    }

    private fun directionAt(
        x: Float,
        y: Float
    ): Direction? {
        val dx = x - dpadCx
        val dy = y - dpadCy

        val outer =
            (dx * dx) / (dpadRx * dpadRx) +
                (dy * dy) / (dpadRy * dpadRy)

        if (outer > 1f) return null

        val inner =
            (dx * dx) / (centerRx * centerRx) +
                (dy * dy) / (centerRy * centerRy)

        if (inner < 1f) return null

        return if (abs(dx / dpadRx) > abs(dy / dpadRy)) {
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
        // 사진의 애니콜 네비게이션 키처럼 가로로 살짝 넓은 타원형 외곽.
        val shadowRect = RectF(
            dpadCx - dpadRx - dpF(8),
            dpadCy - dpadRy - dpF(4),
            dpadCx + dpadRx + dpF(8),
            dpadCy + dpadRy + dpF(8)
        )

        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(188, 197, 207)
        canvas.drawOval(shadowRect, paint)

        val bezelRect = RectF(
            dpadCx - dpadRx - dpF(3),
            dpadCy - dpadRy - dpF(2),
            dpadCx + dpadRx + dpF(3),
            dpadCy + dpadRy + dpF(3)
        )

        paint.color = Color.rgb(91, 103, 116)
        canvas.drawOval(bezelRect, paint)

        Direction.entries.forEach { direction ->
            drawRingSegment(
                canvas = canvas,
                direction = direction,
                pressed = pressedDirection == direction
            )
        }

        // 중앙 확인키도 바깥 다이얼과 같은 계열의 가로 타원형으로 맞춘다.
        val centerRect = RectF(
            dpadCx - centerRx,
            dpadCy - centerRy,
            dpadCx + centerRx,
            dpadCy + centerRy
        )

        if (pressedCenter) {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = dpF(6f)
            paint.color = Color.argb(72, 0, 151, 255)
            canvas.drawOval(centerRect, paint)
        }

        paint.style = Paint.Style.FILL
        paint.color = if (pressedCenter) {
            LED_BLUE_DARK
        } else {
            Color.rgb(142, 153, 166)
        }
        canvas.drawOval(centerRect, paint)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = if (pressedCenter) dpF(2f) else dpF(1.6f)
        paint.color = if (pressedCenter) {
            LED_BLUE_BRIGHT
        } else {
            Color.rgb(43, 54, 66)
        }
        canvas.drawOval(centerRect, paint)

        val centerMarkRect = RectF(
            dpadCx - centerRx * 0.18f,
            dpadCy - centerRy * 0.17f,
            dpadCx + centerRx * 0.18f,
            dpadCy + centerRy * 0.17f
        )

        paint.style = Paint.Style.FILL
        paint.color = if (pressedCenter) {
            Color.rgb(220, 244, 255)
        } else {
            Color.rgb(93, 107, 122)
        }
        canvas.drawOval(centerMarkRect, paint)

        Direction.entries.forEach { direction ->
            drawRoundedIndicator(
                canvas = canvas,
                direction = direction,
                pressed = pressedDirection == direction
            )
        }

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = dpF(1.6f)
        paint.color = Color.rgb(27, 35, 44)
        canvas.drawOval(
            RectF(
                dpadCx - dpadRx,
                dpadCy - dpadRy,
                dpadCx + dpadRx,
                dpadCy + dpadRy
            ),
            paint
        )
    }

    private fun drawRingSegment(
        canvas: Canvas,
        direction: Direction,
        pressed: Boolean
    ) {
        val startAngle = when (direction) {
            Direction.UP -> 228f
            Direction.RIGHT -> 318f
            Direction.DOWN -> 48f
            Direction.LEFT -> 138f
        }

        val sweep = 84f

        val outerRect = RectF(
            dpadCx - dpadRx,
            dpadCy - dpadRy,
            dpadCx + dpadRx,
            dpadCy + dpadRy
        )

        val innerRect = RectF(
            dpadCx - ringInnerRx,
            dpadCy - ringInnerRy,
            dpadCx + ringInnerRx,
            dpadCy + ringInnerRy
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

    }

    /**
     * 사진의 방향 표시는 날카로운 삼각형이 아니라 둥근 작은 버튼/슬롯처럼
     * 보여서, 방향에 따라 가로 또는 세로로 긴 캡슐 모양으로 그린다.
     */
    private fun drawRoundedIndicator(
        canvas: Canvas,
        direction: Direction,
        pressed: Boolean
    ) {
        val distanceX = dpadRx * 0.66f
        val distanceY = dpadRy * 0.66f

        val cx = when (direction) {
            Direction.LEFT -> dpadCx - distanceX
            Direction.RIGHT -> dpadCx + distanceX
            else -> dpadCx
        }

        val cy = when (direction) {
            Direction.UP -> dpadCy - distanceY
            Direction.DOWN -> dpadCy + distanceY
            else -> dpadCy
        }

        val horizontal =
            direction == Direction.LEFT ||
                direction == Direction.RIGHT

        val halfW = if (horizontal) dpF(5.5f) else dpF(10f)
        val halfH = if (horizontal) dpF(10f) else dpF(5.5f)

        val rect = RectF(
            cx - halfW,
            cy - halfH,
            cx + halfW,
            cy + halfH
        )

        if (pressed) {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = dpF(5f)
            paint.color = Color.argb(78, 0, 151, 255)
            canvas.drawRoundRect(
                rect,
                dpF(8),
                dpF(8),
                paint
            )
        }

        paint.style = Paint.Style.FILL
        paint.color = if (pressed) {
            Color.rgb(218, 244, 255)
        } else {
            Color.rgb(207, 217, 227)
        }
        canvas.drawRoundRect(
            rect,
            dpF(8),
            dpF(8),
            paint
        )

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = dpF(1f)
        paint.color = if (pressed) {
            LED_BLUE_PALE
        } else {
            Color.rgb(64, 75, 88)
        }
        canvas.drawRoundRect(
            rect,
            dpF(8),
            dpF(8),
            paint
        )

        val inset = dpF(2f)
        val inner = RectF(
            rect.left + inset,
            rect.top + inset,
            rect.right - inset,
            rect.bottom - inset
        )

        paint.style = Paint.Style.FILL
        paint.color = if (pressed) {
            Color.rgb(115, 211, 255)
        } else {
            Color.rgb(238, 242, 246)
        }
        canvas.drawRoundRect(
            inner,
            dpF(6),
            dpF(6),
            paint
        )
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
