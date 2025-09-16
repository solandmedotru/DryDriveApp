package ru.devsoland.drydrive.data.api.model

import kotlinx.serialization.Serializable

@Serializable
data class Weather( // Это для /data/2.5/weather
    val name: String,
    val main: Main, // Используем Main, а не MainData, если они разные
    val weather: List<WeatherInfo>, // Используем WeatherInfo
    // Добавьте сюда поля coord, если они нужны и приходят в этом запросе
    val coord: CoordData? = null // Опционально, если приходит
)