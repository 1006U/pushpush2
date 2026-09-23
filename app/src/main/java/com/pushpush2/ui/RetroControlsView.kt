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
 * Bottom touch controls styled after the user's feature-phone keypad reference.
 *
 * Functional layout:
 * - upper-left key: STAGE
 * - upper-right key: RESET
 * - center blue navigation pad: UP / DOWN / LEFT / RIGHT
 * - center key: OK
 *
 * The remaining lower phone keys are visual only. All functional hit areas
 * scale together with the available height so the controls remain usable
 * across all 66 stages.
 */
class RetroControlsView(context: Context) : View(context) {

    var onDirection: ((Direction) -> Unit)? = null
    var onStageClick: (() -> Unit)? = null
    var onRetryClick: (() -> Unit)? = null
    var onCenterClick: (() -> Unit)? = null
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
    private val topDecorRect = RectF()
    private val bottomCancelRect = RectF()
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

        drawPhoneHousing(canvas)
        drawTopDecorativeKey(canvas)
        drawSoftKey(
            canvas = canvas,
            rect = stageRect,
            label = "STAGE",
            pressed = pressedSoftKey == SoftKey.STAGE
        )
        drawSoftKey(
            canvas = canvas,
            rect = resetRect,
            label = "RESET",
            pressed = pressedSoftKey == SoftKey.RESET
        )
        drawNavigationPad(canvas)
        drawDecorativeBottomKeys(canvas)
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

        topDecorRect.set(
            shellRect.left + sw * 0.35f,
            shellRect.top + sh * 0.025f,
            shellRect.right - sw * 0.35f,
            shellRect.top + sh * 0.17f
        )

        stageRect.set(
            shellRect.left + sw * 0.035f,
            shellRect.top + sh * 0.10f,
            shellRect.left + sw * 0.275f,
            shellRect.top + sh * 0.405f
        )

        resetRect.set(
            shellRect.right - sw * 0.275f,
            shellRect.top + sh * 0.10f,
            shellRect.right - sw * 0.035f,
            shellRect.top + sh * 0.405f
        )

        navRect.set(
            shellRect.left + sw * 0.255f,
            shellRect.top + sh * 0.185f,
            shellRect.right - sw * 0.255f,
            shellRect.top + sh * 0.715f
        )

        okRect.set(
            navRect.left + navRect.width() * 0.255f,
            navRect.top + navRect.height() * 0.29f,
            navRect.right - navRect.width() * 0.255f,
            navRect.bottom - navRect.height() * 0.29f
        )

        bottomCancelRect.set(
            shellRect.left + sw * 0.35f,
            shellRect.top + sh * 0.78f,
            shellRect.right - sw * 0.35f,
            shellRect.bottom - sh * 0.035f
        )

