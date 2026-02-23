package com.astrallauncher.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.astrallauncher.ui.AL

val navTabs = listOf(
    NavTab("Home",    Icons.Outlined.Home),
    NavTab("Explore", Icons.Outlined.TravelExplore),
    NavTab("Servers", Icons.Outlined.Storage),
    NavTab("Scripts", Icons.Outlined.Code),
    NavTab("Settings",Icons.Outlined.Settings)
)

data class NavTab(val label: String, val icon: ImageVector)

@Composable
fun AstralTopBar(title: String, actions: @Composable RowScope.() -> Unit = {}) {
    Row(Modifier.fillMaxWidth().background(AL.Bg).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = AL.White, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, modifier = Modifier.weight(1f))
        actions()
    }
}

@Composable
fun AstralBottomBar(selected: Int, onSelect: (Int) -> Unit, modCount: Int) {
    NavigationBar(containerColor = AL.BgCard, tonalElevation = 0.dp,
        modifier = Modifier.border(BorderStroke(0.5.dp, AL.Border), RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))) {
        navTabs.forEachIndexed { i, tab ->
            NavigationBarItem(
                selected = selected == i,
                onClick = { onSelect(i) },
                icon = {
                    BadgedBox(badge = { if (i == 0 && modCount > 0) Badge(containerColor = AL.Gold) { Text("$modCount", color = Color.Black, fontSize = 9.sp) } }) {
                        Icon(tab.icon, tab.label, modifier = Modifier.size(22.dp))
                    }
                },
                label = { Text(tab.label, fontSize = 10.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = AL.Gold, selectedTextColor = AL.Gold,
                    unselectedIconColor = AL.Muted, unselectedTextColor = AL.Muted,
                    indicatorColor = AL.GoldBg
                )
            )
        }
    }
}

@Composable
fun StatusChip(text: String, color: Color, modifier: Modifier = Modifier) {
    Box(modifier.clip(RoundedCornerShape(6.dp)).background(color.copy(0.12f)).padding(horizontal = 7.dp, vertical = 3.dp)) {
        Text(text, color = color, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun SectionHeader(text: String, action: String? = null) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text, color = AL.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(1f))
        action?.let { Text(it, color = AL.Muted, fontSize = 12.sp) }
    }
}

@Composable
fun GoldDivider() {
    HorizontalDivider(color = AL.GoldDark.copy(0.3f), thickness = 0.5.dp)
}

@Composable
fun GoldButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null, enabled: Boolean = true) {
    Button(onClick = onClick, enabled = enabled, modifier = modifier.height(44.dp),
        colors = ButtonDefaults.buttonColors(containerColor = AL.Gold, disabledContainerColor = AL.GoldDark.copy(0.3f)),
        shape = RoundedCornerShape(12.dp)) {
        icon?.let { Icon(it, null, modifier = Modifier.size(16.dp), tint = Color.Black); Spacer(Modifier.width(6.dp)) }
        Text(text, fontWeight = FontWeight.Bold, color = Color.Black)
    }
}

@Composable
fun PurpleButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null, enabled: Boolean = true) {
    Button(onClick = onClick, enabled = enabled, modifier = modifier.height(44.dp),
        colors = ButtonDefaults.buttonColors(containerColor = AL.Purple, disabledContainerColor = AL.Purple.copy(0.3f)),
        shape = RoundedCornerShape(12.dp)) {
        icon?.let { Icon(it, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)) }
        Text(text, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun GhostButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, color: Color = AL.White) {
    OutlinedButton(onClick = onClick, modifier = modifier.height(44.dp),
        border = BorderStroke(1.dp, color.copy(0.5f)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = color),
        shape = RoundedCornerShape(12.dp)) {
        Text(text, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun EmptyState(icon: ImageVector, title: String, subtitle: String, action: (@Composable () -> Unit)? = null) {
    Column(Modifier.fillMaxWidth().padding(vertical = 48.dp, horizontal = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = AL.Muted.copy(0.3f), modifier = Modifier.size(56.dp))
        Spacer(Modifier.height(16.dp))
        Text(title, color = AL.MutedL, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(Modifier.height(6.dp))
        Text(subtitle, color = AL.Muted, fontSize = 13.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        action?.let { Spacer(Modifier.height(20.dp)); it() }
    }
}

@Composable
fun AstralCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.clip(RoundedCornerShape(14.dp)).background(AL.Surface)
        .border(BorderStroke(0.5.dp, AL.Border), RoundedCornerShape(14.dp)).padding(14.dp), content = content)
}
