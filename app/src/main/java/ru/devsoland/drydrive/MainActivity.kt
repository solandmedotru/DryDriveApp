package ru.devsoland.drydrive

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue


import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import androidx.core.os.LocaleListCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel // Для Preview
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import ru.devsoland.drydrive.data.City
import ru.devsoland.drydrive.data.preferences.AppLanguage
import ru.devsoland.drydrive.data.preferences.UserPreferencesManager
import ru.devsoland.drydrive.di.UserPreferencesEntryPoint
import ru.devsoland.drydrive.feature_weather.ui.WeatherEvent
import ru.devsoland.drydrive.feature_weather.ui.WeatherUiState
import ru.devsoland.drydrive.feature_weather.ui.WeatherViewModel
import ru.devsoland.drydrive.ui.composables.DailyForecastPlaceholder
import ru.devsoland.drydrive.ui.composables.DailyForecastRow
import ru.devsoland.drydrive.ui.composables.RecommendationsDisplaySection // ИСПРАВЛЕННЫЙ ИМПОРТ
import ru.devsoland.drydrive.ui.composables.WeatherDetails
import ru.devsoland.drydrive.ui.settings.SettingsScreen
import ru.devsoland.drydrive.ui.theme.CityBackgroundOverlay
import ru.devsoland.drydrive.ui.theme.DryDriveTheme
import ru.devsoland.drydrive.ui.theme.SearchFieldBorderFocused
import ru.devsoland.drydrive.ui.theme.SearchFieldBorderUnfocused
import java.util.Locale

// --- УДАЛЕНЫ СТАРЫЕ МОДЕЛИ РЕКОМЕНДАЦИЙ ---
// enum class RecommendationType { DRINK_WATER, UV_PROTECTION, TIRE_CHANGE, UMBRELLA }
// data class RecommendationSlot(...)
// --- КОНЕЦ УДАЛЕНИЯ ---

fun formatCityName(city: City, currentAppLanguageCode: String): String {
    val displayName = when {
        currentAppLanguageCode.isNotBlank() && city.localNames?.containsKey(currentAppLanguageCode) == true -> {
            city.localNames[currentAppLanguageCode]
        }
        city.localNames?.containsKey("en") == true -> {
            city.localNames["en"]
        }
        else -> {
            city.name
        }
    } ?: city.name
    return "$displayName, ${city.country}" + if (city.state != null) ", ${city.state}" else ""
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val weatherViewModel: WeatherViewModel by viewModels()

    override fun attachBaseContext(newBase: Context) {
        Log.d("MainActivityLifecycle", "attachBaseContext CALLED. Initial base context locale: ${newBase.resources.configuration.locale}")

        val userPreferencesManager: UserPreferencesManager
        try {
            val entryPoint = EntryPointAccessors.fromApplication(
                newBase.applicationContext,
                UserPreferencesEntryPoint::class.java
            )
            userPreferencesManager = entryPoint.getUserPreferencesManager()
            Log.d("MainActivityLifecycle", "UserPreferencesManager obtained successfully via EntryPoint.")
        } catch (e: IllegalStateException) {
            Log.e("MainActivityLifecycle", "Hilt not ready in attachBaseContext to get UserPreferencesManager: ${e.message}. Using default base context.", e)
            super.attachBaseContext(newBase)
            return
        } catch (e: Exception) {
            Log.e("MainActivityLifecycle", "Unexpected error getting UserPreferencesManager: ${e.message}. Using default base context.", e)
            super.attachBaseContext(newBase)
            return
        }

        val currentLanguageCode = try {
            Log.d("MainActivityLifecycle", "attachBaseContext: Attempting to get language code via runBlocking...")
            runBlocking {
                userPreferencesManager.selectedLanguageFlow.first().code
            }
        } catch (e: Exception) {
            Log.e("MainActivityLifecycle", "Error getting language code in runBlocking for attachBaseContext: ${e.message}. Defaulting to SYSTEM code.", e)
            AppLanguage.SYSTEM.code
        }

        Log.d("MainActivityLifecycle", "attachBaseContext: Applying language code: '$currentLanguageCode'")
        val contextWithLocale = newBase.setAppLocale(currentLanguageCode)
        super.attachBaseContext(contextWithLocale)
        Log.d("MainActivityLifecycle", "attachBaseContext FINISHED. Applied locale from context: ${contextWithLocale.resources.configuration.locale}")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Log.d("MainActivityLifecycle", "attachBaseContext: AppCompatDelegate locales after set: ${AppCompatDelegate.getApplicationLocales().toLanguageTags()}")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Log.d("MainActivityLifecycle", "onCreate: Current resources locales (from resources.configuration): ${resources.configuration.locales.toLanguageTags()}")
        } else {
            @Suppress("DEPRECATION")
            Log.d("MainActivityLifecycle", "onCreate: Current resources locale (from resources.configuration): ${resources.configuration.locale}")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Log.d("MainActivityLifecycle", "onCreate: Current AppCompatDelegate locales: ${AppCompatDelegate.getApplicationLocales().toLanguageTags()}")
        }

        WindowCompat.setDecorFitsSystemWindows(window, false)

        lifecycleScope.launch {
            weatherViewModel.recreateActivityEvent.collect {
                Log.d("MainActivityLifecycle", "recreateActivityEvent received, calling recreate()")
                this@MainActivity.recreate()
            }
        }

        setContent {
            DryDriveTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DryDriveApp(viewModel = weatherViewModel)
                }
            }
        }
    }
}

