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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.* // Для заполненных иконок
import androidx.compose.material.icons.outlined.* // Для контурных иконок (если предпочитаете)


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
        const val TAG = "DryDriveViewModel" // Добавим тэг для логов ViewModel
        const val MSG_CITIES_NOT_FOUND = "Города не найдены: "
        const val MSG_CITY_SEARCH_ERROR = "Ошибка поиска: "
        const val MSG_CITY_NOT_FOUND_WEATHER = "Город не найден: "
        const val MSG_WEATHER_LOAD_ERROR = "Ошибка при загрузке погоды: "
        const val MSG_FORECAST_LOAD_ERROR = "Ошибка загрузки прогноза: "
        const val MSG_COORDINATES_NOT_FOUND = "Координаты для прогноза не найдены."
        const val DEFAULT_CITY_FALLBACK = "Moscow"
    }

    val currentLanguage: StateFlow<AppLanguage> = userPreferencesManager.selectedLanguageFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppLanguage.defaultLanguage()
        )

    private val _recreateActivityEvent = MutableSharedFlow<Unit>(replay = 0)
    val recreateActivityEvent: SharedFlow<Unit> = _recreateActivityEvent.asSharedFlow()

    init {
        currentLanguage
            .onEach { appLang ->
                _uiState.update { currentState ->
                    if (currentState.currentLanguageCode != appLang.code) {
                        Log.d(TAG, "Language changed via collector: ${appLang.code}")
                        currentState.copy(currentLanguageCode = appLang.code)
                    } else {
                        currentState
                    }
                }
            }
            .launchIn(viewModelScope)

        initialLoadJob = viewModelScope.launch {
            val savedLanguage: AppLanguage = userPreferencesManager.selectedLanguageFlow.first()
            val initialLangCode = savedLanguage.code
            Log.d(TAG, "Init: Lang from prefs: '$initialLangCode'")

            _uiState.update {
                it.copy(
                    currentLanguageCode = initialLangCode,
                    isInitialLoading = true
                )
            }
            Log.d(
                TAG,
                "Init: UiState.currentLanguageCode set to '$_uiState.value.currentLanguageCode'. Loading city..."
            )

            val lastCity = userPreferencesManager.lastSelectedCityFlow.first()
            val apiLangForInit = initialLangCode.ifEmpty { null }

            if (lastCity != null) {
                Log.d(
                    TAG,
                    "Init: Last city found: ${lastCity.name}. Formatting with lang: '$initialLangCode'"
                )
                val formattedName = formatCityNameInternal(lastCity, initialLangCode)
                _uiState.update {
                    it.copy(
                        selectedCityObject = lastCity,
                        cityForDisplay = formattedName,
                        searchQuery = formattedName
                        // isInitialLoading is true, will be set to false by fetch function
                    )
                }
                fetchCurrentWeatherAndForecast(lastCity, apiLangForInit)
            } else {
                Log.d(
                    TAG,
                    "Init: No last city. Loading default: $DEFAULT_CITY_FALLBACK. Search lang: '$initialLangCode'"
                )
                findCityByNameAndFetchWeatherInternal(DEFAULT_CITY_FALLBACK, true, apiLangForInit)
            }
        }
    }

    private fun formatCityNameInternal(city: City, langCode: String): String {
        val displayName = when {
            langCode.isNotBlank() && city.localNames?.containsKey(langCode) == true -> city.localNames[langCode]
            city.localNames?.containsKey("en") == true -> city.localNames["en"]
            else -> city.name
        } ?: city.name // Elvis для случая, если city.name тоже null (хотя API должен его давать)
        return "$displayName, ${city.country}" + if (!city.state.isNullOrBlank()) ", ${city.state}" else ""
    }

    private fun getApiLangCode(): String? {
        val langCode = _uiState.value.currentLanguageCode
        return if (langCode.isEmpty()) null else langCode
    }

    fun onEvent(event: DryDriveEvent) {
        when (event) {
            is DryDriveEvent.SearchQueryChanged -> handleSearchQueryChanged(event.query)
            is DryDriveEvent.CitySelectedFromSearch -> handleCitySelected(
                event.city,
                event.formattedName
            )

            DryDriveEvent.DismissCitySearchDropDown -> _uiState.update {
                it.copy(
                    isSearchDropDownExpanded = false
                )
            }

            DryDriveEvent.RefreshWeatherClicked -> {
                _uiState.value.selectedCityObject?.let {
                    fetchCurrentWeatherAndForecast(it) // explicitApiLang = null
                } ?: run {
                    Log.w(TAG, "Refresh clicked but no city selected. Loading default.")
                    findCityByNameAndFetchWeatherInternal(
                        DEFAULT_CITY_FALLBACK,
                        true
                    ) // explicitApiLang = null
                }
            }

            DryDriveEvent.ClearWeatherErrorMessage -> _uiState.update {
                it.copy(
                    weatherErrorMessage = null,
                    forecastErrorMessage = null
                )
            }
        }
    }

    private fun handleSearchQueryChanged(query: String) {
        _uiState.update {
            it.copy(
                searchQuery = query,
                isSearchDropDownExpanded = query.isNotEmpty(),
                citySearchErrorMessage = null
            )
        }
        citySearchJob?.cancel()
        if (query.length > 2) {
            _uiState.update { it.copy(isLoadingCities = true) }
            citySearchJob = viewModelScope.launch {
                val apiLang = getApiLangCode()
                delay(500) // Debounce
                try {
                    val results = withContext(Dispatchers.IO) {
                        weatherApi.searchCities(query = query, apiKey = apiKey, lang = apiLang)
                    }
                    _uiState.update {
                        it.copy(
                            citySearchResults = results,
                            isLoadingCities = false,
                            citySearchErrorMessage = if (results.isEmpty()) "$MSG_CITIES_NOT_FOUND'$query'" else null,
                            isSearchDropDownExpanded = results.isNotEmpty()
                        )
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error searching cities for '$query': ${e.message}", e)
                    _uiState.update {
                        it.copy(
                            isLoadingCities = false,
                            citySearchErrorMessage = "$MSG_CITY_SEARCH_ERROR${e.localizedMessage}"
                        )
                    }
                }
            }
        } else {
            _uiState.update { it.copy(citySearchResults = emptyList(), isLoadingCities = false) }
        }
    }

    private fun handleCitySelected(city: City, formattedName: String) {
        initialLoadJob?.cancel()
        _uiState.update {
            it.copy(
                selectedCityObject = city,
                cityForDisplay = formattedName,
                searchQuery = formattedName, // Update search query to reflect selected city
                citySearchResults = emptyList(),
                isSearchDropDownExpanded = false,
                weatherErrorMessage = null,
                forecastErrorMessage = null,
                dailyForecasts = emptyList(),
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
            val callSource = if (explicitApiLang != null) "Init/LangChange" else "UserAction"
            Log.d(
                TAG,
                "Fetching weather for '${city.name}'. API Lang: '$apiLang' (Source: $callSource)"
            )

            _uiState.update {
                it.copy(
                    isLoadingWeather = true,
                    weather = null, // Clear old weather while loading
                    weatherErrorMessage = null,
                    isLoadingForecast = true,
                    dailyForecasts = emptyList(), // Clear old forecast
                    forecastErrorMessage = null
                )
            }

            var weatherFetchError: String? = null
            var forecastFetchError: String? = null
            var fetchedWeather: Weather? = null
            var processedForecasts: List<DisplayDayWeather> = emptyList()

            try {
                fetchedWeather = withContext(Dispatchers.IO) {
                    // ИСПРАВЛЕНИЕ: Вернуть именованные аргументы
                    weatherApi.getWeather(city = city.name, apiKey = apiKey, lang = apiLang)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching current weather: ${e.message}", e)
                weatherFetchError =
                    if (e.message?.contains("404") == true) "$MSG_CITY_NOT_FOUND_WEATHER${city.name}" else "$MSG_WEATHER_LOAD_ERROR${e.localizedMessage}"
            }

            if (city.lat != 0.0 && city.lon != 0.0) {
                try {
                    val forecastResponse = withContext(Dispatchers.IO) {
                        // ИСПРАВЛЕНИЕ: Вернуть именованные аргументы (если getFiveDayForecast их тоже использует)
                        // Предполагая, что getFiveDayForecast ожидает lat, lon, apiKey, lang
                        weatherApi.getFiveDayForecast(
                            lat = city.lat,
                            lon = city.lon,
                            apiKey = apiKey,
                            lang = apiLang
                        )
                    }
                    processedForecasts = processForecastResponse(forecastResponse, apiLang)
                } catch (e: Exception) {
                    Log.e(TAG, "Error fetching/processing forecast: ${e.message}", e)
                    forecastFetchError = "$MSG_FORECAST_LOAD_ERROR${e.localizedMessage}"
                }
            } else {
                Log.w(TAG, "Skipping forecast: Lat/Lon for ${city.name} are invalid.")
                forecastFetchError = MSG_COORDINATES_NOT_FOUND
            }

            _uiState.update {
                it.copy(
                    weather = fetchedWeather ?: it.weather, // Keep old weather if new fetch failed
                    dailyForecasts = processedForecasts,
                    isLoadingWeather = false,
                    isLoadingForecast = false,
                    weatherErrorMessage = weatherFetchError, // Set error if any
                    forecastErrorMessage = forecastFetchError,
                    isInitialLoading = false // Initial loading sequence is complete
                )
            }
            // Log.d(TAG, "UiState updated. Forecast count: ${_uiState.value.dailyForecasts.size}")
        }
    }

    private fun processForecastResponse(
        response: ForecastResponse,
        apiLanguageCode: String?
    ): List<DisplayDayWeather> {
        val dailyData = mutableMapOf<String, MutableList<ForecastListItem>>()
        val displayLocale = when {
            apiLanguageCode.isNullOrEmpty() -> Locale.getDefault()
            else -> Locale(apiLanguageCode)
        }

        val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val dayKeyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val dayOfWeekFormat =
            SimpleDateFormat("E", displayLocale) // Uses device's default TZ for display

        response.list.forEach { item ->
            try {
                inputFormat.parse(item.dtTxt)?.let { utcDate ->
                    val dayKey = dayKeyFormat.format(utcDate)
                    dailyData.getOrPut(dayKey) { mutableListOf() }.add(item)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing API date string: ${item.dtTxt} in forecast", e)
            }
        }

        val displayForecasts = mutableListOf<DisplayDayWeather>()
        val currentUtcDayKey = dayKeyFormat.format(Date())
        // Log.d(TAG, "Forecast Processing: Current UTC day key for filtering: $currentUtcDayKey. API UTC days: ${dailyData.size}")

        dailyData.entries
            .asSequence() // Use sequence for potentially better performance on chained operations
            .sortedBy { it.key }
            .filter { entry -> entry.key > currentUtcDayKey }
            .take(5)
            .forEach { entry ->
                val dayItems = entry.value
                val targetItem =
                    dayItems.find { it.dtTxt.substring(11, 13) == "12" } ?: dayItems.firstOrNull()

                targetItem?.let { item ->
                    inputFormat.parse(item.dtTxt)?.let { itemUtcDate ->
                        val dayLabel = dayOfWeekFormat.format(itemUtcDate).replaceFirstChar {
                            if (it.isLowerCase()) it.titlecase(displayLocale) else it.toString()
                        }
                        val iconRes = mapWeatherConditionToIcon(
                            item.weather.firstOrNull()?.main,
                            item.weather.firstOrNull()?.icon
                        )
                        val temperature = "${item.main.temp.toInt()}°"
                        displayForecasts.add(DisplayDayWeather(dayLabel, iconRes, temperature))
                    }
                }
            }
        // Log.d(TAG, "Forecast Processing: Finished. DisplayForecasts count: ${displayForecasts.size}")
        return displayForecasts
    }

    private fun mapWeatherConditionToIcon(condition: String?, iconCode: String?): Int {
        // Используем iconCode для определения базовой погоды,
        // а condition (main) может помочь в редких случаях для уточнения, но обычно iconCode достаточно.
        // Буква 'd' (day) или 'n' (night) в iconCode может использоваться для выбора разных иконок день/ночь.

        return when (iconCode) {
            // Ясно
            "01d" -> R.drawable.ic_weather_sunny // Ясное солнце (день)
            "01n" -> R.drawable.ic_weather_clear_night // Ясная луна/звезды (ночь)
            "02d" -> R.drawable.ic_weather_partly_cloudy_day // Солнце с небольшим облаком (день)
            "02n" -> R.drawable.ic_weather_partly_cloudy_night // Луна с небольшим облаком (ночь)
            "03d", "03n" -> R.drawable.ic_weather_cloudy // Просто облако (можно одно и то же для дня/ночи)
            "04d", "04n" -> R.drawable.ic_weather_very_cloudy // Более плотное облако или несколько облаков
            "09d", "09n" -> R.drawable.ic_weather_pouring_rain // Иконка сильного дождя
            "10d" -> R.drawable.ic_weather_rainy_day // Дождь с солнцем (день)
            "10n" -> R.drawable.ic_weather_rainy_night // Дождь с луной (ночь)
            "11d", "11n" -> R.drawable.ic_weather_thunderstorm // Молния
            "13d", "13n" -> R.drawable.ic_weather_snowy // Снежинка
            "50d", "50n" -> R.drawable.ic_weather_fog // Иконка тумана/дымки
            else -> R.drawable.ic_weather_cloudy // Иконка по умолчанию (например, облако)
        }
    }

    fun onLanguageSelected(language: AppLanguage) {
        viewModelScope.launch {
            val previousLangCode = _uiState.value.currentLanguageCode
            val newLangCode = language.code

            if (previousLangCode == newLangCode && !(language == AppLanguage.SYSTEM && previousLangCode.isNotEmpty())) {
                // Avoid redundant processing if system language is already applied or specific language is already set
                if (language == AppLanguage.SYSTEM && previousLangCode.isEmpty()) return@launch
                if (language != AppLanguage.SYSTEM && previousLangCode == newLangCode) return@launch
            }

            Log.d(TAG, "Language selected: ${language.name}. Code: '$newLangCode'")
            userPreferencesManager.saveSelectedLanguage(language)
            // UiState.currentLanguageCode will be updated by the `currentLanguage.onEach` collector

            val apiLangForUpdate = newLangCode.ifEmpty { null }

            _uiState.value.selectedCityObject?.let { city ->
                Log.d(TAG, "Language changed, refreshing data for ${city.name}")
                val newFormattedName =
                    formatCityNameInternal(city, newLangCode) // Use new code for display
                _uiState.update {
                    it.copy(
                        cityForDisplay = newFormattedName,
                        searchQuery = if (it.searchQuery == it.cityForDisplay && it.selectedCityObject?.name == city.name) newFormattedName else it.searchQuery
                    )
                }
                fetchCurrentWeatherAndForecast(city, apiLangForUpdate)
            } ?: run {
                // If no city is selected, but default city name is displayed, try to reload it
                if (_uiState.value.selectedCityObject == null && _uiState.value.cityForDisplay.contains(
                        DEFAULT_CITY_FALLBACK,
                        ignoreCase = true
                    )
                ) {
                    Log.d(TAG, "Language changed, no selected city, refreshing default city data.")
                    findCityByNameAndFetchWeatherInternal(
                        DEFAULT_CITY_FALLBACK,
                        true,
                        apiLangForUpdate
                    )
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                try {
                    val localeListToSet =
                        if (newLangCode.isEmpty()) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(
                            newLangCode
                        )
                    AppCompatDelegate.setApplicationLocales(localeListToSet)
                    // No need to Log.d here, AppLocaleConfig logs success/failure
                } catch (e: Exception) {
                    Log.e(TAG, "Error setting application locales: ${e.message}", e)
                }
            } else {
                // For older versions, rely on Activity recreation to apply new config
                _recreateActivityEvent.emit(Unit)
            }
        }
    }

    private fun findCityByNameAndFetchWeatherInternal(
        cityName: String,
        isInitialOrFallback: Boolean = false,
        explicitApiLang: String? = null
    ) {
        viewModelScope.launch {
            if (isInitialOrFallback) { // This implies it's part of initial loading process
                _uiState.update {
                    it.copy(
                        isLoadingWeather = true,
                        isLoadingForecast = true,
                        isInitialLoading = true
                    )
                }
            }
            val langForCitySearch = explicitApiLang ?: getApiLangCode()
            Log.d(
                TAG,
                "InternalSearch: Searching city: '$cityName'. API lang for search: '$langForCitySearch'"
            )

            try {
                val cities = withContext(Dispatchers.IO) {
                    weatherApi.searchCities(
                        query = cityName,
                        apiKey = apiKey,
                        limit = 1,
                        lang = langForCitySearch
                    )
                }
                cities.firstOrNull()?.let { foundCity ->
                    val langForDisplay = explicitApiLang
                        ?: _uiState.value.currentLanguageCode // Use current app language for display
                    val formattedName = formatCityNameInternal(foundCity, langForDisplay)
                    Log.d(
                        TAG,
                        "InternalSearch: City found: ${foundCity.name}. Display lang: '$langForDisplay', Formatted: '$formattedName'"
                    )

                    _uiState.update {
                        it.copy(
                            selectedCityObject = foundCity,
                            cityForDisplay = formattedName,
                            searchQuery = formattedName
                            // isInitialLoading will be handled by fetchCurrentWeatherAndForecast
                        )
                    }
                    fetchCurrentWeatherAndForecast(
                        foundCity,
                        explicitApiLang
                    ) // Pass explicitApiLang for weather fetch consistency
                    if (isInitialOrFallback) {
                        userPreferencesManager.saveLastSelectedCity(foundCity)
                    }
                } ?: run {
                    Log.w(TAG, "InternalSearch: City '$cityName' not found via API.")
                    _uiState.update {
                        it.copy(
                            cityForDisplay = if (it.cityForDisplay.isEmpty()) "$MSG_CITY_NOT_FOUND_WEATHER $cityName" else it.cityForDisplay,
                            weatherErrorMessage = "$MSG_CITY_NOT_FOUND_WEATHER $cityName",
                            isLoadingWeather = false,
                            isLoadingForecast = false,
                            isInitialLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(
                    TAG,
                    "InternalSearch: Error finding/fetching city '$cityName': ${e.message}",
                    e
                )
                _uiState.update {
                    it.copy(
                        weatherErrorMessage = "$MSG_CITY_SEARCH_ERROR ${e.localizedMessage}",
                        isLoadingWeather = false,
                        isLoadingForecast = false,
                        isInitialLoading = false
                    )
                }
            }
        }
    }
}
