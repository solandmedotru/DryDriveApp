
package ru.devsoland.drydrive.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Umbrella
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.ui.graphics.vector.ImageVector

// Используйте аннотацию @Keep, если вы используете ProGuard/R8 и хотите сохранить этот класс,
// хотя для data классов, используемых kotlinx.serialization или ViewModel, это обычно не требуется,
// если правила ProGuard настроены для них. Для простоты пока можно опустить.
// import androidx.annotation.Keep

enum class RecommendationType {
    DRINK_WATER, UV_PROTECTION, TIRE_CHANGE, UMBRELLA
}

data class RecommendationSlot(
    val type: RecommendationType,
    val defaultIcon: ImageVector,
    val activeIcon: ImageVector, // Можно использовать ту же, если меняется только цвет
    val defaultText: String,
    val activeText: String, // Может быть тем же, что и defaultText
    var isActive: Boolean = false,
    val defaultContentDescription: String,
    val activeContentDescription: String
)

// @Keep
data class DisplayDayWeather(
    val dayShort: String,
    val iconRes: Int,
    val temperature: String
)
