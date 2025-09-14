package ru.devsoland.drydrive

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable // <<< ДОБАВИТЬ ИМПОРТ
import androidx.compose.foundation.layout.*
// import androidx.compose.foundation.layout.size // Этот импорт уже есть выше, если используется только для Modifier.size(Dp)
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
// import androidx.compose.runtime.getValue // Уже есть
// import androidx.compose.runtime.setValue // Уже есть
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor // <<< ДОБАВИТЬ ИМПОРТ
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow // <<< ДОБАВИТЬ ИМПОРТ
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import ru.devsoland.drydrive.data.City
import ru.devsoland.drydrive.data.Weather
import ru.devsoland.drydrive.ui.DisplayDayWeather
import ru.devsoland.drydrive.ui.theme.DryDriveTheme
import java.util.Locale
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextField

// Модели из UiModels.kt (или аналогичного файла)
enum class RecommendationType {
    DRINK_WATER, UV_PROTECTION, TIRE_CHANGE, UMBRELLA
}

data class RecommendationSlot(
    val type: RecommendationType,
    val defaultIcon: ImageVector,
    val activeIcon: ImageVector,
    val defaultText: String,
    val activeText: String,
    var isActive: Boolean = false,
    val defaultContentDescription: String,
    val activeContentDescription: String
)

fun formatCityName(city: City): String {
    return "${city.name}, ${city.country}" + if (city.state != null) ", ${city.state}" else ""
}

@AndroidEntryPoint
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

// CitySearchDropDown функция УДАЛЕНА

@Composable
fun WeatherDetails(weather: Weather) {
    val weatherDescription = weather.weather.getOrNull(0)?.description?.replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
    } ?: "Нет данных"

    Column(
        horizontalAlignment = Alignment.Start,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "${weather.main.temp.toInt()}°C",
            style = TextStyle(fontSize = 72.sp, fontWeight = FontWeight.Bold, color = Color.White)
        )
        Text(
            text = weatherDescription,
            style = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Normal, color = Color.White),
            modifier = Modifier.padding(top = 0.dp, bottom = 8.dp)
        )
    }
}

@Composable
fun DailyForecastRow(forecasts: List<DisplayDayWeather>) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 0.dp)
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
    val backgroundColor = if (isCurrentDay) Color(0xFF0095FF) else Color.Black.copy(alpha = 0.2f)
    val contentColor = Color.White

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        modifier = Modifier
            .width(80.dp)
            .height(120.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 16.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = dayWeather.dayShort, style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium, color = contentColor))
            Icon(painter = painterResource(id = dayWeather.iconRes), contentDescription = null, tint = contentColor, modifier = Modifier.size(36.dp))
            Text(text = dayWeather.temperature, style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, color = contentColor))
        }
    }
}

@Composable
fun WeatherRecommendationSection(weather: Weather?) {
    val recommendationSlots = remember {
        listOf(
            RecommendationSlot(RecommendationType.DRINK_WATER, Icons.Filled.WaterDrop, Icons.Filled.WaterDrop, "Пейте воду", "Пейте воду!", false, "Рекомендация: пить воду", "Активная рекомендация: пить воду"),
            RecommendationSlot(RecommendationType.UV_PROTECTION, Icons.Outlined.WbSunny, Icons.Filled.WbSunny, "Высокий УФ", "Защита от УФ!", false, "Рекомендация: УФ защита", "Активная рекомендация: УФ защита"),
            RecommendationSlot(RecommendationType.TIRE_CHANGE, Icons.Filled.AcUnit, Icons.Filled.AcUnit, "Смена колес", "Заморозки!", false, "Рекомендация: смена колес", "Активная рекомендация: заморозки"),
            RecommendationSlot(RecommendationType.UMBRELLA, Icons.Filled.Umbrella, Icons.Filled.Umbrella, "Возьмите зонт", "Нужен зонт!", false, "Рекомендация: взять зонт", "Активная рекомендация: взять зонт")
        )
    }
    val updatedSlots = remember(weather, recommendationSlots) {
        if (weather == null) {
            recommendationSlots.onEach { it.isActive = false }
        } else {
            recommendationSlots.map { slot ->
                val isActive = when (slot.type) {
                    RecommendationType.DRINK_WATER -> weather.main.temp > 28
                    RecommendationType.UV_PROTECTION -> weather.weather.any { it.main.contains("Clear", ignoreCase = true) } && weather.main.temp > 15
                    RecommendationType.TIRE_CHANGE -> weather.main.temp < 5
                    RecommendationType.UMBRELLA -> weather.weather.any { it.main.contains("Rain", ignoreCase = true) }
                }
                slot.copy(isActive = isActive)
            }
        }
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        updatedSlots.forEach { slot -> RecommendationChip(slot) }
    }
}

