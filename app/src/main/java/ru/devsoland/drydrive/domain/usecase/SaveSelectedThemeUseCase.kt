package ru.devsoland.drydrive.domain.usecase

import ru.devsoland.drydrive.common.model.ThemeSetting
import ru.devsoland.drydrive.domain.repository.UserPreferencesRepository
import javax.inject.Inject

class SaveSelectedThemeUseCase @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository
) {
    suspend operator fun invoke(theme: ThemeSetting) {
        userPreferencesRepository.saveSelectedTheme(theme)
    }
}
