package com.pushpush2.audio

import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.ToneGenerator

class AudioPlayer(
    private val context: Context
) {
    /*
     * Keep only one MediaPlayer alive at a time.
     *
     * Short game effects are triggered frequently and some older Android
     * devices can run out of decoder resources when multiple MediaPlayers
     * overlap. A new effect therefore replaces the previous one.
     */
    private var currentPlayer: MediaPlayer? = null

    private val toneGenerator: ToneGenerator? = runCatching {
        ToneGenerator(
            AudioManager.STREAM_MUSIC,
            FALLBACK_TONE_VOLUME
        )
    }.getOrNull()

    fun play(resourceName: String) {
        stopCurrentPlayback()

        val resourceId = context.resources.getIdentifier(
            resourceName,
            "raw",
            context.packageName
        )

        if (resourceId == 0) {
            playFallbackTone(resourceName)
            return
        }

        /*
         * MediaPlayer codec support can differ by Android version/emulator
         * image. Sound playback must never terminate the game Activity.
         */
        val player = runCatching {
            MediaPlayer.create(context, resourceId)
        }.getOrNull()

        if (player == null) {
            playFallbackTone(resourceName)
            return
        }

        currentPlayer = player

        player.setOnCompletionListener { completed ->
            if (currentPlayer === completed) {
                currentPlayer = null
            }
            runCatching { completed.release() }
        }

        player.setOnErrorListener { failed, _, _ ->
            if (currentPlayer === failed) {
                currentPlayer = null
            }
            runCatching { failed.release() }
            playFallbackTone(resourceName)
            true
        }

        val started = runCatching {
            player.start()
        }.isSuccess

        if (!started) {
            if (currentPlayer === player) {
                currentPlayer = null
            }
            runCatching { player.release() }
            playFallbackTone(resourceName)
        }
    }

    private fun stopCurrentPlayback() {
        runCatching {
            toneGenerator?.stopTone()
        }

        val player = currentPlayer ?: return
        currentPlayer = null

        runCatching {
            player.setOnCompletionListener(null)
            player.setOnErrorListener(null)
        }
        runCatching {
            if (player.isPlaying) {
                player.stop()
            }
        }
        runCatching {
            player.release()
        }
    }

    private fun playFallbackTone(resourceName: String) {
        val generator = toneGenerator ?: return

        val (tone, durationMs) = when (resourceName) {
            "start" -> ToneGenerator.TONE_PROP_PROMPT to 190
            "move" -> ToneGenerator.TONE_PROP_BEEP2 to 45
            "success" -> ToneGenerator.TONE_PROP_ACK to 180
            "clear" -> ToneGenerator.TONE_PROP_ACK to 360
            "button" -> ToneGenerator.TONE_PROP_BEEP to 55
            else -> ToneGenerator.TONE_PROP_BEEP to 50
        }

        runCatching {
            generator.stopTone()
            generator.startTone(tone, durationMs)
        }
    }

    fun release() {
        stopCurrentPlayback()

        runCatching {
            toneGenerator?.release()
        }
    }

    private companion object {
        const val FALLBACK_TONE_VOLUME = 72
    }
}
