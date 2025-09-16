package ru.devsoland.drydrive.feature_weather.mapper

import android.content.Context
import androidx.annotation.DrawableRes
import ru.devsoland.drydrive.R
import ru.devsoland.drydrive.data.api.model.ForecastListItem
import ru.devsoland.drydrive.feature_weather.ui.model.ForecastCardUiModel
// Импорт для функции getWeatherIconResource - убедитесь, что она существует или будет создана
import ru.devsoland.drydrive.feature_weather.ui.util.getWeatherIconResource 
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Преобразует DTO ForecastListItem в UI модель ForecastCardUiModel.
 *
 * @param context Контекст для доступа к строковым ресурсам.
 * @param isHighlighted Флаг, указывающий, является ли этот день "сегодняшним" для выделения.
 */
fun ForecastListItem.toForecastCardUiModel(
    context: Context,
    isHighlighted: Boolean
): ForecastCardUiModel {
    val sdfDayShort = SimpleDateFormat("E", Locale.getDefault())
    val dayShort = sdfDayShort.format(Date(this.dt * 1000L)).replaceFirstChar { it.titlecase(Locale.getDefault()) }

    val apiWeatherInfo = this.weather.firstOrNull()
    val iconCodeApi = apiWeatherInfo?.icon // *** НОВАЯ СТРОКА: Получаем код иконки от API ***

    @DrawableRes val iconRes = getWeatherIconResource(
        iconCode = iconCodeApi,
        weatherId = apiWeatherInfo?.id ?: 0
    )

    val temperature = "${this.main.temp.roundToInt()}${context.getString(R.string.unit_temperature_celsius)}"
    val feelsLike = "${this.main.feelsLike.roundToInt()}${context.getString(R.string.unit_temperature_celsius)}"
    val humidity = "${this.main.humidity}%"
    val pressure = context.getString(R.string.format_pressure_hpa, this.main.pressure.toString())

    val windSpeed = this.wind.speed.roundToInt()
    val windSpeedString = context.getString(R.string.format_wind_speed_mps, windSpeed)
    val windDirection = getWindDirectionString(this.wind.deg, context)
    val windInfo = "$windSpeedString, $windDirection"

    val visibilityValue = this.visibility / 1000.0
    val visibilityFormatted = if (visibilityValue == visibilityValue.toInt().toDouble()) {
        visibilityValue.toInt().toString()
    } else {
        String.format(Locale.US, "%.1f", visibilityValue)
    }
    val visibility = context.getString(R.string.format_visibility_km, visibilityFormatted)

    val clouds = "${this.clouds.all}%"
    val precipitationProbability = "${(this.pop * 100).roundToInt()}%"

    val weatherDescription = apiWeatherInfo?.description?.replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
    } ?: context.getString(R.string.weather_data_unavailable)

    return ForecastCardUiModel(
        id = this.dt,
        dayShort = dayShort,
        iconRes = iconRes,
        iconCodeApi = iconCodeApi, // *** НОВОЕ ПОЛЕ: Передаем код иконки от API ***
        temperature = temperature,
        isHighlighted = isHighlighted,
        feelsLike = feelsLike,
        humidity = humidity,
        pressure = pressure,
        windInfo = windInfo,
        visibility = visibility,
        clouds = clouds,
        precipitationProbability = precipitationProbability,
        weatherDescription = weatherDescription
    )
}

/**
 * Преобразует градусы направления ветра в локализованную строку (С, СВ, В и т.д.).
 */
private fun getWindDirectionString(degrees: Int, context: Context): String {
    val directions = arrayOf(
        context.getString(R.string.wind_dir_n), context.getString(R.string.wind_dir_ne),
        context.getString(R.string.wind_dir_e), context.getString(R.string.wind_dir_se),
        context.getString(R.string.wind_dir_s), context.getString(R.string.wind_dir_sw),
        context.getString(R.string.wind_dir_w), context.getString(R.string.wind_dir_nw)
    )
    val index = ((degrees.toDouble() % 360) / 45.0).roundToInt() % 8
    return directions[index]
}
