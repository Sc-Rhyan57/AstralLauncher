package com.astrallauncher.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.os.*
import android.view.*
import android.view.animation.DecelerateInterpolator
import android.widget.*
import androidx.core.app.NotificationCompat
import com.astrallauncher.MainActivity
import com.astrallauncher.model.InstalledMod
import com.astrallauncher.util.AppLogger
import com.astrallauncher.util.Constants
import com.astrallauncher.util.Prefs
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.io.File

private const val TAG = "OverlayService"
private const val NOTIF_ID = 42
private const val CHANNEL_ID = "astral_overlay"

class OverlayService : Service() {

    private lateinit var wm: WindowManager
    private var bubbleView: View? = null
    private var panelView: View? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var panelVisible = false
    private var activeTab = 0

    companion object {
        fun start(ctx: Context) {
            val i = Intent(ctx, OverlayService::class.java)
            ctx.startForegroundService(i)
        }
        fun stop(ctx: Context) {
            ctx.stopService(Intent(ctx, OverlayService::class.java))
        }
    }

    override fun onBind(intent: Intent?) = null

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
        startForeground(NOTIF_ID, buildNotification())
        showBubble()
        AppLogger.i(TAG, "OverlayService iniciado")
    }

    override fun onDestroy() {
        scope.cancel()
        removeBubble()
        removePanel()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        val ch = NotificationChannel(CHANNEL_ID, "Astral Overlay", NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
    }

    private fun buildNotification(): Notification {
        val stopIntent = PendingIntent.getBroadcast(
            this, 0,
            Intent("com.astrallauncher.STOP_OVERLAY"),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Astral Launcher")
            .setContentText("Overlay ativo — toque para gerenciar mods")
            .setSmallIcon(android.R.drawable.star_on)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(android.R.drawable.ic_delete, "Parar", stopIntent)
            .build()
    }

    private fun overlayParams(w: Int, h: Int, x: Int, y: Int) = WindowManager.LayoutParams(
        w, h,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
        WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.TOP or Gravity.START
        this.x = x; this.y = y
    }

    private fun showBubble() {
        val size = dp(60)
        val params = overlayParams(size, size, 30, 200)

        val bubble = createBubbleView(size)
        bubbleView = bubble

        setupBubbleDrag(bubble, params)
        wm.addView(bubble, params)
    }

    private fun createBubbleView(size: Int): View {
        val ctx = this
        return object : View(ctx) {
            private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#CC1A1A2E")
                style = Paint.Style.FILL
            }
            private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#FFD700")
                style = Paint.Style.STROKE
                strokeWidth = dp(2).toFloat()
            }
            private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = dp(10).toFloat()
                textAlign = Paint.Align.CENTER
                typeface = Typeface.DEFAULT_BOLD
            }
            private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#FFD700")
                style = Paint.Style.FILL
            }

            override fun onDraw(c: Canvas) {
                val cx = width / 2f; val cy = height / 2f; val r = width / 2f - dp(3)
                c.drawCircle(cx, cy, r, bgPaint)
                c.drawCircle(cx, cy, r, ringPaint)
                c.drawText("⚡", cx, cy + dp(4), textPaint)
                c.drawCircle(width - dp(6).toFloat(), dp(6).toFloat(), dp(4).toFloat(), dotPaint)
            }
        }.apply {
            layoutParams = ViewGroup.LayoutParams(size, size)
        }
    }

    private fun setupBubbleDrag(view: View, params: WindowManager.LayoutParams) {
        var initX = 0; var initY = 0
        var touchX = 0f; var touchY = 0f
        var moved = false

        view.setOnTouchListener { v, ev ->
            when (ev.action) {
                MotionEvent.ACTION_DOWN -> {
                    initX = params.x; initY = params.y
                    touchX = ev.rawX; touchY = ev.rawY
                    moved = false; true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (ev.rawX - touchX).toInt()
                    val dy = (ev.rawY - touchY).toInt()
                    if (Math.abs(dx) > 5 || Math.abs(dy) > 5) moved = true
                    params.x = initX + dx; params.y = initY + dy
                    wm.updateViewLayout(view, params); true
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved) togglePanel(params.x, params.y); true
                }
                else -> false
            }
        }
    }

    private fun togglePanel(bubbleX: Int, bubbleY: Int) {
        if (panelVisible) {
            removePanel()
        } else {
            showPanel(bubbleX, bubbleY)
        }
        panelVisible = !panelVisible
    }

    private fun showPanel(bubbleX: Int, bubbleY: Int) {
        val w = dp(320); val h = dp(480)
        val screenH = wm.currentWindowMetrics.bounds.height()
        val screenW = wm.currentWindowMetrics.bounds.width()

        var px = bubbleX - w - dp(8)
        if (px < 0) px = bubbleX + dp(68)
        var py = bubbleY
        if (py + h > screenH - dp(48)) py = screenH - h - dp(48)

        val params = WindowManager.LayoutParams(
            w, h,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = px; y = py
        }

        val panel = buildPanel()
        panelView = panel
        wm.addView(panel, params)

        panel.alpha = 0f
        panel.animate().alpha(1f).setDuration(180).setInterpolator(DecelerateInterpolator()).start()
    }

    private fun removePanel() {
        panelView?.let {
            try { wm.removeView(it) } catch (_: Exception) {}
        }
        panelView = null
    }

    private fun removeBubble() {
        bubbleView?.let {
            try { wm.removeView(it) } catch (_: Exception) {}
        }
        bubbleView = null
    }

    private fun buildPanel(): View {
        val ctx = this

        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#F01A1A2E"))
            val r = dp(16).toFloat()
            outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(v: View, outline: Outline) {
                    outline.setRoundRect(0, 0, v.width, v.height, r)
                }
            }
            clipToOutline = true
            elevation = dp(8).toFloat()
        }

        root.addView(buildPanelHeader(root))
        root.addView(buildTabBar())
        val content = FrameLayout(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
            )
        }
        root.addView(content)

        refreshTabContent(content, activeTab)
        return root
    }

    private fun buildPanelHeader(root: LinearLayout): View {
        val ctx = this
        val header = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#22FFD700"))
            setPadding(dp(12), dp(8), dp(12), dp(8))
        }

        val title = TextView(ctx).apply {
            text = "⚡ Astral Mod Manager"
            textSize = 14f
            setTextColor(Color.parseColor("#FFD700"))
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val closeBtn = TextView(ctx).apply {
            text = "✕"
            textSize = 16f
            setTextColor(Color.parseColor("#FF6666"))
            typeface = Typeface.DEFAULT_BOLD
            setPadding(dp(8), 0, 0, 0)
            setOnClickListener { removePanel(); panelVisible = false }
        }

        setupHeaderDrag(header, root)
        header.addView(title)
        header.addView(closeBtn)
        return header
    }

    private fun setupHeaderDrag(header: View, panel: View) {
        var initX = 0; var initY = 0
        var touchX = 0f; var touchY = 0f

        header.setOnTouchListener { _, ev ->
            when (ev.action) {
                MotionEvent.ACTION_DOWN -> {
                    val lp = panel.layoutParams as? WindowManager.LayoutParams
                    initX = lp?.x ?: 0; initY = lp?.y ?: 0
                    touchX = ev.rawX; touchY = ev.rawY; true
                }
                MotionEvent.ACTION_MOVE -> {
                    val lp = panel.layoutParams as? WindowManager.LayoutParams ?: return@setOnTouchListener true
                    lp.x = initX + (ev.rawX - touchX).toInt()
                    lp.y = initY + (ev.rawY - touchY).toInt()
                    try { wm.updateViewLayout(panel, lp) } catch (_: Exception) {}; true
                }
                else -> false
            }
        }
    }

    private val tabLabels = listOf("Mods", "Ativos", "Info")

    private fun buildTabBar(): View {
        val ctx = this
        val bar = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#11FFFFFF"))
        }

        tabLabels.forEachIndexed { i, label ->
            val tab = TextView(ctx).apply {
                text = label
                textSize = 12f
                gravity = Gravity.CENTER
                setPadding(0, dp(10), 0, dp(10))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                setTextColor(if (i == activeTab) Color.parseColor("#FFD700") else Color.parseColor("#88FFFFFF"))
                typeface = if (i == activeTab) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                setOnClickListener {
                    activeTab = i
                    val parent = (parent as? LinearLayout)?.parent as? LinearLayout ?: return@setOnClickListener
                    val content = parent.getChildAt(2) as? FrameLayout ?: return@setOnClickListener
                    refreshTabBar(parent.getChildAt(1) as LinearLayout)
                    refreshTabContent(content, i)
                }
            }
            bar.addView(tab)

            if (i < tabLabels.size - 1) {
                bar.addView(View(ctx).apply {
                    setBackgroundColor(Color.parseColor("#22FFFFFF"))
                    layoutParams = LinearLayout.LayoutParams(1, LinearLayout.LayoutParams.MATCH_PARENT)
                })
            }
        }
        return bar
    }

    private fun refreshTabBar(bar: LinearLayout) {
        for (i in 0 until bar.childCount) {
            val child = bar.getChildAt(i)
            if (child is TextView) {
                val idx = tabLabels.indexOf(child.text.toString())
                if (idx >= 0) {
                    child.setTextColor(if (idx == activeTab) Color.parseColor("#FFD700") else Color.parseColor("#88FFFFFF"))
                    child.typeface = if (idx == activeTab) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                }
            }
        }
    }

    private fun refreshTabContent(container: FrameLayout, tab: Int) {
        container.removeAllViews()
        val view = when (tab) {
            0 -> buildModsTab()
            1 -> buildActiveModsTab()
            else -> buildInfoTab()
        }
        container.addView(view)
    }

    private fun buildModsTab(): View {
        val ctx = this
        val scroll = ScrollView(ctx)
        val list = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(8), dp(8), dp(8), dp(8))
        }

        val modsDir = File(filesDir, Constants.MODS_DIR)
        val dllFiles = modsDir.listFiles { f -> f.extension == "dll" } ?: emptyArray()

        if (dllFiles.isEmpty()) {
            list.addView(buildEmptyState("Nenhum mod instalado.\nBaixe mods pela tela Explorar."))
        } else {
            dllFiles.forEach { dll ->
                list.addView(buildModRow(dll))
            }
        }

        scroll.addView(list)
        return scroll
    }

    private fun buildModRow(dll: File): View {
        val ctx = this
        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#1AFFFFFF"))
            setPadding(dp(10), dp(10), dp(10), dp(10))
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.bottomMargin = dp(6)
            layoutParams = lp
            val r = dp(8).toFloat()
            outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(v: View, outline: Outline) {
                    outline.setRoundRect(0, 0, v.width, v.height, r)
                }
            }
            clipToOutline = true
        }

        val info = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        info.addView(TextView(ctx).apply {
            text = dll.nameWithoutExtension
            textSize = 12f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
        })
        info.addView(TextView(ctx).apply {
            text = "${dll.length() / 1024} KB"
            textSize = 10f
            setTextColor(Color.parseColor("#88FFFFFF"))
        })

        val toggle = Switch(ctx).apply {
            isChecked = true
            thumbTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#FFD700"))
            trackTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#33FFD700"))
            setOnCheckedChangeListener { _, checked ->
                AppLogger.i(TAG, "${dll.name}: ${if (checked) "habilitado" else "desabilitado"}")
            }
        }

        val deleteBtn = TextView(ctx).apply {
            text = "🗑"
            textSize = 16f
            setPadding(dp(8), 0, 0, 0)
            setOnClickListener {
                dll.delete()
                AppLogger.i(TAG, "Mod deletado: ${dll.name}")
                val parent = parent as? LinearLayout ?: return@setOnClickListener
                val grandParent = parent.parent as? LinearLayout ?: return@setOnClickListener
                grandParent.removeView(parent)
            }
        }

        row.addView(info)
        row.addView(toggle)
        row.addView(deleteBtn)
        return row
    }

    private fun buildActiveModsTab(): View {
        val ctx = this
        val scroll = ScrollView(ctx)
        val list = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(8), dp(8), dp(8), dp(8))
        }

        val modsDir = File(filesDir, Constants.MODS_DIR)
        val enabled = modsDir.listFiles { f -> f.extension == "dll" }?.toList() ?: emptyList()

        if (enabled.isEmpty()) {
            list.addView(buildEmptyState("Nenhum mod ativo.\nHabilite mods na aba Mods."))
        } else {
            list.addView(buildSectionLabel("${enabled.size} mods ativos para próximo patch"))
            enabled.forEach { dll ->
                list.addView(buildActiveModChip(dll))
            }
        }

        scroll.addView(list)
        return scroll
    }

    private fun buildActiveModChip(dll: File): View {
        val ctx = this
        return LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#1A00FF88"))
            setPadding(dp(10), dp(8), dp(10), dp(8))
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.bottomMargin = dp(4)
            layoutParams = lp
            val r = dp(8).toFloat()
            outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(v: View, outline: Outline) {
                    outline.setRoundRect(0, 0, v.width, v.height, r)
                }
            }
            clipToOutline = true

            addView(TextView(ctx).apply {
                text = "● "
                setTextColor(Color.parseColor("#00FF88"))
                textSize = 12f
            })
            addView(TextView(ctx).apply {
                text = dll.nameWithoutExtension
                setTextColor(Color.WHITE)
                textSize = 12f
            })
        }
    }

    private fun buildInfoTab(): View {
        val ctx = this
        val scroll = ScrollView(ctx)
        val list = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(12), dp(12), dp(12))
        }

        val auVersion = try {
            packageManager.getPackageInfo(Constants.AU_PACKAGE, 0).versionName ?: "?"
        } catch (_: Exception) { "Não instalado" }

        val modsDir = File(filesDir, Constants.MODS_DIR)
        val modCount = modsDir.listFiles { f -> f.extension == "dll" }?.size ?: 0

        list.addView(buildInfoRow("Launcher", "Astral v1.0.0"))
        list.addView(buildInfoRow("Among Us", auVersion))
        list.addView(buildInfoRow("Mods instalados", "$modCount DLLs"))
        list.addView(buildInfoRow("Package", Constants.AU_PACKAGE))
        list.addView(buildInfoRow("Overlay", "Ativo ✓"))

        list.addView(View(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1))
            setBackgroundColor(Color.parseColor("#22FFFFFF"))
            (layoutParams as LinearLayout.LayoutParams).setMargins(0, dp(12), 0, dp(12))
        })

        val stopBtn = TextView(ctx).apply {
            text = "Parar Overlay"
            textSize = 13f
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#FF6666"))
            setBackgroundColor(Color.parseColor("#22FF0000"))
            setPadding(0, dp(12), 0, dp(12))
            val r = dp(8).toFloat()
            outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(v: View, outline: Outline) {
                    outline.setRoundRect(0, 0, v.width, v.height, r)
                }
            }
            clipToOutline = true
            setOnClickListener { stopSelf() }
        }
        list.addView(stopBtn)

        scroll.addView(list)
        return scroll
    }

    private fun buildInfoRow(label: String, value: String): View {
        val ctx = this
        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(6), 0, dp(6))
        }
        row.addView(TextView(ctx).apply {
            text = label
            textSize = 11f
            setTextColor(Color.parseColor("#88FFFFFF"))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        row.addView(TextView(ctx).apply {
            text = value
            textSize = 11f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
        })
        return row
    }

    private fun buildSectionLabel(text: String): View {
        val ctx = this
        return TextView(ctx).apply {
            this.text = text
            textSize = 11f
            setTextColor(Color.parseColor("#88FFFFFF"))
            setPadding(0, 0, 0, dp(8))
        }
    }

    private fun buildEmptyState(text: String): View {
        val ctx = this
        return TextView(ctx).apply {
            this.text = text
            textSize = 12f
            setTextColor(Color.parseColor("#66FFFFFF"))
            gravity = Gravity.CENTER
            setPadding(dp(16), dp(32), dp(16), dp(32))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
