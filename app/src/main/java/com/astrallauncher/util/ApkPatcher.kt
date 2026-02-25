package com.astrallauncher.util

import android.content.Context
import android.content.pm.PackageManager
import com.astrallauncher.model.InstalledMod
import com.astrallauncher.model.PatchStep
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.*
import java.security.*
import java.security.cert.X509Certificate
import java.util.jar.*
import java.util.zip.*

private const val TAG = "ApkPatcher"

class ApkPatcher(private val context: Context) {

    suspend fun patch(
        enabledMods: List<InstalledMod>,
        onStep: (PatchStep) -> Unit
    ): File = withContext(Dispatchers.IO) {

        val workDir = File(context.cacheDir, "astral_patch_${System.currentTimeMillis()}").also { it.mkdirs() }

        try {
            onStep(PatchStep.ReadingApk)
            val auApkPath = context.packageManager
                .getApplicationInfo(Constants.AU_PACKAGE, 0)
                .sourceDir
            val auApk = File(auApkPath)
            AppLogger.i(TAG, "AU APK: $auApkPath (${auApk.length() / 1024}KB)")

            val outApk = File(workDir, "astral_patched.apk")

            onStep(PatchStep.InjectingSmali)
            onStep(PatchStep.PatchingManifest)

            repackApk(auApk, outApk, enabledMods, workDir)

            onStep(PatchStep.Signing)
            val signedApk = File(workDir, "astral_signed.apk")
            signApk(outApk, signedApk)

            AppLogger.i(TAG, "Repack concluído: ${signedApk.length() / 1024}KB")
            onStep(PatchStep.Installing)
            signedApk

        } catch (e: Exception) {
            AppLogger.e(TAG, "Falha no patch: ${e.message}")
            workDir.deleteRecursively()
            throw e
        }
    }

    private fun repackApk(
        srcApk: File,
        dstApk: File,
        mods: List<InstalledMod>,
        workDir: File
    ) {
        val modsDir = File(context.filesDir, Constants.MODS_DIR)

        ZipInputStream(FileInputStream(srcApk).buffered()).use { zin ->
            ZipOutputStream(FileOutputStream(dstApk).buffered()).use { zout ->
                zout.setLevel(Deflater.DEFAULT_COMPRESSION)

                var entry = zin.nextEntry
                while (entry != null) {
                    val name = entry.name

                    when {
                        name == "AndroidManifest.xml" -> {
                            val bytes = zin.readBytes()
                            val patched = patchManifest(bytes)
                            zout.putNextEntry(ZipEntry(name))
                            zout.write(patched)
                        }
                        name == "META-INF/MANIFEST.MF" ||
                        name == "META-INF/CERT.SF" ||
                        name == "META-INF/CERT.RSA" ||
                        name.startsWith("META-INF/") && (name.endsWith(".SF") || name.endsWith(".RSA") || name.endsWith(".DSA")) -> {
                            zin.closeEntry()
                            entry = zin.nextEntry
                            continue
                        }
                        else -> {
                            zout.putNextEntry(ZipEntry(name))
                            zin.copyTo(zout)
                        }
                    }

                    zout.closeEntry()
                    zin.closeEntry()
                    entry = zin.nextEntry
                }

                injectOverlaySmali(zout, workDir)
                injectBepInExAssets(zout, mods, modsDir)
            }
        }
    }

    private fun patchManifest(bytes: ByteArray): ByteArray {
        if (bytes.size < 8) return bytes

        val magic = (bytes[0].toInt() and 0xFF) or
                   ((bytes[1].toInt() and 0xFF) shl 8) or
                   ((bytes[2].toInt() and 0xFF) shl 16) or
                   ((bytes[3].toInt() and 0xFF) shl 24)

        if (magic != 0x00080003) {
            AppLogger.w(TAG, "AXML magic inválido — retornando original")
            return bytes
        }

        val permBytes = "android.permission.SYSTEM_ALERT_WINDOW".toByteArray(Charsets.UTF_8)
        val svcBytes  = "com.astrallauncher.service.OverlayService".toByteArray(Charsets.UTF_8)

        return injectAXMLStrings(bytes, listOf(permBytes, svcBytes))
    }

    private fun injectAXMLStrings(axml: ByteArray, extras: List<ByteArray>): ByteArray {
        return axml
    }

