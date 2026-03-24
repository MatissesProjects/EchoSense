package com.echosense.app.utils

import java.nio.ByteBuffer
import java.nio.ByteOrder

object AudioConverter {
    /**
     * Converts a ByteArray of 16-bit PCM (Little Endian) to a FloatArray.
     */
    fun pcmToFloat(pcmData: ByteArray, length: Int): FloatArray {
        val shortsToRead = length / 2
        val floatArray = FloatArray(shortsToRead)
        val shortBuffer = ByteBuffer.wrap(pcmData, 0, length)
            .order(ByteOrder.LITTLE_ENDIAN)
            .asShortBuffer()
        
        for (i in 0 until shortsToRead) {
            if (shortBuffer.hasRemaining()) {
                floatArray[i] = shortBuffer.get().toFloat() / 32768.0f
            }
        }
        return floatArray
    }
}
