package ru.devsoland.drydrive.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonIgnoreUnknownKeys

@Serializable
data class Weather(
    val name: String, // Название города
    val main: Main,
    val weather: List<WeatherInfo>
)

@Serializable
data class Main(
    val temp: Double // Температура в °C
)

@Serializable
@JsonIgnoreUnknownKeys // Игнорировать неизвестные ключи в WeatherInfo
data class WeatherInfo(
    val description: String, // Описание погоды
    val main: String // Основной тип погоды
)

@Serializable
data class City(
    val name: String,
    val lat: Double,
    val lon: Double,
    val country: String,
    val state: String? = null // Поле state может отсутствовать
)