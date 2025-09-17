package ru.devsoland.drydrive.feature_weather.ui.composables

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle 
import androidx.compose.material.icons.filled.NightsStay 
import androidx.compose.material.icons.filled.Thermostat 
import androidx.compose.material.icons.filled.Opacity 
import androidx.compose.material.icons.filled.Air 
import androidx.compose.material.icons.filled.Compress 
import androidx.compose.material.icons.filled.WbSunny 
import androidx.compose.material.icons.filled.Visibility 
import androidx.compose.material.icons.filled.Cloud 
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.devsoland.drydrive.R
import ru.devsoland.drydrive.feature_weather.ui.model.WeatherDetailsUiModel

val WashRecommendationGreen = Color(0xFF4CAF50)

@Composable
fun StyledWashRecommendation(
    text: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = WashRecommendationGreen,
    contentColor: Color = Color.White
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(dimensionResource(R.dimen.rounded_corner_large)))
            .background(backgroundColor)
            .padding(horizontal = dimensionResource(R.dimen.spacing_medium), vertical = dimensionResource(R.dimen.spacing_small)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = stringResource(R.string.recommendation_icon_description),
            tint = contentColor,
            modifier = Modifier.size(dimensionResource(R.dimen.icon_size_small))
        )
        Spacer(modifier = Modifier.width(dimensionResource(R.dimen.spacing_small)))
        Text(
            text = text,
            color = contentColor,
            style = MaterialTheme.typography.labelLarge, 
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun WeatherInfoGrid(
    weatherDetails: WeatherDetailsUiModel,
    modifier: Modifier = Modifier,
    contentColor: Color
) {
    Column(modifier = modifier) {
        // Ряд 1 (4 элемента)
        Row(modifier = Modifier.fillMaxWidth()) {
            DetailItem(
                icon = Icons.Filled.Thermostat,
                label = stringResource(R.string.details_feels_like),
                value = weatherDetails.feelsLikeValue ?: "-",
                modifier = Modifier.weight(1f),
                iconTint = contentColor, 
                labelColor = contentColor.copy(alpha = 0.7f), 
                valueColor = contentColor
            )
            Spacer(modifier = Modifier.width(dimensionResource(R.dimen.spacing_small)))
            DetailItem(
                icon = Icons.Filled.Opacity,
                label = stringResource(R.string.details_humidity_label),
                value = weatherDetails.humidity ?: "-",
                modifier = Modifier.weight(1f),
                iconTint = contentColor,
                labelColor = contentColor.copy(alpha = 0.7f),
                valueColor = contentColor
            )
            Spacer(modifier = Modifier.width(dimensionResource(R.dimen.spacing_small)))
            DetailItem(
                icon = Icons.Filled.Air,
                label = stringResource(R.string.details_wind_label),
                value = weatherDetails.windSpeed ?: "-",
                modifier = Modifier.weight(1f),
                iconTint = contentColor,
                labelColor = contentColor.copy(alpha = 0.7f),
                valueColor = contentColor
            )
            Spacer(modifier = Modifier.width(dimensionResource(R.dimen.spacing_small)))
            DetailItem(
                icon = Icons.Filled.Compress,
                label = stringResource(R.string.details_pressure_label),
                value = weatherDetails.pressure ?: "-",
                modifier = Modifier.weight(1f),
                iconTint = contentColor,
                labelColor = contentColor.copy(alpha = 0.7f),
                valueColor = contentColor
            )
        }

        // Ряд 2 (до 3 элементов + заглушка)
        if (weatherDetails.tempMinMax != null || weatherDetails.visibility != null || weatherDetails.cloudiness != null) {
            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_medium)))
            Row(modifier = Modifier.fillMaxWidth()) {
                if (weatherDetails.tempMinMax != null) {
                    DetailItem(
                        icon = Icons.Filled.CompareArrows,
                        label = stringResource(R.string.details_temp_min_max_label_short), 
                        value = weatherDetails.tempMinMax,
                        modifier = Modifier.weight(1f),
                        iconTint = contentColor,
                        labelColor = contentColor.copy(alpha = 0.7f),
                        valueColor = contentColor
                    )
                } else {
                    Spacer(Modifier.weight(1f).padding(horizontal = dimensionResource(R.dimen.spacing_tiny)))
                }
                Spacer(modifier = Modifier.width(dimensionResource(R.dimen.spacing_small)))
                if (weatherDetails.visibility != null) {
                    DetailItem(
                        icon = Icons.Filled.Visibility, 
                        label = stringResource(R.string.details_visibility_label_short), 
                        value = weatherDetails.visibility,
                        modifier = Modifier.weight(1f),
                        iconTint = contentColor,
                        labelColor = contentColor.copy(alpha = 0.7f),
                        valueColor = contentColor
                    )
                } else {
                    Spacer(Modifier.weight(1f).padding(horizontal = dimensionResource(R.dimen.spacing_tiny)))
                }
                Spacer(modifier = Modifier.width(dimensionResource(R.dimen.spacing_small)))
                if (weatherDetails.cloudiness != null) {
                    DetailItem(
                        icon = Icons.Filled.Cloud, 
                        label = stringResource(R.string.details_cloudiness_label_short), 
                        value = weatherDetails.cloudiness,
                        modifier = Modifier.weight(1f),
                        iconTint = contentColor,
                        labelColor = contentColor.copy(alpha = 0.7f),
                        valueColor = contentColor
                    )
                } else {
                    Spacer(Modifier.weight(1f).padding(horizontal = dimensionResource(R.dimen.spacing_tiny)))
                }
                Spacer(Modifier.weight(1f).padding(horizontal = dimensionResource(R.dimen.spacing_tiny)))
            }
        }
    }
}

