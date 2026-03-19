package com.echosense.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.echosense.app.databinding.ActivityMainBinding

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
        }
    }

    private fun setupUI() {
        // Noise Gate: Scale 0-100 to 0.0-0.5 threshold
        binding.seekBarNoiseGate.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                setNoiseGateThreshold(progress / 200.0f)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // EQ Bands: Scale 0-200 to 0.0-2.0 gain (100 is 1.0)
        val eqSeekBars = listOf(
            binding.seekBarBand1,
            binding.seekBarBand2,
            binding.seekBarBand3,
            binding.seekBarBand4,
            binding.seekBarBand5
        )

        eqSeekBars.forEachIndexed { index, seekBar ->
            seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    setEqualizerBandGain(index, progress / 100.0f)
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == RECORD_AUDIO_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startAudioEngine()
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
    external fun setEqualizerBandGain(bandIndex: Int, gain: Float)

    companion object {
        init {
            System.loadLibrary("echosense_native")
        }
    }
}