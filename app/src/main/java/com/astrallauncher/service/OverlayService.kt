package com.astrallauncher.service

import android.animation.ValueAnimator
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.OvalShape
import android.os.Build
import android.os.IBinder
import android.view.*
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.*
import androidx.core.app.NotificationCompat
import com.astrallauncher.util.AppLogger
import com.astrallauncher.util.LuaRunner
import kotlin.math.abs

class OverlayService : Service() {

    private lateinit var wm: WindowManager
    private var bubbleView: View? = null
    private var panelView: View? = null
    private var panelVisible = false
    private var bubbleParams: WindowManager.LayoutParams? = null

    companion object {
        const val CHANNEL_ID = "astral_overlay"
        const val EXTRA_MOD = "mod_name"
        const val EXTRA_AU_VER = "au_version"
        const val EXTRA_MODS_COUNT = "mods_count"
        var instance: OverlayService? = null
        private const val TAG = "OverlayService"
        private val C_BG = Color.parseColor("#F0080808")
        private val C_SURFACE = Color.parseColor("#EE111111")
        private val C_CARD = Color.parseColor("#EE161616")
        private val C_GOLD = Color.parseColor("#D4A843")
        private val C_GOLD_DIM = Color.parseColor("#1A1508")
        private val C_MUTED = Color.parseColor("#666666")
        private val C_WHITE = Color.parseColor("#EEEEEE")
        private val C_PURPLE = Color.parseColor("#9C6FFF")
        private val C_GREEN = Color.parseColor("#4CAF50")
        private val C_RED = Color.parseColor("#E53935")
        private val C_BORDER = Color.parseColor("#222222")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        startForeground(1, buildNotification())
        AppLogger.i(TAG, "Overlay service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val mod = intent?.getStringExtra(EXTRA_MOD) ?: "Vanilla"
        val ver = intent?.getStringExtra(EXTRA_AU_VER) ?: "?"
        val cnt = intent?.getIntExtra(EXTRA_MODS_COUNT, 0) ?: 0
        removeBubble(); removePanel()
        showBubble(mod, ver, cnt)
        return START_STICKY
    }

    private val overlayType get() =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE

    private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()

    private fun showBubble(mod: String, ver: String, cnt: Int) {
        val params = WindowManager.LayoutParams(dp(56), dp(56), overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT).apply {
            gravity = Gravity.TOP or Gravity.END; x = dp(12); y = dp(130)
        }
        bubbleParams = params

        val frame = FrameLayout(this)

        val outer = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.parseColor("#CC050505"))
            setStroke(dp(2), C_GOLD)
        }

