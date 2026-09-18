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
import android.widget.Toast
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
    }

    override fun onDestroy() {
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
        if (!engine.move(direction)) return

        audioPlayer.play("move")
        updateUi()

        if (engine.state.isCleared && !clearHandled) {
            clearHandled = true
            progressStore.markCleared(
                stageNumber = currentStageNumber,
                totalStages = StageRepository.stages.size
            )

            audioPlayer.play("clear")
            Toast.makeText(this, "STAGE CLEAR!", Toast.LENGTH_SHORT).show()

            val next = currentStageNumber + 1

            AlertDialog.Builder(this)
                .setTitle("STAGE CLEAR!")
                .setMessage("${engine.state.moves}번 이동으로 클리어했습니다.")
                .setNegativeButton("현재 화면", null)
                .apply {
                    if (next <= StageRepository.stages.size) {
                        setPositiveButton("다음 스테이지") { _, _ ->
                            audioPlayer.play("button")
                            loadStage(next)
                        }
                    }
                }
                .show()
        }
    }

    private fun restartStage() {
        clearHandled = false
        engine.reset()
        updateUi()
    }

    private fun loadStage(number: Int) {
        currentStageNumber = number
        clearHandled = false
        engine.load(StageRepository.get(number))
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

        val BACKGROUND: Int = Color.rgb(172, 181, 191)
        val CONTROL_PANEL: Int = Color.rgb(184, 193, 202)
    }
}
