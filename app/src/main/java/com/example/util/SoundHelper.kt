package com.example.util

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

object SoundHelper {
    private var toneGenerator: ToneGenerator? = null
    private var appContext: Context? = null
    private var alertJob: Job? = null

    // How long the new-order alert keeps repeating before it gives up.
    private const val ALERT_MAX_DURATION_MS = 3 * 60 * 1000L
    private const val ALERT_REPEAT_DELAY_MS = 2500L

    // User-configurable settings (persisted in SharedPreferences)
    var soundEnabled: Boolean = true
        private set
    var volume: Int = 95
        private set

    private const val PREFS_NAME = "sound_settings"
    private const val KEY_ENABLED = "sound_enabled"
    private const val KEY_VOLUME = "sound_volume"

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 95)
        } catch (_: Exception) {
            // Graceful fallback if audio stream is unavailable
        }
    }

    fun init(context: Context) {
        appContext = context.applicationContext
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        soundEnabled = prefs.getBoolean(KEY_ENABLED, true)
        volume = prefs.getInt(KEY_VOLUME, 95).coerceIn(0, 100)
        rebuildToneGenerator()
    }

    fun configure(enabled: Boolean, volumeLevel: Int) {
        soundEnabled = enabled
        volume = volumeLevel.coerceIn(0, 100)
        appContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)?.edit()?.apply {
            putBoolean(KEY_ENABLED, soundEnabled)
            putInt(KEY_VOLUME, volume)
            apply()
        }
        rebuildToneGenerator()
    }

    private fun rebuildToneGenerator() {
        try { toneGenerator?.release() } catch (_: Exception) {}
        toneGenerator = if (soundEnabled && volume > 0) {
            try { ToneGenerator(AudioManager.STREAM_NOTIFICATION, volume) } catch (_: Exception) { null }
        } else null
    }

    fun stopAlert() {
        val wasRinging = alertJob != null
        alertJob?.cancel()
        alertJob = null
        if (wasRinging) {
            try { toneGenerator?.stopTone() } catch (_: Exception) {}
            // rebuild so future alerts still work
            rebuildToneGenerator()
        }
    }

    fun playOrderReadyChime(context: Context) {
        if (!soundEnabled) return
        vibrate(context, 350)
        CoroutineScope(Dispatchers.Default).launch {
            try {
                toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200)
                delay(220)
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 300)
                delay(320)
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_PROMPT, 400)
            } catch (_: Exception) {
            }
        }
    }

    /**
     * Repeating alert for a new customer order. It keeps beeping until
     * [stopAlert] is called (admin opens the Orders tab or handles an order),
     * or until [ALERT_MAX_DURATION_MS] has passed.
     */
    fun playNewOrderAlert(context: Context) {
        if (!soundEnabled) return
        // Already ringing: do not stack another loop on top of it.
        if (alertJob?.isActive == true) return
        vibrate(context, 400)
        alertJob = CoroutineScope(Dispatchers.Default).launch {
            val endAt = System.currentTimeMillis() + ALERT_MAX_DURATION_MS
            try {
                while (isActive && System.currentTimeMillis() < endAt) {
                    toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 300)
                    delay(350)
                    toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 300)
                    delay(ALERT_REPEAT_DELAY_MS)
                }
            } catch (_: Exception) {
            }
        }
    }

    /** Plays the alert sound once, for the "ทดสอบเสียง" button in settings. */
    fun playTestAlert(context: Context) {
        if (!soundEnabled) return
        vibrate(context, 300)
        CoroutineScope(Dispatchers.Default).launch {
            try {
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 300)
                delay(350)
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 300)
            } catch (_: Exception) {
            }
        }
    }

    fun playStampSuccessSound(context: Context) {
        if (!soundEnabled) return
        vibrate(context, 100)
        CoroutineScope(Dispatchers.Default).launch {
            try {
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_PROMPT, 180)
                delay(120)
                toneGenerator?.startTone(ToneGenerator.TONE_CDMA_KEYPAD_VOLUME_KEY_LITE, 200)
            } catch (_: Exception) {
            }
        }
    }

    private fun vibrate(context: Context, durationMs: Long) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(durationMs)
                }
            }
        } catch (_: Exception) {
        }
    }
}