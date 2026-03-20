package com.echosense.wear

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private var isRecording = false
    private var recordingJob: Job? = null
    private val WEAR_AUDIO_PATH = "/audio_stream"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnToggle = findViewById<Button>(R.id.btnToggleWatchMic)
        val tvStatus = findViewById<TextView>(R.id.tvWatchStatus)

        btnToggle.setOnClickListener {
            if (isRecording) {
                stopRecording()
                btnToggle.text = "Start Watch Mic"
                tvStatus.text = "Status: Idle"
            } else {
                if (checkPermissions()) {
                    startRecording()
                    btnToggle.text = "Stop Watch Mic"
                    tvStatus.text = "Status: Streaming to Phone..."
                }
            }
        }
    }

    private fun checkPermissions(): Boolean {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 1)
            return false
        }
        return true
    }

    private fun startRecording() {
        isRecording = true
        recordingJob = CoroutineScope(Dispatchers.IO).launch {
            val bufferSize = AudioRecord.getMinBufferSize(16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
            val audioRecord = AudioRecord(MediaRecorder.AudioSource.MIC, 16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize)
            
            val data = ByteArray(bufferSize)
            audioRecord.startRecording()

            val messageClient = Wearable.getMessageClient(this@MainActivity)
            
            try {
                val nodes = com.google.android.gms.tasks.Tasks.await(Wearable.getNodeClient(this@MainActivity).connectedNodes)
                
                while (isRecording) {
                    val read = audioRecord.read(data, 0, bufferSize)
                    if (read > 0) {
                        val payload = data.copyOf(read)
                        nodes.forEach { node ->
                            messageClient.sendMessage(node.id, WEAR_AUDIO_PATH, payload)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                audioRecord.stop()
                audioRecord.release()
            }
        }
    }

    private fun stopRecording() {
        isRecording = false
        recordingJob?.cancel()
    }
}