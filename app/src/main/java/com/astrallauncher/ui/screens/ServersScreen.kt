package com.astrallauncher.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.*
import com.astrallauncher.MainViewModel
import com.astrallauncher.model.CustomServer
import com.astrallauncher.ui.AL
import com.astrallauncher.ui.components.*
import java.util.UUID

@Composable
fun ServersScreen(vm: MainViewModel) {
    val servers by vm.servers.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    if (showAdd) { AddServerSheet(onAdd = { vm.addServer(it); showAdd = false }, onDismiss = { showAdd = false }); return }

    LazyColumn(Modifier.fillMaxSize().background(AL.Bg), contentPadding = PaddingValues(bottom = 100.dp)) {
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Servers", color = AL.White, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, modifier = Modifier.weight(1f))
                GoldButton("+ Add", onClick = { showAdd = true })
            }
        }
        item { AstralCard(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Info, null, tint = AL.Info, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(8.dp))
                Text("Custom servers are injected into Among Us via the patched APK. Requires mod support.", color = AL.Muted, fontSize = 12.sp, lineHeight = 18.sp)
            }
        } }
        if (servers.isEmpty()) item { EmptyState(Icons.Outlined.Dns, "No custom servers", "Add a private or modded Among Us server", action = { GoldButton("Add Server", { showAdd = true }, icon = Icons.Outlined.Add) }) }
        else {
            item { SectionHeader("Custom Servers", action = "${servers.size}") }
            items(servers) { ServerCard(it) { vm.deleteServer(it.id) } }
        }
        item { SectionHeader("Default Servers") }
        item { listOf("North America" to "na.among.us", "Europe" to "eu.among.us", "Asia" to "as.among.us").forEach { (n, ip) ->
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 3.dp).clip(RoundedCornerShape(14.dp)).background(AL.BgCard).border(BorderStroke(0.5.dp, AL.Border), RoundedCornerShape(14.dp)).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(AL.Surface), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Language, null, tint = AL.Info, modifier = Modifier.size(20.dp)) }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) { Text(n, color = AL.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp); Text("$ip:22023", color = AL.Muted, fontSize = 12.sp) }
                StatusChip("Official", AL.Success)
            }
        } }
    }
}

@Composable fun ServerCard(s: CustomServer, onDelete: () -> Unit) {
    var confirm by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 3.dp).clip(RoundedCornerShape(14.dp)).background(AL.BgCard).border(BorderStroke(0.5.dp, AL.Border), RoundedCornerShape(14.dp)).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(42.dp).clip(RoundedCornerShape(10.dp)).background(AL.GoldBg), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Dns, null, tint = AL.Gold, modifier = Modifier.size(22.dp)) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(s.name, color = AL.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text("${s.ip}:${s.port}", color = AL.Muted, fontSize = 12.sp)
            if (s.description.isNotEmpty()) Text(s.description, color = AL.Muted, fontSize = 11.sp, maxLines = 1)
        }
        StatusChip(s.region, AL.Gold); Spacer(Modifier.width(6.dp))
        IconButton(onClick = { confirm = true }, modifier = Modifier.size(32.dp)) { Icon(Icons.Outlined.DeleteOutline, null, tint = AL.Error.copy(0.7f), modifier = Modifier.size(18.dp)) }
    }
    if (confirm) AlertDialog(onDismissRequest = { confirm = false }, containerColor = AL.Surface,
        title = { Text("Remove?", color = AL.White) }, text = { Text("Delete '${s.name}'?", color = AL.Muted) },
        confirmButton = { TextButton(onClick = { onDelete(); confirm = false }) { Text("Remove", color = AL.Error) } },
        dismissButton = { TextButton(onClick = { confirm = false }) { Text("Cancel", color = AL.Muted) } })
}

@Composable
private fun Field(label: String, value: String, onChange: (String) -> Unit, kb: KeyboardType = KeyboardType.Text, hint: String = "") {
    Text(label, color = AL.MutedL, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    Spacer(Modifier.height(6.dp))
    OutlinedTextField(value = value, onValueChange = onChange, modifier = Modifier.fillMaxWidth(),
        placeholder = { if (hint.isNotEmpty()) Text(hint, color = AL.Muted) },
        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AL.Gold, unfocusedBorderColor = AL.Border, focusedTextColor = AL.White, unfocusedTextColor = AL.White, cursorColor = AL.Gold),
        shape = RoundedCornerShape(12.dp), keyboardOptions = KeyboardOptions(keyboardType = kb), singleLine = true)
    Spacer(Modifier.height(14.dp))
}

@Composable fun AddServerSheet(onAdd: (CustomServer) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }; var ip by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("22023") }; var region by remember { mutableStateOf("Custom") }
    var desc by remember { mutableStateOf("") }; var err by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize().background(AL.Bg).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onDismiss) { Icon(Icons.Outlined.ArrowBack, null, tint = AL.White) }
            Spacer(Modifier.width(8.dp)); Text("Add Server", color = AL.White, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
        }
        Spacer(Modifier.height(20.dp))
        Field("Server Name *", name, { name = it }, hint = "My Modded Server")
        Field("IP / Hostname *", ip, { ip = it }, hint = "192.168.0.1")
        Field("Port", port, { port = it }, KeyboardType.Number, hint = "22023")
        Field("Region Label", region, { region = it }, hint = "Custom")
        Field("Description", desc, { desc = it }, hint = "Optional")
        err?.let { Text(it, color = AL.Error, fontSize = 13.sp); Spacer(Modifier.height(8.dp)) }
        Spacer(Modifier.weight(1f))
        GoldButton("Add Server", onClick = {
            when { name.isBlank() -> err = "Name required"; ip.isBlank() -> err = "IP required"
                port.toIntOrNull() == null -> err = "Invalid port"
                else -> onAdd(CustomServer(UUID.randomUUID().toString(), name.trim(), ip.trim(), port.toInt(), region.trim(), desc.trim())) }
        }, modifier = Modifier.fillMaxWidth(), icon = Icons.Outlined.Add)
        Spacer(Modifier.height(8.dp)); GhostButton("Cancel", onDismiss, Modifier.fillMaxWidth())
    }
}
