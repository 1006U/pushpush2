package com.pushpush2.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class StageSolutionRepositoryTest {

    @Test
    fun containsSolutionsForStages1Through50Only() {
        (1..50).forEach { stage ->
            assertNotNull(
                "Missing solution for stage $stage",
                StageSolutionRepository.sourceSolution(stage)
            )
        }

        assertNull(StageSolutionRepository.sourceSolution(51))
        assertNull(StageSolutionRepository.sourceSolution(66))
    }

    @Test
    fun convertsDirectionWordsToArrowsWithoutChangingRepeatCounts() {
        assertEquals(
            "↓-↑-←(2)-→-↑(2)-↓-←(2)",
            StageSolutionRepository.displaySolution(1)
        )
    }

    @Test
    fun supportedRangeMatchesImportedSolutionFile() {
        assertEquals(1..50, StageSolutionRepository.supportedStages())
    }
}
