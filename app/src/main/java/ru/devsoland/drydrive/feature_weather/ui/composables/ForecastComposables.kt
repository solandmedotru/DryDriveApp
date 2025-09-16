package ru.devsoland.drydrive.feature_weather.ui.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize // Для анимации изменения размера
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items // Используем items с ключом
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.devsoland.drydrive.R
// ИМПОРТИРУЕМ НОВУЮ UI МОДЕЛЬ
import ru.devsoland.drydrive.feature_weather.ui.model.ForecastCardUiModel

@Composable
fun DailyForecastRow(
    forecastItems: List<ForecastCardUiModel>, // Принимаем List<ForecastCardUiModel>
    modifier: Modifier = Modifier
) {
    if (forecastItems.isEmpty()) {
        // Можно отобразить заглушку или ничего, если список пуст
        // Text(stringResource(R.string.forecast_data_not_available), modifier = modifier.padding(all = dimensionResource(R.dimen.spacing_medium)))
        return
    }
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_medium)), // Уменьшил немного для плотности
        contentPadding = PaddingValues(horizontal = dimensionResource(R.dimen.padding_medium))
    ) {
        items(
            items = forecastItems,
            key = { item -> item.id } // Используем id из UI модели как ключ
        ) { uiModel ->
            ForecastDayCard(
                uiModel = uiModel
                // isHighlighted передается внутри uiModel
            )
        }
    }
}

@Composable
fun ForecastDayCard(
    uiModel: ForecastCardUiModel,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    val cardContainerColor = if (uiModel.isHighlighted) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val cardContentColor = if (uiModel.isHighlighted) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        shape = RoundedCornerShape(dimensionResource(R.dimen.corner_radius_large)), // Немного увеличил радиус
        colors = CardDefaults.cardColors(
            containerColor = cardContainerColor,
            contentColor = cardContentColor
        ),
        modifier = modifier
            .width(dimensionResource(R.dimen.forecast_card_width_expanded)) // Можно задать ширину для раскрытой карточки
            .animateContentSize() // Анимируем изменение размера карточки
            .clickable { isExpanded = !isExpanded }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(R.dimen.spacing_medium)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- Свернутый вид (основная информация + стрелка) ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small)),
                    modifier = Modifier.weight(1f) // Чтобы основной контент занимал место
                ) {
                    Text(
                        text = uiModel.dayShort,
                        style = MaterialTheme.typography.bodyMedium, // Используем стили темы
                        fontWeight = FontWeight.SemiBold
                    )
                    Icon(
                        painter = painterResource(id = uiModel.iconRes),
                        contentDescription = stringResource(R.string.forecast_icon_description_template, uiModel.dayShort),
                        modifier = Modifier.size(dimensionResource(R.dimen.icon_size_medium_large)) // Чуть больше иконка
                    )
                    Text(
                        text = uiModel.temperature,
                        style = MaterialTheme.typography.titleMedium, // Используем стили темы
                        fontWeight = FontWeight.Bold
                    )
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = if (isExpanded) stringResource(R.string.action_collapse) else stringResource(R.string.action_expand),
                    modifier = Modifier.size(dimensionResource(R.dimen.icon_size_standard))
                )
            }

            // --- Раскрытый вид (с анимацией) ---
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = dimensionResource(R.dimen.spacing_medium)),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_extra_small))
                ) {
                    DetailRow(stringResource(R.string.details_weather_description), uiModel.weatherDescription)
                    Spacer(Modifier.height(dimensionResource(R.dimen.spacing_small))) // Доп. отступ
                    DetailRow(stringResource(R.string.details_feels_like), uiModel.feelsLike)
                    DetailRow(stringResource(R.string.details_humidity), uiModel.humidity)
                    DetailRow(stringResource(R.string.details_pressure_hpa_label), uiModel.pressure)
                    DetailRow(stringResource(R.string.details_wind_speed_mps_label), uiModel.windInfo)
                    DetailRow(stringResource(R.string.details_visibility_km_label), uiModel.visibility)
                    DetailRow(stringResource(R.string.details_clouds), uiModel.clouds)
                    DetailRow(stringResource(R.string.details_precipitation_probability), uiModel.precipitationProbability)
                }
            }
        }
    }
}

// Вспомогательный Composable для строки с деталями
@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically // Выравнивание по центру для лучшего вида
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall, // Меньший шрифт для меток
            color = MaterialTheme.colorScheme.onSurfaceVariant // Менее яркий цвет для меток
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium, // Шрифт побольше для значений
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(start = dimensionResource(R.dimen.spacing_small)) // Небольшой отступ слева для значения
        )
    }
}
