package ru.devsoland.drydrive.feature_weather.ui.model

import androidx.annotation.DrawableRes

/**
 * Модель данных для отображения карточки прогноза погоды в UI.
 * Содержит уже отформатированные строки и необходимые ресурсы.
 */
data class ForecastCardUiModel(
    // Уникальный идентификатор, может быть полезен для ключей в Lazy списках
    val id: Long, // Обычно это dt (timestamp) из ForecastListItem

    // Данные для свернутого вида
    val dayShort: String,               // Краткое название дня (напр., "Пн")
    @DrawableRes val iconRes: Int,      // ID ресурса drawable для иконки погоды
    val iconCodeApi: String?,           // *** НОВОЕ ПОЛЕ: Код иконки от API (напр., "10d") ***
    val temperature: String,            // Основная температура (напр., "25°")
    val isHighlighted: Boolean = false, // Выделять ли карточку (напр., для "сегодня")

    // Данные для развернутого вида (уже отформатированные)
    val feelsLike: String,              // Ощущается как (напр., "23°")
    val humidity: String,               // Влажность (напр., "65%")
    val pressure: String,               // Давление (напр., "1012 гПа")
    val windInfo: String,               // Информация о ветре (напр., "5 м/с, СЗ")
    val visibility: String,             // Видимость (напр., "10 км")
    val clouds: String,                 // Облачность (напр., "75%")
    val precipitationProbability: String, // Вероятность осадков (напр., "20%")
    val weatherDescription: String      // Подробное описание погоды (напр., "Небольшой дождь")
)
