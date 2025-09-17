package ru.devsoland.drydrive.data.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RainDto(
    @SerialName("1h")
    val oneHour: Double? = null, // Объем дождя за последний час, мм
    @SerialName("3h")
    val threeHours: Double? = null // Объем дождя за последние 3 часа, мм (менее распространено для текущей погоды)
)
