package ru.devsoland.drydrive.data.api.model

import kotlinx.serialization.Serializable

@Serializable
data class Weather( // Это для /data/2.5/weather
    val dt: Long, // <--- ДОБАВЛЕНО ЭТО ПОЛЕ (время расчета данных, Unix, UTC)
    val name: String,
    val main: Main,
    val weather: List<WeatherInfo>,
    val coord: CoordData? = null
)