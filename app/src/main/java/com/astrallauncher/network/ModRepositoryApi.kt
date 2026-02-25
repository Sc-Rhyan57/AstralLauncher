package com.astrallauncher.network

import com.astrallauncher.model.Mod
import com.astrallauncher.util.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

private const val TAG = "ModRepo"

object ModRepositoryApi {
    private val client = OkHttpClient()
    private val json   = Json { ignoreUnknownKeys = true }

    suspend fun fetchMods(repoUrl: String): List<Mod> = withContext(Dispatchers.IO) {
        try {
            val resp = client.newCall(Request.Builder().url(repoUrl).build()).execute()
            val body = resp.body?.string() ?: return@withContext emptyList()
            json.decodeFromString<List<Mod>>(body)
        } catch (e: Exception) {
            AppLogger.e(TAG, "fetchMods falhou: ${e.message}")
            emptyList()
        }
    }

    suspend fun downloadMod(
        mod: Mod,
        destDir: File,
        onProgress: (Float) -> Unit
    ): File? = withContext(Dispatchers.IO) {
        try {
            val resp  = client.newCall(Request.Builder().url(mod.downloadUrl).build()).execute()
            val body  = resp.body ?: return@withContext null
            val total = body.contentLength().toFloat()
            val dest  = File(destDir, "${mod.id}.dll")
            var downloaded = 0L
            FileOutputStream(dest).use { out ->
                body.byteStream().use { inp ->
                    val buf = ByteArray(8192)
                    var n: Int
                    while (inp.read(buf).also { n = it } != -1) {
                        out.write(buf, 0, n)
                        downloaded += n
                        if (total > 0) onProgress(downloaded / total)
                    }
                }
            }
            AppLogger.i(TAG, "Baixado: ${dest.name}")
            dest
        } catch (e: Exception) {
            AppLogger.e(TAG, "download falhou: ${e.message}")
            null
        }
    }
}
