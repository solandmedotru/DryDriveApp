package ru.devsoland.drydrive.feature_weather.ui.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WbSunny // Пример для Preview
import androidx.compose.ui.graphics.Color // Пример для Preview
import ru.devsoland.drydrive.R
import ru.devsoland.drydrive.feature_weather.ui.WeatherEvent
// Логирование было убрано, импорты, связанные с ним, тоже могут быть не нужны, если не используются где-то еще.
// import android.util.Log
// import androidx.compose.ui.platform.LocalContext
// import androidx.appcompat.app.AppCompatDelegate
// import androidx.core.os.ConfigurationCompat
// import java.util.Locale

@Composable
fun RecommendationsDisplaySection(
    recommendations: List<WeatherEvent.Recommendation>,
    onEvent: (WeatherEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    if (recommendations.isNotEmpty()) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = dimensionResource(R.dimen.spacing_medium)),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            recommendations.forEach { recommendation ->
                DynamicRecommendationChip(
                    recommendation = recommendation,
                    onEvent = onEvent,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun DynamicRecommendationChip(
    recommendation: WeatherEvent.Recommendation,
    onEvent: (WeatherEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentAlpha = if (recommendation.isActive) recommendation.activeAlpha else recommendation.inactiveAlpha
    val currentIconColor = if (recommendation.isActive) {
        recommendation.activeColor
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    }
    val currentTextColor = if (recommendation.isActive) {
        recommendation.activeColor
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    val recommendationShortText = stringResource(id = recommendation.textResId)
    val recommendationFullDescription = stringResource(id = recommendation.descriptionResId)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .defaultMinSize(minHeight = dimensionResource(id = R.dimen.chip_min_height))
            .alpha(currentAlpha)
            .clickable(
                onClickLabel = stringResource(id = R.string.select_city_action),
                role = Role.Button,
                onClick = {
                    // Log.d("DynamicChipClick", "Clicked: ${recommendation.id}, TextResId: ${recommendation.textResId}") // Лог удален
                    onEvent(WeatherEvent.RecommendationClicked(recommendation))
                }
            )
            .padding(dimensionResource(id = R.dimen.spacing_small))
            .semantics {
                contentDescription = "$recommendationShortText. $recommendationFullDescription"
            }
    ) {
        Icon(
            imageVector = recommendation.icon,
            contentDescription = null, 
            tint = currentIconColor,
            modifier = Modifier.size(dimensionResource(id = R.dimen.icon_size_large))
        )
        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.spacing_extra_small)))
        Text(
            text = recommendationShortText, 
            color = currentTextColor,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            fontWeight = if (recommendation.isActive) FontWeight.Medium else FontWeight.Normal,
            maxLines = 2
        )
    }
}


@Preview(showBackground = true, name = "Active Dynamic Chip")
@Composable
fun DynamicRecommendationChipActivePreview() {
    MaterialTheme {
        DynamicRecommendationChip(
            recommendation = WeatherEvent.Recommendation(
                id = "preview_active_chip",
                textResId = R.string.rec_drink_water_active,
                descriptionResId = R.string.rec_drink_water_desc_active,
                icon = Icons.Filled.WbSunny,
                isActive = true,
                activeColor = Color.Blue
            ),
            onEvent = {}
        )
    }
}

@Preview(showBackground = true, name = "Inactive Dynamic Chip")
@Composable
fun DynamicRecommendationChipInactivePreview() {
    MaterialTheme {
        DynamicRecommendationChip(
            recommendation = WeatherEvent.Recommendation(
                id = "preview_inactive_chip",
                textResId = R.string.rec_umbrella_default,
                descriptionResId = R.string.rec_umbrella_desc_default,
                icon = Icons.Filled.WbSunny,
                isActive = false,
                activeColor = Color.Gray
            ),
            onEvent = {}
        )
    }
}
