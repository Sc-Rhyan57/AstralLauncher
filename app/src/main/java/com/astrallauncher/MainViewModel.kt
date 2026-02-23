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
    private val TAG = "ViewModel"

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
    val scriptOutput = MutableStateFlow<String?>(null)
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
        AppLogger.i(TAG, "ViewModel init")
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
        AppLogger.i(TAG, "AU=${auInstalled.value} ver=${auVersion.value} patched=${patchedInstalled.value}")
    }

    fun fetchMods(url: String = ModRepositoryApi.REPO_URL) {
        viewModelScope.launch {
            modsLoading.value = true; modsError.value = null
            AppLogger.i(TAG, "Fetching mods: $url")
            val result = withContext(Dispatchers.IO) { ModRepositoryApi.fetchMods(url) }
            result.onSuccess { mods.value = it.mods; AppLogger.i(TAG, "Loaded ${it.mods.size} mods") }
                .onFailure { modsError.value = it.message; AppLogger.e(TAG, "Fetch failed: ${it.message}") }
            modsLoading.value = false
        }
    }

    fun downloadAndInstall(mod: ModEntry) {
        viewModelScope.launch {
            val release = mod.releases.firstOrNull() ?: return@launch
            val ext = when (release.format) {
                ModFormat.AMOD -> ".amod"
                ModFormat.LUA -> ".lua"
                ModFormat.ZIP -> ".zip"
                else -> ".dll"
            }
            val dest = File(ctx.filesDir, "mods/${mod.id}$ext")
            dest.parentFile?.mkdirs()
            AppLogger.i(TAG, "Downloading ${mod.name} -> ${dest.absolutePath}")

            val result = withContext(Dispatchers.IO) {
                ModRepositoryApi.downloadFile(release.url, dest.absolutePath) { p ->
                    downloadProgress.value = downloadProgress.value + (mod.id to p)
                }
            }
            downloadProgress.value = downloadProgress.value - mod.id

            result.onSuccess {
                AppLogger.i(TAG, "Download OK: ${mod.name}")
                val file = when (release.format) {
                    ModFormat.AMOD, ModFormat.ZIP -> {
                        val ext2 = ApkPatcher.extractZip(dest, File(ctx.filesDir, "mods/${mod.id}_ext"))
                        ext2.firstOrNull { it.extension == "dll" } ?: ext2.firstOrNull() ?: dest
                    }
                    else -> dest
                }
                saveMod(mod, file, release.format)
            }.onFailure {
                statusMsg.value = "Download failed: ${it.message}"
                AppLogger.e(TAG, "DL fail: ${it.message}")
            }
        }
    }

    private suspend fun saveMod(mod: ModEntry, file: File, format: ModFormat) {
        val list = installedMods.value.toMutableList()
        list.removeAll { it.id == mod.id }
        list.add(InstalledMod(mod.id, mod.name, mod.author, mod.version, format = format, filePath = file.absolutePath))
        Prefs.saveInstalledMods(ctx, list)
        statusMsg.value = "${mod.name} instalado — toque em Patch para aplicar"
        AppLogger.i(TAG, "Mod saved: ${mod.name} at ${file.absolutePath}")
    }

    fun patchAndInstall() {
        val enabledMods = installedMods.value.filter { it.enabled }
        val dlls = enabledMods
            .filter { it.format == ModFormat.DLL }
            .map { File(it.filePath) }
            .filter { it.exists() }

        AppLogger.i(TAG, "Starting patch: ${dlls.size} DLL mod(s), ${enabledMods.size} total enabled")
        patchState.value = PatchState.Progress("Preparando...", 0)

        ApkPatcher.patch(ctx, dlls, object : ApkPatcher.Callback {
            override fun onProgress(step: String, pct: Int) {
                patchState.value = PatchState.Progress(step, pct)
            }
            override fun onSuccess(apk: File) {
                patchState.value = PatchState.Done(apk)
                GameHelper.installApk(ctx, apk)
            }
            override fun onError(msg: String) {
                patchState.value = PatchState.Err(msg)
                AppLogger.e(TAG, "Patch error: $msg")
            }
        })
    }

    fun launchOrPatch() {
        if (patchedInstalled.value) {
            launchGame()
        } else {
            patchAndInstall()
        }
    }

    fun resetPatch() { patchState.value = PatchState.Idle }

    fun installExternalApk(ctx: Context, uri: Uri) {
        viewModelScope.launch {
            AppLogger.i(TAG, "Installing external APK: $uri")
            statusMsg.value = "Preparando APK..."
            try {
                val stream = ctx.contentResolver.openInputStream(uri) ?: return@launch
                val dest = File(ctx.cacheDir, "ext_${System.currentTimeMillis()}.apk")
                FileOutputStream(dest).use { stream.copyTo(it) }
                GameHelper.installApk(ctx, dest)
                statusMsg.value = "Siga o prompt de instalação"
                AppLogger.i(TAG, "APK install prompt shown")
            } catch (e: Exception) {
                statusMsg.value = "Instalação falhou: ${e.message}"
                AppLogger.e(TAG, "External APK install: ${e.message}")
            }
        }
    }

    fun executeScript(script: String) {
        viewModelScope.launch {
            AppLogger.i(TAG, "Execute script: ${script.take(60)}")
            scriptOutput.value = "Executando..."
            val result = withContext(Dispatchers.IO) { LuaRunner.execute(script) }
            scriptOutput.value = result.getOrElse { it.message }
        }
    }

    fun launchGame() {
        AppLogger.i(TAG, "Launching game")
        GameHelper.launchPatched(ctx)
        if (hasOverlay()) startOverlay()
    }

    fun hasOverlay(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(ctx)

    fun requestOverlay(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }

    fun startOverlay() {
        val active = installedMods.value.firstOrNull { it.enabled }
        val intent = Intent(ctx, OverlayService::class.java).apply {
            putExtra(OverlayService.EXTRA_MOD, active?.name ?: "Vanilla")
            putExtra(OverlayService.EXTRA_AU_VER, auVersion.value)
            putExtra(OverlayService.EXTRA_MODS_COUNT, installedMods.value.count { it.enabled })
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            ctx.startForegroundService(intent)
        else
            ctx.startService(intent)
        overlayActive.value = true
        AppLogger.i(TAG, "Overlay started")
    }

    fun stopOverlay() {
        ctx.stopService(Intent(ctx, OverlayService::class.java))
        overlayActive.value = false
        AppLogger.i(TAG, "Overlay stopped")
    }

    fun addServer(s: CustomServer) = viewModelScope.launch {
        val list = servers.value.toMutableList()
        list.removeAll { it.id == s.id }
        list.add(s)
        Prefs.saveServers(ctx, list)
        AppLogger.i(TAG, "Server added: ${s.name}")
    }

    fun deleteServer(id: String) = viewModelScope.launch {
        Prefs.saveServers(ctx, servers.value.filter { it.id != id })
        AppLogger.i(TAG, "Server deleted: $id")
    }

    fun toggleMod(id: String) = viewModelScope.launch {
        Prefs.saveInstalledMods(ctx, installedMods.value.map {
            if (it.id == id) it.copy(enabled = !it.enabled) else it
        })
    }

    fun deleteMod(id: String) = viewModelScope.launch {
        installedMods.value.find { it.id == id }?.filePath?.let { File(it).delete() }
        Prefs.saveInstalledMods(ctx, installedMods.value.filter { it.id != id })
        AppLogger.i(TAG, "Mod deleted: $id")
    }

    fun clearStatus() { statusMsg.value = null }
    fun clearScript() { scriptOutput.value = null }
}
