package com.pushpush2.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.View
import com.pushpush2.game.GameState
import com.pushpush2.game.Position
import kotlin.math.min

class GameView(context: Context) : View(context) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var gameState: GameState? = null

    fun render(state: GameState) {
        gameState = state
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.rgb(20, 20, 20))

        val state = gameState ?: return
        val stage = state.stage

        val cell = min(
            width.toFloat() / stage.width.coerceAtLeast(1),
            height.toFloat() / stage.height.coerceAtLeast(1)
        )

        val boardWidth = cell * stage.width
        val boardHeight = cell * stage.height
        val offsetX = (width - boardWidth) / 2f
        val offsetY = (height - boardHeight) / 2f

        for (y in 0 until stage.height) {
            for (x in 0 until stage.width) {
                val p = Position(x, y)
                val left = offsetX + x * cell
                val top = offsetY + y * cell
                val rect = RectF(left, top, left + cell, top + cell)

                paint.style = Paint.Style.FILL
                paint.color = if (p in stage.walls) {
                    Color.rgb(70, 75, 80)
                } else {
                    Color.rgb(230, 220, 190)
                }
                canvas.drawRect(rect, paint)

                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 1f
                paint.color = Color.argb(35, 0, 0, 0)
                canvas.drawRect(rect, paint)

                if (p in stage.goals) {
                    paint.style = Paint.Style.FILL
                    paint.color = Color.rgb(220, 70, 70)
                    canvas.drawCircle(rect.centerX(), rect.centerY(), cell * 0.16f, paint)
                }

                if (p in state.boxes) {
                    val margin = cell * 0.12f
                    val boxRect = RectF(
                        rect.left + margin,
                        rect.top + margin,
                        rect.right - margin,
                        rect.bottom - margin
                    )

                    paint.style = Paint.Style.FILL
                    paint.color = if (p in stage.goals) {
                        Color.rgb(80, 170, 85)
                    } else {
                        Color.rgb(180, 120, 55)
                    }
                    canvas.drawRoundRect(boxRect, cell * 0.08f, cell * 0.08f, paint)
                }
            }
        }

        val playerRect = cellRect(state.player, cell, offsetX, offsetY)
        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(55, 105, 205)
        canvas.drawCircle(
            playerRect.centerX(),
            playerRect.centerY(),
            cell * 0.34f,
            paint
        )
    }

    private fun cellRect(
        position: Position,
        cell: Float,
        offsetX: Float,
        offsetY: Float
    ): RectF {
        val left = offsetX + position.x * cell
        val top = offsetY + position.y * cell
        return RectF(left, top, left + cell, top + cell)
    }
}
