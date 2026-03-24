package com.echosense.app.utils

import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class AudioConverterTest {

    @Test
    fun testPcmToFloat() {
        // Arrange
        val pcmData = ByteArray(4)
        val shortBuffer = ByteBuffer.wrap(pcmData).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
        shortBuffer.put(0)      // First short: 0
        shortBuffer.put(32767)  // Second short: max value

        // Act
        val floatData = AudioConverter.pcmToFloat(pcmData, 4)

        // Assert
        assertEquals(2, floatData.size)
        assertEquals(0.0f, floatData[0], 0.001f)
        assertEquals(32767.0f / 32768.0f, floatData[1], 0.001f)
    }

    @Test
    fun testPcmToFloatNegative() {
        // Arrange
        val pcmData = ByteArray(2)
        val shortBuffer = ByteBuffer.wrap(pcmData).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
        shortBuffer.put(-32768) // Min value

        // Act
        val floatData = AudioConverter.pcmToFloat(pcmData, 2)

        // Assert
        assertEquals(1, floatData.size)
        assertEquals(-1.0f, floatData[0], 0.001f)
    }

    @Test
    fun testPcmToFloatOddLength() {
        // Arrange
        val pcmData = ByteArray(3) // Only one complete short
        val shortBuffer = ByteBuffer.wrap(pcmData).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
        shortBuffer.put(16384)

        // Act
        val floatData = AudioConverter.pcmToFloat(pcmData, 3)

        // Assert
        assertEquals(1, floatData.size) // 3 / 2 = 1
        assertEquals(0.5f, floatData[0], 0.001f)
    }
}
