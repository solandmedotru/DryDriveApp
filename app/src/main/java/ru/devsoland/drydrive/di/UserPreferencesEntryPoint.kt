package ru.devsoland.drydrive.di // или ваш пакет для DI

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ru.devsoland.drydrive.data.preferences.UserPreferencesManager

@EntryPoint
@InstallIn(SingletonComponent::class)
interface UserPreferencesEntryPoint {
    fun getUserPreferencesManager(): UserPreferencesManager
}
    