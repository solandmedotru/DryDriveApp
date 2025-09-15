package ru.devsoland.drydrive

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color // Нужен для Color.Transparent в OutlinedTextField
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import ru.devsoland.drydrive.data.City
// import ru.devsoland.drydrive.data.Weather // Не используется напрямую в этом файле
// import ru.devsoland.drydrive.ui.DisplayDayWeather // Не используется напрямую в этом файле
import ru.devsoland.drydrive.ui.theme.DryDriveTheme // Убедитесь, что Theme.kt настроен для Light/Dark
// Импорты кастомных цветов удаляются, так как будем использовать MaterialTheme.colorScheme
// import ru.devsoland.drydrive.ui.theme.AppBackground
// import ru.devsoland.drydrive.ui.theme.AppBarContentColor
// import ru.devsoland.drydrive.ui.theme.AppBarContentColorMediumEmphasis
// import ru.devsoland.drydrive.ui.theme.TextOnDarkBackground
// import ru.devsoland.drydrive.ui.theme.TextOnDarkBackgroundMediumEmphasis
import ru.devsoland.drydrive.ui.theme.SearchFieldBorderFocused // Оставляем, если это часть дизайна
import ru.devsoland.drydrive.ui.theme.SearchFieldBorderUnfocused // Оставляем, если это часть дизайна
// import ru.devsoland.drydrive.ui.theme.BottomNavBackground
import ru.devsoland.drydrive.ui.theme.CityBackgroundOverlay // Оставляем, если это часть дизайна
import ru.devsoland.drydrive.ui.composables.WeatherDetails
import ru.devsoland.drydrive.ui.composables.DailyForecastRow
import ru.devsoland.drydrive.ui.composables.WeatherRecommendationSection
import ru.devsoland.drydrive.ui.composables.DailyForecastPlaceholder
import ru.devsoland.drydrive.ui.settings.SettingsScreen
import java.util.Locale

// --- МОДЕЛИ И УТИЛИТЫ ---
enum class RecommendationType { DRINK_WATER, UV_PROTECTION, TIRE_CHANGE, UMBRELLA }

data class RecommendationSlot(
    val type: RecommendationType,
    val defaultIcon: ImageVector,
    val activeIcon: ImageVector,
    val defaultTextResId: Int,
    val activeTextResId: Int,
    val isActive: Boolean = false,
    val defaultContentDescriptionResId: Int,
    val activeContentDescriptionResId: Int
)

