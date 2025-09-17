package ru.devsoland.drydrive.feature_weather.ui.composables

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.devsoland.drydrive.R
import ru.devsoland.drydrive.feature_weather.ui.model.WeatherDetailsUiModel // <--- ИЗМЕНЕНО: Импорт новой UI модели

@Composable
fun WeatherDetails(
    weatherDetails: WeatherDetailsUiModel, // <--- ИЗМЕНЕНО: Принимаем WeatherDetailsUiModel
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.Start,
        modifier = modifier.fillMaxWidth()
    ) {
        // Отображение города (можно сделать крупнее и жирнее)
        Text(
            text = weatherDetails.cityName,
            style = TextStyle(
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = dimensionResource(R.dimen.font_size_large_title).value.sp, // Крупный шрифт для города
                fontWeight = FontWeight.Bold
            ),
            modifier = Modifier.padding(bottom = dimensionResource(R.dimen.spacing_small))
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(id = weatherDetails.weatherIconRes),
                contentDescription = weatherDetails.weatherConditionDescription,
                modifier = Modifier
                    .size(dimensionResource(R.dimen.icon_size_very_large)) // Увеличим иконку
                    .padding(end = dimensionResource(R.dimen.spacing_medium))
            )
            Column {
                Text(
                    text = weatherDetails.temperature, // Уже отформатировано, например "10°"
                    style = TextStyle(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = dimensionResource(R.dimen.font_size_huge_title).value.sp, // Очень крупный шрифт для температуры
                        fontWeight = FontWeight.ExtraBold // Сделаем еще жирнее
                    )
                )
                Text(
                    text = weatherDetails.weatherConditionDescription, // Уже отформатировано
                    style = TextStyle(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = dimensionResource(R.dimen.font_size_title).value.sp,
                        fontWeight = FontWeight.Normal
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_medium)))

        // Дополнительные детали в несколько строк или в Grid
        Text(
            text = weatherDetails.feelsLike, // Например, "Ощущается как: 8°"
            style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
        )
        Text(
            text = weatherDetails.tempMinMax, // Например, "Мин: 5° Макс: 12°"
            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
        )
        Text(
            text = "${stringResource(R.string.details_wind_label)}: ${weatherDetails.windSpeed}",
            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
        )
        Text(
            text = "${stringResource(R.string.details_humidity_label)}: ${weatherDetails.humidity}",
            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
        )
        Text(
            text = "${stringResource(R.string.details_pressure_label)}: ${weatherDetails.pressure}",
            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
        )
        Text(
            text = "${stringResource(R.string.details_visibility_label)}: ${weatherDetails.visibility}",
            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
        )
        Text(
            text = "${stringResource(R.string.details_clouds_label)}: ${weatherDetails.cloudiness}",
            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
        )
        Text(
            text = "${stringResource(R.string.details_sunrise_label)}: ${weatherDetails.sunrise}",
            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
        )
        Text(
            text = "${stringResource(R.string.details_sunset_label)}: ${weatherDetails.sunset}",
            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
        )
        // Добавьте сюда другие поля из WeatherDetailsUiModel по необходимости
    }
}
