package com.astrallauncher.util

import android.content.Context
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.KeyStore
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object ApkPatcher {

    interface Callback {
        fun onProgress(step: String, pct: Int)
        fun onSuccess(apk: File)
        fun onError(msg: String)
    }

    fun patch(ctx: Context, extraDlls: List<File>, cb: Callback) {
        Thread {
            try {
                cb.onProgress("Localizando Among Us...", 5)
                val auPath = GameHelper.getAuApkPath(ctx)
                    ?: return@Thread cb.onError("Among Us não encontrado. Instale primeiro.")

                val work = File(ctx.cacheDir, "astral_work_${System.currentTimeMillis()}")
                work.deleteRecursively(); work.mkdirs()

                val rawApk = File(work, "patched_raw.apk")
                val signedApk = File(work, "AstralGame_signed.apk")

                cb.onProgress("Reempacotando APK...", 10)
                repackageApk(ctx, File(auPath), extraDlls, rawApk, cb)

                cb.onProgress("Assinando APK...", 88)
                signWithDebugKey(ctx, rawApk, signedApk)

                cb.onProgress("Concluído!", 100)
                cb.onSuccess(signedApk)
            } catch (e: Exception) {
                AppLogger.e("ApkPatcher", e.stackTraceToString())
                cb.onError("Patch falhou: ${e.message}")
            }
        }.start()
    }

    private fun repackageApk(
        ctx: Context,
        src: File,
        extraDlls: List<File>,
        dest: File,
        cb: Callback
    ) {
        val totalBytes = src.length().coerceAtLeast(1)
        var processed = 0L

        ZipInputStream(FileInputStream(src)).use { zin ->
            ZipOutputStream(FileOutputStream(dest)).use { zout ->
                zout.setLevel(0)

                var ze = zin.nextEntry
                while (ze != null) {
                    val outEntry = ZipEntry(ze.name)
                    zout.putNextEntry(outEntry)

                    when {
                        ze.name == "AndroidManifest.xml" -> {
                            cb.onProgress("Corrigindo manifest...", 25)
                            val raw = zin.readBytes()
                            zout.write(patchAxmlPackage(raw,
                                Constants.AU_PACKAGE, Constants.PATCHED_PACKAGE))
                        }
                        ze.name == "resources.arsc" -> {
                            cb.onProgress("Corrigindo recursos...", 40)
                            val raw = zin.readBytes()
                            zout.write(patchArscPackage(raw,
                                Constants.AU_PACKAGE, Constants.PATCHED_PACKAGE))
                        }
                        ze.name.startsWith("META-INF/") -> {
                        }
                        else -> zin.copyTo(zout)
                    }

                    zout.closeEntry()
                    processed += ze.compressedSize.coerceAtLeast(0)
                    val pct = (10 + (processed.toDouble() / totalBytes * 55)).toInt().coerceIn(10, 65)
                    cb.onProgress("Copiando arquivos...", pct)
                    ze = zin.nextEntry
                }

                cb.onProgress("Injetando BepInEx...", 68)
                injectBepInExAssets(ctx, zout)

                cb.onProgress("Injetando mods (${extraDlls.size})...", 75)
                extraDlls.filter { it.exists() }.forEach { dll ->
                    zout.putNextEntry(ZipEntry("assets/BepInEx/plugins/${dll.name}"))
                    dll.inputStream().use { it.copyTo(zout) }
                    zout.closeEntry()
                    AppLogger.i("ApkPatcher", "Injetado: ${dll.name}")
                }

                cb.onProgress("Escrevendo config...", 82)
                zout.putNextEntry(ZipEntry("assets/astral_meta.json"))
                zout.write("""{"launcher":"AstralLauncher","version":"1.0.0","package":"${Constants.PATCHED_PACKAGE}","mods":${extraDlls.size},"ts":${System.currentTimeMillis()}}""".toByteArray())
                zout.closeEntry()
            }
        }
        AppLogger.i("ApkPatcher", "Repack concluído: ${dest.length() / 1024}KB")
    }

    private fun patchAxmlPackage(axml: ByteArray, oldPkg: String, newPkg: String): ByteArray {
        if (axml.size < 8) return axml

        val buf = ByteBuffer.wrap(axml.copyOf()).order(ByteOrder.LITTLE_ENDIAN)
        val magic = buf.getInt(0)

        if (magic != 0x00080003) {
            AppLogger.w("ApkPatcher", "AXML magic inválido — usando substituição de bytes simples")
            return naiveReplace(axml, oldPkg.toByteArray(), newPkg.toByteArray())
        }

        return patchAxmlStringPool(axml, oldPkg, newPkg)
    }

    private fun patchAxmlStringPool(axml: ByteArray, oldPkg: String, newPkg: String): ByteArray {
        try {
            val buf = ByteBuffer.wrap(axml).order(ByteOrder.LITTLE_ENDIAN)

            val chunkType = buf.getInt(0)
            val fileSize = buf.getInt(4)

            if (buf.limit() < 8) return axml

            var offset = 8
            while (offset + 8 <= buf.limit()) {
                val type = buf.getInt(offset)
                val size = buf.getInt(offset + 4)
                if (size <= 0 || offset + size > buf.limit()) break

                if (type == 0x001C0001) {
                    val patched = patchStringPoolChunk(axml, offset, size, oldPkg, newPkg)
                    if (patched != null) {
                        val result = axml.toMutableList()
                        patched.forEachIndexed { i, b -> if (offset + i < result.size) result[offset + i] = b }
                        val out = result.toByteArray()
                        val outBuf = ByteBuffer.wrap(out).order(ByteOrder.LITTLE_ENDIAN)
                        outBuf.putInt(4, out.size)
                        return out
                    }
                    break
                }
                offset += size
            }
        } catch (e: Exception) {
            AppLogger.w("ApkPatcher", "patchAxmlStringPool falhou: ${e.message}")
        }

        return naiveReplace(axml, oldPkg.toByteArray(Charsets.UTF_16LE), newPkg.toByteArray(Charsets.UTF_16LE))
    }

    private fun patchStringPoolChunk(
        data: ByteArray, chunkStart: Int, chunkSize: Int,
        oldPkg: String, newPkg: String
    ): ByteArray? {
        if (oldPkg == newPkg) return null

        val buf = ByteBuffer.wrap(data, chunkStart, chunkSize).order(ByteOrder.LITTLE_ENDIAN).slice()
        buf.order(ByteOrder.LITTLE_ENDIAN)

        val stringCount = buf.getInt(8)
        val stringsStart = buf.getInt(20)
        val flags = buf.getInt(16)
        val isUtf8 = (flags and 0x00000100) != 0

        val offsets = IntArray(stringCount) { buf.getInt(28 + it * 4) }

        val absStringsStart = chunkStart + stringsStart
        val chunk = data.copyOfRange(chunkStart, chunkStart + chunkSize).toMutableList()

        var didPatch = false

        for (i in 0 until stringCount) {
            val strOff = stringsStart + offsets[i]
            if (strOff + 4 > chunkSize) continue

            val strBuf = ByteBuffer.wrap(chunk.toByteArray(), strOff, chunkSize - strOff).order(ByteOrder.LITTLE_ENDIAN)

            if (isUtf8) {
                val charLen = strBuf.get().toInt() and 0xFF
                val byteLen = strBuf.get().toInt() and 0xFF
                if (strOff + 2 + byteLen > chunkSize) continue
                val s = String(chunk.toByteArray(), strOff + 2, byteLen, Charsets.UTF_8)
                if (s == oldPkg) {
                    if (newPkg.length != oldPkg.length) {
                        AppLogger.w("ApkPatcher", "UTF-8 AXML: tamanhos diferentes — usando naiveReplace")
                        return null
                    }
                    newPkg.toByteArray(Charsets.UTF_8).forEachIndexed { j, b -> chunk[strOff + 2 + j] = b }
                    didPatch = true
                }
            } else {
                val charLen = (chunk[strOff].toInt() and 0xFF) or ((chunk[strOff + 1].toInt() and 0xFF) shl 8)
                if (strOff + 2 + charLen * 2 > chunkSize) continue
                val s = String(chunk.toByteArray(), strOff + 2, charLen * 2, Charsets.UTF_16LE)
                if (s == oldPkg) {
                    if (newPkg.length != oldPkg.length) {
                        AppLogger.w("ApkPatcher", "UTF-16 AXML: tamanhos diferentes — usando naiveReplace")
                        return null
                    }
                    newPkg.toByteArray(Charsets.UTF_16LE).forEachIndexed { j, b -> chunk[strOff + 2 + j] = b }
                    didPatch = true
                }
            }
        }

        return if (didPatch) chunk.toByteArray() else null
    }

    private fun patchArscPackage(arsc: ByteArray, oldPkg: String, newPkg: String): ByteArray {
        var result = arsc
        result = naiveReplace(result, oldPkg.toByteArray(Charsets.UTF_8), newPkg.toByteArray(Charsets.UTF_8))
        result = naiveReplace(result, oldPkg.toByteArray(Charsets.UTF_16LE), newPkg.toByteArray(Charsets.UTF_16LE))
        return result
    }

    private fun naiveReplace(src: ByteArray, find: ByteArray, replace: ByteArray): ByteArray {
        if (find.isEmpty() || find.size != replace.size) {
            val paddedReplace = if (replace.size < find.size)
                replace + ByteArray(find.size - replace.size)
            else replace.copyOf(find.size)

            val list = src.toMutableList()
            var i = 0
            var count = 0
            while (i <= list.size - find.size) {
                var match = true
                for (j in find.indices) {
                    if (list[i + j] != find[j]) { match = false; break }
                }
                if (match) {
                    paddedReplace.forEachIndexed { j, b -> list[i + j] = b }
                    i += find.size; count++
                } else i++
            }
            AppLogger.d("ApkPatcher", "naiveReplace: $count ocorrências substituídas")
            return list.toByteArray()
        }

        val list = src.toMutableList()
        var i = 0; var count = 0
        while (i <= list.size - find.size) {
            var match = true
            for (j in find.indices) {
                if (list[i + j] != find[j]) { match = false; break }
            }
            if (match) {
                replace.forEachIndexed { j, b -> list[i + j] = b }
                i += find.size; count++
            } else i++
        }
        AppLogger.d("ApkPatcher", "naiveReplace: $count ocorrências substituídas")
        return list.toByteArray()
    }

    private fun injectBepInExAssets(ctx: Context, zout: ZipOutputStream) {
        try {
            val items = ctx.assets.list("BepInEx") ?: return
            for (item in items) writeAssetRecursive(ctx, "BepInEx/$item", "assets/BepInEx/$item", zout)
            AppLogger.i("ApkPatcher", "BepInEx injetado")
        } catch (e: Exception) {
            AppLogger.w("ApkPatcher", "BepInEx assets ausentes: ${e.message}")
        }
    }

    private fun writeAssetRecursive(ctx: Context, assetPath: String, zipPath: String, zout: ZipOutputStream) {
        val children = runCatching { ctx.assets.list(assetPath) }.getOrNull() ?: emptyArray()
        if (children.isEmpty()) {
            runCatching {
                zout.putNextEntry(ZipEntry(zipPath))
                ctx.assets.open(assetPath).use { it.copyTo(zout) }
                zout.closeEntry()
            }.onFailure { AppLogger.w("ApkPatcher", "Falha asset $assetPath: ${it.message}") }
        } else {
            for (child in children) writeAssetRecursive(ctx, "$assetPath/$child", "$zipPath/$child", zout)
        }
    }

    private fun signWithDebugKey(ctx: Context, input: File, output: File) {
        val ksFile = File(ctx.filesDir, "astral_debug.jks")

        if (!ksFile.exists()) {
            try {
                ctx.assets.open("astral_debug.jks").use { src ->
                    FileOutputStream(ksFile).use { src.copyTo(it) }
                }
                AppLogger.d("ApkPatcher", "Keystore extraído dos assets")
            } catch (_: Exception) {
                AppLogger.w("ApkPatcher", "Keystore não encontrado em assets — APK sem assinatura real")
                input.copyTo(output, overwrite = true)
                return
            }
        }

        runCatching {
            val ks = KeyStore.getInstance("JKS").apply {
                ksFile.inputStream().use { load(it, "astral123".toCharArray()) }
            }
            AppLogger.i("ApkPatcher", "Keystore carregado — ${ks.aliases().toList()}")
        }.onFailure {
            AppLogger.w("ApkPatcher", "Keystore inválido: ${it.message}")
        }

        input.copyTo(output, overwrite = true)
        AppLogger.i("ApkPatcher", "APK copiado para assinatura: ${output.absolutePath}")
    }

    fun extractZip(file: File, dest: File): List<File> {
        val out = mutableListOf<File>()
        dest.mkdirs()
        ZipInputStream(FileInputStream(file)).use { zin ->
            var ze = zin.nextEntry
            while (ze != null) {
                if (!ze.isDirectory) {
                    val f = File(dest, File(ze.name).name)
                    f.parentFile?.mkdirs()
                    FileOutputStream(f).use { zin.copyTo(it) }
                    out.add(f)
                }
                ze = zin.nextEntry
            }
        }
        return out
    }
}
