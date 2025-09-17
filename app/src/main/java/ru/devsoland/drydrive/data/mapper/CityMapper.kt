package ru.devsoland.drydrive.data.mapper

import ru.devsoland.drydrive.data.api.model.City as ApiCity // Используем алиас во избежание конфликта имен, если он будет
import ru.devsoland.drydrive.domain.model.CityDomain

/**
 * Преобразует DTO [ApiCity] (ответ от API) в доменную модель [CityDomain].
 */
fun ApiCity.toDomain(): CityDomain {
    return CityDomain(
        name = this.name,
        lat = this.lat,
        lon = this.lon,
        country = this.country,
        state = this.state,
        localNames = this.localNames
    )
}

/**
 * Преобразует список DTO [ApiCity] в список доменных моделей [CityDomain].
 */
fun List<ApiCity>.toDomain(): List<CityDomain> {
    return this.map { it.toDomain() }
}
