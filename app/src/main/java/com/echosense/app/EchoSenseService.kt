package com.echosense.app

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.wearable.ChannelClient
import com.google.android.gms.wearable.ChannelClient.Channel
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class EchoSenseService : Service() {

    private val TAG = "EchoSenseService"
    private val CHANNEL_ID = "EchoSenseServiceChannel"
    private val NOTIFICATION_ID = 1
    private val WEAR_AUDIO_CHANNEL = "/audio_stream_channel"

    private lateinit var channelClient: ChannelClient
    private var channelJob: Job? = null

    private val channelCallback = object : ChannelClient.ChannelCallback() {
        override fun onChannelOpened(channel: Channel) {
            Log.d(TAG, "Channel opened on phone: ${channel.path}")
            if (channel.path == WEAR_AUDIO_CHANNEL) {
                receiveAudioStream(channel)
            }
        }
        override fun onChannelClosed(channel: Channel, closeReason: Int, appSpecificErrorCode: Int) {
            Log.d(TAG, "Channel closed: ${channel.path}, reason: $closeReason")
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        channelClient = Wearable.getChannelClient(this)
        channelClient.registerChannelCallback(channelCallback)
        Log.d(TAG, "Service Created, ChannelCallback registered")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP_SERVICE") {
            Log.d(TAG, "Stop service action received")
            stopSelf()
            return START_NOT_STICKY
        }
        
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)
        
        AudioEngineLib.startAudioEngine()
        AudioEngineLib.restoreSettings(this)
        Log.d(TAG, "Audio Engine started and settings restored")
        
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        Log.d(TAG, "Service being destroyed")
        channelClient.unregisterChannelCallback(channelCallback)
        channelJob?.cancel()
        AudioEngineLib.stopAudioEngine()
        super.onDestroy()
    }

    private fun receiveAudioStream(channel: Channel) {
        Log.d(TAG, "Starting to receive audio stream from channel")
        channelJob?.cancel()
        channelJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                val inputStream = channelClient.getInputStream(channel).await()
                Log.d(TAG, "InputStream acquired on phone")
                val buffer = ByteArray(4096)
                val floatArray = FloatArray(2048)

                var totalBytesRead = 0L
                while (true) {
                    val read = inputStream.read(buffer)
                    if (read == -1) {
                        Log.d(TAG, "InputStream reached EOF")
                        break
                    }
                    
                    totalBytesRead += read
                    if (totalBytesRead % 32000 == 0L) Log.d(TAG, "Read $totalBytesRead bytes on phone...")

                    val shortBuffer = ByteBuffer.wrap(buffer, 0, read).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
                    val shortsToRead = shortBuffer.remaining()
                    if (shortsToRead > floatArray.size) {
                        // This shouldn't happen with 4096 byte buffer and 2048 float array
                        continue
                    }
                    for (i in 0 until shortsToRead) {
                        floatArray[i] = shortBuffer.get().toFloat() / 32768.0f
                    }
                    AudioEngineLib.writeRemoteAudio(floatArray.copyOf(shortsToRead))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in receiveAudioStream", e)
            }
        }
    }

    // Helper to await Task in Coroutine
    private suspend fun <T> com.google.android.gms.tasks.Task<T>.await(): T {
        return com.google.android.gms.tasks.Tasks.await(this)
    }

    private fun createNotification(): Notification {
        val pendingIntent: PendingIntent = Intent(this, MainActivity::class.java).let { notificationIntent ->
            PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)
        }

        val stopIntent = Intent(this, EchoSenseService::class.java).apply { action = "STOP_SERVICE" }
        val stopPendingIntent = PendingIntent.getService(this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("EchoSense Active")
            .setContentText("Intelligent hearing assistant is running.")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "EchoSense Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }
}
