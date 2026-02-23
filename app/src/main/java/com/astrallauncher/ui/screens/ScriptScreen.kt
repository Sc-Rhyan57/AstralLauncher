package com.astrallauncher.ui.screens

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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

@Composable
fun ScriptScreen(vm: MainViewModel) {
    val scriptOutput by vm.scriptOutput.collectAsState()
    var code by remember { mutableStateOf(TextFieldValue(EXAMPLE_LUA_SCRIPT)) }
    var activeTab by remember { mutableIntStateOf(0) }
    val ctx = LocalContext.current

    Column(modifier = Modifier.fillMaxSize().background(AL.Bg)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(AL.PurpleBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Code, null, tint = AL.Purple, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(10.dp))
            Text("Script Editor", color = AL.White, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, modifier = Modifier.weight(1f))
            StatusChip("LIVE", AL.Purple)
        }

        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(AL.Surface)
        ) {
            listOf("Lua Script", "DLL Injector", "Console").forEachIndexed { idx, label ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (activeTab == idx) AL.PurpleBg else Color.Transparent)
                        .clickable { activeTab = idx }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        label,
                        color = if (activeTab == idx) AL.Purple else AL.Muted,
                        fontSize = 12.sp,
                        fontWeight = if (activeTab == idx) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        when (activeTab) {
            0 -> LuaEditorTab(
                code = code,
                onCodeChange = { code = it },
                onExecute = { vm.executeScript(code.text) },
                onClear = { code = TextFieldValue(""); vm.clearScriptOutput() },
                onPasteClipboard = {
                    val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = cm.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
                    code = TextFieldValue(clip)
                },
                output = scriptOutput
            )
            1 -> DllInjectorTab()
            2 -> ConsoleTab(output = scriptOutput, onClear = { vm.clearScriptOutput() })
        }
    }
}

