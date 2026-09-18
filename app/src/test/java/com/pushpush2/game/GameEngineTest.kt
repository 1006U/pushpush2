package com.pushpush2.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameEngineTest {

    @Test
    fun blockedMoveDoesNotChangeStateOrMoveCount() {
        val stage = Stage.fromAscii(
            number = 1,
            name = "blocked",
            raw = """
                ###
                #@#
                #$.
                ###
            """
        )
        val engine = GameEngine(stage)
        val before = engine.state

        assertFalse(engine.move(Direction.RIGHT))
        assertEquals(before, engine.state)
        assertEquals(0, engine.state.moves)
    }

    @Test
    fun pushingBoxOntoGoalClearsStage() {
        val stage = Stage.fromAscii(
            number = 1,
            name = "clear",
            raw = """
                #####
                #@$.#
                #####
            """
        )
        val engine = GameEngine(stage)

        assertTrue(engine.move(Direction.RIGHT))

        assertEquals(Position(2, 1), engine.state.player)
        assertEquals(setOf(Position(3, 1)), engine.state.boxes)
        assertEquals(1, engine.state.moves)
        assertTrue(engine.state.isCleared)
    }

    @Test
    fun cannotPushBoxIntoAnotherBox() {
        val stage = Stage.fromAscii(
            number = 1,
            name = "double-box",
            raw = """
                #######
                #@$$..#
                #######
            """
        )
        val engine = GameEngine(stage)
        val before = engine.state

        assertFalse(engine.move(Direction.RIGHT))
        assertEquals(before, engine.state)
    }

    @Test
    fun resetRestoresInitialStageState() {
        val stage = Stage.fromAscii(
            number = 1,
            name = "reset",
            raw = """
                ######
                #@ $.#
                ######
            """
        )
        val engine = GameEngine(stage)
        val initial = engine.state

        assertTrue(engine.move(Direction.RIGHT))
        assertTrue(engine.move(Direction.RIGHT))
        assertTrue(engine.state.isCleared)

        engine.reset()

        assertEquals(initial, engine.state)
        assertEquals(0, engine.state.moves)
        assertFalse(engine.state.isCleared)
    }
}
