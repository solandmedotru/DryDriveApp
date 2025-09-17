package ru.devsoland.drydrive.data.preferences

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
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ru.devsoland.drydrive.common.model.AppLanguage
import ru.devsoland.drydrive.common.model.ThemeSetting
import ru.devsoland.drydrive.domain.model.CityDomain // <--- Убеждаемся, что это CityDomain
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.appPreferencesDataStore by preferencesDataStore(name = "user_settings_drydrive")

object UserPreferenceKeys {
    val SELECTED_LANGUAGE_CODE = stringPreferencesKey("selected_language_code")
    val SELECTED_THEME = stringPreferencesKey("selected_theme")
    val LAST_SELECTED_CITY_JSON = stringPreferencesKey("last_selected_city_json") // Ключ для JSON строки CityDomain
}

@Singleton
class UserPreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val json: Json = Json { ignoreUnknownKeys = true; prettyPrint = false } // prettyPrint можно убрать для продакшена

    // --- Языковые настройки ---
    val selectedLanguageFlow: Flow<AppLanguage> = context.appPreferencesDataStore.data
        .catch { exception ->
            handlePreferenceReadError("language", exception)
            emit(emptyPreferences())
        }
        .map { preferences ->
            val languageCode = preferences[UserPreferenceKeys.SELECTED_LANGUAGE_CODE]
            AppLanguage.fromCode(languageCode)
        }

    suspend fun saveSelectedLanguage(language: AppLanguage) {
        try {
            context.appPreferencesDataStore.edit { preferences ->
                preferences[UserPreferenceKeys.SELECTED_LANGUAGE_CODE] = language.code
            }
        } catch (e: Exception) {
            Log.e("UserPrefsManager", "Error saving language: ${language.code}", e)
        }
    }

    // --- Настройки темы ---
    val selectedThemeFlow: Flow<ThemeSetting> = context.appPreferencesDataStore.data
        .catch { exception ->
            handlePreferenceReadError("theme", exception)
            emit(emptyPreferences())
        }
        .map { preferences ->
            val themeName = preferences[UserPreferenceKeys.SELECTED_THEME]
            try {
                if (themeName != null) ThemeSetting.valueOf(themeName) else ThemeSetting.SYSTEM
            } catch (e: IllegalArgumentException) {
                Log.w("UserPrefsManager", "Invalid theme name '$themeName', defaulting to SYSTEM.")
                ThemeSetting.SYSTEM
            }
        }

    suspend fun saveSelectedTheme(theme: ThemeSetting) {
        try {
            context.appPreferencesDataStore.edit { preferences ->
                preferences[UserPreferenceKeys.SELECTED_THEME] = theme.name
            }
        } catch (e: Exception) {
            Log.e("UserPrefsManager", "Error saving theme: ${theme.name}", e)
        }
    }

    // --- Настройки последнего выбранного города ---
    val lastSelectedCityFlow: Flow<CityDomain?> = context.appPreferencesDataStore.data // Явно Flow<CityDomain?>
        .catch { exception ->
            handlePreferenceReadError("last selected city JSON", exception)
            emit(emptyPreferences())
        }
        .map { preferences ->
            val cityJson = preferences[UserPreferenceKeys.LAST_SELECTED_CITY_JSON]
            if (cityJson != null && cityJson.isNotBlank()) {
                try {
                    json.decodeFromString<CityDomain>(cityJson) // Декодируем в CityDomain
                } catch (e: Exception) {
                    Log.e("UserPrefsManager", "Error decoding last city from JSON: $cityJson", e)
                    null // Возвращаем null в случае ошибки декодирования
                }
            } else {
                null // Возвращаем null, если JSON нет или он пуст
            }
        }

    suspend fun saveLastSelectedCity(city: CityDomain?) { // Явно принимает CityDomain?
        try {
            context.appPreferencesDataStore.edit { preferences ->
                if (city != null) {
                    try {
                        val cityJson = json.encodeToString(city) // Кодируем CityDomain в JSON
                        preferences[UserPreferenceKeys.LAST_SELECTED_CITY_JSON] = cityJson
                        Log.d("UserPrefsManager", "Saved last city as JSON: ${city.name}")
                    } catch (e: Exception) {
                        Log.e("UserPrefsManager", "Error encoding city '$${city.name}' to JSON", e)
                        preferences.remove(UserPreferenceKeys.LAST_SELECTED_CITY_JSON) // Очищаем в случае ошибки
                    }
                } else {
                    preferences.remove(UserPreferenceKeys.LAST_SELECTED_CITY_JSON)
                    Log.d("UserPrefsManager", "Cleared last selected city from preferences.")
                }
            }
        } catch (e: Exception) {
            Log.e("UserPrefsManager", "Exception while saving last city: ", e)
        }
    }

    private fun handlePreferenceReadError(preferenceName: String, exception: Throwable) {
        if (exception is IOException) {
            Log.e("UserPrefsManager", "IOException reading $preferenceName preferences.", exception)
        } else {
            Log.e("UserPrefsManager", "Unexpected error reading $preferenceName preferences.", exception)
        }
    }
}
