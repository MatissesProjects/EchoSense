package com.echosense.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.echosense.app.databinding.ActivityMainBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val RECORD_AUDIO_PERMISSION_CODE = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), RECORD_AUDIO_PERMISSION_CODE)
        } else {
            startAudioEngine()
            startMetering()
        }
    }

    private fun setupUI() {
        // Master Gain: Scale 0-200 to 0.0-50.0 (progress / 4.0)
        binding.seekBarMasterGain.max = 200
        binding.seekBarMasterGain.progress = 4 // Default to 1.0x
        binding.seekBarMasterGain.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                setMasterGain(progress / 4.0f)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Noise Gate: Scale 0-100 to 0.0-0.5 threshold
        binding.seekBarNoiseGate.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                setNoiseGateThreshold(progress / 200.0f)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // EQ Bands (Bands 1-5): Scale 0-200 to -12dB to +12dB (100 is 0dB)
        val eqSeekBars = listOf(
            binding.seekBarBand1,
            binding.seekBarBand2,
            binding.seekBarBand3,
            binding.seekBarBand4,
            binding.seekBarBand5
        )

        eqSeekBars.forEachIndexed { index, seekBar ->
            seekBar.max = 200
            seekBar.progress = 100 // 0dB
            seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    // Map 0-200 to -12.0 to +12.0 dB
                    val gainDb = (progress - 100) * 0.12f
                    setEqualizerBandGain(index, gainDb)
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }
    }

    private fun startMetering() {
        lifecycleScope.launch {
            while (true) {
                val volume = getVolumeLevel()
                val progress = (volume * 500).toInt().coerceIn(0, 100)
                binding.progressBarVolume.progress = progress
                delay(33)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == RECORD_AUDIO_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startAudioEngine()
                startMetering()
            } else {
                Toast.makeText(this, "Permission denied. Audio cannot be recorded.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAudioEngine()
    }

    external fun startAudioEngine()
    external fun stopAudioEngine()
    external fun setNoiseGateThreshold(threshold: Float)
    external fun setMasterGain(gain: Float)
    external fun setEqualizerBandGain(bandIndex: Int, gain: Float)
    external fun getVolumeLevel(): Float

    companion object {
        init {
            System.loadLibrary("echosense_native")
        }
    }
}