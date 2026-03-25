package com.echosense.app

import com.echosense.app.fakes.FakeSharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class AudioSettingsManagerTest {

    private lateinit var settingsManager: AudioSettingsManager
    private lateinit var fakePrefs: FakeSharedPreferences

    @Before
    fun setup() {
        fakePrefs = FakeSharedPreferences()
        settingsManager = AudioSettingsManager(null, fakePrefs)
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
