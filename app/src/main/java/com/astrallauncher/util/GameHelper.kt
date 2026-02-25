package com.astrallauncher.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

private const val TAG = "GameHelper"

object GameHelper {

    fun launchGame(ctx: Context) {
        val pkg = Constants.AU_PACKAGE
        val intent = ctx.packageManager.getLaunchIntentForPackage(pkg)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ctx.startActivity(intent)
            AppLogger.i(TAG, "Launching: $pkg")
        } else {
            AppLogger.e(TAG, "AU não encontrado: $pkg")
        }
    }

    fun installApk(ctx: Context, apkFile: File) {
        AppLogger.i(TAG, "Installing APK: ${apkFile.name} (${apkFile.length() / 1024}KB)")
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.provider", apkFile)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        ctx.startActivity(intent)
    }

    fun openPlayStore(ctx: Context) {
        val uri = Uri.parse("market://details?id=${Constants.AU_PACKAGE}")
        ctx.startActivity(Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }
}
