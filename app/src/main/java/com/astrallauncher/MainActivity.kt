package com.astrallauncher

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.*
import com.astrallauncher.ui.AL
import com.astrallauncher.ui.components.*
import com.astrallauncher.ui.screens.*
import com.astrallauncher.util.AmongUsHelper
import java.io.PrintWriter
import java.io.StringWriter

class MainActivity : ComponentActivity() {

    private val vm: MainViewModel by viewModels()

    companion object {
        const val PREF_CRASH = "crash_prefs"
        const val KEY_CRASH_TRACE = "crash_trace"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupCrashHandler()

        val crashTrace = getSharedPreferences(PREF_CRASH, Context.MODE_PRIVATE)
            .getString(KEY_CRASH_TRACE, null)

        if (crashTrace != null) {
            getSharedPreferences(PREF_CRASH, Context.MODE_PRIVATE)
                .edit().remove(KEY_CRASH_TRACE).apply()
            setContent { AppTheme { CrashScreen(trace = crashTrace) } }
            return
        }

        setContent {
            AppTheme {
                val auInstalled = AmongUsHelper.isInstalled(this)
                if (!auInstalled) {
                    AmongUsNotFoundDialog(
                        onInstall = { AmongUsHelper.openPlayStore(this) },
                        onExit = { finish() }
                    )
                } else {
                    AstralApp(vm = vm)
                }
            }
        }
    }

    private fun setupCrashHandler() {
        val def = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            try {
                val sw = StringWriter()
                e.printStackTrace(PrintWriter(sw))
                val log = "Astral Launcher - Crash Report\n" +
                        "Device: ${Build.MANUFACTURER} ${Build.MODEL}\n" +
                        "Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\n" +
                        "App Version: 1.0.0\n\n$sw"
                getSharedPreferences(PREF_CRASH, Context.MODE_PRIVATE)
                    .edit().putString(KEY_CRASH_TRACE, log).commit()
                startActivity(Intent(this, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                })
            } catch (_: Exception) { def?.uncaughtException(t, e) }
            android.os.Process.killProcess(android.os.Process.myPid())
        }
    }
}

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = AL.Gold,
            background = AL.Bg,
            surface = AL.Surface,
            onPrimary = Color.Black,
            onBackground = AL.White,
            onSurface = AL.White
        ),
        content = content
    )
}

@Composable
fun AmongUsNotFoundDialog(onInstall: () -> Unit, onExit: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().background(AL.Bg),
        contentAlignment = Alignment.Center
    ) {
        AlertDialog(
            onDismissRequest = {},
            containerColor = Color(0xFF1A1A1A),
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    "Among Us Not Found!",
                    color = AL.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    "Please install Among Us from the Play Store before using Astral Launcher.",
                    color = AL.Muted,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = onInstall,
                    colors = ButtonDefaults.buttonColors(containerColor = AL.Gold),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Install", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = onExit) {
                    Text("Exit", color = AL.Muted)
                }
            }
        )
    }
}

@Composable
fun AstralApp(vm: MainViewModel) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var footerClicks by remember { mutableIntStateOf(0) }
    val installedMods by vm.installedMods.collectAsState()

    LaunchedEffect(footerClicks) {
        if (footerClicks >= 5) { selectedTab = 4; footerClicks = 0 }
    }

    Scaffold(
        containerColor = AL.Bg,
        topBar = {
            if (selectedTab != 1 && selectedTab != 2) {
                AstralTopBar(
                    title = navTabs[selectedTab].label,
                    actions = {
                        if (selectedTab == 0) {
                            IconButton(onClick = { vm.checkAmongUs() }) {
                                Icon(Icons.Outlined.Refresh, null, tint = AL.Muted)
                            }
                        }
                    }
                )
            }
        },
        bottomBar = {
            Column {
                AstralBottomBar(
                    selected = selectedTab,
                    onSelect = { selectedTab = it },
                    installedCount = installedMods.size
                )
                if (selectedTab == 4) {
                    Spacer(Modifier.height(4.dp))
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (selectedTab) {
                0 -> HomeScreen(vm)
                1 -> ExploreScreen(vm)
                2 -> ServersScreen(vm)
                3 -> ScriptScreen(vm)
                4 -> SettingsScreen(vm, footerClicks, onFooterClick = { footerClicks++ })
            }
        }
    }
}

@Composable
fun CrashScreen(trace: String) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    Column(modifier = Modifier.fillMaxSize().background(AL.Bg)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Warning, null, tint = AL.Error, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
                Text("App Crashed", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold), color = AL.White)
            }
            TextButton(onClick = { android.os.Process.killProcess(android.os.Process.myPid()) }) {
                Text("Close", color = AL.Error, fontWeight = FontWeight.Bold)
            }
        }
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp).padding(bottom = 16.dp)) {
            Text("An unexpected error occurred.", style = MaterialTheme.typography.bodyMedium, color = AL.Muted, modifier = Modifier.padding(bottom = 12.dp))
            Button(
                onClick = {
                    val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    cm.setPrimaryClip(ClipData.newPlainText("Crash Log", trace))
                },
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AL.Gold),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Outlined.ContentCopy, null, modifier = Modifier.size(16.dp), tint = Color.Black)
                Spacer(Modifier.width(8.dp))
                Text("Copy Log", fontWeight = FontWeight.Bold, color = Color.Black)
            }
            Card(
                modifier = Modifier.fillMaxSize(),
                colors = CardDefaults.cardColors(containerColor = AL.Error.copy(0.1f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item {
                        Text(
                            trace,
                            modifier = Modifier.padding(14.dp),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = AL.Error.copy(0.9f),
                            lineHeight = 17.sp
                        )
                    }
                }
            }
        }
    }
}
