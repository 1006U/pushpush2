package com.pushpush2

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.pushpush2.audio.AudioPlayer
import com.pushpush2.data.ProgressStore
import com.pushpush2.game.Direction
import com.pushpush2.game.GameEngine
import com.pushpush2.game.StageRepository
import com.pushpush2.ui.GameView
import com.pushpush2.ui.RetroControlsView
import com.pushpush2.ui.StageSelectView

class MainActivity : Activity() {

    private lateinit var gameView: GameView
    private lateinit var stageLabel: TextView
    private lateinit var moveLabel: TextView
    private lateinit var progressStore: ProgressStore
    private lateinit var audioPlayer: AudioPlayer

    private var currentStageNumber = 1
    private lateinit var engine: GameEngine
    private var clearHandled = false
    private var pendingStageAdvance: Runnable? = null

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

        if (savedInstanceState == null) {
            audioPlayer.play("start")
        }
    }

    override fun onDestroy() {
        cancelPendingStageAdvance()
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
            setBackgroundColor(BACKGROUND)
        }

        val gameShell = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.BLACK)
            setPadding(dp(10), dp(10), dp(10), dp(8))
        }

        val infoBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(6), 0, dp(6), dp(6))
        }

        stageLabel = TextView(this).apply {
            setTextColor(Color.rgb(227, 232, 238))
            textSize = 15f
            typeface = android.graphics.Typeface.MONOSPACE
        }

        moveLabel = TextView(this).apply {
            setTextColor(Color.rgb(163, 174, 187))
            textSize = 13f
            gravity = Gravity.END
            typeface = android.graphics.Typeface.MONOSPACE
        }

        infoBar.addView(
            stageLabel,
            LinearLayout.LayoutParams(0, dp(34), 1f)
        )
        infoBar.addView(
            moveLabel,
            LinearLayout.LayoutParams(dp(120), dp(34))
        )

        gameView = GameView(this)

        gameShell.addView(infoBar)
        gameShell.addView(
            gameView,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
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
        }

        updateUi()

        if (state.isCleared && !clearHandled) {
            clearHandled = true
            progressStore.markCleared(
                stageNumber = currentStageNumber,
                totalStages = StageRepository.stages.size
            )

            val next = currentStageNumber + 1

            if (next <= StageRepository.stages.size) {
                scheduleAutomaticStageAdvance(next)
            } else {
                // 원본은 stage_map frame 67의 엔딩으로 진행한다.
                // 엔딩 화면 이식 전까지 마지막 퍼즐의 클리어 상태를 유지한다.
                audioPlayer.play("clear")
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

            // 원본 stageFade_chk()도 페이드가 끝난 뒤 clear 사운드를 시작하고
            // stage_map의 다음 프레임을 불러온다.
            audioPlayer.play("clear")
            loadStage(nextStage)
        }

        pendingStageAdvance = advance
        gameView.postDelayed(advance, STAGE_CLEAR_FADE_MS)
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
    }

    private fun loadStage(number: Int) {
        cancelPendingStageAdvance()
        currentStageNumber = number
        clearHandled = false
        engine.load(StageRepository.get(number))
        gameView.resetPlayerAnimation()
        updateUi()
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

        stageLabel.text = "STAGE %02d / %02d".format(
            state.stage.number,
            StageRepository.stages.size
        )

        moveLabel.text = "MOVE %03d".format(state.moves)

        gameView.render(state)
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private companion object {
        const val KEY_STAGE = "current_stage"

        // 원본은 stage_map._alpha를 10씩 빠르게 낮춘 뒤 다음 프레임으로 이동한다.
        // Android에서는 짧은 120ms 페이드로 같은 체감을 재현한다.
        const val STAGE_CLEAR_FADE_MS = 120L

        val BACKGROUND: Int = Color.rgb(172, 181, 191)
        val CONTROL_PANEL: Int = Color.rgb(184, 193, 202)
    }
}
