package ru.devsoland.drydrive

import android.os.Build
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
// import androidx.compose.animation.core.copy // Этот импорт, вероятно, не нужен, если используется copy() для data class
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
import kotlinx.coroutines.flow.launchIn // Для collect в init
import kotlinx.coroutines.flow.onEach // Для collect в init
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.devsoland.drydrive.data.City // Убедитесь, что City имеет localNames: Map<String, String>?
import ru.devsoland.drydrive.data.ForecastListItem // Я добавил этот импорт, предполагая, что он вам нужен
import ru.devsoland.drydrive.data.ForecastResponse
import ru.devsoland.drydrive.data.Weather
import ru.devsoland.drydrive.data.WeatherApi
import ru.devsoland.drydrive.data.preferences.AppLanguage
import ru.devsoland.drydrive.data.preferences.LanguageManager
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
    // Состояние текущей погоды
    val weather: Weather? = null,
    val isLoadingWeather: Boolean = false,
    val weatherErrorMessage: String? = null,

    // Состояние поиска города
    val searchQuery: String = "",
    val citySearchResults: List<City> = emptyList(),
    val isLoadingCities: Boolean = false,
    val citySearchErrorMessage: String? = null,
    val isSearchDropDownExpanded: Boolean = false,

    // Состояние выбранного города
    val cityForDisplay: String = "Moscow", // Это имя будет обновляться локализованным значением
    val selectedCityObject: City? = null,

    // Состояние прогноза погоды
    val dailyForecasts: List<DisplayDayWeather> = emptyList(),
    val isLoadingForecast: Boolean = false,
    val forecastErrorMessage: String? = null,

    // НОВОЕ ПОЛЕ: Код текущего языка приложения
    val currentLanguageCode: String = AppLanguage.SYSTEM.code // Инициализируем системным (пустой строкой)
)

