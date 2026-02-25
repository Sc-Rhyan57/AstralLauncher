package com.astrallauncher.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.astrallauncher.MainViewModel
import com.astrallauncher.model.InstalledMod
import com.astrallauncher.model.ModFormat
import com.astrallauncher.ui.AL
import com.astrallauncher.ui.components.*

@Composable
fun HomeScreen(vm: MainViewModel) {
    val mods by vm.installedMods.collectAsState()
    val auVer by vm.auVersion.collectAsState()
    val patchedVer by vm.patchedVersion.collectAsState()
    val auInstalled by vm.auInstalled.collectAsState()
    val patchedInstalled by vm.patchedInstalled.collectAsState()
    val overlay by vm.overlayActive.collectAsState()
    val status by vm.statusMsg.collectAsState()
    val patch by vm.patchState.collectAsState()
    val ctx = LocalContext.current

    val apkPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { vm.installExternalApk(ctx, it) }
    }

    LaunchedEffect(Unit) { vm.checkGame() }

    LazyColumn(modifier = Modifier.fillMaxSize().background(AL.Bg), contentPadding = PaddingValues(bottom = 120.dp)) {
        item { HeroCard(auVer = auVer, patchedVer = patchedVer, patchedInstalled = patchedInstalled) }

        if (!patchedInstalled && auInstalled) {
            item { FirstPatchBanner(mods.count { it.enabled && it.format == ModFormat.DLL }) }
        }

        item {
            PatchCard(
                patch = patch, mods = mods, overlay = overlay,
                auInstalled = auInstalled, patchedInstalled = patchedInstalled,
                onPatch = { vm.patchAndInstall() },
                onLaunch = {
                    if (!vm.hasOverlayPermission()) vm.requestOverlayPermission(ctx)
                    vm.launchGame()
                },
                onStopOverlay = { vm.stopOverlay() },
                onReset = { vm.resetPatch() }
            )
        }

        item {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                SectionHeader(if (mods.isEmpty()) "Nenhum mod instalado" else "Mods Instalados", action = if (mods.isNotEmpty()) "${mods.size}" else null)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { apkPicker.launch("application/vnd.android.package-archive") }) {
                    Icon(Icons.Outlined.FileUpload, "Instalar APK", tint = AL.Muted, modifier = Modifier.size(20.dp))
                }
            }
        }

        if (mods.isEmpty()) {
            item { EmptyState(Icons.Outlined.Extension, "Nenhum mod ainda", "Vá em Explore para instalar mods") }
        } else {
            items(mods) { mod -> ModRow(mod, onToggle = { vm.toggleMod(mod.id) }, onDelete = { vm.deleteMod(mod.id) }) }
        }

        status?.let { s ->
            item {
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(12.dp)).background(AL.GoldBg)
                    .border(BorderStroke(0.5.dp, AL.GoldDark.copy(0.5f)), RoundedCornerShape(12.dp)).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Info, null, tint = AL.Gold, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(s, color = AL.GoldLight, fontSize = 13.sp, modifier = Modifier.weight(1f))
                    IconButton(onClick = { vm.clearStatus() }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Outlined.Close, null, tint = AL.Muted, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun FirstPatchBanner(dllCount: Int) {
    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
        .clip(RoundedCornerShape(16.dp))
        .background(Brush.horizontalGradient(listOf(Color(0xFF0D0820), Color(0xFF1A0830))))
        .border(BorderStroke(1.dp, AL.Purple.copy(0.5f)), RoundedCornerShape(16.dp))
        .padding(16.dp)) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.NewReleases, null, tint = AL.Purple, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Primeiro uso", color = AL.Purple, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "O Among Us precisa ser patcheado antes do primeiro uso. " +
                "Isso clona o jogo com um novo package ID (${com.astrallauncher.util.Constants.PATCHED_PACKAGE}) " +
                "e injeta o BepInEx para suporte a mods.\n\n" +
                "• Instale mods em Explore\n" +
                "• Clique em ⚙ Patch\n" +
                "• Siga o prompt de instalação\n" +
                "• Clique em ▶ Launch para abrir o jogo patcheado",
                color = AL.MutedL, fontSize = 12.sp, lineHeight = 18.sp
            )
            if (dllCount > 0) {
                Spacer(Modifier.height(6.dp))
                StatusChip("$dllCount mod(s) prontos para injeção", AL.Gold)
            }
        }
    }
}

@Composable
fun HeroCard(auVer: String, patchedVer: String, patchedInstalled: Boolean) {
    val t = rememberInfiniteTransition(label = "h")
    val glow by t.animateFloat(0.2f, 0.8f, infiniteRepeatable(tween(2200, easing = EaseInOutSine), RepeatMode.Reverse), label = "g")
    Box(modifier = Modifier.fillMaxWidth().padding(16.dp).clip(RoundedCornerShape(22.dp))
        .background(Brush.verticalGradient(listOf(AL.GoldBg, Color(0xFF0A0A0A))))
        .border(BorderStroke(1.dp, AL.Gold.copy(glow)), RoundedCornerShape(22.dp)).padding(20.dp)) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(54.dp).clip(RoundedCornerShape(16.dp)).background(AL.GoldSub), contentAlignment = Alignment.Center) {
                    Text("✦", fontSize = 28.sp, color = AL.Gold)
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("Astral Launcher", color = AL.White, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                    Text("Among Us Mod Platform", color = AL.Gold, fontSize = 12.sp)
                }
                StatusChip("v1.0.0", AL.Gold)
            }
            Spacer(Modifier.height(16.dp)); GoldDivider(); Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                InfoPill("AU Original", auVer)
                InfoPill("Patcheado", if (patchedInstalled) "✓ $patchedVer" else "✗ Não instalado", if (patchedInstalled) AL.Success else AL.Error)
                InfoPill("Plataforma", "Android")
            }
        }
    }
}

