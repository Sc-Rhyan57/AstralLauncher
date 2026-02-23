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
import androidx.compose.ui.platform.LocalContext
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
    val installedMods by vm.installedMods.collectAsState()
    val downloadProgress by vm.downloadProgress.collectAsState()
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<ModEntry?>(null) }

    val filtered = mods.filter {
        query.isBlank() || it.name.contains(query, true) || it.author.contains(query, true) || it.tags.any { t -> t.contains(query, true) }
    }
    val trending = mods.sortedByDescending { it.downloads }.take(5)

    if (selected != null) {
        ModDetailSheet(
            mod = selected!!,
            isInstalled = installedMods.any { it.id == selected!!.id },
            progress = downloadProgress[selected!!.id],
            onInstall = { vm.downloadAndInstallMod(selected!!) },
            onBack = { selected = null }
        )
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(AL.Bg),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Explore", color = AL.White, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, modifier = Modifier.weight(1f))
                IconButton(onClick = { vm.fetchMods() }) {
                    Icon(Icons.Outlined.Refresh, null, tint = AL.Muted)
                }
            }
        }

        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                placeholder = { Text("Search mods...", color = AL.Muted) },
                leadingIcon = { Icon(Icons.Outlined.Search, null, tint = AL.Muted) },
                trailingIcon = {
                    if (query.isNotEmpty()) IconButton(onClick = { query = "" }) {
                        Icon(Icons.Outlined.Close, null, tint = AL.Muted)
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AL.Gold,
                    unfocusedBorderColor = AL.Border,
                    focusedTextColor = AL.White,
                    unfocusedTextColor = AL.White,
                    cursorColor = AL.Gold
                ),
                shape = RoundedCornerShape(14.dp),
                singleLine = true
            )
        }

        if (loading) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(64.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AL.Gold)
                }
            }
        } else if (error != null) {
            item {
                EmptyState(
                    icon = Icons.Outlined.WifiOff,
                    title = "Could not load mods",
                    subtitle = error ?: "Check your internet connection and try again",
                    action = { GoldButton("Retry", onClick = { vm.fetchMods() }) }
                )
            }
        } else {
            if (query.isEmpty() && trending.isNotEmpty()) {
                item { SectionHeader("Trending", action = "See all") }
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        items(trending) { mod ->
                            TrendingModCard(mod = mod, onClick = { selected = mod })
                        }
                    }
                }
                item { Spacer(Modifier.height(8.dp)) }
                item { SectionHeader("All Mods") }
            }

            if (filtered.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Outlined.SearchOff,
                        title = "No mods found",
                        subtitle = "Try a different search term"
                    )
                }
            } else {
                items(filtered) { mod ->
                    ModListRow(
                        mod = mod,
                        isInstalled = installedMods.any { it.id == mod.id },
                        progress = downloadProgress[mod.id],
                        onInstall = { vm.downloadAndInstallMod(mod) },
                        onClick = { selected = mod }
                    )
                }
            }
        }
    }
}

@Composable
fun TrendingModCard(mod: ModEntry, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .width(160.dp)
            .height(100.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(AL.Surface)
            .border(BorderStroke(0.5.dp, AL.Border), RoundedCornerShape(14.dp))
            .clickable { onClick() }
    ) {
        if (mod.banner.isNotEmpty()) {
            AsyncImage(
                model = mod.banner,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(AL.Bg.copy(0f), AL.Bg.copy(0.85f)))))
        }
        Column(
            modifier = Modifier.align(Alignment.BottomStart).padding(10.dp)
        ) {
            Text(mod.name, color = AL.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${mod.downloads} downloads", color = AL.Muted, fontSize = 10.sp)
        }
        if (mod.icon.isNotEmpty()) {
            AsyncImage(
                model = mod.icon,
                contentDescription = null,
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .align(Alignment.TopStart)
                    .padding(8.dp)
            )
        }
    }
}

