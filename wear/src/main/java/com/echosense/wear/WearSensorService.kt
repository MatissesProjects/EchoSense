package com.echosense.wear

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await

class WearSensorService : Service() {

    private val TAG = "EchoSenseWearSync"
    private val NOTIFICATION_ID = 1
    private val CHANNEL_ID = "WearSyncChannel"

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP_SERVICE") {
            stopSelf()
            return START_NOT_STICKY
        }

        // We run a minimal foreground service to keep the remote sync alive
        startForeground(NOTIFICATION_ID, createNotification())
        
        startHeartbeat()
        
        return START_STICKY
    }

    private fun startHeartbeat() {
        serviceScope.launch {
            while (isActive) {
                // Periodically check connection or send keep-alive if needed
                // Most sync happens via MessageClient in MainActivity
                delay(10000) 
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        Log.d(TAG, "Sync Service being destroyed")
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
