package com.pushpush2.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ResumeStagePolicyTest {

    @Test
    fun restoresLastPlayedStageWhenUnlocked() {
        assertEquals(
            12,
            ResumeStagePolicy.resolve(
                lastPlayedStage = 12,
                highestUnlockedStage = 20,
                totalStages = 66
            )
        )
    }

    @Test
    fun clampsSavedStageToHighestUnlockedStage() {
        assertEquals(
            8,
            ResumeStagePolicy.resolve(
                lastPlayedStage = 30,
                highestUnlockedStage = 8,
                totalStages = 66
            )
        )
    }

    @Test
    fun clampsCorruptValuesToValidRange() {
        assertEquals(
            1,
            ResumeStagePolicy.resolve(
                lastPlayedStage = -4,
                highestUnlockedStage = 0,
                totalStages = 66
            )
        )

        assertEquals(
            66,
            ResumeStagePolicy.resolve(
                lastPlayedStage = 100,
                highestUnlockedStage = 100,
                totalStages = 66
            )
        )
    }
}
