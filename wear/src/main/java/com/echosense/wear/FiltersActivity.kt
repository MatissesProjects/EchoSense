package com.echosense.wear

import android.os.Bundle
import android.widget.Button
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class FiltersActivity : AppCompatActivity() {

    private val uiScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_filters)

        findViewById<Button>(R.id.btnBack).setOnClickListener { finish() }

        setupSeekBar(R.id.seekWatchGain, "/set_watch_gain")
        setupSeekBar(R.id.seekDereverb, "/set_dereverb")
        setupSeekBar(R.id.seekNeuralMask, "/set_neural_mask")
        setupSeekBar(R.id.seekHpss, "/set_hpss")
    }

    private fun setupSeekBar(id: Int, path: String) {
        findViewById<SeekBar>(id).setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, f: Boolean) {
                if (f) sendCommand(path, p.toString())
            }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })
    }

    private fun sendCommand(path: String, data: String) {
        uiScope.launch(Dispatchers.IO) {
            try {
                val nodes = Wearable.getNodeClient(this@FiltersActivity).connectedNodes.await()
                for (node in nodes) {
                    Wearable.getMessageClient(this@FiltersActivity)
                        .sendMessage(node.id, path, data.toByteArray())
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
