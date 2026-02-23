package com.astrallauncher.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.*
import com.astrallauncher.MainViewModel
import com.astrallauncher.model.InstalledMod
import com.astrallauncher.model.ModFormat
import com.astrallauncher.ui.AL
import com.astrallauncher.ui.components.*

@Composable
fun HomeScreen(vm: MainViewModel) {
    val installedMods by vm.installedMods.collectAsState()
    val auVersion by vm.auVersion.collectAsState()
    val overlayActive by vm.overlayActive.collectAsState()
    val injectStatus by vm.injectStatus.collectAsState()
    val ctx = LocalContext.current

    LaunchedEffect(Unit) { vm.checkAmongUs() }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(AL.Bg),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        item {
            GameInfoCard(auVersion = auVersion)
        }

        item {
            LaunchSection(
                installedMods = installedMods,
                overlayActive = overlayActive,
                onLaunch = { activeMod ->
                    if (!vm.hasOverlayPermission()) {
                        vm.requestOverlayPermission(ctx)
                    }
                    vm.launchGame(activeMod)
                },
                onStopOverlay = { vm.stopOverlay() }
            )
        }

        item {
            if (installedMods.isEmpty()) {
                EmptyState(
                    icon = Icons.Outlined.Extension,
                    title = "No mods installed",
                    subtitle = "Go to Explore to browse and install mods for Among Us"
                )
            } else {
                SectionHeader("Installed Mods", action = "${installedMods.size}")
            }
        }

        items(installedMods) { mod ->
            InstalledModRow(
                mod = mod,
                onToggle = { vm.toggleMod(mod.id) },
                onDelete = { vm.uninstallMod(mod.id) }
            )
        }

        item {
            injectStatus?.let { status ->
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(AL.GoldBg)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.Info, null, tint = AL.Gold, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(status, color = AL.GoldLight, fontSize = 13.sp, modifier = Modifier.weight(1f))
                    IconButton(onClick = { vm.clearInjectStatus() }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Outlined.Close, null, tint = AL.Muted, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun GameInfoCard(auVersion: String) {
    val t = rememberInfiniteTransition(label = "pulse")
    val glow by t.animateFloat(0.3f, 0.8f, infiniteRepeatable(tween(2000, easing = EaseInOutSine), RepeatMode.Reverse), label = "g")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.verticalGradient(listOf(AL.GoldBg, AL.Bg))
            )
            .border(BorderStroke(1.dp, AL.Gold.copy(glow)), RoundedCornerShape(20.dp))
            .padding(20.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(AL.GoldSub),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🚀", fontSize = 24.sp)
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Astral Launcher", color = AL.White, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                    Text("Among Us Mod Launcher", color = AL.Gold, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
                Spacer(Modifier.weight(1f))
                StatusChip("v1.0.0", AL.Gold)
            }
            Spacer(Modifier.height(16.dp))
            GoldDivider()
            Spacer(Modifier.height(16.dp))
            Row {
                InfoPill("AU Version", auVersion)
                Spacer(Modifier.width(12.dp))
                InfoPill("Platform", "Android")
            }
        }
    }
}

@Composable
fun InfoPill(label: String, value: String) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(AL.Surface)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(label, color = AL.Muted, fontSize = 10.sp, fontWeight = FontWeight.Medium)
        Text(value, color = AL.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun LaunchSection(
    installedMods: List<InstalledMod>,
    overlayActive: Boolean,
    onLaunch: (String) -> Unit,
    onStopOverlay: () -> Unit
) {
    var selectedModId by remember { mutableStateOf<String?>(null) }
    val activeMod = installedMods.firstOrNull { it.id == selectedModId } ?: installedMods.firstOrNull()

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        if (installedMods.isNotEmpty()) {
            Text("Active Profile", color = AL.Muted, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(8.dp))
            var expanded by remember { mutableStateOf(false) }
            Box {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(AL.Surface2)
                        .border(BorderStroke(1.dp, AL.Border), RoundedCornerShape(12.dp))
                        .clickable { expanded = true }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.Extension, null, tint = AL.Gold, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(activeMod?.name ?: "Vanilla", color = AL.White, fontSize = 14.sp, modifier = Modifier.weight(1f))
                    Icon(Icons.Outlined.KeyboardArrowDown, null, tint = AL.Muted, modifier = Modifier.size(18.dp))
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.background(AL.Surface2)
                ) {
                    DropdownMenuItem(
                        text = { Text("Vanilla (No Mods)", color = AL.MutedLight) },
                        onClick = { selectedModId = null; expanded = false }
                    )
                    installedMods.filter { it.enabled }.forEach { mod ->
                        DropdownMenuItem(
                            text = { Text(mod.name, color = AL.White) },
                            onClick = { selectedModId = mod.id; expanded = false }
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        Row {
            GoldButton(
                text = "Launch Among Us",
                onClick = { onLaunch(activeMod?.name ?: "Vanilla") },
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.RocketLaunch
            )
            if (overlayActive) {
                Spacer(Modifier.width(10.dp))
                GhostButton(
                    text = "Stop Overlay",
                    onClick = onStopOverlay,
                    color = AL.Error
                )
            }
        }
    }
}

@Composable
fun InstalledModRow(mod: InstalledMod, onToggle: () -> Unit, onDelete: () -> Unit) {
    var showConfirm by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(AL.BgCard)
            .border(BorderStroke(0.5.dp, AL.Border), RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (mod.enabled) AL.GoldBg else AL.Surface),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (mod.format == ModFormat.LUA) Icons.Outlined.Code else Icons.Outlined.Extension,
                null,
                tint = if (mod.enabled) AL.Gold else AL.Muted,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(mod.name, color = AL.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Row {
                Text(mod.author, color = AL.Muted, fontSize = 11.sp)
                Spacer(Modifier.width(8.dp))
                Text("v${mod.version}", color = AL.GoldDark, fontSize = 11.sp)
                if (mod.format == ModFormat.LUA) {
                    Spacer(Modifier.width(8.dp))
                    StatusChip("Lua", AL.Purple)
                }
            }
        }
        Switch(
            checked = mod.enabled,
            onCheckedChange = { onToggle() },
            colors = SwitchDefaults.colors(checkedThumbColor = AL.Gold, checkedTrackColor = AL.GoldBg)
        )
        Spacer(Modifier.width(8.dp))
        IconButton(onClick = { showConfirm = true }, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Outlined.DeleteOutline, null, tint = AL.Error.copy(0.7f), modifier = Modifier.size(18.dp))
        }
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            containerColor = AL.Surface,
            title = { Text("Remove Mod", color = AL.White) },
            text = { Text("Remove '${mod.name}'? This will delete its files.", color = AL.Muted) },
            confirmButton = {
                TextButton(onClick = { onDelete(); showConfirm = false }) {
                    Text("Remove", color = AL.Error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) {
                    Text("Cancel", color = AL.Muted)
                }
            }
        )
    }
}
