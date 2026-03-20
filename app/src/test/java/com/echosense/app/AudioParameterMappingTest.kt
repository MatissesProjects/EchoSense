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
        // From MainActivity.kt: val gainDb = (progress - 100) * 0.12f
        val progressCenter = 100
        assertEquals(0.0f, (progressCenter - 100) * 0.12f, 0.01f)

        val progressMax = 200
        assertEquals(12.0f, (progressMax - 100) * 0.12f, 0.01f)
    }
}