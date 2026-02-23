package com.astrallauncher.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.*
import com.astrallauncher.MainViewModel
import com.astrallauncher.network.ModRepositoryApi
import com.astrallauncher.ui.AL
import com.astrallauncher.ui.components.*
import com.astrallauncher.util.AppLogger
import androidx.compose.animation.core.*

@Composable
fun SettingsScreen(vm: MainViewModel, footerClicks: Int, onFooterClick: () -> Unit) {
    val ctx = LocalContext.current
    val overlay by vm.overlayActive.collectAsState()
    var repoUrl by remember { mutableStateOf(ModRepositoryApi.REPO_URL) }
    var showRepo by remember { mutableStateOf(false) }
    var showLogs by remember { mutableStateOf(false) }

    if (showLogs) { LogsScreen(onBack = { showLogs = false }); return }

    LazyColumn(modifier = Modifier.fillMaxSize().background(AL.Bg), contentPadding = PaddingValues(bottom = 120.dp)) {
        item { Text("Settings", color = AL.White, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) }

        item {
            Section("Game") {
                SAction(Icons.Outlined.Security, "Overlay Permission",
                    if (vm.hasOverlay()) "Granted — overlay can appear in-game" else "Required to show executor bubble",
                    iconTint = if (vm.hasOverlay()) AL.Success else AL.Warning, onClick = { vm.requestOverlay(ctx) },
                    trailing = { StatusChip(if (vm.hasOverlay()) "Granted" else "Missing", if (vm.hasOverlay()) AL.Success else AL.Warning) })
                if (overlay) SAction(Icons.Outlined.Stop, "Stop Overlay", "Kill the running overlay service", iconTint = AL.Error, onClick = { vm.stopOverlay() })
            }
        }

        item {
            Section("Repository") {
                SAction(Icons.Outlined.CloudDownload, "Mod Repository", repoUrl.take(44) + if (repoUrl.length > 44) "…" else "", onClick = { showRepo = !showRepo })
                if (showRepo) {
                    Column(Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                        OutlinedTextField(value = repoUrl, onValueChange = { repoUrl = it }, modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AL.Gold, unfocusedBorderColor = AL.Border, focusedTextColor = AL.White, unfocusedTextColor = AL.White, cursorColor = AL.Gold),
                            shape = RoundedCornerShape(10.dp), placeholder = { Text("https://raw.githubusercontent.com/...", color = AL.Muted) })
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            GoldButton("Apply", onClick = { vm.fetchMods(repoUrl); showRepo = false }, modifier = Modifier.weight(1f))
                            GhostButton("Reset", onClick = { repoUrl = ModRepositoryApi.REPO_URL; showRepo = false })
                        }
                    }
                }
                SAction(Icons.Outlined.Refresh, "Refresh Mod List", "Re-fetch mods from repository", onClick = { vm.fetchMods(repoUrl) })
            }
        }

        item {
            Section("Developer") {
                SAction(Icons.Outlined.ReportProblem, "Logs", "View all app logs, errors and events", iconTint = AL.Purple, onClick = { showLogs = true })
            }
        }

        item {
            Section("About") {
                SAction(Icons.Outlined.Code, "Source Code", "github.com/Sc-Rhyan57/AstralLauncher", iconTint = AL.Purple, onClick = {
                    ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Sc-Rhyan57/AstralLauncher")).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
                })
                SAction(Icons.Outlined.Forum, "Discord", "Join the Astral Launcher community", iconTint = AL.Info, onClick = {
                    ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://discord.gg/")).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
                })
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(AL.GoldBg), contentAlignment = Alignment.Center) { Text("✦", color = AL.Gold, fontSize = 18.sp) }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Astral Launcher", color = AL.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text("v1.0.0 • By Rhyan57 • Open Source", color = AL.Muted, fontSize = 12.sp)
                    }
                }
            }
        }

        item { Spacer(Modifier.height(20.dp)); Footer(footerClicks, onFooterClick) }
    }
}

