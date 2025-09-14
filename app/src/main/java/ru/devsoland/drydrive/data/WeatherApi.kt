package ru.devsoland.drydrive.data // Убедитесь, что пакет тот же, что и у data классов

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApi {
    // Запрос текущей погоды (остается без изменений)
    @GET("data/2.5/weather")
    suspend fun getWeather(
        @Query("q") city: String, // Используем имя города для этого запроса
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric",
        @Query("lang") lang: String = "ru"
    ): Weather // Возвращает ваш существующий Weather data class

    // Поиск городов (остается без изменений)
    @GET("geo/1.0/direct")
    suspend fun searchCities(
        @Query("q") query: String,
        @Query("limit") limit: Int = 5,
        @Query("appid") apiKey: String
    ): List<City> // Возвращает ваш City data class

    // НОВЫЙ МЕТОД: Запрос прогноза на 5 дней / 3 часа
    // Для этого эндпоинта обычно требуются координаты lat/lon
    @GET("data/2.5/forecast")
    suspend fun getFiveDayForecast(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric",
        @Query("lang") lang: String = "ru"
    ): ForecastResponse // Возвращает новый ForecastResponse data class

    companion object {
        private const val BASE_URL = "https://api.openweathermap.org/"

        fun create(): WeatherApi {
            val json = Json {
                ignoreUnknownKeys = true // Важно для обработки ответов с полями, которые мы не ожидаем
                coerceInputValues = true // Если приходит null для non-nullable поля с дефолтным значением, будет использовано дефолтное
            }
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()
                .create(WeatherApi::class.java)
        }
    }
}
