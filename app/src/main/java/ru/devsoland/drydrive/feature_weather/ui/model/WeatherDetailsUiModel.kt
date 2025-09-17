package ru.devsoland.drydrive.feature_weather.ui.model

import androidx.annotation.DrawableRes

/**
 * UI модель, представляющая детальную информацию о текущей погоде для отображения.
 */
data class WeatherDetailsUiModel(
    // Актуальные поля, используемые новым UI (CurrentWeatherSection, WeatherHeader, CurrentWeatherDetailsPanel)
    val cityName: String,
    val temperature: String, 
    val weatherConditionDescription: String, 
    @DrawableRes val weatherIconRes: Int, 
    val washRecommendationText: String? = null, 
    val feelsLikeValue: String? = null,      
    val humidity: String,                  
    val windSpeed: String,                 
    val pressure: String,                  
    val sunrise: String,                   
    val sunset: String,                    
    val sunriseEpochMillis: Long?,
    val sunsetEpochMillis: Long?,
    val nextSunriseEpochMillis: Long? = null, // Добавлено для логики прогресс-бара дня/ночи

    // Старые/другие поля - теперь nullable с дефолтным null
    val feelsLike: String? = null,              // Старое поле "Ощущается как: X°"
    val tempMinMax: String? = null,             // Например, "Мин: 10° Макс: 15°"
    val visibility: String? = null,             // Например, "10 км"
    val cloudiness: String? = null,             // Например, "75%"
    val dt: Long? = null                        // Исходное время расчета данных
)
