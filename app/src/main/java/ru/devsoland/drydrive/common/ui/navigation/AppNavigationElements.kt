package ru.devsoland.drydrive.common.ui.navigation

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.CircularProgressIndicator // РАСКОММЕНТИРОВАНО
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.devsoland.drydrive.R
import ru.devsoland.drydrive.common.util.formatCityName
import ru.devsoland.drydrive.data.api.model.City
import ru.devsoland.drydrive.feature_weather.ui.WeatherEvent
import ru.devsoland.drydrive.feature_weather.ui.WeatherUiState
import ru.devsoland.drydrive.ui.theme.SearchFieldBorderFocused
import ru.devsoland.drydrive.ui.theme.SearchFieldBorderUnfocused

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun DryDriveTopAppBar(
    uiState: WeatherUiState,
    onEvent: (WeatherEvent) -> Unit,
    onMenuClick: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    Log.d("TopAppBarDebug", "Recomposing DryDriveTopAppBar. isSearchFieldVisible=${uiState.isSearchFieldVisible}, isLoadingCities=${uiState.isLoadingCities}, searchQuery='${uiState.searchQuery}', isDropDownExpanded=${uiState.isSearchDropDownExpanded}")

    CenterAlignedTopAppBar(
        title = {
            Box(modifier = Modifier.fillMaxWidth()) {
                if (uiState.isSearchFieldVisible) {
                    Log.d("TopAppBarDebug", "Search field IS visible.")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = uiState.searchQuery,
                                onValueChange = {
                                    Log.d("TopAppBarDebug", "OutlinedTextField onValueChange: '$it'")
                                    onEvent(WeatherEvent.SearchQueryChanged(it))
                                },
                                label = { Text(stringResource(R.string.search_city_placeholder), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                modifier = Modifier
                                    .fillMaxWidth()
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
                                    if (uiState.searchQuery.isNotEmpty() && !uiState.isLoadingCities) { // Не показываем крестик во время загрузки, чтобы не было конфликта с индикатором, если он там
                                        IconButton(onClick = {
                                            Log.d("TopAppBarDebug", "Clear icon clicked.")
                                            onEvent(WeatherEvent.SearchQueryChanged(""))
                                            scope.launch {
                                                Log.d("FocusDebug", "Clear clicked. Requesting focus and show keyboard.")
                                                delay(50) // Небольшая задержка перед запросом фокуса
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
                                    // Можно также рассмотреть вариант показа индикатора загрузки здесь,
                                    // если он не будет конфликтовать с иконкой очистки
                                    /* else if (uiState.isLoadingCities) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(dimensionResource(R.dimen.icon_size_small)), // Маленький индикатор
                                            color = MaterialTheme.colorScheme.primary,
                                            strokeWidth = 2.dp
                                        )
                                    }*/
                                }
                            )
                            // РАСКОММЕНТИРОВАНО: Отображение текста ошибки
                            uiState.citySearchErrorMessage?.let { messageResId ->
                                // Показываем ошибку только если DropdownMenu не отображается (чтобы не дублировать информацию)
                                // или если DropdownMenu пуст (например, "город не найден" можно показать под полем)
                                if (!uiState.isSearchDropDownExpanded || uiState.citySearchResults.isEmpty()) {
                                    Log.d("TopAppBarDebug", "Displaying citySearchErrorMessage.")
                                    Text(
                                        text = stringResource(id = messageResId),
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                                    )
                                }
                            }
                        }

                        // РАСКОММЕНТИРОВАНО: Индикатор загрузки рядом с полем
                        // Появляется только если идет загрузка и строка поиска не пуста
                        if (uiState.isLoadingCities && uiState.searchQuery.isNotEmpty()) {
                            Log.d("TopAppBarDebug", "Displaying CircularProgressIndicator next to field.")
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .padding(start = 8.dp, end = 8.dp) // Добавил отступ справа для симметрии
                                    .size(dimensionResource(R.dimen.icon_size_medium)),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 2.dp
                            )
                        } else if (uiState.searchQuery.isNotEmpty()){
                            // Добавляем Spacer такой же ширины, как индикатор, чтобы TextField не "прыгал"
                            // когда индикатор появляется/исчезает, если есть текст, но загрузка не идет.
                            // Это предотвратит изменение ширины TextField.
                            Spacer(modifier = Modifier
                                .padding(start = 8.dp, end = 8.dp)
                                .size(dimensionResource(R.dimen.icon_size_medium))
                            )
                        }
                    }

                    LaunchedEffect(uiState.isSearchFieldVisible) {
                        if (uiState.isSearchFieldVisible) {
                            scope.launch {
                                Log.d("FocusDebug", "Effect for isSearchFieldVisible: STARTING. Delay 100ms.")
                                delay(100)
                                Log.d("FocusDebug", "Effect for isSearchFieldVisible: Requesting focus...")
                                focusRequester.requestFocus()
                                Log.d("FocusDebug", "Effect for isSearchFieldVisible: Showing keyboard...")
                                keyboardController?.show()
                                Log.d("FocusDebug", "Effect for isSearchFieldVisible: DONE.")
                            }
                        }
                    }

                    LaunchedEffect(uiState.isLoadingCities) {
                        // Этот эффект теперь в основном для отладки, т.к. клавиатура должна оставаться
                        // благодаря условной композиции DropdownMenu
                        if (uiState.isSearchFieldVisible && uiState.isLoadingCities) {
                            scope.launch {
                                Log.d("FocusDebug", "Effect for isLoadingCities: STARTING. Delay 200ms.")
                                delay(200)
                                Log.d("FocusDebug", "Effect for isLoadingCities: Attempting to maintain focus/keyboard...")
                                if (!focusRequester.captureFocus()) { // Пробуем запросить, если еще не в фокусе
                                    focusRequester.requestFocus()
                                }
                                keyboardController?.show() // Повторный вызов show() обычно безопасен
                                Log.d("FocusDebug", "Effect for isLoadingCities: Focus/keyboard check DONE.")
                            }
                        }
                    }

                } else { // Отображаем выбранный город
                    Log.d("TopAppBarDebug", "Search field IS NOT visible. Displaying city: '${uiState.cityForDisplay}'")
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                Log.d("TopAppBarDebug", "City display clicked. Showing search field.")
                                onEvent(WeatherEvent.ShowSearchField)
                            }
                            .padding(start = 0.dp) // Убрал лишний отступ для заголовка
                    ) {
                        Icon(
                            Icons.Filled.LocationOn,
                            contentDescription = stringResource(R.string.location_description),
                            modifier = Modifier.size(dimensionResource(R.dimen.icon_size_medium))
                        )
                        Spacer(modifier = Modifier.width(dimensionResource(R.dimen.spacing_small)))
                        Text(
                            text = uiState.cityForDisplay.ifEmpty { stringResource(R.string.select_city_prompt) },
                            fontSize = dimensionResource(R.dimen.font_size_medium_emphasis).value.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Start, // Явно указываем выравнивание
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(dimensionResource(R.dimen.spacing_small)))
                        Icon(
                            Icons.Filled.ArrowDropDown,
                            contentDescription = stringResource(R.string.select_city_action)
                        )
                    }
                }

                // УСЛОВНАЯ КОМПОЗИЦИЯ DropdownMenu
                if (uiState.isSearchFieldVisible && uiState.isSearchDropDownExpanded) {
                    Log.d("TopAppBarDebug", "CONDITIONAL COMPOSITION: DropdownMenu WILL be composed.")
                    DropdownMenu(
                        expanded = true, // Всегда true, так как компонуется только когда должен быть expanded
                        onDismissRequest = {
                            Log.d("TopAppBarDebug", "DropdownMenu onDismissRequest.")
                            onEvent(WeatherEvent.DismissCitySearchDropDown)
                        },
                        modifier = Modifier
                            .fillMaxWidth(0.9f) // Оставляем немного места по бокам
                            .align(Alignment.TopStart) // Выравниваем по началу Box
                            .offset(y = dimensionResource(R.dimen.top_app_bar_height)) // Смещаем под TopAppBar
                    ) {
                        if (uiState.citySearchResults.isNotEmpty()) {
                            Log.d("TopAppBarDebug", "DropdownMenu rendering ${uiState.citySearchResults.size} items.")
                            uiState.citySearchResults.forEach { city ->
                                DropdownMenuItem(
                                    text = { Text(formatCityName(city, uiState.currentLanguageCode)) },
                                    onClick = {
                                        val formattedName = formatCityName(city, uiState.currentLanguageCode)
                                        Log.d("TopAppBarDebug", "DropdownMenuItem clicked: ${city.name}")
                                        onEvent(WeatherEvent.CitySelectedFromSearch(city, formattedName))
                                        // Клавиатура, скорее всего, скроется здесь, и это приемлемо
                                    }
                                )
                            }
                        } else {
                            // Этот блок не должен вызываться, если isSearchDropDownExpanded=true только при непустых результатах
                            Log.d("TopAppBarDebug", "DropdownMenu: citySearchResults is empty despite isSearchDropDownExpanded being true. Check ViewModel logic.")
                            // Можно добавить какой-нибудь placeholder или сообщение "Ничего не найдено" внутри самого DropdownMenu
                            DropdownMenuItem(
                                text = { Text(stringResource(id = R.string.city_not_found_short)) },
                                onClick = { onEvent(WeatherEvent.DismissCitySearchDropDown) }, // Просто закрыть
                                enabled = false
                            )
                        }
                    }
                } else {
                    if (uiState.isSearchFieldVisible) {
                        Log.d("TopAppBarDebug", "CONDITIONAL COMPOSITION: DropdownMenu WILL NOT be composed (isSearchDropDownExpanded=${uiState.isSearchDropDownExpanded}).")
                    }
                }
            }
        },
        actions = {
            if (uiState.isSearchFieldVisible) {
                // Кнопка OK видна только если не идет загрузка (и поле поиска видно)
                // И если нет активного сообщения об ошибке поиска (чтобы не перекрывать)
                if (!uiState.isLoadingCities) {
                    Log.d("TopAppBarDebug", "Displaying OK button in actions.")
                    TextButton(onClick = {
                        Log.d("TopAppBarDebug", "OK button clicked.")
                        // Если есть текст и нет ошибки, можно считать это подтверждением текущего текста
                        // (если нет результатов, то город не выбран, но текст остается)
                        // ViewModel должен обработать searchQuery, если он не пуст
                        onEvent(WeatherEvent.HideSearchFieldAndDismissDropdown)
                    }) {
                        Text(
                            stringResource(R.string.action_ok),
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    // Если идет загрузка, кнопка OK не отображается, т.к. индикатор уже есть рядом с полем
                    Log.d("TopAppBarDebug", "OK button is NOT displayed (isLoadingCities is true).")
                }
            } else { // Показываем стандартные иконки, если поле поиска не видно
                Log.d("TopAppBarDebug", "Displaying Search and MoreVert icons in actions.")
                IconButton(onClick = {
                    Log.d("TopAppBarDebug", "Search icon in actions clicked.")
                    onEvent(WeatherEvent.ShowSearchField)
                }) {
                    Icon(
                        Icons.Filled.Search,
                        contentDescription = stringResource(R.string.action_search)
                    )
                }
                IconButton(onClick = {
                    Log.d("TopAppBarDebug", "MoreVert icon in actions clicked.")
                    onMenuClick() // Для открытия Navigation Drawer или другого меню
                }) {
                    Icon(
                        Icons.Filled.MoreVert,
                        contentDescription = stringResource(R.string.action_menu)
                    )
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = {
                Log.d("TopAppBarDebug", "Navigation icon (Menu) clicked.")
                onMenuClick() // Для открытия Navigation Drawer
            }) {
                Icon(
                    imageVector = Icons.Filled.Menu,
                    contentDescription = stringResource(R.string.open_navigation_drawer)
                )
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface, // или surfaceContainer для Elevation эффекта
            scrolledContainerColor = MaterialTheme.colorScheme.surface,
            navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant // Иконки действий могут иметь другой цвет
        )
    )
}

// Data class для элементов нижней навигации
data class BottomNavItem(
    val title: String,
    val icon: ImageVector,
    // Можно добавить route: String, если используется для навигации
)

@Composable
fun DryDriveBottomNavigationBar(
    selectedIndex: Int, // Текущий выбранный элемент
    onItemSelected: (Int) -> Unit // Функция обратного вызова при выборе элемента
    // items: List<BottomNavItem> // Можно передавать список элементов снаружи для большей гибкости
) {
    val items = listOf(
        BottomNavItem(stringResource(R.string.nav_home), Icons.Filled.Home),
        BottomNavItem(stringResource(R.string.nav_map), Icons.Filled.Place), // Используем Place для карты
        BottomNavItem(stringResource(R.string.nav_settings), Icons.Filled.Settings)
    )

    NavigationBar {
        items.forEachIndexed { index, item ->
            NavigationBarItem(
                selected = selectedIndex == index,
                onClick = { onItemSelected(index) },
                icon = { Icon(item.icon, contentDescription = item.title) },
                label = { Text(item.title) },
                // alwaysShowLabel = false, // Можно настроить, чтобы метки показывались только для активного элемента
            )
        }
    }
}
