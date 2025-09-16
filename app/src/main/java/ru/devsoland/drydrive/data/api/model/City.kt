package ru.devsoland.drydrive.data.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class City(
    val name: String, // Может быть на языке по умолчанию от API
    val lat: Double,
    val lon: Double,
    val country: String,
    val state: String? = null,
    @SerialName("local_names")
    val localNames: Map<String, String>? = null // Карта локализованных имен { "en": "London", "ru": "Лондон" }
)