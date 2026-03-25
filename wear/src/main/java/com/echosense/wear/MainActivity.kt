package com.echosense.wear

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
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
    private val TOGGLE_MIC_PATH = "/toggle_mic_source"

    private val messageListener = MessageClient.OnMessageReceivedListener { messageEvent ->
        when (messageEvent.path) {
            SPEAKER_INFO_PATH -> {
                val data = String(messageEvent.data)
                refreshSpeakerList(data)
            }
            "/status_update" -> {
                val data = String(messageEvent.data)
                tvStatus.text = data
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnFocus = findViewById<Button>(R.id.btnToggleWatchMic) // Reusing ID for Focus toggle
        tvStatus = findViewById<TextView>(R.id.tvWatchStatus)

        btnFocus.text = "Switch Mic Focus"
        tvStatus.text = "Remote Syncing..."

        findViewById<RecyclerView>(R.id.recyclerSpeakers).apply {
            layoutManager = WearableLinearLayoutManager(this@MainActivity)
            visibility = View.VISIBLE
        }

        btnFocus.setOnClickListener {
            sendCommand(TOGGLE_MIC_PATH, "toggle")
        }

        findViewById<Button>(R.id.btnRemoteFilters).setOnClickListener {
            startActivity(Intent(this, FiltersActivity::class.java))
        }

        Wearable.getMessageClient(this).addListener(messageListener)
        
        // Start sync service if not running
        if (!isServiceRunning(WearSensorService::class.java)) {
            startService(Intent(this, WearSensorService::class.java))
        }
    }

    override fun onDestroy() {
        uiScope.cancel()
        Wearable.getMessageClient(this).removeListener(messageListener)
        super.onDestroy()
    }

    private fun refreshSpeakerList(data: String) {
        // data format: "id:name:isActive|id:name:isActive"
        if (data.isEmpty()) return
        val speakerStrings = data.split("|")

        uiScope.launch {
            val recyclerView = findViewById<RecyclerView>(R.id.recyclerSpeakers)
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
                    if (parts.size >= 2) {
                        val id = parts[0]
                        val name = parts[1]
                        val isActive = if(parts.size > 2) parts[2].toBoolean() else false

                        (holder.itemView as Button).apply {
                            text = if (isActive) "● $name" else name
                            textSize = 10f
                            setOnClickListener {
                                sendCommand(SET_TARGET_SPEAKER_PATH, id)
                            }
                        }
                    }
                }

                override fun getItemCount(): Int = speakerStrings.size
            }
        }
    }

    private fun sendCommand(path: String, data: String) {
        uiScope.launch(Dispatchers.IO) {
            try {
                val nodes = Wearable.getNodeClient(this@MainActivity).connectedNodes.await()
                for (node in nodes) {
                    Wearable.getMessageClient(this@MainActivity)
                        .sendMessage(node.id, path, data.toByteArray())
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
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
