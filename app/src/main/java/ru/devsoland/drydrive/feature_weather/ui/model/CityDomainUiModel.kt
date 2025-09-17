package ru.devsoland.drydrive.feature_weather.ui.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue

/**
 * UI модель, представляющая информацию о городе для отображения.
 * Реализует Parcelable для возможной передачи между компонентами UI (например, Navigation).
 */
@Parcelize
data class CityDomainUiModel(
    val name: String, // Может быть локализованное имя, если доступно и выбрано, или имя по умолчанию
    val lat: Double,
    val lon: Double,
    val country: String,
    val state: String? = null,
    val originalNameFromApi: String? = null, // Оригинальное имя от API (обычно на английском)
    val displayName: String, // Имя для отображения, выбранное на основе текущего языка
    val allLocalNames: @RawValue Map<String, String>? = null // Карта всех локализованных имен <язык, имя>
) : Parcelable
