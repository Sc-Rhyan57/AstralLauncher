package com.astrallauncher.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

object GameHelper {
    const val AU_PACKAGE     = "com.innersloth.spacemafia"
    const val PATCHED_PACKAGE = "com.astrallauncher.au"

    fun isAuInstalled(ctx: Context): Boolean =
        runCatching { ctx.packageManager.getPackageInfo(AU_PACKAGE, 0); true }.getOrDefault(false)

    fun isPatchedInstalled(ctx: Context): Boolean =
        runCatching { ctx.packageManager.getPackageInfo(PATCHED_PACKAGE, 0); true }.getOrDefault(false)

    fun getAuVersion(ctx: Context): String =
        runCatching { ctx.packageManager.getPackageInfo(AU_PACKAGE, 0).versionName ?: "?" }.getOrDefault("?")

    fun getPatchedVersion(ctx: Context): String =
        runCatching { ctx.packageManager.getPackageInfo(PATCHED_PACKAGE, 0).versionName ?: "?" }.getOrDefault("?")

    fun launchPatched(ctx: Context) {
        val pkg = if (isPatchedInstalled(ctx)) PATCHED_PACKAGE else AU_PACKAGE
        ctx.packageManager.getLaunchIntentForPackage(pkg)?.let {
            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ctx.startActivity(it)
        }
    }

    fun installApk(ctx: Context, apk: File) {
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.provider", apk)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        ctx.startActivity(intent)
    }

    fun openPlayStore(ctx: Context) {
        runCatching {
            ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$AU_PACKAGE")).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
        }.onFailure {
            ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$AU_PACKAGE")).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
        }
    }
}
