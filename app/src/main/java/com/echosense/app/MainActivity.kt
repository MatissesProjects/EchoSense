package com.echosense.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.WindowManager
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.echosense.app.databinding.ActivityMainBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val PERMISSION_REQUEST_CODE = 1
    
    private val fftData = FloatArray(64)
    private val eqCurveData = FloatArray(100)

    private var speechRecognizer: SpeechRecognizer? = null
    private lateinit var recognizerIntent: Intent
    private var visualizerJob: Job? = null
    private var isDimmed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setupSpeechRecognizer()
        checkPermissions()
    }

    override fun onResume() {
        super.onResume()
        if (hasAllPermissions()) {
            startVisualizer()
            if (isAudioEngineRunning()) {
                startListening()
                binding.btnToggleEngine.text = "Stop Engine"
            }
        }
    }

    private fun checkPermissions() {
        val permissions = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        
        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missing.toTypedArray(), PERMISSION_REQUEST_CODE)
        }
    }

    private fun hasAllPermissions(): Boolean {
        var granted = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            granted = granted && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        }
        return granted
    }

    private fun setupUI() {
        binding.btnToggleEngine.setOnClickListener {
            if (isAudioEngineRunning()) {
                stopService(Intent(this, EchoSenseService::class.java))
                stopListening()
                binding.btnToggleEngine.text = "Start Engine"
            } else {
                startForegroundService(Intent(this, EchoSenseService::class.java))
                startListening()
                binding.btnToggleEngine.text = "Stop Engine"
            }
        }

        binding.btnAutoTune.setOnClickListener {
            autoTune()
            Toast.makeText(this, "AI Auto-Tune Applied", Toast.LENGTH_SHORT).show()
        }

        // Mic Selection Logic
        binding.rgMicSource.setOnCheckedChangeListener { _, checkedId ->
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            if (checkedId == R.id.rbMicPhone) {
                // Find built-in mic
                val devices = audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS)
                val phoneMic = devices.find { it.type == AudioDeviceInfo.TYPE_BUILTIN_MIC }
                phoneMic?.let {
                    setInputDevice(it.id)
                    restartEngineIfRunning()
                }
            } else {
                // Return to auto/unspecified
                setInputDevice(-1) // kUnspecified
                restartEngineIfRunning()
            }
        }

        // Power Controls
        binding.cbKeepScreenOn.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }

        binding.btnDimScreen.setOnClickListener {
            val params = window.attributes
            if (!isDimmed) {
                params.screenBrightness = 0.01f
                binding.btnDimScreen.text = "Restore Brightness"
                isDimmed = true
            } else {
                params.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                binding.btnDimScreen.text = "Dim Mode"
                isDimmed = false
            }
            window.attributes = params
        }

        // Sliders
        binding.seekBarPreAmp.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, f: Boolean) { setPreAmpGain(p / 10.0f) }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })

        binding.seekBarVoiceBoost.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, f: Boolean) { setVoiceBoost(p / 5.0f) }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })

        binding.seekBarMasterGain.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, f: Boolean) { setMasterGain(p / 10.0f) }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })

        binding.seekBarNoiseGate.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, f: Boolean) { setNoiseGateThreshold(p / 200.0f) }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })

        val eqSeekBars = listOf(binding.seekBarBand1, binding.seekBarBand2, binding.seekBarBand3, binding.seekBarBand4, binding.seekBarBand5)
        eqSeekBars.forEachIndexed { index, seekBar ->
            seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, p: Int, f: Boolean) { setEqualizerBandGain(index, (p - 100) * 0.12f) }
                override fun onStartTrackingTouch(s: SeekBar?) {}
                override fun onStopTrackingTouch(s: SeekBar?) {}
            })
        }
    }

    private fun restartEngineIfRunning() {
        if (isAudioEngineRunning()) {
            stopService(Intent(this, EchoSenseService::class.java))
            startForegroundService(Intent(this, EchoSenseService::class.java))
        }
    }

    private fun setupSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
            recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            }

            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onError(error: Int) {
                    if (isAudioEngineRunning()) {
                        lifecycleScope.launch {
                            delay(500)
                            startListening()
                        }
                    }
                }
                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) binding.tvTranscription.text = matches[0]
                    if (isAudioEngineRunning()) startListening()
                }
                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) binding.tvTranscription.text = matches[0]
                }
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }
    }

    private fun startListening() {
        try {
            speechRecognizer?.startListening(recognizerIntent)
        } catch (e: Exception) {}
    }

    private fun stopListening() {
        speechRecognizer?.stopListening()
    }

    private fun startVisualizer() {
        visualizerJob?.cancel()
        visualizerJob = lifecycleScope.launch {
            while (true) {
                if (isAudioEngineRunning()) {
                    val volume = getVolumeLevel()
                    binding.progressBarVolume.progress = (volume * 500).toInt().coerceIn(0, 100)
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
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (hasAllPermissions()) {
                startVisualizer()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer?.destroy()
    }

    external fun isAudioEngineRunning(): Boolean
    external fun setInputDevice(deviceId: Int)
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