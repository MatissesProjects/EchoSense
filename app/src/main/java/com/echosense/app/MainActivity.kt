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
import android.view.View
import android.view.WindowManager
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.echosense.app.databinding.ActivityMainBinding
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.nio.ByteOrder
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

    private val WEAR_AUDIO_PATH = "/audio_stream"

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
        updateBluetoothUi()
    }

    override fun onPause() {
        super.onPause()
        updateBluetoothUi()
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer?.destroy()
    }

    private fun checkPermissions() {
        val permissions = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        
        val missing = permissions.filter { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }
        if (missing.isNotEmpty()) ActivityCompat.requestPermissions(this, missing.toTypedArray(), PERMISSION_REQUEST_CODE)
    }

    private fun hasAllPermissions(): Boolean {
        var granted = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) granted = granted && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        return granted
    }

    private fun setupUI() {
        binding.btnExitApp.setOnClickListener {
            stopService(Intent(this, EchoSenseService::class.java))
            stopListening()
            finishAndRemoveTask()
        }

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
            binding.chipGroupProfile.check(R.id.chipProfileCustom)
            Toast.makeText(this, "AI Adaptive Tuning Applied", Toast.LENGTH_SHORT).show()
        }

        binding.chipGroupProfile.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.chipProfileVoice -> setProfile(0)
                R.id.chipProfileMusic -> setProfile(1)
                R.id.chipProfileTV -> setProfile(2)
                R.id.chipProfileCustom -> setProfile(3)
            }
        }

        binding.rgMicSource.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rbMicPhone -> {
                    val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
                    val phoneMic = audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS).find { it.type == AudioDeviceInfo.TYPE_BUILTIN_MIC }
                    phoneMic?.let { setInputDevice(it.id) }
                    setInputSource(1)
                    binding.layoutWatchGain.visibility = View.GONE
                }
                R.id.rbMicWatch -> {
                    setInputSource(2)
                    binding.layoutWatchGain.visibility = View.VISIBLE
                }
                else -> { 
                    setInputDevice(-1)
                    setInputSource(0)
                    binding.layoutWatchGain.visibility = View.GONE
                }
            }
            restartEngineIfRunning()
        }

        binding.seekBarWatchGain.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, f: Boolean) { 
                // 0-100 maps to 0.0x to 5.0x gain
                setRemoteGain(p / 20.0f) 
            }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })

        binding.swBluetoothAnc.setOnCheckedChangeListener { _, isChecked ->
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            if (isChecked) audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
            else audioManager.mode = AudioManager.MODE_NORMAL
        }

        binding.cbKeepScreenOn.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        binding.btnDimScreen.setOnClickListener {
            val params = window.attributes
            if (!isDimmed) {
                params.screenBrightness = 0.01f
                binding.btnDimScreen.text = "Restore"
                isDimmed = true
            } else {
                params.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                binding.btnDimScreen.text = "Dim Mode"
                isDimmed = false
            }
            window.attributes = params
        }

        binding.seekBarPreAmp.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, f: Boolean) { setPreAmpGain(p / 10.0f) }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })

        binding.seekBarVoiceBoost.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, f: Boolean) { 
                // Scale 0-100 to 0.0-30.0 dB for powerful AI boost
                setVoiceBoost(p * 0.3f) 
            }
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
            seekBar.max = 200
            seekBar.progress = 100
            seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, p: Int, f: Boolean) { 
                    // Map 0-200 to -24dB to +24dB for dramatic control
                    val gainDb = (p - 100) * 0.24f
                    setEqualizerBandGain(index, gainDb) 
                }
                override fun onStartTrackingTouch(s: SeekBar?) {}
                override fun onStopTrackingTouch(s: SeekBar?) {}
            })
        }
    }

    private fun updateBluetoothUi() {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val btConnected = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).any { 
            it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP || it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO
        }
        binding.layoutBluetoothOpt.visibility = if (btConnected) View.VISIBLE else View.GONE
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
                        lifecycleScope.launch { delay(500); startListening() }
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
        try { speechRecognizer?.startListening(recognizerIntent) } catch (e: Exception) {}
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
            if (hasAllPermissions()) startVisualizer()
        }
    }

    external fun isAudioEngineRunning(): Boolean
    external fun setInputSource(source: Int)
    external fun setInputDevice(deviceId: Int)
    external fun setRemoteGain(gain: Float)
    external fun writeRemoteAudio(data: FloatArray)
    external fun setPreAmpGain(gain: Float)
    external fun setVoiceBoost(gainDb: Float)
    external fun setNoiseGateThreshold(threshold: Float)
    external fun setMasterGain(gain: Float)
    external fun setProfile(profile: Int)
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