package ru.devsoland.drydrive

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.devsoland.drydrive.data.City
import ru.devsoland.drydrive.data.ForecastResponse
import ru.devsoland.drydrive.data.Weather
import ru.devsoland.drydrive.data.WeatherApi
import ru.devsoland.drydrive.ui.DisplayDayWeather // <--- ДОБАВЬТЕ ЭТОТ ИМПОРТ (путь к вашему файлу)
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

data class DryDriveUiState(
    val weather: Weather? = null,
    val isLoadingWeather: Boolean = false,val weatherErrorMessage: String? = null,

    val searchQuery: String = "",
    val citySearchResults: List<City> = emptyList(),
    val isLoadingCities: Boolean = false,
    val citySearchErrorMessage: String? = null,
    val isSearchDropDownExpanded: Boolean = false,

    val cityForDisplay: String = "Moscow",
    val selectedCityObject: City? = null,

    val dailyForecasts: List<DisplayDayWeather> = emptyList(), // Теперь DisplayDayWeather из импорта
    val isLoadingForecast: Boolean = false,
    val forecastErrorMessage: String? = null
)

data class DisplayDayWeather(
    val dayShort: String,
    val iconRes: Int,
    val temperature: String
)

@HiltViewModel
class DryDriveViewModel @Inject constructor(
    private val weatherApi: WeatherApi
) : ViewModel() {

    private val _uiState = MutableStateFlow(DryDriveUiState())
    val uiState: StateFlow<DryDriveUiState> = _uiState.asStateFlow()

    private val apiKey = BuildConfig.WEATHER_API_KEY
    private var citySearchJob: Job? = null

    private companion object {
        const val MSG_CITIES_NOT_FOUND = "Города не найдены: "
        const val MSG_CITY_SEARCH_ERROR = "Ошибка поиска: "
        const val MSG_CITY_NOT_FOUND_WEATHER = "Город не найден: "
        const val MSG_WEATHER_LOAD_ERROR = "Ошибка при загрузке погоды: "
        const val MSG_FORECAST_LOAD_ERROR = "Ошибка загрузки прогноза: " // Для ошибок прогноза
    }

    init {
        _uiState.update { it.copy(searchQuery = it.cityForDisplay) }
    }

    fun onEvent(event: DryDriveEvent) {
        when (event) {
            is DryDriveEvent.SearchQueryChanged -> {
                handleSearchQueryChanged(event.query)
            }
            is DryDriveEvent.CitySelectedFromSearch -> {
                handleCitySelected(event.city, event.formattedName)
            }
            DryDriveEvent.DismissCitySearchDropDown -> {
                _uiState.update { it.copy(isSearchDropDownExpanded = false) }
            }
            DryDriveEvent.RefreshWeatherClicked -> {
                _uiState.value.selectedCityObject?.let {
                    // При обновлении запрашиваем и текущую погоду, и прогноз
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
                Log.d("ViewModelEvents", "Starting search coroutine for: '$query'")
                delay(500)
                try {
                    val results = withContext(Dispatchers.IO) {
                        weatherApi.searchCities(query = query, apiKey = apiKey)
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
        _uiState.update {
            it.copy(
                selectedCityObject = city,
                cityForDisplay = formattedName,
                searchQuery = formattedName,
                citySearchResults = emptyList(),
                isSearchDropDownExpanded = false,
                weatherErrorMessage = null, // Сбрасываем все предыдущие ошибки погоды/прогноза
                forecastErrorMessage = null,
                dailyForecasts = emptyList() // Очищаем старые прогнозы
            )
        }
        fetchCurrentWeatherAndForecast(city) // Загружаем и текущую погоду, и прогноз
    }

    // Новая общая функция для загрузки и текущей погоды, и прогноза
    private fun fetchCurrentWeatherAndForecast(city: City) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoadingWeather = true, weather = null, weatherErrorMessage = null,
                    isLoadingForecast = true, dailyForecasts = emptyList(), forecastErrorMessage = null
                )
            }

            var weatherFetchError: String? = null
            var forecastFetchError: String? = null
            var currentWeatherData: Weather? = null

            // 1. Запрос текущей погоды
            try {
                currentWeatherData = withContext(Dispatchers.IO) {
                    weatherApi.getWeather(city = city.name, apiKey = apiKey)
                }
                _uiState.update { it.copy(weather = currentWeatherData, isLoadingWeather = false) }
            } catch (e: Exception) {
                Log.e("ViewModelFetch", "Error fetching current weather for ${city.name}: ${e.message}", e)
                weatherFetchError = if (e.message?.contains("404") == true) {
                    "$MSG_CITY_NOT_FOUND_WEATHER${city.name}"
                } else {
                    "$MSG_WEATHER_LOAD_ERROR${e.message}"
                }
                // Не обновляем isLoadingWeather = false здесь, сделаем это в конце
            }

            // 2. Запрос прогноза (только если текущая погода загрузилась или если хотим пытаться всегда)
            // Здесь мы запрашиваем прогноз независимо от успеха первого запроса,
            // но можно добавить условие if (currentWeatherData != null)
            if (city.lat != 0.0 && city.lon != 0.0) { // Убедимся, что координаты есть
                try {
                    val forecastResponse = withContext(Dispatchers.IO) {
                        weatherApi.getFiveDayForecast(lat = city.lat, lon = city.lon, apiKey = apiKey)
                    }
                    val processedForecasts = processForecastResponse(forecastResponse)
                    _uiState.update { it.copy(dailyForecasts = processedForecasts, isLoadingForecast = false) }
                } catch (e: Exception) {
                    Log.e("ViewModelFetch", "Error fetching forecast for ${city.name}: ${e.message}", e)
                    forecastFetchError = "$MSG_FORECAST_LOAD_ERROR${e.message}"
                    // Не обновляем isLoadingForecast = false здесь
                }
            } else {
                Log.w("ViewModelFetch", "Skipping forecast fetch due to missing coordinates for ${city.name}")
                forecastFetchError = "Координаты для прогноза не найдены." // Ошибка, если нет координат
            }

            // Финальное обновление состояний загрузки и ошибок
            _uiState.update {
                it.copy(
                    isLoadingWeather = false, // В любом случае загрузка текущей погоды завершена (успешно или с ошибкой)
                    isLoadingForecast = false, // В любом случае загрузка прогноза завершена
                    weatherErrorMessage = weatherFetchError ?: it.weatherErrorMessage, // Если есть новая ошибка, ставим ее
                    forecastErrorMessage = forecastFetchError ?: it.forecastErrorMessage
                )
            }
        }
    }


    private fun processForecastResponse(response: ForecastResponse): List<DisplayDayWeather> {
        val dailyData = mutableMapOf<String, MutableList<ru.devsoland.drydrive.data.ForecastListItem>>()

        val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        inputFormat.timeZone = TimeZone.getTimeZone("UTC")
        val dayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dayOfWeekFormat = SimpleDateFormat("E", Locale("ru"))

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

        val displayForecasts = mutableListOf<DisplayDayWeather>() // Использует импортированный DisplayDayWeather
        val todayCal = Calendar.getInstance()

        dailyData.entries.sortedBy { it.key }.take(5).forEachIndexed { index, entry ->
            val dayItems = entry.value
            val targetItem = dayItems.find { it.dtTxt.substring(11, 13) == "12" } ?: dayItems.firstOrNull()

            targetItem?.let { item ->
                val itemDate = inputFormat.parse(item.dtTxt)
                val dayLabel = if (index == 0 && itemDate != null && isSameDay(itemDate, todayCal)) {
                    "Сейчас"
                } else {
                    itemDate?.let { dayOfWeekFormat.format(it).replaceFirstChar { char -> if (char.isLowerCase()) char.titlecase(Locale("ru")) else char.toString() } } ?: "???"
                }
                val iconRes = mapWeatherConditionToIcon(item.weather.firstOrNull()?.main, item.weather.firstOrNull()?.icon)
                val temperature = "${item.main.temp.toInt()}°"
                displayForecasts.add(DisplayDayWeather(dayLabel, iconRes, temperature)) // Создает импортированный DisplayDayWeather
            }
        }
        return displayForecasts
    }

    private fun isSameDay(date: Date, calendar: Calendar): Boolean { // date не null
        val itemCalendar = Calendar.getInstance()
        itemCalendar.time = date
        return calendar.get(Calendar.YEAR) == itemCalendar.get(Calendar.YEAR) &&
                calendar.get(Calendar.DAY_OF_YEAR) == itemCalendar.get(Calendar.DAY_OF_YEAR)
    }

    private fun mapWeatherConditionToIcon(condition: String?, iconCode: String?): Int {
        // ... (код функции mapWeatherConditionToIcon без изменений, убедитесь, что R.drawable.xxx существуют)
        // Пример для ясности:
        return when (iconCode) {
            "01d" -> R.drawable.ic_sun_filled
            "01n" -> R.drawable.ic_sun_filled // Предполагается, что такая иконка есть
            "02d" -> R.drawable.ic_sun_filled // Предполагается
            "03d" -> R.drawable.ic_cloud // Облачно
            "04d" -> R.drawable.ic_sun_filled // Сильная облачность, можно ту же ic_cloud или другую
            "02n" -> R.drawable.ic_sun_filled // Предполагается
            "03n", "04n" -> R.drawable.ic_cloud // Ночная облачность
            "09d", "09n" -> R.drawable.ic_sun_filled
            "10d" -> R.drawable.ic_sun_filled
            "10n" -> R.drawable.ic_sun_filled // или просто ic_rain
            "11d", "11n" -> R.drawable.ic_sun_filled
            "13d", "13n" -> R.drawable.ic_sun_filled
            "50d", "50n" -> R.drawable.ic_sun_filled
            else -> R.drawable.ic_cloud // Иконка по умолчанию
        }
    }
}
