package com.pushpush2.audio

import android.content.Context
import android.media.MediaPlayer

class AudioPlayer(
    private val context: Context
) {
    private val activePlayers = mutableSetOf<MediaPlayer>()

    fun play(resourceName: String) {
        val resourceId = context.resources.getIdentifier(
            resourceName,
            "raw",
            context.packageName
        )

        if (resourceId == 0) return

        /*
         * MediaPlayer codec support can differ by Android version/emulator image.
         * Sound playback must never be able to terminate the game Activity.
         */
        val player = runCatching {
            MediaPlayer.create(context, resourceId)
        }.getOrNull() ?: return

        activePlayers += player

        player.setOnCompletionListener {
            activePlayers -= it
            runCatching { it.release() }
        }

        player.setOnErrorListener { mediaPlayer, _, _ ->
            activePlayers -= mediaPlayer
            runCatching { mediaPlayer.release() }
            true
        }

        val started = runCatching {
            player.start()
        }.isSuccess

        if (!started) {
            activePlayers -= player
            runCatching { player.release() }
        }
    }

    fun release() {
        activePlayers.toList().forEach {
            runCatching { it.release() }
        }
        activePlayers.clear()
    }
}
