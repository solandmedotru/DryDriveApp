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
import ru.devsoland.drydrive.domain.model.CityDomain
import ru.devsoland.drydrive.domain.repository.UserPreferencesRepository // Добавлен импорт
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.appPreferencesDataStore by preferencesDataStore(name = "user_settings_drydrive")

object UserPreferenceKeys {
    val SELECTED_LANGUAGE_CODE = stringPreferencesKey("selected_language_code")
    val SELECTED_THEME = stringPreferencesKey("selected_theme")
    val LAST_SELECTED_CITY_JSON = stringPreferencesKey("last_selected_city_json")
}

@Singleton
class UserPreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) : UserPreferencesRepository { // Реализуем интерфейс

    private val json: Json = Json { ignoreUnknownKeys = true; prettyPrint = false }

    // --- Языковые настройки ---
    override val selectedLanguage: Flow<AppLanguage> = context.appPreferencesDataStore.data // Переименовано и override
        .catch { exception ->
            handlePreferenceReadError("language", exception)
            emit(emptyPreferences())
        }
        .map { preferences ->
            val languageCode = preferences[UserPreferenceKeys.SELECTED_LANGUAGE_CODE]
            AppLanguage.fromCode(languageCode)
        }

    override suspend fun saveSelectedLanguage(language: AppLanguage) { // override
        try {
            context.appPreferencesDataStore.edit { preferences ->
                preferences[UserPreferenceKeys.SELECTED_LANGUAGE_CODE] = language.code
            }
        } catch (e: Exception) {
            Log.e("UserPrefsManager", "Error saving language: ${language.code}", e)
        }
    }

    // --- Настройки темы ---
    override val selectedTheme: Flow<ThemeSetting> = context.appPreferencesDataStore.data // Переименовано и override
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

    override suspend fun saveSelectedTheme(theme: ThemeSetting) { // override
        try {
            context.appPreferencesDataStore.edit { preferences ->
                preferences[UserPreferenceKeys.SELECTED_THEME] = theme.name
            }
        } catch (e: Exception) {
            Log.e("UserPrefsManager", "Error saving theme: ${theme.name}", e)
        }
    }

    // --- Настройки последнего выбранного города ---
    override val lastSelectedCity: Flow<CityDomain?> = context.appPreferencesDataStore.data // Переименовано и override
        .catch { exception ->
            handlePreferenceReadError("last selected city JSON", exception)
            emit(emptyPreferences())
        }
        .map { preferences ->
            val cityJson = preferences[UserPreferenceKeys.LAST_SELECTED_CITY_JSON]
            if (cityJson != null && cityJson.isNotBlank()) {
                try {
                    json.decodeFromString<CityDomain>(cityJson)
                } catch (e: Exception) {
                    Log.e("UserPrefsManager", "Error decoding last city from JSON: $cityJson", e)
                    null
                }
            } else {
                null
            }
        }

    override suspend fun saveLastSelectedCity(city: CityDomain?) { // override
        try {
            context.appPreferencesDataStore.edit { preferences ->
                if (city != null) {
                    try {
                        val cityJson = json.encodeToString(city)
                        preferences[UserPreferenceKeys.LAST_SELECTED_CITY_JSON] = cityJson
                        Log.d("UserPrefsManager", "Saved last city as JSON: ${city.name}")
                    } catch (e: Exception) {
                        Log.e("UserPrefsManager", "Error encoding city '${city.name}' to JSON", e)
                        preferences.remove(UserPreferenceKeys.LAST_SELECTED_CITY_JSON)
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
