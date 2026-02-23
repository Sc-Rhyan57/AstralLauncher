package com.astrallauncher.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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

    if (showAdd) {
        AddServerSheet(onAdd = { server -> vm.addServer(server); showAdd = false }, onDismiss = { showAdd = false })
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(AL.Bg),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Servers", color = AL.White, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, modifier = Modifier.weight(1f))
                GoldButton("+ Add Server", onClick = { showAdd = true })
            }
        }

        item {
            AstralCard(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Info, null, tint = AL.Info, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Custom servers are added directly to Among Us's server list. Make sure your mod supports the custom server's region.",
                        color = AL.Muted,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        if (servers.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Outlined.Dns,
                    title = "No Custom Servers",
                    subtitle = "Add a custom Among Us server to connect to private lobbies and modded servers",
                    action = { GoldButton("Add Server", onClick = { showAdd = true }, icon = Icons.Outlined.Add) }
                )
            }
        } else {
            item { SectionHeader("Custom Servers", action = "${servers.size}") }
            items(servers) { server ->
                ServerRow(server = server, onDelete = { vm.deleteServer(server.id) })
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
        item {
            SectionHeader("Default Servers")
        }
        item {
            val defaults = listOf(
                Triple("North America", "na.among.us", 22023),
                Triple("Europe", "eu.among.us", 22023),
                Triple("Asia", "as.among.us", 22023),
            )
            defaults.forEach { (name, ip, port) ->
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
                        modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(AL.Surface),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.Language, null, tint = AL.Info, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(name, color = AL.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text("$ip:$port", color = AL.Muted, fontSize = 12.sp)
                    }
                    StatusChip("Default", AL.Success)
                }
            }
        }
    }
}

@Composable
fun ServerRow(server: CustomServer, onDelete: () -> Unit) {
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
            modifier = Modifier.size(42.dp).clip(RoundedCornerShape(10.dp)).background(AL.GoldBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.Dns, null, tint = AL.Gold, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(server.name, color = AL.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text("${server.ip}:${server.port}", color = AL.Muted, fontSize = 12.sp)
            if (server.description.isNotEmpty()) {
                Text(server.description, color = AL.Muted, fontSize = 11.sp, maxLines = 1)
            }
        }
        StatusChip(server.region, AL.Gold)
        Spacer(Modifier.width(8.dp))
        IconButton(onClick = { showConfirm = true }, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Outlined.DeleteOutline, null, tint = AL.Error.copy(0.7f), modifier = Modifier.size(18.dp))
        }
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            containerColor = AL.Surface,
            title = { Text("Remove Server", color = AL.White) },
            text = { Text("Remove '${server.name}'?", color = AL.Muted) },
            confirmButton = {
                TextButton(onClick = { onDelete(); showConfirm = false }) {
                    Text("Remove", color = AL.Error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) { Text("Cancel", color = AL.Muted) }
            }
        )
    }
}

@Composable
fun AddServerSheet(onAdd: (CustomServer) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var ip by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("22023") }
    var region by remember { mutableStateOf("Custom") }
    var description by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.fillMaxSize().background(AL.Bg).padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onDismiss) {
                Icon(Icons.Outlined.ArrowBack, null, tint = AL.White)
            }
            Spacer(Modifier.width(8.dp))
            Text("Add Custom Server", color = AL.White, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
        }
        Spacer(Modifier.height(24.dp))

        fun field(label: String, value: String, onChange: (String) -> Unit, kb: KeyboardType = KeyboardType.Text, hint: String = "") {
            Text(label, color = AL.MutedLight, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = value,
                onValueChange = onChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { if (hint.isNotEmpty()) Text(hint, color = AL.Muted) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AL.Gold,
                    unfocusedBorderColor = AL.Border,
                    focusedTextColor = AL.White,
                    unfocusedTextColor = AL.White,
                    cursorColor = AL.Gold
                ),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(keyboardType = kb),
                singleLine = true
            )
            Spacer(Modifier.height(14.dp))
        }

        field("Server Name *", name, { name = it }, hint = "My Modded Server")
        field("IP Address / Hostname *", ip, { ip = it }, hint = "192.168.0.1 or my.server.com")
        field("Port", port, { port = it }, kb = KeyboardType.Number, hint = "22023")
        field("Region Label", region, { region = it }, hint = "Custom")
        field("Description", description, { description = it }, hint = "Optional description")

        error?.let {
            Text(it, color = AL.Error, fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
        }

        Spacer(Modifier.weight(1f))
        GoldButton(
            text = "Add Server",
            onClick = {
                when {
                    name.isBlank() -> error = "Server name is required"
                    ip.isBlank() -> error = "IP address is required"
                    port.toIntOrNull() == null -> error = "Invalid port number"
                    else -> {
                        onAdd(CustomServer(UUID.randomUUID().toString(), name.trim(), ip.trim(), port.toInt(), region.trim(), description.trim()))
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            icon = Icons.Outlined.Add
        )
        Spacer(Modifier.height(8.dp))
        GhostButton("Cancel", onClick = onDismiss, modifier = Modifier.fillMaxWidth())
    }
}
