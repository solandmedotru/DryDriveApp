package ru.devsoland.drydrive.domain.usecase

import kotlinx.coroutines.flow.Flow
import ru.devsoland.drydrive.domain.model.CityDomain
import ru.devsoland.drydrive.domain.repository.UserPreferencesRepository
import javax.inject.Inject

class GetLastSelectedCityUseCase @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository
) {
    operator fun invoke(): Flow<CityDomain?> { // Город может отсутствовать
        return userPreferencesRepository.lastSelectedCity
    }
}
