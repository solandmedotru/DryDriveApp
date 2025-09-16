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
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import ru.devsoland.drydrive.common.ui.navigation.DryDriveBottomNavigationBar
import ru.devsoland.drydrive.common.ui.navigation.DryDriveTopAppBar
import ru.devsoland.drydrive.feature_map.ui.MapScreenPlaceholder
import ru.devsoland.drydrive.feature_settings.ui.SettingsScreen
import ru.devsoland.drydrive.feature_weather.ui.HomeScreenContent
import ru.devsoland.drydrive.feature_weather.ui.WeatherEvent
import ru.devsoland.drydrive.feature_weather.ui.WeatherViewModel
import ru.devsoland.drydrive.ui.theme.DryDriveTheme

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
                        uiState = uiState,
                        onEvent = viewModel::onEvent
                    )
                    1 -> MapScreenPlaceholder(modifier = Modifier.padding(paddingValues))
                    2 -> SettingsScreen(
                        modifier = Modifier.padding(paddingValues),
                        viewModel = viewModel // SettingsScreen уже получает viewModel
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
            // Для превью можно создать фейковый ViewModel или передать null/пустой uiState, если ваш UI это обрабатывает
            // Либо используйте hiltViewModel() для превью, если настроено.
            val previewViewModel: WeatherViewModel = viewModel()
            DryDriveApp(viewModel = previewViewModel)
        }
    }
}