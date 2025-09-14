package ru.devsoland.drydrive

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.devsoland.drydrive.data.Weather
import ru.devsoland.drydrive.data.WeatherApi
import ru.devsoland.drydrive.data.City
import ru.devsoland.drydrive.ui.theme.DryDriveTheme

data class NavigationItem(val title: String)

fun formatCityName(city: City): String {
    return "${city.name}, ${city.country}" + if (city.state != null) ", ${city.state}" else ""
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DryDriveTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DryDriveScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CitySearchDropDown(
    weatherApi: WeatherApi,
    apiKey: String,
    initialCityName: String,
    onCitySelected: (City, String) -> Unit,
    modifier: Modifier = Modifier,
    onError: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf(initialCityName) }
    var cities by remember { mutableStateOf<List<City>>(emptyList()) }
    var expanded by remember { mutableStateOf(false) }
    var searchJob by remember { mutableStateOf<Job?>(null) }
    val coroutineScope = rememberCoroutineScope()

    Box(modifier = modifier) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { query ->
                searchQuery = query
                expanded = query.isNotEmpty()
                searchJob?.cancel()
                Log.d("CitySearch", "Query changed to: '$query'. Previous job cancelled if active.")

                if (query.length > 2) {
                    searchJob = coroutineScope.launch {
                        Log.d("CitySearch", "Starting search coroutine for: '$query'")
                        delay(500)
                        try {
                            Log.d("CitySearch", "Executing API call for: '$query'")
                            val results = withContext(Dispatchers.IO) {
                                weatherApi.searchCities(query = query, apiKey = apiKey)
                            }
                            Log.d("CitySearch", "Received ${results.size} cities for: '$query'. Results: $results")
                            cities = results
                            if (results.isEmpty() && query.isNotBlank()) {
                                onError("Города с таким названием не найдены: '$query'")
                            } else if (results.isNotEmpty()) {
                                onError("") // Очищаем ошибку если города найдены
                            }
                        } catch (e: Exception) {
                            if (e is kotlinx.coroutines.CancellationException) {
                                Log.w("CitySearch", "Search for '$query' was cancelled.")
                            } else {
                                Log.e("CitySearch", "Error searching cities for '$query': ${e.message}", e)
                                onError("Ошибка при поиске городов: ${e.message}")
                                cities = emptyList()
                            }
                        }
                    }
                } else {
                    cities = emptyList()
                    if (query.isEmpty()) {
                        expanded = false
                        onError("")
                    }
                    Log.d("CitySearch", "Query '$query' is too short or empty, clearing/hiding cities list.")
                }
            },
            label = { Text("Введите город") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        DropdownMenu(
            expanded = expanded && cities.isNotEmpty(),
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth()
        ) {
            cities.forEach { city ->
                DropdownMenuItem(
                    text = { Text(formatCityName(city)) },
                    onClick = {
                        val formattedName = formatCityName(city)
                        searchQuery = formattedName
                        onCitySelected(city, formattedName)
                        expanded = false
                        cities = emptyList()
                        Log.d("CitySearch", "City selected: ${formatCityName(city)}")
                    }
                )
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DryDriveScreen(modifier: Modifier = Modifier) {
    var weather by remember { mutableStateOf<Weather?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val apiKey = BuildConfig.WEATHER_API_KEY
    val scope = rememberCoroutineScope()
    var cityForDisplay by remember { mutableStateOf("Moscow") }
    var selectedCityObject by remember { mutableStateOf<City?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val bottomMenuItems = listOf(
        NavigationItem("Главная"),
        NavigationItem("Карта"),
        NavigationItem("Настройки")
    )

    val selectedItem = remember { mutableStateOf(bottomMenuItems.first().title) }

    val carImage = when (weather?.weather?.getOrNull(0)?.main) {
        "Rain", "Snow", "Thunderstorm" -> painterResource(id = R.drawable.car_dirty)
        else -> painterResource(id = R.drawable.car_clean)
    }

    LaunchedEffect(key1 = selectedCityObject) {
        selectedCityObject?.let { cityObj ->
            // Запрос погоды теперь здесь, когда selectedCityObject изменяется (т.е. после выбора города)
            scope.launch {
                errorMessage = null
                weather = null // Очищаем старую погоду перед новым запросом
                Log.d("DryDriveScreen", "LAUNCHED_EFFECT: Fetching weather for: ${cityObj.name}")
                try {
                    val weatherApi = WeatherApi.create()
                    val result = withContext(Dispatchers.IO) {
                        weatherApi.getWeather(city = cityObj.name, apiKey = apiKey)
                    }
                    weather = result
                    Log.d("DryDriveScreen", "LAUNCHED_EFFECT: Weather fetched for ${cityObj.name}: $result")
                } catch (e: Exception) {
                    val newErrorMessage = if (e.message?.contains("404") == true) {
                        "Город не найден: ${cityObj.name}"
                    } else {
                        "Ошибка при загрузке погоды: ${e.message}"
                    }
                    errorMessage = newErrorMessage
                    Log.e("DryDriveScreen", "LAUNCHED_EFFECT: Error fetching weather for ${cityObj.name}: ${e.message}", e)
                }
            }
        }
    }


    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text(
                    text = "Меню",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(16.dp)
                )
                LazyColumn {
                    items(listOf("Section 1", "Section 2", "Section 3")) { item ->
                        NavigationDrawerItem(
                            label = { Text(item) },
                            selected = false,
                            onClick = {}
                        )
                    }
                }
            }
        }
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = { Text("DryDrive") },
                    navigationIcon = {
                        IconButton(
                            onClick = { scope.launch { drawerState.open() } }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Menu"
                            )
                        }
                    }
                )
            },
            bottomBar = {
                NavigationBar {
                    bottomMenuItems.forEach { screen ->
                        NavigationBarItem(
                            selected = screen.title == selectedItem.value,
                            onClick = { selectedItem.value = screen.title },
                            label = { Text(screen.title) },
                            icon = {
                                when (screen.title) {
                                    "Главная" -> Icon(Icons.Default.Home, contentDescription = null)
                                    "Карта" -> Icon(Icons.Default.Place, contentDescription = null)
                                    "Настройки" -> Icon(Icons.Default.Settings, contentDescription = null)
                                    else -> {}
                                }
                            }
                        )
                    }
                }
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = {
                        scope.launch {
                            snackbarHostState.showSnackbar("Кнопка нажата")
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add",
                        tint = Color.White
                    )
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.city_background),
                    contentDescription = "City Background",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CitySearchDropDown(
                        weatherApi = WeatherApi.create(),
                        apiKey = apiKey,
                        initialCityName = cityForDisplay,
                        onCitySelected = { city, formattedName ->
                            selectedCityObject = city // Это вызовет LaunchedEffect для загрузки погоды
                            cityForDisplay = formattedName
                            // errorMessage = null // Убрано отсюда, так как LaunchedEffect сбросит
                            Log.d("DryDriveScreen", "City selected: $formattedName, object: $city. Weather will be fetched by LaunchedEffect.")
                        },
                        modifier = Modifier.fillMaxWidth(0.8f),
                        onError = { errorMsg ->
                            if (errorMsg.isNotBlank()) {
                                errorMessage = errorMsg
                            } else if (errorMessage?.startsWith("Ошибка при поиске городов") == true || errorMessage?.startsWith("Города с таким названием не найдены") == true) {
                                errorMessage = null
                            }
                            Log.d("DryDriveScreen", "onError from CitySearchDropDown: '$errorMsg'")
                        }
                    )

                    Button(
                        onClick = {
                            // selectedCityObject здесь точно не null, так как кнопка неактивна, если он null
                            selectedCityObject?.let { city ->
                                scope.launch {
                                    errorMessage = null // Сбрасываем ошибку перед новым запросом
                                    weather = null // Опционально: сбрасываем старую погоду для индикации загрузки
                                    Log.d("DryDriveScreen", "BUTTON_CLICK: Fetching weather for: ${city.name}")
                                    try {
                                        val weatherApi = WeatherApi.create()
                                        val result = withContext(Dispatchers.IO) {
                                            weatherApi.getWeather(city = city.name, apiKey = apiKey)
                                        }
                                        weather = result
                                        Log.d("DryDriveScreen", "BUTTON_CLICK: Weather fetched: $result")
                                    } catch (e: Exception) {
                                        val newErrorMessage = if (e.message?.contains("404") == true) {
                                            "Город не найден: ${city.name}"
                                        } else {
                                            "Ошибка при загрузке погоды: ${e.message}"
                                        }
                                        errorMessage = newErrorMessage
                                        Log.e("DryDriveScreen", "BUTTON_CLICK: Error fetching weather for ${city.name}: ${e.message}", e)
                                    }
                                }
                            }
                            // Если selectedCityObject будет null, кнопка будет неактивна,
                            // поэтому блок else здесь не нужен.
                        },
                        modifier = Modifier.padding(top = 16.dp),
                        enabled = selectedCityObject != null // Кнопка активна ТОЛЬКО если город выбран
                    ) {
                        // Текст кнопки теперь более точно отражает её действие
                        Text(if (weather != null && selectedCityObject?.name == weather?.name) "Обновить погоду" else "Узнать погоду")
                    }

                    Image(
                        painter = carImage,
                        contentDescription = "Car Image",
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .padding(vertical = 16.dp),
                        contentScale = ContentScale.Fit
                    )

                    AnimatedVisibility(
                        visible = weather != null && (errorMessage == null || errorMessage!!.isBlank()),
                        enter = fadeIn(tween(500)),
                        exit = fadeOut()
                    ) {
                        weather?.let { w ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth(0.9f)
                                    .clip(RoundedCornerShape(16.dp))
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    val recommendation = when (w.weather.getOrNull(0)?.main) {
                                        "Rain" -> "Мыть не стоит: дождь"
                                        "Snow" -> "Мыть не стоит: снег"
                                        "Mist", "Fog" -> "Мыть не стоит: туман"
                                        "Thunderstorm" -> "Мыть не стоит: гроза"
                                        else -> "Можно мыть машину"
                                    }
                                    Text(
                                        text = "${w.main.temp.toInt()}°C",
                                        style = MaterialTheme.typography.headlineMedium
                                    )
                                    Text(
                                        text = w.weather.getOrNull(0)?.description ?: "Нет данных",
                                        style = MaterialTheme.typography.titleMedium,
                                        modifier = Modifier.padding(top = 8.dp)
                                    )
                                    Text(
                                        text = recommendation,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (!errorMessage.isNullOrBlank()) {
                        Text(
                            text = errorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 16.dp)
                        )
                    }

                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(getSampleWeatherForecast()) { dayWeather ->
                            Card(
                                modifier = Modifier
                                    .size(100.dp, 120.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            ) {
                                Column(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(dayWeather.day)
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_cloud),
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Text("${dayWeather.temp.toInt()}°C")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

data class DayWeather(val day: String, val temp: Double)

fun getSampleWeatherForecast(): List<DayWeather> = listOf(
    DayWeather("Mon", 15.0),
    DayWeather("Tue", 14.0),
    DayWeather("Wed", 16.0),
    DayWeather("Thu", 13.0),
    DayWeather("Fri", 17.0),
    DayWeather("Sat", 12.0),
    DayWeather("Sun", 18.0)
)

@Preview(showBackground = true)
@Composable
fun DryDriveScreenPreview() {
    DryDriveTheme {
        DryDriveScreen()
    }
}
