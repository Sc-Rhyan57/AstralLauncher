package com.astrallauncher.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.astrallauncher.MainViewModel
import com.astrallauncher.model.Mod
import com.astrallauncher.ui.CardBg
import com.astrallauncher.ui.Gold
import com.astrallauncher.ui.TextSecondary

@Composable
fun ExploreScreen(vm: MainViewModel) {
    val remoteMods by vm.remoteMods.collectAsState()
    val installed  by vm.installedMods.collectAsState()
    var query by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { vm.fetchRemoteMods() }

    val filtered = remoteMods.filter {
        query.isEmpty() || it.name.contains(query, true) || it.tags.any { t -> t.contains(query, true) }
    }
    val installedIds = installed.map { it.id }.toSet()

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Explorar Mods", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Gold)

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Buscar mods...", color = TextSecondary, fontSize = 13.sp) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Gold,
                unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = Gold
            ),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {})
        )

        if (remoteMods.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator(color = Gold)
                    Text("Carregando repositório...", color = TextSecondary, fontSize = 13.sp)
                }
            }
        } else if (filtered.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Nenhum mod encontrado para \"$query\"", color = TextSecondary, fontSize = 13.sp)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(filtered, key = { it.id }) { mod ->
                    ModCard(mod, installed = mod.id in installedIds, onInstall = { vm.downloadMod(mod) })
                }
            }
        }
    }
}

@Composable
private fun ModCard(mod: Mod, installed: Boolean, onInstall: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(mod.name, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                    Text("por ${mod.author} · v${mod.version}", color = TextSecondary, fontSize = 11.sp)
                }
                if (installed) {
                    Text(
                        "✓ Instalado",
                        color = Color(0xFF00FF88),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(mod.description, color = TextSecondary, fontSize = 12.sp, lineHeight = 17.sp)

            if (mod.tags.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    mod.tags.take(4).forEach { tag ->
                        Text(
                            tag,
                            modifier = Modifier
                                .background(Gold.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                                .border(0.5.dp, Gold.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            color = Gold,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            if (!installed) {
                Button(
                    onClick = onInstall,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Gold),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    Text("⬇ Instalar", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}
