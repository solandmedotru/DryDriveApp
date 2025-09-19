package ru.devsoland.drydrive.feature_map.ui

import android.Manifest
import android.content.Intent // Added
import android.content.pm.PackageManager
import android.net.Uri // Added
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
// import androidx.compose.foundation.Image // Removed as photo is gone
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable // Might be implicitly imported or add if needed
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
// import androidx.compose.foundation.lazy.items // Removed as review items are gone
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
// import androidx.compose.material.icons.filled.Call // Removed
// import androidx.compose.material.icons.filled.LocationOn // Removed
import androidx.compose.material.icons.filled.MyLocation
// import androidx.compose.material.icons.filled.Star // Removed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
// import androidx.compose.ui.draw.clip // Removed as photo is gone
import androidx.compose.ui.graphics.Color
// import androidx.compose.ui.layout.ContentScale // Removed as photo is gone
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
// import androidx.compose.ui.res.painterResource // Removed as photo is gone
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yandex.mapkit.Animation
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.layers.ObjectEvent
import com.yandex.mapkit.map.*
import com.yandex.mapkit.mapview.MapView
import com.yandex.mapkit.user_location.UserLocationLayer
import com.yandex.mapkit.user_location.UserLocationObjectListener
import com.yandex.mapkit.user_location.UserLocationView
import com.yandex.runtime.image.ImageProvider
import kotlinx.coroutines.launch
import ru.devsoland.drydrive.R // Keep for ic_car_wash_marker

