package ru.devsoland.drydrive.data

import kotlinx.serialization.Serializable

@Serializable
data class Weather(
    val name: String, // Название города
    val coord: Coord, // Координаты
    val main: Main,
    val weather: List<WeatherInfo>
)

@Serializable
data class Coord(
    val lon: Double, // Долгота
    val lat: Double  // Широта
)

@Serializable
data class Main(
    val temp: Double // Температура в °C
)

@Serializable
data class WeatherInfo(
    val description: String, // Описание погоды (например, "light rain")
    val main: String // Основной тип погоды (например, "Rain")
)