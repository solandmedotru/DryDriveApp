package ru.devsoland.drydrive.common.util

import ru.devsoland.drydrive.data.api.model.City

fun formatCityName(city: City, currentAppLanguageCode: String): String {
    val displayName = when {
        currentAppLanguageCode.isNotBlank() && city.localNames?.containsKey(currentAppLanguageCode) == true -> {
            city.localNames[currentAppLanguageCode]
        }
        city.localNames?.containsKey("en") == true -> {
            city.localNames["en"]
        }
        else -> {
            city.name
        }
    } ?: city.name
    return "$displayName, ${city.country}" + if (city.state != null) ", ${city.state}" else ""
}