package com.pushpush2.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.os.SystemClock
import android.util.Base64
import android.view.View
import com.pushpush2.R
import com.pushpush2.game.GameState
import com.pushpush2.game.Position
import java.util.ArrayDeque
import kotlin.math.floor
import kotlin.math.min

class GameView(context: Context) : View(context) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        isFilterBitmap = false
    }

    private val endingTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
    }

    private val brickBitmap: Bitmap =
        BitmapFactory.decodeResource(resources, R.drawable.tile_brick)
    private val goalBitmap: Bitmap =
        BitmapFactory.decodeResource(resources, R.drawable.tile_goal)
    private val boxBitmap: Bitmap =
        BitmapFactory.decodeResource(resources, R.drawable.tile_box)
    private val legacyPlayerBitmap: Bitmap =
        BitmapFactory.decodeResource(resources, R.drawable.tile_player)

    private val playerBitmap: Bitmap =
        decodeEmbeddedBitmapOrFallback(
            PlayerCharacterAsset.IDLE,
            legacyPlayerBitmap
        )

    private val playerBlinkHalfBitmap: Bitmap =
        decodeEmbeddedBitmapOrFallback(
            PlayerCharacterAsset.BLINK_HALF,
            playerBitmap
        )

    private val playerBlinkClosedBitmap: Bitmap =
        decodeEmbeddedBitmapOrFallback(
            PlayerCharacterAsset.BLINK_CLOSED,
            playerBitmap
        )

    /*
     * 성공 애니메이션 프레임은 앱 시작 시 한꺼번에 디코딩하지 않는다.
     * 일부 기기/Android 버전에서 BitmapFactory가 특정 프레임을 읽지 못해도
     * Activity 전체가 종료되지 않도록 필요 시점에 lazy 로딩하고 fallback한다.
     */
    private val boxGoalBitmaps: List<Bitmap> by lazy(LazyThreadSafetyMode.NONE) {
        OriginalAnimationFrames.boxGoalPngBase64.map { encoded ->
            makeTileBackgroundTransparent(
                decodeEmbeddedBitmapOrFallback(encoded, boxBitmap)
            )
        }
    }

    private val playerSuccessBitmaps: List<Bitmap> by lazy(LazyThreadSafetyMode.NONE) {
        listOf(
            playerBitmap,
            playerBlinkHalfBitmap,
            playerBlinkClosedBitmap,
            playerBlinkHalfBitmap,
            playerBitmap,
            playerBitmap
        )
    }

    private val sourceRect = Rect(0, 0, ORIGINAL_TILE_PX, ORIGINAL_TILE_PX)

    private var gameState: GameState? = null
    private var playerAnimationStartedAtMs = SystemClock.uptimeMillis()
    private var playerSuccessStartedAtMs: Long? = null
    private var endingStartedAtMs: Long? = null
    private val boxGoalAnimationStarts = mutableMapOf<Position, Long>()

    private val animationTick = object : Runnable {
        override fun run() {
            if (!isAttachedToWindow) return

            invalidate()
            postDelayed(this, ORIGINAL_FRAME_DURATION_MS)
        }
    }

    fun render(state: GameState) {
        val stageChanged =
            gameState?.stage?.number != state.stage.number

        if (stageChanged) {
            resetPlayerAnimation()
        }

        gameState = state

        if (stageChanged) {
            requestLayout()
        }

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

    fun showEnding() {
        val now = SystemClock.uptimeMillis()
        endingStartedAtMs = now
        playerAnimationStartedAtMs = now
        playerSuccessStartedAtMs = null
        boxGoalAnimationStarts.clear()
        invalidate()
    }

    fun resetPlayerAnimation() {
        playerAnimationStartedAtMs = SystemClock.uptimeMillis()
        playerSuccessStartedAtMs = null
        endingStartedAtMs = null
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

    override fun onMeasure(
        widthMeasureSpec: Int,
        heightMeasureSpec: Int
    ) {
        val measuredWidth =
            MeasureSpec.getSize(widthMeasureSpec).coerceAtLeast(1)

        val horizontalPadding = dp(4f)
        val verticalPadding = dp(4f)
        val stage = gameState?.stage

        val desiredHeight =
            if (stage != null && stage.width > 0 && stage.height > 0) {
                val boardWidth =
                    (measuredWidth - horizontalPadding * 2f)
                        .coerceAtLeast(1f)
                val cell = boardWidth / stage.width.toFloat()

                kotlin.math.ceil(
                    stage.height * cell + verticalPadding * 2f
                ).toInt()
            } else {
                measuredWidth
            }

        setMeasuredDimension(
            resolveSize(measuredWidth, widthMeasureSpec),
            resolveSize(desiredHeight, heightMeasureSpec)
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(OUTER_BACKGROUND)

        if (width <= 0 || height <= 0) return

        val now = SystemClock.uptimeMillis()
        val endingStartedAt = endingStartedAtMs

        if (endingStartedAt != null) {
            drawEnding(
                canvas = canvas,
                now = now,
                startedAt = endingStartedAt
            )
            return
        }

        val state = gameState ?: return
        drawStage(canvas, state, now)
    }

    private fun drawStage(
        canvas: Canvas,
        state: GameState,
        now: Long
    ) {
        val stage = state.stage
        val horizontalPadding = dp(4f)
        val verticalPadding = dp(4f)

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
        val cell = integerFriendlyCell(rawCell)

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
        paint.shader = null
        paint.color = FLOOR_COLOR
        canvas.drawRect(boardRect, paint)

        drawFloorDots(
            canvas = canvas,
            boardRect = boardRect,
            cell = cell
        )

        drawInteriorFloor(
            canvas = canvas,
            state = state,
            cell = cell,
            offsetX = offsetX,
            offsetY = offsetY
        )

        /*
         * 원본 게임판 외곽선은 검은/남색 박스처럼 강하게 보이지 않고
         * 바깥 파란 영역과 자연스럽게 이어지는 얇은 파란 선에 가깝다.
         */
        paint.style = Paint.Style.STROKE
        paint.strokeWidth =
            maxOf(1f, (cell / ORIGINAL_TILE_PX) * 0.55f)
        paint.color = FRAME_COLOR
        canvas.drawRect(boardRect, paint)

        drawConnectedWalls(
            canvas = canvas,
            state = state,
            cell = cell,
            offsetX = offsetX,
            offsetY = offsetY
        )

        for (y in 0 until stage.height) {
            for (x in 0 until stage.width) {
                val position = Position(x, y)

                val destination = cellRect(
                    position = position,
                    cell = cell,
                    offsetX = offsetX,
                    offsetY = offsetY
                )

                if (position !in stage.walls && position in stage.goals) {
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

    private fun drawEnding(
        canvas: Canvas,
        now: Long,
        startedAt: Long
    ) {
        val horizontalPadding = dp(12f)
        val verticalPadding = dp(12f)

        val availableWidth =
            (width.toFloat() - horizontalPadding * 2f).coerceAtLeast(1f)
        val availableHeight =
            (height.toFloat() - verticalPadding * 2f).coerceAtLeast(1f)

        val rawCell = min(
            availableWidth / ENDING_SPAN_TILES,
            availableHeight / ENDING_SPAN_TILES
        )
        val cell = integerFriendlyCell(rawCell)

        val centerX = width / 2f
        val centerY = height / 2f - cell * 0.15f

        drawEndingGradientShape(canvas, centerX, centerY, cell)

        ENDING_BRICKS.forEach { point ->
            drawTile(
                canvas = canvas,
                bitmap = brickBitmap,
                destination = endingTileRect(
                    gx = point[0],
                    gy = point[1],
                    cell = cell,
                    centerX = centerX,
                    centerY = centerY
                )
            )
        }

        ENDING_BOXES.forEach { point ->
            drawTile(
                canvas = canvas,
                bitmap = boxBitmap,
                destination = endingTileRect(
                    gx = point[0],
                    gy = point[1],
                    cell = cell,
                    centerX = centerX,
                    centerY = centerY
                )
            )
        }

        drawTile(
            canvas = canvas,
            bitmap = currentPlayerBitmap(now),
            destination = endingTileRect(
                gx = -2.5f,
                gy = 3.5f,
                cell = cell,
                centerX = centerX,
                centerY = centerY
            )
        )

        drawEndingCredits(
            canvas = canvas,
            elapsed = (now - startedAt).coerceAtLeast(0L),
            centerX = centerX,
            centerY = centerY,
            cell = cell
        )
    }

    private fun drawEndingGradientShape(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        cell: Float
    ) {
        fun x(value: Float): Float = centerX + value * cell
        fun y(value: Float): Float = centerY + value * cell

        val path = Path().apply {
            fillType = Path.FillType.EVEN_ODD

            moveTo(x(1.5f), y(-4.5f))
            lineTo(x(1.5f), y(-2.5f))
            lineTo(x(2.5f), y(-2.5f))
            lineTo(x(2.5f), y(-1.5f))
            lineTo(x(3.5f), y(-1.5f))
            lineTo(x(3.5f), y(-0.5f))
            lineTo(x(4.5f), y(-0.5f))
            lineTo(x(4.5f), y(0.5f))
            lineTo(x(3.5f), y(0.5f))
            lineTo(x(3.5f), y(4.5f))
            lineTo(x(-3.5f), y(4.5f))
            lineTo(x(-3.5f), y(0.5f))
            lineTo(x(-4.5f), y(0.5f))
            lineTo(x(-4.5f), y(-0.5f))
            lineTo(x(-3.5f), y(-0.5f))
            lineTo(x(-3.5f), y(-1.5f))
            lineTo(x(-2.5f), y(-1.5f))
            lineTo(x(-2.5f), y(-2.5f))
            lineTo(x(-1.5f), y(-2.5f))
            lineTo(x(-1.5f), y(-3.5f))
            lineTo(x(-0.5f), y(-3.5f))
            lineTo(x(-0.5f), y(-2.5f))
            lineTo(x(0.5f), y(-2.5f))
            lineTo(x(0.5f), y(-4.5f))
            close()

            addRect(
                x(0.5f),
                y(-2.5f),
                x(1.5f),
                y(-1.5f),
                Path.Direction.CW
            )
            addRect(
                x(-2.0f),
                y(0.5f),
                x(0.0f),
                y(2.5f),
                Path.Direction.CW
            )
        }

        paint.style = Paint.Style.FILL
        paint.shader = LinearGradient(
            centerX,
            y(-4.5f),
            centerX,
            y(4.5f),
            ENDING_GRADIENT_TOP,
            ENDING_GRADIENT_BOTTOM,
            Shader.TileMode.CLAMP
        )
        canvas.drawPath(path, paint)
        paint.shader = null
    }

    private fun drawEndingCredits(
        canvas: Canvas,
        elapsed: Long,
        centerX: Float,
        centerY: Float,
        cell: Float
    ) {
        val index =
            ((elapsed / ENDING_CREDIT_CYCLE_MS) % ENDING_CREDITS.size)
                .toInt()
        val inCycle = elapsed % ENDING_CREDIT_CYCLE_MS
        val alphaProgress =
            if (inCycle <= ENDING_CREDIT_FADE_MS) {
                inCycle.toFloat() / ENDING_CREDIT_FADE_MS
            } else {
                1f -
                    (inCycle - ENDING_CREDIT_FADE_MS).toFloat() /
                    ENDING_CREDIT_FADE_MS
            }.coerceIn(0f, 1f)

        val alpha = (alphaProgress * 255f).toInt()
        val credit = ENDING_CREDITS[index]

        endingTextPaint.textSize = cell * 0.57f
        endingTextPaint.color =
            Color.argb(alpha, 0, 153, 255)
        canvas.drawText(
            credit.first,
            centerX,
            centerY - cell * 0.8f,
            endingTextPaint
        )

        endingTextPaint.textSize = cell * 0.43f
        endingTextPaint.color =
            Color.argb(alpha, 255, 255, 255)
        canvas.drawText(
            credit.second,
            centerX,
            centerY + cell * 0.15f,
            endingTextPaint
        )

        endingTextPaint.textSize = cell * 0.42f
        endingTextPaint.color = Color.WHITE
        canvas.drawText(
            "Flash PUSH II v0.95",
            centerX,
            centerY + cell * 5.4f,
            endingTextPaint
        )
    }

    private fun endingTileRect(
        gx: Float,
        gy: Float,
        cell: Float,
        centerX: Float,
        centerY: Float
    ): RectF {
        val left = centerX + gx * cell
        val top = centerY + gy * cell

        return RectF(
            left,
            top,
            left + cell,
            top + cell
        )
    }

    private fun drawFloorDots(
        canvas: Canvas,
        boardRect: RectF,
        cell: Float
    ) {
        /*
         * 원본 베이지 영역은 점 9개가 떠 있는 형태가 아니라,
         * 게임 타일 하나 전체가 3x3 = 9개의 작은 정사각형으로 꽉 나뉜다.
         * 각 작은 칸의 테두리를 보이게 해서 원본의 큐브 단면/LCD 타일 느낌을 낸다.
         */
        val stageColumns =
            (boardRect.width() / cell).toInt().coerceAtLeast(1)
        val stageRows =
            (boardRect.height() / cell).toInt().coerceAtLeast(1)

        val miniSize = cell / 3f
        val gridWidth =
            maxOf(1f, (cell / ORIGINAL_TILE_PX) * 0.72f)

        paint.shader = null
        paint.strokeCap = Paint.Cap.BUTT

        for (tileY in 0 until stageRows) {
            val tileTop = boardRect.top + tileY * cell

            for (tileX in 0 until stageColumns) {
                val tileLeft = boardRect.left + tileX * cell

                for (miniY in 0 until 3) {
                    for (miniX in 0 until 3) {
                        val left = tileLeft + miniX * miniSize
                        val top = tileTop + miniY * miniSize
                        val right = left + miniSize
                        val bottom = top + miniSize

                        paint.style = Paint.Style.FILL
                        paint.color =
                            if ((tileX + tileY + miniX + miniY) % 2 == 0) {
                                FLOOR_TILE_LIGHT
                            } else {
                                FLOOR_TILE_WARM
                            }
                        canvas.drawRect(left, top, right, bottom, paint)

                        paint.style = Paint.Style.STROKE
                        paint.strokeWidth = gridWidth
                        paint.color = FLOOR_TILE_GRID
                        canvas.drawRect(
                            left + gridWidth / 2f,
                            top + gridWidth / 2f,
                            right - gridWidth / 2f,
                            bottom - gridWidth / 2f,
                            paint
                        )
                    }
                }
            }
        }

        paint.style = Paint.Style.FILL
    }

    private fun drawInteriorFloor(
        canvas: Canvas,
        state: GameState,
        cell: Float,
        offsetX: Float,
        offsetY: Float
    ) {
        val floorPositions = playableFloorPositions(state)
        if (floorPositions.isEmpty()) return

        paint.style = Paint.Style.FILL
        paint.shader = null
        paint.color = PLAYFIELD_FLOOR_COLOR

        floorPositions.forEach { position ->
            canvas.drawRect(
                cellRect(
                    position = position,
                    cell = cell,
                    offsetX = offsetX,
                    offsetY = offsetY
                ),
                paint
            )
        }

        // 피처폰 원작의 흰 통로 타일에 보이는 짧은 대각선 무늬.
        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.SQUARE
        paint.strokeWidth =
            maxOf(1f, cell / ORIGINAL_TILE_PX)
        paint.color = PLAYFIELD_DIAGONAL_COLOR

        floorPositions.forEach { position ->
            val rect = cellRect(
                position = position,
                cell = cell,
                offsetX = offsetX,
                offsetY = offsetY
            )

            val slash = cell * 0.18f
            val anchors = floatArrayOf(0.22f, 0.50f, 0.78f)

            anchors.forEachIndexed { index, anchor ->
                val startX = rect.left + cell * anchor
                val startY =
                    rect.top + cell * (0.72f - index * 0.18f)

                canvas.drawLine(
                    startX,
                    startY,
                    startX + slash,
                    startY - slash,
                    paint
                )
            }
        }

        paint.style = Paint.Style.FILL
    }

    /**
     * 벽과 상관없이 플레이어가 움직일 수 있는 연결 영역을 내부 통로로 본다.
     * 박스는 바닥 분류에서는 장애물로 취급하지 않으므로, 퍼즐 진행 중에도
     * 동일한 사선 바닥 모양이 유지된다.
     */
    private fun playableFloorPositions(
        state: GameState
    ): Set<Position> {
        val stage = state.stage
        val walls = stage.walls
        val start = state.player

        if (start in walls) return emptySet()

        val visited = mutableSetOf<Position>()
        val queue = ArrayDeque<Position>()

        visited += start
        queue.add(start)

        val directions = arrayOf(
            0 to -1,
            1 to 0,
            0 to 1,
            -1 to 0
        )

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()

            directions.forEach { (dx, dy) ->
                val next = Position(
                    current.x + dx,
                    current.y + dy
                )

                val inBounds =
                    next.x in 0 until stage.width &&
                        next.y in 0 until stage.height

                if (
                    inBounds &&
                    next !in walls &&
                    next !in visited
                ) {
                    visited += next
                    queue.add(next)
                }
            }
        }

        return visited
    }

    /**
     * 원본 피처폰 화면처럼 인접한 벽 타일을 하나의 벽돌 구조로 이어서 그린다.
     *
     * 각 14x14 셀마다 동일 PNG를 반복하면 셀 경계가 네모난 테두리로 보여
     * 벽이 조각난 느낌이 난다. 전체 스테이지 좌표를 기준으로 벽돌 줄/세로
     * 줄눈만 이어서 그리고, 원본처럼 벽 전체를 감싸는 별도 검은 외곽선은
     * 그리지 않는다.
     */
    private fun drawConnectedWalls(
        canvas: Canvas,
        state: GameState,
        cell: Float,
        offsetX: Float,
        offsetY: Float
    ) {
        val stage = state.stage
        val walls = stage.walls
        if (walls.isEmpty()) return

        val wallPath = Path().apply {
            walls.forEach { position ->
                addRect(
                    cellRect(
                        position = position,
                        cell = cell,
                        offsetX = offsetX,
                        offsetY = offsetY
                    ),
                    Path.Direction.CW
                )
            }
        }

        val boardRight = offsetX + stage.width * cell
        val boardBottom = offsetY + stage.height * cell
        val pixel = (cell / ORIGINAL_TILE_PX).coerceAtLeast(0.75f)
        val brickRowHeight = cell / 2f

        val saveCount = canvas.save()
        canvas.clipPath(wallPath)

        paint.style = Paint.Style.FILL
        paint.shader = null
        paint.color = WALL_RED
        canvas.drawRect(
            offsetX,
            offsetY,
            boardRight,
            boardBottom,
            paint
        )

        // 각 벽돌 줄의 짙은 적갈색 줄눈 + 주황색 하이라이트.
        var row = 0
        var mortarY = offsetY
        while (mortarY <= boardBottom + 0.5f) {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = maxOf(1f, pixel * 0.90f)
            paint.strokeCap = Paint.Cap.BUTT
            paint.color = WALL_MORTAR
            canvas.drawLine(
                offsetX,
                mortarY,
                boardRight,
                mortarY,
                paint
            )

            paint.strokeWidth = maxOf(1f, pixel)
            paint.color = WALL_HIGHLIGHT
            val highlightY = mortarY + pixel * 1.45f
            canvas.drawLine(
                offsetX,
                highlightY,
                boardRight,
                highlightY,
                paint
            )

            // 줄마다 반 칸씩 어긋나는 전형적인 벽돌 패턴.
            val jointOffset =
                if (row % 2 == 0) 0f else cell / 2f
            var jointX = offsetX + jointOffset

            paint.strokeWidth = maxOf(1f, pixel * 0.85f)
            paint.color = WALL_MORTAR

            while (jointX <= boardRight + 0.5f) {
                canvas.drawLine(
                    jointX,
                    mortarY,
                    jointX,
                    (mortarY + brickRowHeight)
                        .coerceAtMost(boardBottom),
                    paint
                )
                jointX += cell
            }

            mortarY += brickRowHeight
            row += 1
        }

        canvas.restoreToCount(saveCount)

        // 원본에는 벽 덩어리를 감싸는 별도의 검은 외곽선이 없으므로
        // clip 안의 벽돌 무늬만 남기고 여기서 추가 테두리는 그리지 않는다.
        paint.style = Paint.Style.FILL
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

    private fun integerFriendlyCell(rawCell: Float): Float =
        /*
         * 원본 14px 타일의 정수배만 강제하면 현대 화면에서 사용할 수 있는
         * 폭을 크게 남기는 경우가 있다. Nearest-neighbor 픽셀아트는 유지하되
         * 실제 셀 크기는 1px 단위로 맞춰 화면을 더 촘촘하게 채운다.
         */
        floor(rawCell)
            .coerceAtLeast(1f)

    /**
     * Removes the opaque square background that exists around the original
     * 14x14 goal/goal-success sprites. Only the border-connected background
     * is cleared, so the house/ball pixels remain intact while the current
     * playfield floor and diagonal pattern show through naturally.
     */
    private fun makeTileBackgroundTransparent(source: Bitmap): Bitmap {
        val width = source.width
        val height = source.height

        if (width <= 0 || height <= 0) return source

        val pixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)

        val borderColors = mutableListOf<Int>()

        fun addBorderColor(x: Int, y: Int) {
            val color = pixels[y * width + x]
            if (Color.alpha(color) > 0) {
                borderColors += color
            }
        }

        for (x in 0 until width) {
            addBorderColor(x, 0)
            addBorderColor(x, height - 1)
        }
        for (y in 1 until height - 1) {
            addBorderColor(0, y)
            addBorderColor(width - 1, y)
        }

        if (borderColors.isEmpty()) return source

        val grouped = borderColors.groupingBy { it }.eachCount()
        val backgroundColor =
            grouped.maxByOrNull { it.value }?.key ?: borderColors.first()

        fun isBackgroundLike(color: Int): Boolean {
            if (Color.alpha(color) == 0) return true

            val dr = kotlin.math.abs(
                Color.red(color) - Color.red(backgroundColor)
            )
            val dg = kotlin.math.abs(
                Color.green(color) - Color.green(backgroundColor)
            )
            val db = kotlin.math.abs(
                Color.blue(color) - Color.blue(backgroundColor)
            )

            return dr <= TILE_BG_TOLERANCE &&
                dg <= TILE_BG_TOLERANCE &&
                db <= TILE_BG_TOLERANCE
        }

        val transparent = BooleanArray(width * height)
        val queue = ArrayDeque<Int>()

        fun enqueueIfBackground(x: Int, y: Int) {
            val index = y * width + x
            if (
                !transparent[index] &&
                isBackgroundLike(pixels[index])
            ) {
                transparent[index] = true
                queue.add(index)
            }
        }

        for (x in 0 until width) {
            enqueueIfBackground(x, 0)
            enqueueIfBackground(x, height - 1)
        }
        for (y in 1 until height - 1) {
            enqueueIfBackground(0, y)
            enqueueIfBackground(width - 1, y)
        }

        while (queue.isNotEmpty()) {
            val index = queue.removeFirst()
            val x = index % width
            val y = index / width

            if (x > 0) enqueueIfBackground(x - 1, y)
            if (x + 1 < width) enqueueIfBackground(x + 1, y)
            if (y > 0) enqueueIfBackground(x, y - 1)
            if (y + 1 < height) enqueueIfBackground(x, y + 1)
        }

        var changed = false
        transparent.forEachIndexed { index, clear ->
            if (clear && Color.alpha(pixels[index]) != 0) {
                pixels[index] = Color.TRANSPARENT
                changed = true
            }
        }

        if (!changed) return source

        return Bitmap.createBitmap(
            pixels,
            width,
            height,
            Bitmap.Config.ARGB_8888
        )
    }

    private fun decodeEmbeddedBitmapOrFallback(
        encoded: String,
        fallback: Bitmap
    ): Bitmap {
        return runCatching {
            val bytes = Base64.decode(encoded, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }.getOrNull() ?: fallback
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
        const val TILE_BG_TOLERANCE = 18

        const val ENDING_SPAN_TILES = 12f
        const val ENDING_CREDIT_FADE_MS = 2000L
        const val ENDING_CREDIT_CYCLE_MS = 4000L

        val ENDING_GRADIENT_TOP: Int =
            Color.rgb(255, 223, 0)
        val ENDING_GRADIENT_BOTTOM: Int =
            Color.rgb(255, 0, 0)

        val ENDING_CREDITS: Array<Pair<String, String>> = arrayOf(
            "Congratulations" to "Game Clear",
            "The Originator" to "Hiroyuki Imabayashi - Socoban",
            "The Original Maker" to "intromobile.com - PUSH II",
            "Copyright" to "SAMSUNG All Right Reserved",
            "Special Thanks" to "chang118",
            "Special Thanks" to "bockdori",
            "Special Thanks" to "yeom1987",
            "Special Thanks" to "& you.",
            "Program Producer" to "ilovecup"
        )

        val ENDING_BRICKS: Array<FloatArray> = arrayOf(
            floatArrayOf(-5.5f, 0.5f),
            floatArrayOf(4.5f, 0.5f),
            floatArrayOf(-1.5f, -4.5f),
            floatArrayOf(-0.5f, -4.5f),
            floatArrayOf(0.5f, -5.5f),
            floatArrayOf(1.5f, -5.5f),
            floatArrayOf(1.5f, -4.5f),
            floatArrayOf(1.5f, -3.5f),
            floatArrayOf(2.5f, -2.5f),
            floatArrayOf(3.5f, -1.5f),
            floatArrayOf(4.5f, -0.5f),
            floatArrayOf(-4.5f, 0.5f),
            floatArrayOf(-5.5f, -0.5f),
            floatArrayOf(-4.5f, -1.5f),
            floatArrayOf(-3.5f, -2.5f),
            floatArrayOf(-2.5f, -3.5f),
            floatArrayOf(-0.5f, -3.5f),
            floatArrayOf(-3.5f, 4.5f),
            floatArrayOf(-4.5f, 1.5f),
            floatArrayOf(-4.5f, 2.5f),
            floatArrayOf(-4.5f, 3.5f),
            floatArrayOf(-4.5f, 4.5f),
            floatArrayOf(-2.5f, 4.5f),
            floatArrayOf(-1.5f, 4.5f),
            floatArrayOf(-0.5f, 4.5f),
            floatArrayOf(3.5f, 0.5f),
            floatArrayOf(3.5f, 1.5f),
            floatArrayOf(3.5f, 2.5f),
            floatArrayOf(3.5f, 3.5f),
            floatArrayOf(3.5f, 4.5f),
            floatArrayOf(2.5f, 4.5f),
            floatArrayOf(1.5f, 4.5f),
            floatArrayOf(0.5f, 4.5f),
            floatArrayOf(0.5f, -2.5f)
        )

        val ENDING_BOXES: Array<FloatArray> = arrayOf(
            floatArrayOf(2.5f, 3.5f),
            floatArrayOf(1.5f, 3.5f),
            floatArrayOf(0.5f, 3.5f),
            floatArrayOf(-0.5f, 3.5f),
            floatArrayOf(1.5f, 2.5f),
            floatArrayOf(0.5f, 2.5f),
            floatArrayOf(-0.5f, 2.5f),
            floatArrayOf(-1.5f, 3.5f),
            floatArrayOf(0.5f, 1.5f)
        )

        // 피처폰 원작 화면처럼 게임판 바깥을 선명한 파란색으로 유지한다.
        val OUTER_BACKGROUND: Int =
            Color.rgb(45, 132, 218)

        // 피처폰 원작의 따뜻한 게임판 배경.
        // 바깥 영역은 한 게임 타일을 3x3 작은 정사각형으로 꽉 채워 그린다.
        val FLOOR_COLOR: Int =
            Color.rgb(255, 232, 188)
        val FLOOR_TILE_LIGHT: Int =
            Color.rgb(255, 232, 188)
        val FLOOR_TILE_WARM: Int =
            Color.rgb(249, 218, 171)
        val FLOOR_TILE_GRID: Int =
            Color.rgb(224, 177, 126)

        // 벽으로 둘러싸인 실제 플레이 통로는 원본처럼 밝은 타일과
        // 연한 적갈색 대각선 무늬로 바깥 배경과 구분한다.
        val PLAYFIELD_FLOOR_COLOR: Int =
            Color.rgb(246, 244, 237)
        val PLAYFIELD_DIAGONAL_COLOR: Int =
            Color.rgb(201, 162, 155)

        // 원본 벽 타일에서 추출한 색상에 맞춘 연결형 벽돌 팔레트.
        val WALL_RED: Int =
            Color.rgb(189, 24, 0)
        val WALL_HIGHLIGHT: Int =
            Color.rgb(222, 121, 0)
        val WALL_MORTAR: Int =
            Color.rgb(112, 32, 18)

        val FRAME_COLOR: Int =
            Color.rgb(42, 115, 196)
    }
}
