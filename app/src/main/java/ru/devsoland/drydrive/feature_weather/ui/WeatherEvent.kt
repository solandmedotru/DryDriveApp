package ru.devsoland.drydrive.feature_weather.ui

import ru.devsoland.drydrive.data.api.model.City

sealed class WeatherEvent {
    // События поиска городов
    data class SearchQueryChanged(val query: String) : WeatherEvent()
    data class CitySelectedFromSearch(val city: City, val formattedName: String) : WeatherEvent()
    object DismissCitySearchDropDown : WeatherEvent()

    // События погоды
    object RefreshWeatherClicked : WeatherEvent()
    object ClearWeatherErrorMessage : WeatherEvent() // Если нужно будет очищать ошибку из UI

    // НОВЫЕ События для диалога рекомендаций
    data class RecommendationClicked(val recommendation: Recommendation) : WeatherEvent() // Добавлено
    object DismissRecommendationDialog : WeatherEvent() // Добавлено

    object ShowSearchField : WeatherEvent() // При клике на иконку лупы
    object HideSearchFieldAndDismissDropdown : WeatherEvent() // При клике на "ОК" или при выборе города

    // Можно добавить и другие события по мере необходимости
}
