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
// import java.util.Calendar // Не используется активно, можно удалить если нет других применений
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
    val currentLanguageCode: String = AppLanguage.SYSTEM.code,
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
                    currentState.copy(currentLanguageCode = appLang.code)
                }
                Log.d("ViewModelInit", "UiState.currentLanguageCode updated to: ${appLang.code}")
            }
            .launchIn(viewModelScope)

        initialLoadJob = viewModelScope.launch {
            val initialLangCode = currentLanguage.first().code
            _uiState.update { it.copy(currentLanguageCode = initialLangCode, isInitialLoading = true) }

            val lastCity = userPreferencesManager.lastSelectedCityFlow.first()
            if (lastCity != null) {
                Log.d("ViewModelInit", "Last selected city found: ${lastCity.name}")
                val formattedName = formatCityNameInternal(lastCity, initialLangCode)
                _uiState.update {
                    it.copy(
                        selectedCityObject = lastCity,
                        cityForDisplay = formattedName,
                        searchQuery = formattedName,
                        isInitialLoading = false
                    )
                }
                fetchCurrentWeatherAndForecast(lastCity)
            } else {
                Log.d("ViewModelInit", "No last selected city. Loading default: $DEFAULT_CITY_FALLBACK")
                findCityByNameAndFetchWeatherInternal(DEFAULT_CITY_FALLBACK, true)
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
        return if (langCode.isEmpty()) null else langCode
    }

    fun onEvent(event: DryDriveEvent) {
        when (event) {
            is DryDriveEvent.SearchQueryChanged -> handleSearchQueryChanged(event.query)
            is DryDriveEvent.CitySelectedFromSearch -> handleCitySelected(event.city, event.formattedName)
            DryDriveEvent.DismissCitySearchDropDown -> _uiState.update { it.copy(isSearchDropDownExpanded = false) }
            DryDriveEvent.RefreshWeatherClicked -> {
                _uiState.value.selectedCityObject?.let {
                    fetchCurrentWeatherAndForecast(it)
                } ?: run {
                    Log.d("ViewModelEvent", "Refresh clicked but no city selected. Loading default.")
                    findCityByNameAndFetchWeatherInternal(DEFAULT_CITY_FALLBACK, true)
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
                    if (e is kotlinx.coroutines.CancellationException) {
                        Log.w("ViewModelEvents", "Search for '$query' was cancelled.")
                        _uiState.update { it.copy(isLoadingCities = false) }
                    } else {
                        Log.e("ViewModelEvents", "Error searching cities for '$query': ${e.message}", e)
                        _uiState.update {
                            it.copy(citySearchResults = emptyList(), isLoadingCities = false, citySearchErrorMessage = "$MSG_CITY_SEARCH_ERROR${e.message}")
                        }
                    }
                }
            }
        } else {
            _uiState.update { it.copy(citySearchResults = emptyList(), isLoadingCities = false, isSearchDropDownExpanded = false) }
        }
    }

    private fun handleCitySelected(city: City, formattedName: String) {
        initialLoadJob?.cancel()
        _uiState.update {
            it.copy(
                selectedCityObject = city,
                cityForDisplay = formattedName,
                searchQuery = formattedName,
                citySearchResults = emptyList(),
                isSearchDropDownExpanded = false,
                weatherErrorMessage = null,
                forecastErrorMessage = null,
                dailyForecasts = emptyList(),
                isInitialLoading = false
            )
        }
        fetchCurrentWeatherAndForecast(city)
        viewModelScope.launch {
            userPreferencesManager.saveLastSelectedCity(city)
        }
    }

    private fun fetchCurrentWeatherAndForecast(city: City) {
        viewModelScope.launch {
            val apiLang = getApiLangCode()
            _uiState.update {
                it.copy(isLoadingWeather = true, weather = null, weatherErrorMessage = null, isLoadingForecast = true, dailyForecasts = emptyList(), forecastErrorMessage = null)
            }
            var weatherFetchError: String? = null
            var forecastFetchError: String? = null
            try {
                val fetchedWeather = withContext(Dispatchers.IO) {
                    weatherApi.getWeather(city = city.name, apiKey = apiKey, lang = apiLang)
                }
                _uiState.update { it.copy(weather = fetchedWeather) }
            } catch (e: Exception) {
                weatherFetchError = if (e.message?.contains("404") == true) "$MSG_CITY_NOT_FOUND_WEATHER${city.name}" else "$MSG_WEATHER_LOAD_ERROR${e.message}"
            }
            if (city.lat != 0.0 && city.lon != 0.0) {
                try {
                    val forecastResponse = withContext(Dispatchers.IO) {
                        weatherApi.getFiveDayForecast(lat = city.lat, lon = city.lon, apiKey = apiKey, lang = apiLang)
                    }
                    val processedForecasts = processForecastResponse(forecastResponse, apiLang)
                    _uiState.update { it.copy(dailyForecasts = processedForecasts) }
                } catch (e: Exception) {
                    forecastFetchError = "$MSG_FORECAST_LOAD_ERROR${e.message}"
                }
            } else {
                forecastFetchError = MSG_COORDINATES_NOT_FOUND
            }
            _uiState.update {
                it.copy(isLoadingWeather = false, isLoadingForecast = false, weatherErrorMessage = weatherFetchError ?: it.weatherErrorMessage, forecastErrorMessage = forecastFetchError ?: it.forecastErrorMessage, isInitialLoading = false)
            }
        }
    }

    private fun processForecastResponse(response: ForecastResponse, apiLanguageCode: String?): List<DisplayDayWeather> {
        val dailyData = mutableMapOf<String, MutableList<ForecastListItem>>()

        val displayLocale = when {
            apiLanguageCode == null || apiLanguageCode.isEmpty() -> Locale.getDefault()
            else -> Locale(apiLanguageCode)
        }
        Log.d("ProcessForecast", "--- Starting Forecast Processing ---")
        Log.d("ProcessForecast", "Display Locale for dayOfWeekFormat: $displayLocale (derived from apiLang: $apiLanguageCode)")
        Log.d("ProcessForecast", "Device Default Locale: ${Locale.getDefault()}, Default TimeZone: ${TimeZone.getDefault().id}")

        val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH)
        inputFormat.timeZone = TimeZone.getTimeZone("UTC")

        val dayKeyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
        dayKeyFormat.timeZone = TimeZone.getTimeZone("UTC")

        val dayOfWeekFormat = SimpleDateFormat("E", displayLocale)
        Log.d("ProcessForecast", "dayOfWeekFormat TimeZone: ${dayOfWeekFormat.calendar.timeZone.id}")

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

        Log.d("ProcessForecast", "Number of unique UTC days collected: ${dailyData.size}")
        dailyData.keys.sorted().forEach { key ->
            Log.d("ProcessForecast", "Collected UTC dayKey: $key, items: ${dailyData[key]?.size}")
        }

        val displayForecasts = mutableListOf<DisplayDayWeather>()

        // Определяем текущий UTC день в формате "yyyy-MM-dd"
        val currentUtcDayKey = dayKeyFormat.format(Date()) // Date() создаст текущий момент времени
        Log.d("ProcessForecast", "Current UTC day key for filtering: $currentUtcDayKey")

        dailyData.entries
            .sortedBy { it.key }
            // Фильтруем: берем только те UTC-дни, которые строго больше текущего UTC-дня
            .filter { entry -> entry.key > currentUtcDayKey }
            .take(5) // Берем первые 5 таких дней
            .forEachIndexed { index, entry ->
                val utcDayKey = entry.key
                val dayItems = entry.value
                val targetItem = dayItems.find { it.dtTxt.substring(11, 13) == "12" } ?: dayItems.firstOrNull()

                Log.d("ProcessForecast", "Processing entry index $index (after filter): UTC DayKey = $utcDayKey")

                targetItem?.let { item ->
                    val itemUtcDate = inputFormat.parse(item.dtTxt)
                    Log.d("ProcessForecast", "  TargetItem dt_txt (UTC): ${item.dtTxt}, Parsed itemUtcDate: $itemUtcDate")

                    val dayLabel = itemUtcDate?.let { date ->
                        val localFormattedDateForLabel = dayOfWeekFormat.format(date)
                        Log.d("ProcessForecast", "    For itemUtcDate $date (UTC), dayOfWeekFormat generated label: '$localFormattedDateForLabel' (using passed displayLocale: $displayLocale and actual formatter TZ: ${dayOfWeekFormat.calendar.timeZone.id})")
                        localFormattedDateForLabel.replaceFirstChar { char ->
                            if (char.isLowerCase()) char.titlecase(displayLocale) else char.toString()
                        }
                    } ?: "???"

                    val iconRes = mapWeatherConditionToIcon(item.weather.firstOrNull()?.main, item.weather.firstOrNull()?.icon)
                    val temperature = "${item.main.temp.toInt()}°"
                    displayForecasts.add(DisplayDayWeather(dayLabel, iconRes, temperature))
                    Log.d("ProcessForecast", "  Added to display: DayLabel='$dayLabel', Temp='$temperature'")
                } ?: run {
                    Log.w("ProcessForecast", "  No targetItem found for UTC DayKey = $utcDayKey")
                }
            }
        Log.d("ProcessForecast", "--- Finished Forecast Processing, DisplayForecasts count: ${displayForecasts.size} ---")
        return displayForecasts
    }

    private fun mapWeatherConditionToIcon(condition: String?, iconCode: String?): Int {
        return when (iconCode) {
            "01d", "01n" -> R.drawable.ic_sun_filled
            "02d", "02n" -> R.drawable.ic_sun_filled
            "03d", "03n" -> R.drawable.ic_cloud
            "04d", "04n" -> R.drawable.ic_cloud
            "09d", "09n" -> R.drawable.ic_sun_filled
            "10d", "10n" -> R.drawable.ic_sun_filled
            "11d", "11n" -> R.drawable.ic_sun_filled
            "13d", "13n" -> R.drawable.ic_sun_filled
            "50d", "50n" -> R.drawable.ic_sun_filled
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

            _uiState.value.selectedCityObject?.let { city ->
                val newFormattedName = formatCityNameInternal(city, language.code)
                _uiState.update {
                    it.copy(cityForDisplay = newFormattedName, searchQuery = if (it.searchQuery == it.cityForDisplay && it.selectedCityObject?.name == city.name) newFormattedName else it.searchQuery)
                }
                fetchCurrentWeatherAndForecast(city)
            } ?: run {
                if (_uiState.value.cityForDisplay == DEFAULT_CITY_FALLBACK && _uiState.value.selectedCityObject == null) {
                    findCityByNameAndFetchWeatherInternal(DEFAULT_CITY_FALLBACK, true)
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

    private fun findCityByNameAndFetchWeatherInternal(cityName: String, isInitialOrFallback: Boolean = false) {
        viewModelScope.launch {
            if (isInitialOrFallback) {
                _uiState.update { it.copy(isLoadingWeather = true, isLoadingForecast = true, isInitialLoading = true) }
            }
            val apiLang = getApiLangCode()
            try {
                val cities = withContext(Dispatchers.IO) {
                    weatherApi.searchCities(query = cityName, apiKey = apiKey, limit = 1, lang = apiLang)
                }
                cities.firstOrNull()?.let { city ->
                    val langCodeForDisplay = _uiState.value.currentLanguageCode
                    val formattedName = formatCityNameInternal(city, langCodeForDisplay)
                    _uiState.update {
                        it.copy(selectedCityObject = city, cityForDisplay = formattedName, searchQuery = formattedName, isInitialLoading = false)
                    }
                    fetchCurrentWeatherAndForecast(city)
                    if (isInitialOrFallback) {
                        userPreferencesManager.saveLastSelectedCity(city)
                    }
                } ?: run {
                    _uiState.update {
                        it.copy(cityForDisplay = if(it.cityForDisplay.isEmpty()) "$MSG_CITY_NOT_FOUND_WEATHER $cityName" else it.cityForDisplay, weatherErrorMessage = "$MSG_CITY_NOT_FOUND_WEATHER $cityName", isLoadingWeather = false, isLoadingForecast = false, isInitialLoading = false)
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(weatherErrorMessage = "$MSG_CITY_SEARCH_ERROR ${e.message}", isLoadingWeather = false, isLoadingForecast = false, isInitialLoading = false)
                }
            }
        }
    }
}

