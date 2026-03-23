package com.echosense.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AudioSettingsManagerTest {

    private lateinit var settingsManager: AudioSettingsManager

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        settingsManager = AudioSettingsManager(context)
    }

    @Test
    fun testSaveAndGetFloat() {
        val key = "test_float"
        val value = 12.34f
        settingsManager.saveFloat(key, value)
        assertEquals(value, settingsManager.getFloat(key, 0.0f), 0.001f)
    }

    @Test
    fun testSaveAndGetInt() {
        val key = "test_int"
        val value = 42
        settingsManager.saveInt(key, value)
        assertEquals(value, settingsManager.getInt(key, 0))
    }

    @Test
    fun testDefaultValues() {
        assertEquals(1.0f, settingsManager.getFloat("non_existent", 1.0f), 0.001f)
        assertEquals(100, settingsManager.getInt("non_existent", 100))
    }
}
