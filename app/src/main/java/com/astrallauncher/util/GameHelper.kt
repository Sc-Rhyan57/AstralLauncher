package com.astrallauncher.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import java.io.File

object GameHelper {
    private const val AU_PACKAGE   = "com.innersloth.spacemafia"
    private const val MOD_PACKAGE  = "com.astrallauncher.game"

    fun isAuInstalled(ctx: Context): Boolean = isPackageInstalled(ctx, AU_PACKAGE)
    fun isPatchedInstalled(ctx: Context): Boolean = isPackageInstalled(ctx, MOD_PACKAGE)

    private fun isPackageInstalled(ctx: Context, pkg: String): Boolean = try {
        ctx.packageManager.getPackageInfo(pkg, 0); true
    } catch (_: PackageManager.NameNotFoundException) { false }

    fun getAuVersion(ctx: Context): String = getVersion(ctx, AU_PACKAGE)
    fun getPatchedVersion(ctx: Context): String = getVersion(ctx, MOD_PACKAGE)

    private fun getVersion(ctx: Context, pkg: String): String = try {
        ctx.packageManager.getPackageInfo(pkg, 0).versionName ?: "?"
    } catch (_: Exception) { "?" }

    fun getAuApkPath(ctx: Context): String? = try {
        ctx.packageManager.getApplicationInfo(AU_PACKAGE, 0).sourceDir
    } catch (_: Exception) { null }

    fun launchPatched(ctx: Context) {
        val intent = ctx.packageManager.getLaunchIntentForPackage(MOD_PACKAGE)
            ?: ctx.packageManager.getLaunchIntentForPackage(AU_PACKAGE)
        intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent?.let { ctx.startActivity(it) }
    }

    fun installApk(ctx: Context, apk: File) {
        val uri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", apk)
        } else {
            Uri.fromFile(apk)
        }
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        ctx.startActivity(intent)
    }
}
