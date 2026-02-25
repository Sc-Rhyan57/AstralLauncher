package com.astrallauncher

import android.app.Application
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.astrallauncher.bridge.AstralBridgeClient
import com.astrallauncher.model.CustomServer
import com.astrallauncher.model.InstalledMod
import com.astrallauncher.model.Mod
import com.astrallauncher.network.ModRepositoryApi
import com.astrallauncher.util.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

private const val TAG = "VM"

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val ctx = app.applicationContext
    private val patcher = ApkPatcher(ctx)
    private val bridge = AstralBridgeClient()

    private val _auInstalled    = MutableStateFlow(false)
    private val _auVersion      = MutableStateFlow<String?>(null)
    private val _isPatchedAu    = MutableStateFlow(false)
    private val _isPatchingNow  = MutableStateFlow(false)
    private val _patchProgress  = MutableStateFlow(0f)
    private val _patchStep      = MutableStateFlow<String?>(null)
    private val _overlayRunning = MutableStateFlow(false)
    private val _remoteMods     = MutableStateFlow<List<Mod>>(emptyList())
    private val _installedMods  = MutableStateFlow<List<InstalledMod>>(emptyList())
    private val _patchError     = MutableStateFlow<String?>(null)
    private val _hasOverlayPerm = MutableStateFlow(false)
    private val _servers        = MutableStateFlow<List<CustomServer>>(emptyList())

    val auInstalled    = _auInstalled.asStateFlow()
    val auVersion      = _auVersion.asStateFlow()
    val isPatchedAu    = _isPatchedAu.asStateFlow()
    val isPatchingNow  = _isPatchingNow.asStateFlow()
    val patchProgress  = _patchProgress.asStateFlow()
    val patchStep      = _patchStep.asStateFlow()
    val overlayRunning = _overlayRunning.asStateFlow()
    val remoteMods     = _remoteMods.asStateFlow()
    val installedMods  = _installedMods.asStateFlow()
    val patchError     = _patchError.asStateFlow()
    val hasOverlayPerm = _hasOverlayPerm.asStateFlow()
    val servers        = _servers.asStateFlow()

    init {
        refreshStatus()
        loadInstalledMods()
    }

    fun refreshStatus() {
        _auInstalled.value    = patcher.isAuInstalled()
        _auVersion.value      = patcher.getAuVersion()
        _isPatchedAu.value    = patcher.isPatchedInstalled()
        _hasOverlayPerm.value = Settings.canDrawOverlays(ctx)
        AppLogger.i(TAG, "AU=${_auInstalled.value} v=${_auVersion.value} patched=${_isPatchedAu.value}")
    }

    fun loadInstalledMods() {
        val modsDir = File(ctx.filesDir, Constants.MODS_DIR).also { it.mkdirs() }
        _installedMods.value = modsDir.listFiles { f -> f.extension == "dll" }
            ?.map { f ->
                InstalledMod(
                    id       = f.nameWithoutExtension,
                    name     = f.nameWithoutExtension,
                    author   = "local",
                    version  = "?",
                    fileName = f.name,
                    enabled  = true
                )
            } ?: emptyList()
    }

    fun patchAndInstall() {
        if (_isPatchingNow.value) return
        viewModelScope.launch {
            _isPatchingNow.value = true
            _patchError.value    = null
            try {
                val apk = patcher.patch(
                    enabledMods = _installedMods.value.filter { it.enabled },
                    onStep = { step ->
                        _patchStep.value    = step.label
                        _patchProgress.value = step.progress
                        AppLogger.i(TAG, "Patch step: ${step.label}")
                    }
                )
                GameHelper.installApk(ctx, apk)
            } catch (e: Exception) {
                _patchError.value = e.message
                AppLogger.e(TAG, "Patch falhou: ${e.message}")
            } finally {
                _isPatchingNow.value = false
                _patchStep.value     = null
            }
        }
    }

    fun launchGame() {
        AppLogger.i(TAG, "Launching game. patched=${_isPatchedAu.value}")
        GameHelper.launchGame(ctx)
    }

    fun toggleOverlay() {
        if (_overlayRunning.value) {
            com.astrallauncher.service.OverlayService.stop(ctx)
            _overlayRunning.value = false
        } else {
            if (Settings.canDrawOverlays(ctx)) {
                com.astrallauncher.service.OverlayService.start(ctx)
                _overlayRunning.value = true
            }
        }
    }

    fun fetchRemoteMods() {
        viewModelScope.launch {
            val url = Prefs.repoUrl(ctx).first()
            _remoteMods.value = ModRepositoryApi.fetchMods(url)
        }
    }

    fun downloadMod(mod: Mod) {
        viewModelScope.launch {
            val dest = File(ctx.filesDir, Constants.MODS_DIR).also { it.mkdirs() }
            ModRepositoryApi.downloadMod(mod, dest) {}
            loadInstalledMods()
        }
    }

    fun deleteMod(mod: InstalledMod) {
        val file = File(ctx.filesDir, "${Constants.MODS_DIR}/${mod.fileName}")
        file.delete()
        loadInstalledMods()
    }

    fun toggleModEnabled(mod: InstalledMod) {
        _installedMods.value = _installedMods.value.map {
            if (it.id == mod.id) it.copy(enabled = !it.enabled) else it
        }
    }

    fun runBridgeScript(script: String): String {
        return bridge.executeScript(script)
    }

    fun addServer(server: CustomServer) {
        _servers.value = _servers.value + server
    }

    fun deleteServer(id: String) {
        _servers.value = _servers.value.filter { it.id != id }
    }

    override fun onCleared() {
        super.onCleared()
        bridge.destroy()
    }
}
