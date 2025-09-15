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
import ru.devsoland.drydrive.data.City // Убедитесь, что City имеет localNames: Map<String, String>?
import ru.devsoland.drydrive.data.preferences.AppLanguage
import ru.devsoland.drydrive.data.preferences.LanguageManager
import ru.devsoland.drydrive.di.LanguageManagerEntryPoint
import ru.devsoland.drydrive.ui.composables.DailyForecastPlaceholder
import ru.devsoland.drydrive.ui.composables.DailyForecastRow
import ru.devsoland.drydrive.ui.composables.WeatherDetails
import ru.devsoland.drydrive.ui.composables.WeatherRecommendationSection
import ru.devsoland.drydrive.ui.settings.SettingsScreen
import ru.devsoland.drydrive.ui.theme.CityBackgroundOverlay
import ru.devsoland.drydrive.ui.theme.DryDriveTheme
import ru.devsoland.drydrive.ui.theme.SearchFieldBorderFocused
import ru.devsoland.drydrive.ui.theme.SearchFieldBorderUnfocused
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

// ОБНОВЛЕННАЯ ФУНКЦИЯ formatCityName
fun formatCityName(city: City, currentAppLanguageCode: String): String {
    val displayName = when {
        // 1. Пытаемся использовать язык приложения, если он не пуст и есть в localNames
        currentAppLanguageCode.isNotBlank() && city.localNames?.containsKey(currentAppLanguageCode) == true -> {
            city.localNames[currentAppLanguageCode]
        }
        // 2. Если язык приложения пуст (системный) ИЛИ для него нет записи, пытаемся использовать "en" из localNames
        city.localNames?.containsKey("en") == true -> {
            city.localNames["en"]
        }
        // 3. Фолбэк на основное имя city.name
        else -> {
            city.name
        }
    } ?: city.name // Дополнительный фолбэк на случай, если из localNames пришел null

    return "$displayName, ${city.country}" + if (city.state != null) ", ${city.state}" else ""
}
// --- КОНЕЦ МОДЕЛЕЙ ---

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val dryDriveViewModel: DryDriveViewModel by viewModels()

    override fun attachBaseContext(newBase: Context) {
        // ... (код без изменений) ...
        Log.d("MainActivityLifecycle", "attachBaseContext CALLED. Initial base context locale: ${newBase.resources.configuration.locale}")

        val languageManager: LanguageManager
        try {
            val entryPoint = EntryPointAccessors.fromApplication(
                newBase.applicationContext,
                LanguageManagerEntryPoint::class.java
            )
            languageManager = entryPoint.getLanguageManager()
            Log.d("MainActivityLifecycle", "LanguageManager obtained successfully via EntryPoint.")
        } catch (e: IllegalStateException) {
            Log.e("MainActivityLifecycle", "Hilt not ready in attachBaseContext to get LanguageManager: ${e.message}. Using default base context.", e)
            super.attachBaseContext(newBase)
            return
        } catch (e: Exception) {
            Log.e("MainActivityLifecycle", "Unexpected error getting LanguageManager: ${e.message}. Using default base context.", e)
            super.attachBaseContext(newBase)
            return
        }

        val currentLanguageCode = try {
            Log.d("MainActivityLifecycle", "attachBaseContext: Attempting to get language code via runBlocking...")
            runBlocking {
                languageManager.selectedLanguageFlow.first().code
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
        // ... (код без изменений) ...
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
            dryDriveViewModel.recreateActivityEvent.collect {
                Log.d("MainActivityLifecycle", "recreateActivityEvent received, calling recreate()")
                this@MainActivity.recreate()
            }
        }

        setContent {
            // val currentLanguage by dryDriveViewModel.currentLanguage.collectAsState() // Это теперь не нужно здесь, если uiState содержит код языка
            // Log.d("MainActivityCompose", "Recomposing with current language from ViewModel: ${currentLanguage.code}")

            DryDriveTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DryDriveApp(viewModel = dryDriveViewModel)
                }
            }
        }
    }
}

