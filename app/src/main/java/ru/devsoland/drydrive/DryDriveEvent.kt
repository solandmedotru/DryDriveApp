package ru.devsoland.drydrive // или ru.devsoland.drydrive.ui

import ru.devsoland.drydrive.data.City

sealed class DryDriveEvent {
    // События поиска городов
    data class SearchQueryChanged(val query: String) : DryDriveEvent()
    data class CitySelectedFromSearch(val city: City, val formattedName: String) : DryDriveEvent()
    object DismissCitySearchDropDown : DryDriveEvent()

    // События погоды
    object RefreshWeatherClicked : DryDriveEvent()object ClearWeatherErrorMessage : DryDriveEvent() // Если нужно будет очищать ошибку из UI

    // Можно добавить и другие события по мере необходимости
}
