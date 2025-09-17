package ru.devsoland.drydrive.data.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SysDto(
    @SerialName("type")
    val type: Int? = null, // Внутренний параметр
    @SerialName("id")
    val id: Int? = null, // Внутренний параметр
    @SerialName("country")
    val country: String? = null, // Код страны (например, "GB", "US")
    @SerialName("sunrise")
    val sunrise: Long, // Время восхода, Unix, UTC
    @SerialName("sunset")
    val sunset: Long // Время заката, Unix, UTC
)