@Composable
fun SunriseSunsetInfo(
    weatherDetails: WeatherDetailsUiModel,
    modifier: Modifier = Modifier,
    contentColor: Color // Used as a base for day colors
) {
    val currentTime = System.currentTimeMillis()
    val sunriseTimeText = weatherDetails.sunrise ?: "-"
    val sunsetTimeText = weatherDetails.sunset ?: "-"

    val isCurrentlyNightForLabels: Boolean
    if (weatherDetails.sunriseEpochMillis != null && weatherDetails.sunsetEpochMillis != null) {
        isCurrentlyNightForLabels = !(currentTime >= weatherDetails.sunriseEpochMillis && currentTime < weatherDetails.sunsetEpochMillis)
    } else {
        isCurrentlyNightForLabels = false // Default to day order if times are missing
    }

    val leftLabelText: String
    val leftTimeValue: String
    val leftIconImage: ImageVector
    val rightLabelText: String
    val rightTimeValue: String
    val rightIconImage: ImageVector

    if (isCurrentlyNightForLabels) {
        // Night: Sunset on left, Sunrise on right
        leftLabelText = stringResource(R.string.details_sunset_label)
        leftTimeValue = sunsetTimeText
        leftIconImage = Icons.Filled.NightsStay
        rightLabelText = stringResource(R.string.details_sunrise_label)
        rightTimeValue = sunriseTimeText 
        rightIconImage = Icons.Filled.WbSunny
    } else {
        // Day: Sunrise on left, Sunset on right
        leftLabelText = stringResource(R.string.details_sunrise_label)
        leftTimeValue = sunriseTimeText
        leftIconImage = Icons.Filled.WbSunny
        rightLabelText = stringResource(R.string.details_sunset_label)
        rightTimeValue = sunsetTimeText
        rightIconImage = Icons.Filled.NightsStay
    }

    Column(modifier = modifier) {
        // Define colors for DaylightProgressBar
        val dayGradientStartColor = contentColor.copy(alpha = 0.9f) // Lightest part of day
        val dayGradientEndColor = contentColor.copy(alpha = 0.5f)   // Darker part of day fill
        val dayTrackColor = contentColor.copy(alpha = 0.25f) // Dim track for day (background)

        val nightProgressStartColor = Color(0xFF01579B) // Deeper Dark Blue
        val nightProgressEndColor = Color(0xFF29B6F6)   // Lighter Sky Blue
        val nightTrackColor = Color(0xFFB0BEC5)       // Blue Grey for track (background for night)

        if (weatherDetails.sunriseEpochMillis != null && 
            weatherDetails.sunsetEpochMillis != null &&
            weatherDetails.nextSunriseEpochMillis != null
            ) {
            DaylightProgressBar(
                sunriseEpochMillis = weatherDetails.sunriseEpochMillis,
                sunsetEpochMillis = weatherDetails.sunsetEpochMillis,
                nextSunriseEpochMillis = weatherDetails.nextSunriseEpochMillis,
                currentTimeMillis = currentTime, 
                modifier = Modifier
                    .fillMaxWidth()
                    .height(dimensionResource(R.dimen.daylight_progress_bar_height)),
                dayGradientStartColor = dayGradientStartColor,
                dayGradientEndColor = dayGradientEndColor,
                dayTrackColor = dayTrackColor,
                nightProgressStartColor = nightProgressStartColor,
                nightProgressEndColor = nightProgressEndColor,
                nightTrackColor = nightTrackColor
            )
        } else {
            Spacer(modifier = Modifier
                .fillMaxWidth()
                .height(dimensionResource(R.dimen.daylight_progress_bar_height)))
        }
        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_medium))) // Spacer below progress bar

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween // Align children to start and end
        ) {
            EventTimeRow(
                icon = leftIconImage,
                labelText = leftLabelText,
                timeText = leftTimeValue,
                // modifier = Modifier.weight(1f), // Removed weight
                iconTint = contentColor,
                labelColor = contentColor.copy(alpha = 0.7f),
                timeColor = contentColor
            )
            // Spacer(modifier = Modifier.width(dimensionResource(R.dimen.spacing_small))) // Removed spacer
            EventTimeRow(
                icon = rightIconImage,
                labelText = rightLabelText,
                timeText = rightTimeValue,
                // modifier = Modifier.weight(1f), // Removed weight
                iconTint = contentColor,
                labelColor = contentColor.copy(alpha = 0.7f),
                timeColor = contentColor
            )
        }
    }
}

