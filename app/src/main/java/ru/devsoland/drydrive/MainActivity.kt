package ru.devsoland.drydrive

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
// NEW: Удаляем прямой импорт Color, если все цвета будут из темы/Color.kt
// import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp // Оставляем для некоторых специфичных dp, которые могут не быть в dimens
import androidx.compose.ui.unit.sp // Оставляем для TextStyle, если размеры шрифтов не все вынесены
import androidx.lifecycle.viewmodel.compose.viewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import ru.devsoland.drydrive.data.City
import ru.devsoland.drydrive.data.Weather
import ru.devsoland.drydrive.ui.DisplayDayWeather
// NEW: Импорты для ваших кастомных цветов из Color.kt
import ru.devsoland.drydrive.ui.theme.AppBackground
import ru.devsoland.drydrive.ui.theme.AppBarContentColor
import ru.devsoland.drydrive.ui.theme.AppBarContentColorMediumEmphasis
import ru.devsoland.drydrive.ui.theme.TextOnDarkBackground
import ru.devsoland.drydrive.ui.theme.TextOnDarkBackgroundMediumEmphasis
import ru.devsoland.drydrive.ui.theme.SearchFieldBorderFocused
import ru.devsoland.drydrive.ui.theme.SearchFieldBorderUnfocused
import ru.devsoland.drydrive.ui.theme.BottomNavBackground
import ru.devsoland.drydrive.ui.theme.CityBackgroundOverlay
// NEW: Импорт для AppDimens, если используете Kotlin-константы для размеров
// import ru.devsoland.drydrive.ui.theme.AppDimens

import ru.devsoland.drydrive.ui.theme.DryDriveTheme

// Импорты для вынесенных Composable компонентов
import ru.devsoland.drydrive.ui.composables.WeatherDetails
import ru.devsoland.drydrive.ui.composables.DailyForecastRow
import ru.devsoland.drydrive.ui.composables.WeatherRecommendationSection
import ru.devsoland.drydrive.ui.composables.DailyForecastPlaceholder

import java.util.Locale

// --- МОДЕЛИ И УТИЛИТЫ, КОТОРЫЕ ПОКА ОСТАЮТСЯ ЗДЕСЬ ---
// Позже мы их тоже перенесем

enum class RecommendationType { DRINK_WATER, UV_PROTECTION, TIRE_CHANGE, UMBRELLA }

// В файле, где определен RecommendationSlot
data class RecommendationSlot(
    val type: RecommendationType,
    val defaultIcon: ImageVector,
    val activeIcon: ImageVector,
    val defaultTextResId: Int, // << ИЗМЕНЕНИЕ: Int для ID ресурса
    val activeTextResId: Int,   // << ИЗМЕНЕНИЕ: Int для ID ресурса
    val isActive: Boolean = false,
    val defaultContentDescriptionResId: Int, // << ИЗМЕНЕНИЕ
    val activeContentDescriptionResId: Int   // << ИЗМЕНЕНИЕ
)


