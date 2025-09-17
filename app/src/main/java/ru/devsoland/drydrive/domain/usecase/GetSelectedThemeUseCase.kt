package ru.devsoland.drydrive.domain.usecase

import kotlinx.coroutines.flow.Flow
import ru.devsoland.drydrive.common.model.ThemeSetting
import ru.devsoland.drydrive.domain.repository.UserPreferencesRepository
import javax.inject.Inject

class GetSelectedThemeUseCase @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository
) {
    operator fun invoke(): Flow<ThemeSetting> {
        return userPreferencesRepository.selectedTheme
    }
}
