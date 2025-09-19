package ru.devsoland.drydrive.feature_map.ui

// import androidx.annotation.DrawableRes // No longer needed if R.drawable.car_wash_placeholder is removed
import com.yandex.mapkit.geometry.Point
// import ru.devsoland.drydrive.R // Keep if other resources are used, or remove if not

// Модель, которую мы используем в UI (для меток на карте и данных для шторки)
data class UiSearchResultItem(
    val id: String,
    val point: Point, // Точка для метки на карте
    val name: String, // Имя для отображения (возможно, на метке или в отладке)
    val address: String, // Адрес для краткой информации или для шторки
    val fullDetails: FakeCarWashDetails // Полные детали для отображения в BottomSheet
)

// Состояние экрана поиска на карте
sealed class SearchState {
    object Idle : SearchState() // Начальное состояние или нет активного поиска
    object Loading : SearchState() // Идет загрузка данных
    data class Success(val items: List<UiSearchResultItem>) : SearchState() // Данные успешно загружены
    data class Error(val message: String) : SearchState() // Произошла ошибка
}

// FakeReview class definition is removed as it's no longer used

data class FakeCarWashDetails(
    val id: String,
    val name: String,
    val address: String,
    val phones: String?, // Added
    val workingHours: String? // Added
    // Removed: photoResId, rating, reviewCount, reviews, yandexMapsUri, reviewsSpecificUri
)

// Объекты с фейковыми данными для примера.
// ViewModel будет использовать их для имитации ответа от сервера/репозитория.
val fakeCarWash1Details = FakeCarWashDetails(
    id = "id1",
    name = "Мойка у Кремля",
    address = "Москва, Красная площадь, 1",
    phones = "8-800-555-35-35",
    workingHours = "Круглосуточно"
)

val fakeCarWash2Details = FakeCarWashDetails(
    id = "id2",
    name = "Чистые Колеса на Лубянке",
    address = "Москва, Лубянская площадь, 2",
    phones = "+7 (495) 123-45-67",
    workingHours = "09:00 - 21:00"
)
