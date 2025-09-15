package ru.devsoland.drydrive.ui.composables

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
// NEW: Удаляем прямой импорт Color
// import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
// import androidx.compose.ui.unit.dp // Можно удалить, если все dp через dimensionResource
// import androidx.compose.ui.unit.sp // Можно удалить, если все sp через dimensionResource
import ru.devsoland.drydrive.R // NEW: Импорт для R.string, R.dimen
import ru.devsoland.drydrive.ui.DisplayDayWeather
// NEW: Импорты для ваших кастомных цветов из Color.kt
import ru.devsoland.drydrive.ui.theme.AppAccentBlue
import ru.devsoland.drydrive.ui.theme.AppCardBackgroundDark
import ru.devsoland.drydrive.ui.theme.TextOnDarkBackground // Предполагается, что это Color.White
// NEW: Импорт для AppDimens, если используете Kotlin-константы для размеров
// import ru.devsoland.drydrive.ui.theme.AppDimens

@Composable
fun DailyForecastRow(
    forecasts: List<DisplayDayWeather>,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_medium_large)), // Например, 12dp
        contentPadding = PaddingValues(horizontal = dimensionResource(R.dimen.padding_none)) // Или 0.dp, если нет такого ресурса
    ) {
        itemsIndexed(forecasts) { index, dayWeather ->
            ForecastDayCard(
                dayWeather = dayWeather,
                // NEW: Используем строку из ресурсов для "Сейчас"
                isCurrentDay = dayWeather.dayShort == stringResource(R.string.forecast_now) || index == 0
            )
        }
    }
}

@Composable
fun ForecastDayCard(
    dayWeather: DisplayDayWeather,
    isCurrentDay: Boolean,
    modifier: Modifier = Modifier
) {
    // NEW: Используем цвета из Color.kt
    val backgroundColor = if (isCurrentDay) AppAccentBlue else AppCardBackgroundDark
    val contentColor = TextOnDarkBackground // Используем общий цвет текста на темном фоне

    Card(
        shape = RoundedCornerShape(dimensionResource(R.dimen.corner_radius_medium)), // Например, 12dp
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        modifier = modifier
            .width(dimensionResource(R.dimen.forecast_card_width)) // Например, 80dp
            .height(dimensionResource(R.dimen.forecast_card_height)) // Например, 120dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    vertical = dimensionResource(R.dimen.spacing_large), // Например, 16dp
                    horizontal = dimensionResource(R.dimen.spacing_medium) // Например, 8dp
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = dayWeather.dayShort,
                style = TextStyle(
                    // NEW: Используем цвет и размер из ресурсов/констант
                    color = contentColor,
                    fontSize = dimensionResource(R.dimen.font_size_body).value.sp,
                    // fontSize = AppDimens.FontSizeBody, // Альтернатива
                    fontWeight = FontWeight.Medium
                )
            )
            Icon(
                painter = painterResource(id = dayWeather.iconRes),
                // NEW: Можно добавить осмысленное описание, если dayWeather содержит информацию для этого
                contentDescription = stringResource(R.string.forecast_icon_description_template, dayWeather.dayShort),
                tint = contentColor,
                modifier = Modifier.size(dimensionResource(R.dimen.icon_size_large))
                // modifier = Modifier.size(AppDimens.IconSizeLarge) // Альтернатива
            )
            Text(
                text = dayWeather.temperature,
                style = TextStyle(
                    // NEW: Используем цвет и размер из ресурсов/констант
                    color = contentColor,
                    fontSize = dimensionResource(R.dimen.font_size_title).value.sp, // Например, 20sp
                    // fontSize = AppDimens.FontSizeLargeEmphasis, // Альтернатива
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

