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
import kotlinx.coroutines.flow.first // Для однократного получения значения из Flow
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
// ИЗМЕНЕННЫЙ ИМПОРТ
import ru.devsoland.drydrive.data.preferences.UserPreferencesManager
// КОНЕЦ ИЗМЕНЕННОГО ИМПОРТА
import java.text.SimpleDateFormat
import java.util.Calendar
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
    val cityForDisplay: String = "", // Изначально пустая, будет заполнена из настроек или при выборе
    val selectedCityObject: City? = null,
    val dailyForecasts: List<DisplayDayWeather> = emptyList(),
    val isLoadingForecast: Boolean = false,
    val forecastErrorMessage: String? = null,
    val currentLanguageCode: String = AppLanguage.SYSTEM.code,
    val isInitialLoading: Boolean = true // Флаг для начальной загрузки (показ индикатора)
)

@HiltViewModel
class DryDriveViewModel @Inject constructor(
    private val weatherApi: WeatherApi,
    private val userPreferencesManager: UserPreferencesManager // ИЗМЕНЕН ТИП
) : ViewModel() {

    private val _uiState = MutableStateFlow(DryDriveUiState())
    val uiState: StateFlow<DryDriveUiState> = _uiState.asStateFlow()

    private val apiKey = BuildConfig.WEATHER_API_KEY
    private var citySearchJob: Job? = null
    private var initialLoadJob: Job? = null // Для отслеживания загрузки начального города

    private companion object {
        const val MSG_CITIES_NOT_FOUND = "Города не найдены: "
        const val MSG_CITY_SEARCH_ERROR = "Ошибка поиска: "
        const val MSG_CITY_NOT_FOUND_WEATHER = "Город не найден: "
        const val MSG_WEATHER_LOAD_ERROR = "Ошибка при загрузке погоды: "
        const val MSG_FORECAST_LOAD_ERROR = "Ошибка загрузки прогноза: "
        const val MSG_COORDINATES_NOT_FOUND = "Координаты для прогноза не найдены."
        const val DEFAULT_CITY_FALLBACK = "Moscow" // Фолбэк, если ничего не сохранено
    }

    // currentLanguage теперь берется из userPreferencesManager
    val currentLanguage: StateFlow<AppLanguage> = userPreferencesManager.selectedLanguageFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppLanguage.defaultLanguage()
        )

    private val _recreateActivityEvent = MutableSharedFlow<Unit>(replay = 0)
    val recreateActivityEvent: SharedFlow<Unit> = _recreateActivityEvent.asSharedFlow()

    init {
        // 1. Подписка на изменение языка для обновления uiState.currentLanguageCode
        currentLanguage
            .onEach { appLang ->
                _uiState.update { currentState ->
                    currentState.copy(currentLanguageCode = appLang.code)
                }
                Log.d("ViewModelInit", "UiState.currentLanguageCode updated to: ${appLang.code}")
            }
            .launchIn(viewModelScope)

        // 2. Загрузка последнего сохраненного города или города по умолчанию
        initialLoadJob = viewModelScope.launch {
            // Сначала дождемся, пока currentLanguageCode будет установлен в UiState, чтобы корректно отформатировать имя
            val initialLangCode = currentLanguage.first().code // Получаем первый (текущий) код языка
            _uiState.update { it.copy(currentLanguageCode = initialLangCode, isInitialLoading = true) }


            val lastCity = userPreferencesManager.lastSelectedCityFlow.first() // Получаем однократно
            if (lastCity != null) {
                Log.d("ViewModelInit", "Last selected city found: ${lastCity.name}")
                // Форматируем имя с учетом текущего языка приложения
                val formattedName = formatCityNameInternal(lastCity, initialLangCode)
                _uiState.update {
                    it.copy(
                        selectedCityObject = lastCity,
                        cityForDisplay = formattedName,
                        searchQuery = formattedName, // Обновляем searchQuery, чтобы поле поиска не было пустым
                        isInitialLoading = false // Завершили попытку загрузки начального города
                    )
                }
                fetchCurrentWeatherAndForecast(lastCity)
            } else {
                Log.d("ViewModelInit", "No last selected city. Attempting to load default: $DEFAULT_CITY_FALLBACK")
                // Если нет сохраненного города, можно загрузить город по умолчанию (например, Москву)
                // Или оставить cityForDisplay пустым, чтобы пользователь выбрал город
                // _uiState.update { it.copy(cityForDisplay = "Выберите город", isInitialLoading = false) } // Пример
                findCityByNameAndFetchWeatherInternal(DEFAULT_CITY_FALLBACK, true) // Загружаем Москву по умолчанию
            }
        }
    }

    // Внутренняя функция для форматирования имени города, чтобы не дублировать логику MainActivity
    // Это упрощенная версия, MainActivity.formatCityName более полная
    private fun formatCityNameInternal(city: City, langCode: String): String {
        val displayName = when {
            langCode.isNotBlank() && city.localNames?.containsKey(langCode) == true -> city.localNames[langCode]
            city.localNames?.containsKey("en") == true -> city.localNames["en"]
            else -> city.name
        } ?: city.name
        return "$displayName, ${city.country}" + if (city.state != null) ", ${city.state}" else ""
    }


    private fun getApiLangCode(): String? {
        val langCode = _uiState.value.currentLanguageCode // Берем из UiState, т.к. он синхронизирован
        return if (langCode.isEmpty()) null else langCode
    }

    fun onEvent(event: DryDriveEvent) {
        when (event) {
            is DryDriveEvent.SearchQueryChanged -> handleSearchQueryChanged(event.query)
            is DryDriveEvent.CitySelectedFromSearch -> {
                // formattedName уже локализован MainActivity
                handleCitySelected(event.city, event.formattedName)
            }
            DryDriveEvent.DismissCitySearchDropDown -> _uiState.update { it.copy(isSearchDropDownExpanded = false) }
            DryDriveEvent.RefreshWeatherClicked -> {
                _uiState.value.selectedCityObject?.let {
                    fetchCurrentWeatherAndForecast(it)
                } ?: run {
                    // Если город не выбран, но нажали обновить, можно попробовать загрузить город по умолчанию
                    Log.d("ViewModelEvent", "Refresh clicked but no city selected. Loading default.")
                    findCityByNameAndFetchWeatherInternal(DEFAULT_CITY_FALLBACK, true)
                }
            }
            DryDriveEvent.ClearWeatherErrorMessage -> _uiState.update { it.copy(weatherErrorMessage = null, forecastErrorMessage = null) }
        }
    }

    private fun handleSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query, isSearchDropDownExpanded = query.isNotEmpty(), citySearchErrorMessage = null) } // Сбрасываем ошибку при новом поиске
        citySearchJob?.cancel()
        Log.d("ViewModelEvents", "Event: SearchQueryChanged to '$query'.")

        if (query.length > 2) {
            _uiState.update { it.copy(isLoadingCities = true) }
            citySearchJob = viewModelScope.launch {
                val apiLang = getApiLangCode()
                Log.d("ViewModelAPI", "Starting city search for: '$query' with lang: '$apiLang'")
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
                    // ... (обработка ошибок без изменений)
                    if (e is kotlinx.coroutines.CancellationException) {
                        Log.w("ViewModelEvents", "Search for '$query' was cancelled.")
                        _uiState.update { it.copy(isLoadingCities = false) }
                    } else {
                        Log.e("ViewModelEvents", "Error searching cities for '$query': ${e.message}", e)
                        _uiState.update {
                            it.copy(
                                citySearchResults = emptyList(),
                                isLoadingCities = false,
                                citySearchErrorMessage = "$MSG_CITY_SEARCH_ERROR${e.message}"
                            )
                        }
                    }
                }
            }
        } else {
            _uiState.update {
                it.copy(citySearchResults = emptyList(), isLoadingCities = false, isSearchDropDownExpanded = false)
            }
        }
    }

    private fun handleCitySelected(city: City, formattedName: String) {
        // formattedName уже локализован MainActivity
        initialLoadJob?.cancel() // Отменяем начальную загрузку, если она еще идет, т.к. пользователь выбрал город
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
                isInitialLoading = false // Пользовательский выбор завершает начальную загрузку
            )
        }
        fetchCurrentWeatherAndForecast(city)
        // Сохраняем выбранный город
        viewModelScope.launch {
            userPreferencesManager.saveLastSelectedCity(city)
            Log.d("ViewModelCitySave", "Saved city to preferences: ${city.name}")
        }
    }

    private fun fetchCurrentWeatherAndForecast(city: City) {
        viewModelScope.launch {
            val apiLang = getApiLangCode()
            Log.d("ViewModelAPI", "Fetching weather and forecast for city: '${city.name}' (API name) with lang: '$apiLang'")

            _uiState.update {
                it.copy(
                    isLoadingWeather = true, weather = null, weatherErrorMessage = null,
                    isLoadingForecast = true, dailyForecasts = emptyList(), forecastErrorMessage = null
                )
            }
            // ... (остальной код fetchCurrentWeatherAndForecast без изменений, до финального update)
            var weatherFetchError: String? = null
            var forecastFetchError: String? = null

            try {
                val fetchedWeather = withContext(Dispatchers.IO) {
                    weatherApi.getWeather(city = city.name, apiKey = apiKey, lang = apiLang)
                }
                _uiState.update { it.copy(weather = fetchedWeather) }
            } catch (e: Exception) {
                Log.e("ViewModelFetch", "Error fetching current weather for ${city.name}: ${e.message}", e)
                weatherFetchError = if (e.message?.contains("404") == true) {
                    "$MSG_CITY_NOT_FOUND_WEATHER${city.name}"
                } else {
                    "$MSG_WEATHER_LOAD_ERROR${e.message}"
                }
            }

            if (city.lat != 0.0 && city.lon != 0.0) {
                try {
                    val forecastResponse = withContext(Dispatchers.IO) {
                        weatherApi.getFiveDayForecast(lat = city.lat, lon = city.lon, apiKey = apiKey, lang = apiLang)
                    }
                    val processedForecasts = processForecastResponse(forecastResponse, apiLang)
                    _uiState.update { it.copy(dailyForecasts = processedForecasts) }
                } catch (e: Exception) {
                    Log.e("ViewModelFetch", "Error fetching forecast for ${city.name}: ${e.message}", e)
                    forecastFetchError = "$MSG_FORECAST_LOAD_ERROR${e.message}"
                }
            } else {
                Log.w("ViewModelFetch", "Skipping forecast fetch due to missing coordinates for ${city.name}")
                forecastFetchError = MSG_COORDINATES_NOT_FOUND
            }

            _uiState.update {
                it.copy(
                    isLoadingWeather = false,
                    isLoadingForecast = false,
                    weatherErrorMessage = weatherFetchError ?: it.weatherErrorMessage,
                    forecastErrorMessage = forecastFetchError ?: it.forecastErrorMessage,
                    isInitialLoading = false // Завершение любой активной загрузки
                )
            }
        }
    }

    private fun processForecastResponse(response: ForecastResponse, apiLanguageCode: String?): List<DisplayDayWeather> {
        // ... (код без изменений)
        val dailyData = mutableMapOf<String, MutableList<ForecastListItem>>()
        val displayLocale = when {
            apiLanguageCode == null || apiLanguageCode.isEmpty() -> Locale.getDefault()
            else -> Locale(apiLanguageCode)
        }
        Log.d("ProcessForecast", "Using locale for dayOfWeekFormat: $displayLocale (based on apiLang: $apiLanguageCode)")
        val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH)
        inputFormat.timeZone = TimeZone.getTimeZone("UTC")
        val dayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
        val dayOfWeekFormat = SimpleDateFormat("E", displayLocale)
        response.list.forEach { item ->
            try {
                val date = inputFormat.parse(item.dtTxt)
                date?.let {
                    val dayKey = dayFormat.format(it)
                    dailyData.getOrPut(dayKey) { mutableListOf() }.add(item)
                }
            } catch (e: Exception) {
                Log.e("ProcessForecast", "Error parsing date: ${item.dtTxt}", e)
            }
        }
        val displayForecasts = mutableListOf<DisplayDayWeather>()
        val todayCal = Calendar.getInstance()
        dailyData.entries.sortedBy { it.key }.take(5).forEachIndexed { index, entry ->
            val dayItems = entry.value
            val targetItem = dayItems.find { it.dtTxt.substring(11, 13) == "12" } ?: dayItems.firstOrNull()
            targetItem?.let { item ->
                val itemDate = inputFormat.parse(item.dtTxt)
                val dayLabel = if (index == 0 && itemDate != null && isSameDay(itemDate, todayCal)) {
                    "Сейчас" // TODO: Локализовать
                } else {
                    itemDate?.let { dayOfWeekFormat.format(it).replaceFirstChar { char -> if (char.isLowerCase()) char.titlecase(displayLocale) else char.toString() } } ?: "???"
                }
                val iconRes = mapWeatherConditionToIcon(item.weather.firstOrNull()?.main, item.weather.firstOrNull()?.icon)
                val temperature = "${item.main.temp.toInt()}°"
                displayForecasts.add(DisplayDayWeather(dayLabel, iconRes, temperature))
            }
        }
        return displayForecasts
    }

    private fun isSameDay(date: Date, calendar: Calendar): Boolean {
        // ... (код без изменений)
        val itemCalendar = Calendar.getInstance()
        itemCalendar.time = date
        return calendar.get(Calendar.YEAR) == itemCalendar.get(Calendar.YEAR) &&
                calendar.get(Calendar.DAY_OF_YEAR) == itemCalendar.get(Calendar.DAY_OF_YEAR)
    }

    private fun mapWeatherConditionToIcon(condition: String?, iconCode: String?): Int {
        // ... (код R.drawable.* без изменений)
        return when (iconCode) {
            "01d" -> R.drawable.ic_sun_filled; "01n" -> R.drawable.ic_sun_filled
            "02d" -> R.drawable.ic_sun_filled; "02n" -> R.drawable.ic_sun_filled
            "03d" -> R.drawable.ic_cloud; "03n" -> R.drawable.ic_cloud
            "04d" -> R.drawable.ic_sun_filled; "04n" -> R.drawable.ic_cloud // 04d usually more cloudy, but using sun_filled for now
            "09d", "09n" -> R.drawable.ic_sun_filled // rain
            "10d" -> R.drawable.ic_sun_filled; "10n" -> R.drawable.ic_sun_filled // sun with rain
            "11d", "11n" -> R.drawable.ic_sun_filled // thunderstorm
            "13d", "13n" -> R.drawable.ic_sun_filled // snow
            "50d", "50n" -> R.drawable.ic_sun_filled // mist
            else -> R.drawable.ic_cloud
        }
    }

    fun onLanguageSelected(language: AppLanguage) {
        viewModelScope.launch {
            val previousLangCode = _uiState.value.currentLanguageCode
            if (previousLangCode == language.code && !(language == AppLanguage.SYSTEM && previousLangCode.isNotEmpty())) {
                Log.d("ViewModelLanguage", "Language code '${language.code}' already selected or effectively the same. No action unless switching to SYSTEM from specific.")
                // Если мы все равно хотим вызвать recreate (например, для системного языка, который может измениться)
                // то условие здесь должно быть более сложным, или recreateActivityEvent должно вызываться всегда.
                // Пока оставим так: не делаем ничего, если код языка не изменился, кроме случая, когда Явно выбрали SYSTEM.
                if (language == AppLanguage.SYSTEM && previousLangCode.isEmpty()) return@launch // Уже системный
                if (language != AppLanguage.SYSTEM && previousLangCode == language.code) return@launch // Уже этот конкретный язык
            }

            userPreferencesManager.saveSelectedLanguage(language)
            // _uiState.currentLanguageCode обновится через collect в init

            Log.d("ViewModelLanguage", "Language with code '${language.code}' saved.")

            _uiState.value.selectedCityObject?.let { city ->
                Log.d("ViewModelLanguage", "Language changed, refreshing weather for ${city.name}")
                // Переформатируем cityForDisplay с новым языком
                val newFormattedName = formatCityNameInternal(city, language.code)
                _uiState.update { it.copy(cityForDisplay = newFormattedName, searchQuery = if(it.searchQuery == it.cityForDisplay) newFormattedName else it.searchQuery ) } // Обновляем и searchQuery, если он был равен cityForDisplay
                Log.d("ViewModelLanguage", "Updated cityForDisplay to: $newFormattedName")
                fetchCurrentWeatherAndForecast(city) // Загрузит погоду и прогноз на новом языке
            } ?: run {
                Log.d("ViewModelLanguage", "Language changed, but no city selected. Maybe refresh default?")
                // Если нет выбранного города, но есть город по умолчанию, можно обновить его имя и перезагрузить
                if (_uiState.value.cityForDisplay == DEFAULT_CITY_FALLBACK && _uiState.value.selectedCityObject == null) {
                    findCityByNameAndFetchWeatherInternal(DEFAULT_CITY_FALLBACK, true)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                try {
                    val localeListToSet = if (language.code.isEmpty()) {
                        LocaleListCompat.getEmptyLocaleList()
                    } else {
                        LocaleListCompat.forLanguageTags(language.code)
                    }
                    AppCompatDelegate.setApplicationLocales(localeListToSet)
                } catch (e: Exception) {
                    Log.e("ViewModelLanguage", "Error setting application locales: ${e.message}", e)
                }
            }
            _recreateActivityEvent.emit(Unit)
        }
    }

    // Внутренняя функция для поиска города по имени и обновления UI
    private fun findCityByNameAndFetchWeatherInternal(cityName: String, isInitialOrFallback: Boolean = false) {
        viewModelScope.launch {
            if (isInitialOrFallback) {
                _uiState.update { it.copy(isLoadingWeather = true, isLoadingForecast = true, isInitialLoading = true) }
            }
            val apiLang = getApiLangCode()
            try {
                Log.d("ViewModelInternalSearch", "Searching for city: $cityName with lang $apiLang")
                val cities = withContext(Dispatchers.IO) {
                    weatherApi.searchCities(query = cityName, apiKey = apiKey, limit = 1, lang = apiLang)
                }
                cities.firstOrNull()?.let { city ->
                    Log.d("ViewModelInternalSearch", "City found: ${city.name}. Fetching weather.")
                    // Имя для отображения формируется на основе текущего языка приложения
                    val langCodeForDisplay = _uiState.value.currentLanguageCode
                    val formattedName = formatCityNameInternal(city, langCodeForDisplay)

                    _uiState.update {
                        it.copy(
                            selectedCityObject = city,
                            cityForDisplay = formattedName,
                            searchQuery = formattedName, // Заполняем searchQuery, чтобы пользователь видел имя
                            isInitialLoading = !isInitialOrFallback // isInitialLoading сбрасывается, если это не начальная загрузка
                        )
                    }
                    fetchCurrentWeatherAndForecast(city)
                    // Если это была загрузка по умолчанию, и мы хотим сохранить этот город как "последний"
                    if (isInitialOrFallback) { // Сохраняем только если это была загрузка по умолчанию и город НАЙДЕН
                        userPreferencesManager.saveLastSelectedCity(city)
                        Log.d("ViewModelInternalSearch", "Saved default city to preferences: ${city.name}")
                    }
                } ?: run {
                    Log.w("ViewModelInternalSearch", "City $cityName not found via API.")
                    _uiState.update {
                        it.copy(
                            cityForDisplay = if(it.cityForDisplay.isEmpty()) "Город не найден" else it.cityForDisplay, // Чтобы не было пустого поля
                            weatherErrorMessage = "$MSG_CITY_NOT_FOUND_WEATHER $cityName",
                            isLoadingWeather = false, isLoadingForecast = false, isInitialLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("ViewModelInternalSearch", "Error finding/fetching city $cityName: ${e.message}", e)
                _uiState.update {
                    it.copy(
                        weatherErrorMessage = "$MSG_CITY_SEARCH_ERROR ${e.message}",
                        isLoadingWeather = false, isLoadingForecast = false, isInitialLoading = false
                    )
                }
            }
        }
    }
}

