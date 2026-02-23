package com.astrallauncher.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.astrallauncher.MainViewModel
import com.astrallauncher.network.ModRepositoryApi
import com.astrallauncher.ui.AL
import com.astrallauncher.ui.components.*

@Composable
fun SettingsScreen(vm: MainViewModel, footerClicks: Int, onFooterClick: () -> Unit) {
    val ctx = LocalContext.current
    val overlayActive by vm.overlayActive.collectAsState()
    var overlayEnabled by remember { mutableStateOf(true) }
    var repoUrl by remember { mutableStateOf(ModRepositoryApi.REPO_URL) }
    var showRepoEdit by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(AL.Bg),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text("Settings", color = AL.White, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
            }
        }

        item {
            SettingsSection("Overlay & Launch") {
                SettingsToggle(
                    icon = Icons.Outlined.Layers,
                    title = "Floating Overlay",
                    subtitle = "Show info bubble when Among Us is running",
                    checked = overlayEnabled,
                    onToggle = { overlayEnabled = it }
                )
                if (overlayActive) {
                    SettingsAction(
                        icon = Icons.Outlined.Stop,
                        title = "Stop Active Overlay",
                        subtitle = "Kill the overlay service",
                        iconTint = AL.Error,
                        onClick = { vm.stopOverlay() }
                    )
                }
                SettingsAction(
                    icon = Icons.Outlined.AdminPanelSettings,
                    title = "Overlay Permission",
                    subtitle = "Required to show overlay on top of games",
                    iconTint = if (vm.hasOverlayPermission()) AL.Success else AL.Warning,
                    onClick = { vm.requestOverlayPermission(ctx) },
                    trailing = {
                        StatusChip(if (vm.hasOverlayPermission()) "Granted" else "Missing",
                            if (vm.hasOverlayPermission()) AL.Success else AL.Warning)
                    }
                )
            }
        }

        item {
            SettingsSection("Repository") {
                SettingsAction(
                    icon = Icons.Outlined.CloudDownload,
                    title = "Mod Repository URL",
                    subtitle = repoUrl.take(48) + if (repoUrl.length > 48) "..." else "",
                    onClick = { showRepoEdit = !showRepoEdit }
                )
                if (showRepoEdit) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        OutlinedTextField(
                            value = repoUrl,
                            onValueChange = { repoUrl = it },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AL.Gold,
                                unfocusedBorderColor = AL.Border,
                                focusedTextColor = AL.White,
                                unfocusedTextColor = AL.White,
                                cursorColor = AL.Gold
                            ),
                            shape = RoundedCornerShape(10.dp),
                            placeholder = { Text("https://raw.githubusercontent.com/...", color = AL.Muted) }
                        )
                        Spacer(Modifier.height(8.dp))
                        Row {
                            GoldButton("Apply", onClick = {
                                vm.fetchMods(repoUrl)
                                showRepoEdit = false
                            }, modifier = Modifier.weight(1f))
                            Spacer(Modifier.width(8.dp))
                            GhostButton("Reset", onClick = {
                                repoUrl = ModRepositoryApi.REPO_URL
                                showRepoEdit = false
                            })
                        }
                    }
                }
                SettingsAction(
                    icon = Icons.Outlined.Refresh,
                    title = "Refresh Mod List",
                    subtitle = "Re-fetch mods from repository",
                    onClick = { vm.fetchMods(repoUrl) }
                )
            }
        }

        item {
            SettingsSection("About") {
                SettingsAction(
                    icon = Icons.Outlined.Code,
                    title = "Source Code",
                    subtitle = "github.com/YOUR_USERNAME/AstralLauncher",
                    iconTint = AL.Purple,
                    onClick = {
                        ctx.startActivity(Intent(Intent.ACTION_VIEW,
                            Uri.parse("https://github.com/YOUR_USERNAME/AstralLauncher")).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        })
                    }
                )
                SettingsAction(
                    icon = Icons.Outlined.ChatBubbleOutline,
                    title = "Discord Community",
                    subtitle = "Join the Astral Launcher Discord",
                    iconTint = AL.Info,
                    onClick = {
                        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://discord.gg/")).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        })
                    }
                )
                SettingsRow {
                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(AL.GoldBg),
                            contentAlignment = Alignment.Center) {
                            Text("✦", color = AL.Gold, fontSize = 18.sp)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Astral Launcher", color = AL.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text("Version 1.0.0", color = AL.Muted, fontSize = 12.sp)
                        }
                        StatusChip("Open Source", AL.Gold)
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(24.dp))
            Footer(footerClicks, onFooterClick)
            Spacer(Modifier.height(8.dp))
            GithubButton()
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Text(title, color = AL.Gold, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp))
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(AL.BgCard)
                .border(BorderStroke(0.5.dp, AL.Border), RoundedCornerShape(16.dp)),
            content = content
        )
    }
}

@Composable
fun SettingsToggle(icon: ImageVector, title: String, subtitle: String, checked: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(AL.Surface),
            contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = AL.Gold, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = AL.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = AL.Muted, fontSize = 12.sp)
        }
        Switch(checked = checked, onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(checkedThumbColor = AL.Gold, checkedTrackColor = AL.GoldBg))
    }
}

@Composable
fun SettingsAction(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconTint: Color = AL.Gold,
    onClick: () -> Unit,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(AL.Surface),
            contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = iconTint, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = AL.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = AL.Muted, fontSize = 12.sp)
        }
        trailing?.invoke() ?: Icon(Icons.Outlined.ChevronRight, null, tint = AL.Muted, modifier = Modifier.size(18.dp))
    }
}

@Composable
fun SettingsRow(content: @Composable () -> Unit) {
    content()
}

@Composable
fun Footer(clicks: Int, onClick: () -> Unit) {
    val t = rememberInfiniteTransition(label = "rgb")
    val hue by t.animateFloat(0f, 360f, infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Restart), label = "h")
    val c = androidx.compose.ui.graphics.Color.hsv(hue, 0.75f, 1f)
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(modifier = Modifier.clickable { onClick() }, horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.AutoAwesome, null, tint = c, modifier = Modifier.size(13.dp))
            Spacer(Modifier.width(6.dp))
            Text("Astral Launcher", fontSize = 12.sp, color = c, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(6.dp))
            Icon(Icons.Outlined.AutoAwesome, null, tint = c, modifier = Modifier.size(13.dp))
        }
        if (clicks in 1..4) Text("${5 - clicks}x more to unlock developer mode", fontSize = 10.sp, color = AL.Muted.copy(0.5f))
    }
}

@Composable
fun GithubButton() {
    val ctx = LocalContext.current
    OutlinedButton(
        onClick = {
            ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/YOUR_USERNAME/AstralLauncher")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        },
        modifier = Modifier.fillMaxWidth().height(40.dp).padding(horizontal = 16.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = AL.Muted),
        border = BorderStroke(1.dp, AL.Border),
        shape = RoundedCornerShape(12.dp)
    ) {
        Icon(Icons.Outlined.Code, null, modifier = Modifier.size(16.dp), tint = AL.Muted)
        Spacer(Modifier.width(8.dp))
        Text("YOUR_USERNAME/AstralLauncher", fontWeight = FontWeight.SemiBold, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 13.sp)
    }
}
