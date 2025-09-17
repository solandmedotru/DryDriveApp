package ru.devsoland.drydrive.feature_weather.ui.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
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

    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val horizontalListPadding = dimensionResource(R.dimen.spacing_large)
    val spacingBetweenCards = dimensionResource(R.dimen.spacing_medium)
    val calculatedCardWidth = (screenWidth - (horizontalListPadding * 2) - (spacingBetweenCards * 2)) / 3

    var showDetailsDialogFor by remember { mutableStateOf<ForecastCardUiModel?>(null) }

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacingBetweenCards),
        contentPadding = PaddingValues(horizontal = horizontalListPadding)
    ) {
        items(
            items = forecastItems,
            key = { item -> item.id }
        ) { uiModel ->
            ForecastDayCard(
                uiModel = uiModel,
                cardWidth = calculatedCardWidth,
                onCardClick = { showDetailsDialogFor = uiModel }
            )
        }
    }

    showDetailsDialogFor?.let { itemToShow ->
        Dialog(onDismissRequest = { showDetailsDialogFor = null }) {
            DetailsDialogContent(
                uiModel = itemToShow
            )
        }
    }
}

@Composable
fun ForecastDayCard(
    uiModel: ForecastCardUiModel,
    cardWidth: Dp,
    onCardClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(dimensionResource(R.dimen.corner_radius_large)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        modifier = modifier
            .width(cardWidth)
            .clickable { onCardClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth() // Занимает всю ширину предоставленной cardWidth
                .padding(dimensionResource(R.dimen.spacing_medium)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small))
        ) {
            Text(
                text = uiModel.dayShort,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            Icon(
                painter = painterResource(id = uiModel.iconRes),
                contentDescription = stringResource(R.string.forecast_icon_description_template, uiModel.dayShort),
                modifier = Modifier.size(dimensionResource(R.dimen.icon_size_medium_large))
            )
            Text(
                text = "${uiModel.tempMinStr} / ${uiModel.tempMaxStr}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun DetailsDialogContent(
    uiModel: ForecastCardUiModel,
    modifier: Modifier = Modifier
) {
    // Используем Surface или Card для фона диалога и отступов/размеров
    Card(
        modifier = modifier
            .fillMaxWidth(0.95f) // Небольшой отступ от краев по ширине
            .wrapContentHeight(), // Высота по содержимому
        shape = RoundedCornerShape(dimensionResource(R.dimen.corner_radius_xlarge)), // Более скругленные углы для диалога
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh, // Другой цвет для диалога
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(R.dimen.spacing_large)) // Внутренние отступы в диалоге
        ) {
            Text(
                text = uiModel.weatherDescription,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = dimensionResource(R.dimen.spacing_large))
            )
            Row(Modifier.fillMaxWidth()) {
                DetailItem(
                    icon = Icons.Filled.Thermostat,
                    label = stringResource(R.string.details_feels_like),
                    value = uiModel.feelsLike,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(dimensionResource(R.dimen.spacing_small)))
                DetailItem(
                    icon = Icons.Filled.Opacity,
                    label = stringResource(R.string.details_humidity_label),
                    value = uiModel.humidity,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(dimensionResource(R.dimen.spacing_medium)))
            Row(Modifier.fillMaxWidth()) {
                DetailItem(
                    icon = Icons.Filled.Air,
                    label = stringResource(R.string.details_wind_label),
                    value = uiModel.windInfo,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(dimensionResource(R.dimen.spacing_small)))
                DetailItem(
                    icon = Icons.Filled.Compress,
                    label = stringResource(R.string.details_pressure_label),
                    value = uiModel.pressure,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(dimensionResource(R.dimen.spacing_medium)))
            Row(Modifier.fillMaxWidth()) {
                DetailItem(
                    icon = Icons.Filled.Visibility,
                    label = stringResource(R.string.details_visibility_label),
                    value = uiModel.visibility,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(dimensionResource(R.dimen.spacing_small)))
                DetailItem(
                    icon = Icons.Filled.Cloud,
                    label = stringResource(R.string.details_clouds_label),
                    value = uiModel.clouds,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(dimensionResource(R.dimen.spacing_medium)))
            Row(Modifier.fillMaxWidth()) {
                DetailItem(
                    icon = Icons.Filled.Grain,
                    label = stringResource(R.string.details_precipitation),
                    value = uiModel.precipitationProbability,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(dimensionResource(R.dimen.spacing_small)))
                Box(modifier = Modifier.weight(1f))
            }

            // Используем поля из uiModel для Восхода и Заката
            // Убедитесь, что uiModel.sunriseTimeStr и uiModel.sunsetTimeStr существуют и заполнены
            if (uiModel.sunriseTimeStr != null && uiModel.sunsetTimeStr != null) {
                Spacer(Modifier.height(dimensionResource(R.dimen.spacing_medium)))
                Row(Modifier.fillMaxWidth()) {
                    DetailItem(
                        icon = Icons.Filled.WbSunny,
                        label = stringResource(R.string.details_sunrise_label),
                        value = uiModel.sunriseTimeStr!!, // !! так как проверили на null
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(dimensionResource(R.dimen.spacing_small)))
                    DetailItem(
                        icon = Icons.Filled.NightsStay,
                        label = stringResource(R.string.details_sunset_label),
                        value = uiModel.sunsetTimeStr!!, // !! так как проверили на null
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
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
    Row(
        modifier = modifier.padding(vertical = dimensionResource(R.dimen.spacing_extra_small)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            // tint = contentColor.copy(alpha = 0.8f), // Можно убрать alpha, если цвет уже контрастный
            tint = contentColor, 
            modifier = Modifier.size(dimensionResource(R.dimen.icon_size_large))
        )
        Spacer(Modifier.width(dimensionResource(R.dimen.spacing_medium)))
        Column(verticalArrangement = Arrangement.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = contentColor.copy(alpha = 0.7f) // Метка может быть менее контрастной
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
        }
    }
}

// TODO: Добавить строки в strings.xml:
// <string name="details_sunrise_label">Восход</string>
// <string name="details_sunset_label">Заход</string>

// НАПОМИНАНИЕ: Для отображения Восхода/Заката:
// 1. Добавьте поля sunriseTimeStr: String?, sunsetTimeStr: String? в ForecastCardUiModel.kt
// 2. Обновите мапперы для заполнения этих полей.
