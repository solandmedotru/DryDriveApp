package ru.devsoland.drydrive.data // Убедитесь, что пакет тот же, что и у data классов

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Query
import ru.devsoland.drydrive.data.api.model.City
import ru.devsoland.drydrive.data.api.model.ForecastResponse
import ru.devsoland.drydrive.data.api.model.Weather

// Убедитесь, что импорты для Weather, City, ForecastResponse корректны,
// если они находятся в этом же пакете, явные импорты не нужны.
// import ru.devsoland.drydrive.data.api.model.Weather
// import ru.devsoland.drydrive.data.api.model.City
// import ru.devsoland.drydrive.data.api.model.ForecastResponse

interface WeatherApi {
    @GET("data/2.5/weather")
    suspend fun getWeather(
        @Query("q") city: String,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric",
        @Query("lang") lang: String? = null // ИЗМЕНЕНО: String? = null, убрано "ru" по умолчанию
    ): Weather

    @GET("geo/1.0/direct")
    suspend fun searchCities(
        @Query("q") query: String,
        @Query("limit") limit: Int = 5,
        @Query("appid") apiKey: String,
        @Query("lang") lang: String? = null // ДОБАВЛЕНО и ИЗМЕНЕНО: String? = null
    ): List<City>

    @GET("data/2.5/forecast")
    suspend fun getFiveDayForecast(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric",
        @Query("lang") lang: String? = null // ИЗМЕНЕНО: String? = null, убрано "ru" по умолчанию
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
