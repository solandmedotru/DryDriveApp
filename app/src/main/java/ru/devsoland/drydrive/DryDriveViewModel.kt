package ru.devsoland.drydrive

import android.os.Build
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.core.copy
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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.devsoland.drydrive.data.City
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
    val iconRes: Int, // Здесь будет ID ресурса иконки (например, R.drawable.ic_sun)
    val temperature: String // Например, "25°"
)

data class DryDriveUiState(
    // Состояние текущей погоды
    val weather: Weather? = null, // Объект Weather из вашего пакета data
    val isLoadingWeather: Boolean = false,
    val weatherErrorMessage: String? = null,

    // Состояние поиска города
    val searchQuery: String = "",
    val citySearchResults: List<City> = emptyList(), // Список объектов City из вашего пакета data
    val isLoadingCities: Boolean = false,
    val citySearchErrorMessage: String? = null,
    val isSearchDropDownExpanded: Boolean = false,

    // Состояние выбранного города
    val cityForDisplay: String = "Moscow", // Строка для отображения в UI по умолчанию
    val selectedCityObject: City? = null, // Выбранный объект City

    // Состояние прогноза погоды
    val dailyForecasts: List<DisplayDayWeather> = emptyList(), // Список DisplayDayWeather
    val isLoadingForecast: Boolean = false,
    val forecastErrorMessage: String? = null
)

