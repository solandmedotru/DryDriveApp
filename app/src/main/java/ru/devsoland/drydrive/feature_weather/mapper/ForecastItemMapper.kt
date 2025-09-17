package ru.devsoland.drydrive.feature_weather.mapper

import android.content.Context
import androidx.annotation.DrawableRes
import ru.devsoland.drydrive.R
import ru.devsoland.drydrive.data.api.model.ForecastListItem
import ru.devsoland.drydrive.domain.model.SingleForecastDomain
import ru.devsoland.drydrive.feature_weather.ui.model.ForecastCardUiModel
import ru.devsoland.drydrive.feature_weather.ui.util.getWeatherIconResource
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

// Объявляем форматтер один раз для переиспользования
private val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())

/**
 * Преобразует DTO ForecastListItem в UI модель ForecastCardUiModel.
 *
 * @param context Контекст для доступа к строковым ресурсам.
 * @param isHighlighted Флаг, указывающий, является ли этот день \"сегодняшним\" для выделения.
 * @param sunriseEpochMillis Время восхода в миллисекундах UTC (опционально).
 * @param sunsetEpochMillis Время заката в миллисекундах UTC (опционально).
 */
fun ForecastListItem.toForecastCardUiModel(
    context: Context,
    isHighlighted: Boolean,
    sunriseEpochMillis: Long? = null, // Новый параметр
    sunsetEpochMillis: Long? = null   // Новый параметр
): ForecastCardUiModel {
    val sdfDayShort = SimpleDateFormat("E", Locale.getDefault())
    val dayShort = sdfDayShort.format(Date(this.dt * 1000L)).replaceFirstChar { it.titlecase(Locale.getDefault()) }

    val apiWeatherInfo = this.weather.firstOrNull()
    val iconCodeApi = apiWeatherInfo?.icon

    @DrawableRes val iconRes = getWeatherIconResource(
        iconCode = iconCodeApi,
        weatherId = apiWeatherInfo?.id ?: 0
    )

    val tempMinStr = "${this.main.tempMin.roundToInt()}${context.getString(R.string.unit_temperature_celsius)}"
    val tempMaxStr = "${this.main.tempMax.roundToInt()}${context.getString(R.string.unit_temperature_celsius)}"

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

    // Форматируем время восхода и заката, если они переданы
    val sunriseStr = sunriseEpochMillis?.let { timeFormatter.format(Date(it * 1000L)) } // Предполагаем, что millis уже UTC, как и dt
    val sunsetStr = sunsetEpochMillis?.let { timeFormatter.format(Date(it * 1000L)) }   // Если они уже в локальной TZ, * 1000L не нужно

    return ForecastCardUiModel(
        id = this.dt,
        dayShort = dayShort,
        iconRes = iconRes,
        iconCodeApi = iconCodeApi,
        tempMinStr = tempMinStr,
        tempMaxStr = tempMaxStr,
        isHighlighted = isHighlighted,
        feelsLike = feelsLike,
        humidity = humidity,
        pressure = pressure,
        windInfo = windInfo,
        visibility = visibility,
        clouds = clouds,
        precipitationProbability = precipitationProbability,
        weatherDescription = weatherDescription,
        sunriseTimeStr = sunriseStr, // Новое поле
        sunsetTimeStr = sunsetStr    // Новое поле
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

/**
 * Преобразует доменную модель SingleForecastDomain в UI модель ForecastCardUiModel.
 *
 * @param context Контекст для доступа к строковым ресурсам.
 * @param isHighlighted Флаг, указывающий, является ли этот день \"сегодняшним\" для выделения.
 * @param sunriseEpochMillis Время восхода в миллисекундах UTC (опционально).
 * @param sunsetEpochMillis Время заката в миллисекундах UTC (опционально).
 */
fun SingleForecastDomain.toForecastCardUiModel(
    context: Context,
    isHighlighted: Boolean,
    sunriseEpochMillis: Long? = null, // Новый параметр
    sunsetEpochMillis: Long? = null   // Новый параметр
): ForecastCardUiModel {
    val sdfDayShort = SimpleDateFormat("E", Locale.getDefault())
    // dateTimeMillis в SingleForecastDomain уже в миллисекундах
    val dayShort = sdfDayShort.format(Date(this.dateTimeMillis)).replaceFirstChar { it.titlecase(Locale.getDefault()) }

    val iconCodeApi = this.weatherIconId

    @DrawableRes val iconRes = getWeatherIconResource(
        iconCode = iconCodeApi,
        weatherId = this.weatherConditionId
    )

    val tempMinStr = "${this.tempMinCelsius.roundToInt()}${context.getString(R.string.unit_temperature_celsius)}"
    val tempMaxStr = "${this.tempMaxCelsius.roundToInt()}${context.getString(R.string.unit_temperature_celsius)}"

    val feelsLike = "${this.feelsLikeCelsius.roundToInt()}${context.getString(R.string.unit_temperature_celsius)}"
    val humidity = "${this.humidityPercent}%"
    val pressure = context.getString(R.string.format_pressure_hpa, this.pressureHpa.toString())

    val windSpeed = this.windSpeedMps.roundToInt()
    val windSpeedString = context.getString(R.string.format_wind_speed_mps, windSpeed)
    val windDirection = getWindDirectionString(this.windDirectionDegrees, context)
    val windInfo = "$windSpeedString, $windDirection"
    
    val visibilityValue = this.visibilityMeters / 1000.0
    val visibilityFormatted = if (visibilityValue == visibilityValue.toInt().toDouble()) {
        visibilityValue.toInt().toString()
    } else {
        String.format(Locale.US, "%.1f", visibilityValue)
    }
    val visibility = context.getString(R.string.format_visibility_km, visibilityFormatted)

    val clouds = "${this.cloudinessPercent}%"
    val precipitationProbability = "${(this.precipitationProbability * 100).roundToInt()}%" 

    val weatherDescriptionText = this.weatherDescription.replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
    }

    // Форматируем время восхода и заката, если они переданы
    // Предполагаем, что sunriseEpochMillis/sunsetEpochMillis передаются как millis UTC
    // Если они уже в локальной TZ, * 1000L не нужно для них, но для Date() все равно нужны millis.
    // Убедитесь, что передаваемые Long это именно миллисекунды.
    val sunriseStr = sunriseEpochMillis?.let { timeFormatter.format(Date(it)) }
    val sunsetStr = sunsetEpochMillis?.let { timeFormatter.format(Date(it)) }

    return ForecastCardUiModel(
        id = this.dateTimeMillis / 1000L, // id остается в секундах, если так было задумано
        dayShort = dayShort,
        iconRes = iconRes,
        iconCodeApi = iconCodeApi,
        tempMinStr = tempMinStr,
        tempMaxStr = tempMaxStr,
        isHighlighted = isHighlighted,
        feelsLike = feelsLike,
        humidity = humidity,
        pressure = pressure,
        windInfo = windInfo,
        visibility = visibility,
        clouds = clouds,
        precipitationProbability = precipitationProbability,
        weatherDescription = weatherDescriptionText,
        sunriseTimeStr = sunriseStr, // Новое поле
        sunsetTimeStr = sunsetStr    // Новое поле
    )
}

