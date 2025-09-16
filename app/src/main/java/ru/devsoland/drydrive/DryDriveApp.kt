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
// import androidx.lifecycle.viewmodel.compose.viewModel // viewModel будет инжектироваться в DryDriveApp
import kotlinx.coroutines.launch
import ru.devsoland.drydrive.common.ui.navigation.DryDriveBottomNavigationBar
import ru.devsoland.drydrive.common.ui.navigation.DryDriveTopAppBar
import ru.devsoland.drydrive.feature_map.ui.MapScreenPlaceholder
import ru.devsoland.drydrive.feature_settings.ui.SettingsScreen
// ЗАМЕНЯЕМ WeatherScreenContent на WeatherScreen
import ru.devsoland.drydrive.feature_weather.ui.WeatherScreen
import ru.devsoland.drydrive.feature_weather.ui.WeatherViewModel
import ru.devsoland.drydrive.ui.theme.DryDriveTheme
import androidx.hilt.navigation.compose.hiltViewModel // Для получения ViewModel в Composable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DryDriveApp(
    weatherViewModel: WeatherViewModel
) {

    val uiState by weatherViewModel.uiState.collectAsState() // Собираем uiState здесь

    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var selectedItemIndex by rememberSaveable { mutableStateOf(0) }

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
                        uiState = uiState, // Используем uiState из общей ViewModel
                        onEvent = weatherViewModel::onEvent,
                        onMenuClick = { scope.launch { drawerState.open() } }
                    )
                },
                bottomBar = {
                    DryDriveBottomNavigationBar(
                        selectedIndex = selectedItemIndex,
                        onItemSelected = { index -> selectedItemIndex = index }
                    )
                }
            ) { paddingValues -> // paddingValues от ЭТОГО Scaffold
                Log.d("DryDriveApp_Scaffold", "PaddingValues: Top=${paddingValues.calculateTopPadding()}, Bottom=${paddingValues.calculateBottomPadding()}")
                when (selectedItemIndex) {
                    0 -> // Экран Погоды
                        WeatherScreen(
                            modifier = Modifier.padding(paddingValues).fillMaxSize(), // Передаем paddingValues
                            viewModel = weatherViewModel // Передаем ту же ViewModel, чтобы uiState был синхронизирован
                            // или WeatherScreen может сам получить через hiltViewModel()
                            // если вы хотите разные инстансы или он не нужен в DryDriveApp напрямую
                        )
                    1 -> MapScreenPlaceholder(
                        modifier = Modifier.padding(paddingValues).fillMaxSize()
                    )
                    2 -> SettingsScreen(
                        modifier = Modifier.padding(paddingValues).fillMaxSize(),
                        viewModel = weatherViewModel // Передаем ту же ViewModel
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
            // Для превью потребуется создать мок/фейк WeatherViewModel,
            // так как hiltViewModel() не будет работать здесь без доп. настройки для превью.
            // Например:
            // val fakeViewModel = object : WeatherViewModel(...) { /* ... */ }
            // DryDriveApp(viewModel = fakeViewModel)
            Text("Preview of DryDriveApp requires a WeatherViewModel instance.")
        }
    }
}
