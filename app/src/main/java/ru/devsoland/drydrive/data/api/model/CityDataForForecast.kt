package ru.devsoland.drydrive.data.api.model

import kotlinx.serialization.Serializable

@Serializable
data class CityDataForForecast( // Информация о городе в ответе прогноза
    val id: Int,
    val name: String,
    val coord: CoordData, // Используем CoordData
    val country: String,
    val population: Int,
    val timezone: Int, // Сдвиг в секундах от UTC
    val sunrise: Long,
    val sunset: Long
)