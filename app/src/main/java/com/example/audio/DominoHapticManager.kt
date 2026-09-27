package com.example.audio

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

enum class HapticType {
    LIGHT_TICK,
    MEDIUM_TAP,
    HEAVY_CLICK,
    SUCCESS_BUZZ,
    ERROR_WARN
}

class DominoHapticManager(context: Context) {
    var isHapticsEnabled: Boolean = true

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    fun trigger(type: HapticType) {
        if (!isHapticsEnabled || vibrator == null || !vibrator.hasVibrator()) return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                when (type) {
                    HapticType.LIGHT_TICK -> {
                        vibrator.vibrate(VibrationEffect.createOneShot(18, 70))
                    }
                    HapticType.MEDIUM_TAP -> {
                        vibrator.vibrate(VibrationEffect.createOneShot(35, 140))
                    }
                    HapticType.HEAVY_CLICK -> {
                        vibrator.vibrate(VibrationEffect.createOneShot(55, 230))
                    }
                    HapticType.SUCCESS_BUZZ -> {
                        val timings = longArrayOf(0, 40, 60, 80)
                        val amplitudes = intArrayOf(0, 120, 0, 200)
                        vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                    }
                    HapticType.ERROR_WARN -> {
                        val timings = longArrayOf(0, 30, 40, 30)
                        val amplitudes = intArrayOf(0, 180, 0, 180)
                        vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                when (type) {
                    HapticType.LIGHT_TICK -> vibrator.vibrate(20)
                    HapticType.MEDIUM_TAP -> vibrator.vibrate(40)
                    HapticType.HEAVY_CLICK -> vibrator.vibrate(65)
                    HapticType.SUCCESS_BUZZ -> vibrator.vibrate(120)
                    HapticType.ERROR_WARN -> vibrator.vibrate(longArrayOf(0, 40, 50, 40), -1)
                }
            }
        } catch (_: Exception) {}
    }
}
