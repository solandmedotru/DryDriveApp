package ru.devsoland.drydrive

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class DryDriveApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // На данный момент здесь ничего не делаем,
        // связанного с языком. Это будет добавлено позже.
        // Hilt будет инициализирован благодаря аннотации @HiltAndroidApp.
    }
}