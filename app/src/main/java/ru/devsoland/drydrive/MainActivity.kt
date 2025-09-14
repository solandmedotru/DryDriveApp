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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.devsoland.drydrive.data.Weather
import ru.devsoland.drydrive.data.WeatherApi
import ru.devsoland.drydrive.ui.theme.DryDriveTheme

data class NavigationItem(val title: String)

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
fun DryDriveScreen(modifier: Modifier = Modifier) {
    var weather by remember { mutableStateOf<Weather?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val apiKey = BuildConfig.WEATHER_API_KEY
    val scope = rememberCoroutineScope()
    var city by remember { mutableStateOf("Moscow") }

    val snackbarHostState = remember { SnackbarHostState() }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    // Элементы нижней навигации
    val bottomMenuItems = listOf(
        NavigationItem("Главная"),
        NavigationItem("Карта"),
        NavigationItem("Настройки")
    )

    val selectedItem = remember { mutableStateOf(bottomMenuItems.first().title) }

    // Фоновая картинка автомобиля
    val carImage = when (weather?.weather?.getOrNull(0)?.main) {
        "Rain", "Snow", "Thunderstorm" -> painterResource(id = R.drawable.car_dirty)
        else -> painterResource(id = R.drawable.car_clean)
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
                // Фоновое изображение города
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
                    // Поле ввода города
                    OutlinedTextField(
                        value = city,
                        onValueChange = { city = it },
                        label = { Text("Введите город") },
                        modifier = Modifier.fillMaxWidth(0.7f)
                    )
                    Button(
                        onClick = {
                            scope.launch {
                                try {
                                    val weatherApi = WeatherApi.create()
                                    val result = withContext(Dispatchers.IO) {
                                        weatherApi.getWeather(city = city.trim(), apiKey = apiKey)
                                    }
                                    weather = result
                                    Log.d("DryDrive", "Weather fetched: $result")
                                } catch (e: Exception) {
                                    errorMessage = if (e.message?.contains("404") == true) {
                                        "Город не найден: проверьте название"
                                    } else {
                                        "Ошибка: ${e.message}"
                                    }
                                    Log.e("DryDrive", "Error fetching weather: ${e.message}", e)
                                }
                            }
                        },
                        modifier = Modifier.padding(top = 16.dp)
                    ) {
                        Text("Узнать погоду")
                    }

                    // Изображение автомобиля
                    Image(
                        painter = carImage,
                        contentDescription = "Car Image",
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .padding(vertical = 16.dp),
                        contentScale = ContentScale.Fit
                    )

                    // Карточка с погодой
                    AnimatedVisibility(
                        visible = weather != null,
                        enter = fadeIn(tween(500)),
                        exit = fadeOut()
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .clip(RoundedCornerShape(16.dp))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                weather?.let { w ->
                                    val recommendation = when (w.weather[0].main) {
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
                                        text = w.weather[0].description,
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

                    // Ошибки
                    errorMessage?.let { msg ->
                        Text(
                            text = msg,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 16.dp)
                        )
                    }

                    // Заглушка прогноза
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
