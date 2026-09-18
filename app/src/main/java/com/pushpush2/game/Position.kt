package com.pushpush2.game

data class Position(
    val x: Int,
    val y: Int
) {
    operator fun plus(direction: Direction): Position =
        Position(x + direction.dx, y + direction.dy)
}
