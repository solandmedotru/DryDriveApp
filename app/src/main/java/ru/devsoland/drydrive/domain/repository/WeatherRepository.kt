package ru.devsoland.drydrive.domain.repository

import ru.devsoland.drydrive.domain.model.CityDomain
import ru.devsoland.drydrive.domain.model.FullForecastDomain // Импорт для новой модели прогноза
import ru.devsoland.drydrive.domain.model.WeatherDomain
import ru.devsoland.drydrive.domain.model.Result

/**
 * Интерфейс репозитория для получения погодных данных и информации о городах.
 */
interface WeatherRepository {

    /**
     * Получает текущие погодные условия для указанного города.
     *
     * @param cityName Название города.
     * @param lat Широта города (используется для прогноза, может быть использована для уточнения).
     * @param lon Долгота города (используется для прогноза, может быть использована для уточнения).
     * @param lang Код языка для локализации ответа.
     * @return Result<WeatherDomain> Результат операции: либо успех с данными о погоде, либо ошибка.
     */
    suspend fun getCurrentWeather(cityName: String, lat: Double, lon: Double, lang: String): Result<WeatherDomain>

    /**
     * Выполняет поиск городов по запросу.
     *
     * @param query Строка запроса для поиска города.
     * @param limit Максимальное количество возвращаемых результатов.
     * @return Result<List<CityDomain>> Результат операции: либо успех со списком городов, либо ошибка.
     */
    suspend fun searchCities(query: String, limit: Int): Result<List<CityDomain>>

    /**
     * Получает прогноз погоды для указанных координат.
     *
     * @param lat Широта.
     * @param lon Долгота.
     * @param lang Код языка для локализации ответа.
     * @return Result<FullForecastDomain> Результат операции: либо успех с данными прогноза, либо ошибка.
     */
    suspend fun getForecast(lat: Double, lon: Double, lang: String): Result<FullForecastDomain>

    // TODO:
    // - Сохранения/загрузки настроек пользователя, связанных с погодой (если они станут частью этого репозитория)
}
