package ru.devsoland.drydrive.domain.model

import kotlinx.serialization.Serializable // <--- ДОБАВЛЕН ИМПОРТ

/**
 * Доменная модель, представляющая информацию о городе.
 */
@Serializable // <--- ДОБАВЛЕНА АННОТАЦИЯ
data class CityDomain(
    val name: String,
    val lat: Double,
    val lon: Double,
    val country: String,
    val state: String? = null, // Может отсутствовать
    val localNames: Map<String, String>? = null // Локализованные названия, если есть
)
