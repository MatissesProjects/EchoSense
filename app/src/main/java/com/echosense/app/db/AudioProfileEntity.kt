package com.echosense.app.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "audio_profiles")
data class AudioProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val preAmpGain: Float = 1.0f,
    val voiceBoostDb: Float = 0.0f,
    val hpfFreq: Float = 150.0f,
    val lpfFreq: Float = 12000.0f,
    val limiterThreshold: Float = 0.9f,
    val noiseGateThreshold: Float = 0.0f,
    val masterGain: Float = 1.0f,
    val band1Gain: Float = 0.0f,
    val band2Gain: Float = 0.0f,
    val band3Gain: Float = 0.0f,
    val band4Gain: Float = 0.0f,
    val band5Gain: Float = 0.0f,
    val watchMicBoost: Float = 2.0f,
    val sensorFusionEnabled: Boolean = false,
    val isBuiltIn: Boolean = false
)
