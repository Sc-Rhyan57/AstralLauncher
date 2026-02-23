package com.astrallauncher.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.*
import com.astrallauncher.ui.AL

data class NavTab(val label: String, val icon: ImageVector)

val navTabs = listOf(
    NavTab("Home", Icons.Outlined.Home),
    NavTab("Explore", Icons.Outlined.Explore),
    NavTab("Servers", Icons.Outlined.Dns),
    NavTab("Script", Icons.Outlined.Code),
    NavTab("Settings", Icons.Outlined.Settings),
)

@Composable
fun AstralTopBar(
    title: String = "Astral Launcher",
    actions: @Composable RowScope.() -> Unit = {}
) {
    val t = rememberInfiniteTransition(label = "glow")
    val alpha by t.animateFloat(0.5f, 1f, infiniteRepeatable(tween(2000), RepeatMode.Reverse), label = "a")

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(AL.GoldBg),
                contentAlignment = Alignment.Center
            ) {
                Text("✦", color = AL.Gold.copy(alpha = alpha), fontSize = 16.sp)
            }
            Spacer(Modifier.width(10.dp))
            Text(
                title,
                color = AL.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                letterSpacing = 0.5.sp
            )
            Spacer(Modifier.weight(1f))
            actions()
        }
        Divider(color = AL.Border, thickness = 0.5.dp)
    }
}

@Composable
fun AstralBottomBar(selected: Int, onSelect: (Int) -> Unit, installedCount: Int = 0) {
    NavigationBar(
        containerColor = AL.Surface,
        tonalElevation = 0.dp
    ) {
        navTabs.forEachIndexed { idx, tab ->
            NavigationBarItem(
                selected = selected == idx,
                onClick = { onSelect(idx) },
                icon = {
                    BadgedBox(badge = {
                        if (idx == 0 && installedCount > 0)
                            Badge(containerColor = AL.Gold) { Text("$installedCount", fontSize = 8.sp, color = Color.Black) }
                    }) {
                        Icon(tab.icon, tab.label, modifier = Modifier.size(22.dp))
                    }
                },
                label = {
                    Text(
                        tab.label,
                        fontSize = 10.sp,
                        fontWeight = if (selected == idx) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = AL.Gold,
                    selectedTextColor = AL.Gold,
                    unselectedIconColor = AL.Muted,
                    unselectedTextColor = AL.Muted,
                    indicatorColor = AL.GoldBg,
                )
            )
        }
    }
}

@Composable
fun GoldButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = AL.Gold,
            contentColor = Color.Black,
            disabledContainerColor = AL.GoldDark.copy(0.4f),
            disabledContentColor = AL.Muted
        )
    ) {
        if (icon != null) {
            Icon(icon, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

@Composable
fun GhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = AL.Muted
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = color),
        border = BorderStroke(1.dp, color.copy(0.4f))
    ) {
        Text(text, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}

@Composable
fun AstralCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val m = if (onClick != null)
        modifier.clickable { onClick() }
    else modifier

    Card(
        modifier = m,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AL.BgCard),
        border = BorderStroke(0.5.dp, AL.Border)
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}

@Composable
fun SectionHeader(title: String, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = AL.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(Modifier.weight(1f))
        if (action != null && onAction != null) {
            TextButton(onClick = onAction) {
                Text(action + " →", color = AL.Gold, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
fun EmptyState(icon: ImageVector, title: String, subtitle: String, action: @Composable (() -> Unit)? = null) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(AL.GoldBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, modifier = Modifier.size(40.dp), tint = AL.Gold.copy(0.7f))
        }
        Spacer(Modifier.height(20.dp))
        Text(title, color = AL.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(Modifier.height(8.dp))
        Text(subtitle, color = AL.Muted, fontSize = 14.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp))
        if (action != null) {
            Spacer(Modifier.height(24.dp))
            action()
        }
    }
}

@Composable
fun GoldDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(
                Brush.horizontalGradient(
                    listOf(Color.Transparent, AL.Gold.copy(0.3f), Color.Transparent)
                )
            )
    )
}

@Composable
fun StatusChip(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(0.15f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(text, color = color, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}
