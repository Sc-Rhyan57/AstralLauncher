package com.astrallauncher

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.*
import com.astrallauncher.ui.AL
import com.astrallauncher.ui.components.*
import com.astrallauncher.ui.screens.*
import com.astrallauncher.util.GameHelper
import java.io.PrintWriter
import java.io.StringWriter

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupCrashHandler()

        val crash = getSharedPreferences("crash", Context.MODE_PRIVATE).getString("trace", null)
        if (crash != null) {
            getSharedPreferences("crash", Context.MODE_PRIVATE).edit().remove("trace").apply()
            setContent { AppTheme { CrashScreen(crash) } }
            return
        }
        setContent {
            AppTheme {
                if (!GameHelper.isAuInstalled(this)) {
                    AuMissingDialog(onInstall = { GameHelper.openPlayStore(this) }, onExit = { finish() })
                } else {
                    AstralApp(vm)
                }
            }
        }
    }

    private fun setupCrashHandler() {
        val def = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            try {
                val sw = StringWriter(); e.printStackTrace(PrintWriter(sw))
                val log = "Astral Crash\nDevice: ${Build.MANUFACTURER} ${Build.MODEL}\nAndroid: ${Build.VERSION.RELEASE}\nVersion: 1.0.0\n\n$sw"
                getSharedPreferences("crash", Context.MODE_PRIVATE).edit().putString("trace", log).commit()
                startActivity(Intent(this, MainActivity::class.java).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK) })
            } catch (_: Exception) { def?.uncaughtException(t, e) }
            android.os.Process.killProcess(android.os.Process.myPid())
        }
    }
}

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = AL.Gold, background = AL.Bg, surface = AL.Surface,
            onPrimary = Color.Black, onBackground = AL.White, onSurface = AL.White
        ), content = content
    )
}

@Composable
fun AuMissingDialog(onInstall: () -> Unit, onExit: () -> Unit) {
    Box(Modifier.fillMaxSize().background(AL.Bg), contentAlignment = Alignment.Center) {
        AlertDialog(onDismissRequest = {}, containerColor = AL.Surface, shape = RoundedCornerShape(20.dp),
            title = { Text("Among Us não encontrado", color = AL.White, fontWeight = FontWeight.ExtraBold) },
            text = { Text("Instale o Among Us para usar o Astral Launcher.", color = AL.Muted, fontSize = 14.sp) },
            confirmButton = { Button(onClick = onInstall, colors = ButtonDefaults.buttonColors(containerColor = AL.Gold), shape = RoundedCornerShape(10.dp)) { Text("Instalar", color = Color.Black, fontWeight = FontWeight.Bold) } },
            dismissButton = { TextButton(onClick = onExit) { Text("Sair", color = AL.Muted) } })
    }
}

@Composable
fun AstralApp(vm: MainViewModel) {
    var tab by remember { mutableIntStateOf(0) }
    var footerClicks by remember { mutableIntStateOf(0) }
    val installed by vm.installedMods.collectAsState()

    LaunchedEffect(footerClicks) { if (footerClicks >= 5) { tab = 4; footerClicks = 0 } }

    Scaffold(containerColor = AL.Bg,
        topBar = {
            if (tab !in listOf(1, 2)) AstralTopBar(navTabs[tab].label, actions = {
                if (tab == 0) IconButton(onClick = { vm.checkGame() }) { Icon(Icons.Outlined.Refresh, null, tint = AL.Muted) }
            })
        },
        bottomBar = { AstralBottomBar(tab, { tab = it }, installed.size) }
    ) { p ->
        Box(Modifier.fillMaxSize().padding(p)) {
            when (tab) {
                0 -> HomeScreen(vm); 1 -> ExploreScreen(vm); 2 -> ServersScreen(vm)
                3 -> ScriptScreen(vm); 4 -> SettingsScreen(vm, footerClicks) { footerClicks++ }
            }
        }
    }
}

@Composable
fun CrashScreen(trace: String) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    Column(Modifier.fillMaxSize().background(AL.Bg)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Warning, null, tint = AL.Error, modifier = Modifier.size(22.dp)); Spacer(Modifier.width(10.dp))
                Text("Crash Report", color = AL.White, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
            }
            TextButton(onClick = { android.os.Process.killProcess(android.os.Process.myPid()) }) { Text("Fechar", color = AL.Error, fontWeight = FontWeight.Bold) }
        }
        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp).padding(bottom = 16.dp)) {
            Text("Um erro inesperado ocorreu.", color = AL.Muted, modifier = Modifier.padding(bottom = 12.dp))
            Button(onClick = { val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager; cm.setPrimaryClip(ClipData.newPlainText("Crash", trace)) },
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AL.Gold), shape = RoundedCornerShape(12.dp)) {
                Icon(Icons.Outlined.ContentCopy, null, modifier = Modifier.size(16.dp), tint = Color.Black); Spacer(Modifier.width(8.dp)); Text("Copiar Log", fontWeight = FontWeight.Bold, color = Color.Black)
            }
            androidx.compose.foundation.shape.RoundedCornerShape(12.dp).let {
                Card(Modifier.fillMaxSize(), colors = CardDefaults.cardColors(containerColor = AL.Error.copy(0.08f)), shape = it) {
                    LazyColumn(Modifier.fillMaxSize()) {
                        item { Text(trace, Modifier.padding(14.dp), fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = AL.Error.copy(0.9f), lineHeight = 17.sp) }
                    }
                }
            }
        }
    }
}
