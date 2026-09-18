package com.pushpush2.game

class GameEngine(stage: Stage) {

    var state: GameState = GameState.initial(stage)
        private set

    fun reset() {
        state = GameState.initial(state.stage)
    }

    fun load(stage: Stage) {
        state = GameState.initial(stage)
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

        state = state.copy(
            player = next,
            boxes = boxes,
            moves = state.moves + 1
        )
        return true
    }
}
