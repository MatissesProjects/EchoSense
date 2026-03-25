package com.echosense.app

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.Button
import android.widget.Toast

class EchoSenseAccessibilityService : AccessibilityService() {

    private var floatingView: View? = null
    private var windowManager: WindowManager? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        showFloatingButton()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Not used for now, but required to override
    }

    override fun onInterrupt() {
        // Not used
    }

    private fun showFloatingButton() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        
        floatingView = inflater.inflate(R.layout.layout_floating_button, null)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        params.gravity = Gravity.TOP or Gravity.END
        params.x = 0
        params.y = 100

        windowManager?.addView(floatingView, params)

        val btnToggle = floatingView?.findViewById<Button>(R.id.btnFloatingToggle)
        btnToggle?.setOnClickListener {
            toggleProfile()
        }

        floatingView?.setOnLongClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            true
        }
    }

    private var currentProfile = 0
    private fun toggleProfile() {
        // Cycle through Voice, Music, TV, Custom
        currentProfile = (currentProfile + 1) % 4
        AudioEngineLib.setProfile(currentProfile)
        
        val profileName = when (currentProfile) {
            0 -> "Voice"
            1 -> "Music"
            2 -> "TV"
            else -> "Custom"
        }
        
        Toast.makeText(this, "EchoSense: $profileName Mode", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (floatingView != null) {
            windowManager?.removeView(floatingView)
        }
    }
}
