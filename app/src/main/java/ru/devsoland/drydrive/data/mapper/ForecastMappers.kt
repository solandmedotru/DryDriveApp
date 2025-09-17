package ru.devsoland.drydrive.data.mapper

import ru.devsoland.drydrive.data.api.model.ForecastListItem
import ru.devsoland.drydrive.data.api.model.ForecastResponse
// Импортируем наши новые доменные модели
import ru.devsoland.drydrive.domain.model.FullForecastDomain
import ru.devsoland.drydrive.domain.model.SingleForecastDomain

// Импорты DTO, используемых в ForecastListItem и ForecastResponse, не нужны здесь явно,
// так как они являются полями этих классов, и Kotlin разрешит их автоматически.

/**
 * Преобразует DTO [ForecastListItem] в доменную модель [SingleForecastDomain].
 */
fun ForecastListItem.toDomain(): SingleForecastDomain {
    val mainWeather = this.weather.firstOrNull() // Берем первое погодное условие, если есть
    return SingleForecastDomain(
        dateTimeMillis = this.dt * 1000L, // dt в API обычно в секундах, переводим в миллисекунды
        tempCelsius = this.main.temp,
        feelsLikeCelsius = this.main.feelsLike,
        tempMinCelsius = this.main.tempMin,
        tempMaxCelsius = this.main.tempMax,
        pressureHpa = this.main.pressure,
        humidityPercent = this.main.humidity,
        weatherConditionId = mainWeather?.id ?: 0, // 0 как fallback, если нет данных
        weatherCondition = mainWeather?.main ?: "Unknown",
        weatherDescription = mainWeather?.description ?: "No description",
        weatherIconId = mainWeather?.icon ?: "",
        cloudinessPercent = this.clouds.all,
        windSpeedMps = this.wind.speed,
        windDirectionDegrees = this.wind.deg,
        windGustMps = this.wind.gust,
        visibilityMeters = this.visibility,
        precipitationProbability = this.pop
    )
}

/**
 * Преобразует DTO [ForecastResponse] в доменную модель [FullForecastDomain].
 */
fun ForecastResponse.toDomain(): FullForecastDomain {
    return FullForecastDomain(
        forecasts = this.list.map { it.toDomain() }, // Используем маппер для каждого элемента списка
        cityName = this.city.name,
        countryCode = this.city.country,
        latitude = this.city.coord.lat,
        longitude = this.city.coord.lon,
        timezoneShiftSeconds = this.city.timezone,
        sunriseMillis = this.city.sunrise * 1000L, // API обычно в секундах
        sunsetMillis = this.city.sunset * 1000L   // API обычно в секундах
    )
}
