package com.astrallauncher.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.astrallauncher.MainViewModel
import com.astrallauncher.ui.CardBg
import com.astrallauncher.ui.Gold
import com.astrallauncher.ui.TextSecondary

@Composable
fun HomeScreen(vm: MainViewModel) {
    val ctx = LocalContext.current

    val auInstalled    by vm.auInstalled.collectAsState()
    val auVersion      by vm.auVersion.collectAsState()
    val isPatchedAu    by vm.isPatchedAu.collectAsState()
    val isPatching     by vm.isPatchingNow.collectAsState()
    val patchProgress  by vm.patchProgress.collectAsState()
    val patchStep      by vm.patchStep.collectAsState()
    val overlayRunning by vm.overlayRunning.collectAsState()
    val patchError     by vm.patchError.collectAsState()
    val hasOverlayPerm by vm.hasOverlayPerm.collectAsState()
    val installedMods  by vm.installedMods.collectAsState()

    val animProgress by animateFloatAsState(patchProgress, label = "progress")

    LaunchedEffect(Unit) { vm.refreshStatus() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("⚡ Astral Launcher", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Gold)

        if (!auInstalled) {
            BannerCard(
                icon   = "⚠️",
                title  = "Among Us não encontrado",
                body   = "Instale o Among Us na Play Store primeiro.",
                color  = Color(0xFFFF6B6B),
                action = "Abrir Play Store"
            ) {}
        }

        if (auInstalled && !isPatchedAu && !isPatching) {
            BannerCard(
                icon   = "ℹ️",
                title  = "Patch necessário para mods",
                body   = "O Among Us precisa ser patcheado para carregar mods BepInEx.\nO APK original é modificado e reinstalado com o mesmo package ID.",
                color  = Color(0xFF4A90D9),
                action = null
            ) {}
        }

        patchError?.let { err ->
            BannerCard(
                icon   = "❌",
                title  = "Erro no patch",
                body   = err,
                color  = Color(0xFFFF6B6B),
                action = "Fechar"
            ) { vm.refreshStatus() }
        }

        StatusCard(
            auInstalled = auInstalled,
            auVersion   = auVersion,
            isPatchedAu = isPatchedAu,
            modCount    = installedMods.size
        )

        if (auInstalled && !isPatching) {
            PatchCard(
                isPatchedAu = isPatchedAu,
                modCount    = installedMods.size,
                onPatch     = { vm.patchAndInstall() }
            )
        }

        if (isPatching) {
            PatchProgressCard(
                step     = patchStep ?: "Preparando...",
                progress = animProgress
            )
        }

        if (isPatchedAu || auInstalled) {
            LaunchCard(
                isPatchedAu    = isPatchedAu,
                overlayRunning = overlayRunning,
                hasOverlayPerm = hasOverlayPerm,
                onLaunch       = { vm.launchGame() },
                onToggleOverlay = { vm.toggleOverlay() },
                onGrantOverlay = {
                    ctx.startActivity(
                        Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            android.net.Uri.parse("package:${ctx.packageName}")
                        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
            )
        }
    }
}

@Composable
private fun BannerCard(
    icon: String, title: String, body: String,
    color: Color, action: String?, onAction: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
        shape    = RoundedCornerShape(12.dp),
        colors   = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f))
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(icon, fontSize = 16.sp)
                Text(title, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
            }
            Text(body, color = TextSecondary, fontSize = 12.sp, lineHeight = 17.sp)
            action?.let {
                TextButton(onClick = onAction, contentPadding = PaddingValues(0.dp)) {
                    Text(it, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun StatusCard(
    auInstalled: Boolean, auVersion: String?,
    isPatchedAu: Boolean, modCount: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(12.dp),
        colors   = CardDefaults.cardColors(containerColor = CardBg)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Status", fontWeight = FontWeight.Bold, color = Gold, fontSize = 14.sp)
            StatusRow("Among Us", if (auInstalled) "Instalado v$auVersion" else "Não instalado", auInstalled)
            StatusRow("Patch BepInEx", if (isPatchedAu) "Aplicado ✓" else "Não aplicado", isPatchedAu)
            StatusRow("Mods", "$modCount DLL(s) prontos", modCount > 0)
        }
    }
}

@Composable
private fun StatusRow(label: String, value: String, ok: Boolean) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = TextSecondary, fontSize = 12.sp)
        Text(
            value,
            color      = if (ok) Color(0xFF00FF88) else Color(0xFFFF6B6B),
            fontSize   = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun PatchCard(isPatchedAu: Boolean, modCount: Int, onPatch: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(12.dp),
        colors   = CardDefaults.cardColors(containerColor = CardBg)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Patch", fontWeight = FontWeight.Bold, color = Gold, fontSize = 14.sp)
            Text(
                if (isPatchedAu) "Reaplicar patch com os mods habilitados atuais ($modCount DLLs)."
                else "Injeta o overlay e os mods BepInEx no APK do Among Us. O jogo é reinstalado com o mesmo package ID.",
                color    = TextSecondary,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )
            Button(
                onClick  = onPatch,
                modifier = Modifier.fillMaxWidth(),
                colors   = ButtonDefaults.buttonColors(containerColor = Gold),
                shape    = RoundedCornerShape(8.dp)
            ) {
                Text(
                    if (isPatchedAu) "⚙ Reaplicar Patch" else "⚙ Aplicar Patch",
                    color      = Color.Black,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun PatchProgressCard(step: String, progress: Float) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(12.dp),
        colors   = CardDefaults.cardColors(containerColor = CardBg)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Gold)
                Text("Patcheando...", fontWeight = FontWeight.Bold, color = Gold, fontSize = 14.sp)
            }
            Text(step, color = TextSecondary, fontSize = 12.sp)
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)),
                color    = Gold,
                trackColor = Gold.copy(alpha = 0.15f)
            )
            Text("${(progress * 100).toInt()}%", color = TextSecondary, fontSize = 11.sp)
        }
    }
}

