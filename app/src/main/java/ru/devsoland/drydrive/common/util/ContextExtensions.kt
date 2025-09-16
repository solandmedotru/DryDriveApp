package ru.devsoland.drydrive.common.util

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

fun Context.setAppLocale(languageCode: String): Context {
    Log.d("AppLocaleConfig", "Context.setAppLocale called with languageCode: '$languageCode'")
    val localeToSet: Locale = if (languageCode.isEmpty()) {
        val systemLocale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            resources.configuration.locales[0]
        } else {
            @Suppress("DEPRECATION")
            resources.configuration.locale
        }
        Log.d("AppLocaleConfig", "Using system locale: $systemLocale")
        systemLocale
    } else {
        Log.d("AppLocaleConfig", "Using specific locale for code: $languageCode")
        Locale(languageCode)
    }
    Locale.setDefault(localeToSet)
    val config = Configuration(resources.configuration)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        config.setLocales(android.os.LocaleList(localeToSet))
    } else {
        @Suppress("DEPRECATION")
        config.locale = localeToSet
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val localeListCompat = if (languageCode.isEmpty()) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(languageCode)
        }
        try {
            AppCompatDelegate.setApplicationLocales(localeListCompat)
            Log.d("AppLocaleConfig", "AppCompatDelegate.setApplicationLocales SUCCEEDED for '$languageCode'")
        } catch (e: Exception) {
            Log.e("AppLocaleConfig", "Error in AppCompatDelegate.setApplicationLocales: ${e.message}", e)
        }
    }
    val updatedContext = createConfigurationContext(config)
    Log.d("AppLocaleConfig", "Returning context with new locale: ${updatedContext.resources.configuration.locale}")
    return updatedContext
}