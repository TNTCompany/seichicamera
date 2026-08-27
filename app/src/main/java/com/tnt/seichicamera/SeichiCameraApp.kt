package com.tnt.seichicamera

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import org.osmdroid.config.Configuration

@HiltAndroidApp
class SeichiCameraApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // osmdroid configuration
        Configuration.getInstance().userAgentValue = packageName
    }
}
