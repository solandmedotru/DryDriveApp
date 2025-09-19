package ru.devsoland.drydrive.feature_map.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import com.yandex.mapkit.map.VisibleRegion
import com.yandex.mapkit.search.SearchFactory
import com.yandex.mapkit.search.SearchManagerType
import com.yandex.mapkit.search.SearchOptions
import com.yandex.mapkit.search.Session
import com.yandex.mapkit.search.Response
import com.yandex.mapkit.map.VisibleRegionUtils
import com.yandex.runtime.Error
import com.yandex.runtime.network.NetworkError // Corrected import for NetworkError
import com.yandex.runtime.network.RemoteError // Corrected import for RemoteError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// import ru.devsoland.drydrive.R // R is no longer used in this file

import com.yandex.mapkit.search.BusinessObjectMetadata
// BusinessPhotoObjectMetadata import can be removed if not used by ReferencesObjectMetadata or other retained logic
// import com.yandex.mapkit.search.BusinessPhotoObjectMetadata 
// BusinessRating1xObjectMetadata import can be removed
// import com.yandex.mapkit.search.BusinessRating1xObjectMetadata
// UriObjectMetadata import can be removed
// import com.yandex.mapkit.uri.UriObjectMetadata
import com.yandex.mapkit.search.ReferencesObjectMetadata // Keep if ReferencesObjectMetadata logging is retained

class MapScreenViewModel : ViewModel() {

    private val searchManager =
        SearchFactory.getInstance().createSearchManager(SearchManagerType.COMBINED)
    private var searchSession: Session? = null

    private val _searchState = MutableStateFlow<SearchState>(SearchState.Idle)
    val searchState: StateFlow<SearchState> = _searchState.asStateFlow()

