package ru.devsoland.drydrive.feature_settings.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.devsoland.drydrive.common.model.AppLanguage
import ru.devsoland.drydrive.common.model.ThemeSetting
import ru.devsoland.drydrive.data.preferences.UserPreferencesManager
import javax.inject.Inject

data class SettingsUiState(
    val selectedLanguage: AppLanguage = AppLanguage.SYSTEM, // Значение по умолчанию
    val selectedTheme: ThemeSetting = ThemeSetting.SYSTEM    // Значение по умолчанию
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferencesManager: UserPreferencesManager
) : ViewModel() {

    // Flow для языка
    val currentLanguage: StateFlow<AppLanguage> = userPreferencesManager.selectedLanguageFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppLanguage.SYSTEM // Начальное значение до первой эмиссии
        )

    // Flow для темы
    val currentTheme: StateFlow<ThemeSetting> = userPreferencesManager.selectedThemeFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ThemeSetting.SYSTEM // Начальное значение
        )

    fun onLanguageSelected(language: AppLanguage) {
        viewModelScope.launch {
            userPreferencesManager.saveSelectedLanguage(language)
        }
    }

    fun onThemeSelected(theme: ThemeSetting) {
        viewModelScope.launch {
            userPreferencesManager.saveSelectedTheme(theme)
        }
    }
}