@Composable
private fun DetailItem(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    iconTint: Color, 
    labelColor: Color, 
    valueColor: Color  
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = iconTint,
            modifier = Modifier.size(dimensionResource(R.dimen.icon_size_medium))
        )
        Spacer(Modifier.height(dimensionResource(R.dimen.spacing_micro)))
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = labelColor, textAlign = TextAlign.Center)
        Text(text = value, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium, color = valueColor, textAlign = TextAlign.Center)
    }
}

@Composable
private fun EventTimeRow(
    icon: ImageVector,
    labelText: String,
    timeText: String,
    modifier: Modifier = Modifier, // Keep modifier parameter for flexibility if needed later
    iconTint: Color,
    labelColor: Color,
    timeColor: Color
) {
    Row(
        modifier = modifier, // Apply the passed modifier
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = labelText, 
            tint = iconTint,
            modifier = Modifier.size(dimensionResource(R.dimen.icon_size_medium))
        )
        Spacer(Modifier.width(dimensionResource(R.dimen.spacing_tiny)))
        Text(
            text = "$labelText:",
            style = MaterialTheme.typography.bodySmall,
            color = labelColor
        )
        Spacer(Modifier.width(dimensionResource(R.dimen.spacing_micro)))
        Text(
            text = timeText,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = timeColor
        )
    }
}