@Composable
fun ModListRow(
    mod: ModEntry,
    isInstalled: Boolean,
    progress: Int?,
    onInstall: () -> Unit,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(AL.BgCard)
            .border(BorderStroke(0.5.dp, if (isInstalled) AL.GoldDark.copy(0.5f) else AL.Border), RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(AL.Surface)
        ) {
            if (mod.icon.isNotEmpty()) {
                AsyncImage(model = mod.icon, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Box(Modifier.fillMaxSize().background(AL.GoldBg), contentAlignment = Alignment.Center) {
                    Text(mod.name.firstOrNull()?.toString() ?: "?", color = AL.Gold, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                }
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(mod.name, color = AL.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(mod.author, color = AL.Gold, fontSize = 11.sp)
            Text(mod.shortDescription.ifEmpty { mod.description }, color = AL.Muted, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            Row {
                Icon(Icons.Outlined.Download, null, tint = AL.Muted, modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(3.dp))
                Text("${mod.downloads}", color = AL.Muted, fontSize = 11.sp)
                if (mod.tags.isNotEmpty()) {
                    Spacer(Modifier.width(8.dp))
                    mod.tags.take(2).forEach { tag ->
                        StatusChip(tag, AL.Purple)
                        Spacer(Modifier.width(4.dp))
                    }
                }
            }
        }
        Spacer(Modifier.width(8.dp))
        if (progress != null) {
            CircularProgressIndicator(
                progress = { progress / 100f },
                modifier = Modifier.size(32.dp),
                color = AL.Gold,
                strokeWidth = 3.dp
            )
        } else if (isInstalled) {
            StatusChip("Installed", AL.Success)
        } else {
            IconButton(
                onClick = onInstall,
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(AL.GoldBg)
            ) {
                Icon(Icons.Outlined.Download, null, tint = AL.Gold, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun ModDetailSheet(
    mod: ModEntry,
    isInstalled: Boolean,
    progress: Int?,
    onInstall: () -> Unit,
    onBack: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(AL.Bg),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(AL.GoldBg)
            ) {
                if (mod.banner.isNotEmpty()) {
                    AsyncImage(model = mod.banner, contentDescription = null,
                        modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(AL.Bg.copy(0f), AL.Bg))))
                } else {
                    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(AL.GoldBg, AL.Bg))),
                        contentAlignment = Alignment.Center) {
                        Text("✦", color = AL.Gold.copy(0.2f), fontSize = 80.sp)
                    }
                }
                IconButton(onClick = onBack, modifier = Modifier.padding(12.dp)) {
                    Icon(Icons.Outlined.ArrowBack, null, tint = AL.White)
                }
            }
        }

        item {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (mod.icon.isNotEmpty()) {
                        AsyncImage(model = mod.icon, contentDescription = null,
                            modifier = Modifier.size(56.dp).clip(RoundedCornerShape(14.dp)), contentScale = ContentScale.Crop)
                        Spacer(Modifier.width(14.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(mod.name, color = AL.White, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                        Text(mod.author, color = AL.Gold, fontSize = 13.sp)
                        Text("v${mod.version} • AU ${mod.gameVersion}", color = AL.Muted, fontSize = 11.sp)
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row {
                    mod.tags.forEach { tag ->
                        StatusChip(tag, AL.Purple)
                        Spacer(Modifier.width(6.dp))
                    }
                    if (mod.starlightCompatible) StatusChip("Starlight ✓", AL.Gold)
                    if (mod.supportsLua) { Spacer(Modifier.width(6.dp)); StatusChip("Lua", AL.Info) }
                }
                Spacer(Modifier.height(20.dp))
                Text(mod.description, color = AL.MutedLight, fontSize = 14.sp, lineHeight = 22.sp)

                if (!isInstalled && progress == null) {
                    Spacer(Modifier.height(24.dp))
                    GoldButton("Install Mod", onClick = onInstall, modifier = Modifier.fillMaxWidth(), icon = Icons.Outlined.Download)
                } else if (progress != null) {
                    Spacer(Modifier.height(24.dp))
                    LinearProgressIndicator(progress = { progress / 100f }, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)), color = AL.Gold)
                    Spacer(Modifier.height(8.dp))
                    Text("Downloading... $progress%", color = AL.Muted, fontSize = 13.sp)
                } else {
                    Spacer(Modifier.height(24.dp))
                    StatusChip("✓ Installed", AL.Success)
                }

                if (mod.releases.isNotEmpty()) {
                    Spacer(Modifier.height(24.dp))
                    GoldDivider()
                    Spacer(Modifier.height(16.dp))
                    Text("Releases", color = AL.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.height(8.dp))
                    mod.releases.forEach { release ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(AL.Surface)
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("v${release.version}", color = AL.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text(release.publishedAt, color = AL.Muted, fontSize = 11.sp)
                            }
                            StatusChip(release.format.name, AL.Info)
                        }
                    }
                }

                if (mod.changelog.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    GoldDivider()
                    Spacer(Modifier.height(16.dp))
                    Text("Changelog", color = AL.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    mod.changelog.forEach { entry ->
                        Spacer(Modifier.height(10.dp))
                        Text("v${entry.version} — ${entry.date}", color = AL.Gold, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        entry.changes.forEach { change ->
                            Text("• $change", color = AL.Muted, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}