@HiltViewModel
class DryDriveViewModel @Inject constructor(
    private val weatherApi: WeatherApi,
    private val languageManager: LanguageManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(DryDriveUiState())
    val uiState: StateFlow<DryDriveUiState> = _uiState.asStateFlow()

    private val apiKey = BuildConfig.WEATHER_API_KEY
    private var citySearchJob: Job? = null

    private companion object {
        // ... (константы без изменений)
        const val MSG_CITIES_NOT_FOUND = "Города не найдены: "
        const val MSG_CITY_SEARCH_ERROR = "Ошибка поиска: "
        const val MSG_CITY_NOT_FOUND_WEATHER = "Город не найден: "
        const val MSG_WEATHER_LOAD_ERROR = "Ошибка при загрузке погоды: "
        const val MSG_FORECAST_LOAD_ERROR = "Ошибка загрузки прогноза: "
        const val MSG_COORDINATES_NOT_FOUND = "Координаты для прогноза не найдены."
    }

    val currentLanguage: StateFlow<AppLanguage> = languageManager.selectedLanguageFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppLanguage.defaultLanguage()
        )

    private val _recreateActivityEvent = MutableSharedFlow<Unit>(replay = 0)
    val recreateActivityEvent: SharedFlow<Unit> = _recreateActivityEvent.asSharedFlow()

    init {
        // Обновляем searchQuery и currentLanguageCode в UiState при инициализации
        // и при каждом изменении currentLanguage
        currentLanguage
            .onEach { appLang ->
                _uiState.update { currentState ->
                    currentState.copy(
                        searchQuery = if (currentState.selectedCityObject == null) currentState.cityForDisplay else currentState.searchQuery, // searchQuery обновляем аккуратно
                        currentLanguageCode = appLang.code
                    )
                }
                Log.d("ViewModelInit", "UiState.currentLanguageCode updated to: ${appLang.code}")
            }
            .launchIn(viewModelScope) // Запускаем сбор Flow

        // Устанавливаем начальное значение searchQuery, если город по умолчанию не выбран
        // Это может быть перекрыто обновлением из currentLanguage.collect, если cityForDisplay останется "Moscow"
        if (_uiState.value.selectedCityObject == null) {
            _uiState.update { it.copy(searchQuery = it.cityForDisplay) }
        }

        // Если вы хотите загружать погоду для города по умолчанию ("Moscow") при старте:
        // Это потребует создания объекта City для "Moscow" или его поиска.
        // viewModelScope.launch {
        //     findCityByNameAndFetchWeather("Moscow") // Пример вызова
        // }
    }

    private fun getApiLangCode(): String? {
        // Используем currentLanguageCode из UiState, так как он синхронизирован
        // или напрямую из currentLanguage.value.code, что более прямолинейно здесь.
        val langCode = currentLanguage.value.code
        return if (langCode.isEmpty()) null else langCode
    }

    fun onEvent(event: DryDriveEvent) {
        when (event) {
            is DryDriveEvent.SearchQueryChanged -> handleSearchQueryChanged(event.query)
            is DryDriveEvent.CitySelectedFromSearch -> {
                // formattedName теперь должен приходить уже локализованным из MainActivity
                handleCitySelected(event.city, event.formattedName)
            }
            DryDriveEvent.DismissCitySearchDropDown -> _uiState.update { it.copy(isSearchDropDownExpanded = false) }
            DryDriveEvent.RefreshWeatherClicked -> _uiState.value.selectedCityObject?.let { fetchCurrentWeatherAndForecast(it) }
            DryDriveEvent.ClearWeatherErrorMessage -> _uiState.update { it.copy(weatherErrorMessage = null, forecastErrorMessage = null) }
        }
    }

    private fun handleSearchQueryChanged(query: String) {
        // ... (код без существенных изменений, только передача apiLang)
        _uiState.update { it.copy(searchQuery = query, isSearchDropDownExpanded = query.isNotEmpty()) }
        citySearchJob?.cancel()
        Log.d("ViewModelEvents", "Event: SearchQueryChanged to '$query'. Previous job cancelled.")

        if (query.length > 2) {
            _uiState.update { it.copy(isLoadingCities = true, citySearchErrorMessage = null) }
            citySearchJob = viewModelScope.launch {
                val apiLang = getApiLangCode() // Получаем язык для API
                Log.d("ViewModelAPI", "Starting city search for: '$query' with lang: '$apiLang'")
                delay(500)
                try {
                    val results = withContext(Dispatchers.IO) {
                        weatherApi.searchCities(
                            query = query,
                            apiKey = apiKey,
                            lang = apiLang
                        )
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
                it.copy(
                    citySearchResults = emptyList(),
                    isLoadingCities = false,
                    citySearchErrorMessage = null,
                    isSearchDropDownExpanded = false
                )
            }
        }
    }

    private fun handleCitySelected(city: City, formattedName: String) {
        // formattedName уже должен быть локализован MainActivity перед передачей сюда
        _uiState.update {
            it.copy(
                selectedCityObject = city,
                cityForDisplay = formattedName, // Используем локализованное имя
                searchQuery = formattedName,    // и для поля поиска
                citySearchResults = emptyList(),
                isSearchDropDownExpanded = false,
                weatherErrorMessage = null,
                forecastErrorMessage = null,
                dailyForecasts = emptyList()
            )
        }
        fetchCurrentWeatherAndForecast(city)
    }

    private fun fetchCurrentWeatherAndForecast(city: City) {
        viewModelScope.launch {
            val apiLang = getApiLangCode() // Получаем язык для API
            Log.d("ViewModelAPI", "Fetching weather and forecast for city: '${city.name}' (API name) with lang: '$apiLang'")

            _uiState.update {
                it.copy(
                    isLoadingWeather = true, weather = null, weatherErrorMessage = null,
                    isLoadingForecast = true, dailyForecasts = emptyList(), forecastErrorMessage = null
                )
            }

            var weatherFetchError: String? = null
            var forecastFetchError: String? = null
            // var currentWeatherData: Weather? = null // Переменная не используется для присвоения перед update

            try {
                val fetchedWeather = withContext(Dispatchers.IO) { // Сохраняем в новую переменную
                    weatherApi.getWeather(
                        city = city.name, // Для API используем оригинальное имя city.name или city.id если есть
                        apiKey = apiKey,
                        lang = apiLang
                    )
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
                        weatherApi.getFiveDayForecast(
                            lat = city.lat,
                            lon = city.lon,
                            apiKey = apiKey,
                            lang = apiLang
                        )
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
                    weatherErrorMessage = weatherFetchError ?: it.weatherErrorMessage, // Сохраняем существующую ошибку, если новая null
                    forecastErrorMessage = forecastFetchError ?: it.forecastErrorMessage
                )
            }
        }
    }

    private fun processForecastResponse(response: ForecastResponse, apiLanguageCode: String?): List<DisplayDayWeather> {
        val dailyData = mutableMapOf<String, MutableList<ForecastListItem>>() // Убедитесь, что ForecastListItem импортирован

        val displayLocale = when {
            apiLanguageCode == null || apiLanguageCode.isEmpty() -> Locale.getDefault() // Используем локаль устройства, если API вернул на en (системный)
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
                    "Сейчас" // TODO: Локализовать через ресурсы (потребует Context во ViewModel или передачи из UI)
                } else {
                    itemDate?.let {
                        dayOfWeekFormat.format(it).replaceFirstChar { char ->
                            if (char.isLowerCase()) char.titlecase(displayLocale) else char.toString()
                        }
                    } ?: "???"
                }
                val iconRes = mapWeatherConditionToIcon(item.weather.firstOrNull()?.main, item.weather.firstOrNull()?.icon)
                val temperature = "${item.main.temp.toInt()}°"
                displayForecasts.add(DisplayDayWeather(dayLabel, iconRes, temperature))
            }
        }
        return displayForecasts
    }

    private fun isSameDay(date: Date, calendar: Calendar): Boolean {
        // ... (код без изменений) ...
        val itemCalendar = Calendar.getInstance()
        itemCalendar.time = date
        return calendar.get(Calendar.YEAR) == itemCalendar.get(Calendar.YEAR) &&
                calendar.get(Calendar.DAY_OF_YEAR) == itemCalendar.get(Calendar.DAY_OF_YEAR)
    }

    private fun mapWeatherConditionToIcon(condition: String?, iconCode: String?): Int {
        // ... (ваш код R.drawable.* без изменений) ...
        return when (iconCode) {
            "01d" -> R.drawable.ic_sun_filled
            "01n" -> R.drawable.ic_sun_filled
            "02d" -> R.drawable.ic_sun_filled
            "03d" -> R.drawable.ic_cloud
            "04d" -> R.drawable.ic_sun_filled
            "02n" -> R.drawable.ic_sun_filled
            "03n", "04n" -> R.drawable.ic_cloud
            "09d", "09n" -> R.drawable.ic_sun_filled
            "10d" -> R.drawable.ic_sun_filled
            "10n" -> R.drawable.ic_sun_filled
            "11d", "11n" -> R.drawable.ic_sun_filled
            "13d", "13n" -> R.drawable.ic_sun_filled
            "50d", "50n" -> R.drawable.ic_sun_filled
            else -> R.drawable.ic_cloud
        }
    }

    fun onLanguageSelected(language: AppLanguage) {
        viewModelScope.launch {
            val previousLanguage = currentLanguage.value // Это значение AppLanguage
            if (previousLanguage.code == language.code) { // Сравниваем коды, чтобы избежать ненужных действий
                Log.d("ViewModelLanguage", "Language code '${language.code}' already selected. No action.")
                // Если мы все равно хотим вызвать recreate (например, для системного языка), эту проверку можно усложнить
                if (language == AppLanguage.SYSTEM && previousLanguage != AppLanguage.SYSTEM) {
                    // Разрешаем, если до этого был не системный, а теперь выбрали системный
                } else if (previousLanguage.code == language.code) {
                    return@launch
                }
            }

            languageManager.saveSelectedLanguage(language)
            // currentLanguage и uiState.currentLanguageCode обновятся автоматически через Flow

            Log.d("ViewModelLanguage", "Language with code '${language.code}' saved. UiState will update via collect.")

            _uiState.value.selectedCityObject?.let { city ->
                Log.d("ViewModelLanguage", "Language changed, refreshing weather data for ${city.name}")
                fetchCurrentWeatherAndForecast(city) // Загрузит погоду и прогноз на новом языке

                // Обновляем cityForDisplay. MainActivity должна будет передать новое отформатированное имя
                // при следующем взаимодействии, или мы можем попытаться сформировать его здесь,
                // если бы у нас была formatCityName.
                // Поскольку MainActivity формирует formattedName при выборе,
                // и мы передаем новый currentLanguageCode в UiState,
                // TopAppBar должен будет перерисоваться с новым языком.
                // Главное, чтобы cityForDisplay обновился, если пользователь выберет город заново.
                // При простой смене языка, если город уже выбран, TopAppBar сам использует новый uiState.currentLanguageCode
                // для форматирования отображаемого uiState.cityForDisplay (если бы мы так сделали в MainActivity).
                // Но так как cityForDisplay - это просто строка, она не обновится сама.
                // ПРОСТОЕ РЕШЕНИЕ: Очистить cityForDisplay, чтобы UI показал "Select City" или переформатировал
                // имя при следующем удобном случае.
                // ЛУЧШЕЕ РЕШЕНИЕ: ViewModel не должна знать о formatCityName.
                // MainActivity при сборе uiState должна форматировать имя города для отображения,
                // используя uiState.selectedCityObject и uiState.currentLanguageCode.
                // Пока что, при смене языка, cityForDisplay останется старым, пока город не выберут заново.
                // Чтобы это исправить, MainActivity должна быть ответственна за форматирование cityForDisplay.
                // Компромисс: если `selectedCityObject` есть, и язык меняется, можно попробовать найти
                // имя на новом языке в `localNames` и обновить `cityForDisplay`.

                val newLangCode = language.code
                val newDisplayName = when {
                    newLangCode.isNotBlank() && city.localNames?.containsKey(newLangCode) == true -> city.localNames[newLangCode]
                    city.localNames?.containsKey("en") == true -> city.localNames["en"]
                    else -> city.name
                } ?: city.name
                val country = city.country
                val state = city.state
                val newFormattedCityForDisplay = "$newDisplayName, $country" + if (state != null) ", $state" else ""

                _uiState.update { it.copy(cityForDisplay = newFormattedCityForDisplay) }
                Log.d("ViewModelLanguage", "Updated cityForDisplay to: $newFormattedCityForDisplay after language change.")

            } ?: run {
                Log.d("ViewModelLanguage", "Language changed, but no city selected to refresh weather or cityForDisplay.")
                // Если город по умолчанию "Moscow" и мы хотим его обновить:
                if (_uiState.value.cityForDisplay == "Moscow" && _uiState.value.selectedCityObject == null) {
                    // viewModelScope.launch { findCityByNameAndFetchWeather("Moscow", true) } // true - для обновления display name
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                try {
                    val localeListToSet = if (language.code.isEmpty()) { // Проверяем напрямую code
                        LocaleListCompat.getEmptyLocaleList()
                    } else {
                        LocaleListCompat.forLanguageTags(language.code)
                    }
                    AppCompatDelegate.setApplicationLocales(localeListToSet)
                    Log.d("ViewModelLanguage", "Application locales call finished for ${language.code.ifEmpty { "SYSTEM" }}")
                } catch (e: Exception) {
                    Log.e("ViewModelLanguage", "Error setting application locales: ${e.message}", e)
                }
            }

            Log.d("ViewModelLanguage", "Emitting recreateActivityEvent")
            _recreateActivityEvent.emit(Unit)
        }
    }

    // Примерная функция, если вы захотите загружать город по умолчанию при старте
    // или при смене языка, если город не выбран.
    // private fun findCityByNameAndFetchWeather(cityName: String, updateCityForDisplayWithNewLang: Boolean = false) {
    //     viewModelScope.launch {
    //         val apiLang = getApiLangCode()
    //         try {
    //             val cities = withContext(Dispatchers.IO) {
    //                 weatherApi.searchCities(query = cityName, apiKey = apiKey, limit = 1, lang = apiLang)
    //             }
    //             cities.firstOrNull()?.let { city ->
    //                 val langCodeForDisplay = uiState.value.currentLanguageCode
    //                 val displayName = when {
    //                     langCodeForDisplay.isNotBlank() && city.localNames?.containsKey(langCodeForDisplay) == true -> city.localNames[langCodeForDisplay]
    //                     city.localNames?.containsKey("en") == true -> city.localNames["en"]
    //                     else -> city.name
    //                 } ?: city.name
    //                 val country = city.country
    //                 val state = city.state
    //                 val formattedName = "$displayName, $country" + if (state != null) ", $state" else ""
    //
    //                 if (updateCityForDisplayWithNewLang || _uiState.value.selectedCityObject == null) {
    //                     _uiState.update {
    //                         it.copy(
    //                             selectedCityObject = city,
    //                             cityForDisplay = formattedName,
    //                             searchQuery = formattedName
    //                         )
    //                     }
    //                 } else { // Только обновить selectedCityObject если уже был какой-то другой
    //                     _uiState.update { it.copy(selectedCityObject = city) }
    //                 }
    //                 fetchCurrentWeatherAndForecast(city)
    //             }
    //         } catch (e: Exception) {
    //             Log.e("ViewModelDefaultCity", "Error finding/fetching default city $cityName: ${e.message}")
    //         }
    //     }
    // }
}

