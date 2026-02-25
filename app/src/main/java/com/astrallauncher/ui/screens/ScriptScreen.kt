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

private val EXAMPLE_SCRIPT = """-- Astral Lua Bridge Script
-- speed(2.5)     noclip_on()    godmode()
-- reveal()       vision(10)     tasks()
-- kill_cooldown_on()            tp(0, 0)

speed(2.5)
reveal()
""".trimIndent()

@Composable
fun ScriptScreen(vm: MainViewModel) {
    var code   by remember { mutableStateOf(TextFieldValue(EXAMPLE_SCRIPT)) }
    var output by remember { mutableStateOf<String?>(null) }
    var tab    by remember { mutableIntStateOf(0) }
    val ctx    = LocalContext.current

    Column(Modifier.fillMaxSize().background(AL.Bg)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(AL.PurpleBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Code, null, tint = AL.Purple, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(10.dp))
            Text("Script Editor", color = AL.White, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, modifier = Modifier.weight(1f))
            StatusChip("Lua", AL.Purple)
        }

        Row(
            Modifier.padding(horizontal = 16.dp).clip(RoundedCornerShape(10.dp)).background(AL.Surface)
        ) {
            listOf("Editor", "Console", "Referência").forEachIndexed { i, label ->
                Box(
                    Modifier.weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (tab == i) AL.PurpleBg else Color.Transparent)
                        .clickable { tab = i }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        label,
                        color      = if (tab == i) AL.Purple else AL.Muted,
                        fontSize   = 12.sp,
                        fontWeight = if (tab == i) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))

        when (tab) {
            0 -> EditorTab(
                code, { code = it },
                onRun   = { val r = vm.runBridgeScript(code.text); output = r },
                onClear = { code = TextFieldValue(""); output = null },
                onPaste = {
                    val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    code = TextFieldValue(cm.primaryClip?.getItemAt(0)?.text?.toString() ?: "")
                },
                output
            )
            1 -> ConsoleTab(output) { output = null }
            2 -> ReferenceTab()
        }
    }
}

