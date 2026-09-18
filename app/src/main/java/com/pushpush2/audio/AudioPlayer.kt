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

        val player = MediaPlayer.create(context, resourceId) ?: return

        activePlayers += player
        player.setOnCompletionListener {
            activePlayers -= it
            it.release()
        }
        player.setOnErrorListener { mediaPlayer, _, _ ->
            activePlayers -= mediaPlayer
            mediaPlayer.release()
            true
        }
        player.start()
    }

    fun release() {
        activePlayers.toList().forEach {
            it.release()
        }
        activePlayers.clear()
    }
}
