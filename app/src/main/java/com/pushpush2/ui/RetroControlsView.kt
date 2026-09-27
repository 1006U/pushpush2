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
 * Functional layout based on the original feature-phone keypad:
 * - upper-left soft key: 스테이지
 * - upper-right soft key: 리셋
 * - large center navigation pad: UP / DOWN / LEFT / RIGHT
 * - center key: 확인
 * - lower-center key: 돌아가기 (one-step undo)
 * - lower-left call key: decorative only
 * - lower-right end-call key: exit
 *
 * Direction and confirm controls keep their original proportions but get the
 * largest practical touch area. Utility buttons stay smaller and separated by
 * dead space so imprecise thumb touches do not trigger the wrong action.
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
    private val topDecorRect = RectF()
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

        drawPhoneHousing(canvas)
        drawSectionDividers(canvas)
        drawTopDecorativeKey(canvas)
        drawSoftKey(
            canvas = canvas,
            rect = stageRect,
            label = "스테이지",
            pressed = pressedSoftKey == SoftKey.STAGE
        )
        drawSoftKey(
            canvas = canvas,
            rect = resetRect,
            label = "리셋",
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
         * Geometry rebuilt from the supplied Samsung feature-phone keypad:
         *
         *          [ small top key ]
         * [스테이지] [ large D-pad + 확인 ] [리셋]
         * [ call ]   [    돌아가기    ]   [ exit ]
         *
         * The D-pad intentionally owns most of the upper keypad. Side and
         * bottom utility buttons are smaller and their touch rectangles never
         * overlap the navigation rectangle.
         */
        topDecorRect.set(
            shellRect.left + sw * 0.425f,
            shellRect.top + sh * 0.025f,
            shellRect.right - sw * 0.425f,
            shellRect.top + sh * 0.105f
        )

        stageRect.set(
            shellRect.left + sw * 0.025f,
            shellRect.top + sh * 0.095f,
            shellRect.left + sw * 0.185f,
            shellRect.top + sh * 0.405f
        )

        resetRect.set(
            shellRect.right - sw * 0.185f,
            shellRect.top + sh * 0.095f,
            shellRect.right - sw * 0.025f,
            shellRect.top + sh * 0.405f
        )

        navRect.set(
            shellRect.left + sw * 0.215f,
            shellRect.top + sh * 0.055f,
            shellRect.right - sw * 0.215f,
            shellRect.top + sh * 0.705f
        )

        // Keep the existing directional-pad : center-key proportions.
        okRect.set(
            navRect.left + navRect.width() * 0.255f,
            navRect.top + navRect.height() * 0.29f,
            navRect.right - navRect.width() * 0.255f,
            navRect.bottom - navRect.height() * 0.29f
        )

        undoRect.set(
            shellRect.left + sw * 0.315f,
            shellRect.top + sh * 0.79f,
            shellRect.right - sw * 0.315f,
            shellRect.bottom - sh * 0.035f
        )

        exitRect.set(
            shellRect.right - sw * 0.19f,
            shellRect.top + sh * 0.73f,
            shellRect.right - sw * 0.025f,
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

    private fun drawSectionDividers(canvas: Canvas) {
        /*
         * Keep the subtle seam weight, but deliberately extend every divider
         * past the neighbouring panel boundary. drawNavigationPad() is rendered
         * afterwards, so the small overlaps disappear underneath its rim and
         * the seams read as continuous instead of stopping short.
         *
         * These lines are visual only; touch geometry remains unchanged.
         */
        paint.shader = null
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = scaledDp(1.5f)
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeJoin = Paint.Join.ROUND
        paint.color = Color.rgb(96, 94, 86)

        val sw = shellRect.width()
        val sh = shellRect.height()
        val overlap = scaledDp(5f)

        // Upper seams: extend from the housing top and slightly into the D-pad.
        canvas.drawLine(
            navRect.left + scaledDp(1.5f),
            shellRect.top + scaledDp(1f),
            navRect.left - scaledDp(0.5f),
            navRect.top + overlap,
            paint
        )
        canvas.drawLine(
            navRect.right - scaledDp(1.5f),
            shellRect.top + scaledDp(1f),
            navRect.right + scaledDp(0.5f),
            navRect.top + overlap,
            paint
        )

        // Middle seams: run fully from the outer housing into the D-pad edge.
        canvas.drawLine(
            shellRect.left,
            shellRect.top + sh * 0.425f,
            navRect.left + overlap,
            shellRect.top + sh * 0.565f,
            paint
        )
        canvas.drawLine(
            navRect.right - overlap,
            shellRect.top + sh * 0.565f,
            shellRect.right,
            shellRect.top + sh * 0.425f,
            paint
        )

        // Lower seams: begin inside the D-pad rim and continue to the bottom.
        canvas.drawLine(
            navRect.left + overlap,
            navRect.bottom - overlap,
            shellRect.left + sw * 0.255f,
            shellRect.bottom,
            paint
        )
        canvas.drawLine(
            navRect.right - overlap,
            navRect.bottom - overlap,
            shellRect.right - sw * 0.255f,
            shellRect.bottom,
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
    }

    private fun drawSoftKey(
        canvas: Canvas,
        rect: RectF,
        label: String,
        pressed: Boolean
    ) {
        /*
         * The reference layout does not need a separate outline around these
         * utility keys. Their region is defined by the thin section seams.
         * Keep only a subtle pressed fill for touch feedback.
         */
        if (pressed) {
            paint.style = Paint.Style.FILL
            paint.color = Color.argb(55, 43, 82, 168)
            canvas.drawRoundRect(
                rect,
                scaledDp(8f),
                scaledDp(8f),
                paint
            )
        }

        textPaint.textSize = scaledDp(10f)
        textPaint.color =
            if (pressed) NAV_BLUE else TEXT_DARK

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
            navRect.left - scaledDp(2f),
            navRect.top + scaledDp(3f),
            navRect.right + scaledDp(2f),
            navRect.bottom + scaledDp(4f)
        )
        canvas.drawRoundRect(
            shadow,
            scaledDp(22f),
            scaledDp(22f),
            paint
        )

        // The photographed keypad uses a pale navigation face with a navy rim.
        paint.style = Paint.Style.FILL
        paint.color = NAV_FACE
        canvas.drawRoundRect(
            navRect,
            scaledDp(22f),
            scaledDp(22f),
            paint
        )

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = scaledDp(3f)
        paint.color = NAV_BLUE
        canvas.drawRoundRect(
            navRect,
            scaledDp(22f),
            scaledDp(22f),
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
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = scaledDp(4f)
            paint.color = PRESS_GLOW
            canvas.drawRoundRect(
                RectF(
                    okRect.left - scaledDp(2f),
                    okRect.top - scaledDp(2f),
                    okRect.right + scaledDp(2f),
                    okRect.bottom + scaledDp(2f)
                ),
                scaledDp(10f),
                scaledDp(10f),
                paint
            )
        }

        // NATE position -> 확인. Keep the dark-blue visual emphasis.
        paint.style = Paint.Style.FILL
        paint.color =
            if (pressedCenter) NAV_PRESSED else NAV_BLUE
        canvas.drawRoundRect(
            okRect,
            scaledDp(9f),
            scaledDp(9f),
            paint
        )

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = scaledDp(1.2f)
        paint.color = NAV_RIM
        canvas.drawRoundRect(
            okRect,
            scaledDp(9f),
            scaledDp(9f),
            paint
        )

        textPaint.textSize = scaledDp(13.5f)
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
        val size = scaledDp(9f)

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
            if (pressed) Color.WHITE else NAV_BLUE

        canvas.drawPath(path, paint)
    }

    private fun drawDecorativeBottomKeys(canvas: Canvas) {
        val sw = shellRect.width()
        val sh = shellRect.height()

        val leftPhone = RectF(
            shellRect.left + sw * 0.025f,
            shellRect.top + sh * 0.73f,
            shellRect.left + sw * 0.19f,
            shellRect.bottom - sh * 0.035f
        )

        /*
         * No individual key outlines here. The black dividers define the three
         * lower regions. Only pressed feedback, text and phone icons are drawn.
         */
        drawPhoneKey(
            canvas = canvas,
            rect = leftPhone,
            pressed = false,
            isLeft = true
        )

        drawBottomActionKey(
            canvas = canvas,
            rect = undoRect,
            label = "돌아가기",
            pressed = pressedSoftKey == SoftKey.UNDO
        )

        drawPhoneKey(
            canvas = canvas,
            rect = exitRect,
            pressed = pressedSoftKey == SoftKey.EXIT,
            isLeft = false
        )

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = scaledDp(4.5f)
        paint.strokeCap = Paint.Cap.ROUND

        // Green call icon, decorative only.
        paint.color = CALL_GREEN
        canvas.drawArc(
            RectF(
                leftPhone.centerX() - scaledDp(17f),
                leftPhone.centerY() - scaledDp(7f),
                leftPhone.centerX() + scaledDp(17f),
                leftPhone.centerY() + scaledDp(15f)
            ),
            205f,
            130f,
            false,
            paint
        )

        // Red end-call icon, still exits the game.
        paint.color = END_RED
        canvas.drawArc(
            RectF(
                exitRect.centerX() - scaledDp(17f),
                exitRect.centerY() - scaledDp(7f),
                exitRect.centerX() + scaledDp(17f),
                exitRect.centerY() + scaledDp(15f)
            ),
            205f,
            130f,
            false,
            paint
        )
    }

    private fun drawPhoneKey(
        canvas: Canvas,
        rect: RectF,
        pressed: Boolean,
        isLeft: Boolean
    ) {
        // Normal state has no surrounding key outline.
        if (pressed) {
            paint.style = Paint.Style.FILL
            paint.color =
                if (isLeft) {
                    Color.argb(45, 19, 139, 113)
                } else {
                    Color.argb(55, 177, 45, 40)
                }
            canvas.drawRoundRect(
                rect,
                scaledDp(8f),
                scaledDp(8f),
                paint
            )
        }
    }

    private fun drawBottomActionKey(
        canvas: Canvas,
        rect: RectF,
        label: String,
        pressed: Boolean
    ) {
        if (pressed) {
            paint.style = Paint.Style.FILL
            paint.color = Color.argb(55, 43, 82, 168)
            canvas.drawRoundRect(
                rect,
                scaledDp(8f),
                scaledDp(8f),
                paint
            )
        }

        textPaint.textSize = scaledDp(10.5f)
        textPaint.color =
            if (pressed) NAV_BLUE else TEXT_DARK

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

        val SHELL_BASE: Int = Color.rgb(200, 195, 177)
        val SHELL_INNER: Int = Color.rgb(219, 215, 198)
        val SHELL_SHADOW: Int = Color.rgb(143, 139, 127)
        val SHELL_BORDER: Int = Color.rgb(104, 101, 92)

        val KEY_NORMAL: Int = Color.rgb(215, 211, 195)
        val KEY_PRESSED: Int = Color.rgb(57, 102, 181)
        val KEY_BORDER: Int = Color.rgb(116, 111, 100)

        val NAV_FACE: Int = Color.rgb(224, 226, 218)
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
