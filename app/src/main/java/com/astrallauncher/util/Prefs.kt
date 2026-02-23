package com.astrallauncher.util

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.astrallauncher.model.CustomServer
import com.astrallauncher.model.InstalledMod
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore by preferencesDataStore("astral_prefs")

object Prefs {
    private val KEY_MODS    = stringPreferencesKey("installed_mods")
    private val KEY_SERVERS = stringPreferencesKey("custom_servers")
    private val json = Json { ignoreUnknownKeys = true }

    fun getInstalledMods(ctx: Context): Flow<List<InstalledMod>> =
        ctx.dataStore.data.map { prefs ->
            prefs[KEY_MODS]?.let {
                try { json.decodeFromString(it) } catch (_: Exception) { emptyList() }
            } ?: emptyList()
        }

    suspend fun saveInstalledMods(ctx: Context, mods: List<InstalledMod>) {
        ctx.dataStore.edit { it[KEY_MODS] = json.encodeToString(mods) }
    }

    fun getServers(ctx: Context): Flow<List<CustomServer>> =
        ctx.dataStore.data.map { prefs ->
            prefs[KEY_SERVERS]?.let {
                try { json.decodeFromString(it) } catch (_: Exception) { emptyList() }
            } ?: emptyList()
        }

    suspend fun saveServers(ctx: Context, servers: List<CustomServer>) {
        ctx.dataStore.edit { it[KEY_SERVERS] = json.encodeToString(servers) }
    }
}