fun formatCityName(city: City): String {
    return "${city.name}, ${city.country}" + if (city.state != null) ", ${city.state}" else ""
}
// --- КОНЕЦ ВРЕМЕННО ОСТАВЛЕННЫХ МОДЕЛЕЙ ---


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

    val screenBackgroundColor = AppBackground
    var isCitySearchActiveInAppBar by remember { mutableStateOf(false) }

    val carImageResId =
        when (uiState.weather?.weather?.getOrNull(0)?.main?.lowercase(Locale.ROOT)) {
            "rain", "snow", "thunderstorm", "drizzle", "mist", "fog" -> R.drawable.car_dirty // Ресурс остается
            else -> R.drawable.car_clean // Ресурс остается
        }
    // NEW: Используем строки из strings.xml
    val carContentDescription = if (carImageResId == R.drawable.car_dirty) {
        stringResource(R.string.car_dirty_description)
    } else {
        stringResource(R.string.car_clean_description)
    }

    val washRecommendation =
        when (uiState.weather?.weather?.getOrNull(0)?.main?.lowercase(Locale.ROOT)) {
            // NEW: Используем строки из strings.xml
            "rain", "snow", "thunderstorm", "drizzle" -> stringResource(R.string.wash_not_recommended)
            "mist", "fog" -> stringResource(R.string.wash_possible_limited_visibility)
            else -> if (uiState.weather != null && uiState.weather!!.main.temp > 5) {
                stringResource(R.string.wash_great_day)
            } else if (uiState.weather != null) {
                stringResource(R.string.wash_good_weather_but_cool)
            } else ""
        }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.systemBarsPadding()) {
                // NEW: Используем строку из strings.xml
                Text(
                    stringResource(R.string.drawer_menu_todo),
                    modifier = Modifier.padding(dimensionResource(R.dimen.spacing_large))
                )
            }
        }
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            if (isCitySearchActiveInAppBar) {
                                OutlinedTextField(
                                    value = uiState.searchQuery,
                                    onValueChange = { query: String ->
                                        viewModel.onEvent(
                                            DryDriveEvent.SearchQueryChanged(query)
                                        )
                                    },
                                    label = @Composable {
                                        Text(
                                            // NEW: Используем строку и цвет
                                            text = stringResource(R.string.search_city_placeholder),
                                            color = AppBarContentColorMediumEmphasis
                                        )
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(end = dimensionResource(R.dimen.spacing_large)),
                                    singleLine = true,
                                    textStyle = TextStyle(
                                        // NEW: Используем цвет и размер
                                        color = AppBarContentColor,
                                        fontSize = dimensionResource(R.dimen.font_size_medium_emphasis).value.sp
                                    ),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        // NEW: Используем цвета
                                        focusedBorderColor = SearchFieldBorderFocused,
                                        unfocusedBorderColor = SearchFieldBorderUnfocused,
                                        focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent, // Явно указываем Transparent
                                        unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                                        disabledContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                                        errorContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                                        errorCursorColor = MaterialTheme.colorScheme.error
                                    ),
                                    trailingIcon = {
                                        if (uiState.isLoadingCities) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(dimensionResource(R.dimen.icon_size_medium)),
                                                // NEW: Используем цвет
                                                color = AppBarContentColor,
                                                strokeWidth = 2.dp // Можно оставить или вынести в dimens
                                            )
                                        } else if (uiState.searchQuery.isNotEmpty()) {
                                            IconButton(onClick = {
                                                viewModel.onEvent(
                                                    DryDriveEvent.SearchQueryChanged(
                                                        ""
                                                    )
                                                )
                                            }) {
                                                Icon(
                                                    Icons.Filled.Clear,
                                                    // NEW: Используем строку и цвет
                                                    contentDescription = stringResource(R.string.clear_search_description),
                                                    tint = AppBarContentColorMediumEmphasis
                                                )
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
                                        .padding(start = 0.dp) // или dimensionResource(R.dimen.spacing_none) если есть
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.LocationOn,
                                        // NEW: Используем строку и цвет
                                        contentDescription = stringResource(R.string.location_description),
                                        tint = AppBarContentColor,
                                        modifier = Modifier.size(dimensionResource(R.dimen.icon_size_medium))
                                    )
                                    Spacer(modifier = Modifier.width(dimensionResource(R.dimen.spacing_small)))
                                    Text(
                                        // NEW: Используем строку, цвет, размер
                                        text = uiState.cityForDisplay.ifEmpty { stringResource(R.string.select_city_prompt) },
                                        color = AppBarContentColor,
                                        fontSize = dimensionResource(R.dimen.font_size_medium_emphasis).value.sp,
                                        fontWeight = FontWeight.Medium,
                                        textAlign = TextAlign.Start,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.width(dimensionResource(R.dimen.spacing_small)))
                                    Icon(
                                        imageVector = Icons.Filled.ArrowDropDown,
                                        // NEW: Используем строку и цвет
                                        contentDescription = stringResource(R.string.select_city_action),
                                        tint = AppBarContentColor
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = isCitySearchActiveInAppBar && uiState.citySearchResults.isNotEmpty() && uiState.searchQuery.isNotBlank(),
                                onDismissRequest = {
                                    viewModel.onEvent(DryDriveEvent.DismissCitySearchDropDown)
                                },
                                modifier = Modifier
                                    .fillMaxWidth(0.9f) // Можно оставить так или сделать более гибким
                                    .align(Alignment.TopStart)
                                    .offset(y = 56.dp) // TODO: Вынести это магическое число
                            ) {
                                uiState.citySearchResults.forEach { city ->
                                    DropdownMenuItem(
                                        text = @Composable { Text(formatCityName(city)) }, // formatCityName пока не трогаем
                                        onClick = {
                                            val formattedName = formatCityName(city)
                                            viewModel.onEvent(
                                                DryDriveEvent.CitySelectedFromSearch(
                                                    city,
                                                    formattedName
                                                )
                                            )
                                            isCitySearchActiveInAppBar = false
                                        }
                                    )
                                }
                                if (uiState.isLoadingCities && uiState.citySearchResults.isEmpty() && uiState.searchQuery.isNotBlank()) {
                                    DropdownMenuItem(
                                        text = @Composable {
                                            Box(
                                                modifier = Modifier.fillMaxWidth(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(
                                                        dimensionResource(R.dimen.icon_size_standard)
                                                    )
                                                )
                                            }
                                        },
                                        enabled = false,
                                        onClick = {}
                                    )
                                } else if (uiState.citySearchErrorMessage != null && uiState.citySearchResults.isEmpty() && uiState.searchQuery.isNotBlank() && !uiState.isLoadingCities) {
                                    DropdownMenuItem(
                                        text = @Composable {
                                            Text(
                                                uiState.citySearchErrorMessage!!,
                                                color = MaterialTheme.colorScheme.error,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        },
                                        enabled = false,
                                        onClick = { }
                                    )
                                } else if (uiState.citySearchResults.isEmpty() && uiState.searchQuery.isNotBlank() && !uiState.isLoadingCities && uiState.citySearchErrorMessage == null) {
                                    DropdownMenuItem(
                                        text = @Composable {
                                            Text(
                                                stringResource(R.string.city_not_found),
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        },
                                        enabled = false,
                                        onClick = { }
                                    )
                                }
                            }
                        }
                    },
                    actions = {
                        if (isCitySearchActiveInAppBar) {
                            TextButton(onClick = {
                                isCitySearchActiveInAppBar = false
                            }) {
                                Text(
                                    // NEW: Используем строку и цвет
                                    text = stringResource(R.string.action_ok),
                                    color = AppBarContentColor,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        } else {
                            IconButton(onClick = { isCitySearchActiveInAppBar = true }) {
                                Icon(
                                    Icons.Filled.Search,
                                    // NEW: Используем строку и цвет
                                    contentDescription = stringResource(R.string.action_search),
                                    tint = AppBarContentColor
                                )
                            }
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(
                                    Icons.Filled.MoreVert,
                                    // NEW: Используем строку и цвет
                                    contentDescription = stringResource(R.string.action_menu),
                                    tint = AppBarContentColor
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = androidx.compose.ui.graphics.Color.Transparent // Явно указываем Transparent
                    )
                )
            },
            bottomBar = {
                NavigationBar(
                    // NEW: Используем цвет
                    containerColor = BottomNavBackground
                ) {
                    // NEW: Используем строки
                    val navItems = listOf(
                        stringResource(R.string.nav_home),
                        stringResource(R.string.nav_map),
                        stringResource(R.string.nav_settings)
                    )
                    var selectedItemIndex by remember { mutableStateOf(0) }
                    navItems.forEachIndexed { index, itemTitle ->
                        NavigationBarItem(
                            selected = selectedItemIndex == index,
                            onClick = { selectedItemIndex = index },
                            icon = {
                                // NEW: Используем цвета
                                val iconTint =
                                    if (selectedItemIndex == index) AppBarContentColor else AppBarContentColorMediumEmphasis
                                // TODO: Можно сделать маппинг строк на иконки более строгим, если строки изменятся
                                when (itemTitle) {
                                    stringResource(R.string.nav_home) -> Icon(
                                        Icons.Filled.Home,
                                        contentDescription = itemTitle,
                                        tint = iconTint
                                    )

                                    stringResource(R.string.nav_map) -> Icon(
                                        Icons.Filled.Place,
                                        contentDescription = itemTitle,
                                        tint = iconTint
                                    )

                                    stringResource(R.string.nav_settings) -> Icon(
                                        Icons.Filled.Settings,
                                        contentDescription = itemTitle,
                                        tint = iconTint
                                    )
                                }
                            },
                            label = {
                                Text(
                                    itemTitle,
                                    // NEW: Используем цвета
                                    color = if (selectedItemIndex == index) AppBarContentColor else AppBarContentColorMediumEmphasis
                                )
                            },
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
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.city_background),
                            // NEW: Используем строку
                            contentDescription = stringResource(R.string.city_background_description),
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                // NEW: Используем цвет
                                .background(CityBackgroundOverlay)
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = dimensionResource(R.dimen.spacing_xlarge))
                        ) {
                            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_large)))

                            when {
                                uiState.isLoadingWeather && uiState.weather == null -> CircularProgressIndicator(
                                    modifier = Modifier
                                        .align(Alignment.CenterHorizontally)
                                        .padding(vertical = 50.dp)
                                ) // padding можно вынести
                                uiState.weatherErrorMessage != null && uiState.weather == null -> Text(
                                    text = uiState.weatherErrorMessage!!,
                                    color = MaterialTheme.colorScheme.error,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 50.dp)
                                )

                                uiState.weather != null -> {
                                    WeatherDetails(weather = uiState.weather!!) // Вызов вынесенного компонента (в нем тоже будут замены)
                                    Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_medium)))
                                    if (washRecommendation.isNotBlank()) {
                                        Text(
                                            text = washRecommendation, // Уже из ресурсов
                                            // NEW: Используем цвет и размер
                                            color = TextOnDarkBackgroundMediumEmphasis,
                                            fontSize = dimensionResource(R.dimen.font_size_caption).value.sp,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }

                                else -> Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(150.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) { // height можно вынести
                                    Text(
                                        // NEW: Используем строку и цвет
                                        text = stringResource(R.string.select_city_prompt),
                                        color = TextOnDarkBackgroundMediumEmphasis, // Или AppBarContentColorMediumEmphasis
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(top = 50.dp) // padding можно вынести
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            if (uiState.weather != null) {
                                Image(
                                    painter = painterResource(id = carImageResId),
                                    contentDescription = carContentDescription, // Уже из ресурсов
                                    modifier = Modifier
                                        .fillMaxWidth(0.8f)
                                        .aspectRatio(16f / 9f)
                                        .align(Alignment.CenterHorizontally)
                                        .padding(bottom = dimensionResource(R.dimen.spacing_large)),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }
                    } // Конец СЕКЦИИ 1

                    // СЕКЦИЯ 2: Рекомендации
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = dimensionResource(R.dimen.spacing_xlarge))
                            .padding(top = dimensionResource(R.dimen.spacing_large)) // Используем один из стандартных отступов
                    ) {
                        WeatherRecommendationSection(weather = uiState.weather) // В нем тоже будут замены
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = dimensionResource(R.dimen.spacing_xlarge))
                            .padding(
                                top = dimensionResource(R.dimen.spacing_large),
                                bottom = dimensionResource(R.dimen.spacing_large)
                            ) // Используем стандартные отступы
                    ) {
                        when {
                            uiState.isLoadingForecast && uiState.dailyForecasts.isEmpty() -> Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                DailyForecastPlaceholder(); CircularProgressIndicator(
                                color = TextOnDarkBackground,
                                strokeWidth = 2.dp
                            )
                            } // height и strokeWidth можно вынести
                            uiState.forecastErrorMessage != null && uiState.dailyForecasts.isEmpty() -> Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.forecast_unavailable),
                                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                    textAlign = TextAlign.Center
                                )
                            }

                            uiState.dailyForecasts.isNotEmpty() -> DailyForecastRow(forecasts = uiState.dailyForecasts) // В нем тоже будут замены
                            else -> DailyForecastPlaceholder() // В нем тоже будут замены
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
