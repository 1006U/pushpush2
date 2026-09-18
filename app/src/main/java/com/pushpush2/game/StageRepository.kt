package com.pushpush2.game

/**
 * 초기 개발용 스테이지.
 *
 * 원본 SWF 분석이 끝나는 대로 이 목록을 실제 스테이지 데이터로 교체한다.
 */
object StageRepository {

    val stages: List<Stage> = listOf(
        Stage.fromAscii(
            number = 1,
            name = "First Push",
            raw = """
                #######
                #     #
                # .$@ #
                #     #
                #######
            """
        ),
        Stage.fromAscii(
            number = 2,
            name = "Two Boxes",
            raw = """
                ########
                #      #
                # .  . #
                # $$   #
                #  @   #
                ########
            """
        ),
        Stage.fromAscii(
            number = 3,
            name = "Corner Lesson",
            raw = """
                ########
                #      #
                # .##  #
                #  $   #
                #  $ . #
                #   @  #
                ########
            """
        )
    )

    fun get(number: Int): Stage =
        stages.firstOrNull { it.number == number } ?: stages.first()
}
