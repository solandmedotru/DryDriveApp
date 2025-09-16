package ru.devsoland.drydrive.data.api.model

import kotlinx.serialization.Serializable

@Serializable
data class CloudsData(
    val all: Int // Облачность в %
)