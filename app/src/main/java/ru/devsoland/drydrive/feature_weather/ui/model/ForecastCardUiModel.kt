package ru.devsoland.drydrive.feature_weather.ui.model

import androidx.annotation.DrawableRes

data class ForecastCardUiModel(
    val id: Long,

    // Данные для свернутого вида
    val dayShort: String,
    @DrawableRes val iconRes: Int,
    val iconCodeApi: String?,
    // val temperature: String, // <<< УДАЛЯЕМ ЭТО
    val tempMinStr: String,             // <<< НОВОЕ ПОЛЕ: Минимальная температура (отформатированная)
    val tempMaxStr: String,             // <<< НОВОЕ ПОЛЕ: Максимальная температура (отформатированная)
    val isHighlighted: Boolean = false, // Оставляем для возможной метки "сегодня"

    // Данные для развернутого вида (уже отформатированные)
    val weatherDescription: String,
    val feelsLike: String,
    val humidity: String,
    val pressure: String,
    val windInfo: String,
    val visibility: String,
    val clouds: String,
    val precipitationProbability: String,
    val sunriseTimeStr: String? = null,
    val sunsetTimeStr: String? = null
)
