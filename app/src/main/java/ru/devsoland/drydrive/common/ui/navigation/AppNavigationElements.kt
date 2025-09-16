package ru.devsoland.drydrive.common.ui.navigation

// import android.util.Log // Удалено
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext // Остается, используется в DryDriveTopAppBar
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.devsoland.drydrive.R
import ru.devsoland.drydrive.common.util.formatCityName
import ru.devsoland.drydrive.feature_weather.ui.WeatherEvent
import ru.devsoland.drydrive.feature_weather.ui.WeatherUiState
import ru.devsoland.drydrive.ui.theme.SearchFieldBorderFocused
import ru.devsoland.drydrive.ui.theme.SearchFieldBorderUnfocused
// import androidx.appcompat.app.AppCompatDelegate // Удалено
// import androidx.core.os.ConfigurationCompat // Удалено
// import java.util.Locale // Удалено

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun DryDriveTopAppBar(
    uiState: WeatherUiState,
    isSearchFieldVisible: Boolean, 
    isSearchDropDownExpanded: Boolean, 
    onEvent: (WeatherEvent) -> Unit,
    onMenuClick: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    CenterAlignedTopAppBar(
        title = {
            if (isSearchFieldVisible) {
                ExposedDropdownMenuBox(
                    expanded = isSearchDropDownExpanded && uiState.citySearchErrorMessage == null, 
                    onExpandedChange = {
                        if (!it) {
                            onEvent(WeatherEvent.DismissCitySearchDropDown)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = uiState.citySearchQuery,
                        onValueChange = { query ->
                            onEvent(WeatherEvent.SearchQueryChanged(query))
                        },
                        label = { Text(stringResource(R.string.search_city_placeholder), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                            .focusRequester(focusRequester),
                        singleLine = true,
                        isError = uiState.citySearchErrorMessage != null,
                        textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = dimensionResource(R.dimen.font_size_medium_emphasis).value.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (uiState.citySearchErrorMessage != null) MaterialTheme.colorScheme.error else SearchFieldBorderFocused,
                            unfocusedBorderColor = if (uiState.citySearchErrorMessage != null) MaterialTheme.colorScheme.error else SearchFieldBorderUnfocused,
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
                            Row {
                                if (uiState.isLoadingCities && uiState.citySearchQuery.isNotEmpty()) {
                                    CircularProgressIndicator(
                                        modifier = Modifier
                                            .padding(end = 8.dp)
                                            .size(dimensionResource(R.dimen.icon_size_small)),
                                        color = MaterialTheme.colorScheme.primary,
                                        strokeWidth = 2.dp
                                    )
                                }
                                if (uiState.citySearchQuery.isNotEmpty() && !uiState.isLoadingCities) {
                                    IconButton(onClick = {
                                        onEvent(WeatherEvent.SearchQueryChanged(""))
                                        scope.launch {
                                            delay(50)
                                            focusRequester.requestFocus()
                                            keyboardController?.show()
                                        }
                                    }) {
                                        Icon(
                                            Icons.Filled.Clear,
                                            contentDescription = stringResource(R.string.clear_search_description),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                } 
                            }
                        }
                    )
                    ExposedDropdownMenu(
                        expanded = isSearchDropDownExpanded && uiState.citySearchErrorMessage == null, 
                        onDismissRequest = {
                            onEvent(WeatherEvent.DismissCitySearchDropDown)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (uiState.isLoadingCities && uiState.citySearchQuery.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(dimensionResource(R.dimen.padding_medium)),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        } else if (uiState.cities.isNotEmpty()) {
                            uiState.cities.forEach { city ->
                                val formattedName = formatCityName(city, uiState.currentLanguageCode ?: "en")
                                DropdownMenuItem(
                                    text = { Text(formattedName) },
                                    onClick = {
                                        onEvent(WeatherEvent.CitySelectedFromSearch(city, formattedName))
                                    }
                                )
                            }
                        } else if (uiState.citySearchQuery.length >= 2 && uiState.citySearchErrorMessage == null) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.city_search_no_results_in_dropdown)) },
                                onClick = { /* No-op */ }
                            )
                        }
                    }
                }
                LaunchedEffect(isSearchFieldVisible) { 
                    if (isSearchFieldVisible) {
                        scope.launch {
                            delay(100)
                            focusRequester.requestFocus()
                            keyboardController?.show()
                        }
                    }
                }
            } else { 
                val cityToDisplay = uiState.selectedCity?.let {
                    formatCityName(it, uiState.currentLanguageCode ?: "en")
                } ?: stringResource(R.string.select_city_prompt)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onEvent(WeatherEvent.ShowSearchField)
                        }
                        .padding(start = 0.dp)
                ) {
                    Icon(
                        Icons.Filled.LocationOn,
                        contentDescription = stringResource(R.string.location_description),
                        modifier = Modifier.size(dimensionResource(R.dimen.icon_size_medium))
                    )
                    Spacer(Modifier.width(dimensionResource(R.dimen.padding_small)))
                    Text(
                        text = cityToDisplay,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onMenuClick) {
                Icon(
                    Icons.Filled.Menu,
                    contentDescription = stringResource(R.string.action_menu),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        actions = {
            if (isSearchFieldVisible) { 
                IconButton(onClick = {
                    onEvent(WeatherEvent.HideSearchFieldAndDismissDropdown)
                }) {
                    Icon(
                        Icons.Filled.Place, 
                        contentDescription = stringResource(R.string.hide_search_description),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            } else {
                IconButton(onClick = {
                    onEvent(WeatherEvent.ShowSearchField)
                }) {
                    Icon(
                        Icons.Filled.Search,
                        contentDescription = stringResource(R.string.action_search),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

sealed class BottomNavItem(val route: String, val titleResId: Int, val icon: ImageVector) {
    object WeatherMain : BottomNavItem(
        route = "weather_screen", 
        titleResId = R.string.nav_home,
        icon = Icons.Filled.Home
    )
    object Map : BottomNavItem(
        route = "map_screen", 
        titleResId = R.string.nav_map, 
        icon = Icons.Filled.Place 
    )
    object Settings : BottomNavItem(
        route = "settings_screen", 
        titleResId = R.string.nav_settings,
        icon = Icons.Filled.Settings
    )
}

@Composable
fun DryDriveBottomNavigationBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    val items = listOf(
        BottomNavItem.WeatherMain,
        BottomNavItem.Map,
        BottomNavItem.Settings
    )

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface, 
        contentColor = MaterialTheme.colorScheme.onSurface   
    ) {
        items.forEach { item ->
            val isSelected = currentRoute == item.route
            val resolvedString = stringResource(item.titleResId)

            // Log.d("BottomNavLocale", "Item: ${item.route}, TitleResId: ${item.titleResId}, ResolvedString: '$resolvedString', ContextLocale: $currentLocale, AppCompatLocale: '$appCompatLocaleTag'") // Лог удален

            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = resolvedString
                    )
                },
                label = { Text(resolvedString) },
                selected = isSelected,
                onClick = {
                    if (!isSelected) { 
                        onNavigate(item.route)
                    }
                },
                colors = androidx.compose.material3.NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    indicatorColor = MaterialTheme.colorScheme.secondaryContainer 
                )
            )
        }
    }
}
