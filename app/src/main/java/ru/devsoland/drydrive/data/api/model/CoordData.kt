package ru.devsoland.drydrive.data.api.model

import kotlinx.serialization.Serializable

@Serializable
data class CoordData( // Общий для разных ответов
    val lat: Double,
    val lon: Double
)