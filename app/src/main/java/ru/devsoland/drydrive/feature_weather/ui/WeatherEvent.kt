package ru.devsoland.drydrive.feature_weather.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import ru.devsoland.drydrive.common.model.AppLanguage
import ru.devsoland.drydrive.feature_weather.ui.model.CityDomainUiModel // <--- ИЗМЕНЕНО: Импорт новой UI модели

sealed class WeatherEvent {
    data class Recommendation(
        val id: String,
        val textResId: Int,
        val descriptionResId: Int,
        val icon: ImageVector,
        val isActive: Boolean,
        val activeColor: Color,
        val activeAlpha: Float = 1.0f,
        val inactiveAlpha: Float = 0.6f
    )

    // События поиска городов
    data class SearchQueryChanged(val query: String) : WeatherEvent()
    // ИЗМЕНЕНО: CitySelectedFromSearch теперь принимает CityDomainUiModel
    data class CitySelectedFromSearch(val city: CityDomainUiModel) : WeatherEvent()
    object DismissCitySearchDropDown : WeatherEvent()

    // События погоды
    object RefreshWeatherClicked : WeatherEvent()
    object ClearWeatherErrorMessage : WeatherEvent()

    // События для диалога рекомендаций
    data class RecommendationClicked(val recommendation: Recommendation) : WeatherEvent()
    object DismissRecommendationDialog : WeatherEvent()

    object ShowSearchField : WeatherEvent()
    object HideSearchFieldAndDismissDropdown : WeatherEvent()

    data class BottomNavItemSelected(val index: Int) : WeatherEvent()

    data class ChangeLanguage(val language: AppLanguage) : WeatherEvent()
}
