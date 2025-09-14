package ru.devsoland.drydrive.di

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import ru.devsoland.drydrive.data.WeatherApi

import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class) // Предоставляем зависимость на уровне всего приложения
object AppModule {
    private const val BASE_URL = "https://api.openweathermap.org/"
    @Provides
    @Singleton // WeatherApi будет создан один раз и переиспользован
    fun provideWeatherApi(): WeatherApi {
        // Здесь используется ваш существующий способ создания WeatherApi

        val json = Json { ignoreUnknownKeys = true }
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(WeatherApi::class.java)
    }
}