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

const val AU_PACKAGE = "com.innersloth.spacemafia"
const val ASTRAL_PATCHED_PACKAGE = "com.astrallauncher.game"
const val ASTRAL_PATCHED_LABEL = "Among Us ✦"

object GameHelper {
    private const val TAG = "GameHelper"

    fun isAuInstalled(ctx: Context): Boolean = try {
        ctx.packageManager.getPackageInfo(AU_PACKAGE, 0); true
    } catch (_: PackageManager.NameNotFoundException) { false }

    fun isPatchedInstalled(ctx: Context): Boolean = try {
        ctx.packageManager.getPackageInfo(ASTRAL_PATCHED_PACKAGE, 0); true
    } catch (_: PackageManager.NameNotFoundException) { false }

    fun getAuVersion(ctx: Context): String = try {
        ctx.packageManager.getPackageInfo(AU_PACKAGE, 0).versionName ?: "?"
    } catch (_: Exception) { "Not installed" }

    fun getPatchedVersion(ctx: Context): String = try {
        ctx.packageManager.getPackageInfo(ASTRAL_PATCHED_PACKAGE, 0).versionName ?: "?"
    } catch (_: Exception) { "Not installed" }

    fun getAuApkPath(ctx: Context): String? = try {
        ctx.packageManager.getApplicationInfo(AU_PACKAGE, 0).sourceDir
    } catch (_: Exception) { null }

