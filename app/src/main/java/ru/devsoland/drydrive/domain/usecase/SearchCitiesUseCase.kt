package ru.devsoland.drydrive.domain.usecase

import ru.devsoland.drydrive.domain.model.CityDomain
import ru.devsoland.drydrive.domain.model.Result
import ru.devsoland.drydrive.domain.repository.WeatherRepository
import javax.inject.Inject

/**
 * Use Case для поиска городов по строковому запросу.
 */
class SearchCitiesUseCase @Inject constructor(
    private val weatherRepository: WeatherRepository
) {
    /**
     * Выполняет операцию поиска городов.
     *
     * @param query Строка запроса для поиска.
     * @param limit Максимальное количество результатов.
     * @return Result<List<CityDomain>> Результат операции.
     */
    suspend operator fun invoke(query: String, limit: Int = 5): Result<List<CityDomain>> {
        if (query.length < 2) {
            // Пример простой бизнес-логики внутри UseCase
            return Result.Error(IllegalArgumentException("Query too short"), "Search query must be at least 2 characters long")
        }
        return weatherRepository.searchCities(query = query, limit = limit)
    }
}
