package com.pushpush2.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StageRepositoryTest {

    @Test
    fun containsAll66OriginalStagesInOrder() {
        val stages = StageRepository.stages

        assertEquals(66, stages.size)
        assertEquals((1..66).toList(), stages.map { it.number })
    }

    @Test
    fun everyStageHasConsistentInBoundsEntities() {
        StageRepository.stages.forEach { stage ->
            assertTrue("Stage ${stage.number} width must be positive", stage.width > 0)
            assertTrue("Stage ${stage.number} height must be positive", stage.height > 0)
            assertTrue(
                "Stage ${stage.number} player must be inside the board",
                stage.contains(stage.playerStart)
            )
            assertFalse(
                "Stage ${stage.number} player must not start inside a wall",
                stage.playerStart in stage.walls
            )

            assertTrue(
                "Stage ${stage.number} must contain at least one box",
                stage.boxes.isNotEmpty()
            )
            assertEquals(
                "Stage ${stage.number} must keep box/goal counts equal",
                stage.goals.size,
                stage.boxes.size
            )

            stage.walls.forEach { position ->
                assertTrue(
                    "Stage ${stage.number} wall outside board: $position",
                    stage.contains(position)
                )
            }
            stage.goals.forEach { position ->
                assertTrue(
                    "Stage ${stage.number} goal outside board: $position",
                    stage.contains(position)
                )
                assertFalse(
                    "Stage ${stage.number} goal overlaps wall: $position",
                    position in stage.walls
                )
            }
            stage.boxes.forEach { position ->
                assertTrue(
                    "Stage ${stage.number} box outside board: $position",
                    stage.contains(position)
                )
                assertFalse(
                    "Stage ${stage.number} box overlaps wall: $position",
                    position in stage.walls
                )
            }
        }
    }

    @Test
    fun stagesDoNotStartAlreadyCleared() {
        StageRepository.stages.forEach { stage ->
            assertFalse(
                "Stage ${stage.number} unexpectedly starts cleared",
                GameState.initial(stage).isCleared
            )
        }
    }
}
