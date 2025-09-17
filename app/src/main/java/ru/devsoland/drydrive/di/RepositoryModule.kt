package ru.devsoland.drydrive.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ru.devsoland.drydrive.data.preferences.UserPreferencesRepositoryImpl // <-- ИЗМЕНЕН ИМПОРТ
import ru.devsoland.drydrive.data.repository.WeatherRepositoryImpl
import ru.devsoland.drydrive.domain.repository.UserPreferencesRepository
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
        userPreferencesRepositoryImpl: UserPreferencesRepositoryImpl // <-- ИЗМЕНЕН ТИП И ИМЯ ПАРАМЕТРА
    ): UserPreferencesRepository
}
