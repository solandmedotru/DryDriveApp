package ru.devsoland.drydrive.data.api.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonIgnoreUnknownKeys

@Serializable
@JsonIgnoreUnknownKeys // Важно для вложенных объектов, где могут быть лишние поля
data class WeatherData( // Для элементов списка в прогнозе
    val id: Int,
    val main: String, // "Rain", "Clouds"
    val description: String,
    val icon: String // Код иконки, например "01d"
)