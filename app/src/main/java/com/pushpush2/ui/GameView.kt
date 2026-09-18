package com.pushpush2.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.view.View
import com.pushpush2.R
import com.pushpush2.game.GameState
import com.pushpush2.game.Position
import kotlin.math.min

class GameView(context: Context) : View(context) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        isFilterBitmap = false
    }

    private val brickBitmap: Bitmap =
        BitmapFactory.decodeResource(resources, R.drawable.tile_brick)
    private val goalBitmap: Bitmap =
        BitmapFactory.decodeResource(resources, R.drawable.tile_goal)
    private val boxBitmap: Bitmap =
        BitmapFactory.decodeResource(resources, R.drawable.tile_box)
    private val playerBitmap: Bitmap =
        BitmapFactory.decodeResource(resources, R.drawable.tile_player)

    private val sourceRect = Rect(0, 0, 14, 14)

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
                val position = Position(x, y)
                val destination = cellRect(position, cell, offsetX, offsetY)

                paint.style = Paint.Style.FILL
                paint.color = FLOOR_COLOR
                canvas.drawRect(destination, paint)

                when {
                    position in stage.walls ->
                        drawTile(canvas, brickBitmap, destination)

                    position in stage.goals ->
                        drawTile(canvas, goalBitmap, destination)
                }

                if (position in state.boxes) {
                    drawTile(canvas, boxBitmap, destination)
                }
            }
        }

        drawTile(
            canvas,
            playerBitmap,
            cellRect(state.player, cell, offsetX, offsetY)
        )
    }

    private fun drawTile(
        canvas: Canvas,
        bitmap: Bitmap,
        destination: RectF
    ) {
        canvas.drawBitmap(bitmap, sourceRect, destination, paint)
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

    private companion object {
        val FLOOR_COLOR: Int = Color.rgb(255, 255, 255)
    }
}
