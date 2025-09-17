package ru.devsoland.drydrive.feature_weather.ui.mapper

import ru.devsoland.drydrive.domain.model.CityDomain
import ru.devsoland.drydrive.feature_weather.ui.model.CityDomainUiModel
import java.util.Locale

/**
 * Преобразует доменную модель [CityDomain] в UI модель [CityDomainUiModel].
 */
fun CityDomain.toUiModel(currentLanguageCode: String? = null): CityDomainUiModel {
    val lang = currentLanguageCode ?: Locale.getDefault().language
    // Пытаемся получить имя для текущего языка, затем для "en", иначе используем основное имя
    val resolvedDisplayName = this.localNames?.get(lang)
        ?: this.localNames?.get("en")
        ?: this.name

    return CityDomainUiModel(
        name = this.name, // Основное имя (часто английское или то, что пришло от API как primary)
        lat = this.lat,
        lon = this.lon,
        country = this.country,
        state = this.state,
        originalNameFromApi = this.name, // Сохраняем оригинальное имя от API, если оно отличается от локализованных
        displayName = resolvedDisplayName, // Имя для отображения, выбранное на основе языка
        allLocalNames = this.localNames // <--- ДОБАВЛЕНО: Копируем всю карту локализованных имен
    )
}

/**
 * Преобразует список доменных моделей [CityDomain] в список UI моделей [CityDomainUiModel].
 */
fun List<CityDomain>.toUiModel(currentLanguageCode: String? = null): List<CityDomainUiModel> {
    return this.map { it.toUiModel(currentLanguageCode) }
}
