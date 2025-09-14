package ru.devsoland.drydrive

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.runtime.LaunchedEffect
import android.util.Log
import androidx.activity.result.launch
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.devsoland.drydrive.data.Weather
import ru.devsoland.drydrive.data.WeatherApi
import ru.devsoland.drydrive.ui.theme.DryDriveTheme
import androidx.compose.runtime.rememberCoroutineScope // Import this
import kotlinx.coroutines.launch // Import this


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
    val scope = rememberCoroutineScope() // Create a CoroutineScope

    // Initial weather fetch
    LaunchedEffect(Unit) {
        try {
            val weatherApi = WeatherApi.create()
            // No .await() needed if getWeather is a suspend function
            val result = withContext(Dispatchers.IO) {
                Log.e("DryDrive", "APIKEY: $apiKey")
                Log.d("DryDrive", "API Key from BuildConfig: ${BuildConfig.WEATHER_API_KEY}")
                weatherApi.getWeather(apiKey = apiKey)
            }
            weather = result
            Log.d("DryDrive", "Weather fetched: $result")
        } catch (e: Exception) {
            errorMessage = "Error: ${e.message}"
            Log.e("DryDrive", "Error fetching weather: ${e.message}", e)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Welcome to DryDrive!",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Check the weather to keep your car clean",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.secondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )
        Button(
            onClick = {
                // Relaunch the request on click using the scope
                scope.launch {
                    try {
                        errorMessage = null
                        val weatherApi = WeatherApi.create()
                        val result = withContext(Dispatchers.IO) {
                            weatherApi.getWeather(apiKey = apiKey) // No .await()
                        }
                        weather = result
                        Log.d("DryDrive", "Weather fetched on click: $result")} catch (e: Exception) {
                        // ...
                    }
                }
            },
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text(text = "Get Weather")
        }
        weather?.let { w ->
            Text(
                text = "Temp: ${w.main.temp}°C, ${w.weather[0].description}",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
        errorMessage?.let { msg ->
            Text(
                text = msg,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}