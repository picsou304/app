package com.dailyvlog.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class DailyVlogApp : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}
