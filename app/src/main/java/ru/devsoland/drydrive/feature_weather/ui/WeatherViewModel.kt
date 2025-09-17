package ru.devsoland.drydrive.feature_weather.ui

import android.app.Application
import android.util.Log
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.* // Keep wildcard for other operators like debounce, collectLatest, etc.
import kotlinx.coroutines.flow.first // <--- ADDED EXPLICIT IMPORT
import kotlinx.coroutines.launch
import ru.devsoland.drydrive.R
import ru.devsoland.drydrive.common.model.AppLanguage
import ru.devsoland.drydrive.common.model.ThemeSetting
// import ru.devsoland.drydrive.data.preferences.UserPreferencesManager // Удалено
import ru.devsoland.drydrive.domain.model.CityDomain
import ru.devsoland.drydrive.domain.model.Result
import ru.devsoland.drydrive.domain.model.SingleForecastDomain
import ru.devsoland.drydrive.domain.usecase.*
import ru.devsoland.drydrive.feature_weather.mapper.toForecastCardUiModel
import ru.devsoland.drydrive.feature_weather.ui.mapper.toUiModel
import ru.devsoland.drydrive.feature_weather.ui.model.CityDomainUiModel
import ru.devsoland.drydrive.feature_weather.ui.model.ForecastCardUiModel
import ru.devsoland.drydrive.feature_weather.ui.model.WeatherDetailsUiModel
import ru.devsoland.drydrive.feature_weather.ui.WeatherEvent.Recommendation
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

