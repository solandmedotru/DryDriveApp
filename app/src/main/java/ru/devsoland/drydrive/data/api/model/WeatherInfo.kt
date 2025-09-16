package ru.devsoland.drydrive.data.api.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonIgnoreUnknownKeys

@Serializable
@JsonIgnoreUnknownKeys // Игнорировать неизвестные ключи в WeatherInfo
data class WeatherInfo( // Для /data/2.5/weather
    val description: String,
    val main: String, // Основной тип погоды (Rain, Snow, Clouds и т.д.)
    val icon: String? = null // Добавим поле icon, оно есть в ответе API
)