package ru.devsoland.drydrive.domain.usecase

import ru.devsoland.drydrive.domain.model.CityDomain
import ru.devsoland.drydrive.domain.repository.UserPreferencesRepository
import javax.inject.Inject

class SaveLastSelectedCityUseCase @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository
) {
    suspend operator fun invoke(city: CityDomain?) { // Можно передать null для сброса
        userPreferencesRepository.saveLastSelectedCity(city)
    }
}
