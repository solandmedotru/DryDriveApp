package ru.devsoland.drydrive.feature_map.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import com.yandex.mapkit.GeoObject
import com.yandex.mapkit.geometry.BoundingBox
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.VisibleRegion
import com.yandex.mapkit.search.SearchFactory
import com.yandex.mapkit.search.SearchManagerType
import com.yandex.mapkit.search.SearchOptions
import com.yandex.mapkit.search.Session
import com.yandex.mapkit.search.Response
import com.yandex.mapkit.map.VisibleRegionUtils
import com.yandex.runtime.Error
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// Data class для хранения информации о найденном объекте
data class SearchResultItem(val point: Point, val name: String?, val obj: GeoObject?)

// Sealed interface для представления состояния поиска
sealed interface SearchState {
    object Off : SearchState
    object Loading : SearchState
    data class Success(val items: List<SearchResultItem>, val boundingBox: BoundingBox?) : SearchState
    object Error : SearchState
}

class MapScreenViewModel : ViewModel() {

    private val searchManager = SearchFactory.getInstance().createSearchManager(SearchManagerType.COMBINED)
    private var searchSession: Session? = null

    private val _searchState = MutableStateFlow<SearchState>(SearchState.Off)
    val searchState: StateFlow<SearchState> = _searchState.asStateFlow()

    private val searchSessionListener = object : Session.SearchListener {
        override fun onSearchResponse(response: Response) {
            val items = response.collection.children.mapNotNull {
                val point = it.obj?.geometry?.firstOrNull()?.point ?: return@mapNotNull null
                SearchResultItem(point = point, name = it.obj?.name, obj = it.obj)
            }
            val boundingBox = response.metadata.boundingBox
            Log.d("MapScreenViewModel", "Search success. Items found: ${items.size}")
            _searchState.value = SearchState.Success(items, boundingBox)
        }

        override fun onSearchError(error: Error) {
            Log.e("MapScreenViewModel", "Search error: $error")
            _searchState.value = SearchState.Error
        }
    }

    fun searchCarWashes(visibleRegion: VisibleRegion) {
        Log.d("MapScreenViewModel", "Starting search for car washes in region: ${visibleRegion.topLeft.latitude},${visibleRegion.topLeft.longitude} to ${visibleRegion.bottomRight.latitude},${visibleRegion.bottomRight.longitude}")
        val searchOptions = SearchOptions().apply {
            resultPageSize = 32 // Количество результатов
            // Можно добавить searchTypes, если нужно искать только организации (например, SuggestType.BIZ.value)
        }

        searchSession?.cancel() // Отменяем предыдущую сессию, если есть

        searchSession = searchManager.submit(
            "автомойка", // Текст запроса
            VisibleRegionUtils.toPolygon(visibleRegion), // Геометрия поиска (текущая видимая область)
            searchOptions,
            searchSessionListener
        )
        _searchState.value = SearchState.Loading
    }

    override fun onCleared() {
        super.onCleared()
        searchSession?.cancel() // Убедимся, что сессия отменена при уничтожении ViewModel
        Log.d("MapScreenViewModel", "ViewModel cleared, search session cancelled.")
    }
}