fun Context.setAppLocale(languageCode: String): Context {
    Log.d("AppLocaleConfig", "Context.setAppLocale called with languageCode: '$languageCode'")
    val localeToSet: Locale = if (languageCode.isEmpty()) {
        val systemLocale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            resources.configuration.locales[0]
        } else {
            @Suppress("DEPRECATION")
            resources.configuration.locale
        }
        Log.d("AppLocaleConfig", "Using system locale: $systemLocale")
        systemLocale
    } else {
        Log.d("AppLocaleConfig", "Using specific locale for code: $languageCode")
        Locale(languageCode)
    }
    Locale.setDefault(localeToSet)
    val config = Configuration(resources.configuration)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        config.setLocales(android.os.LocaleList(localeToSet))
    } else {
        @Suppress("DEPRECATION")
        config.locale = localeToSet
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val localeListCompat = if (languageCode.isEmpty()) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(languageCode)
        }
        try {
            AppCompatDelegate.setApplicationLocales(localeListCompat)
            Log.d("AppLocaleConfig", "AppCompatDelegate.setApplicationLocales SUCCEEDED for '$languageCode'")
        } catch (e: Exception) {
            Log.e("AppLocaleConfig", "Error in AppCompatDelegate.setApplicationLocales: ${e.message}", e)
        }
    }
    val updatedContext = createConfigurationContext(config)
    Log.d("AppLocaleConfig", "Returning context with new locale: ${updatedContext.resources.configuration.locale}")
    return updatedContext
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DryDriveApp( viewModel: WeatherViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var selectedItemIndex by rememberSaveable { mutableStateOf(0) }

    Log.d("DryDriveApp", "Recomposing DryDriveApp with lang code from uiState: ${uiState.currentLanguageCode}")

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.systemBarsPadding(), // Учитываем системные бары
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                drawerContentColor = MaterialTheme.colorScheme.onSurface
            ) {
                Text(
                    stringResource(R.string.drawer_menu_todo),
                    modifier = Modifier.padding(dimensionResource(R.dimen.spacing_large)),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        scrimColor = MaterialTheme.colorScheme.scrim
    ) {
        Surface( // Обертка для основного контента, чтобы scrim корректно работал
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background // Фон для основного контента
        ) {
            Scaffold(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding() // Отступ для навигационного бара
                    .statusBarsPadding(), // Отступ для статус-бара
                containerColor = MaterialTheme.colorScheme.background, // Явный цвет фона Scaffold
                topBar = {
                    DryDriveTopAppBar(
                        uiState = uiState,
                        onQueryChange = { query ->
                            viewModel.onEvent(WeatherEvent.SearchQueryChanged(query))
                        },
                        onCitySelected = { city, formattedName ->
                            viewModel.onEvent(WeatherEvent.CitySelectedFromSearch(city,formattedName))
                        },
                        onDismissSearch = { viewModel.onEvent(WeatherEvent.DismissCitySearchDropDown) },
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
    uiState: WeatherUiState,
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
                        label = { Text(stringResource(R.string.search_city_placeholder), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = dimensionResource(R.dimen.spacing_large)), // Отступ справа для кнопки OK
                        singleLine = true,
                        textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = dimensionResource(R.dimen.font_size_medium_emphasis).value.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SearchFieldBorderFocused,
                            unfocusedBorderColor = SearchFieldBorderUnfocused,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            errorContainerColor = Color.Transparent,
                            cursorColor = MaterialTheme.colorScheme.primary,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                        trailingIcon = {
                            if (uiState.isLoadingCities) {
                                CircularProgressIndicator(modifier = Modifier.size(dimensionResource(R.dimen.icon_size_medium)), color = MaterialTheme.colorScheme.primary, strokeWidth = 2.dp)
                            } else if (uiState.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { onQueryChange("") }) {
                                    Icon(Icons.Filled.Clear, contentDescription = stringResource(R.string.clear_search_description), tint = MaterialTheme.colorScheme.onSurfaceVariant)
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
                            .padding(start = 0.dp) // Убрал отступ, чтобы иконка была ближе к краю
                    ) {
                        Icon(Icons.Filled.LocationOn, contentDescription = stringResource(R.string.location_description), modifier = Modifier.size(dimensionResource(R.dimen.icon_size_medium)))
                        Spacer(modifier = Modifier.width(dimensionResource(R.dimen.spacing_small)))
                        Text(
                            text = uiState.cityForDisplay.ifEmpty { stringResource(R.string.select_city_prompt) },
                            fontSize = dimensionResource(R.dimen.font_size_medium_emphasis).value.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Start,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(dimensionResource(R.dimen.spacing_small)))
                        Icon(Icons.Filled.ArrowDropDown, contentDescription = stringResource(R.string.select_city_action))
                    }
                }
                // DropdownMenu для результатов поиска
                DropdownMenu(
                    expanded = isCitySearchActiveInAppBar && uiState.citySearchResults.isNotEmpty() && uiState.searchQuery.isNotBlank(),
                    onDismissRequest = {
                        isCitySearchActiveInAppBar = false // Закрываем поиск по клику вне меню
                        onDismissSearch()                 // Сбрасываем результаты поиска
                    },
                    modifier = Modifier
                        .fillMaxWidth(0.9f) // Ширина выпадающего меню
                        .align(Alignment.TopStart) // Выравнивание
                        .offset(y = 56.dp) // Смещение вниз, чтобы не перекрывать TopAppBar
                ) {
                    uiState.citySearchResults.forEach { city ->
                        DropdownMenuItem(
                            text = { Text(formatCityName(city, uiState.currentLanguageCode)) },
                            onClick = {
                                val formattedName = formatCityName(city, uiState.currentLanguageCode)
                                onCitySelected(city, formattedName)
                                isCitySearchActiveInAppBar = false // Закрываем поиск
                            }
                        )
                    }
                }
            }
        },
        actions = {
            if (isCitySearchActiveInAppBar) {
                TextButton(onClick = {
                    isCitySearchActiveInAppBar = false
                    onDismissSearch() // Также сбрасываем результаты при нажатии OK
                }) {
                    Text(stringResource(R.string.action_ok), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                }
            } else {
                IconButton(onClick = { isCitySearchActiveInAppBar = true }) { Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.action_search)) }
                IconButton(onClick = onMenuClick) { Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.action_menu)) }
            }
        },
        navigationIcon = {
            IconButton(onClick = onMenuClick) { Icon(imageVector = Icons.Filled.Menu, contentDescription = "Open navigation drawer") }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface, // Цвет фона TopAppBar
            scrolledContainerColor = MaterialTheme.colorScheme.surface, // Цвет фона при прокрутке
            navigationIconContentColor = MaterialTheme.colorScheme.onSurface, // Цвет иконки навигации
            titleContentColor = MaterialTheme.colorScheme.onSurface, // Цвет заголовка
            actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant // Цвет иконок действий
        )
    )
}

