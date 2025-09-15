package ru.devsoland.drydrive.data.preferences

import androidx.datastore.preferences.core.stringPreferencesKey
import ru.devsoland.drydrive.R // Важно: импорт вашего R класса для доступа к строковым ресурсам

/**
 * Ключи, используемые для хранения настроек языка в Preferences DataStore.
 */
internal object LanguageKeys {
    // Ключ для хранения кода выбранного языка (например, "ru", "en")
    val SELECTED_LANGUAGE_CODE = stringPreferencesKey("selected_language_code")
}

/**
 * Enum, представляющий доступные языки в приложении.
 * @param code Код языка (ISO 639-1).
 * @param displayNameResId ID строкового ресурса для отображаемого имени языка.
 */
enum class AppLanguage(val code: String, val displayNameResId: Int) {
    RUSSIAN("ru", R.string.language_russian), // Используем строки, добавленные на Шаге 1.1
    ENGLISH("en", R.string.language_english); // Используем строки, добавленные на Шаге 1.1

    companion object {
        /**
         * Возвращает экземпляр AppLanguage по коду языка.
         * Если код null или не найден, возвращает язык по умолчанию.
         */
        fun fromCode(code: String?): AppLanguage {
            return entries.find { it.code == code } ?: defaultLanguage()
        }

        /**
         * Возвращает язык приложения по умолчанию.
         */
        fun defaultLanguage(): AppLanguage = RUSSIAN
    }
}

