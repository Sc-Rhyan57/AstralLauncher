package com.astrallauncher

import android.app.Application
import com.astrallauncher.util.AppLogger

class AstralApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppLogger.i("App", "AstralLauncher v1.0.0 started")
    }
}
