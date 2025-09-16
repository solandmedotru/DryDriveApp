package ru.devsoland.drydrive.feature_weather.ui

import android.app.Application
import android.os.Build
import android.util.Log
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import ru.devsoland.drydrive.data.api.model.City
import ru.devsoland.drydrive.data.api.model.ForecastListItem
import ru.devsoland.drydrive.data.api.model.ForecastResponse
import ru.devsoland.drydrive.data.api.model.Weather
import ru.devsoland.drydrive.data.WeatherApi
import ru.devsoland.drydrive.common.model.AppLanguage
import ru.devsoland.drydrive.data.preferences.UserPreferencesManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Umbrella
import androidx.compose.material.icons.filled.WbSunny
// Импортируйте иконки для НОВЫХ рекомендаций, если они нужны
import androidx.compose.material.icons.filled.Thermostat // для Dress warmly
import androidx.compose.material.icons.filled.AccessibilityNew // для Limit activity (пример)
import androidx.compose.material.icons.filled.Build // для Check tires (альтернатива DirectionsCar)
import androidx.compose.material.icons.filled.Warning // для Careful roads
import androidx.compose.material.icons.filled.Air // для Ventilate
import androidx.compose.material.icons.filled.Spa // для Moisturizer
import androidx.compose.material.icons.filled.DryCleaning // для Light clothes (пример)

import ru.devsoland.drydrive.BuildConfig
import ru.devsoland.drydrive.R

data class DisplayDayWeather(
    val dayShort: String,
    val iconRes: Int,
    val temperature: String
)

data class Recommendation(
    val id: String,
    @StringRes val textResId: Int, // ID ресурса для текста
    @StringRes val descriptionResId: Int, // ID ресурса для описания
    val icon: ImageVector,
    val isActive: Boolean = false,
    val activeColor: Color,
    val inactiveAlpha: Float = 0.3f,
    val activeAlpha: Float = 1.0f
)