@Composable
fun LogsScreen(onBack: () -> Unit) {
    val logs by AppLogger.logs.collectAsState()
    val ctx = LocalContext.current
    val listState = rememberLazyListState()

    LaunchedEffect(logs.size) { if (logs.isNotEmpty()) listState.animateScrollToItem(logs.size - 1) }

    Column(Modifier.fillMaxSize().background(AL.Bg)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, null, tint = AL.White) }
            Text("Logs", color = AL.White, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, modifier = Modifier.weight(1f))
            IconButton(onClick = {
                val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("Logs", AppLogger.exportLogs()))
            }) { Icon(Icons.Outlined.ContentCopy, null, tint = AL.Muted) }
            IconButton(onClick = { AppLogger.clearLogs() }) { Icon(Icons.Outlined.Delete, null, tint = AL.Error.copy(0.7f)) }
        }

        Row(Modifier.padding(horizontal = 14.dp, vertical = 2.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            val counts = logs.groupBy { it.level }
            listOf(AppLogger.Level.ERROR to AL.Error, AppLogger.Level.WARN to AL.Warning, AppLogger.Level.INFO to AL.Info, AppLogger.Level.DEBUG to AL.Muted)
                .forEach { (lvl, c) -> counts[lvl]?.size?.takeIf { it > 0 }?.let { StatusChip("${lvl.name} $it", c) } }
        }
        Spacer(Modifier.height(4.dp))

        if (logs.isEmpty()) {
            EmptyState(Icons.Outlined.Computer, "No logs yet", "App events, errors and debug info appear here")
        } else {
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp), contentPadding = PaddingValues(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                items(logs) { e ->
                    val c = when (e.level) { AppLogger.Level.ERROR -> AL.Error; AppLogger.Level.WARN -> AL.Warning; AppLogger.Level.INFO -> AL.Info; else -> AL.Muted }
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(5.dp)).background(c.copy(0.06f)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                        Text(e.time, color = AL.Muted.copy(0.5f), fontSize = 9.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.width(68.dp))
                        Text("[${e.level.name.take(1)}]", color = c, fontSize = 9.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.width(18.dp))
                        Text("[${e.tag.take(12)}] ", color = AL.GoldDark, fontSize = 9.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.width(78.dp), maxLines = 1)
                        Text(e.msg, color = AL.White.copy(0.85f), fontSize = 9.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.padding(vertical = 5.dp)) {
        Text(title.uppercase(), color = AL.Gold, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, modifier = Modifier.padding(horizontal = 24.dp, vertical = 5.dp))
        Column(Modifier.padding(horizontal = 16.dp).clip(RoundedCornerShape(16.dp)).background(AL.BgCard).border(BorderStroke(0.5.dp, AL.Border), RoundedCornerShape(16.dp)), content = content)
    }
}

@Composable
fun SAction(icon: ImageVector, title: String, subtitle: String, iconTint: Color = AL.Gold, onClick: () -> Unit, trailing: @Composable (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().clickable { onClick() }.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(AL.Surface), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = iconTint, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = AL.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = AL.Muted, fontSize = 12.sp)
        }
        trailing?.invoke() ?: Icon(Icons.Outlined.ChevronRight, null, tint = AL.Muted, modifier = Modifier.size(18.dp))
    }
}

@Composable
fun Footer(clicks: Int, onClick: () -> Unit) {
    val t = rememberInfiniteTransition(label = "rgb")
    val hue by t.animateFloat(0f, 360f, infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Restart), label = "h")
    val c = Color.hsv(hue, 0.75f, 1f)
    Column(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.clickable { onClick() }, verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Star, null, tint = c, modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(6.dp)); Text("By Rhyan57", fontSize = 12.sp, color = c, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(6.dp)); Icon(Icons.Outlined.Star, null, tint = c, modifier = Modifier.size(12.dp))
        }
        if (clicks in 1..4) Text("${5 - clicks}x to unlock dev mode", fontSize = 10.sp, color = AL.Muted.copy(0.4f))
    }
}
