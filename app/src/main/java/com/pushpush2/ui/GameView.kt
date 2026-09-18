package com.pushpush2.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.os.SystemClock
import android.util.Base64
import android.view.View
import com.pushpush2.R
import com.pushpush2.game.GameState
import com.pushpush2.game.Position
import kotlin.math.floor
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

    /*
     * Sprite 370의 기본 루프에서 사용하는 Shape 358/359를 14x14 PNG로
     * 복원한 데이터다. 원본 SWF 바이너리는 저장소에 포함하지 않는다.
     */
    private val playerBlinkHalfBitmap: Bitmap =
        decodeEmbeddedBitmap(PLAYER_BLINK_HALF_PNG)
    private val playerBlinkClosedBitmap: Bitmap =
        decodeEmbeddedBitmap(PLAYER_BLINK_CLOSED_PNG)

    /*
     * 원본 Sprite 356 frame 2~13 / Sprite 370 frame 71~76을
     * game.swf에서 직접 복원한 프레임이다.
     */
    private val boxGoalBitmaps: List<Bitmap> =
        OriginalAnimationFrames.boxGoalPngBase64.map(::decodeEmbeddedBitmap)
    private val playerSuccessBitmaps: List<Bitmap> =
        OriginalAnimationFrames.playerSuccessPngBase64.map(::decodeEmbeddedBitmap)

    private val sourceRect = Rect(0, 0, ORIGINAL_TILE_PX, ORIGINAL_TILE_PX)

    private var gameState: GameState? = null
    private var playerAnimationStartedAtMs = SystemClock.uptimeMillis()
    private var playerSuccessStartedAtMs: Long? = null
    private val boxGoalAnimationStarts = mutableMapOf<Position, Long>()

    private val animationTick = object : Runnable {
        override fun run() {
            if (!isAttachedToWindow) return

            invalidate()
            postDelayed(this, ORIGINAL_FRAME_DURATION_MS)
        }
    }

    fun render(state: GameState) {
        if (gameState?.stage?.number != state.stage.number) {
            resetPlayerAnimation()
        }

        gameState = state

        val completedBoxes = state.boxes
            .filterTo(mutableSetOf()) { it in state.stage.goals }

        boxGoalAnimationStarts.keys.retainAll(completedBoxes)
        invalidate()
    }

    fun playGoalSuccess(boxPosition: Position) {
        val now = SystemClock.uptimeMillis()
        boxGoalAnimationStarts[boxPosition] = now
        playerSuccessStartedAtMs = now
        invalidate()
    }

    fun resetPlayerAnimation() {
        playerAnimationStartedAtMs = SystemClock.uptimeMillis()
        playerSuccessStartedAtMs = null
        boxGoalAnimationStarts.clear()
        invalidate()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        removeCallbacks(animationTick)
        post(animationTick)
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(animationTick)
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(OUTER_BACKGROUND)

        val state = gameState ?: return
        val stage = state.stage

        if (width <= 0 || height <= 0) return

        val now = SystemClock.uptimeMillis()
        val horizontalPadding = dp(12f)
        val verticalPadding = dp(12f)

        val availableWidth =
            (width.toFloat() - horizontalPadding * 2f).coerceAtLeast(1f)
        val availableHeight =
            (height.toFloat() - verticalPadding * 2f).coerceAtLeast(1f)

        val rawCell = min(
            availableWidth / stage.width.coerceAtLeast(1),
            availableHeight / stage.height.coerceAtLeast(1)
        )

        /*
         * Pixel-art 타일이 흐릿해지지 않도록 가능한 경우 정수 픽셀 배율을 사용한다.
         * 아주 작은 화면에서는 rawCell을 그대로 사용해서 전체 맵이 잘리지 않게 한다.
         */
        val cell = if (rawCell >= ORIGINAL_TILE_PX) {
            val integerScale = floor(rawCell / ORIGINAL_TILE_PX)
                .coerceAtLeast(1f)
            ORIGINAL_TILE_PX * integerScale
        } else {
            rawCell
        }

        val boardWidth = stage.width * cell
        val boardHeight = stage.height * cell

        val offsetX = (width - boardWidth) / 2f
        val offsetY = (height - boardHeight) / 2f

        val boardRect = RectF(
            offsetX,
            offsetY,
            offsetX + boardWidth,
            offsetY + boardHeight
        )

        paint.style = Paint.Style.FILL
        paint.color = FLOOR_COLOR
        canvas.drawRect(boardRect, paint)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = maxOf(1f, dp(1f))
        paint.color = FRAME_COLOR
        canvas.drawRect(boardRect, paint)

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
                    drawTile(
                        canvas = canvas,
                        bitmap = currentBoxBitmap(
                            position = position,
                            state = state,
                            now = now
                        ),
                        destination = destination
                    )
                }
            }
        }

        drawTile(
            canvas = canvas,
            bitmap = currentPlayerBitmap(now),
            destination = cellRect(
                position = state.player,
                cell = cell,
                offsetX = offsetX,
                offsetY = offsetY
            )
        )
    }

    private fun currentBoxBitmap(
        position: Position,
        state: GameState,
        now: Long
    ): Bitmap {
        if (position !in state.stage.goals) {
            return boxBitmap
        }

        val startedAt = boxGoalAnimationStarts[position]
            ?: return boxGoalBitmaps.last()

        val elapsed = (now - startedAt).coerceAtLeast(0L)
        val frameIndex =
            (elapsed / ORIGINAL_FRAME_DURATION_MS).toInt()

        return if (frameIndex < boxGoalBitmaps.size) {
            boxGoalBitmaps[frameIndex]
        } else {
            /*
             * Sprite 356의 frame 13에는 Stop 액션이 있으므로
             * 목표 위의 박스는 마지막 붉은 집 프레임을 유지한다.
             */
            boxGoalAnimationStarts.remove(position)
            boxGoalBitmaps.last()
        }
    }

    private fun currentPlayerBitmap(now: Long): Bitmap {
        val successStartedAt = playerSuccessStartedAtMs

        if (successStartedAt != null) {
            val elapsed = (now - successStartedAt).coerceAtLeast(0L)
            val successFrameIndex =
                (elapsed / ORIGINAL_FRAME_DURATION_MS).toInt()

            if (successFrameIndex < playerSuccessBitmaps.size) {
                return playerSuccessBitmaps[successFrameIndex]
            }

            /*
             * Sprite 370은 76프레임 뒤 타임라인 처음으로 돌아간다.
             * 성공 애니메이션이 끝난 시점부터 평상시 1~70 루프를 다시 시작한다.
             */
            playerSuccessStartedAtMs = null
            playerAnimationStartedAtMs =
                successStartedAt +
                    playerSuccessBitmaps.size * ORIGINAL_FRAME_DURATION_MS
        }

        val elapsedMs =
            (now - playerAnimationStartedAtMs).coerceAtLeast(0L)

        val frame =
            ((elapsedMs / ORIGINAL_FRAME_DURATION_MS) % PLAYER_IDLE_FRAME_COUNT)
                .toInt() + 1

        /*
         * 원본 Sprite 370의 1~70 프레임은 방향 애니메이션이 아니다.
         * 10fps로 재생되는 대기/눈 깜빡임 루프이며 70프레임에서
         * gotoFrame(0) + play()로 다시 처음부터 반복한다.
         */
        return when (frame) {
            2, 12, 38 -> playerBlinkHalfBitmap
            3, 13, 39 -> playerBlinkClosedBitmap
            else -> playerBitmap
        }
    }

    private fun decodeEmbeddedBitmap(encoded: String): Bitmap {
        val bytes = Base64.decode(encoded, Base64.DEFAULT)
        return requireNotNull(
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        )
    }

    private fun drawTile(
        canvas: Canvas,
        bitmap: Bitmap,
        destination: RectF
    ) {
        canvas.drawBitmap(
            bitmap,
            sourceRect,
            destination,
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

        return RectF(
            left,
            top,
            left + cell,
            top + cell
        )
    }

    private fun dp(value: Float): Float =
        value * resources.displayMetrics.density

    private companion object {
        const val ORIGINAL_TILE_PX = 14
        const val PLAYER_IDLE_FRAME_COUNT = 70L
        const val ORIGINAL_FRAME_DURATION_MS = 100L

        const val PLAYER_BLINK_HALF_PNG =
            "iVBORw0KGgoAAAANSUhEUgAAAA4AAAAOCAYAAAAfSC3RAAAABmJLR0QA/wD/AP+gvaeTAAAAsUlEQVQokZ2QMRaDIBBE//psLNJzFC0x5xbLeBRvYEkKsixIfL5kGmZ2dwYWoUX8UgOQK2EGP8ExwHAkvb4aj1QmPyV1DDam5jpAusZ0Hi5hMxEgLiFmAPksuWIJSXcaEdZPVIyZaz15a4heuwRrPmexD9pC2wN6AMY5F1S7Me2zF8PaYwvJ6PzEXjzD6Sc8wPm56e1bsKf+ih5seRG55aq7c5KI3HKgNpapVzwH8eeOb12Aa1x/oE/aAAAAAElFTkSuQmCC"

        const val PLAYER_BLINK_CLOSED_PNG =
            "iVBORw0KGgoAAAANSUhEUgAAAA4AAAAOCAYAAAAfSC3RAAAABmJLR0QA/wD/AP+gvaeTAAAAnElEQVQokZ2SOw6DMBBEZ1AaF+l9FCjtg0MJR8kNXDrFesH4g6OMhLS/N6wtE7ViowYAzJOpAC5o9oBbSsOzz6wow6VMALa9+jsfoRxWJRMBn6DOBtN4sq1r1VyzF+dgJD/WDqiDqmBg041+tr3q4VhlVesWKaRPIbwB63yz1171B70AIEZhSQ5jzatbJTmMgfuTu7n24tMIf57xC89mQF/nbBXlAAAAAElFTkSuQmCC"

        val OUTER_BACKGROUND: Int =
            Color.rgb(8, 10, 12)

        val FLOOR_COLOR: Int =
            Color.rgb(255, 255, 255)

        val FRAME_COLOR: Int =
            Color.rgb(98, 103, 108)
    }
}
