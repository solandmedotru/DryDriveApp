package ru.devsoland.drydrive.ui.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
// NEW: Удаляем прямой импорт Color, если все цвета будут из темы/Color.kt
// import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp // Оставляем для специфичных отступов, если не вынесены
import androidx.compose.ui.unit.sp // Оставляем для TextStyle, если размеры шрифтов не все вынесены
import ru.devsoland.drydrive.R // NEW: Импорт для R.string, R.dimen
import ru.devsoland.drydrive.data.Weather
// NEW: Импорты для ваших кастомных цветов из Color.kt
import ru.devsoland.drydrive.ui.theme.TextOnDarkBackground
// NEW: Импорт для AppDimens, если используете Kotlin-константы для размеров
// import ru.devsoland.drydrive.ui.theme.AppDimens
import java.util.Locale

@Composable
fun WeatherDetails(
    weather: Weather,
    modifier: Modifier = Modifier
) {
    val weatherDescription = weather.weather.getOrNull(0)?.description?.replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
    } ?: stringResource(R.string.weather_data_unavailable) // NEW: Используем строку из ресурсов

    Column(
        horizontalAlignment = Alignment.Start,
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = "${weather.main.temp.toInt()}°C",
            style = TextStyle(
                // NEW: Используем цвет и размер из ресурсов/констант
                color = TextOnDarkBackground,
                fontSize = dimensionResource(R.dimen.font_size_large_title).value.sp,
                fontWeight = FontWeight.Bold
            )
        )
        Text(
            text = weatherDescription,
            style = TextStyle(
                // NEW: Используем цвет и размер из ресурсов/констант
                color = TextOnDarkBackground,
                fontSize = dimensionResource(R.dimen.font_size_title).value.sp,
                fontWeight = FontWeight.Normal
            ),
            modifier = Modifier.padding(
                top = 0.dp, // Можно оставить 0.dp или вынести, если это стандартный "нулевой" отступ
                bottom = dimensionResource(R.dimen.spacing_medium) // NEW: Используем размер из ресурсов
            )
        )
    }
}

