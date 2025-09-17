package ru.devsoland.drydrive.feature_weather.ui

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.* // Keep existing Box, Column, etc.
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import ru.devsoland.drydrive.feature_weather.ui.composables.RecommendationInfoDialog
import ru.devsoland.drydrive.feature_weather.ui.composables.RecommendationsDisplaySection
import ru.devsoland.drydrive.feature_weather.ui.composables.WeatherDetails
// Предполагается, что WeatherDetailsUiModel будет обновлена и будет содержать sunriseEpochMillis и sunsetEpochMillis
import ru.devsoland.drydrive.feature_weather.ui.model.WeatherDetailsUiModel 
import ru.devsoland.drydrive.ui.theme.CityBackgroundOverlay
import java.util.Locale

@Composable
fun WeatherScreen(
    modifier: Modifier = Modifier,
    viewModel: WeatherViewModel
) {
    val uiState by viewModel.uiState.collectAsState()

    WeatherScreenContent(
        modifier = modifier,
        uiState = uiState,
        onEvent = viewModel::onEvent
    )
}

@Composable
fun WeatherScreenContent(
    modifier: Modifier = Modifier,
    uiState: WeatherUiState,
    onEvent: (WeatherEvent) -> Unit
) {
    val weatherConditionDesc = uiState.weather?.weatherConditionDescription?.lowercase(Locale.getDefault()) ?: ""
    val currentTempForWash = uiState.weather?.temperature?.filter { it.isDigit() || it == '-' }?.toIntOrNull()

    val carImageResId = when {
        weatherConditionDesc.contains(stringResource(R.string.condition_rain).lowercase(Locale.getDefault())) ||
        weatherConditionDesc.contains(stringResource(R.string.condition_snow).lowercase(Locale.getDefault())) ||
        weatherConditionDesc.contains(stringResource(R.string.condition_thunderstorm).lowercase(Locale.getDefault())) ||
        weatherConditionDesc.contains(stringResource(R.string.condition_drizzle).lowercase(Locale.getDefault())) ||
        weatherConditionDesc.contains(stringResource(R.string.condition_mist).lowercase(Locale.getDefault())) ||
        weatherConditionDesc.contains(stringResource(R.string.condition_fog).lowercase(Locale.getDefault())) -> R.drawable.car_dirty
        else -> R.drawable.car_clean
    }
    val carContentDescription = if (carImageResId == R.drawable.car_dirty) stringResource(R.string.car_dirty_description) else stringResource(R.string.car_clean_description)

    val washRecommendation = when {
        weatherConditionDesc.contains(stringResource(R.string.condition_rain).lowercase(Locale.getDefault())) ||
        weatherConditionDesc.contains(stringResource(R.string.condition_snow).lowercase(Locale.getDefault())) ||
        weatherConditionDesc.contains(stringResource(R.string.condition_thunderstorm).lowercase(Locale.getDefault())) ||
        weatherConditionDesc.contains(stringResource(R.string.condition_drizzle).lowercase(Locale.getDefault())) -> stringResource(R.string.wash_not_recommended)
        weatherConditionDesc.contains(stringResource(R.string.condition_mist).lowercase(Locale.getDefault())) ||
        weatherConditionDesc.contains(stringResource(R.string.condition_fog).lowercase(Locale.getDefault())) -> stringResource(R.string.wash_possible_limited_visibility)
        uiState.weather != null && currentTempForWash != null && currentTempForWash > 5 -> stringResource(R.string.wash_great_day)
        uiState.weather != null && currentTempForWash != null -> stringResource(R.string.wash_good_weather_but_cool)
        else -> ""
    }

    if (uiState.showRecommendationDialog) {
        val title = uiState.recommendationDialogTitleResId
        val description = uiState.recommendationDialogDescriptionResId
        if (title != null && description != null) {
            RecommendationInfoDialog(
                titleResId = title,
                descriptionResId = description,
                onDismiss = { onEvent(WeatherEvent.DismissRecommendationDialog) }
            )
        } else {
            Log.w("WeatherScreenContent", "RecommendationInfoDialog not shown: titleResId or descriptionResId is null.")
        }
    }

    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.background)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()) // Allow content to scroll if it overflows
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f / 1.1f)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.city_background),
                        contentDescription = stringResource(R.string.city_background_description),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(CityBackgroundOverlay)
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = dimensionResource(R.dimen.spacing_xlarge))
                    ) {
                        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_large)))
                        when {
                            uiState.isLoadingWeather && uiState.weather == null -> CircularProgressIndicator(
                                modifier = Modifier
                                    .align(Alignment.CenterHorizontally)
                                    .padding(vertical = 50.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                            uiState.weatherErrorMessage != null && uiState.weather == null -> Text(
                                text = uiState.weatherErrorMessage,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 50.dp)
                            )
                            uiState.weather != null -> {
                                WeatherDetails(weatherDetails = uiState.weather)
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
                                    .padding(vertical = 50.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = stringResource(R.string.select_city_prompt),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                        Spacer(modifier = Modifier.weight(1f)) // Pushes car and progress bar down
                        
                        val weatherData = uiState.weather
                        if (weatherData != null) {
                            Image(
                                painter = painterResource(id = carImageResId),
                                contentDescription = carContentDescription,
                                modifier = Modifier
                                    .fillMaxWidth(0.8f)
                                    .aspectRatio(16f / 9f)
                                    .align(Alignment.CenterHorizontally),
                                contentScale = ContentScale.Fit
                            )
                            // Check if sunrise and sunset data is available in WeatherDetailsUiModel
                            if (weatherData.sunriseEpochMillis != null && weatherData.sunsetEpochMillis != null) {
                                DaylightProgressBar(
                                    sunriseEpochMillis = weatherData.sunriseEpochMillis,
                                    sunsetEpochMillis = weatherData.sunsetEpochMillis,
                                    currentTimeMillis = System.currentTimeMillis(),
                                    modifier = Modifier
                                        .fillMaxWidth(0.75f) // Match car width or slightly less
                                        .height(dimensionResource(R.dimen.daylight_progress_bar_height)) // Create this dimen, e.g., 6.dp or 8.dp
                                        .align(Alignment.CenterHorizontally)
                                        .padding(top = dimensionResource(R.dimen.spacing_tiny)) // Small space from car
                                )
                            }
                             Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_large))) // Bottom padding for car section
                        }
                    }
                }
            }
        }

        // Нижняя часть экрана с прогнозом по дням и рекомендациями (остается без изменений)
        if (uiState.recommendations.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimensionResource(R.dimen.spacing_xlarge))
                    .padding(top = dimensionResource(R.dimen.spacing_medium))
            ) {
                RecommendationsDisplaySection(
                    recommendations = uiState.recommendations,
                    onEvent = onEvent
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimensionResource(R.dimen.spacing_xlarge))
                .padding(
                    top = dimensionResource(R.dimen.spacing_medium),
                    bottom = dimensionResource(R.dimen.spacing_small)
                )
        ) {
            val dailyForecastRowHeight = dimensionResource(R.dimen.daily_forecast_row_height)
            when {
                uiState.isLoadingForecast && uiState.dailyForecasts.isEmpty() -> Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(dailyForecastRowHeight),
                    contentAlignment = Alignment.Center
                ) {
                    DailyForecastPlaceholder()
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, strokeWidth = 2.dp)
                }
                uiState.forecastErrorMessage != null && uiState.dailyForecasts.isEmpty() -> Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(dailyForecastRowHeight),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.forecast_unavailable),
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
                uiState.dailyForecasts.isNotEmpty() -> {
                    DailyForecastRow(forecastItems = uiState.dailyForecasts)
                }
                uiState.weather == null && !uiState.isLoadingForecast && uiState.forecastErrorMessage == null -> {
                     Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(dailyForecastRowHeight),
                        contentAlignment = Alignment.Center
                    ) {
                        DailyForecastPlaceholder()
                    }
                }
            }
        }
    }
}

