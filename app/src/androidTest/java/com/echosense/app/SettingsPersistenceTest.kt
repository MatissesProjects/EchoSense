package com.echosense.app

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsPersistenceTest {

    private lateinit var settingsManager: AudioSettingsManager

    @Before
    fun setup() {
        settingsManager = AudioSettingsManager(ApplicationProvider.getApplicationContext())
    }

    @Test
    fun testSaveAndRestoreFloat() {
        val testGain = 5.5f
        settingsManager.saveFloat(AudioSettingsManager.KEY_MASTER_GAIN, testGain)
        
        val restoredGain = settingsManager.getFloat(AudioSettingsManager.KEY_MASTER_GAIN, 1.0f)
        assertEquals(testGain, restoredGain, 0.001f)
    }

    @Test
    fun testSaveAndRestoreInt() {
        val testProfile = 2
        settingsManager.saveInt(AudioSettingsManager.KEY_PROFILE, testProfile)
        
        val restoredProfile = settingsManager.getInt(AudioSettingsManager.KEY_PROFILE, 0)
        assertEquals(testProfile, restoredProfile)
    }

    @Test
    fun testDefaultValues() {
        // Clear prefs for this test if needed, but here we just check non-existent keys
        val defaultValue = 1.23f
        val restored = settingsManager.getFloat("non_existent_key", defaultValue)
        assertEquals(defaultValue, restored, 0.001f)
    }
}
