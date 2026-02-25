package com.astrallauncher.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

private val Context.ds: DataStore<Preferences> by preferencesDataStore("astral_prefs")

object Prefs {
    private val KEY_REPO_URL     = stringPreferencesKey("repo_url")
    private val KEY_ENABLED_MODS = stringSetPreferencesKey("enabled_mods")

    fun repoUrl(ctx: Context) = ctx.ds.data.map {
        it[KEY_REPO_URL] ?: Constants.REPO_URL_DEFAULT
    }

    fun enabledMods(ctx: Context) = ctx.ds.data.map {
        it[KEY_ENABLED_MODS] ?: emptySet()
    }

    suspend fun setRepoUrl(ctx: Context, url: String) {
        ctx.ds.edit { it[KEY_REPO_URL] = url }
    }

    suspend fun setEnabledMods(ctx: Context, ids: Set<String>) {
        ctx.ds.edit { it[KEY_ENABLED_MODS] = ids }
    }
}
