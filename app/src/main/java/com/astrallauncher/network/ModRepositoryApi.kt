package com.astrallauncher.network

import com.astrallauncher.model.ModRepository
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

object ModRepositoryApi {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    const val REPO_URL = "https://raw.githubusercontent.com/YOUR_USERNAME/astral-mods-repo/main/mods.json"

    fun fetchMods(url: String = REPO_URL): Result<ModRepository> {
        return try {
            val req = Request.Builder().url(url).build()
            val resp = client.newCall(req).execute()
            val body = resp.body?.string() ?: return Result.failure(Exception("Empty response"))
            val repo = json.decodeFromString<ModRepository>(body)
            Result.success(repo)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun downloadFile(url: String, destPath: String, onProgress: (Int) -> Unit): Result<Unit> {
        return try {
            val req = Request.Builder().url(url).build()
            val resp = client.newCall(req).execute()
            val body = resp.body ?: return Result.failure(Exception("Empty body"))
            val total = body.contentLength()
            var downloaded = 0L
            val file = java.io.File(destPath)
            file.parentFile?.mkdirs()
            body.byteStream().use { inp ->
                file.outputStream().use { out ->
                    val buf = ByteArray(8192)
                    var read: Int
                    while (inp.read(buf).also { read = it } != -1) {
                        out.write(buf, 0, read)
                        downloaded += read
                        if (total > 0) onProgress(((downloaded * 100) / total).toInt())
                    }
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