private const val TAG = "MapScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    modifier: Modifier = Modifier,
    mapViewModel: MapScreenViewModel = viewModel()
) {
    Log.d(TAG, "MapScreen Composable function CALLED")

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val mapView = remember {
        Log.d(TAG, "MapView being remembered.")
        MapView(context).apply {}
    }
    var mapObjectCollection: MapObjectCollection? by remember { mutableStateOf(null) }
    val placemarkIcon = remember(context) {
        ImageProvider.fromResource(context, R.drawable.ic_car_wash_marker)
    }

    var userLocationLayerState by remember { mutableStateOf<UserLocationLayer?>(null) }
    var locationPermissionGranted by remember {
        val initialState = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        Log.d(TAG, "Initial locationPermissionGranted state: $initialState")
        mutableStateOf(initialState)
    }
    var initialCameraMoveCompleted by remember { mutableStateOf(false) }
    var userInitiatedMapMove by remember { mutableStateOf(false) }
    var currentUserLocationPoint by remember { mutableStateOf<Point?>(null) }

    var selectedCarWashDetails by remember { mutableStateOf<FakeCarWashDetails?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    val searchState by mapViewModel.searchState.collectAsStateWithLifecycle()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            Log.d(TAG, "PermissionLauncher onResult: granted = $granted")
            if (granted) {
                locationPermissionGranted = true
                Toast.makeText(context, "Разрешение на геолокацию получено", Toast.LENGTH_SHORT).show()
            } else {
                locationPermissionGranted = false
                Toast.makeText(context, "Разрешение на геолокацию не предоставлено", Toast.LENGTH_SHORT).show()
            }
        }
    )

    val mapObjectTapListener = remember {
        MapObjectTapListener { mapObject, point ->
            val carWashId = mapObject.userData as? String
            if (carWashId != null) {
                Log.d(TAG, "Tapped on placemark with ID: $carWashId, Point: $point")
                val currentSearchState = mapViewModel.searchState.value
                if (currentSearchState is SearchState.Success) {
                    val foundItem = currentSearchState.items.find { it.id == carWashId }
                    if (foundItem != null) {
                        selectedCarWashDetails = foundItem.fullDetails
                        scope.launch { sheetState.show() }
                    } else {
                        Log.w(TAG, "Car wash with ID $carWashId not found in current search results.")
                        Toast.makeText(context, "Детали для мойки не найдены", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.w(TAG, "Search state is not Success, cannot show details.")
                    Toast.makeText(context, "Данные поиска не загружены", Toast.LENGTH_SHORT).show()
                }
                return@MapObjectTapListener true
            }
            false
        }
    }

    val cameraListener = remember {
        CameraListener { map, _, cameraUpdateReason, finished ->
            if (finished) {
                userInitiatedMapMove = cameraUpdateReason == CameraUpdateReason.GESTURES
                if (userInitiatedMapMove || !initialCameraMoveCompleted) {
                    Log.d(TAG, "CameraListener: Requesting car washes for region: ${map.visibleRegion}")
                    mapViewModel.searchCarWashes(map.visibleRegion)
                }
            }
        }
    }

    val userLocationObjectListener = remember {
        object : UserLocationObjectListener {
            override fun onObjectAdded(userLocationView: UserLocationView) {
                val userPoint = userLocationView.arrow.geometry
                if (userPoint.latitude != 0.0 || userPoint.longitude != 0.0) {
                    currentUserLocationPoint = userPoint
                    if (!initialCameraMoveCompleted) {
                        mapView.mapWindow.map.move(
                            CameraPosition(userPoint, 15.0f, 0.0f, 0.0f),
                            Animation(Animation.Type.SMOOTH, 1.0f)
                        ) { completed ->
                            if (completed) initialCameraMoveCompleted = true
                            mapView.mapWindow.map.visibleRegion.let { mapViewModel.searchCarWashes(it) }
                        }
                    }
                } else {
                    Log.w(TAG, "USER_LOCATION_LISTENER: onObjectAdded - Received zero coordinates.")
                }
            }
            override fun onObjectRemoved(view: UserLocationView) {}
            override fun onObjectUpdated(view: UserLocationView, event: ObjectEvent) {
                val arrowPoint = view.arrow.geometry
                val pinPoint = view.pin.geometry
                val pointToUse: Point? = if (arrowPoint.latitude != 0.0 || arrowPoint.longitude != 0.0) arrowPoint
                else if (pinPoint.latitude != 0.0 || pinPoint.longitude != 0.0) pinPoint
                else null
                if (pointToUse != null) {
                    currentUserLocationPoint = pointToUse
                    if (initialCameraMoveCompleted && !userInitiatedMapMove) {
                        val currentZoom = mapView.mapWindow.map.cameraPosition.zoom
                        mapView.mapWindow.map.move(
                            CameraPosition(pointToUse, currentZoom, 0.0f, 0.0f),
                            Animation(Animation.Type.SMOOTH, 0.5f), null
                        )
                    }
                }
            }
        }
    }

    DisposableEffect(lifecycleOwner, mapView) {
        val lifecycleObserver = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> MapKitFactory.getInstance().onStart().also { mapView.onStart() }
                Lifecycle.Event.ON_STOP -> mapView.onStop().also { MapKitFactory.getInstance().onStop() }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)
        mapView.mapWindow.map.addCameraListener(cameraListener)
        val moc = mapView.mapWindow.map.mapObjects.addCollection()
        mapObjectCollection = moc
        moc.addTapListener(mapObjectTapListener)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
            mapView.mapWindow.map.removeCameraListener(cameraListener)
        }
    }

    LaunchedEffect(locationPermissionGranted, mapView, userLocationObjectListener) {
        if (locationPermissionGranted) {
            try {
                MapKitFactory.getInstance().resetLocationManagerToDefault()
                val newLayer = MapKitFactory.getInstance().createUserLocationLayer(mapView.mapWindow)
                newLayer.isVisible = true
                newLayer.isHeadingModeActive = true
                newLayer.setObjectListener(userLocationObjectListener)
                userLocationLayerState = newLayer
            } catch (e: Exception) {
                Log.e(TAG, "Error in UserLocationLayer setup", e)
                userLocationLayerState = null
            }
        } else {
            userLocationLayerState?.isVisible = false
            userLocationLayerState = null
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    LaunchedEffect(searchState, mapObjectCollection, placemarkIcon) {
        val moc = mapObjectCollection ?: return@LaunchedEffect
        when (val state = searchState) {
            is SearchState.Success -> {
                moc.clear()
                if (state.items.isEmpty()) {
                    Log.d(TAG, "Search successful, but no items found to display.")
                } else {
                    state.items.forEach { item ->
                        val placemark = moc.addPlacemark()
                        placemark.geometry = item.point
                        placemark.setIcon(placemarkIcon)
                        placemark.userData = item.id
                    }
                    Log.d(TAG, "Displayed ${state.items.size} car washes on map.")
                }
            }
            is SearchState.Loading -> {
                Log.d(TAG, "Loading car washes...")
            }
            is SearchState.Error -> {
                Log.e(TAG, "Error searching car washes: ${state.message}")
                Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                moc.clear()
            }
            is SearchState.Idle -> {
                moc.clear()
                Log.d(TAG, "Search state is Idle.")
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
                    userInitiatedMapMove = false
                    mapView.mapWindow.map.move(
                        CameraPosition(point, 15.0f, 0.0f, 0.0f),
                        Animation(Animation.Type.SMOOTH, 1.0f),
                        null
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

    if (sheetState.isVisible) {
        ModalBottomSheet(
            onDismissRequest = { scope.launch { sheetState.hide() } },
            sheetState = sheetState,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        ) {
            selectedCarWashDetails?.let {
                CarWashDetailSheetContent(details = it)
            } ?: Box(modifier = Modifier.fillMaxWidth().height(100.dp).padding(16.dp)) {
                Text("Детали не выбраны", modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

@Composable
fun CarWashDetailSheetContent(details: FakeCarWashDetails) {
    val windowInfo = LocalWindowInfo.current
    val density = LocalDensity.current
    val screenHeight = with(density) { windowInfo.containerSize.height.toDp() }
    val context = LocalContext.current // Added to get context for Intent

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .heightIn(max = screenHeight * 0.75f)
    ) {
        item {
            Text(text = details.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = details.address, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            Spacer(modifier = Modifier.height(16.dp))

            details.phones?.let { phoneString ->
                Text("Телефоны:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                val phoneNumbers = phoneString.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                phoneNumbers.forEach { phoneNumber ->
                    Text(
                        text = phoneNumber,
                        style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .clickable {
                                val intent = Intent(Intent.ACTION_DIAL).apply {
                                    data = Uri.parse("tel:$phoneNumber")
                                }
                                try {
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Log.e("MapScreen", "Could not start dialer for $phoneNumber", e)
                                    Toast
                                        .makeText(context, "Не удалось открыть приложение для звонка", Toast.LENGTH_SHORT)
                                        .show()
                                }
                            }
                            .padding(vertical = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            details.workingHours?.let {
                Text("Время работы:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(it, style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(8.dp))
            }
            // Photo, Rating, Call/Route buttons, and Reviews are removed.
        }
    }
}

// ReviewItem Composable removed
// ReviewItemPreview Composable removed

@Preview(showBackground = true)
@Composable
fun CarWashDetailSheetContentPreview() {
    MaterialTheme {
        CarWashDetailSheetContent(details = fakeCarWash1Details) // fakeCarWash1Details now has the simplified structure
    }
}
