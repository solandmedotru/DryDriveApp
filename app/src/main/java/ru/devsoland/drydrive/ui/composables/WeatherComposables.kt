package ru.devsoland.drydrive.ui.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme // ИМПОРТ ДЛЯ ДОСТУПА К ЦВЕТАМ ТЕМЫ
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
// import androidx.compose.ui.graphics.Color // Удаляем прямой импорт Color, если не используется для кастомных цветов
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp // Оставляем для специфичных отступов, если не вынесены в AppDimens
import androidx.compose.ui.unit.sp // Оставляем для TextStyle, если размеры шрифтов не все вынесены в AppDimens
import ru.devsoland.drydrive.R
import ru.devsoland.drydrive.data.Weather
// import ru.devsoland.drydrive.ui.theme.TextOnDarkBackground // УДАЛЯЕМ ИМПОРТ КАСТОМНОГО ЦВЕТА
// import ru.devsoland.drydrive.ui.theme.AppDimens // Если у вас есть такой файл для размеров
import java.util.Locale

@Composable
fun WeatherDetails(
    weather: Weather,
    modifier: Modifier = Modifier
) {
    val weatherDescription = weather.weather.getOrNull(0)?.description?.replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
    } ?: stringResource(R.string.weather_data_unavailable)

    Column(
        horizontalAlignment = Alignment.Start,
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = "${weather.main.temp.toInt()}°C",
            style = TextStyle(
                // ИСПОЛЬЗУЕМ ЦВЕТ ИЗ ТЕКУЩЕЙ ТЕМЫ
                color = MaterialTheme.colorScheme.onSurface, // Или onBackground, в зависимости от того, на какой поверхности этот текст
                fontSize = dimensionResource(R.dimen.font_size_large_title).value.sp,
                fontWeight = FontWeight.Bold
            )
        )
        Text(
            text = weatherDescription,
            style = TextStyle(
                // ИСПОЛЬЗУЕМ ЦВЕТ ИЗ ТЕКУЩЕЙ ТЕМЫ
                color = MaterialTheme.colorScheme.onSurfaceVariant, // Для менее важного текста
                fontSize = dimensionResource(R.dimen.font_size_title).value.sp,
                fontWeight = FontWeight.Normal
            ),
            modifier = Modifier.padding(
                top = 0.dp, // Можно оставить или вынести в AppDimens
                bottom = dimensionResource(R.dimen.spacing_medium)
            )
        )
    }
}