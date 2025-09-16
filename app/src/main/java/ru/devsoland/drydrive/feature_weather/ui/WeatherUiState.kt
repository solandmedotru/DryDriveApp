package ru.devsoland.drydrive.feature_weather.ui

import androidx.annotation.StringRes
import ru.devsoland.drydrive.data.api.model.City
import ru.devsoland.drydrive.data.api.model.Weather
import ru.devsoland.drydrive.common.model.AppLanguage

data class WeatherUiState(
    val weather: Weather? = null,
    val isLoadingWeather: Boolean = false,
    val weatherErrorMessage: String? = null,
    val searchQuery: String = "",
    val citySearchResults: List<City> = emptyList(),
    val isLoadingCities: Boolean = false,
    val citySearchErrorMessage: String? = null,
    val isSearchDropDownExpanded: Boolean = false,
    val cityForDisplay: String = "",
    val selectedCityObject: City? = null,
    val dailyForecasts: List<DisplayDayWeather> = emptyList(),
    val isLoadingForecast: Boolean = false,
    val forecastErrorMessage: String? = null,
    val currentLanguageCode: String = AppLanguage.SYSTEM.code,
    val isInitialLoading: Boolean = true,
    val recommendations: List<Recommendation> = emptyList(),
    val showRecommendationDialog: Boolean = false,
    @StringRes val recommendationDialogTitleResId: Int? = null,
    @StringRes val recommendationDialogDescriptionResId: Int? = null
)