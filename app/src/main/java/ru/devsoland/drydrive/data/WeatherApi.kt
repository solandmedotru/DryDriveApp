package ru.devsoland.drydrive.data

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Query
import ru.devsoland.drydrive.data.api.model.City // DTO для поиска городов
import ru.devsoland.drydrive.data.api.model.CurrentWeatherResponseDto // Новый DTO для текущей погоды
import ru.devsoland.drydrive.data.api.model.ForecastResponse // DTO для прогноза (пока оставляем)

interface WeatherApi {
    @GET("data/2.5/weather")
    suspend fun getCurrentWeather(
        @Query("q") city: String,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric",
        @Query("lang") lang: String? = null
    ): CurrentWeatherResponseDto // <--- ИЗМЕНЕНО ЗДЕСЬ

    @GET("geo/1.0/direct")
    suspend fun searchCities(
        @Query("q") query: String,
        @Query("limit") limit: Int = 5,
        @Query("appid") apiKey: String,
        @Query("lang") lang: String? = null
    ): List<City> // Оставляем City, так как это DTO ответа geo/1.0/direct

    @GET("data/2.5/forecast")
    suspend fun getFiveDayForecast(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric",
        @Query("lang") lang: String? = null
    ): ForecastResponse

    companion object {
        private const val BASE_URL = "https://api.openweathermap.org/"

        fun create(): WeatherApi {
            val json = Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
            }
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()
                .create(WeatherApi::class.java)
        }
    }
}
