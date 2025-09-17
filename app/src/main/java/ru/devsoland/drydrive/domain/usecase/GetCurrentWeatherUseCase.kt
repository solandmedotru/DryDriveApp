package ru.devsoland.drydrive.domain.usecase

import ru.devsoland.drydrive.domain.model.WeatherDomain
import ru.devsoland.drydrive.domain.model.Result
import ru.devsoland.drydrive.domain.repository.WeatherRepository
import javax.inject.Inject

/**
 * Use Case для получения текущих погодных условий.
 */
class GetCurrentWeatherUseCase @Inject constructor(
    private val weatherRepository: WeatherRepository
) {
    /**
     * Выполняет операцию получения текущей погоды.
     *
     * @param cityName Название города.
     * @param lat Широта города (для возможного использования репозиторием).
     * @param lon Долгота города (для возможного использования репозиторием).
     * @param lang Код языка для локализации.
     * @return Result<WeatherDomain> Результат операции.
     */
    suspend operator fun invoke(cityName: String, lat: Double, lon: Double, lang: String): Result<WeatherDomain> {
        // Здесь может быть дополнительная бизнес-логика перед или после вызова репозитория, если необходимо.
        // Например, проверка кэша, валидация параметров и т.д.
        return weatherRepository.getCurrentWeather(cityName = cityName, lat = lat, lon = lon, lang = lang)
    }
}
