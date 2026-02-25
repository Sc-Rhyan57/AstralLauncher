package com.astrallauncher.model

import kotlinx.serialization.Serializable

@Serializable
data class Mod(
    val id: String,
    val name: String,
    val author: String,
    val version: String,
    val description: String,
    val downloadUrl: String,
    val tags: List<String> = emptyList(),
    val downloads: Long = 0L,
    val changelog: String = ""
)

data class InstalledMod(
    val id: String,
    val name: String,
    val author: String,
    val version: String,
    val fileName: String,
    val enabled: Boolean
)

data class CustomServer(
    val id: String,
    val name: String,
    val ip: String,
    val port: Int,
    val region: String,
    val description: String
)

sealed class PatchStep(val label: String, val progress: Float) {
    object ReadingApk       : PatchStep("Lendo APK do Among Us", 0.10f)
    object InjectingSmali   : PatchStep("Injetando código do overlay", 0.35f)
    object PatchingManifest : PatchStep("Patcheando AndroidManifest", 0.50f)
    object InjectingMods    : PatchStep("Injetando mods BepInEx", 0.70f)
    object Signing          : PatchStep("Assinando APK", 0.85f)
    object Installing       : PatchStep("Abrindo instalador", 1.00f)
}
