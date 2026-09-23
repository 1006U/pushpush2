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
    fun wallLayoutsMatchOriginalSwfExtraction() {
        /*
         * FNV-1a fingerprints captured from the wall coordinates imported by
         * fb88e1483deec5bb6ad0c6121dbb784179d2f2d7
         * (the original game.swf stage_map frames 1..66 extraction).
         *
         * This catches any future missing, added, or moved brick cell across
         * all 66 stages.
         */
        val expected = listOf(
            "963ac731", "3e0bbcb1", "65228d8a", "12cd401d", "b6e98d01",
            "bdc1ef08", "733d21ad", "3de12fa8", "49a40af9", "6a6e0fae",
            "4d91beb3", "8ba6f09e", "c50ee778", "7b175b28", "14d158ef",
            "e16255b6", "01dde74b", "2eb5ee5a", "dee1077c", "57624fe9",
            "298e84cd", "60eb4aea", "28f62754", "b8093e48", "b215fb64",
            "2ce2eb4e", "e6ab486f", "d11faddb", "c7b2af6b", "e674c2af",
            "7ba55f71", "f412e93b", "ffcda08a", "9e194c28", "2e9126be",
            "7a35f040", "a8cc27ac", "594591ad", "19913a53", "75dbfc5a",
            "71b98619", "8cc039c1", "5c0fba9d", "f6892b9c", "7d101f14",
            "9f7b61f0", "4836bd6c", "c844ace3", "709bfa9c", "2ffb0653",
            "c7f441e2", "d19e25e8", "f2c1bbd5", "b7adb1ec", "218e0a42",
            "4519a344", "881299c1", "4d0feed9", "72dca131", "7abab6e9",
            "5cd8351d", "e1a0dc43", "af84cf60", "c211ee96", "a6009def",
            "47ee72f1"
        )

        val actual = StageRepository.stages.map { stage ->
            val canonical = buildString {
                append(stage.number)
                append(':')
                append(
                    stage.walls
                        .sortedWith(compareBy<Position> { it.y }.thenBy { it.x })
                        .joinToString(";") { "${it.x},${it.y}" }
                )
            }

            var hash = 0x811C9DC5u
            canonical.forEach { char ->
                hash = (hash xor char.code.toUInt()) * 0x01000193u
            }
            hash.toString(16).padStart(8, '0')
        }

        assertEquals(
            "One or more wall cells differ from the original SWF extraction",
            expected,
            actual
        )
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
