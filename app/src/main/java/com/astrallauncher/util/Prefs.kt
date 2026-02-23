package com.astrallauncher.util

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.astrallauncher.model.CustomServer
import com.astrallauncher.model.InstalledMod
import com.astrallauncher.model.ModFormat

val Context.dataStore by preferencesDataStore(name = "astral_prefs")

object Prefs {
    private val KEY_SERVERS = stringPreferencesKey("custom_servers")
    private val KEY_INSTALLED_MODS = stringPreferencesKey("installed_mods")
    private val KEY_OVERLAY_ENABLED = booleanPreferencesKey("overlay_enabled")
    private val KEY_REPO_URL = stringPreferencesKey("repo_url")

    private val json = Json { ignoreUnknownKeys = true }

    fun getServers(ctx: Context): Flow<List<CustomServer>> =
        ctx.dataStore.data.map { prefs ->
            val raw = prefs[KEY_SERVERS] ?: "[]"
            try { json.decodeFromString(raw) } catch (e: Exception) { emptyList() }
        }

    suspend fun saveServers(ctx: Context, servers: List<CustomServer>) {
        ctx.dataStore.edit { it[KEY_SERVERS] = json.encodeToString(servers) }
    }

    fun getInstalledMods(ctx: Context): Flow<List<InstalledMod>> =
        ctx.dataStore.data.map { prefs ->
            val raw = prefs[KEY_INSTALLED_MODS] ?: "[]"
            try {
                val list = json.decodeFromString<List<InstalledModSerialized>>(raw)
                list.map { it.toInstalledMod() }
            } catch (e: Exception) { emptyList() }
        }

    suspend fun saveInstalledMods(ctx: Context, mods: List<InstalledMod>) {
        val serialized = mods.map { InstalledModSerialized.from(it) }
        ctx.dataStore.edit { it[KEY_INSTALLED_MODS] = json.encodeToString(serialized) }
    }

    fun getOverlayEnabled(ctx: Context): Flow<Boolean> =
        ctx.dataStore.data.map { it[KEY_OVERLAY_ENABLED] ?: true }

    suspend fun setOverlayEnabled(ctx: Context, enabled: Boolean) {
        ctx.dataStore.edit { it[KEY_OVERLAY_ENABLED] = enabled }
    }

    fun getRepoUrl(ctx: Context): Flow<String> =
        ctx.dataStore.data.map { it[KEY_REPO_URL] ?: com.astrallauncher.network.ModRepositoryApi.REPO_URL }

    suspend fun setRepoUrl(ctx: Context, url: String) {
        ctx.dataStore.edit { it[KEY_REPO_URL] = url }
    }
}

@kotlinx.serialization.Serializable
data class InstalledModSerialized(
    val id: String,
    val name: String,
    val author: String,
    val version: String,
    val installedAt: Long,
    val enabled: Boolean,
    val format: String,
    val filePath: String
) {
    fun toInstalledMod() = InstalledMod(id, name, author, version, installedAt, enabled,
        try { ModFormat.valueOf(format) } catch (e: Exception) { ModFormat.DLL }, filePath)

    companion object {
        fun from(m: InstalledMod) = InstalledModSerialized(
            m.id, m.name, m.author, m.version, m.installedAt, m.enabled, m.format.name, m.filePath
        )
    }
}