    private val searchSessionListener = object : Session.SearchListener {
        override fun onSearchResponse(response: Response) {
            val yandexItems = response.collection.children
            Log.d("MapScreenViewModel", "Received ${yandexItems.size} items from Yandex Search API.")

            val uiItems = yandexItems.mapNotNull { collectionItem ->
                val geoObject = collectionItem.obj ?: return@mapNotNull null
                
                Log.d("MapScreenViewModel", "--- Processing New GeoObject ---")

                val point = geoObject.geometry.firstOrNull()?.point
                val nameFromGeoObject = geoObject.name ?: "Автомойка" // Default if name is null
                val descriptionFromGeoObject = geoObject.descriptionText ?: "Адрес не указан" // Default if description is null

                Log.d("MapScreenViewModel", "  GeoObject Raw Name: ${geoObject.name ?: "N/A"}")
                Log.d("MapScreenViewModel", "  GeoObject Raw Description: ${geoObject.descriptionText ?: "N/A"}")
                Log.d("MapScreenViewModel", "  GeoObject Point: Lat=${point?.latitude ?: "N/A"}, Lon=${point?.longitude ?: "N/A"}")
                Log.d("MapScreenViewModel", "  GeoObject Arefs: ${geoObject.aref.joinToString()}")

                val metadataContainer = geoObject.metadataContainer
                val businessMetadata = metadataContainer.getItem(BusinessObjectMetadata::class.java)
                // BusinessPhotoObjectMetadata logging block removed

                // Initialize variables with defaults or values from GeoObject itself
                var id: String? = null
                var objectName = nameFromGeoObject
                var address = descriptionFromGeoObject 
                var workingHours: String? = null
                var phones: String? = null
                // Removed: ratingValue, reviewCountValue, category, yandexMapsUri, reviewsSpecificUri, firstLinkHref

                Log.d("MapScreenViewModel", "  Initial ID (before BusinessMetadata): ${id ?: "N/A"}")
                Log.d("MapScreenViewModel", "  Initial Object Name (from GeoObject.name): $objectName")
                Log.d("MapScreenViewModel", "  Initial Address (from GeoObject.descriptionText): $address")

                if (businessMetadata != null) {
                    Log.d("MapScreenViewModel", "  Processing BusinessObjectMetadata...")
                    id = businessMetadata.oid
                    objectName = businessMetadata.name // Override with more specific name from BusinessMetadata
                    businessMetadata.address?.formattedAddress?.let { formattedAddr ->
                        address = formattedAddr // Override with more specific address
                    }
                    workingHours = businessMetadata.workingHours?.text
                    phones = businessMetadata.phones?.joinToString(", ") { it.formattedNumber }
                    // category assignment removed
                    // firstLinkHref assignment removed

                    Log.d("MapScreenViewModel", "    OID (ID): ${id ?: "N/A"}")
                    Log.d("MapScreenViewModel", "    Business Name: ${objectName ?: "N/A"}")
                    Log.d("MapScreenViewModel", "    Formatted Address: $address")
                    Log.d("MapScreenViewModel", "    Working Hours: ${workingHours ?: "N/A"}")
                    Log.d("MapScreenViewModel", "    Phones: ${phones ?: "N/A"}")
                } else {
                    Log.w("MapScreenViewModel", "  BusinessObjectMetadata is NULL for '${nameFromGeoObject}'.")
                }

                // reviewAref block removed
                // UriObjectMetadata block removed
                // yandexMapsUri override logic removed
                
                val referencesMetadata = metadataContainer.getItem(ReferencesObjectMetadata::class.java)
                if (referencesMetadata != null) {
                    Log.d("MapScreenViewModel", "  Processing ReferencesObjectMetadata... (${referencesMetadata.references.size} references)")
                    referencesMetadata.references.forEachIndexed { index, referenceItem ->
                        Log.d("MapScreenViewModel", "    Reference[$index]: ID='${referenceItem.id ?: "N/A"}'") 
                    }
                } else {
                    Log.w("MapScreenViewModel", "  ReferencesObjectMetadata is NULL for '${nameFromGeoObject}'.")
                }

                // BusinessRating1xObjectMetadata block removed

                if (id == null || point == null) {
                    Log.w("MapScreenViewModel", "  Validation FAILED: ID or Point is null. Cannot create UiSearchResultItem.")
                    if (id == null) Log.w("MapScreenViewModel", "    ID is NULL. GeoObject Name: '${nameFromGeoObject}', Arefs: ${geoObject.aref.joinToString()}")
                    if (point == null) Log.w("MapScreenViewModel", "    Point is NULL. GeoObject Name: '${nameFromGeoObject}'")
                    Log.d("MapScreenViewModel", "--- Finished Processing GeoObject (Skipped) ---")
                    return@mapNotNull null
                }

                val finalName = if (objectName.isNotEmpty() && objectName != "Автомойка") objectName else nameFromGeoObject
                Log.d("MapScreenViewModel", "  Final Values for UI Item:")
                Log.d("MapScreenViewModel", "    UI ID: $id")
                Log.d("MapScreenViewModel", "    UI Name: $finalName")
                Log.d("MapScreenViewModel", "    UI Address: $address")
                Log.d("MapScreenViewModel", "    UI Phones: ${phones ?: "N/A"}")
                Log.d("MapScreenViewModel", "    UI Working Hours: ${workingHours ?: "N/A"}")
                
                val details = FakeCarWashDetails(
                    id = id, 
                    name = finalName,
                    address = address,
                    phones = phones,
                    workingHours = workingHours
                )

                Log.d("MapScreenViewModel", "--- Finished Processing GeoObject (Success) ---")
                UiSearchResultItem(
                    id = id, 
                    point = point, 
                    name = finalName,
                    address = details.address,
                    fullDetails = details
                )
            }

            Log.d("MapScreenViewModel", "Search success. Total UI Items mapped: ${uiItems.size}")
            if (uiItems.isNotEmpty()) {
                _searchState.value = SearchState.Success(uiItems)
            } else {
                _searchState.value = SearchState.Success(emptyList())
                Log.d("MapScreenViewModel", "Search successful, but no valid car washes found or mapped in the area.")
            }
        }

        override fun onSearchError(error: Error) {
            Log.e("MapScreenViewModel", "Search error: $error")
            val errorMessage = when (error) {
                is NetworkError -> "Ошибка сети при поиске"
                is RemoteError -> "Ошибка сервера при поиске"
                else -> "Неизвестная ошибка поиска автомоек"
            }
            _searchState.value = SearchState.Error(errorMessage)
        }
    }

    fun searchCarWashes(visibleRegion: VisibleRegion?) {
        if (visibleRegion == null) {
            Log.w("MapScreenViewModel", "Search attempt with null visibleRegion. Skipping.")
            _searchState.value = SearchState.Idle
            return
        }

        Log.d("MapScreenViewModel", "Starting Yandex Search for 'автомойка' in region...")
        _searchState.value = SearchState.Loading

        val searchOptions = SearchOptions().apply {
            resultPageSize = 32
        }

        searchSession?.cancel() 

        searchSession = searchManager.submit(
            "автомойка",
            VisibleRegionUtils.toPolygon(visibleRegion),
            searchOptions,
            searchSessionListener
        )
    }

    override fun onCleared() {
        super.onCleared()
        searchSession?.cancel()
        Log.d("MapScreenViewModel", "ViewModel cleared, search session cancelled.")
    }
}
