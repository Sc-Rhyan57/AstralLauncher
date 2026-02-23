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
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.OvalShape
import android.os.Build
import android.os.IBinder
import android.view.*
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.*
import com.astrallauncher.bridge.AstralBridgeClient
import com.astrallauncher.util.AppLogger
import kotlin.math.abs

class OverlayService : Service() {

    // ─── Constantes ───────────────────────────────────────────
    companion object {
        const val CHANNEL_ID      = "astral_overlay"
        const val EXTRA_MOD       = "mod_name"
        const val EXTRA_AU_VER    = "au_version"
        const val EXTRA_MODS_COUNT = "mods_count"
        var instance: OverlayService? = null

        private val C_BG     = Color.parseColor("#F2080808")
        private val C_GOLD   = Color.parseColor("#D4A843")
        private val C_MUTED  = Color.parseColor("#666666")
        private val C_WHITE  = Color.parseColor("#EEEEEE")
        private val C_PURPLE = Color.parseColor("#9C6FFF")
        private val C_GREEN  = Color.parseColor("#4CAF50")
        private val C_RED    = Color.parseColor("#E53935")
        private val C_BORDER = Color.parseColor("#222222")
        private val C_CODE   = Color.parseColor("#0D1117")
    }

    // ─── Views e state ────────────────────────────────────────
    private lateinit var wm: WindowManager
    private var bubbleView:   View? = null
    private var panelView:    View? = null
    private var bubbleParams: WindowManager.LayoutParams? = null
    private var panelParams:  WindowManager.LayoutParams? = null
    private var panelVisible  = false
    private var outputText:   TextView? = null

    // ─── Bridge com o jogo ────────────────────────────────────
    private val bridge = AstralBridgeClient()

    // ─── Estado dos hacks (para toggle dos botões) ────────────
    private val hackStates = mutableMapOf<String, Boolean>()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        startForeground(1, buildNotification())

        bridge.onConnected = {
            AppLogger.i("OverlayService", "Bridge CONECTADO ao jogo!")
            appendOutput("🟢 Conectado ao AstralBridge!")
        }
        bridge.onDisconnected = {
            AppLogger.w("OverlayService", "Bridge desconectado.")
            appendOutput("🔴 Bridge desconectado.")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val mod = intent?.getStringExtra(EXTRA_MOD)       ?: "Vanilla"
        val ver = intent?.getStringExtra(EXTRA_AU_VER)    ?: "?"
        val cnt = intent?.getIntExtra(EXTRA_MODS_COUNT, 0) ?: 0
        removeBubble(); removePanel()
        showBubble(mod, ver, cnt)
        bridge.connect()                // inicia tentativas de conexão
        return START_STICKY
    }

    // ─── Helpers ──────────────────────────────────────────────
    private val overlayType get() =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE

    private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()

    private fun appendOutput(msg: String) {
        val tv = outputText ?: return
        val lines = tv.text.toString().lines().takeLast(20)
        tv.text = (lines + msg).joinToString("\n")
    }

