package com.pushpush2.game

data class GameState(
    val stage: Stage,
    val player: Position,
    val boxes: Set<Position>,
    val moves: Int = 0
) {
    val isCleared: Boolean
        get() = boxes.isNotEmpty() && boxes.all { it in stage.goals }

    companion object {
        fun initial(stage: Stage): GameState =
            GameState(
                stage = stage,
                player = stage.playerStart,
                boxes = stage.boxes
            )
    }
}
