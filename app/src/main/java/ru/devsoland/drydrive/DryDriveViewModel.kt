package ru.devsoland.drydrive // или ru.devsoland.drydrive.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.devsoland.drydrive.data.City
import ru.devsoland.drydrive.data.Weather
import ru.devsoland.drydrive.data.WeatherApi
import javax.inject.Inject

// Состояния, которые будет предоставлять ViewModel
data class WeatherUiState(
    val weather: Weather? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class DryDriveViewModel @Inject constructor(
    private val weatherApi: WeatherApi // Hilt внедрит WeatherApi из AppModule
) : ViewModel() {

    private val _weatherUiState = MutableStateFlow(WeatherUiState())
    val weatherUiState: StateFlow<WeatherUiState> = _weatherUiState.asStateFlow()

    // Пока оставим apiKey здесь, позже можно вынести в более подходящее место (например, репозиторий или use-case)
    // Либо передавать его в каждую функцию, если он может меняться, но для OpenWeatherMap он обычно один.
    private val apiKey = ru.devsoland.drydrive.BuildConfig.WEATHER_API_KEY


    fun fetchWeatherForCity(city: City) {
        viewModelScope.launch {
            _weatherUiState.value = WeatherUiState(isLoading = true) // Начало загрузки
            try {
                val result = withContext(Dispatchers.IO) {
                    weatherApi.getWeather(city = city.name, apiKey = apiKey)
                }
                _weatherUiState.value = WeatherUiState(weather = result) // Успешная загрузка
            } catch (e: Exception) {
                val newErrorMessage = if (e.message?.contains("404") == true) {
                    "Город не найден: ${city.name}"
                } else {
                    "Ошибка при загрузке погоды: ${e.message}"
                }
                _weatherUiState.value = WeatherUiState(errorMessage = newErrorMessage) // Ошибка загрузки
                // Можно добавить логирование ошибки здесь, если нужно
                // Log.e("DryDriveViewModel", "Error fetching weather for ${city.name}: ${e.message}", e)
            }
        }
    }

    // Этот метод пригодится для кнопки "Обновить" или если мы захотим очистить погоду
    fun clearWeather() {
        _weatherUiState.value = WeatherUiState()
    }

    // Метод для очистки ошибки, если это потребуется из UI
    fun clearErrorMessage() {
        if (_weatherUiState.value.errorMessage != null) {
            _weatherUiState.value = _weatherUiState.value.copy(errorMessage = null)
        }
    }
}
