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
 * Bottom controls rebuilt from the user's silver feature-phone keypad reference.
 *
 * Functional mapping:
 * - upper-left key: 스테이지
 * - upper-right key: 리셋
 * - center NATE position: 확인
 * - four areas around center: UP / DOWN / LEFT / RIGHT
 * - lower-left alarm position: 돌아가기 (one-step undo)
 * - lower-right end key: exit
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
        drawStageResetKeys(canvas)
        drawNavigationCluster(canvas)
        drawUndoKey(canvas)
        drawExitKey(canvas)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)

                when {
                    stageRect.contains(event.x, event.y) -> {
                        pressedSoftKey = SoftKey.STAGE
                        performHapticFeedback(
                            HapticFeedbackConstants.KEYBOARD_TAP
                        )
                        invalidate()
                    }

                    resetRect.contains(event.x, event.y) -> {
                        pressedSoftKey = SoftKey.RESET
                        performHapticFeedback(
                            HapticFeedbackConstants.KEYBOARD_TAP
                        )
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
                            onStageClick?.invoke()
                        }
                    }

                    SoftKey.RESET -> {
                        if (resetRect.contains(event.x, event.y)) {
                            onRetryClick?.invoke()
                        }
                    }

                    SoftKey.UNDO -> {
                        if (undoRect.contains(event.x, event.y)) {
                            onUndoClick?.invoke()
                        }
                    }

                    SoftKey.EXIT -> {
                        if (exitRect.contains(event.x, event.y)) {
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

        // Top soft keys replace the original left/right dash keys.
        stageRect.set(
            shellRect.left + sw * 0.055f,
            shellRect.top + sh * 0.07f,
            shellRect.left + sw * 0.31f,
            shellRect.top + sh * 0.245f
        )

        resetRect.set(
            shellRect.right - sw * 0.31f,
            shellRect.top + sh * 0.07f,
            shellRect.right - sw * 0.055f,
            shellRect.top + sh * 0.245f
        )

        // Central four-way cluster based on the NATE keypad area.
        val navWidth = sw * 0.46f
        val navHeight = sh * 0.49f
        val navCx = shellRect.centerX()
        val navCy = shellRect.top + sh * 0.46f

        navRect.set(
            navCx - navWidth / 2f,
            navCy - navHeight / 2f,
            navCx + navWidth / 2f,
            navCy + navHeight / 2f
        )

        val okWidth = navRect.width() * 0.42f
        val okHeight = navRect.height() * 0.31f

        okRect.set(
            navRect.centerX() - okWidth / 2f,
            navRect.centerY() - okHeight / 2f,
            navRect.centerX() + okWidth / 2f,
            navRect.centerY() + okHeight / 2f
        )

        // Original alarm position -> one-step undo.
        undoRect.set(
            shellRect.left + sw * 0.055f,
            shellRect.top + sh * 0.74f,
            shellRect.left + sw * 0.31f,
            shellRect.bottom - sh * 0.055f
        )

        // Original end key keeps the current exit behaviour.
        exitRect.set(
            shellRect.right - sw * 0.31f,
            shellRect.top + sh * 0.74f,
            shellRect.right - sw * 0.055f,
            shellRect.bottom - sh * 0.055f
        )
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
        if (!navRect.contains(x, y)) return null
        if (okRect.contains(x, y)) return null

        val dx =
            (x - navRect.centerX()) /
                (navRect.width() / 2f)

        val dy =
            (y - navRect.centerY()) /
                (navRect.height() / 2f)

        return if (abs(dx) > abs(dy)) {
            if (dx < 0f) Direction.LEFT else Direction.RIGHT
        } else {
            if (dy < 0f) Direction.UP else Direction.DOWN
        }
    }

    private fun pressDirection(direction: Direction) {
        if (pressedDirection == direction) return

        pressedDirection = direction

        performHapticFeedback(
            HapticFeedbackConstants.KEYBOARD_TAP
        )
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
            scaledDp(24f),
            scaledDp(24f),
            paint
        )

        paint.color = HOUSING_BASE
        canvas.drawRoundRect(
            shellRect,
            scaledDp(24f),
            scaledDp(24f),
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
            scaledDp(21f),
            scaledDp(21f),
            paint
        )

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = scaledDp(1.1f)
        paint.color = HOUSING_BORDER
        canvas.drawRoundRect(
            shellRect,
            scaledDp(24f),
            scaledDp(24f),
            paint
        )
    }

    private fun drawStageResetKeys(canvas: Canvas) {
        drawTopKey(
            canvas = canvas,
            rect = stageRect,
            label = "스테이지",
            pressed = pressedSoftKey == SoftKey.STAGE
        )

        drawTopKey(
            canvas = canvas,
            rect = resetRect,
            label = "리셋",
            pressed = pressedSoftKey == SoftKey.RESET
        )
    }

    private fun drawTopKey(
        canvas: Canvas,
        rect: RectF,
        label: String,
        pressed: Boolean
    ) {
        if (pressed) {
            paint.style = Paint.Style.FILL
            paint.color = KEY_PRESSED
            canvas.drawRoundRect(
                rect,
                scaledDp(8f),
                scaledDp(8f),
                paint
            )
        }

        textPaint.textSize = scaledDp(10.5f)
        textPaint.color =
            if (pressed) Color.WHITE else TEXT_PRIMARY

        val baseline =
            rect.centerY() -
                (textPaint.descent() + textPaint.ascent()) / 2f

        canvas.drawText(
            label,
            rect.centerX(),
            baseline,
            textPaint
        )
    }

    private fun drawNavigationCluster(canvas: Canvas) {
        paint.style = Paint.Style.FILL
        paint.color = NAV_SHADOW

        val shadow = RectF(
            navRect.left,
            navRect.top + scaledDp(2f),
            navRect.right,
            navRect.bottom + scaledDp(2f)
        )

        canvas.drawRoundRect(
            shadow,
            scaledDp(23f),
            scaledDp(23f),
            paint
        )

        paint.color = NAV_FACE
        canvas.drawRoundRect(
            navRect,
            scaledDp(23f),
            scaledDp(23f),
            paint
        )

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = scaledDp(1.5f)
        paint.color = NAV_BORDER
        canvas.drawRoundRect(
            navRect,
            scaledDp(23f),
            scaledDp(23f),
            paint
        )

        drawDirectionHighlight(canvas)

        val xOffset = navRect.width() * 0.35f
        val yOffset = navRect.height() * 0.34f

        drawDirectionIcon(
            canvas,
            Direction.UP,
            navRect.centerX(),
            navRect.centerY() - yOffset
        )
        drawDirectionIcon(
            canvas,
            Direction.DOWN,
            navRect.centerX(),
            navRect.centerY() + yOffset
        )
        drawDirectionIcon(
            canvas,
            Direction.LEFT,
            navRect.centerX() - xOffset,
            navRect.centerY()
        )
        drawDirectionIcon(
            canvas,
            Direction.RIGHT,
            navRect.centerX() + xOffset,
            navRect.centerY()
        )

        if (pressedCenter) {
            paint.style = Paint.Style.FILL
            paint.color = CENTER_GLOW
            canvas.drawRoundRect(
                RectF(
                    okRect.left - scaledDp(3f),
                    okRect.top - scaledDp(3f),
                    okRect.right + scaledDp(3f),
                    okRect.bottom + scaledDp(3f)
                ),
                scaledDp(9f),
                scaledDp(9f),
                paint
            )
        }

        paint.style = Paint.Style.FILL
        paint.color =
            if (pressedCenter) CENTER_PRESSED else CENTER_FILL
        canvas.drawRoundRect(
            okRect,
            scaledDp(8f),
            scaledDp(8f),
            paint
        )

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = scaledDp(1.1f)
        paint.color = CENTER_BORDER
        canvas.drawRoundRect(
            okRect,
            scaledDp(8f),
            scaledDp(8f),
            paint
        )

        textPaint.textSize = scaledDp(12f)
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

        val xOffset = navRect.width() * 0.35f
        val yOffset = navRect.height() * 0.34f

        val cx: Float
        val cy: Float

        when (direction) {
            Direction.UP -> {
                cx = navRect.centerX()
                cy = navRect.centerY() - yOffset
            }

            Direction.DOWN -> {
                cx = navRect.centerX()
                cy = navRect.centerY() + yOffset
            }

            Direction.LEFT -> {
                cx = navRect.centerX() - xOffset
                cy = navRect.centerY()
            }

            Direction.RIGHT -> {
                cx = navRect.centerX() + xOffset
                cy = navRect.centerY()
            }
        }

        paint.style = Paint.Style.FILL
        paint.color = NAV_PRESSED

        canvas.drawCircle(
            cx,
            cy,
            min(navRect.width(), navRect.height()) * 0.095f,
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
                    lineTo(cx - size, cy + size * 0.62f)
                    lineTo(cx + size, cy + size * 0.62f)
                }

                Direction.DOWN -> {
                    moveTo(cx, cy + size)
                    lineTo(cx - size, cy - size * 0.62f)
                    lineTo(cx + size, cy - size * 0.62f)
                }

                Direction.LEFT -> {
                    moveTo(cx - size, cy)
                    lineTo(cx + size * 0.62f, cy - size)
                    lineTo(cx + size * 0.62f, cy + size)
                }

                Direction.RIGHT -> {
                    moveTo(cx + size, cy)
                    lineTo(cx - size * 0.62f, cy - size)
                    lineTo(cx - size * 0.62f, cy + size)
                }
            }
            close()
        }

        paint.style = Paint.Style.FILL
        paint.color =
            if (pressed) Color.WHITE else NAV_ICON

        canvas.drawPath(path, paint)
    }

    private fun drawUndoKey(canvas: Canvas) {
        val pressed = pressedSoftKey == SoftKey.UNDO

        if (pressed) {
            paint.style = Paint.Style.FILL
            paint.color = KEY_PRESSED
            canvas.drawRoundRect(
                undoRect,
                scaledDp(8f),
                scaledDp(8f),
                paint
            )
        }

        textPaint.textSize = scaledDp(10.5f)
        textPaint.color =
            if (pressed) Color.WHITE else TEXT_PRIMARY

        val baseline =
            undoRect.centerY() -
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
                scaledDp(8f),
                scaledDp(8f),
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

        val HOUSING_BASE: Int = Color.rgb(215, 213, 218)
        val HOUSING_INNER: Int = Color.rgb(229, 227, 231)
        val HOUSING_SHADOW: Int = Color.rgb(166, 163, 169)
        val HOUSING_BORDER: Int = Color.rgb(150, 146, 153)

        val TEXT_PRIMARY: Int = Color.rgb(78, 76, 82)
        val KEY_PRESSED: Int = Color.argb(70, 103, 94, 113)

        val NAV_FACE: Int = Color.rgb(130, 117, 136)
        val NAV_BORDER: Int = Color.rgb(91, 82, 98)
        val NAV_SHADOW: Int = Color.rgb(104, 95, 111)
        val NAV_PRESSED: Int = Color.rgb(105, 93, 116)
        val NAV_ICON: Int = Color.rgb(236, 232, 240)

        val CENTER_FILL: Int = Color.rgb(219, 214, 223)
        val CENTER_PRESSED: Int = Color.rgb(193, 185, 201)
        val CENTER_BORDER: Int = Color.rgb(96, 87, 103)
        val CENTER_GLOW: Int = Color.argb(90, 255, 255, 255)

        val END_RED: Int = Color.rgb(190, 58, 57)
    }
}
