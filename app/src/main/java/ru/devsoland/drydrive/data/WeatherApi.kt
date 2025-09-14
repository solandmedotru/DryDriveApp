package ru.devsoland.drydrive.data

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApi {
    @GET("data/2.5/weather")        suspend fun getWeather(
        @Query("q") city: String,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric",
        @Query("lang") lang: String = "ru"
    ): Weather

    // Новый метод для поиска городов
    @GET("geo/1.0/direct")
    suspend fun searchCities(
        @Query("q") query: String,
        @Query("limit") limit: Int = 5, // Ограничиваем количество результатов
        @Query("appid") apiKey: String
    ): List<City> // Возвращаем список объектов City

    companion object {
        private const val BASE_URL = "https://api.openweathermap.org/"

        fun create(): WeatherApi {
            val json = Json { ignoreUnknownKeys = true }
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()
                .create(WeatherApi::class.java)
        }
    }
}
    