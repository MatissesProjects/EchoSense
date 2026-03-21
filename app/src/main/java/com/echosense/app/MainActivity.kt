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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var settingsManager: AudioSettingsManager
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
        settingsManager = AudioSettingsManager(this)

        setupUI()
        restoreUiFromSettings()
        setupSpeechRecognizer()
        checkPermissions()
    }

    override fun onResume() {
        super.onResume()
        if (hasAllPermissions()) {
            startVisualizer()
            if (AudioEngineLib.isAudioEngineRunning()) {
                startListening()
                binding.btnToggleEngine.text = "Stop Engine"
            }
        }
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

    private fun restoreUiFromSettings() {
        binding.seekBarPreAmp.progress = (settingsManager.getFloat(AudioSettingsManager.KEY_PRE_AMP, 1.0f) * 10).toInt()
        binding.seekBarVoiceBoost.progress = (settingsManager.getFloat(AudioSettingsManager.KEY_VOICE_BOOST, 0.0f) / 0.3f).toInt()
        binding.seekBarMasterGain.progress = (settingsManager.getFloat(AudioSettingsManager.KEY_MASTER_GAIN, 1.0f) * 10).toInt()
        binding.seekBarHpf.progress = (settingsManager.getFloat("hpf_freq", 150.0f) / 5.0f).toInt()
        binding.seekBarNoiseGate.progress = (settingsManager.getFloat(AudioSettingsManager.KEY_NOISE_GATE, 0.0f) * 200).toInt()
        binding.seekBarWatchGain.progress = (settingsManager.getFloat(AudioSettingsManager.KEY_WATCH_GAIN, 2.0f) * 20).toInt()
        
        val profile = settingsManager.getInt(AudioSettingsManager.KEY_PROFILE, 3)
        when (profile) {
            0 -> binding.chipGroupProfile.check(R.id.chipProfileVoice)
            1 -> binding.chipGroupProfile.check(R.id.chipProfileMusic)
            2 -> binding.chipGroupProfile.check(R.id.chipProfileTV)
            else -> binding.chipGroupProfile.check(R.id.chipProfileCustom)
        }

        val source = settingsManager.getInt(AudioSettingsManager.KEY_MIC_SOURCE, 0)
        when (source) {
            1 -> binding.rbMicPhone.isChecked = true
            2 -> {
                binding.rbMicWatch.isChecked = true
                binding.layoutWatchGain.visibility = View.VISIBLE
            }
            else -> binding.rbMicAuto.isChecked = true
        }

        binding.swSensorFusion.isChecked = settingsManager.prefs.getBoolean("sensor_fusion", false)

        binding.seekBarBand1.progress = (settingsManager.getFloat("band_0", 0.0f) / 0.24f + 100).toInt()
        binding.seekBarBand2.progress = (settingsManager.getFloat("band_1", 0.0f) / 0.24f + 100).toInt()
        binding.seekBarBand3.progress = (settingsManager.getFloat("band_2", 0.0f) / 0.24f + 100).toInt()
        binding.seekBarBand4.progress = (settingsManager.getFloat("band_3", 0.0f) / 0.24f + 100).toInt()
        binding.seekBarBand5.progress = (settingsManager.getFloat("band_4", 0.0f) / 0.24f + 100).toInt()
    }

    private fun setupUI() {
        binding.btnExitApp.setOnClickListener {
            stopService(Intent(this, EchoSenseService::class.java))
            stopListening()
            finishAndRemoveTask()
        }

        binding.btnToggleEngine.setOnClickListener {
            if (AudioEngineLib.isAudioEngineRunning()) {
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
            AudioEngineLib.autoTune()
            binding.chipGroupProfile.check(R.id.chipProfileCustom)
            Toast.makeText(this, "AI Adaptive Tuning Applied", Toast.LENGTH_SHORT).show()
        }

        binding.chipGroupProfile.setOnCheckedChangeListener { _, checkedId ->
            val p = when (checkedId) {
                R.id.chipProfileVoice -> 0
                R.id.chipProfileMusic -> 1
                R.id.chipProfileTV -> 2
                else -> 3
            }
            AudioEngineLib.setProfile(p)
            settingsManager.saveInt(AudioSettingsManager.KEY_PROFILE, p)
        }

        binding.rgMicSource.setOnCheckedChangeListener { _, checkedId ->
            val source = when (checkedId) {
                R.id.rbMicPhone -> {
                    val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
                    val phoneMic = audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS).find { it.type == AudioDeviceInfo.TYPE_BUILTIN_MIC }
                    phoneMic?.let { AudioEngineLib.setInputDevice(it.id) }
                    binding.layoutWatchGain.visibility = View.GONE
                    1
                }
                R.id.rbMicWatch -> {
                    binding.layoutWatchGain.visibility = View.VISIBLE
                    2
                }
                else -> { 
                    AudioEngineLib.setInputDevice(-1)
                    binding.layoutWatchGain.visibility = View.GONE
                    0
                }
            }
            AudioEngineLib.setInputSource(source)
            settingsManager.saveInt(AudioSettingsManager.KEY_MIC_SOURCE, source)
            restartEngineIfRunning()
        }

        binding.seekBarWatchGain.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, f: Boolean) { 
                val gain = p / 20.0f
                AudioEngineLib.setRemoteGain(gain) 
                settingsManager.saveFloat(AudioSettingsManager.KEY_WATCH_GAIN, gain)
            }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })

        binding.swSensorFusion.setOnCheckedChangeListener { _, isChecked ->
            AudioEngineLib.setSensorFusion(isChecked)
            settingsManager.prefs.edit().putBoolean("sensor_fusion", isChecked).apply()
        }

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
            override fun onProgressChanged(s: SeekBar?, p: Int, f: Boolean) { 
                val gain = p / 10.0f
                AudioEngineLib.setPreAmpGain(gain)
                settingsManager.saveFloat(AudioSettingsManager.KEY_PRE_AMP, gain)
            }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })

        binding.seekBarVoiceBoost.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, f: Boolean) { 
                val gain = p * 0.3f
                AudioEngineLib.setVoiceBoost(gain) 
                settingsManager.saveFloat(AudioSettingsManager.KEY_VOICE_BOOST, gain)
            }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })

        binding.seekBarMasterGain.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, f: Boolean) { 
                val gain = p / 10.0f
                AudioEngineLib.setMasterGain(gain) 
                settingsManager.saveFloat(AudioSettingsManager.KEY_MASTER_GAIN, gain)
            }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })

        binding.seekBarHpf.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, f: Boolean) { 
                val freq = p * 5.0f + 50.0f // 50Hz to 550Hz
                AudioEngineLib.setHpfFreq(freq) 
                settingsManager.saveFloat("hpf_freq", freq)
            }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })

        binding.seekBarNoiseGate.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, f: Boolean) { 
                val thresh = p / 200.0f
                AudioEngineLib.setNoiseGateThreshold(thresh) 
                settingsManager.saveFloat(AudioSettingsManager.KEY_NOISE_GATE, thresh)
            }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })

        val eqSeekBars = listOf(binding.seekBarBand1, binding.seekBarBand2, binding.seekBarBand3, binding.seekBarBand4, binding.seekBarBand5)
        eqSeekBars.forEachIndexed { index, seekBar ->
            seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, p: Int, f: Boolean) { 
                    val gainDb = (p - 100) * 0.24f
                    AudioEngineLib.setEqualizerBandGain(index, gainDb)
                    settingsManager.saveFloat("band_$index", gainDb)
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
        if (AudioEngineLib.isAudioEngineRunning()) {
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
                    if (AudioEngineLib.isAudioEngineRunning()) {
                        lifecycleScope.launch { delay(500); startListening() }
                    }
                }
                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) binding.tvTranscription.text = matches[0]
                    if (AudioEngineLib.isAudioEngineRunning()) startListening()
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
                if (AudioEngineLib.isAudioEngineRunning()) {
                    val volume = AudioEngineLib.getVolumeLevel()
                    binding.progressBarVolume.progress = (volume * 500).toInt().coerceIn(0, 100)
                    AudioEngineLib.getFftData(fftData)
                    AudioEngineLib.getEqCurveData(eqCurveData)
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
}
