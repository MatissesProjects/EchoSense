package com.echosense.app.utils

import kotlin.math.roundToInt

object AudioParameterMapper {

    /**
     * Maps seek bar progress (0-100) to PreAmp Gain (0.0 to 10.0)
     */
    fun progressToPreAmpGain(progress: Int): Float = progress / 10.0f

    /**
     * Maps seek bar progress (0-100) to Voice Boost Gain dB (0.0 to 30.0)
     */
    fun progressToVoiceBoost(progress: Int): Float = progress * 0.3f

    /**
     * Maps seek bar progress (0-100) to Master Gain (0.0 to 10.0)
     */
    fun progressToMasterGain(progress: Int): Float = progress / 10.0f

    /**
     * Maps seek bar progress (0-100) to HPF Frequency (50Hz to 550Hz)
     */
    fun progressToHpfFreq(progress: Int): Float = progress * 5.0f + 50.0f

    /**
     * Maps seek bar progress (0-100) to LPF Frequency (1kHz to 20kHz)
     */
    fun progressToLpfFreq(progress: Int): Float = progress * 190.0f + 1000.0f

    /**
     * Maps seek bar progress (0-100) to Limiter Threshold (0.0 to 1.0)
     */
    fun progressToLimiterThreshold(progress: Int): Float = progress / 100.0f

    /**
     * Maps seek bar progress (0-100) to Transient Suppression (0.0 to 1.0)
     */
    fun progressToTransientSuppression(progress: Int): Float = progress / 100.0f

    /**
     * Maps seek bar progress (0-100) to Noise Gate Threshold (0.0 to 0.5)
     */
    fun progressToNoiseGateThreshold(progress: Int): Float = progress / 200.0f

    /**
     * Maps seek bar progress (0-100) to De-reverberation Strength (0.0 to 1.0)
     */
    fun progressToDereverb(progress: Int): Float = progress / 100.0f

    /**
     * Maps seek bar progress (0-100) to HPSS Strength (0.0 to 1.0)
     */
    fun progressToHpss(progress: Int): Float = progress / 100.0f

    /**
     * Maps seek bar progress (0-100) to Neural Mask Strength (0.0 to 1.0)
     */
    fun progressToNeuralMask(progress: Int): Float = progress / 100.0f

    /**
     * Maps seek bar progress (0-100) to Bass Boost Strength (0.0 to 1.0)
     */
    fun progressToBassBoost(progress: Int): Float = progress / 100.0f

    /**
     * Maps seek bar progress (0-100) to Multi-band Compression Ratio (1.0 to 11.0)
     */
    fun progressToMbCompressionRatio(progress: Int): Float = 1.0f + (progress / 10.0f)

    /**
     * Maps seek bar progress (0-100) to Equalizer Band Gain dB (-12.0 to +12.0)
     */
    fun progressToEqBandGain(progress: Int): Float = (progress - 100) * 0.12f

    /**
     * Maps seek bar progress (0-100) to Watch Gain (0.0 to 5.0)
     */
    fun progressToWatchGain(progress: Int): Float = progress / 20.0f

    /**
     * Inverse mapping: Float to Seek Bar Progress
     */
    fun preAmpGainToProgress(gain: Float): Int = (gain * 10).roundToInt()
    fun voiceBoostToProgress(gainDb: Float): Int = (gainDb / 0.3f).roundToInt()
    fun masterGainToProgress(gain: Float): Int = (gain * 10).roundToInt()
    fun hpfFreqToProgress(freq: Float): Int = ((freq - 50.0f) / 5.0f).roundToInt()
    fun lpfFreqToProgress(freq: Float): Int = ((freq - 1000.0f) / 190.0f).roundToInt()
    fun limiterThresholdToProgress(thresh: Float): Int = (thresh * 100).roundToInt()
    fun transientSuppressionToProgress(strength: Float): Int = (strength * 100).roundToInt()
    fun noiseGateThresholdToProgress(thresh: Float): Int = (thresh * 200).roundToInt()
    fun dereverbToProgress(strength: Float): Int = (strength * 100).roundToInt()
    fun hpssToProgress(strength: Float): Int = (strength * 100).roundToInt()
    fun neuralMaskToProgress(strength: Float): Int = (strength * 100).roundToInt()
    fun bassBoostToProgress(strength: Float): Int = (strength * 100).roundToInt()
    fun mbCompressionRatioToProgress(ratio: Float): Int = ((ratio - 1.0f) * 10).roundToInt()
    fun eqBandGainToProgress(gainDb: Float): Int = (gainDb / 0.12f + 100).roundToInt()
    fun watchGainToProgress(gain: Float): Int = (gain * 20).roundToInt()
}