        val glow = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.TRANSPARENT)
            setStroke(dp(6), C_GOLD.and(0x33FFFFFF.toInt()))
        }

        val glowView = View(this).apply { background = glow }
        frame.addView(glowView, FrameLayout.LayoutParams(dp(56), dp(56)))

        val core = TextView(this).apply {
            text = "✦"; textSize = 22f; gravity = Gravity.CENTER
            setTextColor(C_GOLD); typeface = Typeface.DEFAULT_BOLD; background = outer
        }
        frame.addView(core, FrameLayout.LayoutParams(dp(52), dp(52)).apply {
            gravity = Gravity.CENTER
        })

        ValueAnimator.ofFloat(0.6f, 1f).apply {
            duration = 1600; repeatCount = ValueAnimator.INFINITE; repeatMode = ValueAnimator.REVERSE
            interpolator = DecelerateInterpolator()
            addUpdateListener { v ->
                val a = v.animatedValue as Float
                frame.scaleX = a * 0.05f + 0.95f; frame.scaleY = frame.scaleX
                glowView.alpha = a * 0.4f
            }
        }.start()

        var startX = 0f; var startY = 0f; var ox = 0; var oy = 0; var moved = false
        frame.setOnTouchListener { v, ev ->
            when (ev.action) {
                MotionEvent.ACTION_DOWN -> { startX = ev.rawX; startY = ev.rawY; ox = params.x; oy = params.y; moved = false; true }
                MotionEvent.ACTION_MOVE -> {
                    val dx = ev.rawX - startX; val dy = ev.rawY - startY
                    if (abs(dx) > 8 || abs(dy) > 8) moved = true
                    params.x = (ox - dx).toInt().coerceAtLeast(0)
                    params.y = (oy + dy).toInt().coerceAtLeast(0)
                    wm.updateViewLayout(v, params); true
                }
                MotionEvent.ACTION_UP -> { if (!moved) togglePanel(mod, ver, cnt); true }
                else -> false
            }
        }

        wm.addView(frame, params)
        bubbleView = frame
        frame.scaleX = 0f; frame.scaleY = 0f
        frame.animate().scaleX(1f).scaleY(1f).setDuration(300).setInterpolator(OvershootInterpolator()).start()
        AppLogger.i(TAG, "Bubble shown for mod=$mod ver=$ver")
    }

    private fun togglePanel(mod: String, ver: String, cnt: Int) {
        if (panelVisible) { removePanel(); return }
        showPanel(mod, ver, cnt)
    }

    private fun showPanel(mod: String, ver: String, cnt: Int) {
        val params = WindowManager.LayoutParams(dp(310), WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT).apply {
            gravity = Gravity.TOP or Gravity.END
            x = dp(12)
            y = (bubbleParams?.y ?: dp(130)) + dp(68)
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = roundRect(dp(20), C_BG, C_BORDER, 1)
            setPadding(dp(16), dp(14), dp(16), dp(16))
        }

        root.addView(buildHeader { removePanel() })
        root.addView(buildDivider())
        root.addView(buildInfoRow("Mod", mod, C_GOLD))
        root.addView(buildInfoRow("AU", ver, C_WHITE))
        root.addView(buildInfoRow("Mods", "$cnt loaded", if (cnt > 0) C_GREEN else C_MUTED))
        root.addView(buildInfoRow("Status", "● Running", C_GREEN))
        root.addView(buildDivider())

        val executorView = buildExecutorView()
        val logsView = buildLogsView()
        val infoView = buildInfoView(mod, ver)
        val tabContent = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        tabContent.addView(executorView); tabContent.addView(logsView); tabContent.addView(infoView)

        val tabRow = buildTabRow(
            listOf("▶ Execute", "📋 Logs", "ℹ Info"),
            listOf(executorView, logsView, infoView)
        )
        root.addView(tabRow)
        root.addView(tabContent)

        root.alpha = 0f; root.translationY = -dp(20).toFloat()
        wm.addView(root, params)
        panelView = root; panelVisible = true
        root.animate().alpha(1f).translationY(0f).setDuration(220).setInterpolator(DecelerateInterpolator()).start()
        AppLogger.i(TAG, "Panel shown")
    }

    private fun buildHeader(onClose: () -> Unit): View {
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        row.addView(TextView(this).apply {
            text = "✦  Astral"; textSize = 16f; setTextColor(C_GOLD); typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        val badge = TextView(this).apply {
            text = "LIVE"; textSize = 9f; setTextColor(C_GREEN); typeface = Typeface.DEFAULT_BOLD
            setPadding(dp(5), dp(2), dp(5), dp(2)); background = roundRect(dp(4), C_GREEN.withAlpha(30), C_GREEN.withAlpha(80), 1)
        }
        row.addView(badge)
        row.addView(View(this).apply { layoutParams = LinearLayout.LayoutParams(dp(8), 1) })
        val close = TextView(this).apply {
            text = "✕"; textSize = 15f; setTextColor(C_MUTED); gravity = Gravity.CENTER
            setPadding(dp(4), dp(2), dp(4), dp(2))
            setOnClickListener { onClose() }
        }
        row.addView(close)
        row.setPadding(0, 0, 0, dp(10))
        return row
    }

    private fun buildTabRow(labels: List<String>, views: List<View>): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            background = roundRect(dp(10), Color.parseColor("#0D0D0D"), C_BORDER, 1)
            setPadding(dp(3), dp(3), dp(3), dp(3))
        }
        var active = 0
        val btns = mutableListOf<TextView>()
        fun activate(idx: Int) {
            active = idx
            btns.forEachIndexed { i, b ->
                if (i == active) {
                    b.setTextColor(C_WHITE); b.background = roundRect(dp(8), Color.parseColor("#1A1A2E"), C_PURPLE.withAlpha(100), 1)
                } else {
                    b.setTextColor(C_MUTED); b.background = null
                }
            }
            views.forEachIndexed { i, v -> v.visibility = if (i == active) View.VISIBLE else View.GONE }
        }
        labels.forEachIndexed { i, label ->
            val btn = TextView(this).apply {
                text = label; textSize = 11f; gravity = Gravity.CENTER; typeface = Typeface.DEFAULT_BOLD
                setPadding(dp(4), dp(7), dp(4), dp(7))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                setOnClickListener { activate(i) }
            }
            btns.add(btn); row.addView(btn)
        }
        activate(0)
        views.forEachIndexed { i, v -> if (i != 0) v.visibility = View.GONE }
        row.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(8); bottomMargin = dp(8) }
        return row
    }

    private fun buildExecutorView(): LinearLayout {
        val v = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(4), 0, dp(6))
        }
        header.addView(dot(Color.parseColor("#FF5F56")))
        header.addView(View(this).apply { layoutParams = LinearLayout.LayoutParams(dp(5), 1) })
        header.addView(dot(Color.parseColor("#FFBD2E")))
        header.addView(View(this).apply { layoutParams = LinearLayout.LayoutParams(dp(5), 1) })
        header.addView(dot(Color.parseColor("#27C93F")))
        header.addView(View(this).apply { layoutParams = LinearLayout.LayoutParams(dp(8), 1) })
        header.addView(TextView(this).apply {
            text = "script.lua"; textSize = 10f; setTextColor(C_MUTED); typeface = Typeface.MONOSPACE
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        header.addView(TextView(this).apply { text = "Lua 5.4"; textSize = 9f; setTextColor(C_PURPLE) })
        v.addView(header)

        val editor = EditText(this).apply {
            hint = "-- Lua script\nAU.LocalPlayer.Speed = 2.0"
            setHintTextColor(Color.parseColor("#333333"))
            setTextColor(Color.parseColor("#E6EDF3"))
            textSize = 11.5f; typeface = Typeface.MONOSPACE; minLines = 4; maxLines = 7
            background = roundRect(dp(8), Color.parseColor("#0D1117"), Color.parseColor("#30304A"), 1)
            setPadding(dp(8), dp(8), dp(8), dp(8))
        }
        v.addView(editor)
        v.addView(spacer(dp(8)))

        val btnRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }

        val runBtn = chipBtn("▶  Run", C_PURPLE)
        runBtn.setOnClickListener {
            val script = editor.text.toString().trim()
            if (script.isEmpty()) return@setOnClickListener
            AppLogger.i("Executor", "Running: ${script.take(80)}")
            LuaRunner.execute(script)
            editor.setText("")
        }

        val clearBtn = chipBtn("✕  Clear", Color.parseColor("#2A2A2A"))
        clearBtn.setOnClickListener { editor.setText("") }

        btnRow.addView(runBtn, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = dp(6) })
        btnRow.addView(clearBtn, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        v.addView(btnRow)
        v.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        return v
    }

    private fun buildLogsView(): LinearLayout {
        val v = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val scroll = ScrollView(this).apply {
            background = roundRect(dp(8), Color.parseColor("#0D1117"), C_BORDER, 1)
        }
        val logsText = TextView(this).apply {
            typeface = Typeface.MONOSPACE; textSize = 10f; setTextColor(Color.parseColor("#7EE787"))
            setPadding(dp(8), dp(8), dp(8), dp(8))
            val entries = AppLogger.logs.value.takeLast(25)
            text = if (entries.isEmpty()) "No logs yet." else entries.joinToString("\n") { "[${it.time}] [${it.level.name.take(1)}] ${it.msg}" }
        }
        scroll.addView(logsText)
        scroll.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(130))
        v.addView(scroll)
        v.addView(spacer(dp(6)))
        val clearBtn = chipBtn("🗑  Clear Logs", Color.parseColor("#2A2A2A"))
        clearBtn.setOnClickListener { AppLogger.clearLogs(); logsText.text = "Logs cleared." }
        v.addView(clearBtn)
        v.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        return v
    }

    private fun buildInfoView(mod: String, ver: String): LinearLayout {
        val v = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(0, dp(4), 0, 0) }
        v.addView(buildInfoRow("Launcher", "Astral v1.0.0", C_GOLD))
        v.addView(buildInfoRow("Active Mod", mod, C_PURPLE))
        v.addView(buildInfoRow("Among Us", ver, C_WHITE))
        v.addView(buildInfoRow("Overlay", "Active", C_GREEN))
        v.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        return v
    }

    private fun buildInfoRow(label: String, value: String, valueColor: Int): View {
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, dp(3), 0, dp(3)) }
        row.addView(TextView(this).apply {
            text = label; textSize = 11f; setTextColor(C_MUTED)
            layoutParams = LinearLayout.LayoutParams(dp(68), LinearLayout.LayoutParams.WRAP_CONTENT)
        })
        row.addView(TextView(this).apply { text = value; textSize = 11f; setTextColor(valueColor); typeface = Typeface.DEFAULT_BOLD })
        return row
    }

    private fun buildDivider(): View = View(this).apply {
        setBackgroundColor(C_BORDER)
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1).apply { topMargin = dp(8); bottomMargin = dp(8) }
    }

    private fun spacer(h: Int): View = View(this).apply { layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, h) }

    private fun dot(color: Int): View = View(this).apply {
        background = ShapeDrawable(OvalShape()).apply { paint.color = color }
        layoutParams = LinearLayout.LayoutParams(dp(9), dp(9))
    }

    private fun chipBtn(text: String, bg: Int): TextView = TextView(this).apply {
        this.text = text; textSize = 12f; gravity = Gravity.CENTER; setTextColor(C_WHITE)
        typeface = Typeface.DEFAULT_BOLD; setPadding(dp(8), dp(9), dp(8), dp(9))
        background = roundRect(dp(8), bg, Color.TRANSPARENT, 0)
    }

    private fun roundRect(radius: Int, fill: Int, stroke: Int, strokeW: Int): GradientDrawable =
        GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE; cornerRadius = radius.toFloat()
            setColor(fill); if (strokeW > 0) setStroke(strokeW, stroke)
        }

    private fun Int.withAlpha(a: Int): Int = Color.argb(a, Color.red(this), Color.green(this), Color.blue(this))

    private fun removeBubble() { bubbleView?.let { try { wm.removeView(it) } catch (_: Exception) {} }; bubbleView = null }
    private fun removePanel() { panelView?.let { try { wm.removeView(it) } catch (_: Exception) {} }; panelView = null; panelVisible = false }

    private fun buildNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(CHANNEL_ID, "Astral Overlay", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(ch)
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("✦ Astral Launcher Active")
            .setContentText("Tap the gold bubble in-game to open the executor")
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy(); instance = null; removeBubble(); removePanel()
        AppLogger.i(TAG, "Overlay destroyed")
    }
}
