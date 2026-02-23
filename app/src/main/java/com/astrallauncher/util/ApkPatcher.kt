package com.astrallauncher.util

import android.content.Context
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object ApkPatcher {

    interface Callback {
        fun onProgress(step: String, pct: Int)
        fun onSuccess(apk: File)
        fun onError(msg: String)
    }

    fun patch(ctx: Context, dlls: List<File>, callback: Callback) {
        Thread {
            try {
                val auApkPath = GameHelper.getAuApkPath(ctx)
                    ?: return@Thread callback.onError("Among Us não encontrado. Instale o jogo primeiro.")

                callback.onProgress("Copiando APK original...", 5)
                val workDir = File(ctx.cacheDir, "patch_${System.currentTimeMillis()}")
                workDir.mkdirs()
                val srcApk = File(auApkPath)
                val outApk = File(workDir, "patched.apk")

                callback.onProgress("Injetando BepInEx...", 20)
                injectBepInEx(ctx, srcApk, outApk, dlls, callback)

                callback.onProgress("Finalizando...", 95)
                callback.onSuccess(outApk)

            } catch (e: Exception) {
                AppLogger.e("ApkPatcher", "Patch error: ${e.message}")
                callback.onError(e.message ?: "Erro desconhecido")
            }
        }.start()
    }

    private fun injectBepInEx(
        ctx: Context,
        src: File,
        out: File,
        dlls: List<File>,
        callback: Callback
    ) {
        val zin  = ZipInputStream(FileInputStream(src))
        val zout = ZipOutputStream(FileOutputStream(out))

        callback.onProgress("Reempacotando APK...", 40)
        var entry = zin.nextEntry
        while (entry != null) {
            zout.putNextEntry(ZipEntry(entry.name))
            zin.copyTo(zout)
            zout.closeEntry()
            entry = zin.nextEntry
        }
        zin.close()

        callback.onProgress("Injetando AstralBridge.dll...", 60)

        val bepInExDirs = listOf(
            "assets/BepInEx/core/",
            "assets/BepInEx/plugins/"
        )
        bepInExDirs.forEach { dir ->
            zout.putNextEntry(ZipEntry(dir))
            zout.closeEntry()
        }

        dlls.forEach { dll ->
            if (dll.exists()) {
                val pluginPath = "assets/BepInEx/plugins/${dll.name}"
                zout.putNextEntry(ZipEntry(pluginPath))
                dll.inputStream().copyTo(zout)
                zout.closeEntry()
                AppLogger.i("ApkPatcher", "Injetado: ${dll.name}")
            }
        }

        val bridgeDll = File(ctx.filesDir, "AstralBridge.dll")
        if (bridgeDll.exists()) {
            zout.putNextEntry(ZipEntry("assets/BepInEx/plugins/AstralBridge.dll"))
            bridgeDll.inputStream().copyTo(zout)
            zout.closeEntry()
            AppLogger.i("ApkPatcher", "AstralBridge.dll injetado")
        } else {
            AppLogger.w("ApkPatcher", "AstralBridge.dll não encontrado em ${ctx.filesDir} — bridge TCP não estará disponível")
        }

        callback.onProgress("Finalizando ZIP...", 85)
        zout.finish()
        zout.close()
    }

    fun extractZip(zip: File, outDir: File): List<File> {
        outDir.mkdirs()
        val extracted = mutableListOf<File>()
        ZipInputStream(FileInputStream(zip)).use { zin ->
            var entry = zin.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    val dest = File(outDir, entry.name)
                    dest.parentFile?.mkdirs()
                    FileOutputStream(dest).use { zin.copyTo(it) }
                    extracted.add(dest)
                }
                entry = zin.nextEntry
            }
        }
        return extracted
    }
}
