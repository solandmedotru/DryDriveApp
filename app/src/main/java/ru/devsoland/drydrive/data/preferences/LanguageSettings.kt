package ru.devsoland.drydrive.data.preferences

import androidx.annotation.StringRes
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
enum class AppLanguage(val code: String, @StringRes val displayNameResId: Int) {
    SYSTEM("", R.string.language_system), // <<--- ДОБАВЛЕНО: Пустой код для системного языка
    RUSSIAN("ru", R.string.language_russian),
    ENGLISH("en", R.string.language_english);

    companion object {
        fun fromCode(code: String?): AppLanguage {
            if (code == null || code.isEmpty()) { // "" (пустой код) это SYSTEM
                return SYSTEM
            }
            return entries.find { it.code == code && it != SYSTEM } ?: SYSTEM // Если код не стандартный, возвращаем SYSTEM
        }
        // По умолчанию приложение будет использовать системный язык
        fun defaultLanguage(): AppLanguage = SYSTEM
    }
}