// --- УТИЛИТНАЯ ФУНКЦИЯ ДЛЯ УСТАНОВКИ ЛОКАЛИ ---
fun Context.setAppLocale(languageCode: String): Context {
    // ... (код без изменений) ...
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
        val localeList = android.os.LocaleList(localeToSet)
        config.setLocales(localeList)
    } else {
        @Suppress("DEPRECATION")
        config.locale = localeToSet
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val localeListCompat = if (languageCode.isEmpty()) {
            Log.d("AppLocaleConfig", "AppCompatDelegate: Setting to empty list (system default).")
            LocaleListCompat.getEmptyLocaleList()
        } else {
            Log.d("AppLocaleConfig", "AppCompatDelegate: Setting to language tags: $languageCode")
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
// --- КОНЕЦ УТИЛИТНОЙ ФУНКЦИИ ---


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DryDriveApp( viewModel: DryDriveViewModel ) {
    val uiState by viewModel.uiState.collectAsState() // uiState теперь должен содержать currentLanguageCode
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var selectedItemIndex by rememberSaveable { mutableStateOf(0) }

    // Логируем код языка из uiState для проверки
    Log.d("DryDriveApp", "Recomposing DryDriveApp with lang code from uiState: ${uiState.currentLanguageCode}")


    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.systemBarsPadding(),
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
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Scaffold(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .statusBarsPadding(),
                containerColor = MaterialTheme.colorScheme.background,
                topBar = {
                    DryDriveTopAppBar(
                        uiState = uiState, // uiState передается как есть
                        onQueryChange = { query ->
                            viewModel.onEvent(DryDriveEvent.SearchQueryChanged(query))
                        },
                        onCitySelected = { city, formattedName ->
                            viewModel.onEvent(DryDriveEvent.CitySelectedFromSearch(city,formattedName))
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
    uiState: DryDriveUiState, // uiState содержит currentLanguageCode
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
                    // ... (код OutlinedTextField без изменений) ...
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = onQueryChange,
                        label = { Text(stringResource(R.string.search_city_placeholder), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = dimensionResource(R.dimen.spacing_large)),
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
                            .padding(start = 0.dp)
                    ) {
                        Icon(Icons.Filled.LocationOn, contentDescription = stringResource(R.string.location_description), modifier = Modifier.size(dimensionResource(R.dimen.icon_size_medium)))
                        Spacer(modifier = Modifier.width(dimensionResource(R.dimen.spacing_small)))
                        Text(
                            // uiState.cityForDisplay уже должен быть локализован ViewModel'ю
                            // при выборе города, так как formattedName формируется с учетом языка
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
                DropdownMenu(
                    expanded = isCitySearchActiveInAppBar && uiState.citySearchResults.isNotEmpty() && uiState.searchQuery.isNotBlank(),
                    onDismissRequest = {
                        isCitySearchActiveInAppBar = false
                        onDismissSearch()
                    },
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .align(Alignment.TopStart)
                        .offset(y = 56.dp)
                ) {
                    uiState.citySearchResults.forEach { city ->
                        DropdownMenuItem(
                            text = {
                                // ИСПОЛЬЗУЕМ uiState.currentLanguageCode
                                Text(formatCityName(city, uiState.currentLanguageCode))
                            },
                            onClick = {
                                // ИСПОЛЬЗУЕМ uiState.currentLanguageCode для формирования имени,
                                // которое пойдет в ViewModel и затем в uiState.cityForDisplay
                                val formattedName = formatCityName(city, uiState.currentLanguageCode)
                                onCitySelected(city, formattedName)
                                isCitySearchActiveInAppBar = false
                            }
                        )
                    }
                }
            }
        },
        // ... (остальные параметры TopAppBar без изменений) ...
        actions = {
            if (isCitySearchActiveInAppBar) {
                TextButton(onClick = { isCitySearchActiveInAppBar = false }) {
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
            containerColor = MaterialTheme.colorScheme.surface,
            scrolledContainerColor = MaterialTheme.colorScheme.surface,
            navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    )
}

data class BottomNavItem(val title: String, val icon: ImageVector)
@Composable
fun DryDriveBottomNavigationBar(
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit
) {
    // ... (код без изменений, ошибка про item.icon - это отдельный вопрос, не связанный с языком) ...
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
            )
        }
    }
}

@Composable
fun HomeScreenContent(modifier: Modifier = Modifier, uiState: DryDriveUiState) {
    // ... (код без изменений) ...
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
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier
                .fillMaxWidth()
                .weight(1f)) {
                Image(painter = painterResource(id = R.drawable.city_background), contentDescription = stringResource(R.string.city_background_description), modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                Box(modifier = Modifier
                    .fillMaxSize()
                    .background(CityBackgroundOverlay))
                Column(modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = dimensionResource(R.dimen.spacing_xlarge))) {
                    Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_large)))
                    when {
                        uiState.isLoadingWeather && uiState.weather == null -> CircularProgressIndicator(modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(vertical = 50.dp), color = MaterialTheme.colorScheme.primary)
                        uiState.weatherErrorMessage != null && uiState.weather == null -> Text(text = uiState.weatherErrorMessage!!, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center, modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 50.dp))
                        uiState.weather != null -> {
                            WeatherDetails(weather = uiState.weather)
                            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_medium)))
                            if (washRecommendation.isNotBlank()) {
                                Text(text = washRecommendation, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = dimensionResource(R.dimen.font_size_caption).value.sp, modifier = Modifier.fillMaxWidth())
                            }
                        }
                        else -> Column(modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = stringResource(R.string.select_city_prompt), color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 50.dp))
                        }
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    if (uiState.weather != null) {
                        Image(painter = painterResource(id = carImageResId), contentDescription = carContentDescription, modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .aspectRatio(16f / 9f)
                            .align(Alignment.CenterHorizontally)
                            .padding(bottom = dimensionResource(R.dimen.spacing_large)), contentScale = ContentScale.Fit)
                    }
                }
            }
            Column(modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimensionResource(R.dimen.spacing_xlarge))
                .padding(top = dimensionResource(R.dimen.spacing_large))) {
                WeatherRecommendationSection(weather = uiState.weather)
            }
            Column(modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimensionResource(R.dimen.spacing_xlarge))
                .padding(
                    top = dimensionResource(R.dimen.spacing_large),
                    bottom = dimensionResource(R.dimen.spacing_large)
                )) {
                when {
                    uiState.isLoadingForecast && uiState.dailyForecasts.isEmpty() -> Box(modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp), contentAlignment = Alignment.Center) {
                        DailyForecastPlaceholder()
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, strokeWidth = 2.dp)
                    }
                    uiState.forecastErrorMessage != null && uiState.dailyForecasts.isEmpty() -> Box(modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp), contentAlignment = Alignment.Center) {
                        Text(text = stringResource(R.string.forecast_unavailable), color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f), textAlign = TextAlign.Center)
                    }
                    uiState.dailyForecasts.isNotEmpty() -> { DailyForecastRow(forecasts = uiState.dailyForecasts) }
                    else -> { DailyForecastPlaceholder() }
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
            // Для Preview может потребоваться фейковая ViewModel или передача uiState напрямую,
            // если DryDriveViewModel() без Hilt не работает корректно в Preview.
            // val previewViewModel: DryDriveViewModel = viewModel() // Убедитесь, что это работает в Preview
            // DryDriveApp(viewModel = previewViewModel)
            // ИЛИ:
            DryDriveApp(viewModel = viewModel()) // Оставляем так, если работает
        }
    }
}

