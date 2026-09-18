package com.pushpush2.audio

import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.ToneGenerator

class AudioPlayer(
    private val context: Context
) {
    private val activePlayers = mutableSetOf<MediaPlayer>()

    /*
     * The original WAV resources can be dropped into res/raw later without
     * changing call sites. Until those files are present, a short feature-phone
     * style tone is used so the game never becomes completely silent.
     */
    private val toneGenerator: ToneGenerator? = runCatching {
        ToneGenerator(
            AudioManager.STREAM_MUSIC,
            FALLBACK_TONE_VOLUME
        )
    }.getOrNull()

    fun play(resourceName: String) {
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
         * MediaPlayer codec support can differ by Android version/emulator image.
         * Sound playback must never be able to terminate the game Activity.
         */
        val player = runCatching {
            MediaPlayer.create(context, resourceId)
        }.getOrNull()

        if (player == null) {
            playFallbackTone(resourceName)
            return
        }

        activePlayers += player

        player.setOnCompletionListener {
            activePlayers -= it
            runCatching { it.release() }
        }

        player.setOnErrorListener { mediaPlayer, _, _ ->
            activePlayers -= mediaPlayer
            runCatching { mediaPlayer.release() }
            playFallbackTone(resourceName)
            true
        }

        val started = runCatching {
            player.start()
        }.isSuccess

        if (!started) {
            activePlayers -= player
            runCatching { player.release() }
            playFallbackTone(resourceName)
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
            generator.startTone(tone, durationMs)
        }
    }

    fun release() {
        activePlayers.toList().forEach {
            runCatching { it.release() }
        }
        activePlayers.clear()

        runCatching {
            toneGenerator?.release()
        }
    }

    private companion object {
        const val FALLBACK_TONE_VOLUME = 72
    }
}