@Composable
fun DaylightProgressBar(
    sunriseEpochMillis: Long?,
    sunsetEpochMillis: Long?,
    nextSunriseEpochMillis: Long?,
    currentTimeMillis: Long,
    modifier: Modifier = Modifier,
    // Day colors
    dayGradientStartColor: Color,
    dayGradientEndColor: Color,
    dayTrackColor: Color,
    // Night colors
    nightProgressStartColor: Color,
    nightProgressEndColor: Color,
    nightTrackColor: Color
) {
    if (sunriseEpochMillis == null || sunsetEpochMillis == null || nextSunriseEpochMillis == null ||
        sunriseEpochMillis >= sunsetEpochMillis || sunsetEpochMillis >= nextSunriseEpochMillis
    ) {
        Spacer(modifier = modifier.height(0.dp))
        return
    }

    val isNight: Boolean
    val periodStartMillis: Long
    val periodEndMillis: Long

    if (currentTimeMillis >= sunriseEpochMillis && currentTimeMillis < sunsetEpochMillis) {
        isNight = false
        periodStartMillis = sunriseEpochMillis
        periodEndMillis = sunsetEpochMillis
    } else {
        isNight = true
        if (currentTimeMillis >= sunsetEpochMillis) { // Night after current day's sunset
            periodStartMillis = sunsetEpochMillis
            periodEndMillis = nextSunriseEpochMillis
        } else { // currentTimeMillis < sunriseEpochMillis: Night before current day's sunrise
            periodStartMillis = sunsetEpochMillis - (24 * 60 * 60 * 1000) // Approx previous sunset
            periodEndMillis = sunriseEpochMillis
        }
    }

    val currentPeriodDuration = remember(periodStartMillis, periodEndMillis) {
        (periodEndMillis - periodStartMillis).coerceAtLeast(1)
    }
    val elapsedInPeriod = remember(currentTimeMillis, periodStartMillis) {
        (currentTimeMillis - periodStartMillis).coerceAtLeast(0)
    }
    val progressRatio = remember(elapsedInPeriod, currentPeriodDuration) {
        (elapsedInPeriod.toFloat() / currentPeriodDuration.toFloat()).coerceIn(0f, 1f)
    }

    val trackBrush: Brush
    val progressBrush: Brush

    if (!isNight) {
        progressBrush = Brush.horizontalGradient(colors = listOf(dayGradientStartColor, dayGradientEndColor))
        trackBrush = Brush.linearGradient(colors = listOf(dayTrackColor, dayTrackColor))
    } else {
        progressBrush = Brush.horizontalGradient(
            colors = listOf(nightProgressStartColor, nightProgressEndColor)
        )
        trackBrush = Brush.linearGradient(colors = listOf(nightTrackColor, nightTrackColor))
    }

    BoxWithConstraints(modifier = modifier) {
        val barHeight = this.maxHeight
        val barWidth = this.maxWidth
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeight)
                .clip(RoundedCornerShape(barHeight / 2))
                .background(trackBrush) 
        )
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(barWidth * progressRatio)
                .clip(RoundedCornerShape(barHeight / 2))
                .background(progressBrush) 
        )
    }
}

@Composable
fun WeatherDetails(
    weatherDetails: WeatherDetailsUiModel, 
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.Start,
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = weatherDetails.cityName ?: "",
            style = TextStyle(
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = dimensionResource(R.dimen.font_size_large_title).value.sp, 
                fontWeight = FontWeight.Bold
            ),
            modifier = Modifier.padding(bottom = dimensionResource(R.dimen.spacing_small))
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            weatherDetails.weatherIconRes?.let {
                Image(
                    painter = painterResource(id = it),
                    contentDescription = weatherDetails.weatherConditionDescription,
                    modifier = Modifier
                        .size(dimensionResource(R.dimen.icon_size_very_large))
                        .padding(end = dimensionResource(R.dimen.spacing_medium))
                )
            }
            Column {
                Text(
                    text = weatherDetails.temperature ?: "", 
                    style = TextStyle(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = dimensionResource(R.dimen.font_size_huge_title).value.sp, 
                        fontWeight = FontWeight.ExtraBold
                    )
                )
                Text(
                    text = weatherDetails.weatherConditionDescription ?: "", 
                    style = TextStyle(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = dimensionResource(R.dimen.font_size_title).value.sp,
                        fontWeight = FontWeight.Normal
                    )
                )
            }
        }
        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_medium)))
    }
}

// TODO: Добавить R.dimen.spacing_micro (например, 2.dp или 4.dp) в dimens.xml
// Используется в DetailItem для меньшего отступа между иконкой и текстом.
