package ru.devsoland.drydrive.data.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SnowDto(
    @SerialName("1h")
    val oneHour: Double? = null, // Объем снега за последний час, мм
    @SerialName("3h")
    val threeHours: Double? = null // Объем снега за последние 3 часа, мм (менее распространено для текущей погоды)
)
