package com.echosense.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
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
    private val RECORD_AUDIO_PERMISSION_CODE = 1
    
    private val fftData = FloatArray(64)
    private val eqCurveData = FloatArray(100)

    private var speechRecognizer: SpeechRecognizer? = null
    private lateinit var recognizerIntent: Intent
    private var visualizerJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setupSpeechRecognizer()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), RECORD_AUDIO_PERMISSION_CODE)
        }
    }

    override fun onResume() {
        super.onResume()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            startAudioEngine()
            startVisualizer()
            startListening()
            binding.btnToggleEngine.text = "Stop Engine"
        }
    }

    override fun onPause() {
        super.onPause()
        stopListening()
        stopAudioEngine()
        visualizerJob?.cancel()
    }

    private fun setupUI() {
        binding.btnToggleEngine.setOnClickListener {
            if (isAudioEngineRunning()) {
                stopAudioEngine()
                stopListening()
                binding.btnToggleEngine.text = "Start Engine"
            } else {
                startAudioEngine()
                startListening()
                binding.btnToggleEngine.text = "Stop Engine"
            }
        }

        binding.btnAutoTune.setOnClickListener {
            autoTune()
            Toast.makeText(this, "AI Auto-Tune Applied", Toast.LENGTH_SHORT).show()
        }

        binding.seekBarPreAmp.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                setPreAmpGain(progress / 10.0f)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        binding.seekBarVoiceBoost.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
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
                    if (isAudioEngineRunning()) startListening()
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        binding.tvTranscription.text = matches[0]
                    }
                    if (isAudioEngineRunning()) startListening()
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        binding.tvTranscription.text = matches[0]
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }
    }

    private fun startListening() {
        speechRecognizer?.startListening(recognizerIntent)
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
                startListening()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAudioEngine()
        speechRecognizer?.destroy()
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