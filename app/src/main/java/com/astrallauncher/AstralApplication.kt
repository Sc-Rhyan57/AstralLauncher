package com.astrallauncher

import android.app.Application
import com.astrallauncher.util.AppLogger

class AstralApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppLogger.init(this)
        AppLogger.i("App", "AstralLauncher started — version 1.0.0")
    }
}
