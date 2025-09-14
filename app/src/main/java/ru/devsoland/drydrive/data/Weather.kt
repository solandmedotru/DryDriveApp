package ru.devsoland.drydrive.data


import kotlinx.serialization.Serializable

@Serializable
data class Weather(
    val main: Main,
    val weather: List<WeatherInfo>
)

@Serializable
data class Main(
    val temp: Double // Температура в °C
)

@Serializable
data class WeatherInfo(
    val description: String, // Описание погоды (например, "rain", "clear")
    val main: String // Основной тип погоды (например, "Rain", "Clear")
)