data class BottomNavItem(val title: String, val icon: ImageVector)
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
    NavigationBar {
        items.forEachIndexed { index, item ->
            NavigationBarItem(
                selected = selectedIndex == index,
                onClick = { onItemSelected(index) },
                icon = { Icon(item.icon, contentDescription = item.title) },
                label = { Text(item.title) },
                // colors = NavigationBarItemDefaults.colors(...) // Для кастомизации цветов элементов
            )
        }
    }
}

@Composable
fun HomeScreenContent(modifier: Modifier = Modifier, uiState: WeatherUiState) {
    val carImageResId = when (uiState.weather?.weather?.getOrNull(0)?.main?.lowercase(Locale.ROOT)) {
        "rain", "snow", "thunderstorm", "drizzle", "mist", "fog" -> R.drawable.car_dirty
        else -> R.drawable.car_clean
    }
    val carContentDescription = if (carImageResId == R.drawable.car_dirty) stringResource(R.string.car_dirty_description) else stringResource(R.string.car_clean_description)
    val washRecommendation = when (uiState.weather?.weather?.getOrNull(0)?.main?.lowercase(Locale.ROOT)) {
        "rain", "snow", "thunderstorm", "drizzle" -> stringResource(R.string.wash_not_recommended)
        "mist", "fog" -> stringResource(R.string.wash_possible_limited_visibility)
        else -> if (uiState.weather != null && uiState.weather.main.temp > 5) stringResource(R.string.wash_great_day)
        else if (uiState.weather != null) stringResource(R.string.wash_good_weather_but_cool)
        else ""
    }

    Box(modifier = modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)) {
        Column(modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()) // Добавил вертикальный скролл для всего контента
        ) {
            // Секция с фоном города и текущей погодой/машиной
            Box(modifier = Modifier
                .fillMaxWidth()
                // .weight(1f) // weight не очень хорошо работает с verticalScroll, лучше задавать высоты явно или использовать minHeight
                .aspectRatio(1f / 1.1f) // Примерное соотношение для этой секции, подберите по вкусу
            ) {
                Image(
                    painter = painterResource(id = R.drawable.city_background),
                    contentDescription = stringResource(R.string.city_background_description),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(modifier = Modifier
                    .fillMaxSize()
                    .background(CityBackgroundOverlay))

                Column(modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = dimensionResource(R.dimen.spacing_xlarge))) {
                    Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_large)))
                    when {
                        uiState.isLoadingWeather && uiState.weather == null -> CircularProgressIndicator(
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(vertical = 50.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                        uiState.weatherErrorMessage != null && uiState.weather == null -> Text(
                            text = uiState.weatherErrorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 50.dp)
                        )
                        uiState.weather != null -> {
                            WeatherDetails(weather = uiState.weather)
                            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_medium)))
                            if (washRecommendation.isNotBlank()) {
                                Text(
                                    text = washRecommendation,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = dimensionResource(R.dimen.font_size_caption).value.sp,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                        else -> Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp), // Задаем высоту, чтобы Spacer.weight работал корректно
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = stringResource(R.string.select_city_prompt),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 50.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.weight(1f)) // Занимает оставшееся место, чтобы машина была внизу
                    if (uiState.weather != null) {
                        Image(
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

            // Секция рекомендаций
            Column(modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimensionResource(R.dimen.spacing_xlarge))
                .padding(top = dimensionResource(R.dimen.spacing_large))) {
                // ИСПРАВЛЕННЫЙ ВЫЗОВ:
                RecommendationsDisplaySection(recommendations = uiState.recommendations)
            }

            // Секция прогноза
            Column(modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimensionResource(R.dimen.spacing_xlarge))
                .padding(
                    top = dimensionResource(R.dimen.spacing_large),
                    bottom = dimensionResource(R.dimen.spacing_large)
                )) {
                when {
                    uiState.isLoadingForecast && uiState.dailyForecasts.isEmpty() -> Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp), // Задаем высоту для плейсхолдера
                        contentAlignment = Alignment.Center
                    ) {
                        DailyForecastPlaceholder() // Можно оставить или убрать, если CircularProgressIndicator достаточно
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, strokeWidth = 2.dp)
                    }
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
                    uiState.dailyForecasts.isNotEmpty() -> {
                        DailyForecastRow(forecasts = uiState.dailyForecasts)
                    }
                    else -> { // Если нет ни загрузки, ни ошибки, ни данных - показываем плейсхолдер
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
        Text(stringResource(R.string.map_screen_placeholder), color = MaterialTheme.colorScheme.onBackground)
    }
}

@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES, name = "Dark Mode")
@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_NO, name = "Light Mode")
@Composable
fun DryDriveAppPreview() {
    DryDriveTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            // Для превью можно создать фейковый ViewModel или передать null/пустой uiState, если ваш UI это обрабатывает
            // Либо используйте hiltViewModel() для превью, если настроено.
            val previewViewModel: WeatherViewModel = viewModel()
            DryDriveApp(viewModel = previewViewModel)
        }
    }
}
