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

class MainActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnToggle = findViewById<Button>(R.id.btnToggleWatchMic)
        tvStatus = findViewById<TextView>(R.id.tvWatchStatus)

        btnToggle.setOnClickListener {
            if (isServiceRunning(WearSensorService::class.java)) {
                stopSensorService()
                btnToggle.text = "Start Ambient Collector"
                tvStatus.text = "Status: Idle"
            } else {
                if (checkPermissions()) {
                    startSensorService()
                    btnToggle.text = "Stop Collector"
                    tvStatus.text = "Status: Collector Active"
                }
            }
        }

        // Initial UI state
        if (isServiceRunning(WearSensorService::class.java)) {
            btnToggle.text = "Stop Collector"
            tvStatus.text = "Status: Collector Active"
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
