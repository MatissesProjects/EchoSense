package com.echosense.app.utils

import com.echosense.app.AudioEngineLib

object HearingTestCalibration {

    fun generateCurve(
        thresholds: FloatArray,
        setGain: (Int, Float) -> Unit = AudioEngineLib::setEqualizerBandGain
    ) {
        // Simple "Half-Gain Rule" for EQ
        // If threshold is high (e.g. 0.1 vs 0.0001), apply more gain.
        // Thresholds are linear volume. Convert to dB loss.
        // Assume 0.0001 is "perfect" (0dB) and 1.0 is "severe" (60dB+)
        
        for (i in thresholds.indices) {
            val lossDb = 20.0f * Math.log10((thresholds[i] / 0.0001f).toDouble()).toFloat()
            val gainDb = lossDb * 0.5f // Half-gain rule
            
            // Map to the 5 bands we have (200, 500, 1500, 3000, 6000)
            // This is a rough mapping
            when (i) {
                0 -> setGain(0, gainDb.coerceIn(0f, 15f)) // 250 -> 200 band
                1 -> setGain(1, gainDb.coerceIn(0f, 15f)) // 500 -> 500 band
                2 -> {} // 1000 Hz between bands
                3 -> setGain(2, gainDb.coerceIn(0f, 15f)) // 2000 -> 1500 band
                4 -> setGain(3, gainDb.coerceIn(0f, 15f)) // 4000 -> 3000 band
                5 -> setGain(4, gainDb.coerceIn(0f, 15f)) // 8000 -> 6000 band
            }
        }
    }
}
