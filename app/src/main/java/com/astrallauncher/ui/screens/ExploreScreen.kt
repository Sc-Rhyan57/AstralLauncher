package com.astrallauncher.ui.screens

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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import coil.compose.AsyncImage
import com.astrallauncher.MainViewModel
import com.astrallauncher.model.ModEntry
import com.astrallauncher.ui.AL
import com.astrallauncher.ui.components.*

@Composable
fun ExploreScreen(vm: MainViewModel) {
    val mods by vm.mods.collectAsState()
    val loading by vm.modsLoading.collectAsState()
    val error by vm.modsError.collectAsState()
    val installed by vm.installedMods.collectAsState()
    val progress by vm.downloadProgress.collectAsState()
    var query by remember { mutableStateOf("") }
    var detail by remember { mutableStateOf<ModEntry?>(null) }

    val filtered = mods.filter { query.isBlank() || it.name.contains(query, true) || it.author.contains(query, true) || it.tags.any { t -> t.contains(query, true) } }
    val trending = mods.sortedByDescending { it.downloads }.take(5)

    if (detail != null) {
        ModDetail(mod = detail!!, isInstalled = installed.any { it.id == detail!!.id }, progress = progress[detail!!.id],
            onInstall = { vm.downloadAndInstall(detail!!) }, onBack = { detail = null })
        return
    }

    LazyColumn(modifier = Modifier.fillMaxSize().background(AL.Bg), contentPadding = PaddingValues(bottom = 100.dp)) {
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Explore", color = AL.White, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, modifier = Modifier.weight(1f))
                IconButton(onClick = { vm.fetchMods() }) { Icon(Icons.Outlined.Refresh, null, tint = AL.Muted) }
            }
        }
        item {
            OutlinedTextField(value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                placeholder = { Text("Search mods...", color = AL.Muted) }, leadingIcon = { Icon(Icons.Outlined.Search, null, tint = AL.Muted) },
                trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Outlined.Close, null, tint = AL.Muted) } },
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AL.Gold, unfocusedBorderColor = AL.Border, focusedTextColor = AL.White, unfocusedTextColor = AL.White, cursorColor = AL.Gold),
                shape = RoundedCornerShape(14.dp), singleLine = true)
        }
        if (loading) item { Box(Modifier.fillMaxWidth().padding(64.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = AL.Gold) } }
        else if (error != null) item { EmptyState(Icons.Outlined.WifiOff, "Could not load mods", error ?: "Check connection", action = { GoldButton("Retry", { vm.fetchMods() }) }) }
        else {
            if (query.isEmpty() && trending.isNotEmpty()) {
                item { SectionHeader("Trending") }
                item { LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(horizontal = 16.dp)) {
                    items(trending) { TrendCard(it) { detail = it } }
                } }
                item { Spacer(Modifier.height(8.dp)); SectionHeader("All Mods") }
            }
            if (filtered.isEmpty()) item { EmptyState(Icons.Outlined.Search, "No results", "Try a different search") }
            else items(filtered) { mod -> ModRow2(mod, installed.any { it.id == mod.id }, progress[mod.id], { vm.downloadAndInstall(mod) }) { detail = mod } }
        }
    }
}

@Composable fun TrendCard(mod: ModEntry, onClick: () -> Unit) {
    Box(Modifier.width(155.dp).height(95.dp).clip(RoundedCornerShape(14.dp)).background(AL.Surface)
        .border(BorderStroke(0.5.dp, AL.Border), RoundedCornerShape(14.dp)).clickable { onClick() }) {
        if (mod.banner.isNotEmpty()) { AsyncImage(mod.banner, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(AL.Bg.copy(0f), AL.Bg.copy(0.9f))))) }
        Column(Modifier.align(Alignment.BottomStart).padding(8.dp)) {
            Text(mod.name, color = AL.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${mod.downloads} dl", color = AL.Muted, fontSize = 10.sp)
        }
    }
}