@HiltViewModel
class DryDriveViewModel @Inject constructor(
    private val weatherApi: WeatherApi,
    private val languageManager: LanguageManager
    // private val applicationContext: Context // Понадобится для строковых ресурсов, если будете использовать их во ViewModel
) : ViewModel() {

    private val _uiState = MutableStateFlow(DryDriveUiState())
    val uiState: StateFlow<DryDriveUiState> = _uiState.asStateFlow()

    private val apiKey = BuildConfig.WEATHER_API_KEY
    private var citySearchJob: Job? = null

    private companion object {
        const val MSG_CITIES_NOT_FOUND = "Города не найдены: " // TODO: Локализовать или использовать строки из ресурсов
        const val MSG_CITY_SEARCH_ERROR = "Ошибка поиска: " // TODO: Локализовать
        const val MSG_CITY_NOT_FOUND_WEATHER = "Город не найден: " // TODO: Локализовать
        const val MSG_WEATHER_LOAD_ERROR = "Ошибка при загрузке погоды: " // TODO: Локализовать
        const val MSG_FORECAST_LOAD_ERROR = "Ошибка загрузки прогноза: " // TODO: Локализовать
        const val MSG_COORDINATES_NOT_FOUND = "Координаты для прогноза не найдены." // TODO: Локализовать
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
        _uiState.update { it.copy(searchQuery = it.cityForDisplay) }
        // Можно загрузить погоду для города по умолчанию при инициализации, если нужно
        // viewModelScope.launch {
        //     val defaultCityName = _uiState.value.cityForDisplay
        //     // Сначала нужен объект City для fetchCurrentWeatherAndForecast
        //     // Это потребует поиска города по имени или хранения City по умолчанию
        // }
    }

    /**
     * Возвращает код языка для API запросов.
     * Если выбран системный язык (код пустой), возвращает null (API использует свой default, обычно "en").
     * Иначе возвращает код выбранного языка ("ru", "en", и т.д.).
     */
    private fun getApiLangCode(): String? {
        val langCode = currentLanguage.value.code
        return if (langCode.isEmpty()) null else langCode
        // Альтернатива: всегда возвращать "en" для системного, если API не очень хорошо обрабатывает отсутствие lang
        // return if (langCode.isEmpty()) "en" else langCode
    }

    fun onEvent(event: DryDriveEvent) {
        when (event) {
            is DryDriveEvent.SearchQueryChanged -> {
                handleSearchQueryChanged(event.query)
            }
            is DryDriveEvent.CitySelectedFromSearch -> {
                // Если мы будем использовать localNames, то formattedName нужно будет получать
                // с учетом текущего языка приложения уже на стороне UI или здесь перед вызовом.
                // Пока оставим как есть, но это место для потенциального улучшения с localNames.
                handleCitySelected(event.city, event.formattedName)
            }
            DryDriveEvent.DismissCitySearchDropDown -> {
                _uiState.update { it.copy(isSearchDropDownExpanded = false) }
            }
            DryDriveEvent.RefreshWeatherClicked -> {
                _uiState.value.selectedCityObject?.let {
                    fetchCurrentWeatherAndForecast(it)
                }
            }
            DryDriveEvent.ClearWeatherErrorMessage -> {
                if (_uiState.value.weatherErrorMessage != null || _uiState.value.forecastErrorMessage != null) {
                    _uiState.update { it.copy(weatherErrorMessage = null, forecastErrorMessage = null) }
                }
            }
        }
    }


    private fun handleSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query, isSearchDropDownExpanded = query.isNotEmpty()) }
        citySearchJob?.cancel()
        Log.d("ViewModelEvents", "Event: SearchQueryChanged to '$query'. Previous job cancelled.")

        if (query.length > 2) {
            _uiState.update { it.copy(isLoadingCities = true, citySearchErrorMessage = null) }
            citySearchJob = viewModelScope.launch {
                val apiLang = getApiLangCode()
                Log.d("ViewModelAPI", "Starting city search for: '$query' with lang: '$apiLang'")
                delay(500) // Debounce
                try {
                    val results = withContext(Dispatchers.IO) {
                        weatherApi.searchCities(
                            query = query, // Сам поисковый запрос на языке ввода пользователя
                            apiKey = apiKey,
                            lang = apiLang // Язык для локализации результатов от API
                        )
                    }
                    _uiState.update {
                        it.copy(
                            citySearchResults = results,
                            isLoadingCities = false,
                            // Сообщение об ошибке лучше брать из строковых ресурсов
                            citySearchErrorMessage = if (results.isEmpty() && query.isNotBlank()) "Города не найдены для: '$query'" else null, // TODO: Локализовать
                            isSearchDropDownExpanded = results.isNotEmpty()
                        )
                    }
                } catch (e: Exception) {
                    if (e is kotlinx.coroutines.CancellationException) {
                        Log.w("ViewModelEvents", "Search for '$query' was cancelled.")
                        _uiState.update { it.copy(isLoadingCities = false) } // Сбросить isLoadingCities
                    } else {
                        Log.e("ViewModelEvents", "Error searching cities for '$query': ${e.message}", e)
                        _uiState.update {
                            it.copy(
                                citySearchResults = emptyList(),
                                isLoadingCities = false,
                                citySearchErrorMessage = "$MSG_CITY_SEARCH_ERROR${e.message}" // Используем константу
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
        _uiState.update {
            it.copy(
                selectedCityObject = city,
                cityForDisplay = formattedName, // Этот formattedName может потребовать переформатирования, если localNames будут использоваться
                searchQuery = formattedName,
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
            val apiLang = getApiLangCode()
            Log.d("ViewModelAPI", "Fetching weather and forecast for city: '${city.name}' with lang: '$apiLang'")

            _uiState.update {
                it.copy(
                    isLoadingWeather = true, weather = null, weatherErrorMessage = null,
                    isLoadingForecast = true, dailyForecasts = emptyList(), forecastErrorMessage = null
                )
            }

            var weatherFetchError: String? = null
            var forecastFetchError: String? = null
            var currentWeatherData: Weather? = null

            try {
                currentWeatherData = withContext(Dispatchers.IO) {
                    weatherApi.getWeather(
                        city = city.name, // Имя города, как оно есть в объекте City
                        apiKey = apiKey,
                        lang = apiLang // Передаем язык
                    )
                }
                _uiState.update { it.copy(weather = currentWeatherData) } // isLoadingWeather будет сброшен в конце
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
                            lang = apiLang // Передаем язык
                        )
                    }
                    // Передаем apiLang в processForecastResponse для локализации дат
                    val processedForecasts = processForecastResponse(forecastResponse, apiLang)
                    _uiState.update { it.copy(dailyForecasts = processedForecasts) } // isLoadingForecast будет сброшен в конце
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
                    forecastErrorMessage = forecastFetchError ?: it.forecastErrorMessage
                )
            }
        }
    }

    // Обновляем processForecastResponse для принятия языка и локализации формата дня недели
    private fun processForecastResponse(response: ForecastResponse, apiLanguageCode: String?): List<DisplayDayWeather> {
        val dailyData = mutableMapOf<String, MutableList<ru.devsoland.drydrive.data.ForecastListItem>>()

        // Определяем локаль для форматирования дня недели
        // Если apiLanguageCode null (системный, запросили у API на en), или пуст, используем Locale.getDefault()
        // или Locale("en") как фолбэк. Если код языка есть ("ru", "en"), используем его.
        val displayLocale = when {
            apiLanguageCode == null -> Locale.getDefault() // Для дат лучше использовать локаль устройства, если API вернул на en
            apiLanguageCode.isNotEmpty() -> Locale(apiLanguageCode)
            else -> Locale.getDefault() // Фолбэк на локаль устройства
        }
        Log.d("ProcessForecast", "Using locale for dayOfWeekFormat: $displayLocale (based on apiLang: $apiLanguageCode)")


        val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH) // Формат от API всегда одинаков
        inputFormat.timeZone = TimeZone.getTimeZone("UTC") // API возвращает в UTC
        val dayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH) // Для ключа карты
        val dayOfWeekFormat = SimpleDateFormat("E", displayLocale) // Локализуем день недели

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
        val todayCal = Calendar.getInstance() // Календарь с текущей локалью устройства

        dailyData.entries.sortedBy { it.key }.take(5).forEachIndexed { index, entry ->
            val dayItems = entry.value
            val targetItem = dayItems.find { it.dtTxt.substring(11, 13) == "12" } ?: dayItems.firstOrNull()

            targetItem?.let { item ->
                val itemDate = inputFormat.parse(item.dtTxt)
                val dayLabel = if (index == 0 && itemDate != null && isSameDay(itemDate, todayCal)) {
                    // "Сейчас" - эту строку лучше брать из ресурсов, если ViewModel имеет доступ к Context
                    // или передавать строку из Activity/Fragment. Пока оставим как есть.
                    // Для локализации этой строки:
                    // val nowString = applicationContext.getString(R.string.forecast_now)
                    "Сейчас" // TODO: Локализовать через ресурсы (потребует Context во ViewModel)
                } else {
                    itemDate?.let {
                        dayOfWeekFormat.format(it).replaceFirstChar { char ->
                            if (char.isLowerCase()) char.titlecase(displayLocale) else char.toString()
                        }
                    } ?: "???"
                }
                val iconRes = mapWeatherConditionToIcon(item.weather.firstOrNull()?.main, item.weather.firstOrNull()?.icon)
                val temperature = "${item.main.temp.toInt()}°" // Температура не локализуется в данном контексте
                displayForecasts.add(DisplayDayWeather(dayLabel, iconRes, temperature))
            }
        }
        return displayForecasts
    }

    private fun isSameDay(date: Date, calendar: Calendar): Boolean {
        val itemCalendar = Calendar.getInstance()
        itemCalendar.time = date
        return calendar.get(Calendar.YEAR) == itemCalendar.get(Calendar.YEAR) &&
                calendar.get(Calendar.DAY_OF_YEAR) == itemCalendar.get(Calendar.DAY_OF_YEAR)
    }

    private fun mapWeatherConditionToIcon(condition: String?, iconCode: String?): Int {
        // ... (ваш код без изменений) ...
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
            val previousLanguage = currentLanguage.value
            if (previousLanguage == language) {
                Log.d("ViewModelLanguage", "Language $language already selected. No action.")
                return@launch
            }

            languageManager.saveSelectedLanguage(language)
            Log.d("ViewModelLanguage", "Language with code '${language.code}' saved.")

            // После сохранения языка, данные (погода, прогноз) могут стать неактуальными
            // из-за изменения языка ответа API. Перезапросим их.
            _uiState.value.selectedCityObject?.let { city ->
                Log.d("ViewModelLanguage", "Language changed, refreshing weather data for ${city.name}")
                fetchCurrentWeatherAndForecast(city)
            } ?: run {
                // Если город не выбран, но язык изменился, возможно, нужно какое-то действие.
                // Например, если cityForDisplay - это просто строка, а не объект City,
                // то погоду для него мы и так не грузим до выбора City.
                // Если же у вас есть логика загрузки для cityForDisplay, ее тоже нужно учесть.
                Log.d("ViewModelLanguage", "Language changed, but no city selected to refresh weather.")
            }


            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                try {
                    val localeListToSet = if (language == AppLanguage.SYSTEM || language.code.isEmpty()) {
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
}

