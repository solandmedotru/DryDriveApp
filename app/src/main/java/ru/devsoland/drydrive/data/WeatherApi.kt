package ru.devsoland.drydrive.data

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType // Make sure this import is present
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory // Corrected import
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface WeatherApi {
    @GET("data/2.5/weather")
    suspend fun getWeather(
        @Query("q") city: String = "Moscow", // Город как параметр
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric" // Температура в °C
    ): Weather

    @GET("geo/1.0/direct")
    suspend fun searchCities(
        @Query("q") query: String, // Поисковый запрос (название города)
        @Query("limit") limit: Int = 5, // Ограничение результатов
        @Query("appid") apiKey: String
    ): List<City> // Список городов

    companion object {
        private const val BASE_URL = "https://api.openweathermap.org/"

        fun create(): WeatherApi {
            val json = Json {
                ignoreUnknownKeys = true
            }
            val client = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()

            return retrofit.create(WeatherApi::class.java)
        }
    }
}