@Composable
private fun LaunchCard(
    isPatchedAu: Boolean, overlayRunning: Boolean, hasOverlayPerm: Boolean,
    onLaunch: () -> Unit, onToggleOverlay: () -> Unit, onGrantOverlay: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(12.dp),
        colors   = CardDefaults.cardColors(containerColor = CardBg)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Jogar", fontWeight = FontWeight.Bold, color = Gold, fontSize = 14.sp)

            if (!isPatchedAu) {
                Text("⚠️ Sem patch aplicado — iniciará o AU sem mods.", color = Color(0xFFFFAA44), fontSize = 11.sp)
            }

            Button(
                onClick  = onLaunch,
                modifier = Modifier.fillMaxWidth(),
                colors   = ButtonDefaults.buttonColors(
                    containerColor = if (isPatchedAu) Gold else Color(0xFF444466)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    "▶ Launch Among Us",
                    color      = if (isPatchedAu) Color.Black else Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
            Text("Overlay", fontWeight = FontWeight.Bold, color = Gold, fontSize = 13.sp)

            if (!hasOverlayPerm) {
                OutlinedButton(
                    onClick  = onGrantOverlay,
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(8.dp),
                    colors   = ButtonDefaults.outlinedButtonColors(contentColor = Gold)
                ) {
                    Text("Conceder permissão de overlay")
                }
            } else {
                OutlinedButton(
                    onClick  = onToggleOverlay,
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(8.dp),
                    colors   = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (overlayRunning) Color(0xFFFF6B6B) else Gold
                    )
                ) {
                    Text(if (overlayRunning) "⏹ Parar Overlay" else "▶ Iniciar Overlay")
                }
                Text(
                    if (overlayRunning) "● Overlay ativo — abre ao iniciar o jogo"
                    else "Overlay mostra o gerenciador de mods sobre o jogo",
                    color    = if (overlayRunning) Color(0xFF00FF88) else TextSecondary,
                    fontSize = 11.sp
                )
            }
        }
    }
}
