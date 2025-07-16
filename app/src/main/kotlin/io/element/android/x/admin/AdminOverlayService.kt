package io.element.android.x.admin

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import io.element.android.x.R

class AdminOverlayService : Service() {
    private var windowManager: WindowManager? = null
    private var overlayView: View? = null

    override fun onCreate() {
        super.onCreate()
        createOverlay()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createOverlay() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        
        // Create overlay view
        overlayView = LayoutInflater.from(this).inflate(R.layout.admin_overlay, null)
        
        // Set up click listener for admin dashboard
        overlayView?.findViewById<View>(R.id.admin_fab)?.setOnClickListener {
            // Show admin dashboard content
            showAdminDashboard()
        }
        
        // Configure window parameters
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        
        params.gravity = Gravity.BOTTOM or Gravity.END
        params.x = 50
        params.y = 100
        
        windowManager?.addView(overlayView, params)
    }
    
    private fun showAdminDashboard() {
        // Simple admin dashboard - just show a text overlay for now
        val dashboardView = TextView(this).apply {
            text = "Admin Dashboard\n\nWelcome nabil.baig@gmail.com!\n\nFeatures:\n• User Management\n• Document Review\n• System Settings\n\n(Coming Soon)"
            textSize = 16f
            setPadding(32, 32, 32, 32)
            setBackgroundColor(0xDD000000.toInt())
            setTextColor(0xFFFFFFFF.toInt())
        }
        
        val dashboardParams = WindowManager.LayoutParams(
            800,
            600,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        
        dashboardParams.gravity = Gravity.CENTER
        
        windowManager?.addView(dashboardView, dashboardParams)
        
        // Auto-dismiss after 5 seconds
        dashboardView.postDelayed({
            try {
                windowManager?.removeView(dashboardView)
            } catch (e: Exception) {
                // View might already be removed
            }
        }, 5000)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        overlayView?.let { windowManager?.removeView(it) }
    }
    
    companion object {
        fun start(context: Context) {
            val intent = Intent(context, AdminOverlayService::class.java)
            context.startService(intent)
        }
        
        fun stop(context: Context) {
            val intent = Intent(context, AdminOverlayService::class.java)
            context.stopService(intent)
        }
    }
} 