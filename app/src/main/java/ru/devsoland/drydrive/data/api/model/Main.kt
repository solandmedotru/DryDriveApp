package ru.devsoland.drydrive.data.api.model

import kotlinx.serialization.Serializable

@Serializable
data class Main( // Для /data/2.5/weather
    val temp: Double
    // ... другие поля из вашего Main ...
)