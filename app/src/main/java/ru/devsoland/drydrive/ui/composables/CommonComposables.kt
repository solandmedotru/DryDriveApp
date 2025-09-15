package ru.devsoland.drydrive.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
// NEW: Удаляем прямой импорт Color
// import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
// import androidx.compose.ui.unit.dp // Можно удалить, если все dp через dimensionResource
import ru.devsoland.drydrive.R // NEW: Импорт для R.dimen
// NEW: Импорты для ваших кастомных цветов из Color.kt
import ru.devsoland.drydrive.ui.theme.AppPlaceholderGray
import ru.devsoland.drydrive.ui.theme.AppCardBackgroundDark
// NEW: Импорт для AppDimens, если используете Kotlin-константы для размеров
// import ru.devsoland.drydrive.ui.theme.AppDimens

@Composable
fun DailyForecastPlaceholder(
    modifier: Modifier = Modifier
) {
    // NEW: Используем цвета из Color.kt
    val placeholderColor = AppPlaceholderGray
    val cardBackgroundColor = AppCardBackgroundDark

    // NEW: Используем размеры из dimens.xml или AppDimens.kt
    val cardShape = RoundedCornerShape(dimensionResource(R.dimen.corner_radius_medium)) // Например, 12dp
    val cardModifier = Modifier
        .width(dimensionResource(R.dimen.forecast_card_width))    // Например, 80dp
        .height(dimensionResource(R.dimen.forecast_card_height)) // Например, 120dp

    // Размеры для элементов плейсхолдера
    val placeholderTextHeight = dimensionResource(R.dimen.placeholder_text_width) // Например, 16dp
    val placeholderTextShortWidth = dimensionResource(R.dimen.placeholder_text_short_width) // Например, 40dp
    val placeholderTextMediumWidth = dimensionResource(R.dimen.placeholder_text_medium_width) // Например, 30dp (для нижней температуры)
    val placeholderIconSize = dimensionResource(R.dimen.placeholder_icon_size) // Например, 36dp
    val placeholderCornerRadius = dimensionResource(R.dimen.corner_radius_small) // Например, 4dp


    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_medium_large)) // Например, 12dp
    ) {
        repeat(5) {
            Card(
                shape = cardShape,
                colors = CardDefaults.cardColors(containerColor = cardBackgroundColor),
                modifier = cardModifier
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            vertical = dimensionResource(R.dimen.spacing_large),   // Например, 16dp
                            horizontal = dimensionResource(R.dimen.spacing_medium) // Например, 8dp
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .size(
                                width = placeholderTextShortWidth,
                                height = placeholderTextHeight
                            )
                            .background(placeholderColor, RoundedCornerShape(placeholderCornerRadius))
                    )
                    Box(
                        modifier = Modifier
                            .size(placeholderIconSize)
                            .background(placeholderColor, CircleShape)
                    )
                    Box(
                        modifier = Modifier
                            .size(
                                width = placeholderTextMediumWidth,
                                height = placeholderTextHeight // Можно использовать тот же placeholderTextHeight или отдельный
                            )
                            .background(placeholderColor, RoundedCornerShape(placeholderCornerRadius))
                    )
                }
            }
        }
    }
}

