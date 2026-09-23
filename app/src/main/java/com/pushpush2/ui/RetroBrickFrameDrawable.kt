package com.pushpush2.ui

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.drawable.Drawable
import kotlin.math.max

/**
 * Feature-phone style brick frame used around the header portrait/message.
 *
 * The original portrait has brick columns only on the left/right, while the
 * message panel has brick bands on all four sides. The drawable supports both
 * layouts so the two halves can match the original screen independently.
 */
internal class RetroBrickFrameDrawable(
    private val fillColor: Int,
    private val borderWidthPx: Float,
    private val horizontalBands: Boolean = false
) : Drawable() {

    private val paint = Paint().apply {
        isAntiAlias = false
        style = Paint.Style.FILL
    }

    private var drawableAlpha = 255

    override fun draw(canvas: Canvas) {
        val b = bounds
        if (b.width() <= 0 || b.height() <= 0) return

        val left = b.left.toFloat()
        val top = b.top.toFloat()
        val right = b.right.toFloat()
        val bottom = b.bottom.toFloat()
        val bw = borderWidthPx.coerceAtLeast(2f)

        val contentTop = if (horizontalBands) top + bw else top
        val contentBottom = if (horizontalBands) bottom - bw else bottom

        paint.style = Paint.Style.FILL
        paint.color = withAlpha(fillColor)
        canvas.drawRect(
            left + bw,
            contentTop,
            right - bw,
            contentBottom,
            paint
        )

        if (horizontalBands) {
            drawHorizontalBrickBand(
                canvas = canvas,
                left = left,
                top = top,
                right = right,
                bottom = top + bw,
                bw = bw
            )
            drawHorizontalBrickBand(
                canvas = canvas,
                left = left,
                top = bottom - bw,
                right = right,
                bottom = bottom,
                bw = bw
            )
        }

        drawVerticalBrickBand(canvas, left, top, left + bw, bottom, bw)
        drawVerticalBrickBand(canvas, right - bw, top, right, bottom, bw)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = max(1f, bw * 0.13f)
        paint.color = withAlpha(MORTAR)

        if (horizontalBands) {
            canvas.drawRect(
                RectF(left, top, right, bottom),
                paint
            )
            canvas.drawRect(
                RectF(
                    left + bw,
                    top + bw,
                    right - bw,
                    bottom - bw
                ),
                paint
            )
        } else {
            canvas.drawLine(left + bw, top, left + bw, bottom, paint)
            canvas.drawLine(right - bw, top, right - bw, bottom, paint)
        }

        paint.style = Paint.Style.FILL
    }

    private fun drawHorizontalBrickBand(
        canvas: Canvas,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        bw: Float
    ) {
        paint.color = withAlpha(BRICK_RED)
        canvas.drawRect(left, top, right, bottom, paint)

        val rowHeight = bw / 2f
        val brickWidth = bw * 1.75f
        val mortar = max(1f, bw * 0.13f)

        paint.color = withAlpha(MORTAR)
        canvas.drawRect(
            left,
            top + rowHeight - mortar / 2f,
            right,
            top + rowHeight + mortar / 2f,
            paint
        )

        repeat(2) { row ->
            val rowTop = top + row * rowHeight
            val rowBottom = (rowTop + rowHeight).coerceAtMost(bottom)
            val offset = if (row == 0) 0f else brickWidth / 2f

            paint.color = withAlpha(BRICK_HIGHLIGHT)
            canvas.drawRect(
                left,
                rowTop + mortar,
                right,
                (rowTop + mortar + max(1f, bw * 0.12f))
                    .coerceAtMost(rowBottom),
                paint
            )

            paint.color = withAlpha(MORTAR)
            var x = left + offset
            while (x < right) {
                canvas.drawRect(
                    x - mortar / 2f,
                    rowTop,
                    x + mortar / 2f,
                    rowBottom,
                    paint
                )
                x += brickWidth
            }
        }
    }

    private fun drawVerticalBrickBand(
        canvas: Canvas,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        bw: Float
    ) {
        paint.color = withAlpha(BRICK_RED)
        canvas.drawRect(left, top, right, bottom, paint)

        val columnWidth = bw / 2f
        val brickHeight = bw * 1.75f
        val mortar = max(1f, bw * 0.13f)

        paint.color = withAlpha(MORTAR)
        canvas.drawRect(
            left + columnWidth - mortar / 2f,
            top,
            left + columnWidth + mortar / 2f,
            bottom,
            paint
        )

        repeat(2) { column ->
            val columnLeft = left + column * columnWidth
            val offset = if (column == 0) 0f else brickHeight / 2f

            paint.color = withAlpha(BRICK_HIGHLIGHT)
            canvas.drawRect(
                columnLeft + mortar,
                top,
                (columnLeft + mortar + max(1f, bw * 0.12f))
                    .coerceAtMost(right),
                bottom,
                paint
            )

            paint.color = withAlpha(MORTAR)
            var y = top + offset
            while (y < bottom) {
                canvas.drawRect(
                    columnLeft,
                    y - mortar / 2f,
                    (columnLeft + columnWidth).coerceAtMost(right),
                    y + mortar / 2f,
                    paint
                )
                y += brickHeight
            }
        }
    }

    override fun setAlpha(alpha: Int) {
        drawableAlpha = alpha.coerceIn(0, 255)
        invalidateSelf()
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
        invalidateSelf()
    }

    @Suppress("DEPRECATION")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    private fun withAlpha(color: Int): Int =
        Color.argb(
            drawableAlpha,
            Color.red(color),
            Color.green(color),
            Color.blue(color)
        )

    private companion object {
        val BRICK_RED: Int = Color.rgb(189, 24, 0)
        val BRICK_HIGHLIGHT: Int = Color.rgb(230, 116, 0)
        val MORTAR: Int = Color.BLACK
    }
}
