package ru.devsoland.drydrive.data.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ForecastListItem(
    val dt: Long, // Время расчета данных, unix, UTC
    val main: MainData, // Используем MainData, так как структура в forecast list может отличаться
    val weather: List<WeatherData>, // Используем WeatherData
    val clouds: CloudsData,
    val wind: WindData,
    val visibility: Int,
    val pop: Double, // Вероятность осадков
    @SerialName("dt_txt") val dtTxt: String // Текстовое представление времени
    // Можно добавить sys { pod: "d" or "n" } если нужно для определения дня/ночи
)