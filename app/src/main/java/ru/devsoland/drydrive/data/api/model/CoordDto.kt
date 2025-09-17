package ru.devsoland.drydrive.data.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CoordDto(
    @SerialName("lon")
    val lon: Double,
    @SerialName("lat")
    val lat: Double
)