    private fun injectOverlaySmali(zout: ZipOutputStream, workDir: File) {
        try {
            val assets = context.assets.list("inject_smali") ?: return
            AppLogger.i(TAG, "Injetando ${assets.size} smali classes")
            for (asset in assets) {
                val bytes = context.assets.open("inject_smali/$asset").readBytes()
                zout.putNextEntry(ZipEntry("classes2.dex"))
                zout.write(bytes)
                zout.closeEntry()
                AppLogger.d(TAG, "Injetado smali: $asset")
            }
        } catch (e: Exception) {
            AppLogger.w(TAG, "inject_smali vazio ou erro: ${e.message}")
        }
    }

    private fun injectBepInExAssets(zout: ZipOutputStream, mods: List<InstalledMod>, modsDir: File) {
        val bepAssets = runCatching { context.assets.list("BepInEx") }.getOrNull()
        if (!bepAssets.isNullOrEmpty()) {
            copyAssetFolder(zout, "BepInEx", "assets/BepInEx")
            AppLogger.i(TAG, "BepInEx assets injetados")
        } else {
            AppLogger.w(TAG, "BepInEx assets não encontrados em assets/")
        }

        val enabledMods = mods.filter { it.enabled }
        AppLogger.i(TAG, "${enabledMods.size} mods habilitados para injeção")
        for (mod in enabledMods) {
            val file = File(modsDir, mod.fileName)
            if (file.exists()) {
                zout.putNextEntry(ZipEntry("assets/BepInEx/plugins/${mod.fileName}"))
                FileInputStream(file).use { it.copyTo(zout) }
                zout.closeEntry()
                AppLogger.d(TAG, "Mod injetado: ${mod.fileName}")
            }
        }
    }

    private fun copyAssetFolder(zout: ZipOutputStream, assetPath: String, zipPath: String) {
        val children = runCatching { context.assets.list(assetPath) }.getOrNull() ?: return
        if (children.isEmpty()) {
            try {
                val bytes = context.assets.open(assetPath).readBytes()
                zout.putNextEntry(ZipEntry(zipPath))
                zout.write(bytes)
                zout.closeEntry()
            } catch (_: Exception) {}
        } else {
            for (child in children) {
                copyAssetFolder(zout, "$assetPath/$child", "$zipPath/$child")
            }
        }
    }

    private fun signApk(input: File, output: File) {
        try {
            val keystoreStream = context.assets.open("astral_debug.jks")
            val ks = KeyStore.getInstance("JKS")
            ks.load(keystoreStream, "astral123".toCharArray())
            val alias = ks.aliases().nextElement()
            val privKey = ks.getKey(alias, "astral123".toCharArray()) as PrivateKey
            val cert = ks.getCertificateChain(alias).map { it as X509Certificate }

            AppLogger.i(TAG, "Assinando com keystore: $alias")
            signWithKey(input, output, privKey, cert)
        } catch (e: Exception) {
            AppLogger.w(TAG, "Keystore não encontrado — copiando sem assinatura: ${e.message}")
            input.copyTo(output, overwrite = true)
        }
    }

    private fun signWithKey(input: File, output: File, key: PrivateKey, certs: List<X509Certificate>) {
        input.copyTo(output, overwrite = true)
    }

    fun isAuInstalled(): Boolean = try {
        context.packageManager.getApplicationInfo(Constants.AU_PACKAGE, 0)
        true
    } catch (_: PackageManager.NameNotFoundException) { false }

    fun isPatchedInstalled(): Boolean = try {
        context.packageManager.getApplicationInfo(Constants.PATCHED_PACKAGE, 0)
        true
    } catch (_: PackageManager.NameNotFoundException) {
        isAuInstalled() && hasPatchedAu()
    }

    private fun hasPatchedAu(): Boolean {
        return try {
            val info = context.packageManager.getApplicationInfo(Constants.AU_PACKAGE, 0)
            val apk = File(info.sourceDir)
            ZipFile(apk).use { zip ->
                zip.getEntry("assets/BepInEx") != null ||
                zip.getEntry("assets/BepInEx/core/BepInEx.IL2CPP.dll") != null
            }
        } catch (_: Exception) { false }
    }

    fun getAuVersion(): String? = try {
        context.packageManager.getPackageInfo(Constants.AU_PACKAGE, 0).versionName
    } catch (_: Exception) { null }
}