@Composable
fun LuaEditorTab(
    code: TextFieldValue,
    onCodeChange: (TextFieldValue) -> Unit,
    onExecute: () -> Unit,
    onClear: () -> Unit,
    onPasteClipboard: () -> Unit,
    output: String?
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .weight(if (output != null) 0.55f else 0.7f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF0D1117))
                .border(BorderStroke(1.dp, AL.Purple.copy(0.3f)), RoundedCornerShape(14.dp))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF161B22))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(10.dp).clip(androidx.compose.foundation.shape.CircleShape).background(AL.Error))
                    Spacer(Modifier.width(6.dp))
                    Box(modifier = Modifier.size(10.dp).clip(androidx.compose.foundation.shape.CircleShape).background(AL.Warning))
                    Spacer(Modifier.width(6.dp))
                    Box(modifier = Modifier.size(10.dp).clip(androidx.compose.foundation.shape.CircleShape).background(AL.Success))
                    Spacer(Modifier.width(12.dp))
                    Text("script.lua", color = AL.Muted, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    Spacer(Modifier.weight(1f))
                    Text("Lua 5.4", color = AL.Purple, fontSize = 10.sp)
                }

                Row(modifier = Modifier.fillMaxSize()) {
                    val lineCount = code.text.lines().count()
                    LazyColumn(
                        modifier = Modifier
                            .width(36.dp)
                            .fillMaxHeight()
                            .background(Color(0xFF0D1117))
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        items(lineCount) { i ->
                            Text(
                                "${i + 1}",
                                color = AL.Muted.copy(0.5f),
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                        }
                    }

                    Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                        BasicTextField(
                            value = code,
                            onValueChange = onCodeChange,
                            modifier = Modifier.fillMaxSize(),
                            textStyle = TextStyle(
                                color = Color(0xFFE6EDF3),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                lineHeight = 20.sp
                            ),
                            cursorBrush = SolidColor(AL.Purple)
                        )
                        if (code.text.isEmpty()) {
                            Text(
                                "-- Write your Lua script here...\n-- Among Us objects are available via the AU global",
                                color = AL.Muted.copy(0.5f),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        Row(modifier = Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onExecute,
                colors = ButtonDefaults.buttonColors(containerColor = AL.Purple),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.height(40.dp)
            ) {
                Icon(Icons.Outlined.PlayArrow, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Execute", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            OutlinedButton(
                onClick = onPasteClipboard,
                border = BorderStroke(1.dp, AL.Border),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AL.MutedLight),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.height(40.dp)
            ) {
                Icon(Icons.Outlined.ContentPaste, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Paste", fontSize = 13.sp)
            }
            OutlinedButton(
                onClick = onClear,
                border = BorderStroke(1.dp, AL.Error.copy(0.4f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AL.Error),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.height(40.dp)
            ) {
                Icon(Icons.Outlined.Clear, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Clear", fontSize = 13.sp)
            }
        }

        if (output != null) {
            Spacer(Modifier.height(10.dp))
            Column(
                modifier = Modifier
                    .weight(0.3f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF0D1117))
                    .border(BorderStroke(1.dp, AL.Success.copy(0.3f)), RoundedCornerShape(14.dp))
                    .padding(12.dp)
            ) {
                Text("Output", color = AL.Success, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text(output, color = Color(0xFF7EE787), fontFamily = FontFamily.Monospace, fontSize = 12.sp)
            }
        }

        Spacer(Modifier.height(12.dp))
    }
}

@Composable
fun DllInjectorTab() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Outlined.FileUpload, null, tint = AL.Gold.copy(0.5f), modifier = Modifier.size(64.dp))
        Spacer(Modifier.height(16.dp))
        Text("DLL Injector", color = AL.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(Modifier.height(8.dp))
        Text(
            "Drop a .dll file here to inject it directly into Among Us. This will repackage the APK with your plugin and prompt for installation.",
            color = AL.Muted,
            fontSize = 14.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        GoldButton("Select DLL File", onClick = { }, icon = Icons.Outlined.FolderOpen)
        Spacer(Modifier.height(12.dp))
        AstralCard(modifier = Modifier.fillMaxWidth()) {
            Text("Supported formats", color = AL.MutedLight, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
            listOf("BepInEx .dll plugin", "MiraAPI mod (.zip)", "Astral .amod package", "Lua script (.lua)").forEach { fmt ->
                Row(modifier = Modifier.padding(vertical = 2.dp)) {
                    Text("✦ ", color = AL.Gold, fontSize = 13.sp)
                    Text(fmt, color = AL.Muted, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
fun ConsoleTab(output: String?, onClear: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Console Output", color = AL.White, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.weight(1f))
            if (output != null) {
                TextButton(onClick = onClear) {
                    Text("Clear", color = AL.Muted, fontSize = 12.sp)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF0D1117))
                .border(BorderStroke(1.dp, AL.Border), RoundedCornerShape(14.dp))
                .padding(14.dp)
        ) {
            if (output.isNullOrEmpty()) {
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.Terminal, null, tint = AL.Muted.copy(0.4f), modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("No output yet", color = AL.Muted.copy(0.5f), fontSize = 14.sp)
                    Text("Execute a script to see output here", color = AL.Muted.copy(0.4f), fontSize = 12.sp)
                }
            } else {
                Text(output, color = Color(0xFF7EE787), fontFamily = FontFamily.Monospace, fontSize = 12.sp, lineHeight = 18.sp)
            }
        }
    }
}

private val EXAMPLE_LUA_SCRIPT = """
-- Astral Launcher - Lua Mod Example
-- This script runs in the Among Us game context

local player = AU.LocalPlayer

function onGameStart()
    print("Game started! Player: " .. player.name)
    player.speed = 2.0
end

function onKill(target)
    print("Killed: " .. target.name)
end

-- Register event listeners
AU.on("gameStart", onGameStart)
AU.on("kill", onKill)
""".trimIndent()
