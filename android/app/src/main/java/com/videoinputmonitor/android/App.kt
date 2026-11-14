package com.videoinputmonitor.android

import android.app.Application
import com.serenegiant.utils.UVCUtils

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        UVCUtils.init(this)
        CaptureManager.start(this)
        AudioController.init(this)
    }
}
