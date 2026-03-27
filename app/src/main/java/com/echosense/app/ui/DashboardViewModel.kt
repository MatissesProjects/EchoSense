package com.echosense.app.ui

import androidx.lifecycle.ViewModel
import com.echosense.app.AudioEngineLib
import com.echosense.app.AudioSettingsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DashboardViewModel(private val settingsManager: AudioSettingsManager) : ViewModel() {

    private val _preAmpGain = MutableStateFlow(settingsManager.getFloat(AudioSettingsManager.KEY_PRE_AMP, 1.0f))
    val preAmpGain: StateFlow<Float> = _preAmpGain.asStateFlow()

    private val _voiceBoost = MutableStateFlow(settingsManager.getFloat(AudioSettingsManager.KEY_VOICE_BOOST, 0.0f))
    val voiceBoost: StateFlow<Float> = _voiceBoost.asStateFlow()

    private val _masterGain = MutableStateFlow(settingsManager.getFloat(AudioSettingsManager.KEY_MASTER_GAIN, 1.0f))
    val masterGain: StateFlow<Float> = _masterGain.asStateFlow()

    private val _noiseGate = MutableStateFlow(settingsManager.getFloat(AudioSettingsManager.KEY_NOISE_GATE, -60.0f))
    val noiseGate: StateFlow<Float> = _noiseGate.asStateFlow()

    fun updatePreAmp(value: Float) {
        _preAmpGain.value = value
        settingsManager.saveFloat(AudioSettingsManager.KEY_PRE_AMP, value)
        AudioEngineLib.setPreAmpGain(value)
    }

    fun updateVoiceBoost(value: Float) {
        _voiceBoost.value = value
        settingsManager.saveFloat(AudioSettingsManager.KEY_VOICE_BOOST, value)
        AudioEngineLib.setVoiceBoost(value)
    }

    fun updateMasterGain(value: Float) {
        _masterGain.value = value
        settingsManager.saveFloat(AudioSettingsManager.KEY_MASTER_GAIN, value)
        AudioEngineLib.setMasterGain(value)
    }

    fun updateNoiseGate(value: Float) {
        _noiseGate.value = value
        settingsManager.saveFloat(AudioSettingsManager.KEY_NOISE_GATE, value)
        AudioEngineLib.setNoiseGateThreshold(value)
    }

    // Add other parameters as needed...
}
