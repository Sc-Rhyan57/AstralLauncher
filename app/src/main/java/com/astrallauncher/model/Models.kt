package com.astrallauncher.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ModRepository(val version: Int = 1, val mods: List<ModEntry> = emptyList())

@Serializable
data class ModEntry(
    val id: String,
    val name: String,
    val author: String,
    val description: String,
    @SerialName("short_description") val shortDescription: String = "",
    val version: String,
    @SerialName("game_version") val gameVersion: String = "2026.x",
    val icon: String = "",
    val banner: String = "",
    val screenshots: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val downloads: Int = 0,
    @SerialName("updated_at") val updatedAt: String = "",
    val releases: List<ModRelease> = emptyList(),
    @SerialName("source_url") val sourceUrl: String = "",
    @SerialName("discord_url") val discordUrl: String = "",
    @SerialName("starlight_compatible") val starlightCompatible: Boolean = true,
    @SerialName("astral_format") val astralFormat: Boolean = false,
    @SerialName("supports_lua") val supportsLua: Boolean = false,
    val changelog: List<ChangelogEntry> = emptyList()
)

@Serializable
data class ModRelease(
    val version: String,
    val url: String,
    val format: ModFormat = ModFormat.DLL,
    val size: Long = 0,
    val checksum: String = "",
    @SerialName("game_version") val gameVersion: String = "",
    @SerialName("release_notes") val releaseNotes: String = "",
    @SerialName("published_at") val publishedAt: String = ""
)

@Serializable
enum class ModFormat {
    @SerialName("dll") DLL,
    @SerialName("zip") ZIP,
    @SerialName("amod") AMOD,
    @SerialName("lua") LUA
}

@Serializable
data class ChangelogEntry(
    val version: String,
    val date: String,
    val changes: List<String>
)

data class InstalledMod(
    val id: String,
    val name: String,
    val author: String,
    val version: String,
    val installedAt: Long = System.currentTimeMillis(),
    val enabled: Boolean = true,
    val format: ModFormat = ModFormat.DLL,
    val filePath: String = ""
)

@Serializable
data class CustomServer(
    val id: String,
    val name: String,
    val ip: String,
    val port: Int = 22023,
    val region: String = "Custom",
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
