package ru.devsoland.drydrive.data.api.model

import kotlinx.serialization.Serializable

@Serializable
data class WindData(
    val speed: Double,
    val deg: Int,
    val gust: Double? = null // Может отсутствовать
)