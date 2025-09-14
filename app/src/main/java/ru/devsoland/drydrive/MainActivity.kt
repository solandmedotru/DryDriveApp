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
import dagger.hilt.android.AndroidEntryPoint
import androidx.compose.material3.CircularProgressIndicator // Добавьте, если еще нет
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel // Для hiltViewModel()
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
// import androidx.compose.ui.unit.sp // Уже есть выше
import java.util.Locale // Для titlecase
import ru.devsoland.drydrive.ui.DisplayDayWeather // <--- ДОБАВЬТЕ ЭТОТ ИМПОРТ (тот же путь, что и в ViewModel)


data class NavigationItem(val title: String)

fun formatCityName(city: City): String {
    return "${city.name}, ${city.country}" + if (city.state != null) ", ${city.state}" else ""
}
@AndroidEntryPoint // <--- ДОБАВИТЬ ЭТУ АННОТАЦИЮ
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
fun CitySearchDropDown( // Параметры остаются такими же, как на предыдущем шаге
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    cities: List<City>,
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    onCitySelected: (City, String) -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean,
    errorMessage: String?
) {
    Box(modifier = modifier) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onQueryChange,
            label = { Text("Введите город", color = Color.White.copy(alpha = 0.7f)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            trailingIcon = {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            },
            isError = errorMessage != null,
            colors = OutlinedTextFieldDefaults.colors(
                // Стилизация для темного фона
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White.copy(alpha = 0.9f),
                cursorColor = Color.White,
                focusedBorderColor = Color.White,
                unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                focusedLabelColor = Color.White,
                unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                errorBorderColor = MaterialTheme.colorScheme.error,
                errorLabelColor = MaterialTheme.colorScheme.error,
                errorTextColor = MaterialTheme.colorScheme.error,
                disabledTextColor = Color.White.copy(alpha = 0.5f),
                disabledBorderColor = Color.White.copy(alpha = 0.3f),
                disabledLabelColor = Color.White.copy(alpha = 0.5f),
                // Фоны можно сделать прозрачными или полупрозрачными, если нужно
                // focusedContainerColor = Color.Transparent,
                // unfocusedContainerColor = Color.Transparent,
                // disabledContainerColor = Color.Transparent,
                // errorContainerColor = Color.Transparent
            )
        )


        // Отображение сообщения об ошибке поиска под полем ввода
        if (errorMessage != null) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .padding(start = 16.dp, top = 4.dp)
                    .align(Alignment.BottomStart)
            )
        }

        DropdownMenu(
            expanded = expanded && cities.isNotEmpty(),
            onDismissRequest = onDismissRequest,
            modifier = Modifier.fillMaxWidth()
        ) {
            cities.forEach { city ->
                DropdownMenuItem(
                    text = { Text(formatCityName(city)) },
                    onClick = {
                        val formattedName = formatCityName(city)
                        onCitySelected(city, formattedName)
                    }
                )
            }
        }
    }
}

@Composable
fun WeatherDetails(weather: Weather?) {
    weather?.let { w ->
        val weatherDescription = w.weather.getOrNull(0)?.description?.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
        } ?: "Нет данных"

        val recommendation = when (w.weather.getOrNull(0)?.main) {
            "Rain" -> "Сегодня дождь. Лучше остаться дома!"
            "Snow" -> "Идет снег! Время для зимних забав."
            "Mist", "Fog" -> "Туманно. Будьте осторожны на дорогах."
            "Thunderstorm" -> "Гроза! Не забудьте зонт."
            else -> "Отличный день для мойки! Дождя не предвидится."
        }

        Column(
            horizontalAlignment = Alignment.Start,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "${w.main.temp.toInt()}°C",
                style = TextStyle(
                    fontSize = 72.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
            Text(
                text = weatherDescription,
                style = TextStyle(
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color.White
                ),
                modifier = Modifier.padding(top = 0.dp, bottom = 8.dp)
            )
            Text(
                text = recommendation,
                style = TextStyle(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color.White.copy(alpha = 0.8f)
                )
            )
        }
    }
}



