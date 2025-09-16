package ru.devsoland.drydrive.feature_weather.ui

// Все ваши существующие импорты для HomeScreenContent ...
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
import ru.devsoland.drydrive.feature_weather.ui.composables.RecommendationInfoDialog
import ru.devsoland.drydrive.ui.theme.CityBackgroundOverlay
import java.util.Locale

import android.util.Log
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext // *** ДОБАВЛЕН ИМПОРТ ***
import androidx.appcompat.app.AppCompatDelegate // *** ДОБАВЛЕН ИМПОРТ ***
import android.os.Build // *** ДОБАВЛЕН ИМПОРТ ***

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
                .verticalScroll(rememberScrollState())
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
                        Spacer(modifier = Modifier.weight(1f))
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
            }
        }

        if (!uiState.recommendations.isEmpty()) {
            // *** НАЧАЛО БЛОКА ЛОГИРОВАНИЯ ДЛЯ РЕКОМЕНДАЦИЙ (ОБЩИЙ) ***
            val currentContext = LocalContext.current
            val currentLocale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                currentContext.resources.configuration.locales[0]
            } else {
                @Suppress("DEPRECATION")
                currentContext.resources.configuration.locale
            }
            val appCompatLocaleTag = AppCompatDelegate.getApplicationLocales().toLanguageTags()
            Log.d("RecommendationLocale", "Preparing RecommendationsDisplaySection. ContextLocale: $currentLocale, AppCompatLocale: '$appCompatLocaleTag', NumRecommendations: ${uiState.recommendations.size}")
            // *** КОНЕЦ БЛОКА ЛОГИРОВАНИЯ ***
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
                !uiState.dailyForecasts.isEmpty() -> {
                    DailyForecastRow(forecastItems = uiState.dailyForecasts)
                }
                uiState.weather == null -> {
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
