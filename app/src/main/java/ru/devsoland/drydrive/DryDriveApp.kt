package ru.devsoland.drydrive

import android.util.Log
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.launch
import ru.devsoland.drydrive.common.model.AppLanguage
import ru.devsoland.drydrive.common.ui.navigation.BottomNavItem
import ru.devsoland.drydrive.common.ui.navigation.DryDriveBottomNavigationBar
import ru.devsoland.drydrive.common.ui.navigation.DryDriveTopAppBar
import ru.devsoland.drydrive.feature_map.ui.MapScreen // *** ИЗМЕНЕН ИМПОРТ ***
import ru.devsoland.drydrive.feature_settings.ui.SettingsScreen
import ru.devsoland.drydrive.feature_weather.ui.WeatherEvent
import ru.devsoland.drydrive.feature_weather.ui.WeatherScreen
import ru.devsoland.drydrive.feature_weather.ui.WeatherViewModel
import ru.devsoland.drydrive.ui.theme.DryDriveTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DryDriveApp(
    weatherViewModel: WeatherViewModel
) {
    val uiState by weatherViewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    var selectedItemIndex by rememberSaveable { mutableStateOf(0) } // 0: WeatherMain, 1: Map, 2: Settings
    var isSearchFieldVisible by rememberSaveable { mutableStateOf(false) }
    var isSearchDropDownExpanded by rememberSaveable { mutableStateOf(false) }

    Log.d("DryDriveApp", "Recomposing DryDriveApp. Lang: ${uiState.currentLanguageCode}, SearchVisible: $isSearchFieldVisible, DropdownExpanded: $isSearchDropDownExpanded, SelectedIndex: $selectedItemIndex, ErrorMsg: ${uiState.citySearchErrorMessage}")

    val screenRoutes = listOf(
        BottomNavItem.WeatherMain.route,
        BottomNavItem.Map.route,
        BottomNavItem.Settings.route
    )

    fun onTopBarEvent(event: WeatherEvent) {
        Log.d("DryDriveApp", "onTopBarEvent: $event")
        weatherViewModel.onEvent(event)

        when (event) {
            is WeatherEvent.ShowSearchField -> {
                isSearchFieldVisible = true
                isSearchDropDownExpanded = uiState.citySearchQuery.isNotEmpty() && uiState.citySearchErrorMessage == null
            }
            is WeatherEvent.HideSearchFieldAndDismissDropdown -> {
                isSearchFieldVisible = false
                isSearchDropDownExpanded = false
            }
            is WeatherEvent.CitySelectedFromSearch -> {
                isSearchFieldVisible = false
                isSearchDropDownExpanded = false
            }
            is WeatherEvent.DismissCitySearchDropDown -> {
                isSearchDropDownExpanded = false
            }
            is WeatherEvent.SearchQueryChanged -> {
                isSearchDropDownExpanded = event.query.isNotEmpty() && isSearchFieldVisible && uiState.citySearchErrorMessage == null
            }
            else -> Unit
        }
    }

    val handleLanguageConfirmed: (AppLanguage) -> Unit = {
        selectedLanguage -> weatherViewModel.onEvent(WeatherEvent.ChangeLanguage(selectedLanguage))
    }

    val isMapScreenSelected = selectedItemIndex == 1

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = !isMapScreenSelected, // <<< ВОТ ИЗМЕНЕНИЕ: отключаем жесты на экране карты
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
                        uiState = uiState,
                        isSearchFieldVisible = isSearchFieldVisible,
                        isSearchDropDownExpanded = isSearchDropDownExpanded,
                        onEvent = ::onTopBarEvent,
                        onMenuClick = { scope.launch { drawerState.open() } }
                    )
                },
                bottomBar = {
                    DryDriveBottomNavigationBar(
                        currentRoute = screenRoutes.getOrNull(selectedItemIndex),
                        onNavigate = { route ->
                            val newIndex = screenRoutes.indexOf(route)
                            if (newIndex != -1) {
                                selectedItemIndex = newIndex
                                if (isSearchFieldVisible) {
                                    onTopBarEvent(WeatherEvent.HideSearchFieldAndDismissDropdown)
                                }
                            }
                        }
                    )
                }
            ) { paddingValues ->
                when (selectedItemIndex) {
                    0 -> WeatherScreen(
                        modifier = Modifier.padding(paddingValues).fillMaxSize(),
                        viewModel = weatherViewModel
                    )
                    1 -> MapScreen( // *** ИЗМЕНЕН ВЫЗОВ ФУНКЦИИ ***
                        modifier = Modifier.padding(paddingValues).fillMaxSize()
                    )
                    2 -> SettingsScreen(
                        modifier = Modifier.padding(paddingValues).fillMaxSize(),
                        onLanguageConfirmed = handleLanguageConfirmed
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES, name = "Dark Mode")
@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_NO, name = "Light Mode")
@Composable
fun DryDriveAppPreview() {
    DryDriveTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Text("Preview of DryDriveApp requires a WeatherViewModel instance and Hilt setup for preview.")
        }
    }
}
