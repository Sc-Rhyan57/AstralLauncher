package com.astrallauncher.network

import com.astrallauncher.model.ModRepository
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

object ModRepositoryApi {
    private val client = OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).build()
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    const val REPO_URL = "https://raw.githubusercontent.com/Sc-Rhyan57/AstralData/main/AstralLauncher/mods.json"

    fun fetchMods(url: String = REPO_URL): Result<ModRepository> {
        return try {
            val resp = client.newCall(Request.Builder().url(url).build()).execute()
            val body = resp.body?.string() ?: return Result.failure(Exception("Empty response"))
            Result.success(json.decodeFromString(body))
        } catch (e: Exception) { Result.failure(e) }
    }

    fun downloadFile(url: String, dest: String, onProgress: (Int) -> Unit): Result<Unit> {
        return try {
            val resp = client.newCall(Request.Builder().url(url).build()).execute()
            val body = resp.body ?: return Result.failure(Exception("Empty body"))
            val total = body.contentLength()
            var dl = 0L
            val file = java.io.File(dest); file.parentFile?.mkdirs()
            body.byteStream().use { inp ->
                file.outputStream().use { out ->
                    val buf = ByteArray(8192); var n: Int
                    while (inp.read(buf).also { n = it } != -1) {
                        out.write(buf, 0, n); dl += n
                        if (total > 0) onProgress(((dl * 100) / total).toInt())
                    }
                }
            }
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }
}
