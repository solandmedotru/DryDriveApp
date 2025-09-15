package ru.devsoland.drydrive

import android.os.Build
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.devsoland.drydrive.data.City
import ru.devsoland.drydrive.data.ForecastListItem
import ru.devsoland.drydrive.data.ForecastResponse
import ru.devsoland.drydrive.data.Weather
import ru.devsoland.drydrive.data.WeatherApi
import ru.devsoland.drydrive.data.preferences.AppLanguage
import ru.devsoland.drydrive.data.preferences.UserPreferencesManager
// import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

data class DisplayDayWeather(
    val dayShort: String,
    val iconRes: Int,
    val temperature: String
)

data class DryDriveUiState(
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
    val currentLanguageCode: String = AppLanguage.SYSTEM.code, // Initial will be overridden
    val isInitialLoading: Boolean = true
)

@HiltViewModel
class DryDriveViewModel @Inject constructor(
    private val weatherApi: WeatherApi,
    private val userPreferencesManager: UserPreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(DryDriveUiState())
    val uiState: StateFlow<DryDriveUiState> = _uiState.asStateFlow()

    private val apiKey = BuildConfig.WEATHER_API_KEY
    private var citySearchJob: Job? = null
    private var initialLoadJob: Job? = null

    private companion object {
        const val MSG_CITIES_NOT_FOUND = "Города не найдены: "
        const val MSG_CITY_SEARCH_ERROR = "Ошибка поиска: "
        const val MSG_CITY_NOT_FOUND_WEATHER = "Город не найден: "
        const val MSG_WEATHER_LOAD_ERROR = "Ошибка при загрузке погоды: "
        const val MSG_FORECAST_LOAD_ERROR = "Ошибка загрузки прогноза: "
        const val MSG_COORDINATES_NOT_FOUND = "Координаты для прогноза не найдены."
        const val DEFAULT_CITY_FALLBACK = "Moscow"
    }

    // This StateFlow is primarily for observing language changes AFTER initial load
    // or if other parts of the app need to observe it reactively via the ViewModel.
    val currentLanguage: StateFlow<AppLanguage> = userPreferencesManager.selectedLanguageFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppLanguage.defaultLanguage() // This initial value might be briefly observed
        )

    private val _recreateActivityEvent = MutableSharedFlow<Unit>(replay = 0)
    val recreateActivityEvent: SharedFlow<Unit> = _recreateActivityEvent.asSharedFlow()

    init {
        // This collector ensures uiState.currentLanguageCode is updated if language changes during app's lifecycle
        currentLanguage
            .onEach { appLang ->
                _uiState.update { currentState ->
                    if (currentState.currentLanguageCode != appLang.code) {
                        Log.d("ViewModelInit", "UiState.currentLanguageCode updated via onEach from '${currentState.currentLanguageCode}' to: '${appLang.code}'")
                        currentState.copy(currentLanguageCode = appLang.code)
                    } else {
                        currentState
                    }
                }
            }
            .launchIn(viewModelScope)

        initialLoadJob = viewModelScope.launch {
            // Get language DIRECTLY from Preferences Flow for reliable initialization
            val savedLanguage: AppLanguage = userPreferencesManager.selectedLanguageFlow.first()
            val initialLangCode = savedLanguage.code
            Log.d("ViewModelInit", "Initial lang code DIRECTLY from prefs flow: '$initialLangCode'")

            // Update uiState with this definitive initial language code immediately
            _uiState.update { it.copy(currentLanguageCode = initialLangCode, isInitialLoading = true) }
            Log.d("ViewModelInit", "UiState.currentLanguageCode now (after direct read): '${_uiState.value.currentLanguageCode}'. About to load city.")

            val lastCity = userPreferencesManager.lastSelectedCityFlow.first()
            if (lastCity != null) {
                Log.d("ViewModelInit", "Last selected city found: ${lastCity.name}. Lang for format: '$initialLangCode'")
                val formattedName = formatCityNameInternal(lastCity, initialLangCode)
                _uiState.update {
                    it.copy(
                        selectedCityObject = lastCity,
                        cityForDisplay = formattedName,
                        searchQuery = formattedName
                        // isInitialLoading will be set to false by fetchCurrentWeatherAndForecast
                    )
                }
                fetchCurrentWeatherAndForecast(lastCity, initialLangCode.ifEmpty { null })
            } else {
                Log.d("ViewModelInit", "No last selected city. Loading default: $DEFAULT_CITY_FALLBACK. Lang for search: '$initialLangCode'")
                findCityByNameAndFetchWeatherInternal(DEFAULT_CITY_FALLBACK, true, initialLangCode.ifEmpty { null })
            }
        }
    }

    private fun formatCityNameInternal(city: City, langCode: String): String {
        val displayName = when {
            langCode.isNotBlank() && city.localNames?.containsKey(langCode) == true -> city.localNames[langCode]
            city.localNames?.containsKey("en") == true -> city.localNames["en"]
            else -> city.name
        } ?: city.name
        return "$displayName, ${city.country}" + if (city.state != null) ", ${city.state}" else ""
    }

    private fun getApiLangCode(): String? {
        val langCode = _uiState.value.currentLanguageCode
        // Log.d("ViewModelGetApiLang", "getApiLangCode called. uiState.currentLanguageCode = '$langCode'")
        return if (langCode.isEmpty()) null else langCode
    }

    fun onEvent(event: DryDriveEvent) {
        when (event) {
            is DryDriveEvent.SearchQueryChanged -> handleSearchQueryChanged(event.query)
            is DryDriveEvent.CitySelectedFromSearch -> handleCitySelected(event.city, event.formattedName)
            DryDriveEvent.DismissCitySearchDropDown -> _uiState.update { it.copy(isSearchDropDownExpanded = false) }
            DryDriveEvent.RefreshWeatherClicked -> {
                _uiState.value.selectedCityObject?.let {
                    fetchCurrentWeatherAndForecast(it) // explicitApiLang = null, uses getApiLangCode
                } ?: run {
                    Log.d("ViewModelEvent", "Refresh clicked but no city selected. Loading default.")
                    findCityByNameAndFetchWeatherInternal(DEFAULT_CITY_FALLBACK, true) // explicitApiLang = null
                }
            }
            DryDriveEvent.ClearWeatherErrorMessage -> _uiState.update { it.copy(weatherErrorMessage = null, forecastErrorMessage = null) }
        }
    }

    private fun handleSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query, isSearchDropDownExpanded = query.isNotEmpty(), citySearchErrorMessage = null) }
        citySearchJob?.cancel()
        if (query.length > 2) {
            _uiState.update { it.copy(isLoadingCities = true) }
            citySearchJob = viewModelScope.launch {
                val apiLang = getApiLangCode()
                delay(500)
                try {
                    val results = withContext(Dispatchers.IO) {
                        weatherApi.searchCities(query = query, apiKey = apiKey, lang = apiLang)
                    }
                    _uiState.update {
                        it.copy(
                            citySearchResults = results,
                            isLoadingCities = false,
                            citySearchErrorMessage = if (results.isEmpty() && query.isNotBlank()) "$MSG_CITIES_NOT_FOUND'$query'" else null,
                            isSearchDropDownExpanded = results.isNotEmpty()
                        )
                    }
                } catch (e: Exception) {
                    Log.e("ViewModelSearch", "Error searching cities: ${e.message}", e)
                    _uiState.update { it.copy(isLoadingCities = false, citySearchErrorMessage = "$MSG_CITY_SEARCH_ERROR${e.message}")}
                }
            }
        } else {
            _uiState.update { it.copy(citySearchResults = emptyList(), isLoadingCities = false) }
        }
    }

    private fun handleCitySelected(city: City, formattedName: String) {
        initialLoadJob?.cancel() // Cancel initial load if user selects a city
        _uiState.update {
            it.copy(
                selectedCityObject = city,
                cityForDisplay = formattedName,
                searchQuery = formattedName,
                citySearchResults = emptyList(),
                isSearchDropDownExpanded = false,
                weatherErrorMessage = null,
                forecastErrorMessage = null,
                dailyForecasts = emptyList(), // Clear previous forecast
                isInitialLoading = false // User interaction implies initial load is done or overridden
            )
        }
        fetchCurrentWeatherAndForecast(city) // explicitApiLang = null
        viewModelScope.launch {
            userPreferencesManager.saveLastSelectedCity(city)
        }
    }

    private fun fetchCurrentWeatherAndForecast(city: City, explicitApiLang: String? = null) {
        viewModelScope.launch {
            val apiLang = explicitApiLang ?: getApiLangCode()
            Log.d("ViewModelFetch", "Fetching weather for ${city.name}. API lang: '$apiLang' (explicit: '$explicitApiLang')")

            _uiState.update {
                it.copy(isLoadingWeather = true, weather = null, weatherErrorMessage = null, isLoadingForecast = true, dailyForecasts = emptyList(), forecastErrorMessage = null)
            }
            var weatherFetchError: String? = null
            var forecastFetchError: String? = null
            var fetchedWeatherUpdate: Weather? = null

            try {
                fetchedWeatherUpdate = withContext(Dispatchers.IO) {
                    weatherApi.getWeather(city = city.name, apiKey = apiKey, lang = apiLang)
                }
            } catch (e: Exception) {
                Log.e("ViewModelFetch", "Error fetching current weather: ${e.message}", e)
                weatherFetchError = if (e.message?.contains("404") == true) "$MSG_CITY_NOT_FOUND_WEATHER${city.name}" else "$MSG_WEATHER_LOAD_ERROR${e.message}"
            }

            var processedForecastsUpdate: List<DisplayDayWeather> = emptyList()
            if (city.lat != 0.0 && city.lon != 0.0) {
                try {
                    val forecastResponse = withContext(Dispatchers.IO) {
                        weatherApi.getFiveDayForecast(lat = city.lat, lon = city.lon, apiKey = apiKey, lang = apiLang)
                    }
                    Log.d("ViewModelFetch", "Forecast API response received. Items: ${forecastResponse.list.size}")
                    processedForecastsUpdate = processForecastResponse(forecastResponse, apiLang)
                    Log.d("ViewModelFetch", "Processed ${processedForecastsUpdate.size} forecast items. First item label: ${processedForecastsUpdate.firstOrNull()?.dayShort}")
                } catch (e: Exception) {
                    Log.e("ViewModelFetch", "Error fetching/processing forecast: ${e.message}", e)
                    forecastFetchError = "$MSG_FORECAST_LOAD_ERROR${e.message}"
                }
            } else {
                Log.w("ViewModelFetch", "Skipping forecast: Lat/Lon for ${city.name} are 0.")
                forecastFetchError = MSG_COORDINATES_NOT_FOUND
            }

            _uiState.update {
                it.copy(
                    weather = fetchedWeatherUpdate ?: it.weather, // Update weather only if successfully fetched
                    dailyForecasts = processedForecastsUpdate,    // Update forecast
                    isLoadingWeather = false,
                    isLoadingForecast = false,
                    weatherErrorMessage = weatherFetchError ?: it.weatherErrorMessage, // Preserve old error if new is null
                    forecastErrorMessage = forecastFetchError ?: it.forecastErrorMessage,
                    isInitialLoading = false
                )
            }
            Log.d("ViewModelFetch", "UiState updated with weather & forecasts. Forecast count in state: ${_uiState.value.dailyForecasts.size}")
        }
    }

    private fun processForecastResponse(response: ForecastResponse, apiLanguageCode: String?): List<DisplayDayWeather> {
        val dailyData = mutableMapOf<String, MutableList<ForecastListItem>>()
        val displayLocale = when { apiLanguageCode.isNullOrEmpty() -> Locale.getDefault() else -> Locale(apiLanguageCode) }

        val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH)
        inputFormat.timeZone = TimeZone.getTimeZone("UTC")
        val dayKeyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
        dayKeyFormat.timeZone = TimeZone.getTimeZone("UTC")
        val dayOfWeekFormat = SimpleDateFormat("E", displayLocale)

        response.list.forEach { item ->
            try {
                val utcDate = inputFormat.parse(item.dtTxt)
                utcDate?.let {
                    val dayKey = dayKeyFormat.format(it)
                    dailyData.getOrPut(dayKey) { mutableListOf() }.add(item)
                }
            } catch (e: Exception) {
                Log.e("ProcessForecast", "Error parsing API date string: ${item.dtTxt}", e)
            }
        }

        val displayForecasts = mutableListOf<DisplayDayWeather>()
        val currentUtcDayKey = dayKeyFormat.format(Date())
        Log.d("ProcessForecast", "Filtering forecast. Current UTC day key: $currentUtcDayKey. Total UTC days from API: ${dailyData.size}")


        dailyData.entries
            .sortedBy { it.key }
            .filter { entry -> entry.key > currentUtcDayKey }
            .take(5)
            .forEachIndexed { index, entry ->
                val utcDayKey = entry.key
                val dayItems = entry.value
                val targetItem = dayItems.find { it.dtTxt.substring(11, 13) == "12" } ?: dayItems.firstOrNull()
                // Log.d("ProcessForecast", "Processing entry index $index (after filter): UTC DayKey = $utcDayKey")

                targetItem?.let { item ->
                    val itemUtcDate = inputFormat.parse(item.dtTxt)
                    // Log.d("ProcessForecast", "  TargetItem dt_txt (UTC): ${item.dtTxt}, Parsed itemUtcDate: $itemUtcDate")
                    val dayLabel = itemUtcDate?.let { date ->
                        dayOfWeekFormat.format(date).replaceFirstChar { char ->
                            if (char.isLowerCase()) char.titlecase(displayLocale) else char.toString()
                        }
                    } ?: "???"
                    val iconRes = mapWeatherConditionToIcon(item.weather.firstOrNull()?.main, item.weather.firstOrNull()?.icon)
                    val temperature = "${item.main.temp.toInt()}°"
                    displayForecasts.add(DisplayDayWeather(dayLabel, iconRes, temperature))
                    // Log.d("ProcessForecast", "  Added to display: DayLabel='$dayLabel', Temp='$temperature'")
                }
            }
        Log.d("ProcessForecast", "Finished processing. DisplayForecasts count: ${displayForecasts.size}")
        return displayForecasts
    }

    private fun mapWeatherConditionToIcon(condition: String?, iconCode: String?): Int {
        return when (iconCode) {
            "01d", "01n" -> R.drawable.ic_sun_filled
            "02d", "02n" -> R.drawable.ic_sun_filled
            "03d", "03n" -> R.drawable.ic_cloud
            "04d", "04n" -> R.drawable.ic_cloud
            "09d", "09n" -> R.drawable.ic_sun_filled // Consider R.drawable.ic_rain
            "10d", "10n" -> R.drawable.ic_sun_filled // Consider R.drawable.ic_sun_rain
            "11d", "11n" -> R.drawable.ic_sun_filled // Consider R.drawable.ic_thunderstorm
            "13d", "13n" -> R.drawable.ic_sun_filled // Consider R.drawable.ic_snow
            "50d", "50n" -> R.drawable.ic_sun_filled // Consider R.drawable.ic_fog
            else -> R.drawable.ic_cloud
        }
    }

    fun onLanguageSelected(language: AppLanguage) {
        viewModelScope.launch {
            val previousLangCode = _uiState.value.currentLanguageCode
            if (previousLangCode == language.code && !(language == AppLanguage.SYSTEM && previousLangCode.isNotEmpty())) {
                if (language == AppLanguage.SYSTEM && previousLangCode.isEmpty()) return@launch
                if (language != AppLanguage.SYSTEM && previousLangCode == language.code) return@launch
            }

            userPreferencesManager.saveSelectedLanguage(language)
            // uiState.currentLanguageCode will be updated by the `currentLanguage.onEach` collector

            _uiState.value.selectedCityObject?.let { city ->
                val newFormattedName = formatCityNameInternal(city, language.code)
                val langForApi = language.code.ifEmpty { null }
                _uiState.update {
                    it.copy(
                        cityForDisplay = newFormattedName,
                        searchQuery = if (it.searchQuery == it.cityForDisplay && it.selectedCityObject?.name == city.name) newFormattedName else it.searchQuery
                    )
                }
                fetchCurrentWeatherAndForecast(city, langForApi)
            } ?: run {
                if (_uiState.value.selectedCityObject == null && _uiState.value.cityForDisplay.contains(DEFAULT_CITY_FALLBACK, ignoreCase = true) ) {
                    findCityByNameAndFetchWeatherInternal(DEFAULT_CITY_FALLBACK, true, language.code.ifEmpty { null })
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                try {
                    val localeListToSet = if (language.code.isEmpty()) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(language.code)
                    AppCompatDelegate.setApplicationLocales(localeListToSet)
                } catch (e: Exception) {
                    Log.e("ViewModelLanguage", "Error setting application locales: ${e.message}", e)
                }
            }
            _recreateActivityEvent.emit(Unit)
        }
    }

    private fun findCityByNameAndFetchWeatherInternal(
        cityName: String,
        isInitialOrFallback: Boolean = false,
        explicitApiLang: String? = null
    ) {
        viewModelScope.launch {
            if (isInitialOrFallback) {
                _uiState.update { it.copy(isLoadingWeather = true, isLoadingForecast = true, isInitialLoading = true) }
            }
            val langForCitySearch = explicitApiLang ?: getApiLangCode()
            Log.d("ViewModelInternalSearch", "Searching city: '$cityName'. API lang for search: '$langForCitySearch'")

            try {
                val cities = withContext(Dispatchers.IO) {
                    weatherApi.searchCities(query = cityName, apiKey = apiKey, limit = 1, lang = langForCitySearch)
                }
                cities.firstOrNull()?.let { foundCity ->
                    val langForDisplay = explicitApiLang ?: _uiState.value.currentLanguageCode
                    val formattedName = formatCityNameInternal(foundCity, langForDisplay)
                    Log.d("ViewModelInternalSearch", "City found: ${foundCity.name}. Lang for display name: '$langForDisplay', Formatted: '$formattedName'")

                    _uiState.update {
                        it.copy(
                            selectedCityObject = foundCity,
                            cityForDisplay = formattedName,
                            searchQuery = formattedName
                        )
                    }
                    fetchCurrentWeatherAndForecast(foundCity, explicitApiLang) // Pass explicitApiLang for weather fetch
                    if (isInitialOrFallback) {
                        userPreferencesManager.saveLastSelectedCity(foundCity)
                    }
                } ?: run {
                    Log.w("ViewModelInternalSearch", "City '$cityName' not found via API.")
                    _uiState.update {
                        it.copy(
                            cityForDisplay = if(it.cityForDisplay.isEmpty()) "$MSG_CITY_NOT_FOUND_WEATHER $cityName" else it.cityForDisplay,
                            weatherErrorMessage = "$MSG_CITY_NOT_FOUND_WEATHER $cityName",
                            isLoadingWeather = false, isLoadingForecast = false, isInitialLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("ViewModelInternalSearch", "Error finding/fetching city '$cityName': ${e.message}", e)
                _uiState.update {
                    it.copy(weatherErrorMessage = "$MSG_CITY_SEARCH_ERROR ${e.message}", isLoadingWeather = false, isLoadingForecast = false, isInitialLoading = false)
                }
            }
        }
    }
}
