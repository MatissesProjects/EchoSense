package com.echosense.app

import android.content.Context

object AudioEngineLib {
    init {
        System.loadLibrary("echosense_native")
    }

    external fun startAudioEngine()
    external fun stopAudioEngine()
    external fun isAudioEngineRunning(): Boolean
    
    external fun setInputSource(source: Int)
    external fun setInputDevice(deviceId: Int)
    external fun setRemoteGain(gain: Float)
    external fun writeRemoteAudio(data: FloatArray)
    
    external fun setPreAmpGain(gain: Float)
    external fun setVoiceBoost(gainDb: Float)
    external fun setHpfFreq(freq: Float)
    external fun setLpfFreq(freq: Float)
    external fun setLimiterThreshold(threshold: Float)
    external fun setNoiseGateThreshold(threshold: Float)
    external fun setSpectralReduction(strength: Float)
    external fun setSpectralGateThreshold(threshold: Float)
    external fun setDereverbStrength(strength: Float)
    external fun setHpssStrength(strength: Float)
    external fun setFreqWarpStrength(strength: Float)
    external fun setNeuralMaskStrength(strength: Float)
    external fun setBassBoostStrength(strength: Float)
    external fun setTransientSuppression(strength: Float)
    external fun setMasterGain(gain: Float)
    external fun setProfile(profile: Int)
    external fun setSensorFusion(enabled: Boolean)
    external fun setTargetLock(enabled: Boolean)
    external fun setTargetSpeaker(speakerId: Int)
    external fun setMbCompression(ratio: Float)
    external fun setBeamforming(enabled: Boolean)
    external fun setWindReduction(strength: Float)
    external fun setSelfVoiceSuppression(enabled: Boolean)
    external fun setBluetoothDelayComp(ms: Float)
    external fun setAutoSceneDetection(enabled: Boolean)
    external fun getDetectedScene(): Int
    external fun setTone(freq: Float, volume: Float)
    external fun setEqualizerBandGain(bandIndex: Int, gain: Float)
    external fun getVolumeLevel(): Float
    external fun getIsolationGainDb(): Float
    external fun getDominantMic(): Int
    external fun getFftData(output: FloatArray)
    external fun getEqCurveData(output: FloatArray)
    external fun autoTune()
    external fun learnNoise()

    data class SpeakerInfo(
        val id: Int,
        val energyPhone: Float,
        val energyWatch: Float,
        val isActive: Boolean
    )

    fun getSpeakerInfo(maxSpeakers: Int = 5): List<SpeakerInfo> {
        val ids = IntArray(maxSpeakers)
        val energyPhone = FloatArray(maxSpeakers)
        val energyWatch = FloatArray(maxSpeakers)
        val active = BooleanArray(maxSpeakers)
        
        val count = getSpeakerInfo(ids, energyPhone, energyWatch, active)
        
        return (0 until count).map { i ->
            SpeakerInfo(ids[i], energyPhone[i], energyWatch[i], active[i])
        }
    }

    private external fun getSpeakerInfo(ids: IntArray, energyPhone: FloatArray, energyWatch: FloatArray, active: BooleanArray): Int

    fun restoreSettings(context: Context) {
        val settings = AudioSettingsManager(context)
        setPreAmpGain(settings.getFloat(AudioSettingsManager.KEY_PRE_AMP, 1.0f))
        setVoiceBoost(settings.getFloat(AudioSettingsManager.KEY_VOICE_BOOST, 0.0f))
        setHpfFreq(settings.getFloat("hpf_freq", 150.0f))
        setLpfFreq(settings.getFloat("lpf_freq", 12000.0f))
        setLimiterThreshold(settings.getFloat("limiter_thresh", 0.9f))
        setMasterGain(settings.getFloat(AudioSettingsManager.KEY_MASTER_GAIN, 1.0f))
        setNoiseGateThreshold(settings.getFloat(AudioSettingsManager.KEY_NOISE_GATE, 0.0f))
        setSpectralReduction(settings.getFloat("spectral_reduction", 0.0f))
        setSpectralGateThreshold(settings.getFloat("spectral_gate_thresh", 0.0f))
        setDereverbStrength(settings.getFloat("dereverb_strength", 0.0f))
        setHpssStrength(settings.getFloat("hpss_strength", 0.0f))
        setFreqWarpStrength(settings.getFloat("freq_warp_strength", 0.0f))
        setNeuralMaskStrength(settings.getFloat("neural_mask_strength", 0.0f))
        setBassBoostStrength(settings.getFloat("bass_boost_strength", 0.0f))
        setTransientSuppression(settings.getFloat("transient_suppression", 0.0f))
        setRemoteGain(settings.getFloat(AudioSettingsManager.KEY_WATCH_GAIN, 2.0f))
        setProfile(settings.getInt(AudioSettingsManager.KEY_PROFILE, 3))
        setSensorFusion(settings.prefs.getBoolean("sensor_fusion", false))
        setTargetLock(settings.prefs.getBoolean("target_lock", false))
        setTargetSpeaker(settings.getInt(AudioSettingsManager.KEY_TARGET_SPEAKER, -1))
        setMbCompression(settings.getFloat("mb_compression", 1.0f))
        setBeamforming(settings.prefs.getBoolean("beamforming", false))
        
        for (i in 0 until 5) {
            val gain = settings.getFloat("band_$i", 0.0f)
            setEqualizerBandGain(i, gain)
        }
        
        setInputSource(settings.getInt(AudioSettingsManager.KEY_MIC_SOURCE, 0))
        
        setSelfVoiceSuppression(settings.prefs.getBoolean("self_voice_suppression", false))
        setBluetoothDelayComp(settings.getInt("bt_delay_ms", 0).toFloat())
        setAutoSceneDetection(settings.prefs.getBoolean("auto_scene_detection", false))
    }
}
