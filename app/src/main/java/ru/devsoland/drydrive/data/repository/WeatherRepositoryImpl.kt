package ru.devsoland.drydrive.data.repository

import ru.devsoland.drydrive.BuildConfig
import ru.devsoland.drydrive.data.WeatherApi
import ru.devsoland.drydrive.data.mapper.* // Импортируем все мапперы
import ru.devsoland.drydrive.domain.model.CityDomain
import ru.devsoland.drydrive.domain.model.FullForecastDomain // Импорт для новой модели прогноза
import ru.devsoland.drydrive.domain.model.WeatherDomain
import ru.devsoland.drydrive.domain.repository.WeatherRepository
import ru.devsoland.drydrive.domain.model.Result
import javax.inject.Inject
import javax.inject.Singleton

@Singleton // Если используете Hilt для DI, репозиторий обычно Singleton
class WeatherRepositoryImpl @Inject constructor(
    private val weatherApi: WeatherApi
    // private val userPreferencesManager: UserPreferencesManager // Пока не используем для API ключа здесь, но может понадобиться для языка
) : WeatherRepository {

    // API ключ будет браться из BuildConfig, как и раньше во ViewModel
    // Это можно будет перенести в interceptor или передавать через UserPreferencesManager, если потребуется большая гибкость
    private val apiKey: String = BuildConfig.WEATHER_API_KEY

    override suspend fun getCurrentWeather(cityName: String, lat: Double, lon: Double, lang: String): Result<WeatherDomain> {
        return try {
            // В API getCurrentWeather сейчас нет параметров lat/lon, если они нужны для уточнения, их надо добавить в WeatherApi
            val response = weatherApi.getCurrentWeather(city = cityName, apiKey = apiKey, lang = lang)
            Result.Success(response.toDomain()) // Используем маппер CurrentWeatherResponseDto.toDomain()
        } catch (e: Exception) {
            // Здесь можно добавить более детальную обработку ошибок API, если необходимо
            Result.Error(e, e.localizedMessage)
        }
    }

    override suspend fun searchCities(query: String, limit: Int): Result<List<CityDomain>> {
        return try {
            val response = weatherApi.searchCities(query = query, limit = limit, apiKey = apiKey)
            Result.Success(response.toDomain()) // Используем маппер List<ApiCity>.toDomain()
        } catch (e: Exception) {
            Result.Error(e, e.localizedMessage)
        }
    }

    override suspend fun getForecast(lat: Double, lon: Double, lang: String): Result<FullForecastDomain> {
        return try {
            val responseDto = weatherApi.getFiveDayForecast(
                lat = lat,
                lon = lon,
                apiKey = apiKey,
                lang = lang
            )
            Result.Success(responseDto.toDomain()) // Использует toDomain из ForecastMappers.kt
        } catch (e: Exception) {
            Result.Error(e, e.localizedMessage)
        }
    }
}
