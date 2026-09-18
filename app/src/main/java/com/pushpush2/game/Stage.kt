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
    fun contains(position: Position): Boolean =
        position.x in 0 until width && position.y in 0 until height

    companion object {
        fun fromRows(number: Int, name: String, rows: List<String>): Stage {
            require(rows.isNotEmpty()) { "Stage must contain at least one row." }

            val width = rows.maxOf { it.length }
            val height = rows.size
            val walls = mutableSetOf<Position>()
            val goals = mutableSetOf<Position>()
            val boxes = mutableSetOf<Position>()
            var player: Position? = null

            rows.forEachIndexed { y, line ->
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

            require(player != null) { "Stage $number requires one player (@ or +)." }
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

        fun fromAscii(number: Int, name: String, raw: String): Stage =
            fromRows(
                number = number,
                name = name,
                rows = raw.trimIndent().trim('\n').lines()
            )
    }
}
