package com.astrallauncher.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.astrallauncher.MainViewModel
import com.astrallauncher.model.InstalledMod
import com.astrallauncher.ui.CardBg
import com.astrallauncher.ui.Gold
import com.astrallauncher.ui.TextSecondary

@Composable
fun ModsScreen(vm: MainViewModel) {
    val mods by vm.installedMods.collectAsState()

    LaunchedEffect(Unit) { vm.loadInstalledMods() }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Mods Instalados", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Gold)
            Text("${mods.size} DLLs", color = TextSecondary, fontSize = 13.sp)
        }

        if (mods.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("📦", fontSize = 40.sp)
                    Text("Nenhum mod instalado", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text("Vá em Explorar para baixar mods", color = TextSecondary, fontSize = 13.sp)
                }
            }
        } else {
            Text(
                "Mods habilitados serão injetados no próximo patch.",
                color = TextSecondary,
                fontSize = 12.sp
            )
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(mods, key = { it.id }) { mod ->
                    InstalledModCard(
                        mod = mod,
                        onToggle = { vm.toggleModEnabled(mod) },
                        onDelete = { vm.deleteMod(mod) }
                    )
                }
            }
        }
    }
}

@Composable
private fun InstalledModCard(mod: InstalledMod, onToggle: () -> Unit, onDelete: () -> Unit) {
    var showDelete by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (mod.enabled) CardBg else CardBg.copy(alpha = 0.5f)
        )
    ) {
        Row(
            Modifier.padding(14.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(Modifier.weight(1f)) {
                Text(mod.name, fontWeight = FontWeight.Bold, color = if (mod.enabled) Color.White else TextSecondary, fontSize = 13.sp)
                Text("${mod.fileName}", color = TextSecondary, fontSize = 10.sp)
                Text(
                    if (mod.enabled) "● Habilitado" else "○ Desabilitado",
                    color = if (mod.enabled) Color(0xFF00FF88) else Color(0xFFFF6B6B),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Switch(
                checked = mod.enabled,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.Black,
                    checkedTrackColor = Gold,
                    uncheckedThumbColor = Color.Gray,
                    uncheckedTrackColor = Color.DarkGray
                )
            )

            IconButton(onClick = { showDelete = true }) {
                Text("🗑", fontSize = 16.sp)
            }
        }
    }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("Deletar mod?", color = Color.White) },
            text = { Text("Deletar ${mod.name}?", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = { onDelete(); showDelete = false }) {
                    Text("Deletar", color = Color(0xFFFF6B6B))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDelete = false }) {
                    Text("Cancelar", color = Gold)
                }
            },
            containerColor = CardBg
        )
    }
}
