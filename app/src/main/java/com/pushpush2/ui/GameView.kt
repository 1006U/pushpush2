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

    private val sourceRect = Rect(0, 0, ORIGINAL_TILE_PX, ORIGINAL_TILE_PX)

    private var gameState: GameState? = null

    fun render(state: GameState) {
        gameState = state
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(OUTER_BACKGROUND)

        val state = gameState ?: return
        val stage = state.stage

        val virtualScale = min(
            width.toFloat() / ORIGINAL_STAGE_WIDTH,
            height.toFloat() / ORIGINAL_STAGE_HEIGHT
        )

        val virtualWidth = ORIGINAL_STAGE_WIDTH * virtualScale
        val virtualHeight = ORIGINAL_STAGE_HEIGHT * virtualScale
        val viewportLeft = (width - virtualWidth) / 2f
        val viewportTop = (height - virtualHeight) / 2f

        val viewport = RectF(
            viewportLeft,
            viewportTop,
            viewportLeft + virtualWidth,
            viewportTop + virtualHeight
        )

        paint.style = Paint.Style.FILL
        paint.color = FLOOR_COLOR
        canvas.drawRect(viewport, paint)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = maxOf(1f, virtualScale)
        paint.color = FRAME_COLOR
        canvas.drawRect(viewport, paint)

        val cell = ORIGINAL_TILE_PX * virtualScale

        val boardWidth = stage.width * cell
        val boardHeight = stage.height * cell

        val offsetX = viewport.centerX() - boardWidth / 2f
        val offsetY = viewport.centerY() - boardHeight / 2f

        for (y in 0 until stage.height) {
            for (x in 0 until stage.width) {
                val position = Position(x, y)
                val destination = cellRect(
                    position = position,
                    cell = cell,
                    offsetX = offsetX,
                    offsetY = offsetY
                )

                if (position in stage.walls) {
                    drawTile(canvas, brickBitmap, destination)
                } else if (position in stage.goals) {
                    drawTile(canvas, goalBitmap, destination)
                }

                if (position in state.boxes) {
                    drawTile(canvas, boxBitmap, destination)
                }
            }
        }

        drawTile(
            canvas = canvas,
            bitmap = playerBitmap,
            destination = cellRect(
                position = state.player,
                cell = cell,
                offsetX = offsetX,
                offsetY = offsetY
            )
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

        return RectF(
            left,
            top,
            left + cell,
            top + cell
        )
    }

    private companion object {
        const val ORIGINAL_STAGE_WIDTH = 240f
        const val ORIGINAL_STAGE_HEIGHT = 250f
        const val ORIGINAL_TILE_PX = 14

        val OUTER_BACKGROUND: Int = Color.rgb(8, 10, 12)
        val FLOOR_COLOR: Int = Color.rgb(255, 255, 255)
        val FRAME_COLOR: Int = Color.rgb(98, 103, 108)
    }
}
