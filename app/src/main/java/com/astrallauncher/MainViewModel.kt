package com.astrallauncher

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.astrallauncher.model.CustomServer
import com.astrallauncher.model.InstalledMod
import com.astrallauncher.model.ModEntry
import com.astrallauncher.model.ModFormat
import com.astrallauncher.network.ModRepositoryApi
import com.astrallauncher.service.OverlayService
import com.astrallauncher.util.AmongUsHelper
import com.astrallauncher.util.ModInjector
import com.astrallauncher.util.Prefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val ctx: Context get() = getApplication()

    private val _auInstalled = MutableStateFlow(false)
    val auInstalled: StateFlow<Boolean> = _auInstalled.asStateFlow()

    private val _auVersion = MutableStateFlow("Unknown")
    val auVersion: StateFlow<String> = _auVersion.asStateFlow()

    private val _mods = MutableStateFlow<List<ModEntry>>(emptyList())
    val mods: StateFlow<List<ModEntry>> = _mods.asStateFlow()

    private val _modsLoading = MutableStateFlow(false)
    val modsLoading: StateFlow<Boolean> = _modsLoading.asStateFlow()

    private val _modsError = MutableStateFlow<String?>(null)
    val modsError: StateFlow<String?> = _modsError.asStateFlow()

    private val _installedMods = MutableStateFlow<List<InstalledMod>>(emptyList())
    val installedMods: StateFlow<List<InstalledMod>> = _installedMods.asStateFlow()

    private val _servers = MutableStateFlow<List<CustomServer>>(emptyList())
    val servers: StateFlow<List<CustomServer>> = _servers.asStateFlow()

    private val _downloadProgress = MutableStateFlow<Map<String, Int>>(emptyMap())
    val downloadProgress: StateFlow<Map<String, Int>> = _downloadProgress.asStateFlow()

    private val _injectStatus = MutableStateFlow<String?>(null)
    val injectStatus: StateFlow<String?> = _injectStatus.asStateFlow()

    private val _scriptOutput = MutableStateFlow<String?>(null)
    val scriptOutput: StateFlow<String?> = _scriptOutput.asStateFlow()

    private val _overlayActive = MutableStateFlow(false)
    val overlayActive: StateFlow<Boolean> = _overlayActive.asStateFlow()

    init {
        checkAmongUs()
        fetchMods()
        observeInstalledMods()
        observeServers()
    }

    fun checkAmongUs() {
        _auInstalled.value = AmongUsHelper.isInstalled(ctx)
        _auVersion.value = AmongUsHelper.getVersion(ctx)
    }

    fun fetchMods(repoUrl: String = ModRepositoryApi.REPO_URL) {
        viewModelScope.launch {
            _modsLoading.value = true
            _modsError.value = null
            val result = withContext(Dispatchers.IO) { ModRepositoryApi.fetchMods(repoUrl) }
            result.onSuccess { repo ->
                _mods.value = repo.mods
            }.onFailure { e ->
                _modsError.value = e.message ?: "Unknown error"
            }
            _modsLoading.value = false
        }
    }

    private fun observeInstalledMods() {
        viewModelScope.launch {
            Prefs.getInstalledMods(ctx).collect { _installedMods.value = it }
        }
    }

    private fun observeServers() {
        viewModelScope.launch {
            Prefs.getServers(ctx).collect { _servers.value = it }
        }
    }

    fun downloadAndInstallMod(mod: ModEntry) {
        viewModelScope.launch {
            val release = mod.releases.firstOrNull() ?: return@launch
            val ext = when (release.format) {
                ModFormat.AMOD -> ".amod"
                ModFormat.LUA -> ".lua"
                ModFormat.ZIP -> ".zip"
                else -> ".dll"
            }
            val destFile = File(ctx.filesDir, "mods/${mod.id}${ext}")

            val result = withContext(Dispatchers.IO) {
                ModRepositoryApi.downloadFile(release.url, destFile.absolutePath) { progress ->
                    _downloadProgress.value = _downloadProgress.value + (mod.id to progress)
                }
            }

            result.onSuccess {
                _downloadProgress.value = _downloadProgress.value - mod.id
                when (release.format) {
                    ModFormat.DLL -> injectDll(mod, destFile)
                    ModFormat.AMOD -> {
                        val extractDir = File(ctx.filesDir, "mods/${mod.id}_extracted")
                        val dlls = ModInjector.extractAmod(destFile, extractDir)
                        dlls.firstOrNull()?.let { injectDll(mod, it) }
                    }
                    ModFormat.LUA -> saveLuaMod(mod, destFile)
                    else -> injectDll(mod, destFile)
                }
            }.onFailure { e ->
                _downloadProgress.value = _downloadProgress.value - mod.id
                _injectStatus.value = "Download failed: ${e.message}"
            }
        }
    }

    private fun injectDll(mod: ModEntry, dllFile: File) {
        val release = mod.releases.firstOrNull() ?: return
        _injectStatus.value = "Injecting ${mod.name}..."
        ModInjector.injectDll(ctx, dllFile) { success, message ->
            _injectStatus.value = message
            if (success) {
                viewModelScope.launch {
                    val updated = _installedMods.value.toMutableList()
                    updated.removeAll { it.id == mod.id }
                    updated.add(InstalledMod(mod.id, mod.name, mod.author, mod.version,
                        format = release.format, filePath = dllFile.absolutePath))
                    Prefs.saveInstalledMods(ctx, updated)
                }
            }
        }
    }

    private fun saveLuaMod(mod: ModEntry, luaFile: File) {
        val release = mod.releases.firstOrNull() ?: return
        viewModelScope.launch {
            val updated = _installedMods.value.toMutableList()
            updated.removeAll { it.id == mod.id }
            updated.add(InstalledMod(mod.id, mod.name, mod.author, mod.version,
                format = ModFormat.LUA, filePath = luaFile.absolutePath))
            Prefs.saveInstalledMods(ctx, updated)
            _injectStatus.value = "Lua mod '${mod.name}' installed."
        }
    }

    fun executeScript(script: String) {
        viewModelScope.launch {
            _scriptOutput.value = "Executing script..."
            val result = withContext(Dispatchers.IO) {
                com.astrallauncher.util.LuaRunner.executeScript(script, ctx)
            }
            _scriptOutput.value = result.getOrElse { it.message }
        }
    }

    fun clearScriptOutput() { _scriptOutput.value = null }
    fun clearInjectStatus() { _injectStatus.value = null }

    fun launchGame(modName: String = "Vanilla") {
        AmongUsHelper.launch(ctx)
        if (hasOverlayPermission()) {
            startOverlay(modName)
        }
    }

    fun hasOverlayPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(ctx)

    fun requestOverlayPermission(context: Context) {
        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun startOverlay(modName: String) {
        val intent = Intent(ctx, OverlayService::class.java).apply {
            putExtra(OverlayService.EXTRA_MOD_NAME, modName)
            putExtra(OverlayService.EXTRA_AU_VERSION, _auVersion.value)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ctx.startForegroundService(intent)
        } else {
            ctx.startService(intent)
        }
        _overlayActive.value = true
    }

    fun stopOverlay() {
        ctx.stopService(Intent(ctx, OverlayService::class.java))
        _overlayActive.value = false
    }

    fun addServer(server: CustomServer) {
        viewModelScope.launch {
            val updated = _servers.value.toMutableList()
            updated.removeAll { it.id == server.id }
            updated.add(server)
            Prefs.saveServers(ctx, updated)
        }
    }

    fun deleteServer(id: String) {
        viewModelScope.launch {
            val updated = _servers.value.filter { it.id != id }
            Prefs.saveServers(ctx, updated)
        }
    }

    fun uninstallMod(id: String) {
        viewModelScope.launch {
            val mod = _installedMods.value.find { it.id == id }
            mod?.filePath?.let { File(it).delete() }
            val updated = _installedMods.value.filter { it.id != id }
            Prefs.saveInstalledMods(ctx, updated)
        }
    }

    fun toggleMod(id: String) {
        viewModelScope.launch {
            val updated = _installedMods.value.map {
                if (it.id == id) it.copy(enabled = !it.enabled) else it
            }
            Prefs.saveInstalledMods(ctx, updated)
        }
    }
}
