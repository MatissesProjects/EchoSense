package com.echosense.app

import com.echosense.app.utils.HearingTestCalibration
import org.junit.Assert.assertEquals
import org.junit.Test

class HearingTestCalibrationTest {

    @Test
    fun testCurveGenerationLogic() {
        val gains = mutableMapOf<Int, Float>()
        
        // Simulate threshold results (Linear volume)
        // 0.0001 is "perfect" (0dB)
        // 0.001 is ~20dB loss
        // 0.01 is ~40dB loss
        // 0.1 is ~60dB loss
        val thresholds = floatArrayOf(
            0.0001f, // 250 Hz (0dB loss)
            0.001f,  // 500 Hz (20dB loss)
            0.01f,   // 1000 Hz (40dB loss) -> Maps to nothing currently
            0.01f,   // 2000 Hz (40dB loss)
            0.1f,    // 4000 Hz (60dB loss)
            0.1f     // 8000 Hz (60dB loss)
        )

        // Test the calibration logic directly using a lambda for setGain
        HearingTestCalibration.generateCurve(thresholds) { band, gain ->
            gains[band] = gain
        }

        // Verify half-gain rule: 
        // 250Hz: 0dB loss -> 0dB gain
        // 500Hz: 20dB loss -> 10dB gain
        // 2000Hz: 40dB loss -> 20dB gain (capped at 15dB in our code)
        // 4000Hz: 60dB loss -> 30dB gain (capped at 15dB in our code)
        // 8000Hz: 60dB loss -> 30dB gain (capped at 15dB in our code)

        assertEquals(0.0f, gains[0]!!, 0.001f)
        assertEquals(10.0f, gains[1]!!, 0.001f)
        assertEquals(15.0f, gains[2]!!, 0.001f) // 2000 band
        assertEquals(15.0f, gains[3]!!, 0.001f) // 4000 band
        assertEquals(15.0f, gains[4]!!, 0.001f) // 8000 band
    }
}
