package ru.devsoland.drydrive.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// Создаем DataStore Preferences instance на уровне файла (top-level).
// Имя "app_settings" будет использоваться для файла настроек.
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

@Singleton
class LanguageManager @Inject constructor(@ApplicationContext private val context: Context) {

    /**
     * Flow, который эмитит текущий выбранный AppLanguage при его изменении в DataStore.
     * Если язык не был сохранен, вернет язык по умолчанию.
     */
    val selectedLanguageFlow: Flow<AppLanguage> = context.dataStore.data
        .map { preferences ->
            val langCode = preferences[LanguageKeys.SELECTED_LANGUAGE_CODE] // Используем ключ из LanguageSettings.kt
            AppLanguage.fromCode(langCode) // Преобразуем код в AppLanguage
        }

    /**
     * Сохраняет выбранный язык (его код) в DataStore.
     * @param language Язык для сохранения.
     */
    suspend fun saveSelectedLanguage(language: AppLanguage) {
        context.dataStore.edit { settings ->
            settings[LanguageKeys.SELECTED_LANGUAGE_CODE] = language.code
        }
    }

    /**
     * Вспомогательная Composable функция для получения отображаемого имени языка.
     * ПРИМЕЧАНИЕ: Мы ее добавим позже, когда будем делать UI, чтобы не было ошибок компиляции,
     * если у вас еще нет Compose зависимостей или LocalContext в этом модуле.
     * Пока что ее можно закомментировать или не добавлять.
     */
    /*
    @Composable
    @ReadOnlyComposable
    fun appLanguageDisplayName(language: AppLanguage): String {
        return LocalContext.current.getString(language.displayNameResId)
    }
    */
}

