package ru.devsoland.drydrive.data.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WeatherConditionDto(
    @SerialName("id")
    val id: Int, // Идентификатор погодного условия (например, 800 для "ясно")
    @SerialName("main")
    val main: String, // Группа погодных параметров (Rain, Snow, Extreme и т.д.)
    @SerialName("description")
    val description: String, // Описание погоды внутри группы
    @SerialName("icon")
    val icon: String // Код иконки погоды
)
