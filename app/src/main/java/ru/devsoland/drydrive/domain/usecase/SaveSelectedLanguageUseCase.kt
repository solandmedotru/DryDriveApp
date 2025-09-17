package ru.devsoland.drydrive.domain.usecase

import ru.devsoland.drydrive.common.model.AppLanguage
import ru.devsoland.drydrive.domain.repository.UserPreferencesRepository
import javax.inject.Inject

class SaveSelectedLanguageUseCase @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository
) {
    suspend operator fun invoke(language: AppLanguage) {
        userPreferencesRepository.saveSelectedLanguage(language)
    }
}
