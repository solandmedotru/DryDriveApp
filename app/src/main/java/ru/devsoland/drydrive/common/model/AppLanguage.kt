package ru.devsoland.drydrive.common.model

import androidx.annotation.StringRes
import ru.devsoland.drydrive.R

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
            if (code.isNullOrEmpty()) {
                return SYSTEM
            }
            // entries.find { it.code == code } должен работать для всех, включая SYSTEM, если его код ""
            return entries.find { it.code == code } ?: SYSTEM // Если код не найден, возвращаем SYSTEM
        }
        // По умолчанию приложение будет использовать системный язык
        fun defaultLanguage(): AppLanguage = SYSTEM
    }
}