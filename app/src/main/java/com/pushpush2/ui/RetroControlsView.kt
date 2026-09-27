package com.pushpush2.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import com.pushpush2.game.Direction
import kotlin.math.abs
import kotlin.math.min

/**
 * Bottom touch controls rebuilt around the user's Motorola-style keypad
 * reference.
 *
 * Functional mapping:
 * - upper-left soft key: 스테이지
 * - upper-right soft key: 리셋
 * - circular ring: UP / DOWN / LEFT / RIGHT
 * - NATE center position: 확인
 * - lower-center key: 돌아가기 (one-step undo)
 * - lower-left key: decorative only
 * - lower-right key: exit
 *
 * Touch areas remain independent from the visual styling so controls can stay
 * generous without creating accidental neighbouring presses.
 */
class RetroControlsView(context: Context) : View(context) {

    var onDirection: ((Direction) -> Unit)? = null
    var onStageClick: (() -> Unit)? = null
    var onRetryClick: (() -> Unit)? = null
    var onCenterClick: (() -> Unit)? = null
    var onUndoClick: (() -> Unit)? = null
    var onExitClick: (() -> Unit)? = null

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }

    private val shellRect = RectF()
    private val stageRect = RectF()
    private val resetRect = RectF()
    private val navRect = RectF()
    private val okRect = RectF()
    private val undoRect = RectF()
    private val leftDecorRect = RectF()
    private val exitRect = RectF()

    private var controlScale = 1f
    private var controlOffsetY = 0f

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
        val desiredHeight = dp(BASE_CONTROL_HEIGHT_DP)

        val height = when (MeasureSpec.getMode(heightMeasureSpec)) {
            MeasureSpec.EXACTLY ->
                MeasureSpec.getSize(heightMeasureSpec)

            MeasureSpec.AT_MOST ->
                min(
                    desiredHeight,
                    MeasureSpec.getSize(heightMeasureSpec)
                )

            else -> desiredHeight
        }.coerceAtLeast(1)

        setMeasuredDimension(width, height)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()

        controlScale =
            min(
                1f,
                h / dpF(BASE_CONTROL_HEIGHT_DP.toFloat())
            ).coerceAtLeast(MIN_CONTROL_SCALE)

        val scaledHeight =
            dpF(BASE_CONTROL_HEIGHT_DP.toFloat()) * controlScale

        controlOffsetY =
            ((h - scaledHeight) / 2f).coerceAtLeast(0f)

        calculateGeometry(w, scaledHeight)

        drawHousing(canvas)
        drawTopSoftKey(
            canvas = canvas,
            rect = stageRect,
            label = "스테이지",
            pressed = pressedSoftKey == SoftKey.STAGE
        )
        drawTopSoftKey(
            canvas = canvas,
            rect = resetRect,
            label = "리셋",
            pressed = pressedSoftKey == SoftKey.RESET
        )
        drawCircularNavigation(canvas)
        drawBottomKeys(canvas)
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

                    resetRect.contains(event.x, event.y) -> {
                        pressedSoftKey = SoftKey.RESET
                        invalidate()
                    }

                    undoRect.contains(event.x, event.y) -> {
                        pressedSoftKey = SoftKey.UNDO
                        performHapticFeedback(
                            HapticFeedbackConstants.KEYBOARD_TAP
                        )
                        invalidate()
                    }

                    exitRect.contains(event.x, event.y) -> {
                        pressedSoftKey = SoftKey.EXIT
                        performHapticFeedback(
                            HapticFeedbackConstants.KEYBOARD_TAP
                        )
                        invalidate()
                    }

                    centerAt(event.x, event.y) -> {
                        pressedCenter = true
                        performHapticFeedback(
                            HapticFeedbackConstants.KEYBOARD_TAP
                        )
                        invalidate()
                    }

                    else -> {
                        directionAt(event.x, event.y)
                            ?.let(::pressDirection)
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
                        SoftKey.STAGE ->
                            stageRect.contains(event.x, event.y)

                        SoftKey.RESET ->
                            resetRect.contains(event.x, event.y)

                        SoftKey.UNDO ->
                            undoRect.contains(event.x, event.y)

                        SoftKey.EXIT ->
                            exitRect.contains(event.x, event.y)

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

                if (
                    pressedCenter &&
                    centerAt(event.x, event.y)
                ) {
                    onCenterClick?.invoke()
                }

                when (pressedSoftKey) {
                    SoftKey.STAGE -> {
                        if (stageRect.contains(event.x, event.y)) {
                            performHapticFeedback(
                                HapticFeedbackConstants.KEYBOARD_TAP
                            )
                            onStageClick?.invoke()
                        }
                    }

                    SoftKey.RESET -> {
                        if (resetRect.contains(event.x, event.y)) {
                            performHapticFeedback(
                                HapticFeedbackConstants.KEYBOARD_TAP
                            )
                            onRetryClick?.invoke()
                        }
                    }

                    SoftKey.UNDO -> {
                        if (undoRect.contains(event.x, event.y)) {
                            performHapticFeedback(
                                HapticFeedbackConstants.KEYBOARD_TAP
                            )
                            onUndoClick?.invoke()
                        }
                    }

                    SoftKey.EXIT -> {
                        if (exitRect.contains(event.x, event.y)) {
                            performHapticFeedback(
                                HapticFeedbackConstants.KEYBOARD_TAP
                            )
                            onExitClick?.invoke()
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

    private fun calculateGeometry(
        widthPx: Float,
        scaledHeight: Float
    ) {
        val side = scaledDp(4f)
        val top = controlOffsetY + scaledDp(3f)
        val bottom =
            controlOffsetY +
                scaledHeight -
                scaledDp(3f)

        shellRect.set(
            side,
            top,
            widthPx - side,
            bottom
        )

        val sw = shellRect.width()
        val sh = shellRect.height()

        /*
         * Layout reset from the supplied Motorola keypad reference:
         *
         * [ 스테이지 ─ ]      [ ─ 리셋 ]
         *
         *              ○
         *        ←    확인    →
         *              ↓
         *
         * [ decorative ]  [ 돌아가기 ]  [ 종료 ]
         */
        stageRect.set(
            shellRect.left + sw * 0.055f,
            shellRect.top + sh * 0.055f,
            shellRect.left + sw * 0.31f,
            shellRect.top + sh * 0.225f
        )

        resetRect.set(
            shellRect.right - sw * 0.31f,
            shellRect.top + sh * 0.055f,
            shellRect.right - sw * 0.055f,
            shellRect.top + sh * 0.225f
        )

        val navSize = min(sw * 0.50f, sh * 0.58f)
        val navCx = shellRect.centerX()
        val navCy = shellRect.top + sh * 0.45f

        navRect.set(
            navCx - navSize / 2f,
            navCy - navSize / 2f,
            navCx + navSize / 2f,
            navCy + navSize / 2f
        )

        val okWidth = navRect.width() * 0.42f
        val okHeight = navRect.height() * 0.28f
        okRect.set(
            navRect.centerX() - okWidth / 2f,
            navRect.centerY() - okHeight / 2f,
            navRect.centerX() + okWidth / 2f,
            navRect.centerY() + okHeight / 2f
        )

        leftDecorRect.set(
            shellRect.left + sw * 0.055f,
            shellRect.top + sh * 0.73f,
            shellRect.left + sw * 0.23f,
            shellRect.bottom - sh * 0.055f
        )

        undoRect.set(
            shellRect.left + sw * 0.35f,
            shellRect.top + sh * 0.76f,
            shellRect.right - sw * 0.35f,
            shellRect.bottom - sh * 0.055f
        )

        exitRect.set(
            shellRect.right - sw * 0.23f,
            shellRect.top + sh * 0.73f,
            shellRect.right - sw * 0.055f,
            shellRect.bottom - sh * 0.055f
        )
    }

    private fun pressDirection(direction: Direction) {
        if (pressedDirection == direction) return

        pressedDirection = direction

        performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        onDirection?.invoke(direction)

        removeCallbacks(repeatRunnable)
        postDelayed(
            repeatRunnable,
            INITIAL_REPEAT_DELAY_MS
        )

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
    ): Boolean =
        okRect.contains(x, y)

    private fun directionAt(
        x: Float,
        y: Float
    ): Direction? {
        val radius = navRect.width() / 2f
        val dx = x - navRect.centerX()
        val dy = y - navRect.centerY()

        if (dx * dx + dy * dy > radius * radius) return null
        if (okRect.contains(x, y)) return null

        return if (abs(dx) > abs(dy)) {
            if (dx < 0f) {
                Direction.LEFT
            } else {
                Direction.RIGHT
            }
        } else {
            if (dy < 0f) {
                Direction.UP
            } else {
                Direction.DOWN
            }
        }
    }

    private fun drawHousing(canvas: Canvas) {
        paint.style = Paint.Style.FILL
        paint.color = HOUSING_SHADOW

        val shadow = RectF(
            shellRect.left,
            shellRect.top + scaledDp(2f),
            shellRect.right,
            shellRect.bottom + scaledDp(2f)
        )

        canvas.drawRoundRect(
            shadow,
            scaledDp(26f),
            scaledDp(26f),
            paint
        )

        paint.color = HOUSING_BASE
        canvas.drawRoundRect(
            shellRect,
            scaledDp(26f),
            scaledDp(26f),
            paint
        )

        val inner = RectF(
            shellRect.left + scaledDp(3f),
            shellRect.top + scaledDp(3f),
            shellRect.right - scaledDp(3f),
            shellRect.bottom - scaledDp(3f)
        )

        paint.color = HOUSING_INNER
        canvas.drawRoundRect(
            inner,
            scaledDp(23f),
            scaledDp(23f),
            paint
        )

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = scaledDp(1.1f)
        paint.color = HOUSING_BORDER
        canvas.drawRoundRect(
            shellRect,
            scaledDp(26f),
            scaledDp(26f),
            paint
        )
    }

    private fun drawTopSoftKey(
        canvas: Canvas,
        rect: RectF,
        label: String,
        pressed: Boolean
    ) {
        if (pressed) {
            paint.style = Paint.Style.FILL
            paint.color = Color.argb(38, 94, 123, 148)
            canvas.drawRoundRect(
                rect,
                scaledDp(8f),
                scaledDp(8f),
                paint
            )
        }

        val leftSide = rect.centerX() < shellRect.centerX()

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = scaledDp(1.4f)
        paint.strokeCap = Paint.Cap.ROUND
        paint.color = if (pressed) ACCENT_LIGHT else SOFTKEY_MARK

        val barY = rect.top + rect.height() * 0.25f
        val barStart: Float
        val barEnd: Float

        if (leftSide) {
            barStart = rect.left + rect.width() * 0.05f
            barEnd = rect.left + rect.width() * 0.48f
        } else {
            barStart = rect.right - rect.width() * 0.48f
            barEnd = rect.right - rect.width() * 0.05f
        }

        canvas.drawLine(
            barStart,
            barY,
            barEnd,
            barY,
            paint
        )

        textPaint.textSize = scaledDp(10f)
        textPaint.color =
            if (pressed) Color.WHITE else TEXT_PRIMARY

        val textY = rect.top + rect.height() * 0.70f
        val baseline =
            textY -
                (textPaint.descent() + textPaint.ascent()) / 2f

        canvas.drawText(
            label,
            rect.centerX(),
            baseline,
            textPaint
        )
    }

    private fun drawCircularNavigation(canvas: Canvas) {
        val cx = navRect.centerX()
        val cy = navRect.centerY()
        val radius = navRect.width() / 2f

        paint.style = Paint.Style.FILL
        paint.color = NAV_SHADOW
        canvas.drawCircle(
            cx,
            cy + scaledDp(2f),
            radius + scaledDp(2f),
            paint
        )

        paint.color = NAV_RING
        canvas.drawCircle(
            cx,
            cy,
            radius,
            paint
        )

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = scaledDp(2.2f)
        paint.color = NAV_BORDER
        canvas.drawCircle(
            cx,
            cy,
            radius,
            paint
        )

        paint.strokeWidth = scaledDp(1f)
        paint.color = NAV_INNER_RIM
        canvas.drawCircle(
            cx,
            cy,
            radius * 0.82f,
            paint
        )

        drawDirectionHighlight(canvas)

        drawDirectionIcon(
            canvas,
            Direction.UP,
            cx,
            cy - radius * 0.62f
        )
        drawDirectionIcon(
            canvas,
            Direction.DOWN,
            cx,
            cy + radius * 0.62f
        )
        drawDirectionIcon(
            canvas,
            Direction.LEFT,
            cx - radius * 0.62f,
            cy
        )
        drawDirectionIcon(
            canvas,
            Direction.RIGHT,
            cx + radius * 0.62f,
            cy
        )

        if (pressedCenter) {
            paint.style = Paint.Style.FILL
            paint.color = CENTER_GLOW
            canvas.drawOval(
                RectF(
                    okRect.left - scaledDp(3f),
                    okRect.top - scaledDp(3f),
                    okRect.right + scaledDp(3f),
                    okRect.bottom + scaledDp(3f)
                ),
                paint
            )
        }

        paint.style = Paint.Style.FILL
        paint.color =
            if (pressedCenter) CENTER_PRESSED else CENTER_FILL
        canvas.drawOval(okRect, paint)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = scaledDp(1.2f)
        paint.color = CENTER_BORDER
        canvas.drawOval(okRect, paint)

        textPaint.textSize = scaledDp(12.5f)
        textPaint.color = Color.WHITE

        val baseline =
            okRect.centerY() -
                (textPaint.descent() + textPaint.ascent()) / 2f

        canvas.drawText(
            "확인",
            okRect.centerX(),
            baseline,
            textPaint
        )
    }

    private fun drawDirectionHighlight(canvas: Canvas) {
        val direction = pressedDirection ?: return
        val cx = navRect.centerX()
        val cy = navRect.centerY()
        val radius = navRect.width() / 2f

        val hx: Float
        val hy: Float

        when (direction) {
            Direction.UP -> {
                hx = cx
                hy = cy - radius * 0.62f
            }

            Direction.DOWN -> {
                hx = cx
                hy = cy + radius * 0.62f
            }

            Direction.LEFT -> {
                hx = cx - radius * 0.62f
                hy = cy
            }

            Direction.RIGHT -> {
                hx = cx + radius * 0.62f
                hy = cy
            }
        }

        paint.style = Paint.Style.FILL
        paint.color = NAV_PRESSED
        canvas.drawCircle(
            hx,
            hy,
            radius * 0.18f,
            paint
        )
    }

    private fun drawDirectionIcon(
        canvas: Canvas,
        direction: Direction,
        cx: Float,
        cy: Float
    ) {
        val pressed = pressedDirection == direction
        val size = scaledDp(7.5f)

        val path = Path().apply {
            when (direction) {
                Direction.UP -> {
                    moveTo(cx, cy - size)
                    lineTo(cx - size, cy + size * 0.65f)
                    lineTo(cx + size, cy + size * 0.65f)
                }

                Direction.DOWN -> {
                    moveTo(cx, cy + size)
                    lineTo(cx - size, cy - size * 0.65f)
                    lineTo(cx + size, cy - size * 0.65f)
                }

                Direction.LEFT -> {
                    moveTo(cx - size, cy)
                    lineTo(cx + size * 0.65f, cy - size)
                    lineTo(cx + size * 0.65f, cy + size)
                }

                Direction.RIGHT -> {
                    moveTo(cx + size, cy)
                    lineTo(cx - size * 0.65f, cy - size)
                    lineTo(cx - size * 0.65f, cy + size)
                }
            }
            close()
        }

        paint.style = Paint.Style.FILL
        paint.color =
            if (pressed) Color.WHITE else NAV_ICON

        canvas.drawPath(path, paint)
    }

    private fun drawBottomKeys(canvas: Canvas) {
        drawDecorativeLeftKey(canvas)
        drawUndoKey(canvas)
        drawExitKey(canvas)
    }

    private fun drawDecorativeLeftKey(canvas: Canvas) {
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = scaledDp(1.4f)
        paint.color = DECOR_ICON

        val cx = leftDecorRect.centerX()
        val cy = leftDecorRect.centerY()
        val w = leftDecorRect.width() * 0.32f
        val h = leftDecorRect.height() * 0.22f

        val iconRect = RectF(
            cx - w,
            cy - h,
            cx + w,
            cy + h
        )

        canvas.drawRoundRect(
            iconRect,
            scaledDp(2f),
            scaledDp(2f),
            paint
        )

        canvas.drawLine(
            iconRect.left - scaledDp(5f),
            cy,
            iconRect.left,
            cy,
            paint
        )
    }

    private fun drawUndoKey(canvas: Canvas) {
        val pressed = pressedSoftKey == SoftKey.UNDO

        if (pressed) {
            paint.style = Paint.Style.FILL
            paint.color = Color.argb(42, 94, 123, 148)
            canvas.drawRoundRect(
                undoRect,
                scaledDp(7f),
                scaledDp(7f),
                paint
            )
        }

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = scaledDp(1.2f)
        paint.color =
            if (pressed) ACCENT_LIGHT else SOFTKEY_MARK

        val y = undoRect.top + undoRect.height() * 0.22f
        canvas.drawLine(
            undoRect.left + undoRect.width() * 0.28f,
            y,
            undoRect.right - undoRect.width() * 0.28f,
            y,
            paint
        )

        textPaint.textSize = scaledDp(9.5f)
        textPaint.color =
            if (pressed) Color.WHITE else TEXT_PRIMARY

        val baseline =
            undoRect.centerY() + undoRect.height() * 0.16f -
                (textPaint.descent() + textPaint.ascent()) / 2f

        canvas.drawText(
            "돌아가기",
            undoRect.centerX(),
            baseline,
            textPaint
        )
    }

    private fun drawExitKey(canvas: Canvas) {
        val pressed = pressedSoftKey == SoftKey.EXIT

        if (pressed) {
            paint.style = Paint.Style.FILL
            paint.color = Color.argb(50, 184, 63, 63)
            canvas.drawRoundRect(
                exitRect,
                scaledDp(7f),
                scaledDp(7f),
                paint
            )
        }

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = scaledDp(4f)
        paint.strokeCap = Paint.Cap.ROUND
        paint.color = END_RED

        canvas.drawArc(
            RectF(
                exitRect.centerX() - scaledDp(17f),
                exitRect.centerY() - scaledDp(6f),
                exitRect.centerX() + scaledDp(17f),
                exitRect.centerY() + scaledDp(15f)
            ),
            205f,
            130f,
            false,
            paint
        )
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun dpF(value: Float): Float =
        value * resources.displayMetrics.density

    private fun scaledDp(value: Float): Float =
        dpF(value) * controlScale

    private enum class SoftKey {
        STAGE,
        RESET,
        UNDO,
        EXIT
    }

    private companion object {
        const val INITIAL_REPEAT_DELAY_MS = 280L
        const val REPEAT_INTERVAL_MS = 110L
        const val BASE_CONTROL_HEIGHT_DP = 248
        const val MIN_CONTROL_SCALE = 0.58f

        val HOUSING_BASE: Int = Color.rgb(74, 94, 111)
        val HOUSING_INNER: Int = Color.rgb(84, 104, 121)
        val HOUSING_SHADOW: Int = Color.rgb(42, 55, 67)
        val HOUSING_BORDER: Int = Color.rgb(128, 145, 157)

        val SOFTKEY_MARK: Int = Color.rgb(200, 211, 217)
        val TEXT_PRIMARY: Int = Color.rgb(232, 238, 241)
        val ACCENT_LIGHT: Int = Color.rgb(224, 238, 245)

        val NAV_RING: Int = Color.rgb(47, 69, 85)
        val NAV_BORDER: Int = Color.rgb(161, 179, 191)
        val NAV_INNER_RIM: Int = Color.rgb(101, 123, 139)
        val NAV_SHADOW: Int = Color.rgb(28, 38, 47)
        val NAV_PRESSED: Int = Color.rgb(70, 100, 126)
        val NAV_ICON: Int = Color.rgb(220, 230, 236)

        val CENTER_FILL: Int = Color.rgb(55, 63, 66)
        val CENTER_PRESSED: Int = Color.rgb(87, 105, 116)
        val CENTER_BORDER: Int = Color.rgb(188, 202, 210)
        val CENTER_GLOW: Int = Color.argb(80, 190, 220, 236)

        val DECOR_ICON: Int = Color.rgb(205, 218, 225)
        val END_RED: Int = Color.rgb(193, 62, 61)
    }
}
