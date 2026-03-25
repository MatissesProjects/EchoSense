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
import com.echosense.app.db.ConversationNote
import com.echosense.app.db.EchoSenseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

import com.echosense.app.utils.AudioParameterMapper

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var settingsManager: AudioSettingsManager
    private lateinit var summarizationManager: SummarizationManager
    private val PERMISSION_REQUEST_CODE = 1
    
    private val fftData = FloatArray(64)
    private val eqCurveData = FloatArray(100)

    private var speechRecognizer: SpeechRecognizer? = null
    private lateinit var recognizerIntent: Intent
    private var visualizerJob: Job? = null
    private var isDimmed = false
    private var isListeningActive = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        settingsManager = AudioSettingsManager(this)
        summarizationManager = SummarizationManager(this)

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
        binding.seekBarPreAmp.progress = AudioParameterMapper.preAmpGainToProgress(settingsManager.getFloat(AudioSettingsManager.KEY_PRE_AMP, 1.0f))
        binding.seekBarVoiceBoost.progress = AudioParameterMapper.voiceBoostToProgress(settingsManager.getFloat(AudioSettingsManager.KEY_VOICE_BOOST, 0.0f))
        binding.seekBarMasterGain.progress = AudioParameterMapper.masterGainToProgress(settingsManager.getFloat(AudioSettingsManager.KEY_MASTER_GAIN, 1.0f))
        binding.seekBarHpf.progress = AudioParameterMapper.hpfFreqToProgress(settingsManager.getFloat("hpf_freq", 150.0f))
        binding.seekBarLpf.progress = AudioParameterMapper.lpfFreqToProgress(settingsManager.getFloat("lpf_freq", 12000.0f))
        binding.seekBarLimiter.progress = AudioParameterMapper.limiterThresholdToProgress(settingsManager.getFloat("limiter_thresh", 0.9f))
        binding.seekBarTransient.progress = AudioParameterMapper.transientSuppressionToProgress(settingsManager.getFloat("transient_suppression", 0.0f))
        binding.seekBarSpectralReduction.progress = (settingsManager.getFloat("spectral_reduction", 0.0f) * 100).toInt()
        binding.seekBarSpectralGate.progress = (settingsManager.getFloat("spectral_gate_thresh", 0.0f) * 5000).toInt()
        binding.seekBarDereverb.progress = AudioParameterMapper.dereverbToProgress(settingsManager.getFloat("dereverb_strength", 0.0f))
        binding.seekBarHpss.progress = AudioParameterMapper.hpssToProgress(settingsManager.getFloat("hpss_strength", 0.0f))
        binding.seekBarNeuralMask.progress = AudioParameterMapper.neuralMaskToProgress(settingsManager.getFloat("neural_mask_strength", 0.0f))
        binding.seekBarBassBoost.progress = AudioParameterMapper.bassBoostToProgress(settingsManager.getFloat("bass_boost_strength", 0.0f))
        binding.seekBarNoiseGate.progress = AudioParameterMapper.noiseGateThresholdToProgress(settingsManager.getFloat(AudioSettingsManager.KEY_NOISE_GATE, 0.0f))
        binding.seekBarWatchGain.progress = AudioParameterMapper.watchGainToProgress(settingsManager.getFloat(AudioSettingsManager.KEY_WATCH_GAIN, 2.0f))
        
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
        binding.swTargetLock.isChecked = settingsManager.prefs.getBoolean("target_lock", false)
        binding.swBeamforming.isChecked = settingsManager.prefs.getBoolean("beamforming", false)
        binding.seekBarMbRatio.progress = AudioParameterMapper.mbCompressionRatioToProgress(settingsManager.getFloat("mb_compression", 1.0f))

        val speakerId = settingsManager.getInt(AudioSettingsManager.KEY_TARGET_SPEAKER, -1)
        // Dynamic speakers will be checked in refreshSpeakerList
        if (speakerId == -1) {
            binding.chipGroupSpeaker.check(R.id.chipSpeakerNone)
        }

        binding.seekBarBand1.progress = AudioParameterMapper.eqBandGainToProgress(settingsManager.getFloat("band_0", 0.0f))
        binding.seekBarBand2.progress = AudioParameterMapper.eqBandGainToProgress(settingsManager.getFloat("band_1", 0.0f))
        binding.seekBarBand3.progress = AudioParameterMapper.eqBandGainToProgress(settingsManager.getFloat("band_2", 0.0f))
        binding.seekBarBand4.progress = AudioParameterMapper.eqBandGainToProgress(settingsManager.getFloat("band_3", 0.0f))
        binding.seekBarBand5.progress = AudioParameterMapper.eqBandGainToProgress(settingsManager.getFloat("band_4", 0.0f))
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

        binding.btnHearingTest.setOnClickListener {
            val intent = Intent(this, HearingTestActivity::class.java)
            startActivity(intent)
        }

        binding.btnSummarize.setOnClickListener {
            binding.tvTranscription.text = "AI is thinking... please wait."
            lifecycleScope.launch {
                val summary = summarizationManager.getRecentNotesSummary()
                binding.tvTranscription.text = summary
                Toast.makeText(this@MainActivity, "Conversation Summarized", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnClearHistory.setOnClickListener {
            lifecycleScope.launch {
                summarizationManager.clearHistory()
                binding.tvTranscription.text = "History Cleared."
                Toast.makeText(this@MainActivity, "Database Cleared", Toast.LENGTH_SHORT).show()
            }
        }

        binding.swTargetLock.setOnCheckedChangeListener { _, isChecked ->
            AudioEngineLib.setTargetLock(isChecked)
            settingsManager.prefs.edit().putBoolean("target_lock", isChecked).apply()
            
            // Give visual feedback that sliders are overridden
            if (isChecked) {
                binding.chipGroupProfile.isEnabled = false
                binding.seekBarHpf.isEnabled = false
                binding.seekBarLpf.isEnabled = false
                Toast.makeText(this, "Target Lock Engaged: Crowds Isolated", Toast.LENGTH_SHORT).show()
            } else {
                binding.chipGroupProfile.isEnabled = true
                binding.seekBarHpf.isEnabled = true
                binding.seekBarLpf.isEnabled = true
            }
        }

        binding.btnLearnNoise.setOnClickListener {
            AudioEngineLib.learnNoise()
            Toast.makeText(this, "Learning Environment Noise... (Keep Quiet)", Toast.LENGTH_LONG).show()
        }

        binding.seekBarSpectralReduction.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, f: Boolean) { 
                val strength = p / 100.0f // Special case for spectral
                AudioEngineLib.setSpectralReduction(strength) 
                settingsManager.saveFloat("spectral_reduction", strength)
            }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })

        binding.seekBarSpectralGate.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, f: Boolean) { 
                val thresh = p / 5000.0f // Special case for spectral gate
                AudioEngineLib.setSpectralGateThreshold(thresh) 
                settingsManager.saveFloat("spectral_gate_thresh", thresh)
            }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })

        binding.chipGroupProfile.setOnCheckedStateChangeListener { _, checkedIds ->
            val checkedId = if (checkedIds.isNotEmpty()) checkedIds[0] else View.NO_ID
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
                val gain = AudioParameterMapper.progressToWatchGain(p)
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
                val gain = AudioParameterMapper.progressToPreAmpGain(p)
                AudioEngineLib.setPreAmpGain(gain)
                settingsManager.saveFloat(AudioSettingsManager.KEY_PRE_AMP, gain)
            }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })

        binding.seekBarVoiceBoost.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, f: Boolean) { 
                val gain = AudioParameterMapper.progressToVoiceBoost(p)
                AudioEngineLib.setVoiceBoost(gain) 
                settingsManager.saveFloat(AudioSettingsManager.KEY_VOICE_BOOST, gain)
            }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })

        binding.seekBarMasterGain.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, f: Boolean) { 
                val gain = AudioParameterMapper.progressToMasterGain(p)
                AudioEngineLib.setMasterGain(gain) 
                settingsManager.saveFloat(AudioSettingsManager.KEY_MASTER_GAIN, gain)
            }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })

        binding.seekBarHpf.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, f: Boolean) { 
                val freq = AudioParameterMapper.progressToHpfFreq(p)
                AudioEngineLib.setHpfFreq(freq) 
                settingsManager.saveFloat("hpf_freq", freq)
            }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })

        binding.seekBarLpf.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, f: Boolean) { 
                val freq = AudioParameterMapper.progressToLpfFreq(p)
                AudioEngineLib.setLpfFreq(freq) 
                settingsManager.saveFloat("lpf_freq", freq)
            }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })

        binding.seekBarLimiter.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, f: Boolean) { 
                val thresh = AudioParameterMapper.progressToLimiterThreshold(p)
                AudioEngineLib.setLimiterThreshold(thresh) 
                settingsManager.saveFloat("limiter_thresh", thresh)
            }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })

        binding.seekBarTransient.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, f: Boolean) { 
                val strength = AudioParameterMapper.progressToTransientSuppression(p)
                AudioEngineLib.setTransientSuppression(strength) 
                settingsManager.saveFloat("transient_suppression", strength)
            }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })

        binding.seekBarNoiseGate.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, f: Boolean) { 
                val thresh = AudioParameterMapper.progressToNoiseGateThreshold(p)
                AudioEngineLib.setNoiseGateThreshold(thresh) 
                settingsManager.saveFloat(AudioSettingsManager.KEY_NOISE_GATE, thresh)
            }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })

        binding.seekBarMbRatio.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, f: Boolean) { 
                val ratio = AudioParameterMapper.progressToMbCompressionRatio(p)
                AudioEngineLib.setMbCompression(ratio) 
                settingsManager.saveFloat("mb_compression", ratio)
            }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })

        binding.swBeamforming.setOnCheckedChangeListener { _, isChecked ->
            AudioEngineLib.setBeamforming(isChecked)
            settingsManager.prefs.edit().putBoolean("beamforming", isChecked).apply()
            if (isChecked) {
                Toast.makeText(this, "AI Spatial Beamforming Active", Toast.LENGTH_SHORT).show()
            }
        }

        val eqSeekBars = listOf(binding.seekBarBand1, binding.seekBarBand2, binding.seekBarBand3, binding.seekBarBand4, binding.seekBarBand5)
        eqSeekBars.forEachIndexed { index, seekBar ->
            seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, p: Int, f: Boolean) { 
                    val gainDb = AudioParameterMapper.progressToEqBandGain(p)
                    AudioEngineLib.setEqualizerBandGain(index, gainDb)
                    settingsManager.saveFloat("band_$index", gainDb)
                }
                override fun onStartTrackingTouch(s: SeekBar?) {}
                override fun onStopTrackingTouch(s: SeekBar?) {}
            })
        }

        binding.seekBarDereverb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, f: Boolean) { 
                val strength = AudioParameterMapper.progressToDereverb(p)
                AudioEngineLib.setDereverbStrength(strength) 
                settingsManager.saveFloat("dereverb_strength", strength)
            }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })

        binding.seekBarHpss.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, f: Boolean) { 
                val strength = AudioParameterMapper.progressToHpss(p)
                AudioEngineLib.setHpssStrength(strength) 
                settingsManager.saveFloat("hpss_strength", strength)
            }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })

        binding.seekBarFreqWarp.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, f: Boolean) { 
                val strength = p / 100.0f
                AudioEngineLib.setFreqWarpStrength(strength) 
                settingsManager.saveFloat("freq_warp_strength", strength)
            }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })

        binding.seekBarNeuralMask.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, f: Boolean) { 
                val strength = AudioParameterMapper.progressToNeuralMask(p)
                AudioEngineLib.setNeuralMaskStrength(strength) 
                settingsManager.saveFloat("neural_mask_strength", strength)
            }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })

        binding.seekBarBassBoost.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, f: Boolean) { 
                val strength = AudioParameterMapper.progressToBassBoost(p)
                AudioEngineLib.setBassBoostStrength(strength) 
                settingsManager.saveFloat("bass_boost_strength", strength)
            }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })
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
                override fun onReadyForSpeech(params: Bundle?) {
                    isListeningActive = true
                }
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {
                    // Standard recognizer stops here, we need to restart
                    if (AudioEngineLib.isAudioEngineRunning()) {
                        lifecycleScope.launch { delay(100); startListening() }
                    }
                }
                override fun onError(error: Int) {
                    isListeningActive = false
                    
                    // Don't restart on fatal errors (busy/permissions), but do on timeouts
                    if (AudioEngineLib.isAudioEngineRunning() && error != SpeechRecognizer.ERROR_RECOGNIZER_BUSY) {
                        lifecycleScope.launch { delay(500); startListening() }
                    }
                }
                override fun onResults(results: Bundle?) {
                    isListeningActive = false
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        val text = matches[0]
                        binding.tvTranscription.text = text
                        
                        lifecycleScope.launch(Dispatchers.IO) {
                            val dominantMic = AudioEngineLib.getDominantMic()
                            val speaker = if (dominantMic == 1) "Speaker A (Watch)" else "Speaker B (Phone)"
                            
                            val db = EchoSenseDatabase.getDatabase(this@MainActivity)
                            db.conversationNoteDao().insertNote(ConversationNote(
                                text = text,
                                speakerLabel = speaker
                            ))
                        }
                    }
                    // Restart for continuous capture
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
        if (!AudioEngineLib.isAudioEngineRunning()) return
        
        runOnUiThread {
            try {
                speechRecognizer?.startListening(recognizerIntent)
            } catch (e: Exception) {
                isListeningActive = false
            }
        }
    }

    private fun stopListening() {
        isListeningActive = false
        speechRecognizer?.stopListening()
        speechRecognizer?.cancel()
    }

    private fun refreshSpeakerList() {
        val speakers = AudioEngineLib.getSpeakerInfo()
        val currentTargetId = settingsManager.getInt(AudioSettingsManager.KEY_TARGET_SPEAKER, -1)
        
        // Only update if something changed or we need to initialize
        if (binding.chipGroupSpeaker.childCount <= 1) { // Only "None" exists or empty
            // Ensure "None" exists
            if (binding.chipGroupSpeaker.findViewById<View>(R.id.chipSpeakerNone) == null) {
                val noneChip = com.google.android.material.chip.Chip(this).apply {
                    id = R.id.chipSpeakerNone
                    text = "None"
                    isCheckable = true
                    isChecked = currentTargetId == -1
                }
                binding.chipGroupSpeaker.addView(noneChip)
            }
        }

        speakers.forEach { speaker ->
            val chipId = 100 + speaker.id
            var chip = binding.chipGroupSpeaker.findViewById<com.google.android.material.chip.Chip>(chipId)
            
            if (chip == null) {
                chip = com.google.android.material.chip.Chip(this).apply {
                    id = chipId
                    isCheckable = true
                    text = if (speaker.id == 0) "Speaker A (Watch)" else "Speaker B (Phone)"
                    isChecked = currentTargetId == speaker.id
                    setOnCheckedChangeListener { _, isChecked ->
                        if (isChecked) {
                            AudioEngineLib.setTargetSpeaker(speaker.id)
                            settingsManager.saveInt(AudioSettingsManager.KEY_TARGET_SPEAKER, speaker.id)
                            Toast.makeText(this@MainActivity, "Isolating ${this.text}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                binding.chipGroupSpeaker.addView(chip)
            }
            
            // Visual feedback of activity
            if (speaker.isActive) {
                chip.chipIcon = ContextCompat.getDrawable(this, android.R.drawable.ic_btn_speak_now)
                chip.isChipIconVisible = true
            } else {
                chip.isChipIconVisible = false
            }
        }
    }

    private fun startVisualizer() {
        visualizerJob?.cancel()
        visualizerJob = lifecycleScope.launch {
            var counter = 0
            while (true) {
                if (AudioEngineLib.isAudioEngineRunning()) {
                    val volume = AudioEngineLib.getVolumeLevel()
                    binding.progressBarVolume.progress = (volume * 500).toInt().coerceIn(0, 100)
                    
                    val isolation = AudioEngineLib.getIsolationGainDb()
                    AudioEngineLib.getFftData(fftData)
                    AudioEngineLib.getEqCurveData(eqCurveData)
                    binding.visualizerView.updateData(fftData, eqCurveData, isolation)
                    
                    // Refresh speakers every ~300ms
                    if (counter % 10 == 0) {
                        refreshSpeakerList()
                    }
                    counter++
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
