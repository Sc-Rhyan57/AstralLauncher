package com.astrallauncher.util

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.astrallauncher.model.CustomServer
import com.astrallauncher.model.InstalledMod
import com.astrallauncher.model.ModFormat
import com.astrallauncher.network.ModRepositoryApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

val Context.dataStore by preferencesDataStore(name = "astral_prefs")

object Prefs {
    private val KEY_SERVERS = stringPreferencesKey("custom_servers")
    private val KEY_INSTALLED_MODS = stringPreferencesKey("installed_mods")
    private val KEY_REPO_URL = stringPreferencesKey("repo_url")
    private val json = Json { ignoreUnknownKeys = true }

    fun getServers(ctx: Context): Flow<List<CustomServer>> =
        ctx.dataStore.data.map { try { json.decodeFromString(it[KEY_SERVERS] ?: "[]") } catch (_: Exception) { emptyList() } }

    suspend fun saveServers(ctx: Context, list: List<CustomServer>) =
        ctx.dataStore.edit { it[KEY_SERVERS] = json.encodeToString(list) }

    fun getInstalledMods(ctx: Context): Flow<List<InstalledMod>> =
        ctx.dataStore.data.map {
            try { json.decodeFromString<List<InstalledModSer>>(it[KEY_INSTALLED_MODS] ?: "[]").map { m -> m.toDomain() } }
            catch (_: Exception) { emptyList() }
        }

    suspend fun saveInstalledMods(ctx: Context, list: List<InstalledMod>) =
        ctx.dataStore.edit { it[KEY_INSTALLED_MODS] = json.encodeToString(list.map { InstalledModSer.from(it) }) }

    fun getRepoUrl(ctx: Context): Flow<String> =
        ctx.dataStore.data.map { it[KEY_REPO_URL] ?: ModRepositoryApi.REPO_URL }

    suspend fun setRepoUrl(ctx: Context, url: String) =
        ctx.dataStore.edit { it[KEY_REPO_URL] = url }
}

@Serializable
data class InstalledModSer(
    val id: String, val name: String, val author: String, val version: String,
    val installedAt: Long, val enabled: Boolean, val format: String, val filePath: String
) {
    fun toDomain() = InstalledMod(id, name, author, version, installedAt, enabled,
        try { ModFormat.valueOf(format) } catch (_: Exception) { ModFormat.DLL }, filePath)
    companion object { fun from(m: InstalledMod) = InstalledModSer(m.id, m.name, m.author, m.version, m.installedAt, m.enabled, m.format.name, m.filePath) }
}
