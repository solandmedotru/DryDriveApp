package ru.devsoland.drydrive.data.preferences

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import ru.devsoland.drydrive.data.api.model.City // <-- Убедитесь, что импорт City корректен
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

// AppLanguage должен быть доступен (в этом пакете или импортирован)
// LanguageKeys должен быть доступен (в этом пакете или импортирован)

private val Context.appPreferencesDataStore by preferencesDataStore(name = "user_settings_drydrive") // Обновил имя файла DataStore

// Переименовываем LanguageKeys в UserPreferenceKeyHolder или подобное, чтобы было более общим
object UserPreferenceKeys { // Был LanguageKeys, теперь более общий
    val SELECTED_LANGUAGE_CODE = stringPreferencesKey("selected_language_code")

    // Новые ключи для города
    val LAST_CITY_NAME = stringPreferencesKey("last_city_name")
    val LAST_CITY_LAT = doublePreferencesKey("last_city_lat") // Широта - Double
    val LAST_CITY_LON = doublePreferencesKey("last_city_lon") // Долгота - Double
    val LAST_CITY_COUNTRY = stringPreferencesKey("last_city_country")
    val LAST_CITY_STATE = stringPreferencesKey("last_city_state") // State может быть null, сохраняем как String
    // localNames пока не сохраняем, их получим из API при загрузке
}

@Singleton
class UserPreferencesManager @Inject constructor(@ApplicationContext private val context: Context) {

    // --- Языковые настройки ---
    val selectedLanguageFlow: Flow<AppLanguage> = context.appPreferencesDataStore.data
        .catch { exception ->
            handlePreferenceReadError("language", exception)
            emit(emptyPreferences())
        }
        .map { preferences ->
            val languageCode = preferences[UserPreferenceKeys.SELECTED_LANGUAGE_CODE]
            Log.d("UserPrefsManager", "Read language code: '$languageCode'")
            val selectedLang = AppLanguage.fromCode(languageCode)
            Log.d("UserPrefsManager", "Mapped to AppLanguage: ${selectedLang.name} (code: '${selectedLang.code}')")
            selectedLang
        }

    suspend fun saveSelectedLanguage(language: AppLanguage) {
        try {
            context.appPreferencesDataStore.edit { preferences ->
                preferences[UserPreferenceKeys.SELECTED_LANGUAGE_CODE] = language.code
                Log.d("UserPrefsManager", "Saved language code: '${language.code}' for ${language.name}")
            }
        } catch (e: IOException) {
            Log.e("UserPrefsManager", "IOException while saving language.", e)
            // Можно добавить обработку ошибки, например, через SharedFlow для UI
        } catch (e: Exception) {
            Log.e("UserPrefsManager", "Unexpected error saving language.", e)
        }
    }

    // --- Настройки последнего выбранного города ---
    val lastSelectedCityFlow: Flow<City?> = context.appPreferencesDataStore.data
        .catch { exception ->
            handlePreferenceReadError("last selected city", exception)
            emit(emptyPreferences())
        }
        .map { preferences ->
            val name = preferences[UserPreferenceKeys.LAST_CITY_NAME]
            val lat = preferences[UserPreferenceKeys.LAST_CITY_LAT]
            val lon = preferences[UserPreferenceKeys.LAST_CITY_LON]
            val country = preferences[UserPreferenceKeys.LAST_CITY_COUNTRY]

            if (name != null && lat != null && lon != null && country != null) {
                val state = preferences[UserPreferenceKeys.LAST_CITY_STATE] // State может быть null
                Log.d("UserPrefsManager", "Read last city: $name, $lat, $lon, $country, State: $state")
                // localNames будут null, так как мы их не сохраняем. API их вернет.
                City(name = name, lat = lat, lon = lon, country = country, state = state, localNames = null)
            } else {
                Log.d("UserPrefsManager", "No valid last city data found in preferences.")
                null
            }
        }

    suspend fun saveLastSelectedCity(city: City?) {
        try {
            context.appPreferencesDataStore.edit { preferences ->
                if (city != null) {
                    preferences[UserPreferenceKeys.LAST_CITY_NAME] = city.name
                    preferences[UserPreferenceKeys.LAST_CITY_LAT] = city.lat
                    preferences[UserPreferenceKeys.LAST_CITY_LON] = city.lon
                    preferences[UserPreferenceKeys.LAST_CITY_COUNTRY] = city.country
                    if (city.state != null) {
                        preferences[UserPreferenceKeys.LAST_CITY_STATE] = city.state
                    } else {
                        preferences.remove(UserPreferenceKeys.LAST_CITY_STATE) // Удаляем, если state null
                    }
                    Log.d("UserPrefsManager", "Saved last city: ${city.name}")
                } else {
                    // Если city is null, удаляем все ключи города (сброс)
                    preferences.remove(UserPreferenceKeys.LAST_CITY_NAME)
                    preferences.remove(UserPreferenceKeys.LAST_CITY_LAT)
                    preferences.remove(UserPreferenceKeys.LAST_CITY_LON)
                    preferences.remove(UserPreferenceKeys.LAST_CITY_COUNTRY)
                    preferences.remove(UserPreferenceKeys.LAST_CITY_STATE)
                    Log.d("UserPrefsManager", "Cleared last selected city from preferences.")
                }
            }
        } catch (e: IOException) {
            Log.e("UserPrefsManager", "IOException while saving last city.", e)
        } catch (e: Exception) {
            Log.e("UserPrefsManager", "Unexpected error saving last city.", e)
        }
    }

    // Вспомогательная функция для обработки ошибок чтения
    private fun handlePreferenceReadError(preferenceName: String, exception: Throwable) {
        if (exception is IOException) {
            Log.e("UserPrefsManager", "IOException while reading $preferenceName preferences.", exception)
        } else {
            Log.e("UserPrefsManager", "Unexpected error reading $preferenceName preferences.", exception)
            // Не перебрасываем, чтобы не уронить приложение, map вернет значение по умолчанию
        }
    }
}
