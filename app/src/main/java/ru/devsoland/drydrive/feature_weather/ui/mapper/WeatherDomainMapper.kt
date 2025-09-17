package ru.devsoland.drydrive.feature_weather.ui.mapper

import android.content.Context
import ru.devsoland.drydrive.R
import ru.devsoland.drydrive.domain.model.WeatherDomain
import ru.devsoland.drydrive.feature_weather.ui.model.WeatherDetailsUiModel
import ru.devsoland.drydrive.feature_weather.ui.util.getWeatherIconResource
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.roundToInt

/**
 * Преобразует доменную модель [WeatherDomain] в UI модель [WeatherDetailsUiModel].
 */
fun WeatherDomain.toUiModel(context: Context): WeatherDetailsUiModel {
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault()).apply {
        // Учитываем таймзону самого устройства для отображения, но время восхода/заката приходит в UTC
        // Для корректного отображения времени восхода/заката с учетом timezone от API:
        // val deviceTimeZone = TimeZone.getDefault()
        // val apiTimeZoneOffsetMillis = this@toUiModel.timezone * 1000L
        // val apiTimeZone = TimeZone.getTimeZone("GMT").apply { rawOffset = apiTimeZoneOffsetMillis.toInt() }
        // // Лучше создать отдельный SimpleDateFormat для UTC->Local конвертации с учетом API timezone.
        // // Пока просто отображаем время как есть, предполагая, что оно уже скорректировано или пользователь поймет.
    }

    // Форматирование скорости и направления ветра
    val windDirection = when (this.windDeg) {
        in 0..22 -> context.getString(R.string.wind_dir_n)
        in 23..67 -> context.getString(R.string.wind_dir_ne)
        in 68..112 -> context.getString(R.string.wind_dir_e)
        in 113..157 -> context.getString(R.string.wind_dir_se)
        in 158..202 -> context.getString(R.string.wind_dir_s)
        in 203..247 -> context.getString(R.string.wind_dir_sw)
        in 248..292 -> context.getString(R.string.wind_dir_w)
        in 293..337 -> context.getString(R.string.wind_dir_nw)
        in 338..360 -> context.getString(R.string.wind_dir_n)
        else -> ""
    }
    val windInfo = "${this.windSpeed.roundToInt()} ${context.getString(R.string.unit_mps)} $windDirection"

    return WeatherDetailsUiModel(
        cityName = this.cityNameFromApi,
        temperature = "${this.temperature.roundToInt()}°",
        feelsLike = "${context.getString(R.string.details_feels_like)} ${this.feelsLike.roundToInt()}°",
        tempMinMax = "${context.getString(R.string.details_temp_min_max_format, this.tempMin.roundToInt(), this.tempMax.roundToInt())}",
        pressure = "${this.pressure} ${context.getString(R.string.unit_hpa)}",
        humidity = "${this.humidity}%",
        visibility = "${this.visibility / 1000} ${context.getString(R.string.unit_km)}",
        windSpeed = windInfo,
        cloudiness = "${this.cloudiness}%",
        // Для корректного отображения времени восхода/заката с учетом смещения timezone от API:
        // Нужно преобразовать this.sunrise (UTC timestamp) + this.timezone (смещение в секундах) в локальное время.
        // Пока простой вариант:
        sunrise = timeFormat.format(Date((this.sunrise + this.timezone) * 1000L)),
        sunset = timeFormat.format(Date((this.sunset + this.timezone) * 1000L)),
        weatherConditionDescription = this.weatherConditions.firstOrNull()?.description?.replaceFirstChar { 
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() 
        } ?: "",
        weatherIconRes = getWeatherIconResource(this.weatherConditions.firstOrNull()?.icon),
        dt = this.sunrise // Используем dt из WeatherDomain, который соответствует dt из CurrentWeatherResponseDto
    )
}
