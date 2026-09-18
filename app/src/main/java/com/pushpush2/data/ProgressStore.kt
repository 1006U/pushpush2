package com.pushpush2.data

import android.content.Context
import kotlin.math.max

class ProgressStore(context: Context) {

    private val preferences =
        context.getSharedPreferences("pushpush_progress", Context.MODE_PRIVATE)

    fun highestUnlockedStage(): Int =
        preferences.getInt(KEY_HIGHEST_UNLOCKED, 1)

    fun markCleared(stageNumber: Int, totalStages: Int) {
        val nextUnlocked = (stageNumber + 1).coerceAtMost(totalStages)
        val highest = max(highestUnlockedStage(), nextUnlocked)
        preferences.edit()
            .putInt(KEY_HIGHEST_UNLOCKED, highest)
            .apply()
    }

    fun resetProgress() {
        preferences.edit().clear().apply()
    }

    private companion object {
        const val KEY_HIGHEST_UNLOCKED = "highest_unlocked_stage"
    }
}
