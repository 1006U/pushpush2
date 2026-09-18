package com.pushpush2

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.pushpush2.data.ProgressStore
import com.pushpush2.game.Direction
import com.pushpush2.game.GameEngine
import com.pushpush2.game.StageRepository
import com.pushpush2.ui.GameView

class MainActivity : Activity() {

    private lateinit var gameView: GameView
    private lateinit var stageLabel: TextView
    private lateinit var moveLabel: TextView
    private lateinit var progressStore: ProgressStore

    private var currentStageNumber = 1
    private lateinit var engine: GameEngine
    private var clearHandled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        progressStore = ProgressStore(this)
        currentStageNumber = savedInstanceState?.getInt(KEY_STAGE, 1) ?: 1
        currentStageNumber =
            currentStageNumber.coerceAtMost(progressStore.highestUnlockedStage())

        engine = GameEngine(StageRepository.get(currentStageNumber))
        setContentView(buildContentView())
        updateUi()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(KEY_STAGE, currentStageNumber)
        super.onSaveInstanceState(outState)
    }

    private fun buildContentView(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(17, 17, 17))
            setPadding(dp(12), dp(10), dp(12), dp(14))
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        stageLabel = TextView(this).apply {
            setTextColor(Color.WHITE)
            textSize = 20f
        }

        moveLabel = TextView(this).apply {
            setTextColor(Color.LTGRAY)
            textSize = 14f
            gravity = Gravity.END
        }

        header.addView(stageLabel, LinearLayout.LayoutParams(0, dp(48), 1f))
        header.addView(moveLabel, LinearLayout.LayoutParams(dp(78), dp(48)))
        header.addView(
            button("스테이지") { showStageSelector() },
            LinearLayout.LayoutParams(dp(88), dp(48))
        )
        header.addView(
            button("↻") { restartStage() },
            LinearLayout.LayoutParams(dp(58), dp(48))
        )

        gameView = GameView(this)

        root.addView(header)
        root.addView(
            gameView,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )
        root.addView(buildDPad())

        return root
    }

    private fun buildDPad(): View {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(0, dp(10), 0, 0)
        }

        val top = LinearLayout(this).apply {
            gravity = Gravity.CENTER
        }
        top.addView(
            button("▲") { move(Direction.UP) },
            LinearLayout.LayoutParams(dp(76), dp(58))
        )

        val middle = LinearLayout(this).apply {
            gravity = Gravity.CENTER
        }
        middle.addView(
            button("◀") { move(Direction.LEFT) },
            LinearLayout.LayoutParams(dp(76), dp(58))
        )
        middle.addView(
            button("▼") { move(Direction.DOWN) },
            LinearLayout.LayoutParams(dp(76), dp(58))
        )
        middle.addView(
            button("▶") { move(Direction.RIGHT) },
            LinearLayout.LayoutParams(dp(76), dp(58))
        )

        container.addView(top)
        container.addView(middle)
        return container
    }

    private fun button(text: String, onClick: () -> Unit): Button =
        Button(this).apply {
            this.text = text
            textSize = 16f
            setOnClickListener { onClick() }
        }

    private fun move(direction: Direction) {
        if (!engine.move(direction)) return

        updateUi()

        if (engine.state.isCleared && !clearHandled) {
            clearHandled = true
            progressStore.markCleared(
                stageNumber = currentStageNumber,
                totalStages = StageRepository.stages.size
            )

            Toast.makeText(this, "STAGE CLEAR!", Toast.LENGTH_SHORT).show()

            val next = currentStageNumber + 1
            if (next <= StageRepository.stages.size) {
                AlertDialog.Builder(this)
                    .setTitle("STAGE CLEAR!")
                    .setMessage("${engine.state.moves}번 이동으로 클리어했습니다.")
                    .setNegativeButton("계속 보기", null)
                    .setPositiveButton("다음 스테이지") { _, _ ->
                        loadStage(next)
                    }
                    .show()
            }
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

        val choices = StageRepository.stages.map { stage ->
            val marker = if (stage.number <= unlocked) "✓" else "🔒"
            "$marker STAGE ${stage.number}  ${stage.name}"
        }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("스테이지 선택")
            .setItems(choices) { dialog, which ->
                val stage = StageRepository.stages[which]
                if (stage.number <= unlocked) {
                    loadStage(stage.number)
                    dialog.dismiss()
                } else {
                    Toast.makeText(
                        this,
                        "이전 스테이지를 먼저 클리어하세요.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .show()
    }

    private fun updateUi() {
        val state = engine.state
        stageLabel.text = "STAGE ${state.stage.number}"
        moveLabel.text = "MOVE ${state.moves}"
        gameView.render(state)
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private companion object {
        const val KEY_STAGE = "current_stage"
    }
}
