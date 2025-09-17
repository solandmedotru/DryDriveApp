package ru.devsoland.drydrive.feature_weather.ui.model

import android.content.Context
import androidx.annotation.DrawableRes
import ru.devsoland.drydrive.R
import ru.devsoland.drydrive.feature_weather.ui.util.getWeatherIconResource
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * UI модель, представляющая детальную информацию о текущей погоде для отображения.
 */
data class WeatherDetailsUiModel(
    val cityName: String,
    val temperature: String,
    val feelsLike: String,
    val tempMinMax: String, // Например, "Мин: 10° Макс: 15°"
    val pressure: String,   // Например, "1012 гПа"
    val humidity: String,   // Например, "75%"
    val visibility: String, // Например, "10 км"
    val windSpeed: String,  // Например, "5 м/с СВ"
    val cloudiness: String, // Например, "75%"
    val sunrise: String,    // Например, "06:30"
    val sunset: String,     // Например, "20:45"
    val weatherConditionDescription: String,
    @DrawableRes val weatherIconRes: Int,
    val dt: Long // Исходное время расчета данных, для возможного использования (например, в рекомендациях)
)

/**
 * Вспомогательная модель для представления погодных условий в WeatherDetailsUiModel,
 * если потребуется отдельная детализация.
 * Пока что основные данные включены напрямую в WeatherDetailsUiModel.
 */
// data class WeatherConditionUiModel(
//     val description: String,
//     @DrawableRes val iconRes: Int
// )