fun formatCityName(city: City): String {
    return "${city.name}, ${city.country}" + if (city.state != null) ", ${city.state}" else ""
}
// --- КОНЕЦ МОДЕЛЕЙ ---

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false) // Для Edge-to-Edge

        setContent {
            DryDriveTheme { // Ваш Theme.kt должен корректно выбирать Light/DarkColorScheme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background // Основной фон приложения
                ) {
                    DryDriveApp()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DryDriveApp(
    viewModel: DryDriveViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var selectedItemIndex by rememberSaveable { mutableStateOf(0) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.systemBarsPadding(), // Отступы для системных панелей
                drawerContainerColor = MaterialTheme.colorScheme.surface, // Фон самого drawer
                drawerContentColor = MaterialTheme.colorScheme.onSurface // Цвет контента по умолчанию
            ) {
                // Пример текста в Drawer
                Text(
                    stringResource(R.string.drawer_menu_todo),
                    modifier = Modifier.padding(dimensionResource(R.dimen.spacing_large)),
                    color = MaterialTheme.colorScheme.onSurface // Явное указание цвета для контента drawer
                )
                // TODO: Добавьте сюда реальные элементы меню для Drawer
            }
        },
        scrimColor = MaterialTheme.colorScheme.scrim // Затемнение остального экрана при открытом drawer
    ) {
        // Дополнительная Surface для гарантии, что фон под Scaffold - это фон темы.
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Scaffold(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding() // Отступ для навигационной панели
                    .statusBarsPadding(),  // Отступ для статус-бара
                containerColor = MaterialTheme.colorScheme.background, // Фон Scaffold
                topBar = {
                    DryDriveTopAppBar(
                        uiState = uiState,
                        onQueryChange = { query ->
                            viewModel.onEvent(
                                DryDriveEvent.SearchQueryChanged(
                                    query
                                )
                            )
                        },
                        onCitySelected = { city, formattedName ->
                            viewModel.onEvent(
                                DryDriveEvent.CitySelectedFromSearch(
                                    city,
                                    formattedName
                                )
                            )
                        },
                        onDismissSearch = { viewModel.onEvent(DryDriveEvent.DismissCitySearchDropDown) },
                        onMenuClick = { scope.launch { drawerState.open() } }
                    )
                },
                bottomBar = {
                    DryDriveBottomNavigationBar(
                        selectedIndex = selectedItemIndex,
                        onItemSelected = { index -> selectedItemIndex = index }
                    )
                }
            ) { paddingValues ->
                when (selectedItemIndex) {
                    0 -> HomeScreenContent(
                        modifier = Modifier.padding(paddingValues),
                        uiState = uiState
                    )

                    1 -> MapScreenPlaceholder(modifier = Modifier.padding(paddingValues))
                    2 -> SettingsScreen(
                        modifier = Modifier.padding(paddingValues),
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DryDriveTopAppBar(
    uiState: DryDriveUiState,
    onQueryChange: (String) -> Unit,
    onCitySelected: (City, String) -> Unit,
    onDismissSearch: () -> Unit,
    onMenuClick: () -> Unit
) {
    var isCitySearchActiveInAppBar by remember { mutableStateOf(false) }

    CenterAlignedTopAppBar(
        title = {
            Box(modifier = Modifier.fillMaxWidth()) {
                if (isCitySearchActiveInAppBar) {
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = onQueryChange,
                        label = {
                            Text(
                                stringResource(R.string.search_city_placeholder),
                                color = MaterialTheme.colorScheme.onSurfaceVariant // Цвет для placeholder
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = dimensionResource(R.dimen.spacing_large)),
                        singleLine = true,
                        textStyle = TextStyle(
                            color = MaterialTheme.colorScheme.onSurface, // Цвет введенного текста
                            fontSize = dimensionResource(R.dimen.font_size_medium_emphasis).value.sp
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SearchFieldBorderFocused, // Оставляем ваш кастомный цвет или MaterialTheme.colorScheme.primary
                            unfocusedBorderColor = SearchFieldBorderUnfocused, // Оставляем ваш кастомный цвет или MaterialTheme.colorScheme.outline
                            focusedContainerColor = Color.Transparent, // Фон поля ввода должен быть прозрачным
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            errorContainerColor = Color.Transparent,
                            cursorColor = MaterialTheme.colorScheme.primary,
                            errorCursorColor = MaterialTheme.colorScheme.error,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                            errorTextColor = MaterialTheme.colorScheme.error,
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                            errorLabelColor = MaterialTheme.colorScheme.error,
                            focusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledLeadingIconColor = MaterialTheme.colorScheme.onSurface.copy(
                                alpha = 0.38f
                            ),
                            errorLeadingIconColor = MaterialTheme.colorScheme.error,
                            focusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unfocusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledTrailingIconColor = MaterialTheme.colorScheme.onSurface.copy(
                                alpha = 0.38f
                            ),
                            errorTrailingIconColor = MaterialTheme.colorScheme.error
                        ),
                        trailingIcon = {
                            if (uiState.isLoadingCities) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(dimensionResource(R.dimen.icon_size_medium)),
                                    color = MaterialTheme.colorScheme.primary, // или onSurfaceVariant
                                    strokeWidth = 2.dp
                                )
                            } else if (uiState.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { onQueryChange("") }) {
                                    Icon(
                                        Icons.Filled.Clear,
                                        contentDescription = stringResource(R.string.clear_search_description),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
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
                            .padding(start = 0.dp) // padding start = 0.dp был в вашем коде
                    ) {
                        Icon(
                            Icons.Filled.LocationOn,
                            contentDescription = stringResource(R.string.location_description),
                            // tint не нужен, берется из titleContentColor (через TopAppBarDefaults)
                            modifier = Modifier.size(dimensionResource(R.dimen.icon_size_medium))
                        )
                        Spacer(modifier = Modifier.width(dimensionResource(R.dimen.spacing_small)))
                        Text(
                            text = uiState.cityForDisplay.ifEmpty { stringResource(R.string.select_city_prompt) },
                            // color не нужен, берется из titleContentColor (через TopAppBarDefaults)
                            fontSize = dimensionResource(R.dimen.font_size_medium_emphasis).value.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Start,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(dimensionResource(R.dimen.spacing_small)))
                        Icon(
                            Icons.Filled.ArrowDropDown,
                            contentDescription = stringResource(R.string.select_city_action)
                            // tint не нужен, берется из titleContentColor (через TopAppBarDefaults)
                        )
                    }
                }
                // DropdownMenu и его элементы обычно хорошо работают с цветами темы по умолчанию
                DropdownMenu(
                    expanded = isCitySearchActiveInAppBar && uiState.citySearchResults.isNotEmpty() && uiState.searchQuery.isNotBlank(),
                    onDismissRequest = {
                        isCitySearchActiveInAppBar = false
                        onDismissSearch()
                    },
                    modifier = Modifier
                        .fillMaxWidth(0.9f) // Выравнивание и отступ как в вашем коде
                        .align(Alignment.TopStart)
                        .offset(y = 56.dp) // Используйте dp здесь
                ) {
                    uiState.citySearchResults.forEach { city ->
                        DropdownMenuItem(
                            text = { Text(formatCityName(city)) }, // Цвет текста из темы
                            onClick = {
                                val formattedName = formatCityName(city)
                                onCitySelected(city, formattedName)
                                isCitySearchActiveInAppBar = false
                            }
                        )
                    }
                    if (uiState.isLoadingCities && uiState.citySearchResults.isEmpty() && uiState.searchQuery.isNotBlank()) {
                        DropdownMenuItem(
                            text = {
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(dimensionResource(R.dimen.icon_size_standard))
                                        // Цвет индикатора по умолчанию из темы
                                    )
                                }
                            },
                            enabled = false,
                            onClick = {}
                        )
                    } else if (uiState.citySearchErrorMessage != null && uiState.citySearchResults.isEmpty() && uiState.searchQuery.isNotBlank() && !uiState.isLoadingCities) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    uiState.citySearchErrorMessage!!,
                                    color = MaterialTheme.colorScheme.error, // Цвет ошибки из темы
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            },
                            enabled = false,
                            onClick = { }
                        )
                    } else if (uiState.citySearchResults.isEmpty() && uiState.searchQuery.isNotBlank() && !uiState.isLoadingCities && uiState.citySearchErrorMessage == null) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    stringResource(R.string.city_not_found),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                    // Цвет текста по умолчанию из темы
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
                TextButton(onClick = { isCitySearchActiveInAppBar = false }) {
                    Text(
                        stringResource(R.string.action_ok),
                        color = MaterialTheme.colorScheme.primary, // Цвет для текстовой кнопки (акцентный)
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                IconButton(onClick = { isCitySearchActiveInAppBar = true }) {
                    Icon(
                        Icons.Filled.Search,
                        contentDescription = stringResource(R.string.action_search)
                        // tint не нужен, берется из actionIconContentColor (через TopAppBarDefaults)
                    )
                }
                IconButton(onClick = onMenuClick) {
                    Icon(
                        Icons.Filled.MoreVert,
                        contentDescription = stringResource(R.string.action_menu)
                        // tint не нужен, берется из actionIconContentColor (через TopAppBarDefaults)
                    )
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onMenuClick) {
                Icon(
                    imageVector = Icons.Filled.Menu,
                    contentDescription = "Open navigation drawer" // TODO: Локализовать
                    // tint не нужен, берется из navigationIconContentColor (через TopAppBarDefaults)
                )
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface, // Фон TopAppBar
            scrolledContainerColor = MaterialTheme.colorScheme.surface, // Для поведения при прокрутке
            navigationIconContentColor = MaterialTheme.colorScheme.onSurface, // Цвет иконки навигации
            titleContentColor = MaterialTheme.colorScheme.onSurface, // Цвет заголовка
            actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant // Цвет иконок действий (часто чуть менее контрастный)
        )
    )
}

data class BottomNavItem(val title: String, val icon: ImageVector) // Без изменений

@Composable
fun DryDriveBottomNavigationBar(
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit
) {
    val items = listOf(
        BottomNavItem(stringResource(R.string.nav_home), Icons.Filled.Home),
        BottomNavItem(stringResource(R.string.nav_map), Icons.Filled.Place),
        BottomNavItem(stringResource(R.string.nav_settings), Icons.Filled.Settings)
    )

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface, // Фон панели навигации
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant // Цвет для неактивных элементов по умолчанию
    ) {
        items.forEachIndexed { index, item ->
            NavigationBarItem(
                selected = selectedIndex == index,
                onClick = { onItemSelected(index) },
                icon = { Icon(item.icon, contentDescription = item.title) }, // tint не нужен
                label = { Text(item.title) }, // color не нужен
                alwaysShowLabel = true, // или false, по вашему усмотрению
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary, // Цвет выбранной иконки
                    selectedTextColor = MaterialTheme.colorScheme.primary, // Цвет выбранного текста
                    indicatorColor = MaterialTheme.colorScheme.secondaryContainer, // Цвет индикатора под выбранным элементом
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant, // Цвет невыбранной иконки
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant  // Цвет невыбранного текста
                )
            )
        }
    }
}

@Composable
fun HomeScreenContent(
    modifier: Modifier = Modifier,
    uiState: DryDriveUiState
) {
    // val screenBackgroundColor = AppBackground // ЗАМЕНЯЕМ: фон будет от Scaffold/Surface

    val carImageResId =
        when (uiState.weather?.weather?.getOrNull(0)?.main?.lowercase(Locale.ROOT)) {
            "rain", "snow", "thunderstorm", "drizzle", "mist", "fog" -> R.drawable.car_dirty
            else -> R.drawable.car_clean
        }
    val carContentDescription = if (carImageResId == R.drawable.car_dirty) {
        stringResource(R.string.car_dirty_description)
    } else {
        stringResource(R.string.car_clean_description)
    }
    val washRecommendation =
        when (uiState.weather?.weather?.getOrNull(0)?.main?.lowercase(Locale.ROOT)) {
            "rain", "snow", "thunderstorm", "drizzle" -> stringResource(R.string.wash_not_recommended)
            "mist", "fog" -> stringResource(R.string.wash_possible_limited_visibility)
            else -> if (uiState.weather != null && uiState.weather.main.temp > 5) {
                stringResource(R.string.wash_great_day)
            } else if (uiState.weather != null) {
                stringResource(R.string.wash_good_weather_but_cool)
            } else ""
        }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background) // Этот фон уже будет от родительского Scaffold/Surface
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box( // Верхняя часть с фоновым изображением города
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.city_background),
                    contentDescription = stringResource(R.string.city_background_description),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box( // Оверлей для затемнения/тонирования изображения города
                    modifier = Modifier
                        .fillMaxSize()
                        .background(CityBackgroundOverlay) // Оставляем ваш кастомный CityBackgroundOverlay
                    // Если он должен меняться с темой, его тоже нужно сделать динамическим
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
                                .padding(vertical = 50.dp),
                            color = MaterialTheme.colorScheme.primary // ИСПОЛЬЗУЕМ ЦВЕТ ТЕМЫ
                        )

                        uiState.weatherErrorMessage != null && uiState.weather == null -> Text(
                            text = uiState.weatherErrorMessage!!,
                            color = MaterialTheme.colorScheme.error, // ИСПОЛЬЗУЕМ ЦВЕТ ТЕМЫ
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 50.dp)
                        )

                        uiState.weather != null -> {
                            // TODO: Обновите WeatherDetails для использования MaterialTheme.colorScheme
                            WeatherDetails(weather = uiState.weather)
                            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_medium)))
                            if (washRecommendation.isNotBlank()) {
                                Text(
                                    text = washRecommendation,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant, // Заменили TextOnDarkBackgroundMediumEmphasis
                                    fontSize = dimensionResource(R.dimen.font_size_caption).value.sp,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        else -> Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp), // Используйте dp здесь
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = stringResource(R.string.select_city_prompt),
                                color = MaterialTheme.colorScheme.onSurfaceVariant, // Заменили TextOnDarkBackgroundMediumEmphasis
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 50.dp) // Используйте dp здесь
                            )
                        }
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    if (uiState.weather != null) {
                        Image( // Изображение машины
                            painter = painterResource(id = carImageResId),
                            contentDescription = carContentDescription,
                            modifier = Modifier
                                .fillMaxWidth(0.8f)
                                .aspectRatio(16f / 9f)
                                .align(Alignment.CenterHorizontally)
                                .padding(bottom = dimensionResource(R.dimen.spacing_large)),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }
            // Нижняя часть с рекомендациями и прогнозом
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimensionResource(R.dimen.spacing_xlarge))
                    .padding(top = dimensionResource(R.dimen.spacing_large))
            ) {
                // TODO: Обновите WeatherRecommendationSection для использования MaterialTheme.colorScheme
                WeatherRecommendationSection(weather = uiState.weather)
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimensionResource(R.dimen.spacing_xlarge))
                    .padding(
                        top = dimensionResource(R.dimen.spacing_large),
                        bottom = dimensionResource(R.dimen.spacing_large)
                    )
            ) {
                when {
                    uiState.isLoadingForecast && uiState.dailyForecasts.isEmpty() -> Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp), // Используйте dp здесь
                        contentAlignment = Alignment.Center
                    ) {
                        // TODO: Обновите DailyForecastPlaceholder для использования MaterialTheme.colorScheme
                        DailyForecastPlaceholder()
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary, // Заменили TextOnDarkBackground
                            strokeWidth = 2.dp
                        )
                    }

                    uiState.forecastErrorMessage != null && uiState.dailyForecasts.isEmpty() -> Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp), // Используйте dp здесь
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.forecast_unavailable),
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }

                    uiState.dailyForecasts.isNotEmpty() -> {
                        // TODO: Обновите DailyForecastRow для использования MaterialTheme.colorScheme
                        DailyForecastRow(forecasts = uiState.dailyForecasts)
                    }

                    else -> {
                        // TODO: Обновите DailyForecastPlaceholder для использования MaterialTheme.colorScheme
                        DailyForecastPlaceholder()
                    }
                }
            }
        }
    }
}

@Composable
fun MapScreenPlaceholder(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            stringResource(R.string.map_screen_placeholder),
            color = MaterialTheme.colorScheme.onBackground // Цвет текста на фоне
        )
    }
}

@Preview(
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
    name = "Dark Mode"
)
@Preview(
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_NO,
    name = "Light Mode"
)
@Composable
fun DryDriveAppPreview() {
    DryDriveTheme { // Используем параметры по умолчанию из Theme.kt
        Surface(color = MaterialTheme.colorScheme.background) { // Добавляем Surface для корректного фона в Preview
            DryDriveApp()
        }
    }
}