@Composable
fun DailyForecastRow(forecasts: List<DisplayDayWeather>) { // Принимает List<DisplayDayWeather>
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 0.dp) // В примере карточки идут от края до края
    ) {
        itemsIndexed(forecasts) { index, dayWeather ->
            ForecastDayCard(
                dayWeather = dayWeather,
                isCurrentDay = dayWeather.dayShort == "Сейчас" || index == 0
            )
        }
    }
}

@Composable
fun ForecastDayCard(dayWeather: DisplayDayWeather, isCurrentDay: Boolean) {
    val backgroundColor = if (isCurrentDay) {
        Color(0xFF0095FF) // Яркий синий для текущего дня
    } else {
        Color.Black.copy(alpha = 0.2f) // Полупрозрачный темный для остальных, как в примере
        // или Color.Transparent, если фон под ними должен быть виден без затемнения
    }
    val contentColor = Color.White

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        modifier = Modifier
            .width(80.dp) // Фиксированная ширина карточки
            .height(120.dp) // Фиксированная высота карточки
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 16.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = dayWeather.dayShort,
                style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium, color = contentColor)
            )
            Icon(
                painter = painterResource(id = dayWeather.iconRes),
                contentDescription = null, // Можно добавить описание на основе погоды
                tint = contentColor,
                modifier = Modifier.size(36.dp)
            )
            Text(
                text = dayWeather.temperature,
                style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, color = contentColor)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DryDriveScreen(
    modifier: Modifier = Modifier,
    viewModel: DryDriveViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val scope = rememberCoroutineScope() // Для Snackbar и Drawer
    val snackbarHostState = remember { SnackbarHostState() }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    // TODO: Настроить TopAppBar и NavigationBar для соответствия стилю
    // Для примера, они пока оставлены как есть или закомментированы, если не нужны

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet { /* ... ваше меню ... */ }
        }
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            // TopAppBar можно убрать, если в дизайне его нет, или стилизовать
            topBar = {
                 TopAppBar(title = { Text("DryDrive", color = Color.White) }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent))
            },
            // Нижняя навигация - ее нужно будет стилизовать под пример
            bottomBar = {
                NavigationBar(containerColor = Color.Black.copy(alpha = 0.8f)) { // Пример стилизации
                    val navItems = listOf("Главная", "Карта", "Настройки") // Добавьте свои иконки
                    navItems.forEach { itemTitle ->
                        NavigationBarItem(
                            selected = itemTitle == "Главная", // Пример, как определять выбранный
                            onClick = { /* TODO: handle navigation */ },
                            icon = {
                                when (itemTitle) {
                                    "Главная" -> Icon(Icons.Filled.Home, contentDescription = "Главная", tint = Color.White)
                                    "Карта" -> Icon(Icons.Filled.Place, contentDescription = "Карта", tint = Color.White.copy(alpha=0.7f))
                                    "Настройки" -> Icon(Icons.Filled.Settings, contentDescription = "Настройки", tint = Color.White.copy(alpha=0.7f))
                                }
                            },
                            label = { Text(itemTitle, color = if (itemTitle == "Главная") Color.White else Color.White.copy(alpha=0.7f)) }
                        )
                    }
                }
            },
            floatingActionButton = { /* Если FAB не нужен, его можно убрать */ }
        ) { paddingValues ->

            Box(
                modifier = modifier
                    .fillMaxSize()
                // Отступы Scaffold применяются к внутреннему Column
            ) {
                // 1. Фоновое изображение города
                Image(
                    painter = painterResource(id = R.drawable.city_background), // Убедитесь, что этот ресурс есть
                    contentDescription = "Фон города",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // 2. Затемняющий слой
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.6f)) // Альфа-канал можно настроить
                )

                // 3. КОНТЕНТ поверх фона и затемнения
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues) // Применяем отступы Scaffold здесь
                        .padding(horizontal = 24.dp) // Общие горизонтальные отступы
                        .padding(top = 16.dp, bottom = 16.dp) // Общие вертикальные отступы
                ) {
                    // Поле поиска города
                    CitySearchDropDown(
                        searchQuery = uiState.searchQuery,
                        onQueryChange = { query -> viewModel.onEvent(DryDriveEvent.SearchQueryChanged(query)) },
                        cities = uiState.citySearchResults,
                        expanded = uiState.isSearchDropDownExpanded,
                        onDismissRequest = { viewModel.onEvent(DryDriveEvent.DismissCitySearchDropDown) },
                        onCitySelected = { city, formattedName ->
                            viewModel.onEvent(DryDriveEvent.CitySelectedFromSearch(city, formattedName))
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp), // Отступ под полем поиска
                        isLoading = uiState.isLoadingCities,
                        errorMessage = uiState.citySearchErrorMessage
                    )

                    // Spacer(modifier = Modifier.height(16.dp)) // Дополнительный отступ, если нужен

                    // Блок с основной информацией о погоде
                    // Он будет выше, если нет Spacer(Modifier.weight(1f)) перед ним
                    if (uiState.isLoadingWeather && uiState.weather == null) { // Показываем загрузку только если погоды еще нет
                        CircularProgressIndicator(
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(vertical = 50.dp)
                        )
                    } else if (uiState.weatherErrorMessage != null && uiState.weather == null) { // Показываем ошибку только если погоды нет
                        Text(
                            text = uiState.weatherErrorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 50.dp)
                        )
                    } else if (uiState.weather != null) {
                        WeatherDetails(weather = uiState.weather)
                    } else {
                        // Начальное состояние или если город еще не выбран
                        // Можно добавить Placeholder или оставить пустым
                        Spacer(modifier = Modifier.height(150.dp)) // Занимаем место, чтобы прогноз не уехал вверх
                    }

                    // Этот Spacer отодвигает DailyForecastRow вниз,
                    // освобождая место для WeatherDetails и кнопки "Узнать погоду" (если она нужна)
                    Spacer(modifier = Modifier.weight(1f))

                    // Кнопка "Узнать погоду" - ее можно убрать, если загрузка происходит автоматически
                    // или если она не вписывается в дизайн примера.
                    // Button(
                    //    onClick = { viewModel.onEvent(DryDriveEvent.RefreshWeatherClicked) },
                    //    modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 16.dp),
                    //    enabled = uiState.selectedCityObject != null && !uiState.isLoadingWeather,
                    //    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0095FF))
                    // ) {
                    //    Text(
                    //        if (uiState.isLoadingWeather && uiState.weather == null) "Загрузка..." // Только если нет данных
                    //        else if (uiState.weather != null) "Обновить погоду"
                    //        else "Узнать погоду",
                    //        color = Color.White
                    //    )
                    // }


                    // Прогноз на несколько дней
                    if (uiState.isLoadingForecast && uiState.dailyForecasts.isEmpty()) {
                        // Можно добавить маленький индикатор для прогноза, если он грузится отдельно
                        // CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally).size(32.dp))
                    } else if (uiState.forecastErrorMessage != null && uiState.dailyForecasts.isEmpty()) {
                        Text(
                            text = "Не удалось загрузить прогноз.", //uiState.forecastErrorMessage!!,
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        )
                    } else if (uiState.dailyForecasts.isNotEmpty()) {
                        DailyForecastRow(forecasts = uiState.dailyForecasts)
                    }

                    // Spacer(modifier = Modifier.height(16.dp)) // Дополнительный отступ внизу, если нужно
                }
            }
        }
    }
}



@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun DryDriveScreenPreview() {
    DryDriveTheme {
        // Для превью можно использовать фейковый ViewModel или передать UiState напрямую,
        // но это потребует больше настроек. Простой запуск DryDriveScreen() покажет только статику.
        DryDriveScreen()
    }
}

