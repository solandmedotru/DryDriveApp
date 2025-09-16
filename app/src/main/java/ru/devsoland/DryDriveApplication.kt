package ru.devsoland.drydrive

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class DryDriveApplication : Application() {

    override fun onCreate() {
        super.onCreate()
    }
}