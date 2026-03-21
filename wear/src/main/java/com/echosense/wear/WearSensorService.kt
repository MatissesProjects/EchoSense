package com.echosense.wear

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.ChannelClient
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import java.io.OutputStream

class WearSensorService : Service() {

    private val TAG = "EchoSenseWearService"
    private val NOTIFICATION_ID = 1
    private val CHANNEL_ID = "WearSensorChannel"
    private val WEAR_AUDIO_CHANNEL = "/audio_stream_channel"

    private var isRecording = false
    private var serviceJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP_SERVICE") {
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(NOTIFICATION_ID, createNotification())
        startStreaming()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startStreaming() {
        if (isRecording) return
        isRecording = true
        
        serviceJob = CoroutineScope(Dispatchers.IO).launch {
            Log.d(TAG, "Streaming Coroutine Started - Waiting for Data Layer")
            delay(2000) // Give Data Layer a moment to sync
            
            val bufferSize = AudioRecord.getMinBufferSize(16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
            val audioRecord = AudioRecord(MediaRecorder.AudioSource.MIC, 16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize)
            
            val data = ByteArray(bufferSize)
            val channelClient = Wearable.getChannelClient(this@WearSensorService)
            val capabilityClient = Wearable.getCapabilityClient(this@WearSensorService)

            try {
                // Find phone via capability
                val capabilityInfo = capabilityClient.getCapability("echo_sense_phone", CapabilityClient.FILTER_REACHABLE).await()
                val phoneNode = capabilityInfo.nodes.firstOrNull()
                
                if (phoneNode == null) {
                    Log.e(TAG, "No phone node found with capability echo_sense_phone")
                    stopSelf()
                    return@launch
                }

                Log.d(TAG, "Connecting to phone: ${phoneNode.displayName}")
                val channel = channelClient.openChannel(phoneNode.id, WEAR_AUDIO_CHANNEL).await()
                val outputStream = channelClient.getOutputStream(channel).await()

                audioRecord.startRecording()
                Log.d(TAG, "Recording and Streaming...")

                var bytesSent = 0L
                while (isRecording && isActive) {
                    val read = audioRecord.read(data, 0, bufferSize)
                    if (read > 0) {
                        outputStream.write(data, 0, read)
                        bytesSent += read
                        if (bytesSent % 32000 == 0L) Log.d(TAG, "Sent $bytesSent bytes to phone")
                    }
                }
                
                outputStream.close()
                channelClient.close(channel).await()

            } catch (e: Exception) {
                Log.e(TAG, "Streaming error", e)
            } finally {
                audioRecord.stop()
                audioRecord.release()
                isRecording = false
            }
        }
    }

    override fun onDestroy() {
        isRecording = false
        serviceJob?.cancel()
        super.onDestroy()
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("EchoSense Collector")
            .setContentText("Streaming ambient noise to phone...")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(CHANNEL_ID, "Wear Sensor Channel", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }
}
