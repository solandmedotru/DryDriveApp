package ru.devsoland.drydrive.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonIgnoreUnknownKeys // Убедитесь, что этот импорт есть, если используется
import kotlinx.serialization.SerialName

// --- Существующие Data классы (оставляем или адаптируем при необходимости) ---

@Serializable
data class Weather( // Это для /data/2.5/weather
    val name: String,
    val main: Main, // Используем Main, а не MainData, если они разные
    val weather: List<WeatherInfo>, // Используем WeatherInfo
    // Добавьте сюда поля coord, если они нужны и приходят в этом запросе
    val coord: CoordData? = null // Опционально, если приходит
)

@Serializable
data class Main( // Для /data/2.5/weather
    val temp: Double
    // ... другие поля из вашего Main ...
)

@Serializable
@JsonIgnoreUnknownKeys // Игнорировать неизвестные ключи в WeatherInfo
data class WeatherInfo( // Для /data/2.5/weather
    val description: String,
    val main: String, // Основной тип погоды (Rain, Snow, Clouds и т.д.)
    val icon: String? = null // Добавим поле icon, оно есть в ответе API
)

@Serializable
data class City( // Это ваш класс для поиска городов /geo/1.0/direct
    val name: String,
    val lat: Double,
    val lon: Double,
    val country: String,
    val state: String? = null
)

// --- НОВЫЕ или АДАПТИРОВАННЫЕ Data классы для /data/2.5/forecast ---

@Serializable
data class ForecastResponse( // Главный класс для ответа на запрос прогноза
    val cod: String,
    val message: Int,
    val cnt: Int,
    val list: List<ForecastListItem>,
    val city: CityDataForForecast // Используем специфичное имя, чтобы не путать с City для поиска
)

@Serializable
data class ForecastListItem(
    val dt: Long, // Время расчета данных, unix, UTC
    val main: MainData, // Используем MainData, так как структура в forecast list может отличаться
    val weather: List<WeatherData>, // Используем WeatherData
    val clouds: CloudsData,
    val wind: WindData,
    val visibility: Int,
    val pop: Double, // Вероятность осадков
    @SerialName("dt_txt") val dtTxt: String // Текстовое представление времени
    // Можно добавить sys { pod: "d" or "n" } если нужно для определения дня/ночи
)

@Serializable
data class MainData( // Для элементов списка в прогнозе
    val temp: Double,
    @SerialName("feels_like") val feelsLike: Double,
    @SerialName("temp_min") val tempMin: Double,
    @SerialName("temp_max") val tempMax: Double,
    val pressure: Int,
    @SerialName("sea_level") val seaLevel: Int? = null, // Могут отсутствовать
    @SerialName("grnd_level") val grndLevel: Int? = null, // Могут отсутствовать
    val humidity: Int,
    @SerialName("temp_kf") val tempKf: Double? = null // Может отсутствовать
)

@Serializable
@JsonIgnoreUnknownKeys // Важно для вложенных объектов, где могут быть лишние поля
data class WeatherData( // Для элементов списка в прогнозе
    val id: Int,
    val main: String, // "Rain", "Clouds"
    val description: String,
    val icon: String // Код иконки, например "01d"
)

@Serializable
data class CloudsData(
    val all: Int // Облачность в %
)

@Serializable
data class WindData(
    val speed: Double,
    val deg: Int,
    val gust: Double? = null // Может отсутствовать
)

@Serializable
data class CityDataForForecast( // Информация о городе в ответе прогноза
    val id: Int,
    val name: String,
    val coord: CoordData, // Используем CoordData
    val country: String,
    val population: Int,
    val timezone: Int, // Сдвиг в секундах от UTC
    val sunrise: Long,
    val sunset: Long
)

@Serializable
data class CoordData( // Общий для разных ответов
    val lat: Double,
    val lon: Double
)

// Убедитесь, что все используемые здесь data классы (Main, WeatherInfo, City, CoordData)
// определены в этом файле или корректно импортированы, если они в других файлах.
// Я включил определения, которые были в вашем контексте [1], и адаптировал их.
// Ваш класс ForecastResponse, CityData, ForecastListItem были почти такими же.
// Основные добавления/изменения:
// - MainData, WeatherData, CloudsData, WindData для ForecastListItem.
// - CityDataForForecast и CoordData.
// - Добавлено поле `icon` в `WeatherInfo` и `WeatherData`, так как оно полезно.
// - Постарался сделать опциональными поля, которые могут отсутствовать в ответе API (с помощью `? = null`).
