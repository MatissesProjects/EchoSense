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
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.echosense.app.utils.AudioConverter
import com.echosense.app.utils.AudioParameterMapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class EchoSenseService : Service() {

    private val TAG = "EchoSenseService"
    private val CHANNEL_ID = "EchoSenseServiceChannel"
    private val NOTIFICATION_ID = 1
    private val WEAR_AUDIO_CHANNEL = "/audio_stream_channel"
    private val SPEAKER_INFO_PATH = "/speaker_info"
    private val SET_TARGET_SPEAKER_PATH = "/set_target_speaker"

    private lateinit var channelClient: ChannelClient
    private lateinit var messageClient: MessageClient
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val messageListener = MessageClient.OnMessageReceivedListener { messageEvent ->
        val data = String(messageEvent.data)
        val settings = AudioSettingsManager(this)
        
        when (messageEvent.path) {
            SET_TARGET_SPEAKER_PATH -> {
                val speakerId = data.toIntOrNull() ?: -1
                AudioEngineLib.setTargetSpeaker(speakerId)
                settings.saveInt(AudioSettingsManager.KEY_TARGET_SPEAKER, speakerId)
            }
            "/set_watch_gain" -> {
                val p = data.toIntOrNull() ?: 40
                val gain = AudioParameterMapper.progressToWatchGain(p)
                AudioEngineLib.setRemoteGain(gain)
                settings.saveFloat(AudioSettingsManager.KEY_WATCH_GAIN, gain)
            }
            "/set_dereverb" -> {
                val p = data.toIntOrNull() ?: 0
                val strength = AudioParameterMapper.progressToDereverb(p)
                AudioEngineLib.setDereverbStrength(strength)
                settings.saveFloat("dereverb_strength", strength)
            }
            "/set_neural_mask" -> {
                val p = data.toIntOrNull() ?: 0
                val strength = AudioParameterMapper.progressToNeuralMask(p)
                AudioEngineLib.setNeuralMaskStrength(strength)
                settings.saveFloat("neural_mask_strength", strength)
            }
            "/set_hpss" -> {
                val p = data.toIntOrNull() ?: 0
                val strength = AudioParameterMapper.progressToHpss(p)
                AudioEngineLib.setHpssStrength(strength)
                settings.saveFloat("hpss_strength", strength)
            }
        }
    }

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
        
        messageClient = Wearable.getMessageClient(this)
        messageClient.addListener(messageListener)
        
        startSpeakerInfoBroadcaster()
        Log.d(TAG, "Service Created, ChannelCallback & MessageListener registered")
    }

    private fun startSpeakerInfoBroadcaster() {
        serviceScope.launch {
            while (true) {
                try {
                    if (AudioEngineLib.isAudioEngineRunning()) {
                        val speakers = AudioEngineLib.getSpeakerInfo()
                        // Format: "id:name:isActive|id:name:isActive"
                        val data = speakers.joinToString("|") { 
                            "${it.id}:${if(it.id == 0) "Speaker A" else "Speaker B"}:${it.isActive}"
                        }
                        
                        val nodes = Wearable.getNodeClient(this@EchoSenseService).connectedNodes.await()
                        for (node in nodes) {
                            messageClient.sendMessage(node.id, SPEAKER_INFO_PATH, data.toByteArray()).await()
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error in speaker info broadcaster", e)
                }
                delay(2000) // Polling every 2 seconds for efficiency
            }
        }
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
        messageClient.removeListener(messageListener)
        serviceScope.cancel()
        AudioEngineLib.stopAudioEngine()
        super.onDestroy()
    }

    private fun receiveAudioStream(channel: Channel) {
        Log.d(TAG, "Starting to receive audio stream from channel")
        serviceScope.launch {
            try {
                val inputStream = channelClient.getInputStream(channel).await()
                Log.d(TAG, "InputStream acquired on phone")
                val buffer = ByteArray(4096)

                var totalBytesRead = 0L
                while (true) {
                    val read = inputStream.read(buffer)
                    if (read == -1) {
                        Log.d(TAG, "InputStream reached EOF")
                        break
                    }
                    
                    totalBytesRead += read
                    if (totalBytesRead % 32000 == 0L) Log.d(TAG, "Read $totalBytesRead bytes on phone...")

                    val floatArray = AudioConverter.pcmToFloat(buffer, read)
                    AudioEngineLib.writeRemoteAudio(floatArray)
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
