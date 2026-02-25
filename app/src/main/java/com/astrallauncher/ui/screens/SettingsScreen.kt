package com.astrallauncher.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.astrallauncher.MainViewModel
import com.astrallauncher.ui.CardBg
import com.astrallauncher.ui.Gold
import com.astrallauncher.ui.TextSecondary
import com.astrallauncher.util.AppLogger
import com.astrallauncher.util.LogLevel
import com.astrallauncher.util.Prefs
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(vm: MainViewModel) {
    val ctx       = LocalContext.current
    val scope     = rememberCoroutineScope()
    val logs      by AppLogger.logs.collectAsState()
    var repoUrl   by remember { mutableStateOf("") }
    var logFilter by remember { mutableStateOf(LogLevel.INFO) }

    LaunchedEffect(Unit) {
        Prefs.repoUrl(ctx).collect { repoUrl = it }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Configurações", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Gold)

        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = CardBg)) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Permissão de Overlay", fontWeight = FontWeight.Bold, color = Gold, fontSize = 14.sp)
                val hasPerm = Settings.canDrawOverlays(ctx)
                Text(
                    if (hasPerm) "✓ Permissão concedida" else "✕ Permissão negada",
                    color = if (hasPerm) Color(0xFF00FF88) else Color(0xFFFF6B6B),
                    fontSize = 12.sp
                )
                if (!hasPerm) {
                    Button(
                        onClick = {
                            ctx.startActivity(
                                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    android.net.Uri.parse("package:${ctx.packageName}"))
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Gold),
                        shape  = RoundedCornerShape(8.dp)
                    ) {
                        Text("Conceder", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = CardBg)) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Repositório de Mods", fontWeight = FontWeight.Bold, color = Gold, fontSize = 14.sp)
                OutlinedTextField(
                    value         = repoUrl,
                    onValueChange = { repoUrl = it },
                    modifier      = Modifier.fillMaxWidth(),
                    label         = { Text("URL do repositório", color = TextSecondary) },
                    singleLine    = true,
                    shape         = RoundedCornerShape(8.dp),
                    colors        = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = Gold,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                        focusedTextColor     = Color.White,
                        unfocusedTextColor   = Color.White,
                        cursorColor          = Gold
                    )
                )
                Button(
                    onClick = { scope.launch { Prefs.setRepoUrl(ctx, repoUrl) } },
                    colors  = ButtonDefaults.buttonColors(containerColor = Gold),
                    shape   = RoundedCornerShape(8.dp)
                ) {
                    Text("Salvar", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }

        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = CardBg)) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Logs", fontWeight = FontWeight.Bold, color = Gold, fontSize = 14.sp)
                    TextButton(onClick = { AppLogger.clear() }) {
                        Text("Limpar", color = Color(0xFFFF6B6B), fontSize = 12.sp)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    LogLevel.values().forEach { level ->
                        FilterChip(
                            selected  = logFilter == level,
                            onClick   = { logFilter = level },
                            label     = { Text(level.name, fontSize = 10.sp) },
                            colors    = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Gold,
                                selectedLabelColor     = Color.Black
                            ),
                            shape = RoundedCornerShape(6.dp)
                        )
                    }
                }

                val filtered = logs.filter { it.level.ordinal >= logFilter.ordinal }.takeLast(80)
                Surface(
                    modifier = Modifier.fillMaxWidth().height(220.dp),
                    color    = Color.Black.copy(alpha = 0.4f),
                    shape    = RoundedCornerShape(8.dp)
                ) {
                    val scroll = rememberScrollState(Int.MAX_VALUE)
                    Column(
                        Modifier.verticalScroll(scroll).padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        filtered.forEach { log ->
                            val color = when (log.level) {
                                LogLevel.ERROR -> Color(0xFFFF6B6B)
                                LogLevel.WARN  -> Color(0xFFFFAA44)
                                LogLevel.INFO  -> Color.White
                                LogLevel.DEBUG -> TextSecondary
                            }
                            Text(
                                "[${log.time}] [${log.level}] [${log.tag}] ${log.msg}",
                                color      = color,
                                fontSize   = 9.sp,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }
}
