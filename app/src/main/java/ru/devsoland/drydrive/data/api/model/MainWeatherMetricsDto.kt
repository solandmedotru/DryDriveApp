package ru.devsoland.drydrive.data.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MainWeatherMetricsDto(
    @SerialName("temp")
    val temp: Double,
    @SerialName("feels_like")
    val feelsLike: Double,
    @SerialName("temp_min")
    val tempMin: Double,
    @SerialName("temp_max")
    val tempMax: Double,
    @SerialName("pressure")
    val pressure: Int,
    @SerialName("humidity")
    val humidity: Int,
    @SerialName("sea_level")
    val seaLevel: Int? = null, // Опционально
    @SerialName("grnd_level")
    val groundLevel: Int? = null // Опционально
)
