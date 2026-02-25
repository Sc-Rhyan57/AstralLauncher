package com.astrallauncher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.astrallauncher.ui.AstralTheme
import com.astrallauncher.ui.CardBg
import com.astrallauncher.ui.DarkBg
import com.astrallauncher.ui.Gold
import com.astrallauncher.ui.TextSecondary
import com.astrallauncher.ui.screens.*

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AstralTheme {
                AstralApp(vm)
            }
        }
    }
}

data class NavTab(val icon: String, val label: String)

val tabs = listOf(
    NavTab("🏠", "Início"),
    NavTab("🔍", "Explorar"),
    NavTab("📦", "Mods"),
    NavTab("🖥", "Servers"),
    NavTab("⚙", "Config")
)

@Composable
fun AstralApp(vm: MainViewModel) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(
        Modifier.fillMaxSize().background(DarkBg)
    ) {
        Box(Modifier.weight(1f)) {
            when (selectedTab) {
                0 -> HomeScreen(vm)
                1 -> ExploreScreen(vm)
                2 -> ModsScreen(vm)
                3 -> ServersScreen(vm)
                4 -> SettingsScreen(vm)
            }
        }

        HorizontalDivider(color = Color.White.copy(alpha = 0.06f))

        Row(
            Modifier.fillMaxWidth().background(CardBg).padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            tabs.forEachIndexed { i, tab ->
                val selected = selectedTab == i
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedTab = i }
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(tab.icon, fontSize = 18.sp)
                    Text(
                        tab.label,
                        fontSize = 9.sp,
                        color = if (selected) Gold else TextSecondary,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                    )
                    if (selected) {
                        Box(
                            Modifier
                                .width(20.dp)
                                .height(2.dp)
                                .background(Gold)
                        )
                    }
                }
            }
        }
    }
}
