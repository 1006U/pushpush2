package com.pushpush2.data

/**
 * Pure policy for choosing a safe stage when restoring the last-played value.
 */
internal object ResumeStagePolicy {

    fun resolve(
        lastPlayedStage: Int,
        highestUnlockedStage: Int,
        totalStages: Int
    ): Int {
        if (totalStages <= 0) return 1

        val maxAllowed =
            highestUnlockedStage.coerceIn(1, totalStages)

        return lastPlayedStage.coerceIn(1, maxAllowed)
    }
}
