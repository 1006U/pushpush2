package com.pushpush2

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Base64
import android.view.Gravity
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.pushpush2.audio.AudioPlayer
import com.pushpush2.data.ProgressStore
import com.pushpush2.game.Direction
import com.pushpush2.game.GameEngine
import com.pushpush2.game.StageRepository
import com.pushpush2.ui.GameView
import com.pushpush2.ui.HeaderCharacterAsset
import com.pushpush2.ui.RetroBrickFrameDrawable
import com.pushpush2.ui.RetroControlsView
import com.pushpush2.ui.StageLayoutPolicy
import com.pushpush2.ui.StageSelectView

class MainActivity : Activity() {

    private lateinit var gameView: GameView
    private lateinit var stageLabel: TextView
    private lateinit var moveLabel: TextView
    private lateinit var headerCharacter: ImageView
    private lateinit var headerMessage: TextView
    private lateinit var rootView: LinearLayout
    private lateinit var gameShell: LinearLayout
    private lateinit var controlsPanel: LinearLayout
    private lateinit var controlsView: RetroControlsView
    private lateinit var headerBar: LinearLayout
    private lateinit var statusBar: LinearLayout
    private lateinit var gameClearScreenView: ImageView
    private lateinit var progressStore: ProgressStore
    private lateinit var audioPlayer: AudioPlayer

    private var currentStageNumber = 1
    private lateinit var engine: GameEngine
    private var clearHandled = false
    private var pendingStageAdvance: Runnable? = null
    private var pendingHeaderReset: Runnable? = null
    private var lastWallVibrationAt = 0L
    private var heldGamepadDirection: Direction? = null
    private var showingGameClearScreen = false

    private val gamepadRepeatRunnable = object : Runnable {
        override fun run() {
            val direction = heldGamepadDirection ?: return
            move(direction)

            if (::gameView.isInitialized) {
                gameView.postDelayed(
                    this,
                    GAMEPAD_REPEAT_INTERVAL_MS
                )
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        progressStore = ProgressStore(this)
        audioPlayer = AudioPlayer(this)

        currentStageNumber = savedInstanceState?.getInt(KEY_STAGE, 1) ?: 1
        currentStageNumber =
            currentStageNumber.coerceAtMost(progressStore.highestUnlockedStage())

        engine = GameEngine(StageRepository.get(currentStageNumber))
        setContentView(buildContentView())
        updateUi()
        showHeaderState(HeaderState.PLAYING)

        val shouldShowGameClear =
            savedInstanceState?.getBoolean(KEY_GAME_CLEAR_SCREEN, false) ?: false

        if (shouldShowGameClear) {
            showGameClearScreen()
        }
    }

    override fun onPause() {
        stopGamepadRepeat()
        super.onPause()
    }

    override fun onDestroy() {
        cancelPendingStageAdvance()
        cancelHeaderReset()
        stopGamepadRepeat()
        audioPlayer.release()
        super.onDestroy()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (showingGameClearScreen) {
            if (
                event.action == KeyEvent.ACTION_DOWN &&
                event.repeatCount == 0 &&
                isConfirmKey(event.keyCode)
            ) {
                returnToStageOne()
            }
            return true
        }

        if (isHardwareControlKey(event.keyCode)) {
            if (event.action == KeyEvent.ACTION_DOWN) {
                handleHardwareKeyDown(event)
            }
            return true
        }

        return super.dispatchKeyEvent(event)
    }

    override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
        if (showingGameClearScreen) {
            return true
        }

        if (
            event.action == MotionEvent.ACTION_MOVE &&
            isGamepadMotion(event)
        ) {
            handleGamepadMotion(event)
            return true
        }

        return super.dispatchGenericMotionEvent(event)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(KEY_STAGE, currentStageNumber)
        outState.putBoolean(KEY_GAME_CLEAR_SCREEN, showingGameClearScreen)
        super.onSaveInstanceState(outState)
    }

    private fun buildContentView(): View {
        rootView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)

            setOnApplyWindowInsetsListener { view, insets ->
                val topInset =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        insets.getInsets(
                            WindowInsets.Type.statusBars()
                        ).top
                    } else {
                        @Suppress("DEPRECATION")
                        insets.systemWindowInsetTop
                    }

                view.setPadding(
                    0,
                    topInset,
                    0,
                    0
                )

                insets
            }

