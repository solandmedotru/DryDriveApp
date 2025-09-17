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
// import ru.devsoland.drydrive.data.preferences.UserPreferencesManager // Удалено
import ru.devsoland.drydrive.domain.usecase.GetSelectedLanguageUseCase // Добавлено
import ru.devsoland.drydrive.domain.usecase.SaveSelectedLanguageUseCase // Добавлено
import ru.devsoland.drydrive.domain.usecase.GetSelectedThemeUseCase     // Добавлено
import ru.devsoland.drydrive.domain.usecase.SaveSelectedThemeUseCase     // Добавлено
import javax.inject.Inject

data class SettingsUiState(
    val selectedLanguage: AppLanguage = AppLanguage.SYSTEM, // Значение по умолчанию
    val selectedTheme: ThemeSetting = ThemeSetting.SYSTEM    // Значение по умолчанию
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    // private val userPreferencesManager: UserPreferencesManager, // Удалено
    private val getSelectedLanguageUseCase: GetSelectedLanguageUseCase, // Добавлено
    private val saveSelectedLanguageUseCase: SaveSelectedLanguageUseCase, // Добавлено
    private val getSelectedThemeUseCase: GetSelectedThemeUseCase,       // Добавлено
    private val saveSelectedThemeUseCase: SaveSelectedThemeUseCase        // Добавлено
) : ViewModel() {

    // Flow для языка
    val currentLanguage: StateFlow<AppLanguage> = getSelectedLanguageUseCase() // Изменено
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppLanguage.SYSTEM // Начальное значение до первой эмиссии
        )

    // Flow для темы
    val currentTheme: StateFlow<ThemeSetting> = getSelectedThemeUseCase() // Изменено
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ThemeSetting.SYSTEM // Начальное значение
        )

    // Используется для отображения начального состояния в UI до первой эмиссии от Flow
    // Этот State НЕ является основным источником правды для выбранных значений, 
    // основные источники - currentLanguage и currentTheme.
    // Он может быть полезен, если вам нужно отобразить что-то *до* того, как Flow эмитирует первое значение,
    // или если текущие значения currentLanguage/currentTheme используются для инициализации этого стейта.
    // Однако, учитывая, что currentLanguage/currentTheme уже имеют initialValue, явный uiState может быть избыточен
    // если он дублирует их. Если же SettingsUiState будет расширяться другими полями, не связанными
    // напрямую с этими двумя потоками, то он останется полезен.
    // val uiState: StateFlow<SettingsUiState> = combine(
    // currentLanguage,
    // currentTheme
    // ) { lang, theme ->
    // SettingsUiState(selectedLanguage = lang, selectedTheme = theme)
    // }.stateIn(
    // scope = viewModelScope,
    // started = SharingStarted.WhileSubscribed(5000),
    // initialValue = SettingsUiState() // Использует значения по умолчанию из SettingsUiState
    // )
    // Пока закомментируем uiState, т.к. currentLanguage и currentTheme уже предоставляют нужные данные

    fun onLanguageSelected(language: AppLanguage) {
        viewModelScope.launch {
            saveSelectedLanguageUseCase(language) // Изменено
        }
    }

    fun onThemeSelected(theme: ThemeSetting) {
        viewModelScope.launch {
            saveSelectedThemeUseCase(theme) // Изменено
        }
    }
}
