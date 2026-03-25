package com.echosense.app

import android.content.Context
import android.content.SharedPreferences

class AudioSettingsManager(
    context: Context?,
    val prefs: SharedPreferences = context!!.getSharedPreferences("audio_settings", Context.MODE_PRIVATE)
) {
    companion object {
        const val KEY_PRE_AMP = "pre_amp"
        const val KEY_VOICE_BOOST = "voice_boost"
        const val KEY_MASTER_GAIN = "master_gain"
        const val KEY_NOISE_GATE = "noise_gate"
        const val KEY_BAND_1 = "band_1"
        const val KEY_BAND_2 = "band_2"
        const val KEY_BAND_3 = "band_3"
        const val KEY_BAND_4 = "band_4"
        const val KEY_BAND_5 = "band_5"
        const val KEY_WATCH_GAIN = "watch_gain"
        const val KEY_PROFILE = "audio_profile"
        const val KEY_MIC_SOURCE = "mic_source"
        const val KEY_TARGET_SPEAKER = "target_speaker"
    }

    fun saveFloat(key: String, value: Float) {
        prefs.edit().putFloat(key, value).apply()
    }

    fun getFloat(key: String, defaultValue: Float): Float {
        return prefs.getFloat(key, defaultValue)
    }

    fun saveInt(key: String, value: Int) {
        prefs.edit().putInt(key, value).apply()
    }

    fun getInt(key: String, defaultValue: Int): Int {
        return prefs.getInt(key, defaultValue)
    }
}
