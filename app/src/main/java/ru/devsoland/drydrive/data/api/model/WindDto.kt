package ru.devsoland.drydrive.data.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WindDto(
    @SerialName("speed")
    val speed: Double,
    @SerialName("deg")
    val deg: Int,
    @SerialName("gust")
    val gust: Double? = null // Опционально
)