        exitRect.set(
            shellRect.right - sw * 0.30f,
            shellRect.top + sh * 0.68f,
            shellRect.right - sw * 0.04f,
            shellRect.bottom - sh * 0.035f
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
        if (!navRect.contains(x, y)) return null
        if (okRect.contains(x, y)) return null

        val dx =
            (x - navRect.centerX()) /
                (navRect.width() / 2f)
        val dy =
            (y - navRect.centerY()) /
                (navRect.height() / 2f)

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

    private fun drawPhoneHousing(canvas: Canvas) {
        paint.style = Paint.Style.FILL
        paint.color = SHELL_SHADOW

        val shadow = RectF(
            shellRect.left,
            shellRect.top + scaledDp(2f),
            shellRect.right,
            shellRect.bottom + scaledDp(2f)
        )

        canvas.drawRoundRect(
            shadow,
            scaledDp(32f),
            scaledDp(32f),
            paint
        )

        paint.color = SHELL_BASE
        canvas.drawRoundRect(
            shellRect,
            scaledDp(32f),
            scaledDp(32f),
            paint
        )

        val inner = RectF(
            shellRect.left + scaledDp(3f),
            shellRect.top + scaledDp(3f),
            shellRect.right - scaledDp(3f),
            shellRect.bottom - scaledDp(3f)
        )

        paint.color = SHELL_INNER
        canvas.drawRoundRect(
            inner,
            scaledDp(29f),
            scaledDp(29f),
            paint
        )

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = scaledDp(1.5f)
        paint.color = SHELL_BORDER
        canvas.drawRoundRect(
            shellRect,
            scaledDp(32f),
            scaledDp(32f),
            paint
        )
    }

    private fun drawTopDecorativeKey(canvas: Canvas) {
        paint.style = Paint.Style.FILL
        paint.color = KEY_NORMAL
        canvas.drawRoundRect(
            topDecorRect,
            scaledDp(12f),
            scaledDp(12f),
            paint
        )

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = scaledDp(1.2f)
        paint.color = KEY_BORDER
        canvas.drawRoundRect(
            topDecorRect,
            scaledDp(12f),
            scaledDp(12f),
            paint
        )

        // Small neutral handset/menu mark from the reference phone keypad.
        val cx = topDecorRect.centerX()
        val cy = topDecorRect.centerY()

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = scaledDp(1.8f)
        paint.color = TEXT_DARK

        canvas.drawRect(
            cx - scaledDp(4f),
            cy - scaledDp(4f),
            cx + scaledDp(4f),
            cy + scaledDp(4f),
            paint
        )
    }

    private fun drawSoftKey(
        canvas: Canvas,
        rect: RectF,
        label: String,
        pressed: Boolean
    ) {
        if (pressed) {
            paint.style = Paint.Style.FILL
            paint.color = PRESS_GLOW
            canvas.drawRoundRect(
                RectF(
                    rect.left - scaledDp(3f),
                    rect.top - scaledDp(3f),
                    rect.right + scaledDp(3f),
                    rect.bottom + scaledDp(3f)
                ),
                scaledDp(18f),
                scaledDp(18f),
                paint
            )
        }

        paint.style = Paint.Style.FILL
        paint.color =
            if (pressed) KEY_PRESSED else KEY_NORMAL

        canvas.drawRoundRect(
            rect,
            scaledDp(16f),
            scaledDp(16f),
            paint
        )

        paint.style = Paint.Style.STROKE
        paint.strokeWidth =
            if (pressed) scaledDp(2f) else scaledDp(1.3f)
        paint.color =
            if (pressed) BLUE_BRIGHT else KEY_BORDER

        canvas.drawRoundRect(
            rect,
            scaledDp(16f),
            scaledDp(16f),
            paint
        )

        textPaint.textSize = scaledDp(13f)
        textPaint.color =
            if (pressed) Color.WHITE else TEXT_DARK

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

    private fun drawNavigationPad(canvas: Canvas) {
        paint.style = Paint.Style.FILL
        paint.color = NAV_SHADOW

        val shadow = RectF(
            navRect.left - scaledDp(3f),
            navRect.top + scaledDp(2f),
            navRect.right + scaledDp(3f),
            navRect.bottom + scaledDp(4f)
        )

        canvas.drawRoundRect(
            shadow,
            scaledDp(24f),
            scaledDp(24f),
            paint
        )

        paint.color = NAV_BLUE
        canvas.drawRoundRect(
            navRect,
            scaledDp(24f),
            scaledDp(24f),
            paint
        )

        // Pale inner rim, similar to the silver/white trim in the reference.
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = scaledDp(3f)
        paint.color = NAV_RIM
        canvas.drawRoundRect(
            RectF(
                navRect.left + scaledDp(2f),
                navRect.top + scaledDp(2f),
                navRect.right - scaledDp(2f),
                navRect.bottom - scaledDp(2f)
            ),
            scaledDp(21f),
            scaledDp(21f),
            paint
        )

        drawDirectionHighlight(canvas)
        drawDirectionIcon(
            canvas,
            Direction.UP,
            navRect.centerX(),
            navRect.top + navRect.height() * 0.14f
        )
        drawDirectionIcon(
            canvas,
            Direction.DOWN,
            navRect.centerX(),
            navRect.bottom - navRect.height() * 0.14f
        )
        drawDirectionIcon(
            canvas,
            Direction.LEFT,
            navRect.left + navRect.width() * 0.13f,
            navRect.centerY()
        )
        drawDirectionIcon(
            canvas,
            Direction.RIGHT,
            navRect.right - navRect.width() * 0.13f,
            navRect.centerY()
        )

        if (pressedCenter) {
            paint.style = Paint.Style.FILL
            paint.color = PRESS_GLOW
            canvas.drawRoundRect(
                RectF(
                    okRect.left - scaledDp(3f),
                    okRect.top - scaledDp(3f),
                    okRect.right + scaledDp(3f),
                    okRect.bottom + scaledDp(3f)
                ),
                scaledDp(17f),
                scaledDp(17f),
                paint
            )
        }

        paint.style = Paint.Style.FILL
        paint.color =
            if (pressedCenter) {
                OK_PRESSED
            } else {
                OK_NORMAL
            }

        canvas.drawRoundRect(
            okRect,
            scaledDp(15f),
            scaledDp(15f),
            paint
        )

        paint.style = Paint.Style.STROKE
        paint.strokeWidth =
            if (pressedCenter) scaledDp(2f) else scaledDp(1.3f)
        paint.color =
            if (pressedCenter) BLUE_BRIGHT else OK_BORDER

        canvas.drawRoundRect(
            okRect,
            scaledDp(15f),
            scaledDp(15f),
            paint
        )

        textPaint.textSize = scaledDp(17f)
        textPaint.color = TEXT_DARK

        val baseline =
            okRect.centerY() -
                (textPaint.descent() + textPaint.ascent()) / 2f

        canvas.drawText(
            "OK",
            okRect.centerX(),
            baseline,
            textPaint
        )
    }

    private fun drawDirectionHighlight(canvas: Canvas) {
        val direction = pressedDirection ?: return

        val highlight = when (direction) {
            Direction.UP -> RectF(
                navRect.left + navRect.width() * 0.29f,
                navRect.top + scaledDp(5f),
                navRect.right - navRect.width() * 0.29f,
                okRect.top - scaledDp(2f)
            )

            Direction.DOWN -> RectF(
                navRect.left + navRect.width() * 0.29f,
                okRect.bottom + scaledDp(2f),
                navRect.right - navRect.width() * 0.29f,
                navRect.bottom - scaledDp(5f)
            )

            Direction.LEFT -> RectF(
                navRect.left + scaledDp(5f),
                navRect.top + navRect.height() * 0.30f,
                okRect.left - scaledDp(2f),
                navRect.bottom - navRect.height() * 0.30f
            )

            Direction.RIGHT -> RectF(
                okRect.right + scaledDp(2f),
                navRect.top + navRect.height() * 0.30f,
                navRect.right - scaledDp(5f),
                navRect.bottom - navRect.height() * 0.30f
            )
        }

        paint.style = Paint.Style.FILL
        paint.color = NAV_PRESSED

        canvas.drawRoundRect(
            highlight,
            scaledDp(10f),
            scaledDp(10f),
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
        val size = scaledDp(7f)

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

    private fun drawDecorativeBottomKeys(canvas: Canvas) {
        val sw = shellRect.width()
        val sh = shellRect.height()

        val leftPhone = RectF(
            shellRect.left + sw * 0.04f,
            shellRect.top + sh * 0.68f,
            shellRect.left + sw * 0.30f,
            shellRect.bottom - sh * 0.035f
        )

        drawDecorativeKey(canvas, leftPhone)
        drawDecorativeKey(
            canvas = canvas,
            rect = exitRect,
            pressed = pressedSoftKey == SoftKey.EXIT
        )
        drawDecorativeKey(canvas, bottomCancelRect)

        // Green call-like arc.
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = scaledDp(5f)
        paint.color = CALL_GREEN

        canvas.drawArc(
            RectF(
                leftPhone.centerX() - scaledDp(18f),
                leftPhone.centerY() - scaledDp(8f),
                leftPhone.centerX() + scaledDp(18f),
                leftPhone.centerY() + scaledDp(16f)
            ),
            205f,
            130f,
            false,
            paint
        )

        // Red end-call-like arc.
        paint.color = END_RED
        canvas.drawArc(
            RectF(
                exitRect.centerX() - scaledDp(18f),
                exitRect.centerY() - scaledDp(8f),
                exitRect.centerX() + scaledDp(18f),
                exitRect.centerY() + scaledDp(16f)
            ),
            205f,
            130f,
            false,
            paint
        )

        textPaint.textSize = scaledDp(11f)
        textPaint.color = TEXT_MUTED

        val cancelBaseline =
            bottomCancelRect.centerY() -
                (textPaint.descent() + textPaint.ascent()) / 2f

        canvas.drawText(
            "CANCEL",
            bottomCancelRect.centerX(),
            cancelBaseline,
            textPaint
        )
    }

    private fun drawDecorativeKey(
        canvas: Canvas,
        rect: RectF,
        pressed: Boolean = false
    ) {
        if (pressed) {
            paint.style = Paint.Style.FILL
            paint.color = Color.argb(92, 215, 45, 40)
            canvas.drawRoundRect(
                RectF(
                    rect.left - scaledDp(3f),
                    rect.top - scaledDp(3f),
                    rect.right + scaledDp(3f),
                    rect.bottom + scaledDp(3f)
                ),
                scaledDp(19f),
                scaledDp(19f),
                paint
            )
        }

        paint.style = Paint.Style.FILL
        paint.color =
            if (pressed) Color.rgb(232, 194, 181) else KEY_NORMAL

        canvas.drawRoundRect(
            rect,
            scaledDp(16f),
            scaledDp(16f),
            paint
        )

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = scaledDp(1.1f)
        paint.color =
            if (pressed) END_RED else KEY_BORDER

        canvas.drawRoundRect(
            rect,
            scaledDp(16f),
            scaledDp(16f),
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
        EXIT
    }

    private companion object {
        const val INITIAL_REPEAT_DELAY_MS = 280L
        const val REPEAT_INTERVAL_MS = 110L
        const val BASE_CONTROL_HEIGHT_DP = 248
        const val MIN_CONTROL_SCALE = 0.58f

        val SHELL_BASE: Int = Color.rgb(200, 195, 177)
        val SHELL_INNER: Int = Color.rgb(219, 215, 198)
        val SHELL_SHADOW: Int = Color.rgb(143, 139, 127)
        val SHELL_BORDER: Int = Color.rgb(104, 101, 92)

        val KEY_NORMAL: Int = Color.rgb(215, 211, 195)
        val KEY_PRESSED: Int = Color.rgb(57, 102, 181)
        val KEY_BORDER: Int = Color.rgb(116, 111, 100)

        val NAV_BLUE: Int = Color.rgb(43, 82, 168)
        val NAV_PRESSED: Int = Color.rgb(25, 61, 143)
        val NAV_SHADOW: Int = Color.rgb(77, 79, 89)
        val NAV_RIM: Int = Color.rgb(230, 230, 218)
        val NAV_ICON: Int = Color.rgb(209, 221, 235)

        val OK_NORMAL: Int = Color.rgb(211, 211, 197)
        val OK_PRESSED: Int = Color.rgb(181, 210, 242)
        val OK_BORDER: Int = Color.rgb(100, 105, 106)

        val PRESS_GLOW: Int = Color.argb(110, 0, 142, 255)
        val BLUE_BRIGHT: Int = Color.rgb(0, 148, 255)

        val CALL_GREEN: Int = Color.rgb(19, 139, 113)
        val END_RED: Int = Color.rgb(177, 45, 40)

        val TEXT_DARK: Int = Color.rgb(26, 29, 31)
        val TEXT_MUTED: Int = Color.rgb(80, 78, 71)
    }
}
