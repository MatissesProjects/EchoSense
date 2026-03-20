package com.echosense.app

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import java.nio.ByteBuffer
import java.nio.ByteOrder

class EchoSenseService : Service(), MessageClient.OnMessageReceivedListener {

    private val CHANNEL_ID = "EchoSenseServiceChannel"
    private val NOTIFICATION_ID = 1
    private val WEAR_AUDIO_PATH = "/audio_stream"

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        Wearable.getMessageClient(this).addListener(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP_SERVICE") {
            stopSelf()
            return START_NOT_STICKY
        }
        
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)
        
        startAudioEngine()
        
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        Wearable.getMessageClient(this).removeListener(this)
        stopAudioEngine()
        super.onDestroy()
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path == WEAR_AUDIO_PATH) {
            val pcmData = messageEvent.data
            val shortBuffer = ByteBuffer.wrap(pcmData).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
            val floatArray = FloatArray(shortBuffer.remaining())
            for (i in floatArray.indices) floatArray[i] = shortBuffer.get().toFloat() / 32768.0f
            writeRemoteAudio(floatArray)
        }
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

    // JNI calls
    private external fun startAudioEngine()
    private external fun stopAudioEngine()
    private external fun writeRemoteAudio(data: FloatArray)

    companion object {
        init {
            System.loadLibrary("echosense_native")
        }
    }
}