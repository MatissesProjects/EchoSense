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
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import androidx.wear.widget.WearableLinearLayoutManager
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

        findViewById<RecyclerView>(R.id.recyclerSpeakers).apply {
            layoutManager = WearableLinearLayoutManager(this@MainActivity)
            visibility = View.VISIBLE
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

        findViewById<Button>(R.id.btnRemoteFilters).setOnClickListener {
            startActivity(Intent(this, FiltersActivity::class.java))
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
            val recyclerView = findViewById<RecyclerView>(R.id.recyclerSpeakers)

            // Simple dynamic adapter for the prototype
            recyclerView.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
                override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                    val btn = Button(this@MainActivity).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                    }
                    return object : RecyclerView.ViewHolder(btn) {}
                }

                override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                    val parts = speakerStrings[position].split(":")
                    if (parts.size == 3) {
                        val id = parts[0]
                        val name = parts[1]
                        val isActive = parts[2].toBoolean()

                        (holder.itemView as Button).apply {
                            text = if (isActive) "● $name" else name
                            textSize = 10f
                            setOnClickListener {
                                sendTargetSpeaker(id)
                            }
                        }
                    }
                }

                override fun getItemCount(): Int = speakerStrings.size
            }
        }
    }    private fun sendTargetSpeaker(id: String) {
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
