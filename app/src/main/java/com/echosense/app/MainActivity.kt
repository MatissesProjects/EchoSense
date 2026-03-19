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
    
    private val fftData = FloatArray(512)
    private val eqCurveData = FloatArray(100)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), RECORD_AUDIO_PERMISSION_CODE)
        } else {
            startAudioEngine()
            startVisualizer()
        }
    }

    private fun setupUI() {
        binding.btnToggleEngine.setOnClickListener {
            if (isAudioEngineRunning()) {
                stopAudioEngine()
                binding.btnToggleEngine.text = "Start Engine"
            } else {
                startAudioEngine()
                binding.btnToggleEngine.text = "Stop Engine"
            }
        }

        binding.btnAutoTune.setOnClickListener {
            autoTune()
            Toast.makeText(this, "AI Auto-Tune Applied", Toast.LENGTH_SHORT).show()
        }

        binding.seekBarPreAmp.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                // Scale 0-100 to 0.0-10.0 (progress / 10.0)
                setPreAmpGain(progress / 10.0f)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        binding.seekBarVoiceBoost.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                // Scale 0-100 to 0.0-20.0 dB (progress / 5.0)
                setVoiceBoost(progress / 5.0f)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        binding.seekBarMasterGain.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                setMasterGain(progress / 10.0f)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        binding.seekBarNoiseGate.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                setNoiseGateThreshold(progress / 200.0f)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        val eqSeekBars = listOf(
            binding.seekBarBand1,
            binding.seekBarBand2,
            binding.seekBarBand3,
            binding.seekBarBand4,
            binding.seekBarBand5
        )

        eqSeekBars.forEachIndexed { index, seekBar ->
            seekBar.max = 200
            seekBar.progress = 100
            seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    val gainDb = (progress - 100) * 0.12f
                    setEqualizerBandGain(index, gainDb)
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }
    }

    private fun startVisualizer() {
        lifecycleScope.launch {
            while (true) {
                if (isAudioEngineRunning()) {
                    val volume = getVolumeLevel()
                    val progress = (volume * 500).toInt().coerceIn(0, 100)
                    binding.progressBarVolume.progress = progress

                    getFftData(fftData)
                    getEqCurveData(eqCurveData)
                    binding.visualizerView.updateData(fftData, eqCurveData)
                }
                delay(33)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == RECORD_AUDIO_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startAudioEngine()
                startVisualizer()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAudioEngine()
    }

    external fun startAudioEngine()
    external fun stopAudioEngine()
    external fun isAudioEngineRunning(): Boolean
    external fun setPreAmpGain(gain: Float)
    external fun setVoiceBoost(gainDb: Float)
    external fun setNoiseGateThreshold(threshold: Float)
    external fun setMasterGain(gain: Float)
    external fun setEqualizerBandGain(bandIndex: Int, gain: Float)
    external fun getVolumeLevel(): Float
    external fun getFftData(output: FloatArray)
    external fun getEqCurveData(output: FloatArray)
    external fun autoTune()

    companion object {
        init {
            System.loadLibrary("echosense_native")
        }
    }
}