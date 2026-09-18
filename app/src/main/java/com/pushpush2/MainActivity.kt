package com.pushpush2

import android.app.Activity
import android.app.AlertDialog
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
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
import com.pushpush2.ui.PlayerCharacterAsset
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
        showHeaderState(
            HeaderState.START,
            resetAfterMs = HEADER_START_MS
        )

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
            background = borderedPanel(Color.WHITE)
            setPadding(dp(6), dp(6), dp(6), dp(6))
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            setImageDrawable(playerPortraitDrawable(PlayerCharacterAsset.IDLE))
            contentDescription = "PushPush character"
        }

        headerMessage = TextView(this).apply {
            background = borderedPanel(Color.WHITE)
            setTextColor(Color.rgb(28, 46, 62))
            textSize = 17f
            gravity = Gravity.CENTER
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            setPadding(dp(8), dp(4), dp(8), dp(4))
            includeFontPadding = false
        }

        headerBar.addView(
            headerCharacter,
            LinearLayout.LayoutParams(
                dp(92),
                dp(78)
            )
        )

        headerBar.addView(
            headerMessage,
            LinearLayout.LayoutParams(
                0,
                dp(78),
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
        val boxesBefore = engine.state.boxes

        if (!engine.move(direction)) return

        val state = engine.state
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
        showHeaderState(
            HeaderState.RETRY,
            resetAfterMs = HEADER_REACTION_MS
        )
    }

    private fun loadStage(number: Int) {
        cancelPendingStageAdvance()
        currentStageNumber = number
        clearHandled = false
        engine.load(StageRepository.get(number))
        gameView.resetPlayerAnimation()
        updateUi()
        showHeaderState(
            HeaderState.START,
            resetAfterMs = HEADER_START_MS
        )
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

        stageLabel.text = "STAGE %02d".format(state.stage.number)
        moveLabel.text = "STEP %03d".format(state.moves)

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
            isFilterBitmap = false
            setAntiAlias(false)
        }
    }

    private fun borderedPanel(fillColor: Int): GradientDrawable =
        GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(fillColor)
            setStroke(dp(2), RETRO_BORDER)
        }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private enum class HeaderState(
        val message: String,
        val sprite: String
    ) {
        START(
            message = "자~!!\n출발~!!",
            sprite = PlayerCharacterAsset.IDLE
        ),
        PLAYING(
            message = "푸시 푸시~!!\n힘내!!",
            sprite = PlayerCharacterAsset.IDLE
        ),
        GOAL_SUCCESS(
            message = "좋아~!!\n그렇지!!",
            sprite = PlayerCharacterAsset.BLINK_HALF
        ),
        STAGE_CLEAR(
            message = "와우~!!\n짠 짠 짠...",
            sprite = PlayerCharacterAsset.IDLE
        ),
        RETRY(
            message = "앗차~!!\n다시 해봐!",
            sprite = PlayerCharacterAsset.BLINK_CLOSED
        ),
        GAME_CLEAR(
            message = "와우~!!\nGAME CLEAR!",
            sprite = PlayerCharacterAsset.IDLE
        )
    }

    private companion object {
        const val KEY_STAGE = "current_stage"

        // 원작 10fps에서 alpha를 단계적으로 낮추는 체감을 살리기 위해
        // 클리어 메시지와 캐릭터 반응이 눈에 보이는 시간까지 확보한다.
        const val STAGE_CLEAR_FADE_MS = 900L
        const val HEADER_REACTION_MS = 900L
        const val HEADER_START_MS = 1200L

        val RETRO_BLUE: Int =
            Color.rgb(45, 132, 218)
        val RETRO_STATUS_BLUE: Int =
            Color.rgb(31, 110, 222)
        val RETRO_BORDER: Int =
            Color.rgb(31, 41, 48)

        val CONTROL_PANEL: Int =
            Color.rgb(184, 193, 202)
    }
}
