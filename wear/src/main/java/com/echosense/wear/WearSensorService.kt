package com.echosense.wear

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.wearable.ChannelClient
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import java.io.OutputStream

class WearSensorService : Service() {

    private val TAG = "EchoSenseWearSync"
    private val NOTIFICATION_ID = 1
    private val CHANNEL_ID = "WearSyncChannel"
    private val WEAR_AUDIO_CHANNEL = "/audio_stream_channel"

    private val START_STREAMING_PATH = "/start_streaming"
    private val STOP_STREAMING_PATH = "/stop_streaming"
    private val START_BURST_PATH = "/start_burst"

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var streamingJob: Job? = null
    private var isStreaming = false

    private val messageListener = MessageClient.OnMessageReceivedListener { messageEvent ->
        when (messageEvent.path) {
            START_STREAMING_PATH -> startStreaming(false)
            START_BURST_PATH -> startStreaming(true)
            STOP_STREAMING_PATH -> stopStreaming()
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        Wearable.getMessageClient(this).addListener(messageListener)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP_SERVICE") {
            stopStreaming()
            stopSelf()
            return START_NOT_STICKY
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, createNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } else {
            startForeground(NOTIFICATION_ID, createNotification())
        }
        
        return START_STICKY
    }

    private fun startStreaming(isBurst: Boolean) {
        if (isStreaming) return
        isStreaming = true
        Log.d(TAG, "Starting audio streaming from watch (Burst: $isBurst)")
        
        streamingJob = serviceScope.launch {
            if (isBurst) {
                launch {
                    delay(30000) // 30 second burst
                    if (isStreaming) {
                        Log.d(TAG, "Burst timeout reached")
                        stopStreaming()
                        
                        // Notify phone that burst ended
                        val nodes = Wearable.getNodeClient(this@WearSensorService).connectedNodes.await()
                        for (node in nodes) {
                            Wearable.getMessageClient(this@WearSensorService)
                                .sendMessage(node.id, "/status_update", "Burst Ended".toByteArray())
                        }
                    }
                }
            }
            val sampleRate = 16000
            val bufferSize = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
            
            val audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )

            if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord initialization failed")
                isStreaming = false
                return@launch
            }

            try {
                val nodes = Wearable.getNodeClient(this@WearSensorService).connectedNodes.await()
                val phoneNode = nodes.find { it.isNearby } ?: nodes.firstOrNull()
                
                if (phoneNode == null) {
                    Log.e(TAG, "No phone node found")
                    isStreaming = false
                    return@launch
                }

                val channelClient = Wearable.getChannelClient(this@WearSensorService)
                val channel = channelClient.openChannel(phoneNode.id, WEAR_AUDIO_CHANNEL).await()
                val outputStream = channelClient.getOutputStream(channel).await()

                audioRecord.startRecording()
                val buffer = ByteArray(bufferSize)

                while (isActive && isStreaming) {
                    val read = audioRecord.read(buffer, 0, buffer.size)
                    if (read > 0) {
                        outputStream.write(buffer, 0, read)
                    }
                }

                audioRecord.stop()
                audioRecord.release()
                outputStream.close()
                channelClient.close(channel)
                Log.d(TAG, "Streaming stopped cleanly")

            } catch (e: Exception) {
                Log.e(TAG, "Error during streaming", e)
            } finally {
                isStreaming = false
            }
        }
    }

    private fun stopStreaming() {
        isStreaming = false
        streamingJob?.cancel()
        streamingJob = null
        Log.d(TAG, "Streaming stop requested")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        Log.d(TAG, "Sync Service being destroyed")
        stopStreaming()
        Wearable.getMessageClient(this).removeListener(messageListener)
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("EchoSense Remote")
            .setContentText("Syncing with phone...")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(CHANNEL_ID, "Wear Sync Channel", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }
}

