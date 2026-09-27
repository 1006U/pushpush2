package com.pushpush2.game

class GameEngine(stage: Stage) {

    var state: GameState = GameState.initial(stage)
        private set

    private val history = ArrayDeque<GameState>()

    fun reset() {
        state = GameState.initial(state.stage)
        history.clear()
    }

    fun load(stage: Stage) {
        state = GameState.initial(stage)
        history.clear()
    }

    fun undo(): Boolean {
        if (history.isEmpty()) return false

        state = history.removeLast()
        return true
    }

    fun move(direction: Direction): Boolean {
        if (state.isCleared) return false

        val next = state.player + direction

        if (!state.stage.contains(next)) return false
        if (next in state.stage.walls) return false

        val boxes = state.boxes.toMutableSet()

        if (next in boxes) {
            val pushed = next + direction

            if (!state.stage.contains(pushed)) return false
            if (pushed in state.stage.walls || pushed in boxes) return false

            boxes.remove(next)
            boxes.add(pushed)
        }

        val previousState = state

        state = state.copy(
            player = next,
            boxes = boxes,
            moves = state.moves + 1
        )
        history.addLast(previousState)
        return true
    }
}