@Composable
fun InfoPill(label: String, value: String, valueColor: Color = AL.White) {
    Column(modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(AL.Surface).padding(horizontal = 12.dp, vertical = 8.dp)) {
        Text(label, color = AL.Muted, fontSize = 10.sp)
        Text(value, color = valueColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun PatchCard(
    patch: MainViewModel.PatchState,
    mods: List<InstalledMod>,
    overlay: Boolean,
    auInstalled: Boolean,
    patchedInstalled: Boolean,
    onPatch: () -> Unit,
    onLaunch: () -> Unit,
    onStopOverlay: () -> Unit,
    onReset: () -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
        .clip(RoundedCornerShape(18.dp)).background(AL.BgCard)
        .border(BorderStroke(0.5.dp, AL.Border), RoundedCornerShape(18.dp)).padding(16.dp)) {
        when (patch) {
            is MainViewModel.PatchState.Progress -> Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = AL.Gold, strokeWidth = 2.dp)
                    Spacer(Modifier.width(10.dp))
                    Text(patch.step, color = AL.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(progress = { patch.pct / 100f }, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)), color = AL.Gold, trackColor = AL.Surface2)
                Spacer(Modifier.height(4.dp))
                Text("${patch.pct}%", color = AL.Gold, fontSize = 11.sp)
            }
            is MainViewModel.PatchState.Err -> Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.ErrorOutline, null, tint = AL.Error, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp)); Text("Patch falhou", color = AL.Error, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Spacer(Modifier.height(6.dp)); Text(patch.msg, color = AL.Muted, fontSize = 12.sp)
                Spacer(Modifier.height(12.dp)); GhostButton("Fechar", onClick = onReset, modifier = Modifier.fillMaxWidth(), color = AL.Error)
            }
            is MainViewModel.PatchState.Done -> Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.CheckCircleOutline, null, tint = AL.Success, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp)); Text("Patch concluído!", color = AL.Success, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Spacer(Modifier.height(6.dp)); Text("Siga o prompt para instalar o jogo patcheado.", color = AL.Muted, fontSize = 12.sp)
                Spacer(Modifier.height(12.dp)); GhostButton("OK", onClick = onReset, modifier = Modifier.fillMaxWidth(), color = AL.Success)
            }
            else -> Column {
                val dllCount = mods.count { it.enabled && it.format == ModFormat.DLL }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GoldButton("▶ Launch", onClick = onLaunch, modifier = Modifier.weight(1f),
                        icon = Icons.Outlined.RocketLaunch, enabled = auInstalled)
                    PurpleButton("⚙ Patch", onClick = onPatch, modifier = Modifier.wrapContentWidth(),
                        icon = Icons.Outlined.Build, enabled = auInstalled)
                }
                if (!patchedInstalled) {
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Warning, null, tint = AL.Warning, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Jogo ainda não patcheado — Launch abre o AU original", color = AL.Warning, fontSize = 11.sp)
                    }
                }
                if (dllCount > 0) { Spacer(Modifier.height(6.dp)); Text("$dllCount mod(s) DLL prontos para injeção", color = AL.Muted, fontSize = 11.sp) }
                if (overlay) { Spacer(Modifier.height(8.dp)); GhostButton("Parar Overlay", onClick = onStopOverlay, modifier = Modifier.fillMaxWidth(), color = AL.Error) }
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Info, null, tint = AL.Muted, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Patch clona AU com novo package ID + BepInEx (estilo Starlight)", color = AL.Muted, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
fun ModRow(mod: InstalledMod, onToggle: () -> Unit, onDelete: () -> Unit) {
    var confirm by remember { mutableStateOf(false) }
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 3.dp)
        .clip(RoundedCornerShape(14.dp)).background(AL.BgCard)
        .border(BorderStroke(0.5.dp, if (mod.enabled) AL.GoldDark.copy(0.4f) else AL.Border), RoundedCornerShape(14.dp))
        .padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(42.dp).clip(RoundedCornerShape(10.dp)).background(if (mod.enabled) AL.GoldBg else AL.Surface), contentAlignment = Alignment.Center) {
            Icon(if (mod.format == ModFormat.LUA) Icons.Outlined.Code else Icons.Outlined.Extension, null,
                tint = if (mod.enabled) AL.Gold else AL.Muted, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(mod.name, color = AL.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(mod.author, color = AL.Muted, fontSize = 11.sp)
                Spacer(Modifier.width(6.dp)); Text("v${mod.version}", color = AL.GoldDark, fontSize = 11.sp)
                Spacer(Modifier.width(6.dp)); StatusChip(mod.format.name, if (mod.format == ModFormat.LUA) AL.Purple else AL.Info)
            }
        }
        Switch(checked = mod.enabled, onCheckedChange = { onToggle() },
            colors = SwitchDefaults.colors(checkedThumbColor = AL.Gold, checkedTrackColor = AL.GoldBg))
        Spacer(Modifier.width(4.dp))
        IconButton(onClick = { confirm = true }, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Outlined.DeleteOutline, null, tint = AL.Error.copy(0.7f), modifier = Modifier.size(18.dp))
        }
    }
    if (confirm) AlertDialog(onDismissRequest = { confirm = false }, containerColor = AL.Surface,
        title = { Text("Remover mod?", color = AL.White) },
        text = { Text("'${mod.name}' será removido.", color = AL.Muted) },
        confirmButton = { TextButton(onClick = { onDelete(); confirm = false }) { Text("Remover", color = AL.Error) } },
        dismissButton = { TextButton(onClick = { confirm = false }) { Text("Cancelar", color = AL.Muted) } })
}
