package ru.devsoland.drydrive.data.mapper

import ru.devsoland.drydrive.data.api.model.CurrentWeatherResponseDto
import ru.devsoland.drydrive.data.api.model.WeatherConditionDto
import ru.devsoland.drydrive.domain.model.WeatherConditionDomain
import ru.devsoland.drydrive.domain.model.WeatherDomain

/**
 * Преобразует DTO [WeatherConditionDto] в доменную модель [WeatherConditionDomain].
 */
fun WeatherConditionDto.toDomain(): WeatherConditionDomain {
    return WeatherConditionDomain(
        id = this.id,
        main = this.main,
        description = this.description,
        icon = this.icon
    )
}

/**
 * Преобразует DTO ответа API [CurrentWeatherResponseDto] в доменную модель [WeatherDomain].
 */
fun CurrentWeatherResponseDto.toDomain(): WeatherDomain {
    return WeatherDomain(
        temperature = this.main.temp,
        feelsLike = this.main.feelsLike,
        tempMin = this.main.tempMin,
        tempMax = this.main.tempMax,
        pressure = this.main.pressure,
        humidity = this.main.humidity,
        visibility = this.visibility,
        windSpeed = this.wind.speed,
        windDeg = this.wind.deg,
        windGust = this.wind.gust,
        cloudiness = this.clouds.all,
        sunrise = this.sys.sunrise,
        sunset = this.sys.sunset,
        timezone = this.timezone,
        cityNameFromApi = this.cityName,
        weatherConditions = this.weather.map { it.toDomain() }
    )
}
