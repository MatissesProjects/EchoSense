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
    external fun setMasterGain(gain: Float)
    external fun setProfile(profile: Int)
    external fun setSensorFusion(enabled: Boolean)
    external fun setEqualizerBandGain(bandIndex: Int, gain: Float)
    
    external fun getVolumeLevel(): Float
    external fun getFftData(output: FloatArray)
    external fun getEqCurveData(output: FloatArray)
    external fun autoTune()

    fun restoreSettings(context: Context) {
        val settings = AudioSettingsManager(context)
        setPreAmpGain(settings.getFloat(AudioSettingsManager.KEY_PRE_AMP, 1.0f))
        setVoiceBoost(settings.getFloat(AudioSettingsManager.KEY_VOICE_BOOST, 0.0f))
        setHpfFreq(settings.getFloat("hpf_freq", 150.0f))
        setLpfFreq(settings.getFloat("lpf_freq", 12000.0f))
        setLimiterThreshold(settings.getFloat("limiter_thresh", 0.9f))
        setMasterGain(settings.getFloat(AudioSettingsManager.KEY_MASTER_GAIN, 1.0f))
        setNoiseGateThreshold(settings.getFloat(AudioSettingsManager.KEY_NOISE_GATE, 0.0f))
        setRemoteGain(settings.getFloat(AudioSettingsManager.KEY_WATCH_GAIN, 2.0f))
        setProfile(settings.getInt(AudioSettingsManager.KEY_PROFILE, 3))
        setSensorFusion(settings.prefs.getBoolean("sensor_fusion", false))
        
        for (i in 0 until 5) {
            val gain = settings.getFloat("band_$i", 0.0f)
            setEqualizerBandGain(i, gain)
        }
        
        setInputSource(settings.getInt(AudioSettingsManager.KEY_MIC_SOURCE, 0))
    }
}
