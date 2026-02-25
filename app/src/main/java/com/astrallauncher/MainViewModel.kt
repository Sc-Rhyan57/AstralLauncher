package com.astrallauncher

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.astrallauncher.model.*
import com.astrallauncher.network.ModRepositoryApi
import com.astrallauncher.service.OverlayService
import com.astrallauncher.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val ctx: Context get() = getApplication()
    private val TAG = "VM"

    val auInstalled = MutableStateFlow(false)
    val auVersion = MutableStateFlow("?")
    val patchedInstalled = MutableStateFlow(false)
    val patchedVersion = MutableStateFlow("?")
    val mods = MutableStateFlow<List<ModEntry>>(emptyList())
    val modsLoading = MutableStateFlow(false)
    val modsError = MutableStateFlow<String?>(null)
    val installedMods = MutableStateFlow<List<InstalledMod>>(emptyList())
    val servers = MutableStateFlow<List<CustomServer>>(emptyList())
    val downloadProgress = MutableStateFlow<Map<String, Int>>(emptyMap())
    val statusMsg = MutableStateFlow<String?>(null)
    val overlayActive = MutableStateFlow(false)
    val patchState = MutableStateFlow<PatchState>(PatchState.Idle)

    sealed class PatchState {
        object Idle : PatchState()
        data class Progress(val step: String, val pct: Int) : PatchState()
        data class Done(val apk: File) : PatchState()
        data class Err(val msg: String) : PatchState()
    }

    init {
        AppLogger.init(ctx)
        checkGame()
        fetchMods()
        viewModelScope.launch { Prefs.getInstalledMods(ctx).collect { installedMods.value = it } }
        viewModelScope.launch { Prefs.getServers(ctx).collect { servers.value = it } }
    }

    fun checkGame() {
        auInstalled.value = GameHelper.isAuInstalled(ctx)
        auVersion.value = GameHelper.getAuVersion(ctx)
        patchedInstalled.value = GameHelper.isPatchedInstalled(ctx)
        patchedVersion.value = GameHelper.getPatchedVersion(ctx)
        AppLogger.i(TAG, "AU=${auInstalled.value} v=${auVersion.value} patched=${patchedInstalled.value} pv=${patchedVersion.value}")
    }

    fun fetchMods(url: String = ModRepositoryApi.REPO_URL) {
        viewModelScope.launch {
            modsLoading.value = true; modsError.value = null
            val result = withContext(Dispatchers.IO) { ModRepositoryApi.fetchMods(url) }
            result.onSuccess { mods.value = it.mods }
                  .onFailure { modsError.value = it.message }
            modsLoading.value = false
        }
    }

    fun downloadAndInstall(mod: ModEntry) {
        viewModelScope.launch {
            val release = mod.releases.firstOrNull() ?: return@launch
            val ext = when (release.format) {
                ModFormat.AMOD -> ".amod"; ModFormat.LUA -> ".lua"
                ModFormat.ZIP -> ".zip"; else -> ".dll"
            }
            val dest = File(ctx.filesDir, "mods/${mod.id}$ext")
            dest.parentFile?.mkdirs()

            val result = withContext(Dispatchers.IO) {
                ModRepositoryApi.downloadFile(release.url, dest.absolutePath) { p ->
                    downloadProgress.value = downloadProgress.value + (mod.id to p)
                }
            }
            downloadProgress.value = downloadProgress.value - mod.id

            result.onSuccess {
                val file = when (release.format) {
                    ModFormat.AMOD, ModFormat.ZIP -> {
                        val ext2 = ApkPatcher.extractZip(dest, File(ctx.filesDir, "mods/${mod.id}_ext"))
                        ext2.firstOrNull { it.extension == "dll" } ?: ext2.firstOrNull() ?: dest
                    }
                    else -> dest
                }
                saveMod(mod, file, release.format)
            }.onFailure {
                statusMsg.value = "Download falhou: ${it.message}"
            }
        }
    }

    private suspend fun saveMod(mod: ModEntry, file: File, format: ModFormat) {
        val list = installedMods.value.toMutableList()
        list.removeAll { it.id == mod.id }
        list.add(InstalledMod(mod.id, mod.name, mod.author, mod.version, format = format, filePath = file.absolutePath))
        Prefs.saveInstalledMods(ctx, list)
        statusMsg.value = "${mod.name} instalado — faça o Patch para aplicar"
    }

    fun patchAndInstall() {
        val dlls = installedMods.value
            .filter { it.enabled && it.format == ModFormat.DLL }
            .map { File(it.filePath) }
            .filter { it.exists() }

        patchState.value = PatchState.Progress("Preparando...", 0)
        ApkPatcher.patch(ctx, dlls, object : ApkPatcher.Callback {
            override fun onProgress(step: String, pct: Int) {
                viewModelScope.launch { patchState.value = PatchState.Progress(step, pct) }
            }
            override fun onSuccess(apk: File) {
                viewModelScope.launch {
                    patchState.value = PatchState.Done(apk)
                    GameHelper.installApk(ctx, apk)
                }
            }
            override fun onError(msg: String) {
                viewModelScope.launch { patchState.value = PatchState.Err(msg) }
            }
        })
    }

    fun resetPatch() { patchState.value = PatchState.Idle }

    fun installExternalApk(ctx: Context, uri: Uri) {
        viewModelScope.launch {
            statusMsg.value = "Preparando APK..."
            try {
                val stream = ctx.contentResolver.openInputStream(uri) ?: return@launch
                val dest = File(ctx.cacheDir, "ext_${System.currentTimeMillis()}.apk")
                withContext(Dispatchers.IO) { FileOutputStream(dest).use { stream.copyTo(it) } }
                GameHelper.installApk(ctx, dest)
                statusMsg.value = "Siga o prompt de instalação"
            } catch (e: Exception) {
                statusMsg.value = "Falha: ${e.message}"
            }
        }
    }

    fun launchGame() {
        AppLogger.i(TAG, "Launching game. patched=${patchedInstalled.value}")
        GameHelper.launchPatched(ctx)
        if (hasOverlayPermission()) startOverlay()
    }

    fun hasOverlayPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(ctx)

    fun requestOverlayPermission(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        )
    }

    fun startOverlay() {
        val active = installedMods.value.firstOrNull { it.enabled }
        val intent = Intent(ctx, OverlayService::class.java).apply {
            putExtra(OverlayService.EXTRA_MOD, active?.name ?: "Vanilla")
            putExtra(OverlayService.EXTRA_AU_VER, auVersion.value)
            putExtra(OverlayService.EXTRA_MODS_COUNT, installedMods.value.count { it.enabled })
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ctx.startForegroundService(intent)
        else ctx.startService(intent)
        overlayActive.value = true
    }

    fun stopOverlay() {
        ctx.stopService(Intent(ctx, OverlayService::class.java))
        overlayActive.value = false
    }

    fun addServer(s: CustomServer) = viewModelScope.launch {
        val list = servers.value.toMutableList()
        list.removeAll { it.id == s.id }; list.add(s)
        Prefs.saveServers(ctx, list)
    }

    fun deleteServer(id: String) = viewModelScope.launch {
        Prefs.saveServers(ctx, servers.value.filter { it.id != id })
    }

    fun toggleMod(id: String) = viewModelScope.launch {
        Prefs.saveInstalledMods(ctx, installedMods.value.map { if (it.id == id) it.copy(enabled = !it.enabled) else it })
    }

    fun deleteMod(id: String) = viewModelScope.launch {
        installedMods.value.find { it.id == id }?.filePath?.let { File(it).delete() }
        Prefs.saveInstalledMods(ctx, installedMods.value.filter { it.id != id })
    }

    fun runBridgeScript(script: String): String {
        val svc = com.astrallauncher.service.OverlayService.instance ?: return "Overlay não ativo — inicie o jogo e ative o overlay."
        return try {
            val field = svc.javaClass.getDeclaredField("bridge")
            field.isAccessible = true
            val client = field.get(svc) as com.astrallauncher.bridge.AstralBridgeClient
            client.executeScript(script)
        } catch (e: Exception) {
            "Erro: ${e.message}"
        }
    }

    fun clearStatus() { statusMsg.value = null }
}
