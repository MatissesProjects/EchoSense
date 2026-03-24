package com.echosense.app.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class AudioParameterMapperTest {

    @Test
    fun testPreAmpMapping() {
        assertEquals(1.0f, AudioParameterMapper.progressToPreAmpGain(10), 0.001f)
        assertEquals(5.0f, AudioParameterMapper.progressToPreAmpGain(50), 0.001f)
        assertEquals(10, AudioParameterMapper.preAmpGainToProgress(1.0f))
    }

    @Test
    fun testVoiceBoostMapping() {
        assertEquals(3.0f, AudioParameterMapper.progressToVoiceBoost(10), 0.001f)
        assertEquals(30.0f, AudioParameterMapper.progressToVoiceBoost(100), 0.001f)
        assertEquals(100, AudioParameterMapper.voiceBoostToProgress(30.0f))
    }

    @Test
    fun testHpfMapping() {
        assertEquals(50.0f, AudioParameterMapper.progressToHpfFreq(0), 0.001f)
        assertEquals(150.0f, AudioParameterMapper.progressToHpfFreq(20), 0.001f)
        assertEquals(20, AudioParameterMapper.hpfFreqToProgress(150.0f))
    }

    @Test
    fun testLpfMapping() {
        assertEquals(1000.0f, AudioParameterMapper.progressToLpfFreq(0), 0.001f)
        assertEquals(20000.0f, AudioParameterMapper.progressToLpfFreq(100), 0.001f)
        assertEquals(100, AudioParameterMapper.lpfFreqToProgress(20000.0f))
    }

    @Test
    fun testEqBandMapping() {
        assertEquals(0.0f, AudioParameterMapper.progressToEqBandGain(100), 0.001f)
        assertEquals(-12.0f, AudioParameterMapper.progressToEqBandGain(0), 0.001f)
        assertEquals(12.0f, AudioParameterMapper.progressToEqBandGain(200), 0.001f)
        assertEquals(100, AudioParameterMapper.eqBandGainToProgress(0.0f))
    }
}
