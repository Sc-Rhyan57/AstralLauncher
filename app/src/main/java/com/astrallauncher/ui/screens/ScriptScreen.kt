package com.astrallauncher.ui.screens

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.*
import com.astrallauncher.MainViewModel
import com.astrallauncher.ui.AL
import com.astrallauncher.ui.components.*

private val EXAMPLE = """
-- Astral Lua API Example
local p = AU.LocalPlayer
function onStart()
    p.speed = 2.0
    print("Mod loaded for: " .. p.name)
end
AU.on("gameStart", onStart)
""".trimIndent()

@Composable
fun ScriptScreen(vm: MainViewModel) {
    val output by vm.scriptOutput.collectAsState()
    var code by remember { mutableStateOf(TextFieldValue(EXAMPLE)) }
    var tab by remember { mutableIntStateOf(0) }
    val ctx = LocalContext.current

    Column(Modifier.fillMaxSize().background(AL.Bg)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(AL.PurpleBg), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Code, null, tint = AL.Purple, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(10.dp))
            Text("Script Editor", color = AL.White, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, modifier = Modifier.weight(1f))
            StatusChip("Lua 5.4", AL.Purple)
        }

        Row(Modifier.padding(horizontal = 16.dp).clip(RoundedCornerShape(10.dp)).background(AL.Surface)) {
            listOf("Editor", "Console", "DLL Drop").forEachIndexed { i, label ->
                Box(Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
                    .background(if (tab == i) AL.PurpleBg else Color.Transparent)
                    .clickable { tab = i }.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                    Text(label, color = if (tab == i) AL.Purple else AL.Muted, fontSize = 12.sp, fontWeight = if (tab == i) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }
        Spacer(Modifier.height(12.dp))

        when (tab) {
            0 -> EditorTab(code, { code = it }, { vm.executeScript(code.text) }, { code = TextFieldValue(""); vm.clearScript() }, {
                val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                code = TextFieldValue(cm.primaryClip?.getItemAt(0)?.text?.toString() ?: "")
            }, output)
            1 -> ConsoleTab(output) { vm.clearScript() }
            2 -> DllDropTab()
        }
    }
}

@Composable
fun EditorTab(code: TextFieldValue, onChange: (TextFieldValue) -> Unit, onRun: () -> Unit, onClear: () -> Unit, onPaste: () -> Unit, output: String?) {
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(if (output != null) 0.58f else 0.72f).fillMaxWidth().padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(14.dp)).background(Color(0xFF0D1117)).border(BorderStroke(1.dp, AL.Purple.copy(0.25f)), RoundedCornerShape(14.dp))) {
            Column(Modifier.fillMaxSize()) {
                Row(Modifier.fillMaxWidth().background(Color(0xFF161B22)).padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    listOf(AL.Error, AL.Warning, AL.Success).forEach { c ->
                        Box(Modifier.size(10.dp).clip(CircleShape).background(c)); Spacer(Modifier.width(5.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Text("script.lua", color = AL.Muted, fontSize = 12.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
                    Text("Lua 5.4", color = AL.Purple, fontSize = 10.sp)
                }
                Row(Modifier.fillMaxSize()) {
                    val lines = code.text.lines().size
                    LazyColumn(Modifier.width(36.dp).fillMaxHeight().padding(vertical = 12.dp), horizontalAlignment = Alignment.End) {
                        items(lines) { i -> Text("${i + 1}", color = AL.Muted.copy(0.4f), fontSize = 12.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(end = 8.dp)) }
                    }
                    Box(Modifier.fillMaxSize().padding(12.dp)) {
                        BasicTextField(value = code, onValueChange = onChange, modifier = Modifier.fillMaxSize(),
                            textStyle = TextStyle(color = Color(0xFFE6EDF3), fontFamily = FontFamily.Monospace, fontSize = 13.sp, lineHeight = 20.sp),
                            cursorBrush = SolidColor(AL.Purple))
                        if (code.text.isEmpty()) Text("-- Write Lua here...", color = AL.Muted.copy(0.4f), fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onRun, colors = ButtonDefaults.buttonColors(containerColor = AL.Purple), shape = RoundedCornerShape(10.dp), modifier = Modifier.height(40.dp)) {
                Icon(Icons.Outlined.PlayArrow, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Run", fontWeight = FontWeight.Bold)
            }
            OutlinedButton(onClick = onPaste, border = BorderStroke(1.dp, AL.Border), colors = ButtonDefaults.outlinedButtonColors(contentColor = AL.MutedL), shape = RoundedCornerShape(10.dp), modifier = Modifier.height(40.dp)) {
                Icon(Icons.Outlined.ContentPaste, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Paste")
            }
            OutlinedButton(onClick = onClear, border = BorderStroke(1.dp, AL.Error.copy(0.4f)), colors = ButtonDefaults.outlinedButtonColors(contentColor = AL.Error), shape = RoundedCornerShape(10.dp), modifier = Modifier.height(40.dp)) {
                Icon(Icons.Outlined.Clear, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Clear")
            }
        }
        output?.let { out ->
            Spacer(Modifier.height(10.dp))
            Column(Modifier.weight(0.28f).fillMaxWidth().padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(12.dp)).background(Color(0xFF0D1117))
                .border(BorderStroke(1.dp, AL.Success.copy(0.25f)), RoundedCornerShape(12.dp)).padding(12.dp)) {
                Text("OUTPUT", color = AL.Success, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(Modifier.height(6.dp))
                Text(out, color = Color(0xFF7EE787), fontFamily = FontFamily.Monospace, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
fun ConsoleTab(output: String?, onClear: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Console", color = AL.White, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.weight(1f))
            if (output != null) TextButton(onClick = onClear) { Text("Clear", color = AL.Muted, fontSize = 12.sp) }
        }
        Spacer(Modifier.height(8.dp))
        Box(Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp)).background(Color(0xFF0D1117)).border(BorderStroke(1.dp, AL.Border), RoundedCornerShape(14.dp)).padding(14.dp)) {
            if (output.isNullOrEmpty()) Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Outlined.Computer, null, tint = AL.Muted.copy(0.3f), modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(12.dp)); Text("No output", color = AL.Muted.copy(0.4f), fontSize = 14.sp)
            } else Text(output, color = Color(0xFF7EE787), fontFamily = FontFamily.Monospace, fontSize = 12.sp, lineHeight = 18.sp)
        }
    }
}

@Composable
fun DllDropTab() {
    Column(Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(Modifier.size(80.dp).clip(RoundedCornerShape(20.dp))
            .background(AL.GoldBg).border(BorderStroke(1.dp, AL.GoldDark.copy(0.5f)), RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center) { Icon(Icons.Outlined.ArrowUpward, null, tint = AL.Gold, modifier = Modifier.size(40.dp)) }
        Spacer(Modifier.height(20.dp))
        Text("DLL Injector", color = AL.White, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
        Spacer(Modifier.height(10.dp))
        Text("Select a .dll (BepInEx plugin) to add it to the mod list. It will be injected into Among Us when you Patch from the Home screen.", color = AL.Muted, fontSize = 14.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.padding(horizontal = 20.dp))
        Spacer(Modifier.height(24.dp))
        GoldButton("Select .dll File", onClick = {}, icon = Icons.Outlined.FolderOpen, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(16.dp))
        AstralCard(modifier = Modifier.fillMaxWidth()) {
            Text("Supported injection formats", color = AL.MutedL, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
            listOf("BepInEx .dll plugin" to AL.Info, "MiraAPI .zip bundle" to AL.Purple, "Astral .amod package" to AL.Gold, "Lua script .lua" to AL.Success)
                .forEach { (t, c) -> Row(Modifier.padding(vertical = 2.dp)) { Text("✦ ", color = c, fontSize = 12.sp); Text(t, color = AL.Muted, fontSize = 13.sp) } }
        }
    }
}
