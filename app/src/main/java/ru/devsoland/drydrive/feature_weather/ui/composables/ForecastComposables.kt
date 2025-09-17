package ru.devsoland.drydrive.feature_weather.ui.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air // Ветер
import androidx.compose.material.icons.filled.Cloud // Облачность
import androidx.compose.material.icons.filled.Compress // Давление
import androidx.compose.material.icons.filled.Grain // Осадки
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Opacity // Влажность (альтернатива WaterDrop)
import androidx.compose.material.icons.filled.Thermostat // Ощущается как
import androidx.compose.material.icons.filled.Visibility // Видимость
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ru.devsoland.drydrive.R
import ru.devsoland.drydrive.feature_weather.ui.model.ForecastCardUiModel

@Composable
fun DailyForecastRow(
    forecastItems: List<ForecastCardUiModel>,
    modifier: Modifier = Modifier
) {
    if (forecastItems.isEmpty()) {
        return
    }
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_medium)),
        contentPadding = PaddingValues(horizontal = dimensionResource(R.dimen.padding_medium))
    ) {
        items(
            items = forecastItems,
            key = { item -> item.id }
        ) { uiModel ->
            ForecastDayCard(
                uiModel = uiModel
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
        MaterialTheme.colorScheme.surface // Используем surface для обычной карточки, как в новой теме
    }
    val cardContentColor = if (uiModel.isHighlighted) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface // Соответственно onSurface
    }

    val cardWidthModifier = if (isExpanded) {
        Modifier.fillMaxWidth()
    } else {
        Modifier.width(dimensionResource(R.dimen.forecast_card_width_expanded))
    }

    Card(
        shape = RoundedCornerShape(dimensionResource(R.dimen.corner_radius_large)),
        colors = CardDefaults.cardColors(
            containerColor = cardContainerColor,
            contentColor = cardContentColor
        ),
        modifier = modifier
            .then(cardWidthModifier)
            .animateContentSize()
            .clickable { isExpanded = !isExpanded }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(R.dimen.spacing_medium)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small)),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = uiModel.dayShort,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = cardContentColor
                    )
                    Icon(
                        painter = painterResource(id = uiModel.iconRes),
                        contentDescription = stringResource(R.string.forecast_icon_description_template, uiModel.dayShort),
                        modifier = Modifier.size(dimensionResource(R.dimen.icon_size_medium_large)),
                        tint = cardContentColor
                    )
                    Text(
                        text = uiModel.temperature,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = cardContentColor
                    )
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = if (isExpanded) stringResource(R.string.action_collapse) else stringResource(R.string.action_expand),
                    modifier = Modifier.size(dimensionResource(R.dimen.icon_size_standard)),
                    tint = cardContentColor.copy(alpha = 0.7f)
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = dimensionResource(R.dimen.spacing_large))
                ) {
                    Text(
                        text = uiModel.weatherDescription,
                        style = MaterialTheme.typography.bodyMedium,
                        color = cardContentColor,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = dimensionResource(R.dimen.spacing_large)) // Увеличил отступ под описанием
                    )

                    // Детали в два столбца
                    Row(Modifier.fillMaxWidth()) { // Убрано Arrangement.SpaceAround
                        DetailItem(
                            icon = Icons.Filled.Thermostat,
                            label = stringResource(R.string.details_feels_like_short),
                            value = uiModel.feelsLike,
                            contentColor = cardContentColor,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(dimensionResource(R.dimen.spacing_small))) // Небольшой разделитель между колонками
                        DetailItem(
                            icon = Icons.Filled.Opacity,
                            label = stringResource(R.string.details_humidity_short),
                            value = uiModel.humidity,
                            contentColor = cardContentColor,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(dimensionResource(R.dimen.spacing_medium)))
                    Row(Modifier.fillMaxWidth()) { // Убрано Arrangement.SpaceAround
                        DetailItem(
                            icon = Icons.Filled.Air,
                            label = stringResource(R.string.details_wind_label_short),
                            value = uiModel.windInfo,
                            contentColor = cardContentColor,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(dimensionResource(R.dimen.spacing_small)))
                        DetailItem(
                            icon = Icons.Filled.Compress,
                            label = stringResource(R.string.details_pressure_label_short),
                            value = uiModel.pressure,
                            contentColor = cardContentColor,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(dimensionResource(R.dimen.spacing_medium)))
                    Row(Modifier.fillMaxWidth()) { // Убрано Arrangement.SpaceAround
                        DetailItem(
                            icon = Icons.Filled.Visibility,
                            label = stringResource(R.string.details_visibility_label_short),
                            value = uiModel.visibility,
                            contentColor = cardContentColor,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(dimensionResource(R.dimen.spacing_small)))
                        DetailItem(
                            icon = Icons.Filled.Cloud,
                            label = stringResource(R.string.details_clouds_short),
                            value = uiModel.clouds,
                            contentColor = cardContentColor,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(dimensionResource(R.dimen.spacing_medium)))
                     Row(Modifier.fillMaxWidth()) { // Убрано Arrangement.SpaceAround
                         DetailItem(
                            icon = Icons.Filled.Grain,
                            label = stringResource(R.string.details_precipitation_short),
                            value = uiModel.precipitationProbability,
                            contentColor = cardContentColor,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(dimensionResource(R.dimen.spacing_small)))
                        Box(modifier = Modifier.weight(1f)) // Пустой Box для выравнивания, если элементов нечетное количество
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailItem(
    icon: ImageVector,
    label: String,
    value: String,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier.padding(
            vertical = dimensionResource(R.dimen.spacing_small),
            horizontal = dimensionResource(R.dimen.spacing_small) // Добавлены горизонтальные отступы
        )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = contentColor.copy(alpha = 0.8f),
            modifier = Modifier.size(dimensionResource(R.dimen.icon_size_medium))
        )
        Spacer(Modifier.height(dimensionResource(R.dimen.spacing_extra_small)))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = contentColor,
            textAlign = TextAlign.Center
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = contentColor.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    }
}

// Убедитесь, что строки для коротких меток (например, R.string.details_feels_like_short) 
// добавлены в ваш файл strings.xml.
