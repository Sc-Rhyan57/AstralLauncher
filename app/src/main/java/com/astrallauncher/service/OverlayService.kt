package com.astrallauncher.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat

class OverlayService : Service() {

    private lateinit var wm: WindowManager
    private var bubble: View? = null
    private var panel: View? = null
    private var panelVisible = false

    companion object {
        const val CHANNEL_ID = "astral_overlay"
        const val EXTRA_MOD_NAME = "mod_name"
        const val EXTRA_AU_VERSION = "au_version"
        var instance: OverlayService? = null
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        startForeground(1, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val modName = intent?.getStringExtra(EXTRA_MOD_NAME) ?: "Astral Launcher"
        val auVersion = intent?.getStringExtra(EXTRA_AU_VERSION) ?: "Unknown"
        showBubble(modName, auVersion)
        return START_STICKY
    }

    private fun showBubble(modName: String, auVersion: String) {
        bubble?.let { wm.removeView(it) }
        panel?.let { wm.removeView(it) }

        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE

        val bubbleParams = WindowManager.LayoutParams(
            120, 120,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 30
            y = 200
        }

        val bubbleView = ImageView(this).apply {
            setBackgroundResource(android.R.drawable.ic_menu_info_details)
        }

        var startX = 0f; var startY = 0f
        var initX = 0; var initY = 0

        bubbleView.setOnTouchListener { v, ev ->
            when (ev.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = ev.rawX; startY = ev.rawY
                    initX = bubbleParams.x; initY = bubbleParams.y
                    false
                }
                MotionEvent.ACTION_MOVE -> {
                    bubbleParams.x = initX + (ev.rawX - startX).toInt()
                    bubbleParams.y = initY + (ev.rawY - startY).toInt()
                    wm.updateViewLayout(v, bubbleParams)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (Math.abs(ev.rawX - startX) < 10 && Math.abs(ev.rawY - startY) < 10) {
                        togglePanel(modName, auVersion, overlayType)
                    }
                    false
                }
                else -> false
            }
        }

        wm.addView(bubbleView, bubbleParams)
        bubble = bubbleView
    }

    private fun togglePanel(modName: String, auVersion: String, type: Int) {
        if (panelVisible) {
            panel?.let { wm.removeView(it) }
            panel = null
            panelVisible = false
            return
        }

        val panelParams = WindowManager.LayoutParams(
            600, WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 160; y = 200
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
            setBackgroundColor(0xEE161616.toInt())
        }

        fun addRow(label: String, value: String) {
            val row = LinearLayout(this@OverlayService).apply { orientation = LinearLayout.HORIZONTAL }
            row.addView(TextView(this@OverlayService).apply {
                text = label; setTextColor(0xFF888888.toInt()); textSize = 12f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            row.addView(TextView(this@OverlayService).apply {
                text = value; setTextColor(0xFFD4A843.toInt()); textSize = 12f
            })
            layout.addView(row)
        }

        layout.addView(TextView(this).apply {
            text = "✦ Astral Launcher"
            setTextColor(0xFFD4A843.toInt())
            textSize = 14f
            setPadding(0, 0, 0, 16)
        })
        addRow("Active Mod:", modName)
        addRow("AU Version:", auVersion)
        addRow("Status:", "Running")

        val closeBtn = TextView(this).apply {
            text = "✕ Close"
            setTextColor(0xFFE53935.toInt())
            textSize = 12f
            setPadding(0, 16, 0, 0)
            setOnClickListener { togglePanel(modName, auVersion, type) }
        }
        layout.addView(closeBtn)

        wm.addView(layout, panelParams)
        panel = layout
        panelVisible = true
    }

    private fun buildNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Astral Overlay", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Astral Launcher Active")
            .setContentText("Overlay running")
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        bubble?.let { try { wm.removeView(it) } catch (e: Exception) { } }
        panel?.let { try { wm.removeView(it) } catch (e: Exception) { } }
    }
}
