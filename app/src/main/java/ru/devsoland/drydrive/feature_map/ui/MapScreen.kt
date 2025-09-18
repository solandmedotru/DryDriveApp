package ru.devsoland.drydrive.feature_map.ui

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yandex.mapkit.Animation
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.layers.ObjectEvent
import com.yandex.mapkit.map.CameraListener
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.CameraUpdateReason
import com.yandex.mapkit.map.Map
import com.yandex.mapkit.map.MapObjectCollection
import com.yandex.mapkit.map.PlacemarkMapObject
import com.yandex.mapkit.mapview.MapView
import com.yandex.mapkit.user_location.UserLocationLayer
import com.yandex.mapkit.user_location.UserLocationObjectListener
import com.yandex.mapkit.user_location.UserLocationView
import com.yandex.runtime.image.ImageProvider
import ru.devsoland.drydrive.R

private const val TAG = "MapScreen"

@Composable
fun MapScreen(
    modifier: Modifier = Modifier,
    mapViewModel: MapScreenViewModel = viewModel()
) {
    Log.d(TAG, "MapScreen Composable function CALLED")

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val mapView = remember {
        MapView(context).apply {}
    }

    val mapObjects: MapObjectCollection? = remember { mapView.map.mapObjects }
    var searchResultPlacemarks = remember { mutableListOf<PlacemarkMapObject>() }

    var locationPermissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    var initialCameraMoveCompleted by remember { mutableStateOf(false) } // Changed from initialSetupDone
    var userInitiatedMapMove by remember { mutableStateOf(false) }
    var currentUserLocationPoint by remember { mutableStateOf<Point?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (granted) {
                locationPermissionGranted = true
                Toast.makeText(context, "Разрешение на геолокацию получено", Toast.LENGTH_SHORT).show()
            } else {
                locationPermissionGranted = false
                Toast.makeText(context, "Разрешение на геолокацию не предоставлено", Toast.LENGTH_SHORT).show()
            }
        }
    )

    val cameraListener = remember {
        CameraListener { map, _, reason, finished ->
            if (finished) {
                when (reason) {
                    CameraUpdateReason.GESTURES -> {
                        Log.d(TAG, "CameraListener: GESTURES finished. Searching car washes. Setting userInitiatedMapMove = true")
                        mapViewModel.searchCarWashes(map.visibleRegion)
                        userInitiatedMapMove = true
                    }
                    CameraUpdateReason.APPLICATION -> {
                        Log.d(TAG, "CameraListener: APPLICATION finished. Current region: ${map.visibleRegion.topLeft}")
                    }
                    else -> {
                        // Log.d(TAG, "CameraListener: OTHER reason finished ($reason).")
                    }
                }
            }
        }
    }

    val userLocationObjectListener = remember {
        object : UserLocationObjectListener {
            override fun onObjectAdded(userLocationView: UserLocationView) {
                Log.i(TAG, "!!! USER_LOCATION_LISTENER: onObjectAdded - ENTERED !!!")
                val userPoint = userLocationView.arrow.geometry // Or consider pin, accuracyCircle as fallbacks
                Log.d(TAG, "USER_LOCATION_LISTENER: onObjectAdded CALLED! UserPoint: (${userPoint.latitude}, ${userPoint.longitude}), initialCMCompleted: $initialCameraMoveCompleted")
                if (userPoint.latitude != 0.0 || userPoint.longitude != 0.0) {
                    currentUserLocationPoint = userPoint
                    // onObjectUpdated will likely be called immediately after, which will handle camera logic
                }
            }

            override fun onObjectRemoved(view: UserLocationView) {
                Log.i(TAG, "!!! USER_LOCATION_LISTENER: onObjectRemoved - ENTERED !!!")
                Log.d(TAG, "USER_LOCATION_LISTENER: onObjectRemoved CALLED!")
            }

            override fun onObjectUpdated(view: UserLocationView, event: ObjectEvent) {
                Log.i(TAG, "!!! USER_LOCATION_LISTENER: onObjectUpdated - ENTERED !!!")
                val arrowPoint = view.arrow.geometry
                val pinPoint = view.pin.geometry
                val pointToUse: Point? = if (arrowPoint.latitude != 0.0 || arrowPoint.longitude != 0.0) arrowPoint
                                        else if (pinPoint.latitude != 0.0 || pinPoint.longitude != 0.0) pinPoint
                                        else null

                if (pointToUse != null) {
                    currentUserLocationPoint = pointToUse
                }
                
                Log.d(TAG, "USER_LOCATION_LISTENER: onObjectUpdated. Point: $currentUserLocationPoint, initialCMCompleted: $initialCameraMoveCompleted, userMovedMap: $userInitiatedMapMove")

                if (currentUserLocationPoint != null) {
                    val currentNonNullUserPoint = currentUserLocationPoint!!

                    if (!initialCameraMoveCompleted) {
                        Log.i(TAG, "Attempting INITIAL camera move to UserPoint: (${currentNonNullUserPoint.latitude}, ${currentNonNullUserPoint.longitude}) with zoom 15.0f")
                        userInitiatedMapMove = false // This is an automatic initial move

                        mapView.map.move(
                            CameraPosition(currentNonNullUserPoint, 15.0f, 0.0f, 0.0f),
                            Animation(Animation.Type.SMOOTH, 1.0f),
                            object : Map.CameraCallback {
                                override fun onMoveFinished(completed: Boolean) {
                                    if (completed) {
                                        Log.i(TAG, "CameraCallback (Initial): Initial move FINISHED. Searching car washes.")
                                        mapViewModel.searchCarWashes(mapView.map.visibleRegion)
                                        initialCameraMoveCompleted = true
                                    } else {
                                        Log.w(TAG, "CameraCallback (Initial): Initial move NOT completed. Will retry on next update.")
                                        // initialCameraMoveCompleted remains false, so this block will be tried again.
                                    }
                                }
                            }
                        )
                    } else if (initialCameraMoveCompleted && !userInitiatedMapMove) {
                        // Initial move/zoom has completed. Now we are in "follow user" mode.
                        Log.d(TAG, "USER_LOCATION_LISTENER: onObjectUpdated - FOLLOWING user.")
                        val currentZoom = mapView.map.cameraPosition.zoom 
                        mapView.map.move(
                            CameraPosition(currentNonNullUserPoint, currentZoom, 0.0f, 0.0f),
                            Animation(Animation.Type.SMOOTH, 0.5f),
                            null 
                        )
                    }
                    // If userInitiatedMapMove is true, we do nothing here, respecting user's manual pan/zoom.
                } else {
                    Log.w(TAG, "USER_LOCATION_LISTENER: onObjectUpdated - currentUserLocationPoint is NULL.")
                }
            }
        }
    }
    
    DisposableEffect(lifecycleOwner, mapView) {
        val lifecycleObserver = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    MapKitFactory.getInstance().onStart()
                    mapView.onStart()
                }
                Lifecycle.Event.ON_STOP -> {
                    mapView.onStop()
                    MapKitFactory.getInstance().onStop()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)
        mapView.map.addCameraListener(cameraListener)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
            mapView.map.removeCameraListener(cameraListener)
        }
    }

    LaunchedEffect(locationPermissionGranted, mapView, mapViewModel, userLocationObjectListener) {
        Log.d(TAG, "MapScreen LaunchedEffect CALLED. locationPermissionGranted: $locationPermissionGranted")

        if (locationPermissionGranted) {
            var userLocationLayer: UserLocationLayer? = null 
            try {
                Log.d(TAG, "LaunchedEffect: Trying to setup UserLocationLayer.")
                MapKitFactory.getInstance().resetLocationManagerToDefault()
                Log.d(TAG, "LaunchedEffect: resetLocationManagerToDefault() CALLED.")

                userLocationLayer = MapKitFactory.getInstance().createUserLocationLayer(mapView.mapWindow)
                if (userLocationLayer == null) {
                    Log.e(TAG, "LaunchedEffect: userLocationLayer is NULL after creation!")
                    return@LaunchedEffect 
                }
                userLocationLayer.isVisible = true
                Log.d(TAG, "LaunchedEffect: UserLocationLayer properties set.")

                userLocationLayer.setObjectListener(userLocationObjectListener)
                Log.d(TAG, "LaunchedEffect: UserLocationLayer Listener SET.")
            } catch (e: Exception) {
                Log.e(TAG, "Error in UserLocationLayer setup", e)
            }

            mapViewModel.searchState.collect { state ->
                when (state) {
                    is SearchState.Loading -> {
                        Log.d(TAG, "SearchState: Loading...")
                    }
                    is SearchState.Success -> {
                        Log.d(TAG, "SearchState: Success! Found ${state.items.size} car washes.")
                        searchResultPlacemarks.forEach { it.parent?.remove(it) }
                        searchResultPlacemarks.clear()

                        val imageProvider = try {
                            ImageProvider.fromResource(context, R.drawable.ic_car_wash_marker) 
                        } catch (e: Exception) {
                            Log.e(TAG, "Error loading marker icon", e)
                            null
                        }

                        state.items.forEach { searchResultItem ->
                            val placemark = mapObjects?.addPlacemark()?.apply {
                                geometry = searchResultItem.point
                                if (imageProvider != null) {
                                    setIcon(imageProvider)
                                }
                                userData = searchResultItem 
                                addTapListener { mapObject, point ->
                                    val tappedItem = mapObject.userData as? SearchResultItem
                                    Log.d(TAG, "Tapped on car wash: ${tappedItem?.name}")
                                    Toast.makeText(context, "Автомойка: ${tappedItem?.name}", Toast.LENGTH_SHORT).show()
                                    true
                                }
                            }
                            if ( placemark != null) {
                                searchResultPlacemarks.add(placemark)
                            }
                        }
                    }
                    is SearchState.Error -> {
                        Log.e(TAG, "SearchState: Error during car wash search!")
                        Toast.makeText(context, "Ошибка поиска автомоек", Toast.LENGTH_SHORT).show()
                    }
                    SearchState.Off -> {
                        Log.d(TAG, "SearchState: Off")
                    }
                }
            }
        } else {
            Log.d(TAG, "LaunchedEffect: Location permission NOT granted. Requesting permission...")
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                 permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize()
        )
        if (!locationPermissionGranted) {
            Text(
                "Для отображения местоположения требуется разрешение на геолокацию.",
                 Modifier.align(Alignment.Center)
            )
        }
        FloatingActionButton(
            onClick = {
                currentUserLocationPoint?.let { point ->
                    Log.d(TAG, "FAB Clicked: Moving to user location: (${point.latitude}, ${point.longitude})")
                    mapView.map.move(
                        CameraPosition(point, 15.0f, 0.0f, 0.0f), // FAB always zooms to 15
                        Animation(Animation.Type.SMOOTH, 1.0f),
                        object : Map.CameraCallback {
                            override fun onMoveFinished(completed: Boolean) {
                                if (completed) {
                                    Log.d(TAG, "FAB Click: Move to user location FINISHED. Searching car washes and resuming follow.")
                                    userInitiatedMapMove = false // Возобновляем слежение
                                    mapViewModel.searchCarWashes(mapView.map.visibleRegion) // Новый поиск
                                    // initialCameraMoveCompleted should already be true if FAB is used after initial setup,
                                    // but explicitly setting it true again or ensuring it is true might be an option
                                    // if FAB can be clicked before first auto-zoom. However, current logic implies
                                    // currentUserLocationPoint would be null then.
                                } else {
                                     Log.w(TAG, "FAB Click: Move to user location NOT completed.")
                                }
                            }
                        }
                    )
                } ?: Toast.makeText(context, "Местоположение пока не определено", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.MyLocation,
                contentDescription = "Мое местоположение"
            )
        }
    }
}