@Composable
fun RecommendationChip(slot: RecommendationSlot) {
    val accentColor = Color(0xFF0095FF)
    val defaultIconColor = Color.White.copy(alpha = 0.5f)
    val defaultTextColor = Color.White.copy(alpha = 0.6f)
    val iconToShow = if (slot.isActive) slot.activeIcon else slot.defaultIcon
    val textToShow = if (slot.isActive) slot.activeText else slot.defaultText
    val iconColor = if (slot.isActive) accentColor else defaultIconColor
    val textColor = if (slot.isActive) Color.White else defaultTextColor
    val contentDescription = if (slot.isActive) slot.activeContentDescription else slot.defaultContentDescription
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(IntrinsicSize.Min)
    ) {
        Icon(imageVector = iconToShow, contentDescription = contentDescription, tint = iconColor, modifier = Modifier.size(36.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = textToShow, color = textColor, fontSize = 12.sp, textAlign = TextAlign.Center, fontWeight = if (slot.isActive) FontWeight.Medium else FontWeight.Normal)
    }
}

@Composable
fun DailyForecastPlaceholder() {
    val placeholderColor = Color.White.copy(alpha = 0.3f)
    val cardBackgroundColor = Color.Black.copy(alpha = 0.2f)
    val cardShape = RoundedCornerShape(12.dp)
    val cardModifier = Modifier
        .width(80.dp)
        .height(120.dp)
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        repeat(5) {
            Card(shape = cardShape, colors = CardDefaults.cardColors(containerColor = cardBackgroundColor), modifier = cardModifier) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 16.dp, horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(modifier = Modifier
                        .size(width = 40.dp, height = 16.dp)
                        .background(placeholderColor, RoundedCornerShape(4.dp)))
                    Box(modifier = Modifier
                        .size(36.dp)
                        .background(placeholderColor, CircleShape))
                    Box(modifier = Modifier
                        .size(width = 30.dp, height = 20.dp)
                        .background(placeholderColor, RoundedCornerShape(4.dp)))
                }
            }
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
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val snackbarHostState = remember { SnackbarHostState() }

    val screenBackgroundColor = Color(0xFF1A1C20)

    var isCitySearchActiveInAppBar by remember { mutableStateOf(false) } // <<< НОВОЕ СОСТОЯНИЕ

    val carImageResId = when (uiState.weather?.weather?.getOrNull(0)?.main?.lowercase(Locale.ROOT)) {
        "rain", "snow", "thunderstorm", "drizzle", "mist", "fog" -> R.drawable.car_dirty
        else -> R.drawable.car_clean
    }
    val carContentDescription = if (carImageResId == R.drawable.car_dirty) "Грязный автомобиль" else "Чистый автомобиль"

    val washRecommendation = when (uiState.weather?.weather?.getOrNull(0)?.main?.lowercase(Locale.ROOT)) {
        "rain", "snow", "thunderstorm", "drizzle" -> "Мойка не рекомендуется."
        "mist", "fog" -> "Можно помыть, но видимость ограничена."
        else -> if (uiState.weather != null && uiState.weather!!.main.temp > 5) "Отличный день для мойки!" else if (uiState.weather != null) "Погода хорошая, но прохладно для мойки." else ""
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.systemBarsPadding()) {
                Text("Меню (TODO)", modifier = Modifier.padding(16.dp))
            }
        }
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        // --- НАЧАЛО ИЗМЕНЕНИЙ В TITLE ---
                        Box(modifier = Modifier.fillMaxWidth()) {
                            if (isCitySearchActiveInAppBar) {
                                OutlinedTextField(
                                    value = uiState.searchQuery,
                                    onValueChange = { query: String -> viewModel.onEvent(DryDriveEvent.SearchQueryChanged(query)) },
                                    label = @Composable { // Явно указываем, что лямбда является Composable
                                        Text("Поиск города...", color = Color.White.copy(alpha = 0.7f))
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(end = 16.dp), // Небольшой отступ справа от поля ввода
                                    singleLine = true,
                                    textStyle = TextStyle(color = Color.White, fontSize = 18.sp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color.White.copy(alpha = 0.8f),
                                        unfocusedBorderColor = Color.White.copy(alpha = 0.4f),
                                        // --- ИЗМЕНЕНИЯ ЗДЕСЬ ---
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        disabledContainerColor = Color.Transparent, // На случай, если поле может быть disabled
                                        errorContainerColor = Color.Transparent,   // На случай ошибки
                                        // --- КОНЕЦ ИЗМЕНЕНИЙ ---
                                        errorCursorColor = MaterialTheme.colorScheme.error
                                    ),
                                    trailingIcon = @Composable {
                                        if (uiState.isLoadingCities) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(20.dp), // Dp для размера
                                                color = Color.White,
                                                strokeWidth = 2.dp
                                            )
                                        } else if (uiState.searchQuery.isNotEmpty()) {
                                            IconButton(onClick = { viewModel.onEvent(DryDriveEvent.SearchQueryChanged("")) }) {
                                                Icon(Icons.Filled.Clear, contentDescription = "Очистить", tint = Color.White.copy(alpha = 0.7f))
                                            }
                                        }
                                    }
                                )
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { isCitySearchActiveInAppBar = true }
                                        .padding(start = 0.dp) // Прижимаем к левому краю
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.LocationOn,
                                        contentDescription = "Местоположение",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp) // Dp для размера
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = uiState.cityForDisplay.ifEmpty { "Выберите город" }, // Отображаем полное имя или заглушку
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Medium,
                                        textAlign = TextAlign.Start, // Прижимаем текст влево
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis // Обрезка, если не помещается
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Filled.ArrowDropDown, // Иконка выпадающего списка
                                        contentDescription = "Выбрать город",
                                        tint = Color.White
                                    )
                                }
                            }

                            // DropdownMenu для результатов поиска
                            DropdownMenu(
                                expanded = isCitySearchActiveInAppBar && uiState.citySearchResults.isNotEmpty() && uiState.searchQuery.isNotBlank(),
                                onDismissRequest = {
                                    viewModel.onEvent(DryDriveEvent.DismissCitySearchDropDown) // Только скрываем список
                                    // isCitySearchActiveInAppBar = false // Опционально: скрывать ли поле ввода при клике вне меню
                                },
                                modifier = Modifier
                                    .fillMaxWidth(0.9f) // Занимает почти всю ширину TopAppBar
                                    .align(Alignment.TopStart) // Выравниваем по левому краю поля ввода
                                    .offset(y = (56).dp) // Смещаем вниз (примерная высота TopAppBar)
                            ) {
                                uiState.citySearchResults.forEach { city ->
                                    DropdownMenuItem(
                                        text = { Text(formatCityName(city)) },
                                        onClick = {
                                            val formattedName = formatCityName(city)
                                            viewModel.onEvent(DryDriveEvent.CitySelectedFromSearch(city, formattedName))
                                            isCitySearchActiveInAppBar = false // Скрываем поле поиска после выбора
                                        }
                                    )
                                }
                                // Обработка состояний загрузки, ошибки, "не найдено"
                                if (uiState.isLoadingCities && uiState.citySearchResults.isEmpty() && uiState.searchQuery.isNotBlank()) {
                                    DropdownMenuItem(
                                        text = { Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator(modifier = Modifier.size(24.dp)) } },
                                        enabled = false, // Делаем некликабельным
                                        onClick = {}
                                    )
                                } else if (uiState.citySearchErrorMessage != null && uiState.citySearchResults.isEmpty() && uiState.searchQuery.isNotBlank() && !uiState.isLoadingCities) {
                                    DropdownMenuItem(
                                        text = { Text(uiState.citySearchErrorMessage!!, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                                        enabled = false,
                                        onClick = { }
                                    )
                                } else if (uiState.citySearchResults.isEmpty() && uiState.searchQuery.isNotBlank() && !uiState.isLoadingCities && uiState.citySearchErrorMessage == null) {
                                    DropdownMenuItem(
                                        text = { Text("Город не найден", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                                        enabled = false,
                                        onClick = { }
                                    )
                                }
                            }
                        }
                        // --- КОНЕЦ ИЗМЕНЕНИЙ В TITLE ---
                    },
                    actions = {
                        // --- НАЧАЛО ИЗМЕНЕНИЙ В ACTIONS ---
                        if (isCitySearchActiveInAppBar) {
                            TextButton(onClick = {
                                isCitySearchActiveInAppBar = false
                                // Опционально:viewModel.onEvent(DryDriveEvent.SearchQueryChanged("")) // Очистить поиск при нажатии ОК
                            }) {
                                Text("ОК", color = Color.White, fontWeight = FontWeight.Medium)
                            }
                        } else {
                            IconButton(onClick = { isCitySearchActiveInAppBar = true }) { // Иконка поиска теперь активирует режим ввода
                                Icon(imageVector = Icons.Filled.Search, contentDescription = "Поиск", tint = Color.White)
                            }
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(imageVector = Icons.Filled.MoreVert, contentDescription = "Меню", tint = Color.White)
                            }
                        }
                        // --- КОНЕЦ ИЗМЕНЕНИЙ В ACTIONS ---
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = Color.Black.copy(alpha = 0.8f)
                ) {
                    val navItems = listOf("Главная", "Карта", "Настройки")
                    var selectedItemIndex by remember { mutableStateOf(0) }
                    navItems.forEachIndexed { index, itemTitle ->
                        NavigationBarItem(
                            selected = selectedItemIndex == index,
                            onClick = { selectedItemIndex = index },
                            icon = {
                                val iconTint = if (selectedItemIndex == index) Color.White else Color.White.copy(alpha = 0.7f)
                                when (itemTitle) {
                                    "Главная" -> Icon(Icons.Filled.Home, contentDescription = itemTitle, tint = iconTint)
                                    "Карта" -> Icon(Icons.Filled.Place, contentDescription = itemTitle, tint = iconTint)
                                    "Настройки" -> Icon(Icons.Filled.Settings, contentDescription = itemTitle, tint = iconTint)
                                }
                            },
                            label = { Text(itemTitle, color = if (selectedItemIndex == index) Color.White else Color.White.copy(alpha = 0.7f)) },
                            alwaysShowLabel = true
                        )
                    }
                }
            }
        ) { paddingValues ->
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(screenBackgroundColor)
                    .padding(paddingValues)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // СЕКЦИЯ 1: "Погода-Автомобиль"
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.city_background),
                            contentDescription = "Фон города",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.6f))
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 24.dp)
                        ) {
                            // CitySearchDropDown УДАЛЕН ОТСЮДА
                            Spacer(modifier = Modifier.height(16.dp)) // Компенсирующий отступ

                            when {
                                uiState.isLoadingWeather && uiState.weather == null -> CircularProgressIndicator(modifier = Modifier
                                    .align(Alignment.CenterHorizontally)
                                    .padding(vertical = 50.dp))
                                uiState.weatherErrorMessage != null && uiState.weather == null -> Text(text = uiState.weatherErrorMessage!!, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center, modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 50.dp))
                                uiState.weather != null -> {
                                    WeatherDetails(weather = uiState.weather!!)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    if (washRecommendation.isNotBlank()) {
                                        Text(
                                            text = washRecommendation,
                                            color = Color.White.copy(alpha = 0.8f),
                                            fontSize = 14.sp,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                                else -> Column(modifier = Modifier
                                    .fillMaxWidth()
                                    .height(150.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text("Выберите город", color = Color.White.copy(alpha = 0.7f), textAlign = TextAlign.Center, modifier = Modifier.padding(top = 50.dp)) }
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            if (uiState.weather != null) {
                                Image(
                                    painter = painterResource(id = carImageResId),
                                    contentDescription = carContentDescription,
                                    modifier = Modifier
                                        .fillMaxWidth(0.8f)
                                        .aspectRatio(16f / 9f)
                                        .align(Alignment.CenterHorizontally)
                                        .padding(bottom = 16.dp),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }
                    } // Конец СЕКЦИИ 1

                    // СЕКЦИЯ 2: Рекомендации
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .padding(top = 20.dp)
                    ) {
                        WeatherRecommendationSection(weather = uiState.weather)
                    }

                    // СЕКЦИЯ 3: Прогноз на дни
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .padding(top = 16.dp, bottom = 20.dp)
                    ) {
                        when {
                            uiState.isLoadingForecast && uiState.dailyForecasts.isEmpty() -> Box(modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp), contentAlignment = Alignment.Center) { DailyForecastPlaceholder(); CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp) }
                            uiState.forecastErrorMessage != null && uiState.dailyForecasts.isEmpty() -> Box(modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp), contentAlignment = Alignment.Center) { Text(text = "Прогноз недоступен", color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f), textAlign = TextAlign.Center) }
                            uiState.dailyForecasts.isNotEmpty() -> DailyForecastRow(forecasts = uiState.dailyForecasts)
                            else -> DailyForecastPlaceholder()
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun DryDriveScreenPreview() {
    DryDriveTheme {
        DryDriveScreen()
    }
}
