package ru.devsoland.drydrive.common.ui.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.devsoland.drydrive.R
import ru.devsoland.drydrive.common.util.formatCityName
import ru.devsoland.drydrive.data.api.model.City
import ru.devsoland.drydrive.feature_weather.ui.WeatherUiState
import ru.devsoland.drydrive.ui.theme.SearchFieldBorderFocused
import ru.devsoland.drydrive.ui.theme.SearchFieldBorderUnfocused
import kotlin.text.ifEmpty


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