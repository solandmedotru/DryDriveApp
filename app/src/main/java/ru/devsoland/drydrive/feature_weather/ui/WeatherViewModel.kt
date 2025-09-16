package ru.devsoland.drydrive.feature_weather.ui

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar // Для шин
import androidx.compose.material.icons.filled.LocalDrink // Для воды
import androidx.compose.material.icons.filled.Umbrella // Для зонта
import androidx.compose.material.icons.filled.WbSunny // Для УФ и общей солнечной погоды
import ru.devsoland.drydrive.BuildConfig
import ru.devsoland.drydrive.R

// Важно: Убедитесь, что R класс вашего проекта импортирован, если Android Studio не сделала это автоматически
// import ru.devsoland.drydrive.R

// Data class для отображения дневного прогноза (использует ID ресурса drawable)
data class DisplayDayWeather(
    val dayShort: String,
    val iconRes: Int,
    val temperature: String
)

// Data class для рекомендаций (использует ImageVector)
data class Recommendation(
    val id: String,
    val text: String,
    val icon: ImageVector,
    val isActive: Boolean = false,
    val activeColor: Color,
    val inactiveAlpha: Float = 0.3f,
    val activeAlpha: Float = 1.0f
)

@HiltViewModel
class WeatherViewModel @Inject constructor(
    private val weatherApi: WeatherApi,
    private val userPreferencesManager: UserPreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(WeatherUiState())
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    private val apiKey = BuildConfig.WEATHER_API_KEY
    private var citySearchJob: Job? = null
    private var initialLoadJob: Job? = null

    private companion object {
        const val TAG = "DryDriveViewModel"
        // TODO: Локализовать все эти сообщения
        const val MSG_CITIES_NOT_FOUND = "Города не найдены: "
        const val MSG_CITY_SEARCH_ERROR = "Ошибка поиска: "
        const val MSG_CITY_NOT_FOUND_WEATHER = "Город не найден: "
        const val MSG_WEATHER_LOAD_ERROR = "Ошибка при загрузке погоды: "
        const val MSG_FORECAST_LOAD_ERROR = "Ошибка загрузки прогноза: "
        const val MSG_COORDINATES_NOT_FOUND = "Координаты для прогноза не найдены."
        const val DEFAULT_CITY_FALLBACK = "Moscow" // Можно вынести в ресурсы, если нужно менять
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
                        Log.d(TAG, "Язык изменен через коллектор: ${appLang.code}")
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
            Log.d(TAG, "Init: Язык из настроек: '$initialLangCode'")

            _uiState.update { it.copy(currentLanguageCode = initialLangCode, isInitialLoading = true) }
            Log.d(TAG, "Init: UiState.currentLanguageCode установлен в '${_uiState.value.currentLanguageCode}'. Загрузка города...")

            val lastCity = userPreferencesManager.lastSelectedCityFlow.first()
            val apiLangForInit = initialLangCode.ifEmpty { null }

            if (lastCity != null) {
                Log.d(TAG, "Init: Найден последний город: ${lastCity.name}. Форматирование с языком: '$initialLangCode'")
                val formattedName = formatCityNameInternal(lastCity, initialLangCode)
                _uiState.update {
                    it.copy(
                        selectedCityObject = lastCity,
                        cityForDisplay = formattedName,
                        searchQuery = formattedName
                    )
                }
                fetchCurrentWeatherAndForecast(lastCity, apiLangForInit)
            } else {
                Log.d(TAG, "Init: Последний город не найден. Загрузка города по умолчанию: $DEFAULT_CITY_FALLBACK. Язык для поиска: '$initialLangCode'")
                findCityByNameAndFetchWeatherInternal(DEFAULT_CITY_FALLBACK, true, apiLangForInit)
            }
        }
    }

    private fun formatCityNameInternal(city: City, langCode: String): String {
        val displayName = when {
            langCode.isNotBlank() && city.localNames?.containsKey(langCode) == true -> city.localNames[langCode]
            city.localNames?.containsKey("en") == true -> city.localNames["en"] // Фоллбэк на английский
            else -> city.name
        } ?: city.name // Elvis на случай, если city.name тоже null (хотя API должен его давать)
        return "$displayName, ${city.country}" + if (!city.state.isNullOrBlank()) ", ${city.state}" else ""
    }

    private fun getApiLangCode(): String? {
        val langCode = _uiState.value.currentLanguageCode
        return if (langCode.isEmpty()) null else langCode // OpenWeatherMap ожидает null или код языка, не пустую строку
    }

    fun onEvent(event: WeatherEvent) {
        when (event) {
            is WeatherEvent.SearchQueryChanged -> handleSearchQueryChanged(event.query)
            is WeatherEvent.CitySelectedFromSearch -> handleCitySelected(event.city, event.formattedName)
            WeatherEvent.DismissCitySearchDropDown -> _uiState.update { it.copy(isSearchDropDownExpanded = false) }
            WeatherEvent.RefreshWeatherClicked -> {
                _uiState.value.selectedCityObject?.let {
                    fetchCurrentWeatherAndForecast(it) // Язык API будет взят из getApiLangCode()
                } ?: run {
                    Log.w(TAG, "Кнопка Refresh нажата, но город не выбран. Загрузка города по умолчанию.")
                    findCityByNameAndFetchWeatherInternal(DEFAULT_CITY_FALLBACK, true)
                }
            }
            WeatherEvent.ClearWeatherErrorMessage -> _uiState.update {
                it.copy(weatherErrorMessage = null, forecastErrorMessage = null)
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
        if (query.length > 2) { // Начинаем поиск после ввода 3 символов
            _uiState.update { it.copy(isLoadingCities = true) }
            citySearchJob = viewModelScope.launch {
                val apiLang = getApiLangCode()
                delay(500) // Debounce для предотвращения слишком частых запросов
                try {
                    val results = withContext(Dispatchers.IO) {
                        weatherApi.searchCities(query = query, apiKey = apiKey, lang = apiLang)
                    }
                    _uiState.update {
                        it.copy(
                            citySearchResults = results,
                            isLoadingCities = false,
                            citySearchErrorMessage = if (results.isEmpty()) "$MSG_CITIES_NOT_FOUND'$query'" else null,
                            isSearchDropDownExpanded = results.isNotEmpty() || (results.isEmpty() && query.isNotEmpty()) // Показывать выпадашку даже если пусто, но есть запрос
                        )
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Ошибка поиска городов для '$query': ${e.message}", e)
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
        initialLoadJob?.cancel() // Отменяем начальную загрузку, если она еще идет
        _uiState.update {
            it.copy(
                selectedCityObject = city,
                cityForDisplay = formattedName,
                searchQuery = formattedName, // Обновляем поисковый запрос, чтобы отразить выбранный город
                citySearchResults = emptyList(),
                isSearchDropDownExpanded = false,
                weatherErrorMessage = null,
                forecastErrorMessage = null,
                dailyForecasts = emptyList(), // Очищаем старый прогноз
                isInitialLoading = false // Пользовательское взаимодействие означает, что начальная загрузка завершена или переопределена
            )
        }
        fetchCurrentWeatherAndForecast(city)
        viewModelScope.launch {
            userPreferencesManager.saveLastSelectedCity(city)
        }
    }

    private fun fetchCurrentWeatherAndForecast(city: City, explicitApiLang: String? = null) {
        viewModelScope.launch {
            val apiLang = explicitApiLang ?: getApiLangCode()
            val callSource = if (explicitApiLang != null) "Init/LangChange" else "UserAction"
            Log.d(TAG, "Загрузка погоды для '${city.name}'. Язык API: '$apiLang' (Источник: $callSource)")

            _uiState.update {
                it.copy(
                    isLoadingWeather = true,
                    weather = null, // Очищаем старую погоду во время загрузки
                    weatherErrorMessage = null,
                    isLoadingForecast = true,
                    dailyForecasts = emptyList(), // Очищаем старый прогноз
                    forecastErrorMessage = null
                    // isInitialLoading здесь не трогаем, он будет false в конце этой функции
                )
            }

            var weatherFetchError: String? = null
            var forecastFetchError: String? = null
            var fetchedWeather: Weather? = null
            var processedForecasts: List<DisplayDayWeather> = emptyList()

            try {
                fetchedWeather = withContext(Dispatchers.IO) {
                    weatherApi.getWeather(city = city.name, apiKey = apiKey, lang = apiLang)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка загрузки текущей погоды: ${e.message}", e)
                weatherFetchError = if (e.message?.contains("404") == true) "$MSG_CITY_NOT_FOUND_WEATHER${city.name}" else "$MSG_WEATHER_LOAD_ERROR${e.localizedMessage}"
            }

            if (city.lat != 0.0 && city.lon != 0.0) {
                try {
                    val forecastResponse = withContext(Dispatchers.IO) {
                        weatherApi.getFiveDayForecast(lat = city.lat, lon = city.lon, apiKey = apiKey, lang = apiLang)
                    }
                    processedForecasts = processForecastResponse(forecastResponse, apiLang)
                } catch (e: Exception) {
                    Log.e(TAG, "Ошибка загрузки/обработки прогноза: ${e.message}", e)
                    forecastFetchError = "$MSG_FORECAST_LOAD_ERROR${e.localizedMessage}"
                }
            } else {
                Log.w(TAG, "Пропуск прогноза: Неверные Lat/Lon для ${city.name}.")
                forecastFetchError = MSG_COORDINATES_NOT_FOUND
            }

            val currentRecommendations = generateRecommendations(fetchedWeather, processedForecasts)

            _uiState.update {
                it.copy(
                    weather = fetchedWeather ?: it.weather, // Сохраняем старую погоду, если новая загрузка не удалась
                    dailyForecasts = processedForecasts,
                    isLoadingWeather = false,
                    isLoadingForecast = false,
                    weatherErrorMessage = weatherFetchError,
                    forecastErrorMessage = forecastFetchError,
                    isInitialLoading = false, // Последовательность начальной загрузки завершена
                    recommendations = currentRecommendations
                )
            }
            Log.d(TAG, "UiState обновлен. Кол-во дней прогноза: ${_uiState.value.dailyForecasts.size}, Рекомендаций: ${_uiState.value.recommendations.size}")
        }
    }

    private fun processForecastResponse(response: ForecastResponse, apiLanguageCode: String?): List<DisplayDayWeather> {
        val dailyData = mutableMapOf<String, MutableList<ForecastListItem>>()
        val displayLocale = when {
            apiLanguageCode.isNullOrEmpty() -> Locale.getDefault() // Используем язык системы, если API язык не указан
            else -> Locale(apiLanguageCode)
        }

        val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH).apply {
            timeZone = TimeZone.getTimeZone("UTC") // Даты от API приходят в UTC
        }
        val dayKeyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        // Формат для отображения дня недели будет использовать локаль устройства или выбранный язык
        val dayOfWeekFormat = SimpleDateFormat("E", displayLocale)

        response.list.forEach { item ->
            try {
                inputFormat.parse(item.dtTxt)?.let { utcDate ->
                    val dayKey = dayKeyFormat.format(utcDate)
                    dailyData.getOrPut(dayKey) { mutableListOf() }.add(item)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка парсинга даты из API: ${item.dtTxt} в прогнозе", e)
            }
        }

        val displayForecasts = mutableListOf<DisplayDayWeather>()
        // Получаем текущий день в UTC для фильтрации, чтобы не показывать сегодняшний день (т.к. прогноз обычно на следующие дни)
        val currentUtcDayKey = dayKeyFormat.format(Date())

        dailyData.entries
            .asSequence()
            .sortedBy { it.key } // Сортируем по дате
            .filter { entry -> entry.key > currentUtcDayKey } // Берем только будущие дни
            .take(5) // Берем следующие 5 дней
            .forEach { entry ->
                val dayItems = entry.value
                // Пытаемся найти запись на 12:00 UTC как репрезентативную для дня
                val targetItem = dayItems.find { it.dtTxt.substring(11, 13) == "12" } ?: dayItems.firstOrNull()

                targetItem?.let { item ->
                    inputFormat.parse(item.dtTxt)?.let { itemUtcDate ->
                        // Важно: Чтобы день недели отображался на языке пользователя, SimpleDateFormat должен быть инициализирован с Locale пользователя
                        // DayOfWeekFormat уже использует displayLocale (либо язык API, либо системный)
                        val dayLabel = dayOfWeekFormat.format(itemUtcDate).replaceFirstChar {
                            if (it.isLowerCase()) it.titlecase(displayLocale) else it.toString()
                        }
                        val iconRes = mapWeatherConditionToIcon(item.weather.firstOrNull()?.main, item.weather.firstOrNull()?.icon)
                        val temperature = "${item.main.temp.toInt()}°" // Округляем температуру
                        displayForecasts.add(DisplayDayWeather(dayLabel, iconRes, temperature))
                    }
                }
            }
        return displayForecasts
    }
    // Эта функция возвращает ID ресурса drawable для иконок прогноза
    private fun mapWeatherConditionToIcon(condition: String?, iconCode: String?): Int {
        // TODO: Доработать маппинг иконок и добавить все необходимые drawable ресурсы
        return when (iconCode) {
            "01d" -> R.drawable.ic_sun_filled // Ясно (день)
            "01n" -> R.drawable.ic_moon_filled // Ясно (ночь) - нужна иконка луны
            "02d" -> R.drawable.ic_sun_cloud_filled // Малооблачно (день) - нужна иконка
            "02n" -> R.drawable.ic_moon_cloud_filled // Малооблачно (ночь) - нужна иконка
            "03d", "03n" -> R.drawable.ic_light_cloud// Рассеянные облака
            "04d", "04n" -> R.drawable.ic_clouds_filled // Облачно, пасмурно - нужна иконка
            "09d", "09n" -> R.drawable.ic_rain_heavy_filled // Ливень - нужна иконка
            "10d" -> R.drawable.ic_sun_rain_filled // Дождь (день) - нужна иконка
            "10n" -> R.drawable.ic_moon_rain_filled // Дождь (ночь) - нужна иконка
            "11d", "11n" -> R.drawable.ic_thunderstorm_filled // Гроза - нужна иконка
            "13d", "13n" -> R.drawable.ic_snow_filled // Снег - нужна иконка
            "50d", "50n" -> R.drawable.ic_fog_filled // Туман - нужна иконка
            else -> R.drawable.ic_sun_cloud_filled // Иконка по умолчанию
        }
    }
    private fun generateRecommendations(weatherData: Weather?, forecastData: List<DisplayDayWeather>?): List<Recommendation> {
        val recommendations = mutableListOf<Recommendation>()
        if (weatherData == null) return emptyList()

        val currentTemp = weatherData.main.temp

        // 1. Пейте воду
        val isHot = currentTemp > 25.0 // TODO: Сделать порог настраиваемым или более умным
        recommendations.add(
            Recommendation(
                id = "drink_water",
                text = "Пейте больше воды", // TODO: Локализовать
                icon = Icons.Filled.LocalDrink,
                isActive = isHot,
                activeColor = Color(0xFF4FC3F7) // Голубой
            )
        )

        // 2. Высокий УФ (Очень упрощенное условие)
        val weatherCondition = weatherData.weather.firstOrNull()
        val iconCode = weatherCondition?.icon

        val isSunnyOrFewCloudsDay = when (iconCode) {
            "01d", "02d" -> true // "01d" - ясно, "02d" - малооблачно (день)
            else -> false
        }

        val isWarmEnoughForUvConcern = currentTemp > 15.0 // Порог температуры для УФ-опасений
        recommendations.add(
            Recommendation(
                id = "high_uv",
                text = "Защититесь от солнца", // TODO: Локализовать
                icon = Icons.Filled.WbSunny,
                isActive = isSunnyOrFewCloudsDay && isWarmEnoughForUvConcern,
                activeColor = Color(0xFFFFD54F) // Желтый
            )
        )

        // 3. Шины / Гололед
        val isColdForTires = currentTemp < 3.0 // TODO: Улучшить условие (учесть осадки)
        recommendations.add(
            Recommendation(
                id = "check_tires",
                text = "Возможен гололед, осторожнее!", // TODO: Локализовать
                icon = Icons.Filled.DirectionsCar,
                isActive = isColdForTires,
                activeColor = Color(0xFF81D4FA) // Светло-голубой
            )
        )

        // 4. Зонтик
        val isRainingNow = weatherData.weather.any { it.main.contains("Rain", ignoreCase = true) }

        // ID ресурсов для иконок дождя из функции mapWeatherConditionToIcon
        // Убедитесь, что эта функция возвращает правильные ID для этих кодов!
        val rainIconResId = mapWeatherConditionToIcon(null, "10d")
        val showerRainIconResId = mapWeatherConditionToIcon(null, "09d")

        val willRainSoon = forecastData?.take(2)?.any { dailyWeather -> // Проверяем следующие 2 дня
            dailyWeather.iconRes == rainIconResId || dailyWeather.iconRes == showerRainIconResId
        } ?: false

        recommendations.add(
            Recommendation(
                id = "take_umbrella",
                text = "Возьмите зонт", // TODO: Локализовать
                icon = Icons.Filled.Umbrella,
                isActive = isRainingNow || willRainSoon,
                activeColor = Color(0xFFFFB74D) // Оранжевый
            )
        )
        return recommendations
    }


    fun onLanguageSelected(language: AppLanguage) {
        viewModelScope.launch {
            val previousLangCode = _uiState.value.currentLanguageCode
            val newLangCode = language.code

            // Избегаем избыточной обработки, если язык не изменился
            // Особый случай для AppLanguage.SYSTEM, если currentLanguageCode уже пуст (означает системный)
            if (language == AppLanguage.SYSTEM && previousLangCode.isEmpty()) {
                Log.d(TAG, "Language selection skipped: already system language and no specific language was previously set.")
                return@launch
            }
            if (language != AppLanguage.SYSTEM && previousLangCode == newLangCode) {
                Log.d(TAG, "Language selection skipped: language ${language.name} is already selected.")
                return@launch
            }
            Log.d(TAG, "Выбран язык: ${language.name}. Код: '$newLangCode'")
            userPreferencesManager.saveSelectedLanguage(language)
            // UiState.currentLanguageCode обновится через коллектор currentLanguage.onEach
            // Это также вызовет обновление apiLangForUpdate и cityForDisplay через последующие вызовы

            val apiLangForUpdate = newLangCode.ifEmpty { null }

            _uiState.value.selectedCityObject?.let { city ->
                Log.d(TAG, "Язык изменен, обновление данных для ${city.name}")
                // Обновляем отображаемое имя города и поисковый запрос, если он совпадал с отображаемым именем
                // Это изменение в UiState произойдет до перезагрузки погоды и до recreate
                val newFormattedName = formatCityNameInternal(city, newLangCode)
                val oldFormattedName = formatCityNameInternal(city, previousLangCode)
                _uiState.update {
                    it.copy(
                        cityForDisplay = newFormattedName,
                        searchQuery = if (it.searchQuery == oldFormattedName) newFormattedName else it.searchQuery
                        // currentLanguageCode будет обновлен через flow от userPreferencesManager,
                        // который коллектится в init {} и обновляет uiState
                    )
                }
                fetchCurrentWeatherAndForecast(city, apiLangForUpdate)
            } ?: run {
                if (_uiState.value.selectedCityObject == null && _uiState.value.cityForDisplay.contains(DEFAULT_CITY_FALLBACK, ignoreCase = true)) {
                    Log.d(TAG, "Язык изменен, город не выбран, обновление данных для города по умолчанию.")
                    findCityByNameAndFetchWeatherInternal(DEFAULT_CITY_FALLBACK, true, apiLangForUpdate)
                }
            }

            // Установка локали приложения
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                try {
                    val localeListToSet = if (newLangCode.isEmpty()) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(newLangCode)
                    AppCompatDelegate.setApplicationLocales(localeListToSet)
                    Log.d(TAG, "AppCompatDelegate.setApplicationLocales called for API 33+ with '$newLangCode'")
                } catch (e: Exception) {
                    Log.e(TAG, "Ошибка установки локали приложения на API 33+: ${e.message}", e)
                }
            }
            // НЕЗАВИСИМО ОТ ВЕРСИИ ANDROID, вызываем recreate для теста
            _recreateActivityEvent.emit(Unit)
            Log.d(TAG, "Forced recreateActivityEvent emitted after language change.")
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
            // Язык для поиска города: если это начальная загрузка/смена языка, используем explicitApiLang, иначе текущий язык приложения
            val langForCitySearch = explicitApiLang ?: getApiLangCode()
            Log.d(TAG, "Внутренний поиск: Поиск города: '$cityName'. Язык API для поиска: '$langForCitySearch'")

            try {
                val cities = withContext(Dispatchers.IO) {
                    weatherApi.searchCities(query = cityName, apiKey = apiKey, limit = 1, lang = langForCitySearch)
                }
                cities.firstOrNull()?.let { foundCity ->
                    // Язык для отображения: используем текущий язык приложения (из _uiState) или explicitApiLang если он есть
                    val langForDisplay = _uiState.value.currentLanguageCode.ifEmpty { explicitApiLang ?: "" }
                    val formattedName = formatCityNameInternal(foundCity, langForDisplay)
                    Log.d(TAG, "Внутренний поиск: Город найден: ${foundCity.name}. Язык отображения: '$langForDisplay', Форматированное имя: '$formattedName'")

                    _uiState.update {
                        it.copy(
                            selectedCityObject = foundCity,
                            cityForDisplay = formattedName,
                            searchQuery = formattedName
                        )
                    }
                    // Передаем explicitApiLang для единообразия языка при загрузке погоды
                    fetchCurrentWeatherAndForecast(foundCity, explicitApiLang)
                    if (isInitialOrFallback) { // Сохраняем город только если это начальная загрузка или загрузка по умолчанию
                        userPreferencesManager.saveLastSelectedCity(foundCity)
                    }
                } ?: run {
                    Log.w(TAG, "Внутренний поиск: Город '$cityName' не найден через API.")
                    _uiState.update {
                        it.copy(
                            cityForDisplay = if (it.cityForDisplay.isEmpty()) "$MSG_CITY_NOT_FOUND_WEATHER $cityName" else it.cityForDisplay,
                            weatherErrorMessage = "$MSG_CITY_NOT_FOUND_WEATHER $cityName",
                            isLoadingWeather = false, isLoadingForecast = false, isInitialLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Внутренний поиск: Ошибка поиска/загрузки города '$cityName': ${e.message}", e)
                _uiState.update {
                    it.copy(
                        weatherErrorMessage = "$MSG_CITY_SEARCH_ERROR ${e.localizedMessage}",
                        isLoadingWeather = false, isLoadingForecast = false, isInitialLoading = false
                    )
                }
            }
        }
    }
}