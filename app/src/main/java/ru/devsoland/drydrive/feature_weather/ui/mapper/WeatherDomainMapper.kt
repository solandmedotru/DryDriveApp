package ru.devsoland.drydrive.feature_weather.ui.mapper

import android.content.Context
import ru.devsoland.drydrive.R
import ru.devsoland.drydrive.domain.model.WeatherDomain
import ru.devsoland.drydrive.feature_weather.ui.model.WeatherDetailsUiModel
import ru.devsoland.drydrive.feature_weather.ui.util.getWeatherIconResource
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Преобразует доменную модель [WeatherDomain] в UI модель [WeatherDetailsUiModel].
 */
fun WeatherDomain.toUiModel(context: Context): WeatherDetailsUiModel {
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    val windDirectionStr = when (this.windDeg) {
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
    val windInfoForUi = "${this.windSpeed.roundToInt()} ${context.getString(R.string.unit_mps)} $windDirectionStr"

    val sunriseUtcEpochMillis = this.sunrise * 1000L
    val sunsetUtcEpochMillis = this.sunset * 1000L

    val currentTemperature = this.temperature.roundToInt()
    val conditionDescription = this.weatherConditions.firstOrNull()?.description
        ?.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() } ?: ""
    
    val weatherConditionDescLower = conditionDescription.lowercase(Locale.getDefault())

    val washRecommendationText = when {
        weatherConditionDescLower.contains(context.getString(R.string.condition_rain).lowercase(Locale.getDefault())) ||
        weatherConditionDescLower.contains(context.getString(R.string.condition_snow).lowercase(Locale.getDefault())) ||
        weatherConditionDescLower.contains(context.getString(R.string.condition_thunderstorm).lowercase(Locale.getDefault())) ||
        weatherConditionDescLower.contains(context.getString(R.string.condition_drizzle).lowercase(Locale.getDefault())) -> context.getString(R.string.wash_not_recommended)
        weatherConditionDescLower.contains(context.getString(R.string.condition_mist).lowercase(Locale.getDefault())) ||
        weatherConditionDescLower.contains(context.getString(R.string.condition_fog).lowercase(Locale.getDefault())) -> context.getString(R.string.wash_possible_limited_visibility)
        currentTemperature > 5 -> context.getString(R.string.wash_great_day)
        else -> context.getString(R.string.wash_good_weather_but_cool) 
    }

    // --- НОВАЯ ЛОГИКА ДЛЯ ОПРЕДЕЛЕНИЯ ПОЗИТИВНОСТИ РЕКОМЕНДАЦИИ ---
    val isWashRecommendationPositive = when (washRecommendationText) {
        context.getString(R.string.wash_great_day),
        context.getString(R.string.wash_good_weather_but_cool) -> true
        else -> false
    }
    // --- КОНЕЦ НОВОЙ ЛОГИКИ ---

    val feelsLikeValueStr = "${this.feelsLike.roundToInt()}°"

    return WeatherDetailsUiModel(
        cityName = this.cityNameFromApi ?: context.getString(R.string.city_not_found),
        temperature = "${currentTemperature}°",
        weatherConditionDescription = conditionDescription,
        weatherIconRes = getWeatherIconResource(this.weatherConditions.firstOrNull()?.icon),
        washRecommendationText = washRecommendationText.ifBlank { null }, // Обработка пустой строки
        isWashRecommendationPositive = isWashRecommendationPositive,      // ПЕРЕДАЕМ НОВЫЙ ФЛАГ
        feelsLikeValue = feelsLikeValueStr,
        humidity = "${this.humidity}%",
        windSpeed = windInfoForUi, 
        pressure = "${this.pressure} ${context.getString(R.string.unit_hpa)}",
        sunrise = timeFormat.format(Date((this.sunrise + this.timezone) * 1000L)), 
        sunset = timeFormat.format(Date((this.sunset + this.timezone) * 1000L)),   
        sunriseEpochMillis = sunriseUtcEpochMillis,
        sunsetEpochMillis = sunsetUtcEpochMillis
        // Старые поля feelsLike, tempMinMax, visibility, cloudiness, dt УДАЛЕНЫ из конструктора
        // если они не были явно присвоены выше. Если они есть в data class и имеют значения по умолчанию,
        // то все будет хорошо. Если их нет или у них нет значений по умолчанию, а они нужны,
        // их нужно либо добавить в конструктор с присвоением, либо обеспечить значения по умолчанию в data class.
    )
}
