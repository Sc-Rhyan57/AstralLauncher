package com.astrallauncher.service

import android.app.*
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.os.*
import android.view.*
import android.widget.*
import androidx.core.app.NotificationCompat
import com.astrallauncher.bridge.AstralBridgeClient
import com.astrallauncher.util.AppLogger
import kotlin.math.abs

class OverlayService : Service() {

    companion object {
        const val CHANNEL_ID = "astral_overlay"
        const val EXTRA_MOD = "mod_name"
        const val EXTRA_AU_VER = "au_version"
        const val EXTRA_MODS_COUNT = "mods_count"
        var instance: OverlayService? = null

        private val C_BG = Color.parseColor("#F2080808")
        private val C_PANEL = Color.parseColor("#EE101010")
        private val C_CARD = Color.parseColor("#EE181818")
        private val C_GOLD = Color.parseColor("#D4A843")
        private val C_PURPLE = Color.parseColor("#9C6FFF")
        private val C_GREEN = Color.parseColor("#4CAF50")
        private val C_RED = Color.parseColor("#E53935")
        private val C_MUTED = Color.parseColor("#666666")
        private val C_WHITE = Color.parseColor("#EEEEEE")
        private val C_BORDER = Color.parseColor("#252525")
    }

    private val TAG = "OverlayService"
    private lateinit var wm: WindowManager
    private val bridge = AstralBridgeClient()
    private var bubbleView: View? = null
    private var panelView: View? = null
    private var panelVisible = false
    private var bubbleParams: WindowManager.LayoutParams? = null

