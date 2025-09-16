package ru.devsoland.drydrive.feature_weather.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.devsoland.drydrive.R
import ru.devsoland.drydrive.feature_weather.ui.composables.DailyForecastPlaceholder
import ru.devsoland.drydrive.feature_weather.ui.composables.DailyForecastRow
import ru.devsoland.drydrive.feature_weather.ui.composables.RecommendationsDisplaySection
import ru.devsoland.drydrive.feature_weather.ui.composables.WeatherDetails
import ru.devsoland.drydrive.ui.theme.CityBackgroundOverlay
import java.util.Locale

@Composable
fun HomeScreenContent(modifier: Modifier = Modifier, uiState: WeatherUiState) {
    val carImageResId = when (uiState.weather?.weather?.getOrNull(0)?.main?.lowercase(Locale.ROOT)) {
        "rain", "snow", "thunderstorm", "drizzle", "mist", "fog" -> R.drawable.car_dirty
        else -> R.drawable.car_clean
    }
    val carContentDescription = if (carImageResId == R.drawable.car_dirty) stringResource(R.string.car_dirty_description) else stringResource(R.string.car_clean_description)
    val washRecommendation = when (uiState.weather?.weather?.getOrNull(0)?.main?.lowercase(Locale.ROOT)) {
        "rain", "snow", "thunderstorm", "drizzle" -> stringResource(R.string.wash_not_recommended)
        "mist", "fog" -> stringResource(R.string.wash_possible_limited_visibility)
        else -> if (uiState.weather != null && uiState.weather.main.temp > 5) stringResource(R.string.wash_great_day)
        else if (uiState.weather != null) stringResource(R.string.wash_good_weather_but_cool)
        else ""
    }

    Box(modifier = modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)) {
        Column(modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()) // Добавил вертикальный скролл для всего контента
        ) {
            // Секция с фоном города и текущей погодой/машиной
            Box(modifier = Modifier
                .fillMaxWidth()
                // .weight(1f) // weight не очень хорошо работает с verticalScroll, лучше задавать высоты явно или использовать minHeight
                .aspectRatio(1f / 1.1f) // Примерное соотношение для этой секции, подберите по вкусу
            ) {
                Image(
                    painter = painterResource(id = R.drawable.city_background),
                    contentDescription = stringResource(R.string.city_background_description),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(modifier = Modifier
                    .fillMaxSize()
                    .background(CityBackgroundOverlay))

                Column(modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = dimensionResource(R.dimen.spacing_xlarge))) {
                    Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_large)))
                    when {
                        uiState.isLoadingWeather && uiState.weather == null -> CircularProgressIndicator(
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(vertical = 50.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                        uiState.weatherErrorMessage != null && uiState.weather == null -> Text(
                            text = uiState.weatherErrorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 50.dp)
                        )
                        uiState.weather != null -> {
                            WeatherDetails(weather = uiState.weather)
                            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_medium)))
                            if (washRecommendation.isNotBlank()) {
                                Text(
                                    text = washRecommendation,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = dimensionResource(R.dimen.font_size_caption).value.sp,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                        else -> Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp), // Задаем высоту, чтобы Spacer.weight работал корректно
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = stringResource(R.string.select_city_prompt),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 50.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.weight(1f)) // Занимает оставшееся место, чтобы машина была внизу
                    if (uiState.weather != null) {
                        Image(
                            painter = painterResource(id = carImageResId),
                            contentDescription = carContentDescription,
                            modifier = Modifier
                                .fillMaxWidth(0.8f)
                                .aspectRatio(16f / 9f)
                                .align(Alignment.CenterHorizontally)
                                .padding(bottom = dimensionResource(R.dimen.spacing_large)),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }

            // Секция рекомендаций
            Column(modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimensionResource(R.dimen.spacing_xlarge))
                .padding(top = dimensionResource(R.dimen.spacing_large))) {
                // ИСПРАВЛЕННЫЙ ВЫЗОВ:
                RecommendationsDisplaySection(recommendations = uiState.recommendations)
            }

            // Секция прогноза
            Column(modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimensionResource(R.dimen.spacing_xlarge))
                .padding(
                    top = dimensionResource(R.dimen.spacing_large),
                    bottom = dimensionResource(R.dimen.spacing_large)
                )) {
                when {
                    uiState.isLoadingForecast && uiState.dailyForecasts.isEmpty() -> Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp), // Задаем высоту для плейсхолдера
                        contentAlignment = Alignment.Center
                    ) {
                        DailyForecastPlaceholder() // Можно оставить или убрать, если CircularProgressIndicator достаточно
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, strokeWidth = 2.dp)
                    }
                    uiState.forecastErrorMessage != null && uiState.dailyForecasts.isEmpty() -> Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.forecast_unavailable),
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                    uiState.dailyForecasts.isNotEmpty() -> {
                        DailyForecastRow(forecasts = uiState.dailyForecasts)
                    }
                    else -> { // Если нет ни загрузки, ни ошибки, ни данных - показываем плейсхолдер
                        DailyForecastPlaceholder()
                    }
                }
            }
        }
    }
}