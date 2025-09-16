package ru.devsoland.drydrive.data.api.model

import kotlinx.serialization.Serializable

@Serializable
data class ForecastResponse( // Главный класс для ответа на запрос прогноза
    val cod: String,
    val message: Int,
    val cnt: Int,
    val list: List<ForecastListItem>,
    val city: CityDataForForecast // Используем специфичное имя, чтобы не путать с City для поиска
)