@Composable fun ModRow2(mod: ModEntry, installed: Boolean, progress: Int?, onInstall: () -> Unit, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 3.dp).clip(RoundedCornerShape(14.dp))
        .background(AL.BgCard).border(BorderStroke(0.5.dp, if (installed) AL.GoldDark.copy(0.5f) else AL.Border), RoundedCornerShape(14.dp))
        .clickable { onClick() }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(50.dp).clip(RoundedCornerShape(12.dp)).background(AL.Surface)) {
            if (mod.icon.isNotEmpty()) AsyncImage(mod.icon, null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            else Box(Modifier.fillMaxSize().background(AL.GoldBg), contentAlignment = Alignment.Center) { Text(mod.name.firstOrNull()?.toString() ?: "?", color = AL.Gold, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp) }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(mod.name, color = AL.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(mod.author, color = AL.Gold, fontSize = 11.sp)
            Text(mod.shortDescription.ifEmpty { mod.description }, color = AL.Muted, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Row(Modifier.padding(top = 3.dp)) {
                Icon(Icons.Outlined.GetApp, null, tint = AL.Muted, modifier = Modifier.size(12.dp))
                Text(" ${mod.downloads}", color = AL.Muted, fontSize = 11.sp)
                Spacer(Modifier.width(6.dp))
                mod.tags.take(2).forEach { StatusChip(it, AL.Purple); Spacer(Modifier.width(4.dp)) }
            }
        }
        Spacer(Modifier.width(8.dp))
        if (progress != null) CircularProgressIndicator(progress = { progress / 100f }, modifier = Modifier.size(32.dp), color = AL.Gold, strokeWidth = 3.dp)
        else if (installed) StatusChip("✓", AL.Success)
        else IconButton(onClick = onInstall, modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(AL.GoldBg)) {
            Icon(Icons.Outlined.GetApp, null, tint = AL.Gold, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable fun ModDetail(mod: ModEntry, isInstalled: Boolean, progress: Int?, onInstall: () -> Unit, onBack: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize().background(AL.Bg), contentPadding = PaddingValues(bottom = 100.dp)) {
        item {
            Box(Modifier.fillMaxWidth().height(190.dp).background(AL.GoldBg)) {
                if (mod.banner.isNotEmpty()) { AsyncImage(mod.banner, null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(AL.Bg.copy(0f), AL.Bg)))) }
                else Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(AL.GoldBg, AL.Bg))), contentAlignment = Alignment.Center) { Text("✦", color = AL.Gold.copy(0.2f), fontSize = 80.sp) }
                IconButton(onClick = onBack, modifier = Modifier.padding(12.dp)) { Icon(Icons.Outlined.ArrowBack, null, tint = AL.White) }
            }
        }
        item {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (mod.icon.isNotEmpty()) { AsyncImage(mod.icon, null, modifier = Modifier.size(54.dp).clip(RoundedCornerShape(14.dp)), contentScale = ContentScale.Crop); Spacer(Modifier.width(12.dp)) }
                    Column(Modifier.weight(1f)) {
                        Text(mod.name, color = AL.White, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                        Text(mod.author, color = AL.Gold, fontSize = 13.sp)
                        Text("v${mod.version} • AU ${mod.gameVersion}", color = AL.Muted, fontSize = 11.sp)
                    }
                }
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    mod.tags.forEach { StatusChip(it, AL.Purple) }
                    if (mod.starlightCompatible) StatusChip("Starlight ✓", AL.Gold)
                    if (mod.supportsLua) StatusChip("Lua", AL.Info)
                }
                Spacer(Modifier.height(18.dp))
                Text(mod.description, color = AL.MutedL, fontSize = 14.sp, lineHeight = 22.sp)
                Spacer(Modifier.height(22.dp))
                when {
                    progress != null -> { LinearProgressIndicator(progress = { progress / 100f }, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)), color = AL.Gold); Spacer(Modifier.height(8.dp)); Text("Downloading $progress%", color = AL.Muted, fontSize = 13.sp) }
                    isInstalled -> StatusChip("✓ Installed", AL.Success)
                    else -> GoldButton("Install", onClick = onInstall, modifier = Modifier.fillMaxWidth(), icon = Icons.Outlined.GetApp)
                }
                if (mod.changelog.isNotEmpty()) {
                    Spacer(Modifier.height(20.dp)); GoldDivider(); Spacer(Modifier.height(14.dp))
                    Text("Changelog", color = AL.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    mod.changelog.forEach { e ->
                        Spacer(Modifier.height(8.dp)); Text("v${e.version} — ${e.date}", color = AL.Gold, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        e.changes.forEach { Text("• $it", color = AL.Muted, fontSize = 13.sp) }
                    }
                }
            }
        }
    }
}
