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
import android.view.View
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
import com.pushpush2.ui.StageSelectView

class MainActivity : Activity() {

    private lateinit var gameView: GameView
    private lateinit var stageLabel: TextView
    private lateinit var moveLabel: TextView
    private lateinit var headerCharacter: ImageView
    private lateinit var headerMessage: TextView
    private lateinit var progressStore: ProgressStore
    private lateinit var audioPlayer: AudioPlayer

    private var currentStageNumber = 1
    private lateinit var engine: GameEngine
    private var clearHandled = false
    private var pendingStageAdvance: Runnable? = null
    private var pendingHeaderReset: Runnable? = null
    private var lastWallVibrationAt = 0L

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

        if (savedInstanceState == null) {
            audioPlayer.play("start")
        }
    }

    override fun onDestroy() {
        cancelPendingStageAdvance()
        cancelHeaderReset()
        audioPlayer.release()
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(KEY_STAGE, currentStageNumber)
        super.onSaveInstanceState(outState)
    }

    private fun buildContentView(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(RETRO_BLUE)
        }

        val gameShell = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(RETRO_BLUE)
            setPadding(dp(6), dp(6), dp(6), dp(6))
        }

        val headerBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(RETRO_BLUE)
            setPadding(dp(2), dp(2), dp(2), dp(6))
        }

        headerCharacter = ImageView(this).apply {
            background = brickPanel(Color.WHITE)
            setPadding(dp(8), dp(8), dp(8), dp(8))
            scaleType = ImageView.ScaleType.FIT_CENTER
            setImageDrawable(playerPortraitDrawable(HeaderCharacterAsset.MOVE))
            contentDescription = "PushPush character"
        }

        headerMessage = TextView(this).apply {
            background = brickPanel(Color.WHITE)
            setTextColor(Color.rgb(28, 46, 62))
            textSize = 18f
            gravity = Gravity.CENTER
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            setPadding(dp(14), dp(10), dp(14), dp(10))
            includeFontPadding = false
        }

        headerBar.addView(
            headerCharacter,
            LinearLayout.LayoutParams(
                dp(132),
                dp(92)
            )
        )

        headerBar.addView(
            headerMessage,
            LinearLayout.LayoutParams(
                0,
                dp(92),
                1f
            ).apply {
                marginStart = dp(6)
            }
        )

        gameView = GameView(this)

        val statusBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(RETRO_STATUS_BLUE)
            setPadding(dp(10), dp(2), dp(10), dp(2))
        }

        stageLabel = TextView(this).apply {
            setTextColor(Color.WHITE)
            textSize = 18f
            gravity = Gravity.START or Gravity.CENTER_VERTICAL
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            includeFontPadding = false
        }

        moveLabel = TextView(this).apply {
            setTextColor(Color.WHITE)
            textSize = 18f
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            includeFontPadding = false
        }

        statusBar.addView(
            stageLabel,
            LinearLayout.LayoutParams(0, dp(42), 1f)
        )
        statusBar.addView(
            moveLabel,
            LinearLayout.LayoutParams(0, dp(42), 1f)
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
                0,
                1f
            )
        )

        gameShell.addView(
            statusBar,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        val controlsPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(CONTROL_PANEL)
            setPadding(dp(8), dp(2), dp(8), dp(8))
        }

        val controls = RetroControlsView(this).apply {
            onDirection = { direction -> move(direction) }
            onStageClick = {
                audioPlayer.play("button")
                showStageSelector()
            }
            onRetryClick = {
                audioPlayer.play("button")
                restartStage()
            }
        }

        controlsPanel.addView(
            controls,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(216)
            )
        )

        root.addView(
            gameShell,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )
        root.addView(
            controlsPanel,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        return root
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

        audioPlayer.play("move")

        if (boxEnteredGoal != null) {
            audioPlayer.play("success")
            gameView.playGoalSuccess(boxEnteredGoal)
            showHeaderState(
                HeaderState.GOAL_SUCCESS,
                resetAfterMs = HEADER_REACTION_MS
            )
        } else if (boxMoved) {
            showHeaderState(
                HeaderState.PUSH,
                resetAfterMs = HEADER_REACTION_MS
            )
        } else {
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
            loadStage(nextStage)
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
            gameView.showEnding()
            stageLabel.text = "GAME CLEAR"
            moveLabel.text = ""
            showHeaderState(HeaderState.GAME_CLEAR)
        }

        pendingStageAdvance = ending
        gameView.postDelayed(ending, STAGE_CLEAR_FADE_MS)
    }

    private fun cancelPendingStageAdvance() {
        pendingStageAdvance?.let { gameView.removeCallbacks(it) }
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

    private fun loadStage(number: Int) {
        cancelPendingStageAdvance()
        currentStageNumber = number
        clearHandled = false
        engine.load(StageRepository.get(number))
        gameView.resetPlayerAnimation()
        updateUi()
        showHeaderState(HeaderState.PLAYING)
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

        return BitmapDrawable(resources, bitmap).apply {
            // Header art is a separate remastered asset and is intentionally
            // filtered when scaled into the larger feature-phone style panel.
            isFilterBitmap = true
            setAntiAlias(true)
        }
    }

    private fun brickPanel(fillColor: Int): RetroBrickFrameDrawable =
        RetroBrickFrameDrawable(
            fillColor = fillColor,
            borderWidthPx = dp(8).toFloat()
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

        // 원작 10fps에서 alpha를 단계적으로 낮추는 체감을 살리기 위해
        // 클리어 메시지와 캐릭터 반응이 눈에 보이는 시간까지 확보한다.
        const val STAGE_CLEAR_FADE_MS = 900L
        const val HEADER_REACTION_MS = 900L
        const val WALL_VIBRATION_MS = 55L
        const val WALL_VIBRATION_COOLDOWN_MS = 140L

        val RETRO_BLUE: Int =
            Color.rgb(45, 132, 218)
        val RETRO_STATUS_BLUE: Int =
            Color.rgb(31, 110, 222)
        val CONTROL_PANEL: Int =
            Color.rgb(184, 193, 202)
    }
}
