package ru.devsoland.drydrive.feature_weather.ui.util

import androidx.annotation.DrawableRes
import ru.devsoland.drydrive.R // Убедитесь, что R импортируется корректно

/**
 * Определяет ресурс drawable для иконки погоды на основе кода иконки от API.
 *
 * @param iconCode Код иконки, полученный от погодного API (например, "01d", "02n").
 * @param weatherId Опциональный ID погоды от API (на данный момент не используется в логике,
 *                  но может быть полезен для более детального маппинга в будущем).
 * @return ID ресурса drawable для соответствующей иконки.
 */
@DrawableRes
fun getWeatherIconResource(iconCode: String?, weatherId: Int = 0): Int {
    // weatherId пока не используется, но оставлен для возможного расширения
    return when (iconCode) {
        "01d" -> R.drawable.ic_sun_filled
        "01n" -> R.drawable.ic_moon_filled
        "02d" -> R.drawable.ic_sun_cloud_filled
        "02n" -> R.drawable.ic_moon_cloud_filled
        "03d", "03n" -> R.drawable.ic_light_cloud
        "04d", "04n" -> R.drawable.ic_clouds_filled
        "09d", "09n" -> R.drawable.ic_rain_heavy_filled
        "10d" -> R.drawable.ic_sun_rain_filled
        "10n" -> R.drawable.ic_moon_rain_filled
        "11d", "11n" -> R.drawable.ic_thunderstorm_filled
        "13d", "13n" -> R.drawable.ic_snow_filled
        "50d", "50n" -> R.drawable.ic_fog_filled
        else -> {
            // Можно добавить логгирование для неизвестных iconCode, если это необходимо
            // Log.w("WeatherIconUtils", "Unknown weather icon code: $iconCode, using default.")
            R.drawable.ic_sun_cloud_filled // Иконка по умолчанию
        }
    }
}