data class WeatherUiState(
    val isLoadingCities: Boolean = false,
    val isLoadingWeather: Boolean = false,
    val isLoadingForecast: Boolean = false,
    val isInitialLoading: Boolean = true,
    val cities: List<CityDomainUiModel> = emptyList(),
    val selectedCity: CityDomainUiModel? = null,
    val weather: WeatherDetailsUiModel? = null,
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
    private val getCurrentWeatherUseCase: GetCurrentWeatherUseCase,
    private val searchCitiesUseCase: SearchCitiesUseCase,
    private val getForecastUseCase: GetForecastUseCase,
    private val getSelectedLanguageUseCase: GetSelectedLanguageUseCase, // Добавлено
    private val saveSelectedLanguageUseCase: SaveSelectedLanguageUseCase, // Добавлено
    private val getSelectedThemeUseCase: GetSelectedThemeUseCase,       // Добавлено
    private val getLastSelectedCityUseCase: GetLastSelectedCityUseCase,   // Добавлено
    private val saveLastSelectedCityUseCase: SaveLastSelectedCityUseCase,   // Добавлено
    private val application: Application
    // private val userPreferencesManager: UserPreferencesManager, // Удалено
) : AndroidViewModel(application) {

    private val tag = "WeatherViewModel"

    private val _uiState = MutableStateFlow(WeatherUiState())
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    private val _recreateActivityEvent = Channel<Unit>(Channel.BUFFERED)
    val recreateActivityEvent = _recreateActivityEvent.receiveAsFlow()

    private val searchQueryFlow = MutableStateFlow("")

    init {
        Log.d(tag, "ViewModel initialized.")
        loadInitialData()
        observeLanguageChanges()
        observeThemeChanges()
        observeSearchQueryChanges()
    }

    private fun observeSearchQueryChanges() {
        viewModelScope.launch {
            searchQueryFlow
                .debounce(2000L) // Изменено на 2000L (2 секунды)
                .collectLatest { debouncedQuery ->
                    if (debouncedQuery.length >= 2) { // Минимальная длина осталась 2
                        performCitySearch(debouncedQuery)
                    } else {
                        if (debouncedQuery.isNotEmpty()) {
                            _uiState.update {
                                it.copy(isLoadingCities = false, cities = emptyList(), citySearchErrorMessage = application.getString(R.string.city_search_too_short))
                            }
                        } else {
                            _uiState.update {
                                it.copy(isLoadingCities = false, cities = emptyList(), citySearchErrorMessage = null)
                            }
                        }
                    }
                }
        }
    }

    private suspend fun performCitySearch(query: String) {
        _uiState.update { it.copy(isLoadingCities = true, citySearchErrorMessage = null) }
        val langCodeForSearch = _uiState.value.currentLanguageCode ?: Locale.getDefault().language
        when (val result = searchCitiesUseCase(query = query)) {
            is Result.Success -> {
                _uiState.update {
                    it.copy(
                        cities = result.data.toUiModel(langCodeForSearch),
                        isLoadingCities = false,
                        citySearchErrorMessage = if (result.data.isEmpty()) application.getString(R.string.city_search_no_results) else null
                    )
                }
            }
            is Result.Error -> {
                Log.e(tag, "City search FAILED for query: '$query'. Error: ${result.message}", result.exception)
                _uiState.update {
                    it.copy(
                        isLoadingCities = false,
                        citySearchErrorMessage = result.message ?: application.getString(R.string.city_search_error)
                    )
                }
            }
        }
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            try {
                val lastCityDomain: CityDomain? = getLastSelectedCityUseCase().first()
                val currentLang: AppLanguage = getSelectedLanguageUseCase().first()
                val currentLangCode: String = currentLang.code
                val resolvedCurrentTheme: ThemeSetting = getSelectedThemeUseCase().first()

                val lastCityUiModel: CityDomainUiModel? = lastCityDomain?.toUiModel(currentLangCode)

                Log.d(tag, "Initial data: City=${lastCityUiModel?.displayName}, Lang=$currentLangCode, Theme=$resolvedCurrentTheme")

                _uiState.update {
                    it.copy(
                        selectedCity = lastCityUiModel,
                        currentLanguageCode = currentLangCode,
                        currentTheme = resolvedCurrentTheme
                    )
                }
                if (lastCityUiModel != null) {
                    fetchCurrentWeatherAndForecast(lastCityUiModel, currentLangCode)
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
            getSelectedLanguageUseCase().collect { appLang -> // Изменено
                val newLangCode = appLang.code
                val currentState = _uiState.value

                if (currentState.currentLanguageCode != newLangCode) {
                    Log.d(tag, "Language changed to: $newLangCode.")
                    _uiState.update { it.copy(currentLanguageCode = newLangCode) }

                    currentState.selectedCity?.let { currentSelectedCityUiModel ->
                        val cityDomain = CityDomain(
                            name = currentSelectedCityUiModel.originalNameFromApi ?: currentSelectedCityUiModel.displayName,
                            lat = currentSelectedCityUiModel.lat,
                            lon = currentSelectedCityUiModel.lon,
                            country = currentSelectedCityUiModel.country,
                            state = currentSelectedCityUiModel.state,
                            localNames = currentSelectedCityUiModel.allLocalNames
                        )
                        val updatedSelectedCityUiModel = cityDomain.toUiModel(newLangCode)
                        _uiState.update { it.copy(selectedCity = updatedSelectedCityUiModel) }
                        fetchCurrentWeatherAndForecast(updatedSelectedCityUiModel, newLangCode)
                    }

                    if (currentState.cities.isNotEmpty()) {
                        _uiState.update { it.copy(cities = emptyList()) }
                    }
                }
            }
        }
    }

    private fun observeThemeChanges() {
        viewModelScope.launch {
            getSelectedThemeUseCase().collect { theme -> // Изменено
                _uiState.update { it.copy(currentTheme = theme) }
            }
        }
    }

    fun onEvent(event: WeatherEvent) {
        Log.d(tag, "Processing event: $event")
        when (event) {
            is WeatherEvent.SearchQueryChanged -> {
                val query = event.query
                _uiState.update { it.copy(citySearchQuery = query) }
                searchQueryFlow.value = query
            }
            is WeatherEvent.CitySelectedFromSearch -> {
                onCitySelected(event.city)
            }
            is WeatherEvent.ChangeLanguage -> {
                viewModelScope.launch {
                    saveSelectedLanguageUseCase(event.language) // Изменено
                    _recreateActivityEvent.trySend(Unit)
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
                 _uiState.update { it.copy(showRecommendationDialog = true, recommendationDialogTitleResId = event.recommendation.textResId, recommendationDialogDescriptionResId = event.recommendation.descriptionResId) }
            }
            is WeatherEvent.DismissRecommendationDialog -> {
                _uiState.update { it.copy(showRecommendationDialog = false) }
            }
            is WeatherEvent.HideSearchFieldAndDismissDropdown -> {
                 _uiState.update { it.copy(cities = emptyList(), citySearchQuery = "", citySearchErrorMessage = null) }
                 searchQueryFlow.value = ""
            }
            else -> { }
        }
    }

    private fun onCitySelected(cityUiModel: CityDomainUiModel) {
        val currentLangCode = _uiState.value.currentLanguageCode
        Log.d(tag, "City selected: ${cityUiModel.displayName}. Lang: $currentLangCode")
        _uiState.update {
            it.copy(
                selectedCity = cityUiModel,
                cities = emptyList(),
                citySearchQuery = "",
                isInitialLoading = false,
                citySearchErrorMessage = null
            )
        }
        searchQueryFlow.value = ""
        fetchCurrentWeatherAndForecast(cityUiModel, currentLangCode)
        viewModelScope.launch {
            val cityDomainToSave = CityDomain(
                name = cityUiModel.originalNameFromApi ?: cityUiModel.displayName,
                lat = cityUiModel.lat,
                lon = cityUiModel.lon,
                country = cityUiModel.country,
                state = cityUiModel.state,
                localNames = cityUiModel.allLocalNames
            )
            saveLastSelectedCityUseCase(cityDomainToSave) // Изменено
        }
    }

    private suspend fun getApiLangCode(): String {
        return _uiState.value.currentLanguageCode ?: getSelectedLanguageUseCase().first().code // Изменено
    }

    private fun fetchCurrentWeatherAndForecast(city: CityDomainUiModel, explicitApiLang: String?) {
        Log.d(tag, "Fetching weather/forecast for ${city.displayName}. Lang: $explicitApiLang")
        viewModelScope.launch {
            val apiLang = explicitApiLang ?: getApiLangCode()
            _uiState.update {
                it.copy(
                    isLoadingWeather = true, 
                    weather = null, 
                    weatherErrorMessage = null, 
                    isLoadingForecast = true, 
                    dailyForecasts = emptyList(), 
                    forecastErrorMessage = null
                )
            }

            var weatherFetchError: String? = null
            var fetchedWeatherUiModel: WeatherDetailsUiModel? = null

            when (val weatherResult = getCurrentWeatherUseCase(cityName = city.originalNameFromApi ?: city.displayName, lat = city.lat, lon = city.lon, lang = apiLang)) {
                is Result.Success -> {
                    var tempWeatherUiModel = weatherResult.data.toUiModel(application.applicationContext)
                    tempWeatherUiModel.sunriseEpochMillis?.let {
                        tempWeatherUiModel = tempWeatherUiModel.copy(nextSunriseEpochMillis = it + (24 * 60 * 60 * 1000))
                    }
                    fetchedWeatherUiModel = tempWeatherUiModel
                }
                is Result.Error -> {
                    Log.e(tag, "Failed to fetch current weather for ${city.displayName}. Error: ${weatherResult.message}", weatherResult.exception)
                    weatherFetchError = weatherResult.message ?: application.getString(R.string.error_loading_weather)
                    if (weatherResult.message?.contains("401", ignoreCase = true) == true) {
                        weatherFetchError = application.getString(R.string.error_api_key_invalid)
                    } else if ((weatherResult.message?.contains("404", ignoreCase = true) == true) ||
                               (weatherResult.message?.contains("city not found", ignoreCase = true) == true)) {
                        weatherFetchError = application.getString(R.string.city_not_found)
                    }
                }
            }

            var forecastFetchError: String? = null
            var processedForecasts: List<ForecastCardUiModel> = emptyList()

            if (city.lat != 0.0 && city.lon != 0.0) {
                when (val forecastResult = getForecastUseCase(lat = city.lat, lon = city.lon, lang = apiLang)) {
                    is Result.Success -> {
                        val forecastDomainData = forecastResult.data
                        val currentDtForForecastHighlight = fetchedWeatherUiModel?.dt ?: (System.currentTimeMillis() / 1000L)
                        processedForecasts = processDomainForecastToUiModel(forecastDomainData.forecasts, currentDtForForecastHighlight)
                    }
                    is Result.Error -> {
                        Log.e(tag, "Failed to fetch forecast for ${city.displayName}. Error: ${forecastResult.message}", forecastResult.exception)
                        forecastFetchError = forecastResult.message ?: application.getString(R.string.error_loading_forecast) 
                        if (forecastResult.message?.contains("401", ignoreCase = true) == true) {
                            forecastFetchError = application.getString(R.string.error_api_key_invalid) 
                        } 
                    }
                }
            } else {
                Log.w(tag, "Skipping forecast: Invalid Lat/Lon for ${city.displayName}.")
                forecastFetchError = application.getString(R.string.error_invalid_lat_lon_for_forecast)
            }

            val currentRecommendations = generateRecommendations(fetchedWeatherUiModel, processedForecasts)
            
            _uiState.update {
                it.copy(
                    weather = fetchedWeatherUiModel, // Уже содержит nextSunriseEpochMillis, если sunriseEpochMillis был не null
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

    private fun processDomainForecastToUiModel(
        forecastItems: List<SingleForecastDomain>,
        currentWeatherDt: Long 
    ): List<ForecastCardUiModel> {
        if (forecastItems.isEmpty()) {
            return emptyList()
        }

        val dailyData = mutableMapOf<String, MutableList<SingleForecastDomain>>()
        val dayKeyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

        forecastItems.forEach { item ->
            val dayKey = dayKeyFormat.format(Date(item.dateTimeMillis))
            dailyData.getOrPut(dayKey) { mutableListOf() }.add(item)
        }

        val forecastCardUiModels = mutableListOf<ForecastCardUiModel>()
        val todaySdf = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val todayKeyForHighlight = todaySdf.format(Date(currentWeatherDt * 1000L))

        dailyData.entries
            .asSequence()
            .sortedBy { it.key }
            .take(5)
            .forEach { entry ->
                val representativeItem = entry.value.find { item ->
                    val hourOfDay = SimpleDateFormat("HH", Locale.ENGLISH).apply {
                        timeZone = TimeZone.getTimeZone("UTC")
                    }.format(Date(item.dateTimeMillis)).toIntOrNull()
                    hourOfDay in 11..13
                } ?: entry.value.firstOrNull()

                representativeItem?.let { item ->
                    val itemDateForHighlight = todaySdf.format(Date(item.dateTimeMillis))
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

    private fun generateRecommendations(weatherData: WeatherDetailsUiModel?, forecastData: List<ForecastCardUiModel>?): List<Recommendation> {
        val recommendations = mutableListOf<Recommendation>()
        if (weatherData == null) return emptyList()

        val currentTempString = weatherData.temperature.filter { it.isDigit() || it == '-' }
        val currentTemp = currentTempString.toDoubleOrNull() ?: 20.0

        val isHot = currentTemp > 25.0
        recommendations.add(Recommendation(id = "drink_water", textResId = if (isHot) R.string.rec_drink_water_active else R.string.rec_drink_water_default, descriptionResId = if (isHot) R.string.rec_drink_water_desc_active else R.string.rec_drink_water_desc_default, icon = Icons.Filled.LocalDrink, isActive = isHot, activeColor = Color(0xFF4FC3F7), activeAlpha = 1.0f, inactiveAlpha = 0.6f))

        val isSunnyOrFewCloudsDay = weatherData.weatherConditionDescription.contains("ясно", ignoreCase = true) || weatherData.weatherConditionDescription.contains("малооблачно", ignoreCase = true)
        val isWarmEnoughForUvConcern = currentTemp > 15.0
        val isUvActive = isSunnyOrFewCloudsDay && isWarmEnoughForUvConcern
        recommendations.add(Recommendation(id = "high_uv", textResId = if (isUvActive) R.string.rec_uv_protection_active else R.string.rec_uv_protection_default, descriptionResId = if (isUvActive) R.string.rec_uv_protection_desc_active else R.string.rec_uv_protection_desc_default, icon = Icons.Filled.WbSunny, isActive = isUvActive, activeColor = Color(0xFFFFD54F), activeAlpha = 1.0f, inactiveAlpha = 0.6f))

        val isColdForTires = currentTemp < 3.0
        recommendations.add(Recommendation(id = "check_tires", textResId = if (isColdForTires) R.string.rec_tire_change_active else R.string.rec_tire_change_default, descriptionResId = if (isColdForTires) R.string.rec_tire_change_desc_active else R.string.rec_tire_change_desc_default, icon = Icons.Filled.DirectionsCar, isActive = isColdForTires, activeColor = Color(0xFF81D4FA), activeAlpha = 1.0f, inactiveAlpha = 0.6f))

        val rainCondition = application.getString(R.string.condition_rain).lowercase(Locale.getDefault())
        val snowCondition = application.getString(R.string.condition_snow).lowercase(Locale.getDefault())

        val isRainingNow = weatherData.weatherConditionDescription.lowercase(Locale.getDefault()).contains(rainCondition, ignoreCase = false)
        val willRainSoon = forecastData?.take(2)?.any { dailyWeatherUiModel ->
            val forecastIconCode = dailyWeatherUiModel.iconCodeApi
            forecastIconCode?.startsWith("10") == true || forecastIconCode?.startsWith("09") == true
        } ?: false
        val isUmbrellaNeeded = isRainingNow || willRainSoon
        recommendations.add(Recommendation(id = "take_umbrella", textResId = if (isUmbrellaNeeded) R.string.rec_umbrella_active else R.string.rec_umbrella_default, descriptionResId = if (isUmbrellaNeeded) R.string.rec_umbrella_desc_active else R.string.rec_umbrella_desc_default, icon = Icons.Filled.Umbrella, isActive = isUmbrellaNeeded, activeColor = Color(0xFFFFB74D), activeAlpha = 1.0f, inactiveAlpha = 0.6f))

        val needsWarmClothes = currentTemp < 10.0
        recommendations.add(Recommendation(id = "dress_warmly", textResId = if (needsWarmClothes) R.string.rec_dress_warmly_active else R.string.rec_dress_warmly_default, descriptionResId = if (needsWarmClothes) R.string.rec_dress_warmly_desc_active else R.string.rec_dress_warmly_desc_default, icon = Icons.Filled.Thermostat, isActive = needsWarmClothes, activeColor = Color(0xFFB0BEC5), activeAlpha = 1.0f, inactiveAlpha = 0.6f))

        val isVeryHot = currentTemp > 35.0
        val isVeryCold = currentTemp < -15.0
        val isBadWeather = weatherData.weatherConditionDescription.contains("шторм", ignoreCase = true) ||
                           (weatherData.weatherConditionDescription.lowercase(Locale.getDefault()).contains(snowCondition, ignoreCase = false) && currentTemp < 0)
        val limitActivity = isVeryHot || isVeryCold || isBadWeather
        recommendations.add(Recommendation(id = "limit_activity", textResId = if (limitActivity) R.string.rec_limit_activity_active else R.string.rec_limit_activity_default, descriptionResId = if (limitActivity) R.string.rec_limit_activity_desc_active else R.string.rec_limit_activity_desc_default, icon = Icons.Filled.AccessibilityNew, isActive = limitActivity, activeColor = Color(0xFFEF5350), activeAlpha = 1.0f, inactiveAlpha = 0.6f))

        return recommendations
    }
}
