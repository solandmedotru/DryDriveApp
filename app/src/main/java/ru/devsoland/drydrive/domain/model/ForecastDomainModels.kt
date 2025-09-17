package ru.devsoland.drydrive.domain.model

// 1. Доменная модель для одного элемента/записи прогноза
data class SingleForecastDomain(
    val dateTimeMillis: Long,          // Время расчета данных, unix, UTC (Long)
    val tempCelsius: Double,
    val feelsLikeCelsius: Double,
    val tempMinCelsius: Double,
    val tempMaxCelsius: Double,
    val pressureHpa: Int,
    val humidityPercent: Int,
    val weatherConditionId: Int,
    val weatherCondition: String,      // e.g., "Rain", "Clouds"
    val weatherDescription: String,
    val weatherIconId: String,         // e.g., "01d"
    val cloudinessPercent: Int,
    val windSpeedMps: Double,
    val windDirectionDegrees: Int,
    val windGustMps: Double?,
    val visibilityMeters: Int,
    val precipitationProbability: Double // от 0.0 до 1.0
)

// 2. Доменная модель для всего ответа прогноза (включая список записей и информацию о городе)
data class FullForecastDomain(
    val forecasts: List<SingleForecastDomain>, // Список отдельных прогнозов
    val cityName: String,
    val countryCode: String,
    val latitude: Double,
    val longitude: Double,
    val timezoneShiftSeconds: Int,
    val sunriseMillis: Long,
    val sunsetMillis: Long
)
