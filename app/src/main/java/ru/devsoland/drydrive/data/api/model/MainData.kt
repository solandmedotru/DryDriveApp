package ru.devsoland.drydrive.data.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MainData( // Для элементов списка в прогнозе
    val temp: Double,
    @SerialName("feels_like") val feelsLike: Double,
    @SerialName("temp_min") val tempMin: Double,
    @SerialName("temp_max") val tempMax: Double,
    val pressure: Int,
    @SerialName("sea_level") val seaLevel: Int? = null, // Могут отсутствовать
    @SerialName("grnd_level") val grndLevel: Int? = null, // Могут отсутствовать
    val humidity: Int,
    @SerialName("temp_kf") val tempKf: Double? = null // Может отсутствовать
)