package ru.devsoland.drydrive.ui.composables

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import ru.devsoland.drydrive.R
import ru.devsoland.drydrive.feature_weather.ui.Recommendation // ИМПОРТИРУЙТЕ ВАШ DATA CLASS ИЗ ViewModel

// Старый WeatherRecommendationSection и RecommendationChip можно удалить или закомментировать,
// если вы полностью переходите на новую систему.

@Composable
fun RecommendationsDisplaySection( // Новое имя, чтобы не путать
    recommendations: List<Recommendation>,
    modifier: Modifier = Modifier
) {
    if (recommendations.isNotEmpty()) {
        // Можно использовать LazyRow для горизонтальной прокрутки, если их много,
        // или FlowRow для переноса на следующую строку.
        // Для простоты пока оставим Row, как у вас было, но лучше FlowRow.
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = dimensionResource(R.dimen.spacing_medium)),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            recommendations.forEach { recommendation ->
                DynamicRecommendationChip(recommendation = recommendation)
            }
        }
    }
}

@Composable
fun DynamicRecommendationChip( // Новое имя
    recommendation: Recommendation,
    modifier: Modifier = Modifier
) {
    val currentAlpha = if (recommendation.isActive) recommendation.activeAlpha else recommendation.inactiveAlpha
    val iconToShow = recommendation.icon // Иконка уже в Recommendation data class
    val textToShow = recommendation.text // Текст уже в Recommendation data class

    // Цвет для иконки и текста
    val currentTintColor = if (recommendation.isActive) {
        recommendation.activeColor
    } else {
        // Для неактивных: использовать их activeColor, но с низкой альфой,
        // или приглушенный цвет темы. Выберем первый вариант для цветных, но бледных.
        recommendation.activeColor
        // Альтернатива для неактивных (серые):
        // MaterialTheme.colorScheme.onSurfaceVariant
    }

    val currentTextColor = if (recommendation.isActive) {
        recommendation.activeColor // Текст тоже может быть в цвет иконки
        // или MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant // Приглушенный цвет для неактивного текста
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .width(IntrinsicSize.Min) // Для автоматической ширины
            .alpha(currentAlpha)      // Применяем общую прозрачность здесь
            .padding(horizontal = dimensionResource(R.dimen.spacing_small)) // Небольшой отступ между чипами
    ) {
        Icon(
            imageVector = iconToShow,
            contentDescription = textToShow, // Используем текст как описание для простоты
            tint = currentTintColor, // Вот где используется ваш кастомный цвет
            modifier = Modifier.size(dimensionResource(R.dimen.icon_size_large))
        )
        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_small)))
        Text(
            text = textToShow,
            color = currentTextColor, // Цвет текста
            // fontSize = dimensionResource(R.dimen.font_size_small_caption).value.sp,
            style = MaterialTheme.typography.labelSmall, // Используем стиль темы
            textAlign = TextAlign.Center,
            fontWeight = if (recommendation.isActive) FontWeight.Medium else FontWeight.Normal,
            maxLines = 2 // Ограничим высоту текста, если он длинный
        )
    }
}
