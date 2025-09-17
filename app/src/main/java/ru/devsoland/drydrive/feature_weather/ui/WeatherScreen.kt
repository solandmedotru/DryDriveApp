package ru.devsoland.drydrive.feature_weather.ui

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon 
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color 
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.devsoland.drydrive.R
import ru.devsoland.drydrive.feature_weather.ui.composables.DailyForecastPlaceholder
import ru.devsoland.drydrive.feature_weather.ui.composables.DailyForecastRow
import ru.devsoland.drydrive.feature_weather.ui.composables.RecommendationInfoDialog
import ru.devsoland.drydrive.feature_weather.ui.composables.RecommendationsDisplaySection
import ru.devsoland.drydrive.feature_weather.ui.composables.StyledWashRecommendation 
import ru.devsoland.drydrive.feature_weather.ui.composables.WeatherInfoGrid
import ru.devsoland.drydrive.feature_weather.ui.composables.SunriseSunsetInfo
import ru.devsoland.drydrive.feature_weather.ui.model.WeatherDetailsUiModel
// import ru.devsoland.drydrive.ui.theme.CityBackgroundOverlay // Оверлей убран
import java.util.Locale

private const val DAYLIGHT_DEBUG_TAG = "DaylightDebug"

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
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        CurrentWeatherSection(
            weatherDetails = uiState.weather,
            carImageResId = carImageResId,
            carContentDescription = carContentDescription,
            isLoading = uiState.isLoadingWeather && uiState.weather == null,
            errorMessage = if (uiState.weather == null) uiState.weatherErrorMessage else null
        )
        Column(modifier = Modifier.fillMaxSize().verticalScroll(androidx.compose.foundation.rememberScrollState())) {
            if (uiState.recommendations.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = dimensionResource(R.dimen.spacing_xlarge))
                        .padding(top = dimensionResource(R.dimen.spacing_large))
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
                        bottom = dimensionResource(R.dimen.spacing_medium)
                    )
            ) {
                val dailyForecastRowHeight = dimensionResource(R.dimen.daily_forecast_row_height)
                when {
                    uiState.isLoadingForecast && uiState.dailyForecasts.isEmpty() -> Box(
                        modifier = Modifier.fillMaxWidth().height(dailyForecastRowHeight), contentAlignment = Alignment.Center
                    ) {
                        DailyForecastPlaceholder()
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, strokeWidth = 2.dp)
                    }
                    uiState.forecastErrorMessage != null && uiState.dailyForecasts.isEmpty() -> Box(
                        modifier = Modifier.fillMaxWidth().height(dailyForecastRowHeight), contentAlignment = Alignment.Center
                    ) {
                        Text(text = stringResource(R.string.forecast_unavailable), color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f), textAlign = TextAlign.Center)
                    }
                    uiState.dailyForecasts.isNotEmpty() -> {
                        DailyForecastRow(forecastItems = uiState.dailyForecasts)
                    }
                    uiState.weather == null && !uiState.isLoadingForecast && uiState.forecastErrorMessage == null -> {
                         Box(modifier = Modifier.fillMaxWidth().height(dailyForecastRowHeight), contentAlignment = Alignment.Center) {
                            DailyForecastPlaceholder()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CurrentWeatherSection(
    weatherDetails: WeatherDetailsUiModel?,
    carImageResId: Int,
    carContentDescription: String,
    isLoading: Boolean,
    errorMessage: String?,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = screenHeight * 0.45f, max = screenHeight * 0.5f) 
    ) {
        // Слой 1: Фон города
        Image(
            painter = painterResource(id = R.drawable.city_background),
            contentDescription = stringResource(R.string.city_background_description),
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Слой 2: Машина
        Image(
            painter = painterResource(id = carImageResId),
            contentDescription = carContentDescription,
            modifier = Modifier
                .align(Alignment.BottomCenter) 
                .fillMaxWidth(0.85f) 
                .aspectRatio(16f / 9f) 
                .padding(bottom = dimensionResource(R.dimen.spacing_tiny)),
            contentScale = ContentScale.Fit
        )

        // Слой 3: Информационный Column (поверх Машины и Фона Города)
        when {
            isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            errorMessage != null -> {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.titleMedium.copy(background = Color.Black.copy(alpha = 0.3f)), 
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(dimensionResource(R.dimen.spacing_large))
                )
            }
            weatherDetails != null -> {
                // Логирование данных для DaylightProgressBar
                Log.d(DAYLIGHT_DEBUG_TAG, "Current Time (ms): ${System.currentTimeMillis()}")
                Log.d(DAYLIGHT_DEBUG_TAG, "Sunrise Epoch (ms): ${weatherDetails.sunriseEpochMillis}")
                Log.d(DAYLIGHT_DEBUG_TAG, "Sunset Epoch (ms): ${weatherDetails.sunsetEpochMillis}")

                Column(
                    modifier = Modifier
                        .fillMaxSize() 
                        .padding(horizontal = dimensionResource(R.dimen.spacing_small))
                        .padding(top = dimensionResource(R.dimen.spacing_medium)),
                    horizontalAlignment = Alignment.CenterHorizontally 
                ) {
                    // 1. Название города
                    Text(
                        text = weatherDetails.cityName,
                        style = MaterialTheme.typography.headlineSmall, 
                        color = Color.White, 
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = dimensionResource(R.dimen.spacing_medium))
                    )

                    // 2. Двухколоночная область (Иконка/Описание | Температура)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = dimensionResource(R.dimen.spacing_medium)),
                        verticalAlignment = Alignment.CenterVertically 
                    ) {
                        Column(
                            modifier = Modifier.weight(0.5f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center 
                        ) {
                            Image(
                                painter = painterResource(id = weatherDetails.weatherIconRes),
                                contentDescription = weatherDetails.weatherConditionDescription, 
                                modifier = Modifier.size(dimensionResource(R.dimen.icon_size_very_large)),
                                colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(Color.White)
                            )
                            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_small)))
                            Text(
                                text = weatherDetails.weatherConditionDescription,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White.copy(alpha = 0.85f),
                                textAlign = TextAlign.Center
                            )
                        }
                        Column(
                             modifier = Modifier.weight(0.5f),
                             horizontalAlignment = Alignment.CenterHorizontally,
                             verticalArrangement = Arrangement.Center 
                        ){
                            Text(
                                text = weatherDetails.temperature, 
                                style = MaterialTheme.typography.displayLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 72.sp
                                ),
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    // 3. WeatherInfoGrid (Подробности погоды)
                    WeatherInfoGrid(
                        weatherDetails = weatherDetails, 
                        contentColor = Color.White,
                        modifier = Modifier.fillMaxWidth()
                                       .padding(bottom = dimensionResource(R.dimen.spacing_medium))
                    )

                    // 4. StyledWashRecommendation (Плашка "Мыть/не мыть")
                    // Теперь логика определения isPositive убрана отсюда и ожидается из weatherDetails
                    if (!weatherDetails.washRecommendationText.isNullOrBlank()) {
                        StyledWashRecommendation(
                            text = weatherDetails.washRecommendationText!!, // Используем текст из модели
                            isPositive = weatherDetails.isWashRecommendationPositive, // Используем флаг из модели
                            modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.spacing_large))
                        )
                        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_medium))) 
                    } else {
                         Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_medium))) 
                    }
                    
                    // 5. Spacer(Modifier.weight(1f)) - ПЕРЕД SunriseSunsetInfo, чтобы прижать его к низу
                    Spacer(modifier = Modifier.weight(1f)) 

                    // 6. SunriseSunsetInfo - теперь он будет внизу, ПОСЛЕ Spacer(weight(1f))
                    SunriseSunsetInfo(
                        weatherDetails = weatherDetails, 
                        contentColor = Color.White,
                        modifier = Modifier.fillMaxWidth() 
                    )
                    
                    // 7. Финальный небольшой отступ в самом низу Column, чтобы SunriseSunsetInfo не касался края
                    Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_small))) 
                }
            }
            else -> { 
                 Column(
                    modifier = Modifier.fillMaxSize().padding(dimensionResource(R.dimen.spacing_large)),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = stringResource(R.string.select_city_prompt),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}

