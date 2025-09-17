package ru.devsoland.drydrive.domain.model

/**
 * Доменная модель, представляющая текущие погодные условия.
 */
data class WeatherDomain(
    val temperature: Double,
    val feelsLike: Double,
    val tempMin: Double,
    val tempMax: Double,
    val pressure: Int,
    val humidity: Int,
    val visibility: Int,
    val windSpeed: Double,
    val windDeg: Int,
    val windGust: Double? = null,
    val cloudiness: Int,
    val sunrise: Long,
    val sunset: Long,
    val timezone: Int,
    val cityNameFromApi: String,
    val weatherConditions: List<WeatherConditionDomain>
)

/**
 * Доменная модель для описания погодного условия (например, "ясно", "дождь").
 */
data class WeatherConditionDomain(
    val id: Int,             // Идентификатор погодного условия (например, 800 для "ясно")
    val main: String,        // Группа погодных параметров (Rain, Snow, Extreme и т.д.)
    val description: String, // Описание погоды внутри группы
    val icon: String         // Код иконки погоды
)
