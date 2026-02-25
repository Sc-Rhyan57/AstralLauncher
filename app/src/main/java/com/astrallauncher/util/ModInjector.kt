package com.astrallauncher.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object AmongUsHelper {
    const val AU_PACKAGE               = "com.innersloth.spacemafia"
    const val BEPINEX_PLUGINS_SUBPATH  = "BepInEx/plugins"

    fun isInstalled(ctx: Context): Boolean {
        return try {
            ctx.packageManager.getPackageInfo(AU_PACKAGE, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) { false }
    }

    fun getVersion(ctx: Context): String {
        return try {
            ctx.packageManager.getPackageInfo(AU_PACKAGE, 0).versionName ?: "Unknown"
        } catch (e: Exception) { "Unknown" }
    }

    fun getApkPath(ctx: Context): String? {
        return try {
            ctx.packageManager.getApplicationInfo(AU_PACKAGE, 0).sourceDir
        } catch (e: Exception) { null }
    }

    fun launch(ctx: Context) {
        val intent = ctx.packageManager.getLaunchIntentForPackage(AU_PACKAGE)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ctx.startActivity(intent)
        }
    }

    fun openPlayStore(ctx: Context) {
        val uri    = Uri.parse("market://details?id=$AU_PACKAGE")
        val intent = Intent(Intent.ACTION_VIEW, uri).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        try {
            ctx.startActivity(intent)
        } catch (e: Exception) {
            ctx.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$AU_PACKAGE"))
                    .apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            )
        }
    }
}

object ModInjector {

    fun injectDll(ctx: Context, dllFile: File, onDone: (success: Boolean, message: String) -> Unit) {
        Thread {
            try {
                val auApkPath = AmongUsHelper.getApkPath(ctx)
                    ?: return@Thread onDone(false, "Among Us APK not found")

                val workDir = File(ctx.cacheDir, "astral_inject")
                workDir.deleteRecursively()
                workDir.mkdirs()

                val originalApk = File(auApkPath)
                val patchedApk  = File(workDir, "patched.apk")

                injectDllIntoApk(originalApk, dllFile, patchedApk, ctx)

                val signedApk = File(workDir, "signed.apk")
                ApkSigner.signApk(ctx, patchedApk, signedApk)

                installApk(ctx, signedApk)
                onDone(true, "Mod injected successfully. Please follow the install prompt.")
            } catch (e: Exception) {
                onDone(false, "Injection failed: ${e.message}")
            }
        }.start()
    }

    private fun injectDllIntoApk(sourceApk: File, dllFile: File, destApk: File, ctx: Context) {
        ZipInputStream(FileInputStream(sourceApk)).use { zin ->
            ZipOutputStream(FileOutputStream(destApk)).use { zout ->
                var entry = zin.nextEntry
                while (entry != null) {
                    zout.putNextEntry(ZipEntry(entry.name))
                    zin.copyTo(zout)
                    zout.closeEntry()
                    entry = zin.nextEntry
                }
                val pluginEntry = ZipEntry("assets/${AmongUsHelper.BEPINEX_PLUGINS_SUBPATH}/${dllFile.name}")
                zout.putNextEntry(pluginEntry)
                dllFile.inputStream().use { it.copyTo(zout) }
                zout.closeEntry()
            }
        }
    }

    private fun installApk(ctx: Context, apk: File) {
        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            FileProvider.getUriForFile(ctx, "${ctx.packageName}.provider", apk)
        } else {
            Uri.fromFile(apk)
        }
        ctx.startActivity(Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    fun extractAmod(amodFile: File, destDir: File): List<File> {
        val dlls = mutableListOf<File>()
        destDir.mkdirs()
        ZipInputStream(FileInputStream(amodFile)).use { zin ->
            var entry = zin.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    val outFile = File(destDir, entry.name)
                    outFile.parentFile?.mkdirs()
                    FileOutputStream(outFile).use { zin.copyTo(it) }
                    if (entry.name.endsWith(".dll")) dlls.add(outFile)
                }
                entry = zin.nextEntry
            }
        }
        return dlls
    }
}

object ApkSigner {
    fun signApk(ctx: Context, inputApk: File, outputApk: File) {
        val ksFile = File(ctx.filesDir, "astral_debug.jks")
        if (!ksFile.exists()) {
            try {
                ctx.assets.open("astral_debug.jks").use { src ->
                    FileOutputStream(ksFile).use { dst -> src.copyTo(dst) }
                }
            } catch (_: Exception) {}
        }
        outputApk.writeBytes(inputApk.readBytes())
    }
}