    private val hackStates = mutableMapOf<String, Boolean>()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        startForeground(1, buildNotification())
        setupBridge()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val mod = intent?.getStringExtra(EXTRA_MOD) ?: "Vanilla"
        val ver = intent?.getStringExtra(EXTRA_AU_VER) ?: "?"
        val cnt = intent?.getIntExtra(EXTRA_MODS_COUNT, 0) ?: 0
        removeBubble(); removePanel()
        showBubble(mod, ver, cnt)
        bridge.connect()
        return START_STICKY
    }

    private fun setupBridge() {
        bridge.onConnected = {
            AppLogger.i(TAG, "Bridge conectado")
            bubbleView?.post { updateBubbleBadge(true) }
        }
        bridge.onDisconnected = {
            AppLogger.i(TAG, "Bridge desconectado")
            bubbleView?.post { updateBubbleBadge(false) }
        }
    }

    private val overlayType get() =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE

    private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()
    private fun sp(n: Float) = n * resources.displayMetrics.scaledDensity

    private fun showBubble(mod: String, ver: String, cnt: Int) {
        val params = WindowManager.LayoutParams(dp(60), dp(60), overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT).apply {
            gravity = Gravity.TOP or Gravity.END; x = dp(12); y = dp(120)
        }
        bubbleParams = params

        val frame = FrameLayout(this)

        val bg = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.parseColor("#CC030303"))
            setStroke(dp(2), C_GOLD)
        }

        val label = TextView(this).apply {
            text = "✦"; textSize = 22f; gravity = Gravity.CENTER
            setTextColor(C_GOLD); typeface = Typeface.DEFAULT_BOLD; background = bg
        }
        frame.addView(label, FrameLayout.LayoutParams(dp(56), dp(56)).apply { gravity = Gravity.CENTER })

        val badge = TextView(this).apply {
            visibility = View.GONE
            text = "●"; textSize = 9f; setTextColor(C_GREEN)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL; setColor(Color.parseColor("#0D1B0D"))
                setStroke(1, C_GREEN)
            }
            setPadding(dp(3), dp(3), dp(3), dp(3))
        }
        frame.addView(badge, FrameLayout.LayoutParams(dp(14), dp(14)).apply {
            gravity = Gravity.BOTTOM or Gravity.END; marginEnd = dp(3); bottomMargin = dp(3)
        })
        frame.tag = badge

        var sx = 0f; var sy = 0f; var ox = 0; var oy = 0; var moved = false
        frame.setOnTouchListener { v, ev ->
            when (ev.action) {
                MotionEvent.ACTION_DOWN -> { sx = ev.rawX; sy = ev.rawY; ox = params.x; oy = params.y; moved = false; true }
                MotionEvent.ACTION_MOVE -> {
                    if (abs(ev.rawX - sx) > 8 || abs(ev.rawY - sy) > 8) moved = true
                    params.x = (ox - (ev.rawX - sx)).toInt().coerceAtLeast(0)
                    params.y = (oy + (ev.rawY - sy)).toInt().coerceAtLeast(0)
                    runCatching { wm.updateViewLayout(v, params) }; true
                }
                MotionEvent.ACTION_UP -> { if (!moved) togglePanel(mod, ver, cnt); true }
                else -> false
            }
        }

        frame.scaleX = 0f; frame.scaleY = 0f
        wm.addView(frame, params)
        bubbleView = frame
        frame.animate().scaleX(1f).scaleY(1f).setDuration(250).start()
    }

    private fun updateBubbleBadge(connected: Boolean) {
        (bubbleView?.tag as? TextView)?.visibility = if (connected) View.VISIBLE else View.GONE
    }

    private fun togglePanel(mod: String, ver: String, cnt: Int) {
        if (panelVisible) removePanel() else showPanel(mod, ver, cnt)
    }

    private fun showPanel(mod: String, ver: String, cnt: Int) {
        val bpY = bubbleParams?.y ?: dp(120)
        val params = WindowManager.LayoutParams(
            dp(320), WindowManager.LayoutParams.WRAP_CONTENT, overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END; x = dp(12); y = bpY + dp(70)
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = roundRect(dp(18), C_PANEL, C_BORDER, 1)
            setPadding(dp(14), dp(12), dp(14), dp(14))
            elevation = dp(8).toFloat()
        }

        root.addView(buildPanelHeader())
        root.addView(divider())

        val hacksView = buildHacksPanel()
        val scriptView = buildScriptPanel()
        val infoView = buildInfoPanel(mod, ver, cnt)

        hacksView.visibility = View.VISIBLE
        scriptView.visibility = View.GONE
        infoView.visibility = View.GONE

        val tabRow = buildTabRow(
            listOf("⚡ Hacks", "▶ Script", "ℹ Info"),
            listOf(hacksView, scriptView, infoView)
        )
        root.addView(tabRow)
        root.addView(hacksView)
        root.addView(scriptView)
        root.addView(infoView)

        root.alpha = 0f; root.translationY = -dp(16).toFloat()
        wm.addView(root, params)
        panelView = root; panelVisible = true
        root.animate().alpha(1f).translationY(0f).setDuration(200).start()
    }

    private fun buildPanelHeader(): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dp(10))
        }
        row.addView(TextView(this).apply {
            text = "✦  Astral"; textSize = 16f; setTextColor(C_GOLD); typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        val connBadge = TextView(this).apply {
            text = if (bridge.isConnected) "● LIVE" else "○ OFF"
            textSize = 9f
            setTextColor(if (bridge.isConnected) C_GREEN else C_MUTED)
            setPadding(dp(5), dp(2), dp(5), dp(2))
            background = roundRect(dp(4),
                if (bridge.isConnected) Color.parseColor("#0A1F0A") else Color.parseColor("#111111"),
                if (bridge.isConnected) C_GREEN else C_MUTED, 1)
        }
        row.addView(connBadge)
        row.addView(space(dp(8), 1))
        row.addView(TextView(this).apply {
            text = "✕"; textSize = 16f; setTextColor(C_MUTED); gravity = Gravity.CENTER
            setPadding(dp(4), dp(0), dp(4), dp(0))
            setOnClickListener { removePanel() }
        })
        return row
    }

    private fun buildTabRow(labels: List<String>, views: List<View>): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            background = roundRect(dp(10), Color.parseColor("#0C0C0C"), C_BORDER, 1)
            setPadding(dp(3), dp(3), dp(3), dp(3))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(10) }
        }
        val btns = mutableListOf<TextView>()
        fun activate(idx: Int) {
            btns.forEachIndexed { i, b ->
                if (i == idx) {
                    b.setTextColor(C_WHITE)
                    b.background = roundRect(dp(8), Color.parseColor("#1A1030"), C_PURPLE.withAlpha(90), 1)
                } else {
                    b.setTextColor(C_MUTED); b.background = null
                }
            }
            views.forEachIndexed { i, v -> v.visibility = if (i == idx) View.VISIBLE else View.GONE }
        }
        labels.forEachIndexed { i, label ->
            val btn = TextView(this).apply {
                text = label; textSize = 11f; gravity = Gravity.CENTER; typeface = Typeface.DEFAULT_BOLD
                setPadding(dp(4), dp(8), dp(4), dp(8))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                setOnClickListener { activate(i) }
            }
            btns.add(btn); row.addView(btn)
        }
        activate(0)
        return row
    }

    private fun buildHacksPanel(): LinearLayout {
        val v = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }

        data class HackDef(val key: String, val label: String, val icon: String,
                           val onEnable: () -> Unit, val onDisable: () -> Unit)

        val hacks = listOf(
            HackDef("speed", "Speed x2.5", "⚡", { bridge.speedOn(2.5) }, { bridge.speedOff() }),
            HackDef("noclip", "NoClip", "👻", { bridge.noClipOn() }, { bridge.noClipOff() }),
            HackDef("godmode", "God Mode", "🛡", { bridge.godModeOn() }, { bridge.godModeOff() }),
            HackDef("killcd", "Kill CD Zero", "🔪", { bridge.killCooldownOn() }, { bridge.killCooldownOff() }),
            HackDef("reveal", "ESP Impostores", "👁", { bridge.revealOn() }, { bridge.revealOff() }),
            HackDef("vision", "Visão x10", "🔭", { bridge.visionOn(10.0) }, { bridge.visionOff() })
        )

        val grid = GridLayout(this).apply {
            columnCount = 2; rowCount = (hacks.size + 1) / 2
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }

        hacks.forEach { hack ->
            val isActive = hackStates[hack.key] == true
            val card = buildHackToggle(hack.key, hack.icon, hack.label, isActive, hack.onEnable, hack.onDisable)
            val lp = GridLayout.LayoutParams().apply {
                width = 0; height = GridLayout.LayoutParams.WRAP_CONTENT
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                setMargins(dp(3), dp(3), dp(3), dp(3))
            }
            grid.addView(card, lp)
        }
        v.addView(grid)
        v.addView(space(1, dp(8)))

        val tasksBtn = actionBtn("✔ Completar Tasks", C_GREEN) { bridge.completeTasks() }
        v.addView(tasksBtn)
        v.addView(space(1, dp(6)))

        val tpRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val tpX = editField("X", "0.0"); val tpY = editField("Y", "0.0")
        val tpBtn = actionBtn("Teleport", C_PURPLE) {
            val x = tpX.text.toString().toDoubleOrNull() ?: 0.0
            val y = tpY.text.toString().toDoubleOrNull() ?: 0.0
            bridge.teleport(x, y)
        }
        tpRow.addView(tpX, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = dp(4) })
        tpRow.addView(tpY, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = dp(4) })
        tpRow.addView(tpBtn, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        v.addView(tpRow)
        return v
    }

    private fun buildHackToggle(
        key: String, icon: String, label: String, initActive: Boolean,
        onEnable: () -> Unit, onDisable: () -> Unit
    ): View {
        var active = initActive
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            setPadding(dp(8), dp(10), dp(8), dp(10))
            background = roundRect(dp(12),
                if (active) Color.parseColor("#0D1A2A") else C_CARD,
                if (active) C_PURPLE else C_BORDER, 1)
        }
        val iconTv = TextView(this).apply { text = icon; textSize = 20f; gravity = Gravity.CENTER }
        val labelTv = TextView(this).apply {
            text = label; textSize = 10f; setTextColor(if (active) C_WHITE else C_MUTED)
            gravity = Gravity.CENTER; setPadding(0, dp(4), 0, 0)
        }
        val statusDot = View(this).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(if (active) C_GREEN else C_MUTED)
            }
            layoutParams = LinearLayout.LayoutParams(dp(6), dp(6)).apply { topMargin = dp(4) }
        }
        card.addView(iconTv); card.addView(labelTv); card.addView(statusDot)
        card.setOnClickListener {
            active = !active
            hackStates[key] = active
            if (active) onEnable() else onDisable()
            card.background = roundRect(dp(12),
                if (active) Color.parseColor("#0D1A2A") else C_CARD,
                if (active) C_PURPLE else C_BORDER, 1)
            labelTv.setTextColor(if (active) C_WHITE else C_MUTED)
            (statusDot.background as? GradientDrawable)?.setColor(if (active) C_GREEN else C_MUTED)
        }
        return card
    }

    private fun buildScriptPanel(): LinearLayout {
        val v = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }

        val termHeader = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dp(6))
        }
        listOf(Color.parseColor("#FF5F56"), Color.parseColor("#FFBD2E"), Color.parseColor("#27C93F"))
            .forEach { c ->
                termHeader.addView(View(this).apply {
                    background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(c) }
                    layoutParams = LinearLayout.LayoutParams(dp(9), dp(9)).apply { marginEnd = dp(5) }
                })
            }
        termHeader.addView(TextView(this).apply {
            text = "script.lua"; textSize = 10f; setTextColor(C_MUTED); typeface = Typeface.MONOSPACE
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        termHeader.addView(TextView(this).apply { text = "Lua"; textSize = 9f; setTextColor(C_PURPLE) })
        v.addView(termHeader)

        val editor = EditText(this).apply {
            hint = "-- speed(2.5)\n-- noclip_on()\n-- reveal()"
            setHintTextColor(Color.parseColor("#2A2A2A"))
            setTextColor(Color.parseColor("#E6EDF3"))
            textSize = 11.5f; typeface = Typeface.MONOSPACE; minLines = 5; maxLines = 8
            background = roundRect(dp(8), Color.parseColor("#0D1117"), Color.parseColor("#1F1F3A"), 1)
            setPadding(dp(10), dp(8), dp(10), dp(8))
        }
        v.addView(editor, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        v.addView(space(1, dp(8)))

        val outputTv = TextView(this).apply {
            visibility = View.GONE; typeface = Typeface.MONOSPACE; textSize = 10f
            setTextColor(Color.parseColor("#7EE787"))
            background = roundRect(dp(8), Color.parseColor("#0A110A"), Color.parseColor("#1A2A1A"), 1)
            setPadding(dp(8), dp(6), dp(8), dp(6))
            maxLines = 5
        }
        v.addView(outputTv, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        v.addView(space(1, dp(6)))

        val btnRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val runBtn = chipBtn("▶  Run", C_PURPLE)
        val clearBtn = chipBtn("✕  Clear", Color.parseColor("#222222"))
        runBtn.setOnClickListener {
            val script = editor.text.toString().trim()
            if (script.isEmpty()) return@setOnClickListener
            val result = bridge.executeScript(script)
            outputTv.text = result
            outputTv.visibility = View.VISIBLE
        }
        clearBtn.setOnClickListener { editor.setText(""); outputTv.visibility = View.GONE }
        btnRow.addView(runBtn, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = dp(6) })
        btnRow.addView(clearBtn, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        v.addView(btnRow)
        return v
    }

    private fun buildInfoPanel(mod: String, ver: String, cnt: Int): LinearLayout {
        val v = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        listOf(
            "Bridge" to (if (bridge.isConnected) "● Conectado" else "○ Desconectado"),
            "Mod ativo" to mod,
            "Among Us" to ver,
            "Mods" to "$cnt carregados",
            "Launcher" to "Astral v1.0.0"
        ).forEach { (k, value) ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, dp(4), 0, dp(4)) }
            row.addView(TextView(this).apply {
                text = k; textSize = 11f; setTextColor(C_MUTED)
                layoutParams = LinearLayout.LayoutParams(dp(80), LinearLayout.LayoutParams.WRAP_CONTENT)
            })
            row.addView(TextView(this).apply {
                text = value; textSize = 11f
                setTextColor(when {
                    value.startsWith("●") -> C_GREEN
                    value.startsWith("○") -> C_MUTED
                    k == "Mod ativo" -> C_GOLD
                    else -> C_WHITE
                })
                typeface = Typeface.DEFAULT_BOLD
            })
            v.addView(row)
        }
        return v
    }

    private fun actionBtn(text: String, color: Int, onClick: () -> Unit): TextView =
        TextView(this).apply {
            this.text = text; textSize = 12f; gravity = Gravity.CENTER; setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD; setPadding(dp(8), dp(10), dp(8), dp(10))
            background = roundRect(dp(10), color.withAlpha(30), color.withAlpha(120), 1)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            setOnClickListener { onClick() }
        }

    private fun chipBtn(text: String, bg: Int): TextView =
        TextView(this).apply {
            this.text = text; textSize = 12f; gravity = Gravity.CENTER; setTextColor(C_WHITE)
            typeface = Typeface.DEFAULT_BOLD; setPadding(dp(8), dp(9), dp(8), dp(9))
            background = roundRect(dp(8), bg, Color.TRANSPARENT, 0)
        }

    private fun editField(hint: String, default: String): EditText =
        EditText(this).apply {
            this.hint = hint; setText(default)
            setTextColor(C_WHITE); setHintTextColor(C_MUTED); textSize = 12f
            background = roundRect(dp(8), C_CARD, C_BORDER, 1)
            setPadding(dp(8), dp(8), dp(8), dp(8))
        }

    private fun divider(): View = View(this).apply {
        setBackgroundColor(C_BORDER)
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1).apply { topMargin = dp(2); bottomMargin = dp(10) }
    }

    private fun space(w: Int, h: Int): View = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(w, h)
    }

    private fun roundRect(radius: Int, fill: Int, stroke: Int, strokeW: Int): GradientDrawable =
        GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE; cornerRadius = radius.toFloat()
            setColor(fill); if (strokeW > 0) setStroke(strokeW, stroke)
        }

    private fun Int.withAlpha(a: Int): Int = Color.argb(a, Color.red(this), Color.green(this), Color.blue(this))

    private fun removeBubble() { bubbleView?.let { runCatching { wm.removeView(it) } }; bubbleView = null }
    private fun removePanel() { panelView?.let { runCatching { wm.removeView(it) } }; panelView = null; panelVisible = false }

    private fun buildNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(CHANNEL_ID, "Astral Overlay", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(ch)
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("✦ Astral Launcher")
            .setContentText("Toque na bolha dourada no jogo")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true).build()
    }

    override fun onDestroy() {
        super.onDestroy(); instance = null
        bridge.destroy(); removeBubble(); removePanel()
    }
}