@HiltViewModel
class WeatherViewModel @Inject constructor(
    private val application: Application,
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
                if (_uiState.value.currentLanguageCode != appLang.code) {
                    Log.d(TAG, "currentLanguage.onEach: Код языка в UiState обновлен на: ${appLang.code}")
                    _uiState.update { it.copy(currentLanguageCode = appLang.code) }
                    // Рекомендации будут обновлены, когда fetchCurrentWeatherAndForecast
                    // вызовет generateRecommendations после смены языка и загрузки данных.
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
            city.localNames?.containsKey("en") == true -> city.localNames["en"]
            else -> city.name
        } ?: city.name
        return "$displayName, ${city.country}" + if (!city.state.isNullOrBlank()) ", ${city.state}" else ""
    }

    private fun getApiLangCode(): String? {
        val langCode = _uiState.value.currentLanguageCode
        return if (langCode.isEmpty()) null else langCode
    }

    fun onEvent(event: WeatherEvent) {
        when (event) {
            is WeatherEvent.SearchQueryChanged -> handleSearchQueryChanged(event.query)
            is WeatherEvent.CitySelectedFromSearch -> handleCitySelected(event.city, event.formattedName)
            WeatherEvent.DismissCitySearchDropDown -> _uiState.update { it.copy(isSearchDropDownExpanded = false) }
            WeatherEvent.RefreshWeatherClicked -> {
                _uiState.value.selectedCityObject?.let {
                    fetchCurrentWeatherAndForecast(it)
                } ?: run {
                    Log.w(TAG, "Кнопка Refresh нажата, но город не выбран. Загрузка города по умолчанию.")
                    findCityByNameAndFetchWeatherInternal(DEFAULT_CITY_FALLBACK, true)
                }
            }
            WeatherEvent.ClearWeatherErrorMessage -> _uiState.update {
                it.copy(weatherErrorMessage = null, forecastErrorMessage = null)
            }

            is WeatherEvent.RecommendationClicked -> {
                Log.d(TAG, "RecommendationClicked: ${event.recommendation.id}")
                _uiState.update {
                    it.copy(
                        showRecommendationDialog = true,
                        // Используем textResId и descriptionResId, как мы определили в Recommendation data class
                        recommendationDialogTitleResId = event.recommendation.textResId,
                        recommendationDialogDescriptionResId = event.recommendation.descriptionResId
                    )
                }
            }
            WeatherEvent.DismissRecommendationDialog -> {
                Log.d(TAG, "DismissRecommendationDialog")
                _uiState.update {
                    it.copy(
                        showRecommendationDialog = false,
                        recommendationDialogTitleResId = null, // Сбрасываем ID, чтобы избежать случайного показа старых данных
                        recommendationDialogDescriptionResId = null
                    )
                }
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
                delay(500)
                try {
                    val results = withContext(Dispatchers.IO) {
                        weatherApi.searchCities(query = query, apiKey = apiKey, lang = apiLang)
                    }
                    _uiState.update {
                        it.copy(
                            citySearchResults = results,
                            isLoadingCities = false,
                            citySearchErrorMessage = if (results.isEmpty()) application.applicationContext.getString(R.string.city_not_found) else null,
                            isSearchDropDownExpanded = results.isNotEmpty() || (results.isEmpty() && query.isNotEmpty())
                        )
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Ошибка поиска городов для '$query': ${e.message}", e)
                    _uiState.update {
                        it.copy(
                            isLoadingCities = false,
                            citySearchErrorMessage = application.applicationContext.getString(R.string.error_loading_cities)
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

    private fun fetchCurrentWeatherAndForecast(city: City, explicitApiLang: String? = null) {
        viewModelScope.launch {
            val apiLang = explicitApiLang ?: getApiLangCode()
            val callSource = if (explicitApiLang != null) "Init/LangChange" else "UserAction"
            Log.d(TAG, "Загрузка погоды для '${city.name}'. Язык API: '$apiLang' (Источник: $callSource)")

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
            var forecastFetchError: String? = null
            var fetchedWeather: Weather? = null
            var processedForecasts: List<DisplayDayWeather> = emptyList()

            try {
                fetchedWeather = withContext(Dispatchers.IO) {
                    weatherApi.getWeather(city = city.name, apiKey = apiKey, lang = apiLang)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка загрузки текущей погоды: ${e.message}", e)
                weatherFetchError = if (e.message?.contains("404", ignoreCase = true) == true || e.message?.contains("city not found", ignoreCase = true) == true) {
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
                    processedForecasts = processForecastResponse(forecastResponse, apiLang)
                } catch (e: Exception) {
                    Log.e(TAG, "Ошибка загрузки/обработки прогноза: ${e.message}", e)
                    forecastFetchError = application.applicationContext.getString(R.string.error_loading_weather)
                }
            } else {
                Log.w(TAG, "Пропуск прогноза: Неверные Lat/Lon для ${city.name}.")
                forecastFetchError = application.applicationContext.getString(R.string.error_loading_weather) // Или более специфичная строка
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
            Log.d(TAG, "UiState обновлен. Кол-во дней прогноза: ${_uiState.value.dailyForecasts.size}, Рекомендаций: ${_uiState.value.recommendations.size}")
        }
    }

    private fun processForecastResponse(response: ForecastResponse, apiLanguageCode: String?): List<DisplayDayWeather> {
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
        val currentUtcDayKey = dayKeyFormat.format(Date())

        dailyData.entries
            .asSequence()
            .sortedBy { it.key }
            .filter { entry -> entry.key > currentUtcDayKey }
            .take(5)
            .forEach { entry ->
                val dayItems = entry.value
                val targetItem = dayItems.find { it.dtTxt.substring(11, 13) == "12" } ?: dayItems.firstOrNull()

                targetItem?.let { item ->
                    inputFormat.parse(item.dtTxt)?.let { itemUtcDate ->
                        val dayLabel = dayOfWeekFormat.format(itemUtcDate).replaceFirstChar {
                            if (it.isLowerCase()) it.titlecase(displayLocale) else it.toString()
                        }
                        val iconRes = mapWeatherConditionToIcon(item.weather.firstOrNull()?.main, item.weather.firstOrNull()?.icon)
                        val temperature = "${item.main.temp.toInt()}°"
                        displayForecasts.add(DisplayDayWeather(dayLabel, iconRes, temperature))
                    }
                }
            }
        return displayForecasts
    }

    private fun mapWeatherConditionToIcon(condition: String?, iconCode: String?): Int {
        return when (iconCode) {
            "01d" -> R.drawable.ic_sun_filled
            "01n" -> R.drawable.ic_moon_filled
            "02d" -> R.drawable.ic_sun_cloud_filled
            "02n" -> R.drawable.ic_moon_cloud_filled
            "03d", "03n" -> R.drawable.ic_light_cloud
            "04d", "04n" -> R.drawable.ic_clouds_filled
            "09d", "09n" -> R.drawable.ic_rain_heavy_filled
            "10d" -> R.drawable.ic_sun_rain_filled
            "10n" -> R.drawable.ic_moon_rain_filled
            "11d", "11n" -> R.drawable.ic_thunderstorm_filled
            "13d", "13n" -> R.drawable.ic_snow_filled
            "50d", "50n" -> R.drawable.ic_fog_filled
            else -> R.drawable.ic_sun_cloud_filled
        }
    }

    private fun generateRecommendations(weatherData: Weather?, forecastData: List<DisplayDayWeather>?): List<Recommendation> {
        // Логгирование локали здесь теперь менее важно, так как строки будут извлекаться в UI
        // val appCtx = application.applicationContext
        // val currentLocale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        //     appCtx.resources.configuration.locales[0]
        // } else {
        //     appCtx.resources.configuration.locale
        // }
        // Log.d(TAG, "generateRecommendations: Current locale check (ViewModel): $currentLocale")

        val recommendations = mutableListOf<Recommendation>()
        if (weatherData == null) {
            return emptyList()
        }
        val currentTemp = weatherData.main.temp

        // 1. Пейте воду
        val isHot = currentTemp > 25.0
        recommendations.add(
            Recommendation(
                id = "drink_water",
                textResId = if (isHot) R.string.rec_drink_water_active else R.string.rec_drink_water_default,
                descriptionResId = if (isHot) R.string.rec_drink_water_desc_active else R.string.rec_drink_water_desc_default,
                icon = Icons.Filled.LocalDrink,
                isActive = isHot,
                activeColor = Color(0xFF4FC3F7)
            )
        )

        // 2. Высокий УФ
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
                activeColor = Color(0xFFFFD54F)
            )
        )

        // 3. Шины / Гололед
        val isColdForTires = currentTemp < 3.0
        recommendations.add(
            Recommendation(
                id = "check_tires",
                textResId = if (isColdForTires) R.string.rec_tire_change_active else R.string.rec_tire_change_default,
                descriptionResId = if (isColdForTires) R.string.rec_tire_change_desc_active else R.string.rec_tire_change_desc_default,
                icon = Icons.Filled.DirectionsCar,
                isActive = isColdForTires,
                activeColor = Color(0xFF81D4FA)
            )
        )

        // 4. Зонтик
        val isRainingNow = weatherData.weather.any { it.main.contains("Rain", ignoreCase = true) }
        val rainIconResId = mapWeatherConditionToIcon(null, "10d")
        val showerRainIconResId = mapWeatherConditionToIcon(null, "09d")
        val willRainSoon = forecastData?.take(2)?.any { dailyWeather ->
            dailyWeather.iconRes == rainIconResId || dailyWeather.iconRes == showerRainIconResId
        } ?: false
        val isUmbrellaNeeded = isRainingNow || willRainSoon
        recommendations.add(
            Recommendation(
                id = "take_umbrella",
                textResId = if (isUmbrellaNeeded) R.string.rec_umbrella_active else R.string.rec_umbrella_default,
                descriptionResId = if (isUmbrellaNeeded) R.string.rec_umbrella_desc_active else R.string.rec_umbrella_desc_default,
                icon = Icons.Filled.Umbrella,
                isActive = isUmbrellaNeeded,
                activeColor = Color(0xFFFFB74D)
            )
        )

        // --- НОВЫЕ РЕКОМЕНДАЦИИ (продолжите для всех) ---
        // 5. Dress warmly
        val needsWarmClothes = currentTemp < 10.0
        recommendations.add(
            Recommendation(
                id = "dress_warmly",
                textResId = if (needsWarmClothes) R.string.rec_dress_warmly_active else R.string.rec_dress_warmly_default,
                descriptionResId = if (needsWarmClothes) R.string.rec_dress_warmly_desc_active else R.string.rec_dress_warmly_desc_default,
                icon = Icons.Filled.Thermostat,
                isActive = needsWarmClothes,
                activeColor = Color(0xFFB0BEC5)
            )
        )

        // 6. Limit outdoor activity
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
                activeColor = Color(0xFFEF5350)
            )
        )
        // Добавьте остальные новые рекомендации (check_tires уже был, careful_roads, ventilate, moisturizer, light_clothes) по аналогии...
        // Не забудьте для каждой добавить Log.d, чтобы видеть, какие строки извлекаются

        return recommendations
    }


    fun onLanguageSelected(language: AppLanguage) {
        viewModelScope.launch {
            val previousLangCode = _uiState.value.currentLanguageCode
            val newLangCode = language.code

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

            val apiLangForUpdate = newLangCode.ifEmpty { null }

            val cityToUpdate = _uiState.value.selectedCityObject
            val cityForDisplayFromState = _uiState.value.cityForDisplay

            if (cityToUpdate != null) {
                Log.d(TAG, "onLanguageSelected: Язык изменен, обновление данных для ${cityToUpdate.name}")
                val newFormattedName = formatCityNameInternal(cityToUpdate, newLangCode)
                val oldFormattedName = formatCityNameInternal(cityToUpdate, previousLangCode)
                _uiState.update {
                    it.copy(
                        cityForDisplay = newFormattedName,
                        searchQuery = if (it.searchQuery == oldFormattedName) newFormattedName else it.searchQuery
                        // currentLanguageCode обновится через коллектор в init{}
                    )
                }
                fetchCurrentWeatherAndForecast(cityToUpdate, apiLangForUpdate)
            } else if (cityForDisplayFromState.contains(DEFAULT_CITY_FALLBACK, ignoreCase = true) || cityForDisplayFromState.isEmpty()) {
                Log.d(TAG, "onLanguageSelected: Язык изменен, город не выбран (или по умолчанию), обновление данных для города по умолчанию.")
                findCityByNameAndFetchWeatherInternal(DEFAULT_CITY_FALLBACK, true, apiLangForUpdate)
            } else {
                Log.d(TAG, "onLanguageSelected: Язык изменен, но город не выбран и не является городом по умолчанию. UI должен обновиться без перезагрузки погоды (только через currentLanguage.onEach).")
                // В этом случае, если recommendations не были перегенерированы в currentLanguage.onEach (из-за отсутствия weather data),
                // они могут остаться на старом языке. Однако, если weather data нет, то и рекомендаций быть не должно.
                // Если же они есть, то currentLanguage.onEach должен был их обновить.
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                try {
                    val localeListToSet = if (newLangCode.isEmpty()) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(newLangCode)
                    AppCompatDelegate.setApplicationLocales(localeListToSet)
                    Log.d(TAG, "AppCompatDelegate.setApplicationLocales called for API 33+ with '$newLangCode'")
                } catch (e: Exception) {
                    Log.e(TAG, "Ошибка установки локали приложения на API 33+: ${e.message}", e)
                }
            }
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
            val langForCitySearch = explicitApiLang ?: getApiLangCode()
            Log.d(TAG, "Внутренний поиск: Поиск города: '$cityName'. Язык API для поиска: '$langForCitySearch'")

            try {
                val cities = withContext(Dispatchers.IO) {
                    weatherApi.searchCities(query = cityName, apiKey = apiKey, limit = 1, lang = langForCitySearch)
                }
                cities.firstOrNull()?.let { foundCity ->
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
                    fetchCurrentWeatherAndForecast(foundCity, explicitApiLang) // Это вызовет generateRecommendations в конце
                    if (isInitialOrFallback) {
                        userPreferencesManager.saveLastSelectedCity(foundCity)
                    }
                } ?: run {
                    Log.w(TAG, "Внутренний поиск: Город '$cityName' не найден через API.")
                    _uiState.update {
                        it.copy(
                            cityForDisplay = if (it.cityForDisplay.isEmpty()) application.applicationContext.getString(R.string.city_not_found) else it.cityForDisplay,
                            weatherErrorMessage = application.applicationContext.getString(R.string.city_not_found),
                            isLoadingWeather = false, isLoadingForecast = false, isInitialLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Внутренний поиск: Ошибка поиска/загрузки города '$cityName': ${e.message}", e)
                _uiState.update {
                    it.copy(
                        weatherErrorMessage = application.applicationContext.getString(R.string.error_loading_cities),
                        isLoadingWeather = false, isLoadingForecast = false, isInitialLoading = false
                    )
                }
            }
        }
    }
}