@Composable
fun EditorTab(
    code: TextFieldValue, onChange: (TextFieldValue) -> Unit,
    onRun: () -> Unit, onClear: () -> Unit, onPaste: () -> Unit, output: String?
) {
    Column(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .weight(if (output != null) 0.55f else 0.72f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF0D1117))
                .border(BorderStroke(1.dp, AL.Purple.copy(0.25f)), RoundedCornerShape(14.dp))
        ) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier.fillMaxWidth().background(Color(0xFF161B22)).padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(AL.Error, AL.Warning, AL.Success).forEach { c ->
                        Box(Modifier.size(10.dp).clip(CircleShape).background(c))
                        Spacer(Modifier.width(5.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "bridge_script.lua",
                        color      = AL.Muted,
                        fontSize   = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier   = Modifier.weight(1f)
                    )
                    Text("via TCP :7777", color = AL.Purple, fontSize = 10.sp)
                }
                Row(Modifier.fillMaxSize()) {
                    val lines = code.text.lines().size
                    LazyColumn(
                        Modifier.width(36.dp).fillMaxHeight().padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        items(lines) { i ->
                            Text(
                                "${i + 1}",
                                color      = AL.Muted.copy(0.4f),
                                fontSize   = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier   = Modifier.padding(end = 8.dp)
                            )
                        }
                    }
                    Box(Modifier.fillMaxSize().padding(12.dp)) {
                        BasicTextField(
                            value         = code,
                            onValueChange = onChange,
                            modifier      = Modifier.fillMaxSize(),
                            textStyle     = TextStyle(
                                color      = Color(0xFFE6EDF3),
                                fontFamily = FontFamily.Monospace,
                                fontSize   = 13.sp,
                                lineHeight = 20.sp
                            ),
                            cursorBrush = SolidColor(AL.Purple)
                        )
                        if (code.text.isEmpty()) {
                            Text(
                                "-- Escreva comandos aqui...",
                                color      = AL.Muted.copy(0.4f),
                                fontFamily = FontFamily.Monospace,
                                fontSize   = 13.sp
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onRun,
                colors  = ButtonDefaults.buttonColors(containerColor = AL.Purple),
                shape   = RoundedCornerShape(10.dp),
                modifier = Modifier.height(40.dp)
            ) {
                Icon(Icons.Outlined.PlayArrow, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Run", fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
                onClick  = onPaste,
                border   = BorderStroke(1.dp, AL.Border),
                colors   = ButtonDefaults.outlinedButtonColors(contentColor = AL.MutedL),
                shape    = RoundedCornerShape(10.dp),
                modifier = Modifier.height(40.dp)
            ) {
                Icon(Icons.Outlined.ContentPaste, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Paste")
            }
            OutlinedButton(
                onClick  = onClear,
                border   = BorderStroke(1.dp, AL.Error.copy(0.4f)),
                colors   = ButtonDefaults.outlinedButtonColors(contentColor = AL.Error),
                shape    = RoundedCornerShape(10.dp),
                modifier = Modifier.height(40.dp)
            ) {
                Icon(Icons.Outlined.Clear, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Clear")
            }
        }
        output?.let { out ->
            Spacer(Modifier.height(10.dp))
            Column(
                Modifier
                    .weight(0.3f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF0D1117))
                    .border(BorderStroke(1.dp, AL.Success.copy(0.25f)), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
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
            if (output != null) {
                TextButton(onClick = onClear) { Text("Limpar", color = AL.Muted, fontSize = 12.sp) }
            }
        }
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF0D1117))
                .border(BorderStroke(1.dp, AL.Border), RoundedCornerShape(14.dp))
                .padding(14.dp)
        ) {
            if (output.isNullOrEmpty()) {
                Column(
                    Modifier.fillMaxSize(),
                    verticalArrangement  = Arrangement.Center,
                    horizontalAlignment  = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Outlined.Terminal, null, tint = AL.Muted.copy(0.3f), modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("Sem output", color = AL.Muted.copy(0.4f), fontSize = 14.sp)
                }
            } else {
                Text(output, color = Color(0xFF7EE787), fontFamily = FontFamily.Monospace, fontSize = 12.sp, lineHeight = 18.sp)
            }
        }
    }
}

@Composable
fun ReferenceTab() {
    val cmds = listOf(
        "speed(2.5)"          to "Velocidade x2.5",
        "speed_off()"         to "Restaura velocidade",
        "noclip_on()"         to "NoClip ativado",
        "noclip_off()"        to "NoClip desativado",
        "godmode()"           to "Imortalidade ON",
        "kill_cooldown_on()"  to "Kill CD = 0",
        "kill_cooldown_off()" to "Kill CD restaurado",
        "reveal()"            to "ESP impostores (vermelho)",
        "vision(10)"          to "Visão x10",
        "vision_off()"        to "Visão restaurada",
        "tasks()"             to "Completar todas as tasks",
        "tp(x, y)"            to "Teleportar para X, Y"
    )
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
        item {
            Text(
                "Comandos disponíveis",
                color      = AL.White,
                fontWeight = FontWeight.Bold,
                fontSize   = 16.sp,
                modifier   = Modifier.padding(vertical = 10.dp)
            )
            Text(
                "Requer AstralBridge.dll instalado no AU patcheado.",
                color    = AL.Muted,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 10.dp)
            )
        }
        items(cmds) { (cmd, desc) ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(AL.BgCard)
                    .border(BorderStroke(0.5.dp, AL.Border), RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                Text(cmd, color = Color(0xFF79C0FF), fontFamily = FontFamily.Monospace, fontSize = 12.sp, modifier = Modifier.weight(1f))
                Text(desc, color = AL.Muted, fontSize = 12.sp)
            }
        }
    }
}
