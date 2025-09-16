package ru.devsoland.drydrive.feature_weather.ui

import android.app.Application
import android.util.Log
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Umbrella
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.devsoland.drydrive.BuildConfig
import ru.devsoland.drydrive.R
import ru.devsoland.drydrive.common.model.AppLanguage
import ru.devsoland.drydrive.common.model.ThemeSetting
import ru.devsoland.drydrive.data.WeatherApi
import ru.devsoland.drydrive.data.api.model.City
import ru.devsoland.drydrive.data.api.model.ForecastListItem
import ru.devsoland.drydrive.data.api.model.ForecastResponse
import ru.devsoland.drydrive.data.api.model.Weather
import ru.devsoland.drydrive.data.preferences.UserPreferencesManager
import ru.devsoland.drydrive.feature_weather.mapper.toForecastCardUiModel
import ru.devsoland.drydrive.feature_weather.ui.model.ForecastCardUiModel
import ru.devsoland.drydrive.feature_weather.ui.util.getWeatherIconResource
import ru.devsoland.drydrive.feature_weather.ui.WeatherEvent.Recommendation
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class WeatherUiState(
    val isLoadingCities: Boolean = false,
    val isLoadingWeather: Boolean = false,
    val isLoadingForecast: Boolean = false,
    val isInitialLoading: Boolean = true,
    val cities: List<City> = emptyList(),
    val selectedCity: City? = null,
    val weather: Weather? = null,
    val dailyForecasts: List<ForecastCardUiModel> = emptyList(),
    val recommendations: List<Recommendation> = emptyList(),
    val citySearchQuery: String = "",
    val citySearchErrorMessage: String? = null,
    val weatherErrorMessage: String? = null,
    val forecastErrorMessage: String? = null,
    val currentLanguageCode: String? = null,
    val currentTheme: ThemeSetting = ThemeSetting.SYSTEM,
    val showRecommendationDialog: Boolean = false,
    val recommendationDialogTitleResId: Int? = null,
    val recommendationDialogDescriptionResId: Int? = null
)

