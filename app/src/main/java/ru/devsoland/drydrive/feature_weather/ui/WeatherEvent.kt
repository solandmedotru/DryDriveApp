package ru.devsoland.drydrive.feature_weather.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import ru.devsoland.drydrive.common.model.AppLanguage // Импорт для AppLanguage
import ru.devsoland.drydrive.data.api.model.City

sealed class WeatherEvent {
    // Определение Recommendation ПЕРЕМЕЩЕНО ВНУТРЬ WeatherEvent
    data class Recommendation(
        val id: String,
        val textResId: Int,
        val descriptionResId: Int,
        val icon: ImageVector,
        val isActive: Boolean,
        val activeColor: Color,
        val activeAlpha: Float = 1.0f, // ДОБАВЛЕНО
        val inactiveAlpha: Float = 0.6f // ДОБАВЛЕНО
    ) // Recommendation НЕ ДОЛЖЕН наследовать WeatherEvent

    // События поиска городов
    data class SearchQueryChanged(val query: String) : WeatherEvent()
    data class CitySelectedFromSearch(val city: City, val formattedName: String) : WeatherEvent()
    object DismissCitySearchDropDown : WeatherEvent()

    // События погоды
    object RefreshWeatherClicked : WeatherEvent()
    object ClearWeatherErrorMessage : WeatherEvent()

    // События для диалога рекомендаций
    // Теперь Recommendation - это WeatherEvent.Recommendation, что будет правильно разрешаться
    data class RecommendationClicked(val recommendation: Recommendation) : WeatherEvent()
    object DismissRecommendationDialog : WeatherEvent()

    object ShowSearchField : WeatherEvent()
    object HideSearchFieldAndDismissDropdown : WeatherEvent()

    data class BottomNavItemSelected(val index: Int) : WeatherEvent()

    // Новое событие для смены языка
    data class ChangeLanguage(val language: AppLanguage) : WeatherEvent()
}
