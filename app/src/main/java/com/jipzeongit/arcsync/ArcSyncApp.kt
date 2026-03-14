package com.jipzeongit.arcsync

import android.app.Application
import com.jipzeongit.arcsync.util.AppLogger

class ArcSyncApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppLogger.init(this)
    }
}