@Composable
fun DaylightProgressBar(
    sunriseEpochMillis: Long?,
    sunsetEpochMillis: Long?,
    currentTimeMillis: Long,
    modifier: Modifier = Modifier,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), // Цвет фона полосы
    progressColor: Color = MaterialTheme.colorScheme.primary // Цвет заполнения прогресса
) {
    if (sunriseEpochMillis == null || sunsetEpochMillis == null || sunriseEpochMillis >= sunsetEpochMillis) {
        // Нечего отображать, если данные некорректны или отсутствуют
        Spacer(modifier = modifier.height(0.dp)) // Занимаем место, но невидимы, или можно Box(modifier)
        return
    }

    val totalDaylightDuration = remember(sunriseEpochMillis, sunsetEpochMillis) {
        (sunsetEpochMillis - sunriseEpochMillis).coerceAtLeast(1) // Ensure no division by zero or negative
    }
    val elapsedDaylight = remember(currentTimeMillis, sunriseEpochMillis) {
        currentTimeMillis - sunriseEpochMillis
    }
    val progressRatio = remember(elapsedDaylight, totalDaylightDuration) {
        (elapsedDaylight.toFloat() / totalDaylightDuration.toFloat()).coerceIn(0f, 1f)
    }

    BoxWithConstraints(modifier = modifier) {
        val barHeight = this.maxHeight
        val barWidth = this.maxWidth

        // Фон (трек)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeight)
                .clip(RoundedCornerShape(barHeight / 2)) // Скругленные края
                .background(trackColor)
        )
        // Заполнение (прогресс)
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(barWidth * progressRatio)
                .clip(RoundedCornerShape(barHeight / 2))
                .background(progressColor)
        )
    }
}

// TODO: Не забудьте добавить dimension `daylight_progress_bar_height` в ваш файл dimens.xml
// например, <dimen name="daylight_progress_bar_height">6dp</dimen>

// TODO: Убедитесь, что WeatherDetailsUiModel содержит поля:
// val sunriseEpochMillis: Long?
// val sunsetEpochMillis: Long?
// и что они корректно заполняются в маппере.
