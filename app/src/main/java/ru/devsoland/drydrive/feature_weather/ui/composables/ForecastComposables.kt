package ru.devsoland.drydrive.feature_weather.ui.composables

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme // ИМПОРТ ДЛЯ ДОСТУПА К ЦВЕТАМ ТЕМЫ
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
// import androidx.compose.ui.graphics.Color // Удаляем, если не используется для кастомных цветов
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import ru.devsoland.drydrive.feature_weather.ui.DisplayDayWeather
// import androidx.compose.ui.unit.dp // Можно удалить, если все dp через dimensionResource
import ru.devsoland.drydrive.R

@Composable
fun DailyForecastRow(
    forecasts: List<DisplayDayWeather>,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_medium_large)),
        contentPadding = PaddingValues(horizontal = dimensionResource(R.dimen.padding_none)) // Используйте padding_none или 0.dp, если нет такого ресурса
    ) {
        itemsIndexed(forecasts) { index, dayWeather ->
            ForecastDayCard(
                dayWeather = dayWeather,
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
    // Используем цвета из MaterialTheme.colorScheme
    val cardContainerColor = if (isCurrentDay) {
        MaterialTheme.colorScheme.primaryContainer // Акцентный фон для текущего дня
    } else {
        MaterialTheme.colorScheme.surfaceVariant   // Менее выделяющийся фон для остальных дней
    }

    val cardContentColor = if (isCurrentDay) {
        MaterialTheme.colorScheme.onPrimaryContainer // Цвет текста/иконок на акцентном фоне
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant   // Цвет текста/иконок на обычном фоне карточки
    }

    Card(
        shape = RoundedCornerShape(dimensionResource(R.dimen.corner_radius_medium)),
        colors = CardDefaults.cardColors(
            containerColor = cardContainerColor,
            contentColor = cardContentColor // Устанавливаем цвет контента по умолчанию для Card
        ),
        modifier = modifier
            .width(dimensionResource(R.dimen.forecast_card_width))
            .height(dimensionResource(R.dimen.forecast_card_height))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    vertical = dimensionResource(R.dimen.spacing_large),
                    horizontal = dimensionResource(R.dimen.spacing_medium)
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = dayWeather.dayShort,
                // color не нужен, он будет унаследован от CardDefaults.contentColor
                style = TextStyle( // Можно заменить на MaterialTheme.typography.labelLarge или bodyMedium
                    fontSize = dimensionResource(R.dimen.font_size_body).value.sp,
                    fontWeight = FontWeight.Medium
                )
            )
            Icon(
                painter = painterResource(id = dayWeather.iconRes),
                contentDescription = stringResource(R.string.forecast_icon_description_template, dayWeather.dayShort),
                // tint не нужен, он будет унаследован от CardDefaults.contentColor
                modifier = Modifier.size(dimensionResource(R.dimen.icon_size_large))
            )
            Text(
                text = dayWeather.temperature,
                // color не нужен, он будет унаследован от CardDefaults.contentColor
                style = TextStyle( // Можно заменить на MaterialTheme.typography.titleMedium или bodyLarge
                    fontSize = dimensionResource(R.dimen.font_size_title).value.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}
