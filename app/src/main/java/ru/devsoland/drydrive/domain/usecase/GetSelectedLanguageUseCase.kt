package ru.devsoland.drydrive.domain.usecase

import kotlinx.coroutines.flow.Flow
import ru.devsoland.drydrive.common.model.AppLanguage
import ru.devsoland.drydrive.domain.repository.UserPreferencesRepository
import javax.inject.Inject

class GetSelectedLanguageUseCase @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository
) {
    operator fun invoke(): Flow<AppLanguage> {
        return userPreferencesRepository.selectedLanguage
    }
}