@HiltViewModel
class WeatherViewModel @Inject constructor(
    private val weatherApi: WeatherApi,
    private val userPreferencesManager: UserPreferencesManager,
    private val application: Application
) : AndroidViewModel(application) {

    private val apiKey: String = BuildConfig.WEATHER_API_KEY
    private val tag = "WeatherViewModel"

    private val _uiState = MutableStateFlow(WeatherUiState())
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    private val _recreateActivityEvent = Channel<Unit>(Channel.BUFFERED)
    val recreateActivityEvent = _recreateActivityEvent.receiveAsFlow()

    private val searchQueryFlow = MutableStateFlow("")

    init {
        Log.d(tag, "ViewModel initialized. API Key: $apiKey")
        loadInitialData()
        observeLanguageChanges()
        observeThemeChanges()
        observeSearchQueryChanges()
    }

    private fun observeSearchQueryChanges() {
        viewModelScope.launch {
            searchQueryFlow
                .debounce(700L)
                .collectLatest { debouncedQuery ->
                    if (debouncedQuery.length >= 2) {
                        performApiSearch(debouncedQuery)
                    } else {
                        _uiState.update {
                            it.copy(
                                isLoadingCities = false,
                                cities = emptyList()
                            )
                        }
                    }
                }
        }
    }

    private suspend fun performApiSearch(query: String) {
        _uiState.update { it.copy(isLoadingCities = true, citySearchErrorMessage = null) }
        try {
            val citiesResult = withContext(Dispatchers.IO) {
                weatherApi.searchCities(query = query, limit = 5, apiKey = apiKey)
            }
            _uiState.update {
                it.copy(
                    cities = citiesResult,
                    isLoadingCities = false,
                    citySearchErrorMessage = if (citiesResult.isEmpty()) application.getString(R.string.city_search_no_results) else null
                )
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e(tag, "API city search FAILED for query: '$query'. Error: ${e.message}", e)
            _uiState.update {
                it.copy(
                    isLoadingCities = false,
                    citySearchErrorMessage = application.getString(R.string.city_search_error)
                )
            }
        }
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            try {
                val lastCity: City? = userPreferencesManager.lastSelectedCityFlow.first()
                val currentLang: AppLanguage = userPreferencesManager.selectedLanguageFlow.first()
                val currentLangCode: String = currentLang.code
                val resolvedCurrentTheme: ThemeSetting = userPreferencesManager.selectedThemeFlow.first()
                Log.d(tag, "Initial data: City=${lastCity?.name}, Lang=$currentLangCode, Theme=$resolvedCurrentTheme")

                _uiState.update {
                    it.copy(
                        selectedCity = lastCity,
                        currentLanguageCode = currentLangCode,
                        currentTheme = resolvedCurrentTheme
                    )
                }
                if (lastCity != null) {
                    fetchCurrentWeatherAndForecast(lastCity, currentLangCode)
                } else {
                    _uiState.update { it.copy(isInitialLoading = false) }
                }
            } catch (e: Exception) {
                Log.e(tag, "[loadInitialData] FAILED: ${e.message}", e)
                _uiState.update { it.copy(isInitialLoading = false) }
            }
        }
    }

    private fun observeLanguageChanges() {
        viewModelScope.launch {
            try {
                userPreferencesManager.selectedLanguageFlow.collect { appLang ->
                    val langCode = appLang.code
                    val currentState = _uiState.value
                    if (currentState.currentLanguageCode != langCode) {
                        Log.d(tag, "Language changed to: $langCode. Current state lang: ${currentState.currentLanguageCode}.")
                        _uiState.update { it.copy(currentLanguageCode = langCode) }
                        currentState.selectedCity?.let { city -> 
                            fetchCurrentWeatherAndForecast(city, langCode)
                        }
                    } 
                }
            } catch (e: Exception) {
                Log.e(tag, "[observeLanguageChanges] FAILED: ${e.message}", e)
            }
        }
    }

    private fun observeThemeChanges() {
        viewModelScope.launch {
            try {
                userPreferencesManager.selectedThemeFlow.collect { theme ->
                    _uiState.update { it.copy(currentTheme = theme) }
                }
            } catch (e: Exception) {
                Log.e(tag, "[observeThemeChanges] FAILED: ${e.message}", e)
            }
        }
    }

    fun onEvent(event: WeatherEvent) {
        Log.d(tag, "Processing event: $event")
        when (event) {
            is WeatherEvent.SearchQueryChanged -> {
                val query = event.query
                _uiState.update { it.copy(citySearchQuery = query) } 
                if (query.isEmpty()) {
                    _uiState.update { it.copy(cities = emptyList(), isLoadingCities = false, citySearchErrorMessage = null) }
                } else if (query.length < 2) {
                    _uiState.update { it.copy(cities = emptyList(), isLoadingCities = false, citySearchErrorMessage = application.getString(R.string.city_search_too_short)) }
                } else {
                    _uiState.update { it.copy(citySearchErrorMessage = null) }
                }
                searchQueryFlow.value = query
            }
            is WeatherEvent.CitySelectedFromSearch -> {
                onCitySelected(event.city)
            }
            is WeatherEvent.ChangeLanguage -> {
                viewModelScope.launch {
                    try {
                        userPreferencesManager.saveSelectedLanguage(event.language)
                        val sendResult = _recreateActivityEvent.trySend(Unit)
                        Log.d(tag, "Requesting activity recreate for language change. Result: $sendResult")
                    } catch (e: Exception) {
                        Log.e(tag, "ChangeLanguage event FAILED: ${e.message}", e)
                    }
                }
            }
            is WeatherEvent.DismissCitySearchDropDown -> {
                 _uiState.update { it.copy(cities = emptyList(), citySearchErrorMessage = null) }
            }
            is WeatherEvent.RefreshWeatherClicked -> {
                uiState.value.selectedCity?.let {
                    fetchCurrentWeatherAndForecast(it, uiState.value.currentLanguageCode)
                }
            }
            is WeatherEvent.ClearWeatherErrorMessage -> {
                _uiState.update { it.copy(weatherErrorMessage = null, forecastErrorMessage = null) }
            }
            is WeatherEvent.RecommendationClicked -> {
                _uiState.update {
                    it.copy(
                        showRecommendationDialog = true,
                        recommendationDialogTitleResId = event.recommendation.textResId,
                        recommendationDialogDescriptionResId = event.recommendation.descriptionResId
                    )
                }
            }
            is WeatherEvent.DismissRecommendationDialog -> {
                _uiState.update { it.copy(showRecommendationDialog = false, recommendationDialogTitleResId = null, recommendationDialogDescriptionResId = null) }
            }
            is WeatherEvent.ShowSearchField -> { /* UI handles this */ }
            is WeatherEvent.HideSearchFieldAndDismissDropdown -> {
                 _uiState.update { it.copy(cities = emptyList(), citySearchQuery = "", citySearchErrorMessage = null) }
                 searchQueryFlow.value = "" 
            }
            is WeatherEvent.BottomNavItemSelected -> { /* UI handles this */ }
        }
    }

    private fun onCitySelected(city: City) {
        val currentLangCode = _uiState.value.currentLanguageCode
        Log.d(tag, "City selected: ${city.name}. Lang: $currentLangCode")
        _uiState.update {
            it.copy(
                selectedCity = city,
                cities = emptyList(),
                citySearchQuery = "",
                isInitialLoading = false, 
                citySearchErrorMessage = null
            )
        }
        searchQueryFlow.value = "" 
        fetchCurrentWeatherAndForecast(city, currentLangCode)
        viewModelScope.launch {
            try {
                userPreferencesManager.saveLastSelectedCity(city)
            } catch (e: Exception) {
                Log.e(tag, "Failed to save last selected city ${city.name}. Error: ${e.message}", e)
            }
        }
    }

    private suspend fun getApiLangCode(): String {
        return try {
            userPreferencesManager.selectedLanguageFlow.first().code
        } catch (e: Exception) {
            Log.e(tag, "Failed to get lang code from prefs. Error: ${e.message}. Falling back to default.", e)
            Locale.getDefault().language
        }
    }

    private fun fetchCurrentWeatherAndForecast(city: City, explicitApiLang: String?) {
        Log.d(tag, "Fetching weather/forecast for ${city.name}. Lang: $explicitApiLang")
        viewModelScope.launch {
            val apiLang = explicitApiLang ?: getApiLangCode()
            _uiState.update {
                it.copy(isLoadingWeather = true, weather = null, weatherErrorMessage = null, isLoadingForecast = true, dailyForecasts = emptyList(), forecastErrorMessage = null)
            }

            var weatherFetchError: String? = null
            var forecastFetchError: String? = null
            var fetchedWeather: Weather? = null
            var processedForecasts: List<ForecastCardUiModel> = emptyList()

            try {
                fetchedWeather = withContext(Dispatchers.IO) {
                    weatherApi.getWeather(city = city.name, apiKey = apiKey, lang = apiLang)
                }
            } catch (e: Exception) {
                Log.e(tag, "Failed to fetch current weather for ${city.name}. Error: ${e.message}", e)
                weatherFetchError = if (e.message?.contains("401") == true) {
                    application.applicationContext.getString(R.string.error_api_key_invalid)
                } else if (e.message?.contains("404") == true || e.message?.contains("city not found") == true) {
                    application.applicationContext.getString(R.string.city_not_found)
                } else {
                    application.applicationContext.getString(R.string.error_loading_weather)
                }
            }

            if (city.lat != 0.0 && city.lon != 0.0) {
                try {
                    val forecastResponse = withContext(Dispatchers.IO) {
                         weatherApi.getFiveDayForecast(lat = city.lat, lon = city.lon, apiKey = apiKey, lang = apiLang)
                    }
                    processedForecasts = processForecastResponse(forecastResponse, fetchedWeather?.dt ?: (System.currentTimeMillis() / 1000))
                } catch (e: Exception) {
                    Log.e(tag, "Failed to fetch/process forecast for ${city.name}. Error: ${e.message}", e)
                    forecastFetchError = application.applicationContext.getString(R.string.error_loading_weather)
                }
            } else {
                Log.w(tag, "Skipping forecast: Invalid Lat/Lon for ${city.name}.")
            }

            val currentRecommendations = generateRecommendations(fetchedWeather, processedForecasts)

            _uiState.update {
                it.copy(
                    weather = fetchedWeather ?: it.weather,
                    dailyForecasts = processedForecasts,
                    isLoadingWeather = false,
                    isLoadingForecast = false,
                    weatherErrorMessage = weatherFetchError,
                    forecastErrorMessage = forecastFetchError,
                    isInitialLoading = false,
                    recommendations = currentRecommendations
                )
            }
        }
    }

    private fun processForecastResponse(
        response: ForecastResponse,
        currentWeatherDt: Long
    ): List<ForecastCardUiModel> {
        val dailyData = mutableMapOf<String, MutableList<ForecastListItem>>()
        val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
        }
        val dayKeyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
        }

        response.list.forEach { item ->
            try {
                inputFormat.parse(item.dtTxt)?.let { utcDate ->
                    val dayKey = dayKeyFormat.format(utcDate)
                    dailyData.getOrPut(dayKey) { mutableListOf() }.add(item)
                }
            } catch (e: Exception) {
                Log.e(tag, "Error parsing API date: ${item.dtTxt}", e)
            }
        }

        val forecastCardUiModels = mutableListOf<ForecastCardUiModel>()
        val todaySdf = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val todayKeyForHighlight = todaySdf.format(Date(currentWeatherDt * 1000L))

        dailyData.entries
            .asSequence()
            .sortedBy { it.key }
            .take(5)
            .forEach { entry ->
                val targetItem = entry.value.find { it.dtTxt.substring(11, 13) == "12" }
                    ?: entry.value.firstOrNull()

                targetItem?.let { item ->
                    val itemDateForHighlight = todaySdf.format(Date(item.dt * 1000L))
                    val isHighlighted = itemDateForHighlight == todayKeyForHighlight
                    forecastCardUiModels.add(
                        item.toForecastCardUiModel(
                            context = application.applicationContext,
                            isHighlighted = isHighlighted
                        )
                    )
                }
            }
        return forecastCardUiModels
    }

    private fun generateRecommendations(weatherData: Weather?, forecastData: List<ForecastCardUiModel>?): List<Recommendation> { 
        val recommendations = mutableListOf<Recommendation>() 
        if (weatherData == null) return emptyList() 
        val currentTemp = weatherData.main.temp

        val isHot = currentTemp > 25.0
        recommendations.add(
            Recommendation(
                id = "drink_water",
                textResId = if (isHot) R.string.rec_drink_water_active else R.string.rec_drink_water_default,
                descriptionResId = if (isHot) R.string.rec_drink_water_desc_active else R.string.rec_drink_water_desc_default,
                icon = Icons.Filled.LocalDrink,
                isActive = isHot,
                activeColor = Color(0xFF4FC3F7),
                activeAlpha = 1.0f,
                inactiveAlpha = 0.6f
            )
        )

        val weatherCondition = weatherData.weather.firstOrNull()
        val iconCode = weatherCondition?.icon
        val isSunnyOrFewCloudsDay = when (iconCode) { "01d", "02d" -> true else -> false }
        val isWarmEnoughForUvConcern = currentTemp > 15.0
        val isUvActive = isSunnyOrFewCloudsDay && isWarmEnoughForUvConcern
        recommendations.add(
            Recommendation(
                id = "high_uv",
                textResId = if (isUvActive) R.string.rec_uv_protection_active else R.string.rec_uv_protection_default,
                descriptionResId = if (isUvActive) R.string.rec_uv_protection_desc_active else R.string.rec_uv_protection_desc_default,
                icon = Icons.Filled.WbSunny,
                isActive = isUvActive,
                activeColor = Color(0xFFFFD54F),
                activeAlpha = 1.0f,
                inactiveAlpha = 0.6f
            )
        )

        val isColdForTires = currentTemp < 3.0
        recommendations.add(
            Recommendation(
                id = "check_tires",
                textResId = if (isColdForTires) R.string.rec_tire_change_active else R.string.rec_tire_change_default,
                descriptionResId = if (isColdForTires) R.string.rec_tire_change_desc_active else R.string.rec_tire_change_desc_default,
                icon = Icons.Filled.DirectionsCar,
                isActive = isColdForTires,
                activeColor = Color(0xFF81D4FA),
                activeAlpha = 1.0f,
                inactiveAlpha = 0.6f
            )
        )

        val isRainingNow = weatherData.weather.any { it.main.contains("Rain", ignoreCase = true) }

        val willRainSoon = forecastData?.take(2)?.any { dailyWeatherUiModel ->
            val forecastIconCode = dailyWeatherUiModel.iconCodeApi // *** ИЗМЕНЕНО ЗДЕСЬ ***
            forecastIconCode?.startsWith("10") == true || forecastIconCode?.startsWith("09") == true
        } ?: false
        val isUmbrellaNeeded = isRainingNow || willRainSoon
        recommendations.add(
            Recommendation(
                id = "take_umbrella",
                textResId = if (isUmbrellaNeeded) R.string.rec_umbrella_active else R.string.rec_umbrella_default,
                descriptionResId = if (isUmbrellaNeeded) R.string.rec_umbrella_desc_active else R.string.rec_umbrella_desc_default,
                icon = Icons.Filled.Umbrella,
                isActive = isUmbrellaNeeded,
                activeColor = Color(0xFFFFB74D),
                activeAlpha = 1.0f,
                inactiveAlpha = 0.6f
            )
        )
        val needsWarmClothes = currentTemp < 10.0
        recommendations.add(
            Recommendation(
                id = "dress_warmly",
                textResId = if (needsWarmClothes) R.string.rec_dress_warmly_active else R.string.rec_dress_warmly_default,
                descriptionResId = if (needsWarmClothes) R.string.rec_dress_warmly_desc_active else R.string.rec_dress_warmly_desc_default,
                icon = Icons.Filled.Thermostat,
                isActive = needsWarmClothes,
                activeColor = Color(0xFFB0BEC5),
                activeAlpha = 1.0f,
                inactiveAlpha = 0.6f
            )
        )

        val isVeryHot = currentTemp > 35.0
        val isVeryCold = currentTemp < -15.0
        val isBadWeather = weatherData.weather.any { it.main.contains("Storm", ignoreCase = true) || it.main.contains("Snow", ignoreCase = true) && currentTemp < 0 }
        val limitActivity = isVeryHot || isVeryCold || isBadWeather
        recommendations.add(
            Recommendation(
                id = "limit_activity",
                textResId = if (limitActivity) R.string.rec_limit_activity_active else R.string.rec_limit_activity_default,
                descriptionResId = if (limitActivity) R.string.rec_limit_activity_desc_active else R.string.rec_limit_activity_desc_default,
                icon = Icons.Filled.AccessibilityNew,
                isActive = limitActivity,
                activeColor = Color(0xFFEF5350),
                activeAlpha = 1.0f,
                inactiveAlpha = 0.6f
            )
        )
        return recommendations
    }
}
