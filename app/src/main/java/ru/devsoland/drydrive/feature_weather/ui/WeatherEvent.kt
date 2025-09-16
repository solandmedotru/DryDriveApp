package ru.devsoland.drydrive.feature_weather.ui

import ru.devsoland.drydrive.data.City

sealed class WeatherEvent {
    // События поиска городов
    data class SearchQueryChanged(val query: String) : WeatherEvent()
    data class CitySelectedFromSearch(val city: City, val formattedName: String) : WeatherEvent()
    object DismissCitySearchDropDown : WeatherEvent()

    // События погоды
    object RefreshWeatherClicked : WeatherEvent()object ClearWeatherErrorMessage : WeatherEvent() // Если нужно будет очищать ошибку из UI

    // Можно добавить и другие события по мере необходимости
}