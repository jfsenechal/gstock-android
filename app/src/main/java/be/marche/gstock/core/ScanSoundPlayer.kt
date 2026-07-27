package be.marche.gstock.core

import android.media.AudioManager
import android.media.ToneGenerator
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Plays a short "beep" to give audible feedback when a QR code is scanned successfully.
 *
 * Uses [ToneGenerator] so no sound asset is needed. Construction can throw on some devices
 * (audio resources unavailable), so it is guarded and silently no-ops when unavailable.
 */
@Singleton
class ScanSoundPlayer @Inject constructor() {

    private val toneGenerator: ToneGenerator? = runCatching {
        ToneGenerator(AudioManager.STREAM_NOTIFICATION, TONE_VOLUME)
    }.getOrNull()

    /** Emit a short confirmation beep. Safe to call when audio is unavailable. */
    fun playSuccess() {
        toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, TONE_DURATION_MS)
    }

    private companion object {
        const val TONE_VOLUME = 80 // 0..100
        const val TONE_DURATION_MS = 150
    }
}
