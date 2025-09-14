
package ru.devsoland.drydrive.ui

// Используйте аннотацию @Keep, если вы используете ProGuard/R8 и хотите сохранить этот класс,
// хотя для data классов, используемых kotlinx.serialization или ViewModel, это обычно не требуется,
// если правила ProGuard настроены для них. Для простоты пока можно опустить.
// import androidx.annotation.Keep

// @Keep
data class DisplayDayWeather(
    val dayShort: String,
    val iconRes: Int,
    val temperature: String
)