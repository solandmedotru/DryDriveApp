package ru.devsoland.drydrive.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ru.devsoland.drydrive.data.repository.WeatherRepositoryImpl
import ru.devsoland.drydrive.domain.repository.WeatherRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class) // Указываем, что зависимости будут жить на уровне приложения
abstract class RepositoryModule {

    @Binds
    @Singleton // WeatherRepositoryImpl уже помечен как @Singleton, здесь это подтверждает скоуп для интерфейса
    abstract fun bindWeatherRepository(impl: WeatherRepositoryImpl): WeatherRepository

    // Если у вас будут другие репозитории, их байдинги можно добавить сюда
    // @Binds
    // @Singleton
    // abstract fun bindUserPreferencesRepository(impl: UserPreferencesRepositoryImpl): UserPreferencesRepository
}
