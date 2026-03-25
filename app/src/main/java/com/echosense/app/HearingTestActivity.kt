package com.echosense.app

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class HearingTestActivity : AppCompatActivity() {

    private val frequencies = floatArrayOf(250f, 500f, 1000f, 2000f, 4000f, 8000f)
    private val thresholds = FloatArray(frequencies.size)
    private var currentFreqIndex = 0
    private var currentVolume = 0.001f
    private var isTesting = false
    private val handler = Handler(Looper.getMainLooper())

    private lateinit var btnHear: Button
    private lateinit var btnStart: Button
    private lateinit var freqText: TextView
    private lateinit var progress: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hearing_test)

        btnHear = findViewById(R.id.btnHear)
        btnStart = findViewById(R.id.btnStart)
        freqText = findViewById(R.id.frequencyText)
        progress = findViewById(R.id.testProgress)

        btnHear.visibility = View.GONE

        btnStart.setOnClickListener {
            startTest()
        }

        btnHear.setOnClickListener {
            recordThreshold()
        }
    }

    private fun startTest() {
        btnStart.visibility = View.GONE
        btnHear.visibility = View.VISIBLE
        isTesting = true
        currentFreqIndex = 0
        testNextFrequency()
    }

    private fun testNextFrequency() {
        if (currentFreqIndex >= frequencies.size) {
            finishTest()
            return
        }

        val freq = frequencies[currentFreqIndex]
        freqText.text = "Current Frequency: ${freq.toInt()} Hz"
        progress.progress = currentFreqIndex + 1
        
        currentVolume = 0.0001f
        increaseVolume()
    }

    private fun increaseVolume() {
        if (!isTesting) return

        AudioEngineLib.setTone(frequencies[currentFreqIndex], currentVolume)
        
        // Increase volume by 5dB roughly every 500ms
        // (Volume in float is linear, dB is logarithmic: 5dB = 1.77x)
        currentVolume *= 1.4f // ~3dB steps
        
        if (currentVolume < 1.0f) {
            handler.postDelayed({ increaseVolume() }, 800)
        } else {
            // User didn't hear it at max volume
            recordThreshold()
        }
    }

    private fun recordThreshold() {
        AudioEngineLib.setTone(0f, 0f) // Stop tone
        thresholds[currentFreqIndex] = currentVolume
        currentFreqIndex++
        
        handler.postDelayed({ testNextFrequency() }, 1000)
    }

    private fun finishTest() {
        isTesting = false
        AudioEngineLib.setTone(0f, 0f)
        
        // Save thresholds and generate curves
        val settings = AudioSettingsManager(this)
        for (i in frequencies.indices) {
            settings.saveFloat("hearing_thresh_${frequencies[i].toInt()}", thresholds[i])
        }
        
        generateCurve(thresholds)
        
        freqText.text = "Test Complete! Your profile is calibrated."
        btnHear.visibility = View.GONE
        
        handler.postDelayed({ finish() }, 3000)
    }

    private fun generateCurve(thresholds: FloatArray) {
        // Simple "Half-Gain Rule" for EQ
        // If threshold is high (e.g. 0.1 vs 0.001), apply more gain.
        // Thresholds are linear volume. Convert to dB loss.
        // Assume 0.001 is "perfect" (0dB) and 1.0 is "severe" (60dB+)
        
        for (i in thresholds.indices) {
            val lossDb = 20.0f * Math.log10((thresholds[i] / 0.0001f).toDouble()).toFloat()
            val gainDb = lossDb * 0.5f // Half-gain rule
            
            // Map to the 5 bands we have (200, 500, 1500, 3000, 6000)
            // This is a rough mapping
            when (i) {
                0 -> AudioEngineLib.setEqualizerBandGain(0, gainDb.coerceIn(0f, 15f)) // 250 -> 200 band
                1 -> AudioEngineLib.setEqualizerBandGain(1, gainDb.coerceIn(0f, 15f)) // 500 -> 500 band
                2 -> {} // 1000 Hz between bands
                3 -> AudioEngineLib.setEqualizerBandGain(2, gainDb.coerceIn(0f, 15f)) // 2000 -> 1500 band
                4 -> AudioEngineLib.setEqualizerBandGain(3, gainDb.coerceIn(0f, 15f)) // 4000 -> 3000 band
                5 -> AudioEngineLib.setEqualizerBandGain(4, gainDb.coerceIn(0f, 15f)) // 8000 -> 6000 band
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isTesting = false
        AudioEngineLib.setTone(0f, 0f)
        handler.removeCallbacksAndMessages(null)
    }
}