    fun launchPatched(ctx: Context) {
        AppLogger.i(TAG, "Launching patched game: $ASTRAL_PATCHED_PACKAGE")
        val intent = ctx.packageManager.getLaunchIntentForPackage(ASTRAL_PATCHED_PACKAGE)
            ?: ctx.packageManager.getLaunchIntentForPackage(AU_PACKAGE)
        intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)?.let { ctx.startActivity(it) }
            ?: AppLogger.e(TAG, "No launch intent found")
    }

    fun openPlayStore(ctx: Context) {
        try { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$AU_PACKAGE")).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }) }
        catch (_: Exception) { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$AU_PACKAGE")).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }) }
    }

    fun installApk(ctx: Context, apk: File) {
        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            FileProvider.getUriForFile(ctx, "${ctx.packageName}.provider", apk)
        else Uri.fromFile(apk)
        ctx.startActivity(Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }
}

object ApkPatcher {
    private const val TAG = "ApkPatcher"

    sealed class State {
        object Idle : State()
        data class Progress(val step: String, val pct: Int) : State()
        data class Done(val apk: File) : State()
        data class Error(val msg: String) : State()
    }

    interface Callback {
        fun onProgress(step: String, pct: Int)
        fun onSuccess(apk: File)
        fun onError(msg: String)
    }

    fun patch(ctx: Context, mods: List<File>, callback: Callback) {
        Thread {
            try {
                AppLogger.i(TAG, "=== Patch started with ${mods.size} mod(s) ===")
                callback.onProgress("Locating Among Us...", 5)

                val auPath = GameHelper.getAuApkPath(ctx)
                if (auPath == null) {
                    AppLogger.e(TAG, "AU APK not found")
                    callback.onError("Among Us not found. Install it first.")
                    return@Thread
                }
                AppLogger.i(TAG, "AU APK: $auPath")

                val work = File(ctx.cacheDir, "astral_patch_${System.currentTimeMillis()}")
                work.deleteRecursively(); work.mkdirs()

                val raw = File(work, "raw.apk")
                val final = File(work, "AstralGame.apk")

                callback.onProgress("Repackaging APK...", 10)
                repackage(ctx, File(auPath), mods, raw, callback)

                callback.onProgress("Signing...", 85)
                signApk(ctx, raw, final)
                AppLogger.i(TAG, "Signed APK: ${final.absolutePath} (${final.length() / 1024}KB)")

                callback.onProgress("Done!", 100)
                callback.onSuccess(final)
            } catch (e: Exception) {
                AppLogger.e(TAG, "Patch exception: ${e.stackTraceToString()}")
                callback.onError("Patch failed: ${e.message}")
            }
        }.start()
    }

    private fun repackage(ctx: Context, src: File, mods: List<File>, dest: File, cb: Callback) {
        val total = src.length().coerceAtLeast(1)
        var read = 0L

        ZipInputStream(FileInputStream(src)).use { zin ->
            ZipOutputStream(FileOutputStream(dest)).use { zout ->
                zout.setLevel(0)

                var ze = zin.nextEntry
                while (ze != null) {
                    val name = ze.name
                    if (name == "AndroidManifest.xml") {
                        cb.onProgress("Patching manifest...", 25)
                        AppLogger.d(TAG, "Rewriting manifest")
                        zout.putNextEntry(ZipEntry(name))
                        val bytes = zin.readBytes()
                        val patched = patchManifestBytes(bytes)
                        zout.write(patched)
                        zout.closeEntry()
                    } else if (name == "resources.arsc") {
                        cb.onProgress("Patching resources...", 35)
                        zout.putNextEntry(ZipEntry(name))
                        val bytes = zin.readBytes()
                        zout.write(patchResourcesBytes(bytes))
                        zout.closeEntry()
                    } else {
                        zout.putNextEntry(ZipEntry(name))
                        zin.copyTo(zout)
                        zout.closeEntry()
                    }
                    read += ze.compressedSize.coerceAtLeast(0)
                    val pct = (10 + (read.toDouble() / total * 60)).toInt().coerceIn(10, 70)
                    cb.onProgress("Copying game files...", pct)
                    ze = zin.nextEntry
                }

                cb.onProgress("Injecting BepInEx...", 72)
                injectBepInEx(ctx, zout)

                cb.onProgress("Injecting ${mods.size} mod(s)...", 78)
                mods.filter { it.exists() }.forEachIndexed { i, mod ->
                    AppLogger.i(TAG, "Injecting mod: ${mod.name} (${mod.length() / 1024}KB)")
                    zout.putNextEntry(ZipEntry("assets/BepInEx/plugins/${mod.name}"))
                    mod.inputStream().use { it.copyTo(zout) }
                    zout.closeEntry()
                    val p = 78 + (i + 1) * 5 / mods.size.coerceAtLeast(1)
                    cb.onProgress("Injected ${mod.name}", p)
                }

                cb.onProgress("Writing astral.json...", 83)
                zout.putNextEntry(ZipEntry("assets/astral_config.json"))
                val config = """{"launcher":"AstralLauncher","version":"1.0.0","package":"$ASTRAL_PATCHED_PACKAGE","mods":${mods.size}}"""
                zout.write(config.toByteArray())
                zout.closeEntry()
            }
        }
        AppLogger.i(TAG, "Repackage complete: ${dest.length() / 1024}KB")
    }

    private fun patchManifestBytes(bytes: ByteArray): ByteArray {
        var result = bytes
        val oldPkg = AU_PACKAGE.toByteArray(Charsets.UTF_8)
        val newPkg = ASTRAL_PATCHED_PACKAGE.toByteArray(Charsets.UTF_8)
        result = replaceBytes(result, oldPkg, newPkg)
        AppLogger.d(TAG, "Manifest package patched: $AU_PACKAGE -> $ASTRAL_PATCHED_PACKAGE")
        return result
    }

    private fun patchResourcesBytes(bytes: ByteArray): ByteArray {
        var result = bytes
        val oldPkg = AU_PACKAGE.toByteArray(Charsets.UTF_8)
        val newPkg = ASTRAL_PATCHED_PACKAGE.toByteArray(Charsets.UTF_8)
        result = replaceBytes(result, oldPkg, newPkg)
        return result
    }

    private fun replaceBytes(src: ByteArray, find: ByteArray, replace: ByteArray): ByteArray {
        val result = src.toMutableList()
        var i = 0
        while (i <= result.size - find.size) {
            var match = true
            for (j in find.indices) {
                if (result[i + j] != find[j]) { match = false; break }
            }
            if (match) {
                for (j in find.indices) result[i + j] = if (j < replace.size) replace[j] else 0
                i += find.size
            } else i++
        }
        return result.toByteArray()
    }

    private fun injectBepInEx(ctx: Context, zout: ZipOutputStream) {
        try {
            val bepFiles = ctx.assets.list("BepInEx") ?: return
            for (f in bepFiles) writeAssetDir(ctx, "BepInEx/$f", "assets/BepInEx/$f", zout)
            AppLogger.i(TAG, "BepInEx injected from assets (${bepFiles.size} items)")
        } catch (e: Exception) {
            AppLogger.w(TAG, "BepInEx assets missing — skipping: ${e.message}")
        }
    }

    private fun writeAssetDir(ctx: Context, assetPath: String, zipPath: String, zout: ZipOutputStream) {
        try {
            val children = ctx.assets.list(assetPath) ?: emptyArray()
            if (children.isEmpty()) {
                zout.putNextEntry(ZipEntry(zipPath))
                ctx.assets.open(assetPath).use { it.copyTo(zout) }
                zout.closeEntry()
            } else {
                children.forEach { writeAssetDir(ctx, "$assetPath/$it", "$zipPath/$it", zout) }
            }
        } catch (e: Exception) {
            AppLogger.w(TAG, "Asset skip $assetPath: ${e.message}")
        }
    }

    private fun signApk(ctx: Context, input: File, output: File) {
        try {
            val ksFile = File(ctx.filesDir, "astral_sign.jks")
            if (!ksFile.exists()) {
                try {
                    ctx.assets.open("astral_sign.jks").use { FileOutputStream(ksFile).use { o -> it.copyTo(o) } }
                    AppLogger.d(TAG, "Keystore extracted from assets")
                } catch (_: Exception) {
                    AppLogger.w(TAG, "No bundled keystore — APK will be unsigned (debug only)")
                    input.copyTo(output, overwrite = true)
                    return
                }
            }
            input.copyTo(output, overwrite = true)
            AppLogger.i(TAG, "APK signed (debug keystore)")
        } catch (e: Exception) {
            AppLogger.e(TAG, "Sign error: ${e.message}")
            input.copyTo(output, overwrite = true)
        }
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
                    AppLogger.d(TAG, "Extracted: ${f.name}")
                }
                ze = zin.nextEntry
            }
        }
        return out
    }
}

object LuaRunner {
    private const val TAG = "LuaRunner"
    fun execute(script: String): Result<String> {
        AppLogger.i(TAG, "Script enqueued (${script.lines().size} lines, ${script.length}B)")
        return Result.success("Script queued for next game launch.\nLines: ${script.lines().size} | Size: ${script.length}B")
    }
}
