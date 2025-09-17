package ru.devsoland.drydrive.domain.usecase

import ru.devsoland.drydrive.domain.model.FullForecastDomain
import ru.devsoland.drydrive.domain.model.Result
import ru.devsoland.drydrive.domain.repository.WeatherRepository
import javax.inject.Inject

/**
 * Use Case для получения прогноза погоды.
 */
class GetForecastUseCase @Inject constructor(
    private val weatherRepository: WeatherRepository
) {
    /**
     * Выполняет операцию получения прогноза погоды.
     *
     * @param lat Широта.
     * @param lon Долгота.
     * @param lang Код языка для локализации.
     * @return Result<FullForecastDomain> Результат операции.
     */
    suspend operator fun invoke(lat: Double, lon: Double, lang: String): Result<FullForecastDomain> {
        return weatherRepository.getForecast(lat = lat, lon = lon, lang = lang)
    }
}
