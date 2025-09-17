package ru.devsoland.drydrive.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ru.devsoland.drydrive.data.preferences.UserPreferencesManager // <-- Добавлен импорт
import ru.devsoland.drydrive.data.repository.WeatherRepositoryImpl
import ru.devsoland.drydrive.domain.repository.UserPreferencesRepository // <-- Добавлен импорт
import ru.devsoland.drydrive.domain.repository.WeatherRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindWeatherRepository(impl: WeatherRepositoryImpl): WeatherRepository

    @Binds
    @Singleton
    abstract fun bindUserPreferencesRepository(
        userPreferencesManager: UserPreferencesManager // Используем UserPreferencesManager
    ): UserPreferencesRepository
}
