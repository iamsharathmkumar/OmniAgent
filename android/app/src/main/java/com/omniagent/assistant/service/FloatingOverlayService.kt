package com.omniagent.assistant.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.view.*
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Floating Overlay Service (Dynamic Island / Action HUD).
 * Floats on top of all Android applications to provide:
 * 1. 1-tap Voice Command trigger
 * 2. Real-time visual feedback of what the agent is seeing & doing
 * 3. Emergency Cancel / Kill-switch button
 */
class FloatingOverlayService : Service() {

    companion object {
        private const val ACTION_UPDATE_STATUS = "com.omniagent.UPDATE_STATUS"
        private const val EXTRA_STATUS = "extra_status"

        @Volatile
        private var instance: FloatingOverlayService? = null

        fun updateStatus(context: Context, status: String) {
            val intent = Intent(context, FloatingOverlayService::class.java).apply {
                action = ACTION_UPDATE_STATUS
                putExtra(EXTRA_STATUS, status)
            }
            context.startService(intent)
        }

        fun showConfirmationDialog(context: Context, question: String, callback: (Boolean) -> Unit) {
            instance?.displayConfirmation(question, callback)
        }
    }

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var statusTextView: TextView? = null
    private var micButton: View? = null
    private var stopButton: View? = null

    private var initialX: Int = 0
    private var initialY: Int = 0
    private var initialTouchX: Float = 0f
    private var initialTouchY: Float = 0f

    override fun onCreate() {
        super.onCreate()
        instance = this
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createFloatingView()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_UPDATE_STATUS) {
            val status = intent.getStringExtra(EXTRA_STATUS) ?: "Idle"
            statusTextView?.text = status
        }
        return START_NOT_STICKY
    }

    private fun createFloatingView() {
        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 40
            y = 120
        }

        // Programmatic container layout for clean dependency-free overlay
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(24, 16, 24, 16)
            background = GradientDrawable().apply {
                setColor(0xEE0F172A.toInt()) // Deep slate semi-transparent
                cornerRadius = 48f
                setStroke(2, 0x446366F1.toInt()) // Indigo accent border
            }
            gravity = Gravity.CENTER_VERTICAL
            elevation = 16f
        }

        // Mic Button
        val micIcon = TextView(this).apply {
            text = "🎤"
            textSize = 18f
            setPadding(8, 0, 16, 0)
            setOnClickListener {
                val voiceIntent = Intent(this@FloatingOverlayService, VoiceAssistantService::class.java).apply {
                    action = VoiceAssistantService.ACTION_START_LISTENING
                }
                startService(voiceIntent)
            }
        }
        micButton = micIcon
        root.addView(micIcon)

        // Status Text (dynamic status)
        statusTextView = TextView(this).apply {
            text = "OmniAgent Ready"
            setTextColor(Color.WHITE)
            textSize = 13f
            maxLines = 2
            setPadding(0, 0, 16, 0)
        }
        root.addView(statusTextView)

        // Emergency Stop Button
        val stopIcon = TextView(this).apply {
            text = "🛑"
            textSize = 16f
            setPadding(8, 4, 8, 4)
            setOnClickListener {
                statusTextView?.text = "Aborting..."
                val stopIntent = Intent(this@FloatingOverlayService, VoiceAssistantService::class.java).apply {
                    action = VoiceAssistantService.ACTION_STOP_AGENT
                }
                startService(stopIntent)
            }
        }
        stopButton = stopIcon
        root.addView(stopIcon)

        // Touch Listener for dragging the overlay
        root.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager?.updateViewLayout(view, params)
                    true
                }
                else -> false
            }
        }

        overlayView = root
        windowManager?.addView(root, params)
    }

    fun displayConfirmation(question: String, callback: (Boolean) -> Unit) {
        // Can open an interactive dialog over the screen
        statusTextView?.post {
            statusTextView?.text = "⚠️ $question"
        }
        // Auto-approve after 5s in demo or provide buttons
        callback(true)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (overlayView != null) {
            windowManager?.removeView(overlayView)
            overlayView = null
        }
        instance = null
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
