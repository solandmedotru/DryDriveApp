package ru.devsoland.drydrive.di

// import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory // Не нужен здесь больше
import dagger.Module
// import dagger.Provides // Не нужен здесь больше
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
// import kotlinx.serialization.json.Json // Не нужен здесь больше
// import okhttp3.MediaType.Companion.toMediaType // Не нужен здесь больше
// import retrofit2.Retrofit // Не нужен здесь больше
// import ru.devsoland.drydrive.data.WeatherApi // Не нужен здесь больше

// import javax.inject.Singleton // Не нужен здесь больше

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    // private const val BASE_URL = "https://api.openweathermap.org/" // Удалено, есть в NetworkModule

    // Метод provideWeatherApi() удален, так как он дублировался в NetworkModule
    // и NetworkModule предоставляет его более корректно, используя предоставленный Retrofit.
}
