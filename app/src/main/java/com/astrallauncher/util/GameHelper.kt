package com.astrallauncher.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import java.io.File

object GameHelper {

    fun isAuInstalled(ctx: Context): Boolean =
        runCatching { ctx.packageManager.getPackageInfo(Constants.AU_PACKAGE, 0); true }.getOrDefault(false)

    fun isPatchedInstalled(ctx: Context): Boolean =
        runCatching { ctx.packageManager.getPackageInfo(Constants.PATCHED_PACKAGE, 0); true }.getOrDefault(false)

    fun getAuVersion(ctx: Context): String =
        runCatching { ctx.packageManager.getPackageInfo(Constants.AU_PACKAGE, 0).versionName ?: "?" }.getOrDefault("?")

    fun getPatchedVersion(ctx: Context): String =
        runCatching { ctx.packageManager.getPackageInfo(Constants.PATCHED_PACKAGE, 0).versionName ?: "?" }.getOrDefault("?")

    fun getAuApkPath(ctx: Context): String? =
        runCatching { ctx.packageManager.getApplicationInfo(Constants.AU_PACKAGE, 0).sourceDir }.getOrNull()

    fun launchPatched(ctx: Context) {
        val pkg = if (isPatchedInstalled(ctx)) Constants.PATCHED_PACKAGE else Constants.AU_PACKAGE
        AppLogger.i("GameHelper", "Launching: $pkg")
        ctx.packageManager.getLaunchIntentForPackage(pkg)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }?.let { ctx.startActivity(it) } ?: AppLogger.e("GameHelper", "No launch intent for $pkg")
    }

    fun installApk(ctx: Context, apk: File) {
        AppLogger.i("GameHelper", "Installing APK: ${apk.absolutePath} (${apk.length() / 1024}KB)")
        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            FileProvider.getUriForFile(ctx, "${ctx.packageName}.provider", apk)
        else Uri.fromFile(apk)
        ctx.startActivity(Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    fun openPlayStore(ctx: Context) {
        runCatching {
            ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${Constants.AU_PACKAGE}")).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
        }.onFailure {
            ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=${Constants.AU_PACKAGE}")).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
        }
    }
}
