package ru.devsoland.drydrive

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.runtime.rememberCoroutineScope
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.devsoland.drydrive.data.Weather
import ru.devsoland.drydrive.data.WeatherApi
import ru.devsoland.drydrive.ui.theme.DryDriveTheme
import androidx.compose.foundation.Image
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ButtonDefaults
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults // Ensure this import is present and correct

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

@Composable
fun DryDriveScreen(modifier: Modifier = Modifier) {
    var weather by remember { mutableStateOf<Weather?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val apiKey = BuildConfig.WEATHER_API_KEY
    val scope = rememberCoroutineScope()
    var city by remember { mutableStateOf("Moscow") }

    // Фоновая картинка автомобиля (нужно добавить варианты в res/drawable)
    val carImage = when (weather?.weather?.getOrNull(0)?.main) {
        "Rain", "Snow", "Thunderstorm" -> painterResource(id = R.drawable.car_dirty) // Грязный автомобиль
        else -> painterResource(id = R.drawable.car_clean) // Чистый автомобиль
    }

    Box(
        modifier = modifier
            .fillMaxSize()
    ) {
        // Фоновое изображение города с автомобилем
        Image(
            painter = painterResource(id = R.drawable.city_background),
            contentDescription = "City Background with Car",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Image(
            painter = carImage,
            contentDescription = "Car Image",
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(top = 16.dp),
            contentScale = ContentScale.Fit
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
        )

        // UI поверх фона
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Заголовок с анимацией
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(animationSpec = tween(800)),
                exit = fadeOut()
            ) {
                Text(
                    text = "Welcome to DryDrive!",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.semantics { contentDescription = "App title" }
                )
            }
            Text(
                text = "Check the weather to keep your car clean",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )
            // Поле ввода города
            OutlinedTextField(
                value = city,
                onValueChange = { city = it },
                label = { Text("Enter city", color = MaterialTheme.colorScheme.onPrimary) },
                modifier = Modifier
                    .padding(top = 16.dp)
                    .fillMaxWidth(0.7f)
                    .semantics { contentDescription = "City input field" },
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onPrimary)
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
                            errorMessage = if (e.message?.contains("404") == true) "Город не найден: проверьте название" else "Error: ${e.message}"
                            Log.e("DryDrive", "Error fetching weather: ${e.message}", e)
                        }
                    }
                },
                modifier = Modifier.padding(top = 16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(text = "Get Weather", color = MaterialTheme.colorScheme.onPrimary)
            }
            // Карточка погоды на сегодня
            AnimatedVisibility(
                visible = weather != null,
                enter = fadeIn(animationSpec = tween(500)),
                exit = fadeOut()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .padding(top = 16.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        weather?.let { w ->
                            val recommendation = when (w.weather[0].main) {
                                "Rain" -> "Don't wash: Rain"
                                "Snow" -> "Don't wash: Snow"
                                "Mist", "Fog" -> "Don't wash: Mist"
                                "Thunderstorm" -> "Don't wash: Thunderstorm"
                                else -> "Can wash car"
                            }
                            // Температура в круге
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${w.main.temp.toInt()}°C",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            // Иконка погоды (нужно добавить в res/drawable)
                            Image(
                                painter = painterResource(id = R.drawable.ic_cloud), // Замените на реальный ресурс
                                contentDescription = "Weather icon",
                                modifier = Modifier
                                    .size(48.dp)
                                    .padding(top = 8.dp)
                            )
                            Text(
                                text = w.weather[0].description,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                            Text(
                                text = recommendation,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
            errorMessage?.let { msg ->
                Text(
                    text = msg,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
            // Горизонтальный список прогноза на 7 дней (заглушка)
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
                            .clip(RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = dayWeather.day,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Image(
                                painter = painterResource(id = R.drawable.ic_cloud), // Замените на реальный ресурс
                                contentDescription = "Day weather icon",
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "${dayWeather.temp.toInt()}°C",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
        // Навигация внизу
        BottomAppBar(
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Главная") },
                    selected = true,
                    onClick = { /* TODO: Navigate to Home */ }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Place, contentDescription = "Map") },
                    label = { Text("Карта") },
                    selected = false,
                    onClick = { /* TODO: Navigate to Map */ }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Настройки") },
                    selected = false,
                    onClick = { /* TODO: Navigate to Settings */ }
                )
            }
        }
    }
}

// Модель для прогноза на 7 дней (заглушка)
data class DayWeather(val day: String, val temp: Double)

fun getSampleWeatherForecast(): List<DayWeather> {
    return listOf(
        DayWeather("Mon", 15.0),
        DayWeather("Tue", 14.0),
        DayWeather("Wed", 16.0),
        DayWeather("Thu", 13.0),
        DayWeather("Fri", 17.0),
        DayWeather("Sat", 12.0),
        DayWeather("Sun", 18.0)
    )
}

@Preview(showBackground = true)
@Composable
fun DryDriveScreenPreview() {
    DryDriveTheme {
        DryDriveScreen()
    }
}