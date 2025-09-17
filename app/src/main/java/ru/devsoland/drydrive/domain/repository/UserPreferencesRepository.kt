package ru.devsoland.drydrive.domain.repository

import kotlinx.coroutines.flow.Flow
import ru.devsoland.drydrive.common.model.AppLanguage
import ru.devsoland.drydrive.common.model.ThemeSetting
import ru.devsoland.drydrive.domain.model.CityDomain

/**
 * Интерфейс репозитория для управления настройками пользователя.
 */
interface UserPreferencesRepository {

    // --- Языковые настройки ---
    val selectedLanguage: Flow<AppLanguage> // Изменено на val
    suspend fun saveSelectedLanguage(language: AppLanguage)

    // --- Настройки темы ---
    val selectedTheme: Flow<ThemeSetting> // Изменено на val
    suspend fun saveSelectedTheme(theme: ThemeSetting)

    // --- Настройки последнего выбранного города ---
    val lastSelectedCity: Flow<CityDomain?> // Изменено на val
    suspend fun saveLastSelectedCity(city: CityDomain?)
}
