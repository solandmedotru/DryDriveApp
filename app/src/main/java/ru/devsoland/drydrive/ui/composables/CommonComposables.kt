package ru.devsoland.drydrive.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme // ИМПОРТ ДЛЯ ДОСТУПА К ЦВЕТАМ ТЕМЫ
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
// import androidx.compose.ui.graphics.Color // Удаляем, если не используется для кастомных цветов
import androidx.compose.ui.res.dimensionResource
// import androidx.compose.ui.unit.dp // Можно удалить, если все dp через dimensionResource
import ru.devsoland.drydrive.R
// Удаляем импорты кастомных цветов для плейсхолдера и фона карточки
// import ru.devsoland.drydrive.ui.theme.AppPlaceholderGray
// import ru.devsoland.drydrive.ui.theme.AppCardBackgroundDark
// import ru.devsoland.drydrive.ui.theme.AppDimens // Если используете

@Composable
fun DailyForecastPlaceholder(
    modifier: Modifier = Modifier
) {
    // Используем цвета из MaterialTheme.colorScheme для плейсхолдера и фона карточки
    // Цвет плейсхолдера: обычно это приглушенный цвет на фоне поверхности.
    // MaterialTheme.colorScheme.surfaceVariant или surface.copy(alpha = 0.3f) могут подойти.
    // Для большей универсальности можно использовать onSurface с низкой альфой.
    val placeholderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f) // Стандартная альфа для Disabled/Placeholder
    // Фон карточки для плейсхолдера должен быть похож на фон обычной карточки прогноза
    val cardBackgroundColor = MaterialTheme.colorScheme.surfaceVariant


    val cardShape = RoundedCornerShape(dimensionResource(R.dimen.corner_radius_medium))
    val cardModifier = Modifier
        .width(dimensionResource(R.dimen.forecast_card_width))
        .height(dimensionResource(R.dimen.forecast_card_height))

    val placeholderTextHeight = dimensionResource(R.dimen.placeholder_text_width)
    val placeholderTextShortWidth = dimensionResource(R.dimen.placeholder_text_short_width)
    val placeholderTextMediumWidth = dimensionResource(R.dimen.placeholder_text_medium_width)
    val placeholderIconSize = dimensionResource(R.dimen.placeholder_icon_size)
    val placeholderCornerRadius = dimensionResource(R.dimen.corner_radius_small)


    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_medium_large))
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
                            vertical = dimensionResource(R.dimen.spacing_large),
                            horizontal = dimensionResource(R.dimen.spacing_medium)
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
                                height = placeholderTextHeight
                            )
                            .background(placeholderColor, RoundedCornerShape(placeholderCornerRadius))
                    )
                }
            }
        }
    }
}