    // ─── Bolha flutuante ──────────────────────────────────────
    private fun showBubble(mod: String, ver: String, cnt: Int) {
        val params = WindowManager.LayoutParams(
            dp(56), dp(56), overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.END; x = dp(12); y = dp(130) }
        bubbleParams = params

        val frame = FrameLayout(this)
        val glow  = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(Color.TRANSPARENT); setStroke(dp(6), C_GOLD.withAlpha(50)) }
        val outer = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(Color.parseColor("#CC050505")); setStroke(dp(2), C_GOLD) }
        val glowV = View(this).apply { background = glow }
        frame.addView(glowV, FrameLayout.LayoutParams(dp(56), dp(56)))
        val core = TextView(this).apply {
            text = "✦"; textSize = 22f; gravity = Gravity.CENTER
            setTextColor(C_GOLD); typeface = Typeface.DEFAULT_BOLD; background = outer
        }
        frame.addView(core, FrameLayout.LayoutParams(dp(52), dp(52)).apply { gravity = Gravity.CENTER })

        ValueAnimator.ofFloat(0.6f, 1f).apply {
            duration = 1800; repeatCount = ValueAnimator.INFINITE; repeatMode = ValueAnimator.REVERSE
            addUpdateListener { v -> val a = v.animatedValue as Float
                frame.scaleX = a * 0.04f + 0.96f; frame.scaleY = frame.scaleX; glowV.alpha = a * 0.4f }
        }.start()

        var sx = 0f; var sy = 0f; var ox = 0; var oy = 0; var moved = false
        frame.setOnTouchListener { v, ev ->
            when (ev.action) {
                MotionEvent.ACTION_DOWN  -> { sx = ev.rawX; sy = ev.rawY; ox = params.x; oy = params.y; moved = false; true }
                MotionEvent.ACTION_MOVE  -> {
                    if (abs(ev.rawX - sx) > 8 || abs(ev.rawY - sy) > 8) moved = true
                    params.x = (ox - (ev.rawX - sx)).toInt().coerceAtLeast(0)
                    params.y = (oy + (ev.rawY - sy)).toInt().coerceAtLeast(0)
                    wm.updateViewLayout(v, params); true
                }
                MotionEvent.ACTION_UP    -> { if (!moved) togglePanel(mod, ver, cnt); true }
                else -> false
            }
        }
        wm.addView(frame, params); bubbleView = frame
        frame.scaleX = 0f; frame.scaleY = 0f
        frame.animate().scaleX(1f).scaleY(1f).setDuration(300).setInterpolator(OvershootInterpolator()).start()
    }

    // ─── Painel principal ─────────────────────────────────────
    private fun togglePanel(mod: String, ver: String, cnt: Int) {
        if (panelVisible) { hideKb(); removePanel() } else showPanel(mod, ver, cnt)
    }

    private fun showPanel(mod: String, ver: String, cnt: Int) {
        val py = (bubbleParams?.y ?: dp(130)) + dp(68)
        val pp = WindowManager.LayoutParams(
            dp(320), WindowManager.LayoutParams.WRAP_CONTENT, overlayType,
            0, PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.END; x = dp(12); y = py
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE }
        panelParams = pp

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = roundRect(dp(20), C_BG, C_BORDER, 1)
            setPadding(dp(16), dp(14), dp(16), dp(16))
        }

        root.addView(buildHeader { hideKb(); removePanel() })
        root.addView(divider())
        root.addView(infoRow("Bridge", if (bridge.isConnected) "● Conectado" else "○ Aguardando jogo...",
            if (bridge.isConnected) C_GREEN else C_MUTED))
        root.addView(infoRow("Mod",  mod, C_GOLD))
        root.addView(infoRow("AU",   ver, C_WHITE))
        root.addView(infoRow("Mods", "$cnt carregados", if (cnt > 0) C_GREEN else C_MUTED))
        root.addView(divider())

        val tabScriptView  = buildScriptTab()
        val tabHacksView   = buildHacksTab()
        val tabLogsView    = buildLogsTab()
        root.addView(buildTabs(
            listOf("▶ Script", "⚡ Hacks", "📋 Logs"),
            listOf(tabScriptView, tabHacksView, tabLogsView)
        ))
        root.addView(tabScriptView)
        root.addView(tabHacksView)
        root.addView(tabLogsView)

        root.alpha = 0f; root.translationY = -dp(20).toFloat()
        wm.addView(root, pp); panelView = root; panelVisible = true
        root.animate().alpha(1f).translationY(0f).setDuration(200).setInterpolator(DecelerateInterpolator()).start()
    }

    // ─── Aba Script ───────────────────────────────────────────
    private fun buildScriptTab(): LinearLayout {
        val v = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        // Barra de título estilo terminal
        val bar = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, dp(4), 0, dp(6)) }
        listOf("#FF5F56", "#FFBD2E", "#27C93F").forEach { c ->
            bar.addView(View(this).apply {
                background = ShapeDrawable(OvalShape()).apply { paint.color = Color.parseColor(c) }
                layoutParams = LinearLayout.LayoutParams(dp(9), dp(9)).apply { marginEnd = dp(5) }
            })
        }
        bar.addView(TextView(this).apply {
            text = "astral.lua"; textSize = 10f; setTextColor(C_MUTED); typeface = Typeface.MONOSPACE
        })
        v.addView(bar)

        val hint = "speed(2.5)"
        val editor = EditText(this).apply {
            setText(hint); setTextColor(Color.parseColor("#E6EDF3")); setHintTextColor(C_MUTED)
            textSize = 11.5f; typeface = Typeface.MONOSPACE; minLines = 4; maxLines = 8
            background = roundRect(dp(8), C_CODE, Color.parseColor("#30304A"), 1)
            setPadding(dp(8), dp(8), dp(8), dp(8))
            isFocusable = true; isFocusableInTouchMode = true
            setOnClickListener {
                requestFocus()
                (context.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager)
                    .showSoftInput(this, InputMethodManager.SHOW_FORCED)
            }
        }
        v.addView(editor)
        v.addView(spacer(dp(6)))

        val outTv = TextView(this).apply {
            text = "— output —"; textSize = 10f; typeface = Typeface.MONOSPACE
            setTextColor(Color.parseColor("#7EE787"))
            background = roundRect(dp(6), C_CODE, C_BORDER, 1)
            setPadding(dp(8), dp(6), dp(8), dp(6)); minLines = 2; maxLines = 5
        }
        outputText = outTv
        v.addView(outTv)
        v.addView(spacer(dp(6)))

        val btnRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val runBtn = chipBtn("▶  Executar", C_PURPLE)
        runBtn.setOnClickListener {
            val code = editor.text.toString().trim()
            if (code.isEmpty()) return@setOnClickListener
            hideKb()
            if (!bridge.isConnected) {
                outTv.text = "✗ AstralBridge não conectado.\n\nInicie o Among Us com o plugin instalado.\nO Astral tenta reconectar automaticamente..."
                return@setOnClickListener
            }
            val result = bridge.executeScript(code)
            outTv.text = result
            AppLogger.i("Script", result)
        }
        val clearBtn = chipBtn("✕  Limpar", Color.parseColor("#2A2A2A"))
        clearBtn.setOnClickListener { editor.setText(""); outTv.text = "— output —" }

        btnRow.addView(runBtn, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = dp(6) })
        btnRow.addView(clearBtn, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        v.addView(btnRow)
        return v
    }

    // ─── Aba Hacks ────────────────────────────────────────────
    private fun buildHacksTab(): LinearLayout {
        val v = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(0, dp(4), 0, 0) }

        data class Hack(val label: String, val key: String, val onEn: () -> Boolean, val onDis: () -> Boolean)

        val hacks = listOf(
            Hack("⚡ Speed x2.5",          "speed",   { bridge.speedOn(2.5) },     { bridge.speedOff() }),
            Hack("👻 NoClip",              "noclip",  { bridge.noClipOn() },         { bridge.noClipOff() }),
            Hack("💀 Kill CD Zero",        "killcd",  { bridge.killCooldownOn() },   { bridge.killCooldownOff() }),
            Hack("👁 Revelar Impostores",  "reveal",  { bridge.revealOn() },         { bridge.revealOff() }),
            Hack("🛡 God Mode",            "godmode", { bridge.godModeOn() },         { bridge.godModeOff() }),
            Hack("🔭 Visão x10",           "vision",  { bridge.visionOn(10.0) },     { bridge.visionOff() }),
        )

        hacks.forEach { hack ->
            val btn = TextView(this).apply {
                text = hack.label; textSize = 12f; gravity = Gravity.CENTER
                setTextColor(C_WHITE); typeface = Typeface.DEFAULT_BOLD
                setPadding(dp(8), dp(10), dp(8), dp(10))
                background = roundRect(dp(10), Color.parseColor("#1A1A1A"), C_BORDER, 1)
            }
            btn.setOnClickListener {
                if (!bridge.isConnected) { appendOutput("✗ Jogo não conectado."); return@setOnClickListener }
                val isOn = hackStates.getOrDefault(hack.key, false)
                val newState = !isOn
                hackStates[hack.key] = newState
                if (newState) {
                    hack.onEn()
                    btn.background = roundRect(dp(10), C_PURPLE.withAlpha(40), C_PURPLE, 1)
                    appendOutput("✓ ${hack.label} ON")
                } else {
                    hack.onDis()
                    btn.background = roundRect(dp(10), Color.parseColor("#1A1A1A"), C_BORDER, 1)
                    appendOutput("✓ ${hack.label} OFF")
                }
            }
            v.addView(btn)
            v.addView(spacer(dp(4)))
        }

        val tasksBtn = chipBtn("✅  Completar Tasks", C_GOLD)
        tasksBtn.setOnClickListener {
            if (!bridge.isConnected) { appendOutput("✗ Jogo não conectado."); return@setOnClickListener }
            bridge.completeTasks(); appendOutput("✓ Tasks sendo completadas...")
        }
        v.addView(tasksBtn)
        return v
    }

    // ─── Aba Logs ─────────────────────────────────────────────
    private fun buildLogsTab(): LinearLayout {
        val v = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val scroll = ScrollView(this).apply {
            background = roundRect(dp(8), C_CODE, C_BORDER, 1)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(140))
        }
        val tv = TextView(this).apply {
            typeface = Typeface.MONOSPACE; textSize = 10f; setTextColor(Color.parseColor("#7EE787"))
            setPadding(dp(8), dp(8), dp(8), dp(8))
            val logs = AppLogger.logs.value.takeLast(30)
            text = if (logs.isEmpty()) "Sem logs." else logs.joinToString("\n") { "[${it.level.name[0]}] ${it.time} ${it.msg}" }
        }
        scroll.addView(tv); v.addView(scroll); v.addView(spacer(dp(6)))
        v.addView(chipBtn("🗑  Limpar", Color.parseColor("#2A2A2A")).apply {
            setOnClickListener { AppLogger.clearLogs(); tv.text = "Logs limpos." }
        })
        return v
    }

    // ─── Tabs ─────────────────────────────────────────────────
    private fun buildTabs(labels: List<String>, views: List<View>): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            background = roundRect(dp(10), Color.parseColor("#0D0D0D"), C_BORDER, 1)
            setPadding(dp(3), dp(3), dp(3), dp(3))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                .apply { topMargin = dp(8); bottomMargin = dp(8) }
        }
        val btns = mutableListOf<TextView>()
        fun activate(idx: Int) {
            btns.forEachIndexed { i, b ->
                if (i == idx) { b.setTextColor(C_WHITE); b.background = roundRect(dp(8), Color.parseColor("#1A1A2E"), C_PURPLE.withAlpha(100), 1) }
                else          { b.setTextColor(C_MUTED); b.background = null }
            }
            views.forEachIndexed { i, vv -> vv.visibility = if (i == idx) View.VISIBLE else View.GONE }
            // Torna focável apenas na aba Script
            setFocusable(idx == 0)
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
        return row
    }

    // ─── Builders de UI ───────────────────────────────────────
    private fun buildHeader(onClose: () -> Unit): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dp(10))
        }
        row.addView(TextView(this).apply {
            text = "✦  Astral"; textSize = 16f; setTextColor(C_GOLD); typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        val badge = TextView(this).apply {
            text = if (bridge.isConnected) "LIVE" else "OFFLINE"
            textSize = 9f; typeface = Typeface.DEFAULT_BOLD
            setTextColor(if (bridge.isConnected) C_GREEN else C_MUTED)
            setPadding(dp(5), dp(2), dp(5), dp(2))
            background = roundRect(dp(4),
                (if (bridge.isConnected) C_GREEN else C_MUTED).withAlpha(30),
                (if (bridge.isConnected) C_GREEN else C_MUTED).withAlpha(80), 1)
        }
        row.addView(badge)
        row.addView(spacer(dp(8)))
        row.addView(TextView(this).apply {
            text = "✕"; textSize = 16f; setTextColor(C_MUTED); gravity = Gravity.CENTER
            setPadding(dp(4), dp(2), dp(4), dp(2)); setOnClickListener { onClose() }
        })
        return row
    }

    private fun infoRow(label: String, value: String, vc: Int): View {
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, dp(2), 0, dp(2)) }
        row.addView(TextView(this).apply { text = label; textSize = 11f; setTextColor(C_MUTED); layoutParams = LinearLayout.LayoutParams(dp(68), LinearLayout.LayoutParams.WRAP_CONTENT) })
        row.addView(TextView(this).apply { text = value; textSize = 11f; setTextColor(vc); typeface = Typeface.DEFAULT_BOLD })
        return row
    }

    private fun divider() = View(this).apply {
        setBackgroundColor(C_BORDER)
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1).apply { topMargin = dp(8); bottomMargin = dp(8) }
    }

    private fun spacer(h: Int) = View(this).apply { layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, h) }

    private fun chipBtn(text: String, bg: Int) = TextView(this).apply {
        this.text = text; textSize = 12f; gravity = Gravity.CENTER; setTextColor(C_WHITE)
        typeface = Typeface.DEFAULT_BOLD; setPadding(dp(8), dp(9), dp(8), dp(9))
        background = roundRect(dp(8), bg, Color.TRANSPARENT, 0)
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
    }

    private fun roundRect(r: Int, fill: Int, stroke: Int, sw: Int) = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE; cornerRadius = r.toFloat(); setColor(fill)
        if (sw > 0) setStroke(sw, stroke)
    }

    private fun Int.withAlpha(a: Int) = Color.argb(a, Color.red(this), Color.green(this), Color.blue(this))

    // ─── Focus e teclado ──────────────────────────────────────
    private fun setFocusable(f: Boolean) {
        val pp = panelParams ?: return; val pv = panelView ?: return
        if (f) pp.flags = pp.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
        else   pp.flags = pp.flags or  WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        try { wm.updateViewLayout(pv, pp) } catch (_: Exception) {}
    }

    private fun hideKb() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        panelView?.windowToken?.let { imm.hideSoftInputFromWindow(it, 0) }
    }

    // ─── Remoção de views ─────────────────────────────────────
    private fun removeBubble() { bubbleView?.let { try { wm.removeView(it) } catch (_: Exception) {} }; bubbleView = null }
    private fun removePanel()  { panelView?.let  { try { wm.removeView(it) } catch (_: Exception) {} }; panelView = null; panelParams = null; panelVisible = false; outputText = null }

    // ─── Notificação foreground ───────────────────────────────
    private fun buildNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(CHANNEL_ID, "Astral Overlay", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(ch)
        }
        return androidx.core.app.NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("✦ Astral Launcher Ativo")
            .setContentText("Toque na bolha dourada para abrir o executor")
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_LOW)
            .setOngoing(true).build()
    }

    override fun onDestroy() {
        super.onDestroy(); instance = null
        bridge.destroy(); hideKb(); removeBubble(); removePanel()
    }
}
