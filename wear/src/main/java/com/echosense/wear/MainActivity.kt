package com.echosense.wear

import android.Manifest
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

import android.view.View
import android.widget.LinearLayout
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MainActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView
    private lateinit var speakerLayout: LinearLayout
    private val uiScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val SPEAKER_INFO_PATH = "/speaker_info"
    private val SET_TARGET_SPEAKER_PATH = "/set_target_speaker"

    private val messageListener = MessageClient.OnMessageReceivedListener { messageEvent ->
        if (messageEvent.path == SPEAKER_INFO_PATH) {
            val data = String(messageEvent.data)
            refreshSpeakerList(data)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnToggle = findViewById<Button>(R.id.btnToggleWatchMic)
        tvStatus = findViewById<TextView>(R.id.tvWatchStatus)

        // We'll use a simple layout for the prototype instead of full adapter
        speakerLayout = LinearLayout(this).apply {
            orientation = LinearLayout.vertical
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        findViewById<androidx.wear.widget.ScalingLazyColumn>(R.id.recyclerSpeakers).apply {
            visibility = View.VISIBLE
            // Note: In a real app we'd use a ScalingLazyColumn.Adapter, 
            // but for this direct prototype we'll just use the list view.
        }

        btnToggle.setOnClickListener {
            if (isServiceRunning(WearSensorService::class.java)) {
                stopSensorService()
                btnToggle.text = "Start Watch Mic"
                tvStatus.text = "Status: Idle"
            } else {
                if (checkPermissions()) {
                    startSensorService()
                    btnToggle.text = "Stop Mic"
                    tvStatus.text = "Status: Streaming"
                }
            }
        }

        Wearable.getMessageClient(this).addListener(messageListener)

        // Initial UI state
        if (isServiceRunning(WearSensorService::class.java)) {
            btnToggle.text = "Stop Mic"
            tvStatus.text = "Status: Streaming"
        }
    }

    override fun onDestroy() {
        uiScope.cancel()
        Wearable.getMessageClient(this).removeListener(messageListener)
        super.onDestroy()
    }

    private fun refreshSpeakerList(data: String) {
        // data format: "id:name:isActive|id:name:isActive"
        val speakerStrings = data.split("|")

        uiScope.launch {
            // Re-using the ScalingLazyColumn as a container for this prototype
            val container = findViewById<androidx.wear.widget.ScalingLazyColumn>(R.id.recyclerSpeakers)
            container.removeAllViews() 

            speakerStrings.forEach { s ->
                val parts = s.split(":")
                if (parts.size == 3) {
                    val id = parts[0]
                    val name = parts[1]
                    val isActive = parts[2].toBoolean()

                    val btn = Button(this@MainActivity).apply {
                        text = if (isActive) "● $name" else name
                        textSize = 10f
                        setOnClickListener {
                            sendTargetSpeaker(id)
                        }
                    }
                    container.addView(btn)
                }
            }
        }
    }

    private fun sendTargetSpeaker(id: String) {
        uiScope.launch(Dispatchers.IO) {
            try {
                val nodes = Wearable.getNodeClient(this@MainActivity).connectedNodes.await()
                for (node in nodes) {
                    Wearable.getMessageClient(this@MainActivity)
                        .sendMessage(node.id, SET_TARGET_SPEAKER_PATH, id.toByteArray())
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun checkPermissions(): Boolean {
        val permissions = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        
        val missing = permissions.filter { 
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED 
        }
        
        if (missing.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missing.toTypedArray(), 1)
            return false
        }
        return true
    }

    private fun startSensorService() {
        val intent = Intent(this, WearSensorService::class.java)
        ContextCompat.startForegroundService(this, intent)
    }

    private fun stopSensorService() {
        val intent = Intent(this, WearSensorService::class.java).apply {
            action = "STOP_SERVICE"
        }
        startService(intent)
    }

    private fun isServiceRunning(serviceClass: Class<*>): Boolean {
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }
}
