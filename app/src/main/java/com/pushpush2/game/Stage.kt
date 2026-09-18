package com.pushpush2.game

data class Stage(
    val number: Int,
    val name: String,
    val width: Int,
    val height: Int,
    val walls: Set<Position>,
    val goals: Set<Position>,
    val boxes: Set<Position>,
    val playerStart: Position
) {
    companion object {
        fun fromAscii(number: Int, name: String, raw: String): Stage {
            val lines = raw.trimIndent()
                .trim('\n')
                .lines()

            require(lines.isNotEmpty()) { "Stage must contain at least one row." }

            val width = lines.maxOf { it.length }
            val height = lines.size
            val walls = mutableSetOf<Position>()
            val goals = mutableSetOf<Position>()
            val boxes = mutableSetOf<Position>()
            var player: Position? = null

            lines.forEachIndexed { y, line ->
                line.forEachIndexed { x, c ->
                    val p = Position(x, y)
                    when (c) {
                        '#' -> walls += p
                        '.' -> goals += p
                        '$' -> boxes += p
                        '@' -> player = p
                        '*' -> {
                            goals += p
                            boxes += p
                        }
                        '+' -> {
                            goals += p
                            player = p
                        }
                    }
                }
            }

            require(player != null) { "Stage requires one player (@ or +)." }
            require(boxes.size == goals.size) {
                "Stage $number must have the same number of boxes and goals."
            }

            return Stage(
                number = number,
                name = name,
                width = width,
                height = height,
                walls = walls,
                goals = goals,
                boxes = boxes,
                playerStart = player!!
            )
        }
    }
}