            post { requestApplyInsets() }
        }

        gameShell = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(RETRO_BLUE)

            // 원본처럼 상단 캐릭터/문구 UI가 게임 영역 좌우 끝까지
            // 꽉 차도록 좌우 패딩을 두지 않는다. 아래쪽 여백만 유지한다.
            setPadding(0, 0, 0, 0)
        }

        headerBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(RETRO_BLUE)
            setPadding(0, 0, 0, dp(4))
        }

        headerCharacter = ImageView(this).apply {
            background = brickPanel(
                fillColor = Color.WHITE,
                horizontalBands = false
            )
            setPadding(0, 0, 0, 0)
            scaleType = ImageView.ScaleType.FIT_CENTER
            setImageDrawable(playerPortraitDrawable(HeaderCharacterAsset.MOVE))
            contentDescription = "PushPush character"
        }

        headerMessage = TextView(this).apply {
            background = brickPanel(
                fillColor = Color.WHITE,
                horizontalBands = true
            )
            setTextColor(Color.rgb(28, 46, 62))
            textSize = 26f
            textScaleX = 1.04f
            gravity = Gravity.CENTER
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            setPadding(dp(4), dp(2), dp(4), dp(2))
            includeFontPadding = false
        }

        // 원본 상단 UI 비율: 캐릭터 절반 / 문구 절반.
        headerBar.addView(
            headerCharacter,
            LinearLayout.LayoutParams(
                0,
                dp(104),
                1f
            )
        )

        headerBar.addView(
            headerMessage,
            LinearLayout.LayoutParams(
                0,
                dp(104),
                1f
            )
        )

        gameView = GameView(this)

        statusBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(RETRO_STATUS_BLUE)
            setPadding(dp(6), dp(2), dp(6), dp(2))
        }

        stageLabel = TextView(this).apply {
            setTextColor(Color.WHITE)
            textSize = 28f
            textScaleX = 1.10f
            gravity = Gravity.START or Gravity.CENTER_VERTICAL
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            includeFontPadding = false
        }

        moveLabel = TextView(this).apply {
            setTextColor(Color.WHITE)
            textSize = 28f
            textScaleX = 1.10f
            gravity = Gravity.START or Gravity.CENTER_VERTICAL
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            includeFontPadding = false
        }

        statusBar.addView(
            stageLabel,
            LinearLayout.LayoutParams(0, dp(60), 0.56f)
        )
        statusBar.addView(
            moveLabel,
            LinearLayout.LayoutParams(0, dp(60), 0.44f)
        )

        gameShell.addView(
            headerBar,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        gameShell.addView(
            gameView,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        gameShell.addView(
            statusBar,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        controlsPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(CONTROL_PANEL)
            setPadding(dp(8), dp(2), dp(8), dp(8))
        }

        controlsView = RetroControlsView(this).apply {
            onDirection = { direction ->
                if (!showingGameClearScreen) {
                    move(direction)
                }
            }
            onStageClick = {
                if (!showingGameClearScreen) {
                    audioPlayer.play("button")
                    showStageSelector()
                }
            }
            onRetryClick = {
                if (!showingGameClearScreen) {
                    audioPlayer.play("button")
                    restartStage()
                }
            }
            onCenterClick = {
                if (showingGameClearScreen) {
                    returnToStageOne()
                }
            }
            onExitClick = {
                audioPlayer.play("button")
                finishAndRemoveTask()
            }
        }

        controlsPanel.addView(
            controlsView,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        )

        gameClearScreenView = ImageView(this).apply {
            setBackgroundColor(Color.BLACK)
            setImageResource(R.drawable.game_clear_screen)
            scaleType = ImageView.ScaleType.FIT_CENTER
            adjustViewBounds = false
            visibility = View.GONE
            isClickable = false
            isFocusable = false
            contentDescription = "Push Push 2 game clear screen"
        }

        rootView.addView(
            gameClearScreenView,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        rootView.addView(
            gameShell,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )
        rootView.addView(
            controlsPanel,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        controlsPanel.minimumHeight =
            dp(MIN_CONTROLS_VIEW_HEIGHT_DP) +
                controlsPanel.paddingTop +
                controlsPanel.paddingBottom

        rootView.addOnLayoutChangeListener {
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                _,
                _ ->
            applyStageResponsiveLayout()
        }

        return rootView
    }

    private fun showGameClearScreen() {
        cancelPendingStageAdvance()
        cancelHeaderReset()
        stopGamepadRepeat()

        showingGameClearScreen = true

        gameShell.visibility = View.GONE
        gameClearScreenView.visibility = View.VISIBLE
        controlsPanel.visibility = View.VISIBLE
    }

    private fun returnToStageOne() {
        if (!showingGameClearScreen) return

        pendingStageAdvance?.let { gameClearScreenView.removeCallbacks(it) }
        pendingStageAdvance = null
        showingGameClearScreen = false

        gameClearScreenView.visibility = View.GONE
        gameShell.visibility = View.VISIBLE
        controlsPanel.visibility = View.VISIBLE

        loadStage(1)
    }

    private fun isConfirmKey(keyCode: Int): Boolean =
        when (keyCode) {
            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_NUMPAD_ENTER,
            KeyEvent.KEYCODE_SPACE,
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_BUTTON_A,
            KeyEvent.KEYCODE_BUTTON_START -> true

            else -> false
        }

    private fun isHardwareControlKey(keyCode: Int): Boolean =
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_W,
            KeyEvent.KEYCODE_A,
            KeyEvent.KEYCODE_S,
            KeyEvent.KEYCODE_D,
            KeyEvent.KEYCODE_R,
            KeyEvent.KEYCODE_M,
            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_NUMPAD_ENTER,
            KeyEvent.KEYCODE_BUTTON_B,
            KeyEvent.KEYCODE_BUTTON_Y,
            KeyEvent.KEYCODE_BUTTON_START,
            KeyEvent.KEYCODE_BUTTON_SELECT -> true

            else -> false
        }

    private fun handleHardwareKeyDown(event: KeyEvent) {
        when (event.keyCode) {
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_W -> move(Direction.UP)

            KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_S -> move(Direction.DOWN)

            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_A -> move(Direction.LEFT)

            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_D -> move(Direction.RIGHT)

            KeyEvent.KEYCODE_R,
            KeyEvent.KEYCODE_BUTTON_B,
            KeyEvent.KEYCODE_BUTTON_Y -> {
                if (event.repeatCount == 0) {
                    audioPlayer.play("button")
                    restartStage()
                }
            }

            KeyEvent.KEYCODE_M,
            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_NUMPAD_ENTER,
            KeyEvent.KEYCODE_BUTTON_START,
            KeyEvent.KEYCODE_BUTTON_SELECT -> {
                if (event.repeatCount == 0) {
                    audioPlayer.play("button")
                    showStageSelector()
                }
            }
        }
    }

    private fun isGamepadMotion(event: MotionEvent): Boolean {
        val source = event.source

        return (
            source and InputDevice.SOURCE_JOYSTICK
            ) == InputDevice.SOURCE_JOYSTICK ||
            (
                source and InputDevice.SOURCE_GAMEPAD
                ) == InputDevice.SOURCE_GAMEPAD
    }

    private fun handleGamepadMotion(event: MotionEvent) {
        val hatX = event.getAxisValue(MotionEvent.AXIS_HAT_X)
        val hatY = event.getAxisValue(MotionEvent.AXIS_HAT_Y)
        val stickX = event.getAxisValue(MotionEvent.AXIS_X)
        val stickY = event.getAxisValue(MotionEvent.AXIS_Y)

        val x = if (kotlin.math.abs(hatX) >= GAMEPAD_DEAD_ZONE) {
            hatX
        } else {
            stickX
        }

        val y = if (kotlin.math.abs(hatY) >= GAMEPAD_DEAD_ZONE) {
            hatY
        } else {
            stickY
        }

        val direction = when {
            kotlin.math.abs(x) < GAMEPAD_DEAD_ZONE &&
                kotlin.math.abs(y) < GAMEPAD_DEAD_ZONE -> null

            kotlin.math.abs(x) > kotlin.math.abs(y) ->
                if (x < 0f) Direction.LEFT else Direction.RIGHT

            else ->
                if (y < 0f) Direction.UP else Direction.DOWN
        }

        updateHeldGamepadDirection(direction)
    }

    private fun updateHeldGamepadDirection(direction: Direction?) {
        if (direction == heldGamepadDirection) return

        stopGamepadRepeat()

        if (direction == null) return

        heldGamepadDirection = direction
        move(direction)

        if (::gameView.isInitialized) {
            gameView.postDelayed(
                gamepadRepeatRunnable,
                GAMEPAD_INITIAL_REPEAT_DELAY_MS
            )
        }
    }

    private fun stopGamepadRepeat() {
        if (::gameView.isInitialized) {
            gameView.removeCallbacks(gamepadRepeatRunnable)
        }
        heldGamepadDirection = null
    }

    private fun move(direction: Direction) {
        val before = engine.state
        val next = before.player + direction

        if (next in before.stage.walls) {
            vibrateWallBlocked()
            return
        }

        val boxesBefore = before.boxes.toSet()

        if (!engine.move(direction)) return

        val state = engine.state
        val boxMoved = state.boxes != boxesBefore
        val boxEnteredGoal =
            (state.boxes - boxesBefore)
                .firstOrNull { it in state.stage.goals }

        // A goal push is one action. Do not start move.mp3 and success.mp3
        // back-to-back: overlapping MediaPlayer decoders caused audio to stop
        // on some devices immediately after a box entered a goal.
        if (boxEnteredGoal != null) {
            audioPlayer.play("success")
            gameView.playGoalSuccess(boxEnteredGoal)
            showHeaderState(
                HeaderState.GOAL_SUCCESS,
                resetAfterMs = HEADER_REACTION_MS
            )
        } else if (boxMoved) {
            audioPlayer.play("move")
            showHeaderState(
                HeaderState.PUSH,
                resetAfterMs = HEADER_REACTION_MS
            )
        } else {
            audioPlayer.play("move")
            showHeaderState(HeaderState.MOVE)
        }

        updateUi()

        if (state.isCleared && !clearHandled) {
            clearHandled = true
            progressStore.markCleared(
                stageNumber = currentStageNumber,
                totalStages = StageRepository.stages.size
            )

            showHeaderState(HeaderState.STAGE_CLEAR)

            val next = currentStageNumber + 1

            if (next <= StageRepository.stages.size) {
                scheduleAutomaticStageAdvance(next)
            } else {
                scheduleEnding()
            }
        }
    }

    private fun vibrateWallBlocked() {
        val now = SystemClock.uptimeMillis()
        if (now - lastWallVibrationAt < WALL_VIBRATION_COOLDOWN_MS) {
            return
        }
        lastWallVibrationAt = now

        val vibrator: Vibrator? =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                getSystemService(VibratorManager::class.java)
                    ?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

        if (vibrator?.hasVibrator() != true) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(
                    WALL_VIBRATION_MS,
                    VibrationEffect.DEFAULT_AMPLITUDE
                )
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(WALL_VIBRATION_MS)
        }
    }

    private fun scheduleAutomaticStageAdvance(nextStage: Int) {
        cancelPendingStageAdvance()

        gameView.animate()
            .alpha(0f)
            .setDuration(STAGE_CLEAR_FADE_MS)
            .start()

        val advance = Runnable {
            pendingStageAdvance = null
            gameView.animate().cancel()
            gameView.alpha = 1f

            audioPlayer.play("clear")
            loadStage(
                number = nextStage,
                preserveHeaderState = true
            )
        }

        pendingStageAdvance = advance
        gameView.postDelayed(advance, STAGE_CLEAR_FADE_MS)
    }

    private fun scheduleEnding() {
        cancelPendingStageAdvance()

        gameView.animate()
            .alpha(0f)
            .setDuration(STAGE_CLEAR_FADE_MS)
            .start()

        val ending = Runnable {
            pendingStageAdvance = null
            gameView.animate().cancel()
            gameView.alpha = 1f

            audioPlayer.play("clear")
            showGameClearScreen()
        }

        pendingStageAdvance = ending
        gameView.postDelayed(ending, STAGE_CLEAR_FADE_MS)
    }

    private fun cancelPendingStageAdvance() {
        pendingStageAdvance?.let { runnable ->
            if (::gameView.isInitialized) {
                gameView.removeCallbacks(runnable)
            }
            if (::gameClearScreenView.isInitialized) {
                gameClearScreenView.removeCallbacks(runnable)
            }
        }
        pendingStageAdvance = null

        if (::gameView.isInitialized) {
            gameView.animate().cancel()
            gameView.alpha = 1f
        }
    }

    private fun restartStage() {
        cancelPendingStageAdvance()
        clearHandled = false
        engine.reset()
        gameView.resetPlayerAnimation()
        updateUi()
        showHeaderState(HeaderState.PLAYING)
    }

    private fun loadStage(
        number: Int,
        preserveHeaderState: Boolean = false
    ) {
        cancelPendingStageAdvance()
        currentStageNumber = number
        clearHandled = false
        engine.load(StageRepository.get(number))
        gameView.resetPlayerAnimation()
        updateUi()

        /*
         * After an automatic stage clear transition, keep the previous
         * "오~예~~ / 앗싸~~!!" message and clear-reaction portrait visible
         * over the beginning of the next stage. The first successful move
         * naturally replaces it with MOVE / PUSH / GOAL_SUCCESS.
         *
         * Manual stage selection, retry and returning to stage 1 still reset
         * the header immediately.
         */
        if (!preserveHeaderState) {
            showHeaderState(HeaderState.PLAYING)
        }
    }

    private fun showStageSelector() {
        val unlocked = progressStore.highestUnlockedStage()
            .coerceAtMost(StageRepository.stages.size)

        var dialog: AlertDialog? = null

        val stageGrid = StageSelectView(
            context = this,
            totalStages = StageRepository.stages.size,
            unlockedStages = unlocked,
            currentStage = currentStageNumber
        ) { selectedStage ->
            audioPlayer.play("button")
            loadStage(selectedStage)
            dialog?.dismiss()
        }.apply {
            setPadding(dp(8), dp(8), dp(8), dp(12))
        }

        val scrollView = ScrollView(this).apply {
            isFillViewport = true
            setBackgroundColor(CONTROL_PANEL)
            addView(
                stageGrid,
                android.widget.FrameLayout.LayoutParams(
                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                    android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
                )
            )
        }

        dialog = AlertDialog.Builder(this)
            .setTitle("STAGE SELECT")
            .setView(scrollView)
            .setNegativeButton("닫기", null)
            .create()

        dialog.show()
    }

    private fun updateUi() {
        val state = engine.state

        stageLabel.text = "STAGE ${state.stage.number}"
        moveLabel.text = "STEP ${state.moves}"

        gameView.render(state)

        if (::rootView.isInitialized) {
            rootView.post {
                applyStageResponsiveLayout()
            }
        }
    }

    private fun applyStageResponsiveLayout() {
        if (
            !::rootView.isInitialized ||
            !::gameView.isInitialized ||
            !::controlsPanel.isInitialized ||
            !::controlsView.isInitialized ||
            !::headerBar.isInitialized ||
            !::statusBar.isInitialized
        ) {
            return
        }

        val width = rootView.width
        val contentHeight =
            rootView.height -
                rootView.paddingTop -
                rootView.paddingBottom

        if (width <= 0 || contentHeight <= 0) return
        if (headerBar.measuredHeight <= 0 || statusBar.measuredHeight <= 0) return

        val stage = engine.state.stage
        val controlsPanelPadding =
            controlsPanel.paddingTop + controlsPanel.paddingBottom

        val allocation = StageLayoutPolicy.allocate(
            contentWidthPx = width,
            contentHeightPx = contentHeight,
            stageWidth = stage.width,
            stageHeight = stage.height,
            fixedChromeHeightPx =
                headerBar.measuredHeight +
                    statusBar.measuredHeight +
                    gameShell.paddingTop +
                    gameShell.paddingBottom,
            boardHorizontalInsetPx = dp(4),
            boardVerticalInsetPx = dp(4),
            controlsPanelVerticalPaddingPx = controlsPanelPadding,
            minControlsHeightPx = dp(MIN_CONTROLS_VIEW_HEIGHT_DP)
        )

        val gameParams = gameView.layoutParams
        if (gameParams.height != allocation.boardHeightPx) {
            gameParams.height = allocation.boardHeightPx
            gameView.layoutParams = gameParams
        }

        controlsPanel.minimumHeight =
            dp(MIN_CONTROLS_VIEW_HEIGHT_DP) +
                controlsPanelPadding

        controlsView.requestLayout()
    }

    private fun showHeaderState(
        state: HeaderState,
        resetAfterMs: Long? = null
    ) {
        cancelHeaderReset()

        if (::headerMessage.isInitialized) {
            headerMessage.text = state.message
        }

        if (::headerCharacter.isInitialized) {
            headerCharacter.setImageDrawable(
                playerPortraitDrawable(state.sprite)
            )
        }

        if (resetAfterMs != null) {
            val reset = Runnable {
                pendingHeaderReset = null
                showHeaderState(HeaderState.PLAYING)
            }

            pendingHeaderReset = reset
            headerMessage.postDelayed(reset, resetAfterMs)
        }
    }

    private fun cancelHeaderReset() {
        pendingHeaderReset?.let { runnable ->
            if (::headerMessage.isInitialized) {
                headerMessage.removeCallbacks(runnable)
            }
        }
        pendingHeaderReset = null
    }

    private fun playerPortraitDrawable(encoded: String): BitmapDrawable {
        val fallback = BitmapFactory.decodeResource(
            resources,
            R.drawable.tile_player
        )

        val bitmap = runCatching {
            val bytes = Base64.decode(
                encoded,
                Base64.DEFAULT
            )
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }.getOrNull() ?: fallback

        val portraitBitmap =
            cropPortraitOuterWhitespace(bitmap)

        return BitmapDrawable(resources, portraitBitmap).apply {
            // Header art is a separate remastered asset and is intentionally
            // filtered when scaled into the larger feature-phone style panel.
            isFilterBitmap = true
            setAntiAlias(true)
        }
    }

    private fun cropPortraitOuterWhitespace(
        bitmap: android.graphics.Bitmap
    ): android.graphics.Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= 1 || height <= 1) return bitmap

        val pixels = IntArray(width * height)
        bitmap.getPixels(
            pixels,
            0,
            width,
            0,
            0,
            width,
            height
        )

        var minX = width
        var minY = height
        var maxX = -1
        var maxY = -1

        for (y in 0 until height) {
            for (x in 0 until width) {
                val color = pixels[y * width + x]
                val alpha = Color.alpha(color)
                val red = Color.red(color)
                val green = Color.green(color)
                val blue = Color.blue(color)

                val outerBackground =
                    alpha < 24 ||
                    (red >= 242 && green >= 242 && blue >= 242)

                if (!outerBackground) {
                    minX = minOf(minX, x)
                    minY = minOf(minY, y)
                    maxX = maxOf(maxX, x)
                    maxY = maxOf(maxY, y)
                }
            }
        }

        if (maxX < minX || maxY < minY) return bitmap

        val padding = 2
        minX = (minX - padding).coerceAtLeast(0)
        minY = (minY - padding).coerceAtLeast(0)
        maxX = (maxX + padding).coerceAtMost(width - 1)
        maxY = (maxY + padding).coerceAtMost(height - 1)

        val cropWidth = maxX - minX + 1
        val cropHeight = maxY - minY + 1

        if (cropWidth >= width && cropHeight >= height) {
            return bitmap
        }

        return android.graphics.Bitmap.createBitmap(
            bitmap,
            minX,
            minY,
            cropWidth,
            cropHeight
        )
    }

    private fun brickPanel(
        fillColor: Int,
        horizontalBands: Boolean
    ): RetroBrickFrameDrawable =
        RetroBrickFrameDrawable(
            fillColor = fillColor,
            borderWidthPx = dp(8).toFloat(),
            horizontalBands = horizontalBands
        )

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private enum class HeaderState(
        val message: String,
        val sprite: String
    ) {
        PLAYING(
            message = "헛! 둘~!\n헛! 둘~!",
            sprite = HeaderCharacterAsset.MOVE
        ),
        MOVE(
            message = "헛! 둘~!\n헛! 둘~!",
            sprite = HeaderCharacterAsset.MOVE
        ),
        PUSH(
            message = "영 차~!\n영 차~!",
            sprite = HeaderCharacterAsset.START
        ),
        GOAL_SUCCESS(
            message = "와우~!!\n짝 짝 짝 ..",
            sprite = HeaderCharacterAsset.GOAL_SUCCESS
        ),
        STAGE_CLEAR(
            message = "오~예~~\n앗싸~~!!",
            sprite = HeaderCharacterAsset.STAGE_CLEAR
        ),
        RETRY(
            message = "헛! 둘~!\n헛! 둘~!",
            sprite = HeaderCharacterAsset.MOVE
        ),
        GAME_CLEAR(
            message = "오~예~~\n앗싸~~!!",
            sprite = HeaderCharacterAsset.STAGE_CLEAR
        )
    }

    private companion object {
        const val KEY_STAGE = "current_stage"
        const val KEY_GAME_CLEAR_SCREEN = "showing_game_clear_screen"

        // 원작 10fps에서 alpha를 단계적으로 낮추는 체감을 살리기 위해
        // 클리어 메시지와 캐릭터 반응이 눈에 보이는 시간까지 확보한다.
        const val STAGE_CLEAR_FADE_MS = 900L
        const val HEADER_REACTION_MS = 900L
        const val WALL_VIBRATION_MS = 55L
        const val WALL_VIBRATION_COOLDOWN_MS = 140L
        const val GAMEPAD_INITIAL_REPEAT_DELAY_MS = 280L
        const val GAMEPAD_REPEAT_INTERVAL_MS = 110L
        const val GAMEPAD_DEAD_ZONE = 0.55f
        const val MIN_CONTROLS_VIEW_HEIGHT_DP = 148

        val RETRO_BLUE: Int =
            Color.rgb(45, 132, 218)
        val RETRO_STATUS_BLUE: Int =
            Color.rgb(31, 110, 222)
        val CONTROL_PANEL: Int =
            Color.rgb(184, 193, 202)
    }
}
