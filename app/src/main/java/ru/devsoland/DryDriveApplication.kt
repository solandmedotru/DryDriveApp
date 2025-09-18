package ru.devsoland.drydrive

import android.app.Application
// import android.content.pm.PackageManager // Больше не используется для диагностики ключа Яндекс
import android.util.Log
import com.yandex.mapkit.MapKitFactory // Импорт для MapKitFactory
import dagger.hilt.android.HiltAndroidApp
// Важно: Импорт вашего BuildConfig. Путь может отличаться, если ваш applicationId другой
// или если BuildConfig генерируется в другом месте для вашего проекта.
// Обычно он находится в том же пакете, что и ваш Application класс.
import ru.devsoland.drydrive.BuildConfig

@HiltAndroidApp
class DryDriveApplication : Application() {

    companion object {
        private const val TAG = "API_KEY_SETUP"
    }

    override fun onCreate() {
        super.onCreate()

        // Инициализация Yandex MapKit с использованием BuildConfig
        try {
            Log.i(TAG, "Attempting to set Yandex Maps API Key from BuildConfig...")
            // Проверяем, существует ли поле YANDEX_MAPS_API_KEY в BuildConfig
            // и не является ли оно строкой-заглушкой из build.gradle.kts
            if (BuildConfig.YANDEX_MAPS_API_KEY.isNullOrEmpty() || BuildConfig.YANDEX_MAPS_API_KEY == "your_api_key_here") {
                Log.e(TAG, "YANDEX_MAPS_API_KEY in BuildConfig is null, empty, or the placeholder value. Check local.properties and gradle sync!")
                // Можно здесь выбросить исключение или обработать ошибку, чтобы предотвратить дальнейший сбой MapKit
            } else {
                Log.i(TAG, "YANDEX_MAPS_API_KEY from BuildConfig. Length: ${BuildConfig.YANDEX_MAPS_API_KEY.length}. Value (first 5 chars): '${BuildConfig.YANDEX_MAPS_API_KEY.take(5)}...'")
            }
            MapKitFactory.setApiKey(BuildConfig.YANDEX_MAPS_API_KEY)
            Log.i(TAG, "MapKitFactory.setApiKey() called.")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting Yandex Maps API Key from BuildConfig or during MapKit setup", e)
            // Важно обработать эту ошибку, чтобы приложение не падало молча или с непонятной ошибкой далее
        }

        MapKitFactory.setLocale("ru_RU") // Опционально: установка русской локали
        Log.i(TAG, "Now attempting to initialize MapKitFactory...")
        MapKitFactory.initialize(this) // Эта строка может вызвать сбой, если ключ не валиден или не был установлен корректно
        Log.i(TAG, "MapKitFactory.initialize(this) called.")
    }
}