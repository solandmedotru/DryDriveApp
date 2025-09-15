package ru.devsoland.drydrive.ui.composables

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import ru.devsoland.drydrive.R
import ru.devsoland.drydrive.RecommendationSlot
import ru.devsoland.drydrive.RecommendationType
import ru.devsoland.drydrive.data.Weather
import ru.devsoland.drydrive.ui.theme.AppAccentBlue
import ru.devsoland.drydrive.ui.theme.TextOnDarkBackground
import ru.devsoland.drydrive.ui.theme.AppBarContentColorLowEmphasis // Или TextOnDarkBackgroundLowEmphasis
import androidx.compose.ui.unit.dp // Можно удалить, если все dp через dimensionResource
import androidx.compose.ui.unit.sp // Можно удалить, если все sp через dimensionResource

@Composable
fun WeatherRecommendationSection(
    weather: Weather?,
    modifier: Modifier = Modifier
) {
    val recommendationSlots = remember { // Теперь здесь НЕТ вызовов stringResource
        listOf(
            RecommendationSlot(
                type = RecommendationType.DRINK_WATER,
                defaultIcon = Icons.Filled.WaterDrop,
                activeIcon = Icons.Filled.WaterDrop,
                defaultTextResId = R.string.rec_drink_water_default,     // << ПЕРЕДАЕМ ID
                activeTextResId = R.string.rec_drink_water_active,       // << ПЕРЕДАЕМ ID
                isActive = false,
                defaultContentDescriptionResId = R.string.rec_drink_water_desc_default, // << ПЕРЕДАЕМ ID
                activeContentDescriptionResId = R.string.rec_drink_water_desc_active     // << ПЕРЕДАЕМ ID
            ),
            RecommendationSlot(
                type = RecommendationType.UV_PROTECTION,
                defaultIcon = Icons.Outlined.WbSunny,
                activeIcon = Icons.Filled.WbSunny,
                defaultTextResId = R.string.rec_uv_protection_default,
                activeTextResId = R.string.rec_uv_protection_active,
                isActive = false,
                defaultContentDescriptionResId = R.string.rec_uv_protection_desc_default,
                activeContentDescriptionResId = R.string.rec_uv_protection_desc_active
            ),
            RecommendationSlot(
                type = RecommendationType.TIRE_CHANGE,
                defaultIcon = Icons.Filled.AcUnit,
                activeIcon = Icons.Filled.AcUnit,
                defaultTextResId = R.string.rec_tire_change_default,
                activeTextResId = R.string.rec_tire_change_active,
                isActive = false,
                defaultContentDescriptionResId = R.string.rec_tire_change_desc_default,
                activeContentDescriptionResId = R.string.rec_tire_change_desc_active
            ),
            RecommendationSlot(
                type = RecommendationType.UMBRELLA,
                defaultIcon = Icons.Filled.Umbrella,
                activeIcon = Icons.Filled.Umbrella,
                defaultTextResId = R.string.rec_umbrella_default,
                activeTextResId = R.string.rec_umbrella_active,
                isActive = false,
                defaultContentDescriptionResId = R.string.rec_umbrella_desc_default,
                activeContentDescriptionResId = R.string.rec_umbrella_desc_active
            )
        )
    }
    val updatedSlots = remember(weather, recommendationSlots) {
        if (weather == null) {
            recommendationSlots.map { it.copy(isActive = false) }
        } else {
            recommendationSlots.map { slot ->
                val isActive = when (slot.type) {
                    RecommendationType.DRINK_WATER -> weather.main.temp > 28
                    RecommendationType.UV_PROTECTION -> weather.weather.any { it.main.contains("Clear", ignoreCase = true) } && weather.main.temp > 15
                    RecommendationType.TIRE_CHANGE -> weather.main.temp < 5
                    RecommendationType.UMBRELLA -> weather.weather.any { it.main.contains("Rain", ignoreCase = true) }
                }
                slot.copy(isActive = isActive)
            }
        }
    }
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        updatedSlots.forEach { slot -> RecommendationChip(slot = slot) }
    }
}

@Composable
fun RecommendationChip(
    slot: RecommendationSlot,
    modifier: Modifier = Modifier
) {
    val accentColor = AppAccentBlue
    val defaultIconColor = AppBarContentColorLowEmphasis // Убедитесь, что этот цвет подходит
    val defaultTextColor = AppBarContentColorLowEmphasis // или TextOnDarkBackgroundLowEmphasis

    val iconToShow = if (slot.isActive) slot.activeIcon else slot.defaultIcon
    val textToShow = stringResource(if (slot.isActive) slot.activeTextResId else slot.defaultTextResId)
    val iconColor = if (slot.isActive) accentColor else defaultIconColor
    val textColor = if (slot.isActive) TextOnDarkBackground else defaultTextColor
    val contentDescription = stringResource(if (slot.isActive) slot.activeContentDescriptionResId else slot.defaultContentDescriptionResId)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.width(IntrinsicSize.Min) // IntrinsicSize.Min хорошо для автоматической ширины
    ) {
        Icon(
            imageVector = iconToShow,
            contentDescription = contentDescription,
            tint = iconColor,
            modifier = Modifier.size(dimensionResource(R.dimen.icon_size_large))
        )
        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_small)))
        Text(
            text = textToShow,
            color = textColor,
            fontSize = dimensionResource(R.dimen.font_size_small_caption).value.sp,
            textAlign = TextAlign.Center,
            fontWeight = if (slot.isActive) FontWeight.Medium else FontWeight.Normal
        )
    }
}