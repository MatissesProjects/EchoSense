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
import com.google.android.gms.wearable.ChannelClient
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.OutputStream

class MainActivity : AppCompatActivity() {

    private var isRecording = false
    private var recordingJob: Job? = null
    private val WEAR_AUDIO_CHANNEL = "/audio_stream_channel"

    private lateinit var tvStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnToggle = findViewById<Button>(R.id.btnToggleWatchMic)
        tvStatus = findViewById<TextView>(R.id.tvWatchStatus)

        btnToggle.setOnClickListener {
            if (isRecording) {
                stopRecording()
                btnToggle.text = "Start Ambient Collector"
                updateStatus("Status: Idle")
            } else {
                if (checkPermissions()) {
                    startRecording()
                    btnToggle.text = "Stop Collector"
                    updateStatus("Status: Connecting...")
                }
            }
        }
    }

    private fun updateStatus(text: String) {
        runOnUiThread { tvStatus.text = text }
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
            
            val channelClient = Wearable.getChannelClient(this@MainActivity)
            val nodeClient = Wearable.getNodeClient(this@MainActivity)

            try {
                val nodes = nodeClient.connectedNodes.await()
                if (nodes.isEmpty()) {
                    updateStatus("Status: No Phone Found")
                    return@launch
                }

                val phoneNode = nodes[0] // Assuming the first connected node is the phone
                val channel = channelClient.openChannel(phoneNode.id, WEAR_AUDIO_CHANNEL).await()
                val outputStream = channelClient.getOutputStream(channel).await()

                updateStatus("Status: Collecting Ambient...")
                audioRecord.startRecording()

                while (isRecording) {
                    val read = audioRecord.read(data, 0, bufferSize)
                    if (read > 0) {
                        outputStream.write(data, 0, read)
                    }
                }
                
                outputStream.close()
                channelClient.close(channel).await()

            } catch (e: Exception) {
                e.printStackTrace()
                updateStatus("Status: Connection Error")
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
