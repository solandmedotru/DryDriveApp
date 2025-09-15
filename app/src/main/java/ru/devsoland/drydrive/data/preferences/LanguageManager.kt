package ru.devsoland.drydrive.data.preferences // Убедитесь, что AppLanguage здесь или импортируется

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

// Если AppLanguage находится в этом же пакете (ru.devsoland.drydrive.data.preferences),
// явный импорт ru.devsoland.drydrive.data.preferences.AppLanguage не нужен.
// Если он в другом месте, убедитесь, что импорт есть.
// В вашем случае, AppLanguage находится в LanguageSettings.kt в ТОМ ЖЕ ПАКЕТЕ,
// так что явный импорт не требуется.

// Context.appPreferencesDataStore должен быть определен на уровне файла (top-level)
// или как часть объекта-компаньона, если вы хотите ограничить его видимость.
// Определение на уровне файла - обычная практика.
private val Context.appPreferencesDataStore by preferencesDataStore(name = "app_settings_drydrive") // Рекомендую добавить префикс к имени файла DataStore

@Singleton
class LanguageManager @Inject constructor(@ApplicationContext private val context: Context) {

    // Используем ключи из LanguageSettings.LanguageKeys
    // Это предпочтительнее, чем дублировать определение ключа.
    // Убедитесь, что LanguageKeys в LanguageSettings.kt имеет видимость internal или public.
    // Если LanguageKeys internal, и LanguageManager в том же модуле, все будет ОК.

    /**
     * Flow, предоставляющий текущий выбранный AppLanguage.
     * Использует AppLanguage.fromCode(), который корректно обрабатывает null (не сохранено)
     * или пустую строку (сохранен системный язык), возвращая AppLanguage.SYSTEM.
     */
    val selectedLanguageFlow: Flow<AppLanguage> = context.appPreferencesDataStore.data
        .catch { exception ->
            // Важно обрабатывать исключения, особенно IOException для DataStore
            if (exception is IOException) {
                Log.e("LanguageManager", "IOException while reading language preferences.", exception)
                emit(emptyPreferences()) // Возвращаем пустые preferences, чтобы map мог вернуть default/SYSTEM язык
            } else {
                Log.e("LanguageManager", "Unexpected error reading language preferences.", exception)
                throw exception // Перебрасываем другие, неизвестные исключения
            }
        }
        .map { preferences ->
            // Используем ключ из общего объекта LanguageKeys
            val languageCode = preferences[LanguageKeys.SELECTED_LANGUAGE_CODE]
            Log.d("LanguageManager", "Read language code from DataStore: '$languageCode'")
            val selectedLang = AppLanguage.fromCode(languageCode) // fromCode уже корректно обрабатывает null/empty
            Log.d("LanguageManager", "Mapped to AppLanguage: ${selectedLang.name} (code: '${selectedLang.code}')")
            selectedLang
        }

    /**
     * Сохраняет выбранный язык (его код) в DataStore.
     * Для AppLanguage.SYSTEM сохраняется пустая строка (согласно AppLanguage.SYSTEM.code).
     */
    suspend fun saveSelectedLanguage(language: AppLanguage) {
        context.appPreferencesDataStore.edit { preferences ->
            // Используем ключ из общего объекта LanguageKeys
            preferences[LanguageKeys.SELECTED_LANGUAGE_CODE] = language.code
            Log.d("LanguageManager", "Saved language code to DataStore: '${language.code}' for language ${language.name}")
        }
    }
}