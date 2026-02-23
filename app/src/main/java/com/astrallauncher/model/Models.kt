package com.astrallauncher.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ModRepository(
    val mods: List<ModEntry> = emptyList()
)

@Serializable
data class ModEntry(
    val id: String,
    val name: String,
    val author: String,
    val description: String = "",
    val version: String = "1.0.0",
    val icon: String = "",
    val tags: List<String> = emptyList(),
    val releases: List<ModRelease> = emptyList()
)

@Serializable
data class ModRelease(
    val version: String,
    val url: String,
    val format: ModFormat = ModFormat.DLL,
    val auVersion: String = "*",
    val changelog: String = ""
)

@Serializable
enum class ModFormat {
    @SerialName("dll")  DLL,
    @SerialName("lua")  LUA,
    @SerialName("amod") AMOD,
    @SerialName("zip")  ZIP
}

@Serializable
data class InstalledMod(
    val id: String,
    val name: String,
    val author: String,
    val version: String,
    val enabled: Boolean = true,
    val format: ModFormat = ModFormat.DLL,
    val filePath: String = ""
)

@Serializable
data class CustomServer(
    val id: String,
    val name: String,
    val ip: String,
    val port: Int = 22023
)
