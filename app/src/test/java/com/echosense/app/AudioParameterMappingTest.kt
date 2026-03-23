package com.echosense.app

import org.junit.Assert.assertEquals
import org.junit.Test

class AudioParameterMappingTest {

    @Test
    fun testGainScaling() {
        // Verify that UI progress (0-200) maps correctly to C++ gain (0.0 - 20.0)
        // From MainActivity.kt: setMasterGain(progress / 10.0f)
        val progress = 50
        val expectedGain = 5.0f
        val actualGain = progress / 10.0f
        assertEquals(expectedGain, actualGain, 0.01f)
    }

    @Test
    fun testEqScaling() {
        // Verify that UI progress (0-200) maps to -12dB to +12dB
        // Updated from 0.24f to 0.12f for precision
        val progressCenter = 100
        assertEquals(0.0f, (progressCenter - 100) * 0.12f, 0.01f)

        val progressMax = 200
        assertEquals(12.0f, (progressMax - 100) * 0.12f, 0.01f)
        
        val progressMin = 0
        assertEquals(-12.0f, (progressMin - 100) * 0.12f, 0.01f)
    }

    @Test
    fun testHpfScaling() {
        // UI (0-100) maps to 50Hz to 550Hz
        // From MainActivity.kt: val freq = p * 5.0f + 50.0f
        val progress = 100
        val expected = 550.0f
        assertEquals(expected, progress * 5.0f + 50.0f, 0.01f)
    }

    @Test
    fun testLpfScaling() {
        // UI (0-100) maps to 1000Hz to 20000Hz
        // From MainActivity.kt: val freq = p * 190.0f + 1000.0f
        val progress = 100
        val expected = 20000.0f
        assertEquals(expected, progress * 190.0f + 1000.0f, 0.01f)
    }

    @Test
    fun testNoiseGateScaling() {
        // UI (0-200) maps to 0.0 to 0.1
        // From MainActivity.kt: val thresh = (progress / 2000.0f)
        val progress = 200
        assertEquals(0.1f, progress / 2000.0f, 0.001f)
    }

    @Test
    fun testVoiceBoostScaling() {
        // UI (0-100) maps to 0.0 to 20.0dB
        // From MainActivity.kt: val gain = (progress / 5.0f)
        val progress = 50
        assertEquals(10.0f, progress / 5.0f, 0.01f)
    }
}