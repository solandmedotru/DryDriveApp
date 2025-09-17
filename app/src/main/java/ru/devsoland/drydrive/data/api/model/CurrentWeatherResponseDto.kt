package ru.devsoland.drydrive.data.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CurrentWeatherResponseDto(
    @SerialName("coord")
    val coord: CoordDto,
    @SerialName("weather")
    val weather: List<WeatherConditionDto>,
    @SerialName("base")
    val base: String? = null, // Внутренний параметр
    @SerialName("main")
    val main: MainWeatherMetricsDto,
    @SerialName("visibility")
    val visibility: Int, // Видимость в метрах
    @SerialName("wind")
    val wind: WindDto,
    @SerialName("clouds")
    val clouds: CloudsDto,
    @SerialName("rain")
    val rain: RainDto? = null, // Опционально
    @SerialName("snow")
    val snow: SnowDto? = null, // Опционально
    @SerialName("dt")
    val dt: Long, // Время расчета данных, Unix, UTC
    @SerialName("sys")
    val sys: SysDto,
    @SerialName("timezone")
    val timezone: Int, // Сдвиг в секундах от UTC
    @SerialName("id")
    val cityId: Int, // ID города
    @SerialName("name")
    val cityName: String, // Название города
    @SerialName("cod")
    val cod: Int? = null // Код ответа API (например, 200)
)
