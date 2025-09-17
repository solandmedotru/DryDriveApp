package ru.devsoland.drydrive.ui.theme

import androidx.compose.ui.graphics.Color

// --- Material 3 Theme Colors (Dark Scheme - New/Updated) ---
val DarkBackground = Color(0xFF121214)       // Очень темный фон, как на скриншоте
val DarkSurface = Color(0xFF1E1E22)          // Фон для карточек, чуть светлее основного
val DarkPrimary = Color(0xFF0095FF)         // Яркий синий (акцентный)
val DarkOnPrimary = Color.White             // Текст на ярком синем
val DarkPrimaryContainer = DarkPrimary      // Фон для выделенных элементов (погода "Сейчас")
val DarkOnPrimaryContainer = Color.White    // Текст на DarkPrimaryContainer

val DarkOnBackground = Color(0xFFE0E1E1)     // Основной текст на DarkBackground
val DarkOnSurface = Color(0xFFE0E1E1)        // Основной текст на DarkSurface (карточки)
val DarkOnSurfaceVariant = Color(0xFFA8A8A8) // Второстепенный, приглушенный текст

val DarkError = Color(0xFFCF6679)           // Стандартный цвет ошибки для темных тем
val DarkOnError = Color.Black

// --- Existing custom colors (интегрируем или оставляем по необходимости) ---
val AppBackground = DarkBackground        // Используем новое определение
val AppAccentBlue = DarkPrimary           // Используем новое определение

val AppPlaceholderGray = Color.White.copy(alpha = 0.3f)

val AppBarContentColor = DarkOnSurface    // Текст в AppBar
val AppBarContentColorMediumEmphasis = DarkOnSurface.copy(alpha = 0.75f)
val AppBarContentColorLowEmphasis = DarkOnSurface.copy(alpha = 0.5f)

val TextOnDarkBackground = DarkOnBackground // Используем новое определение
val TextOnDarkBackgroundMediumEmphasis = DarkOnBackground.copy(alpha = 0.75f)
val TextOnDarkBackgroundLowEmphasis = DarkOnBackground.copy(alpha = 0.6f)

val SearchFieldBorderFocused = DarkOnSurface.copy(alpha = 0.8f)
val SearchFieldBorderUnfocused = DarkOnSurfaceVariant.copy(alpha = 0.6f)

val BottomNavBackground = Color(0xFF171719) // Фон для Bottom Navigation, чуть светлее DarkBackground
val CityBackgroundOverlay = Color.Black.copy(alpha = 0.6f) // Оставляем, если текущий эффект на фоне города устраивает

// --- Оригинальные Material 3 цвета (можно оставить для справки или удалить, если не используются напрямую) ---
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

// --- Цвета для Recommendation Chips (можно оставить, если они должны быть универсальными, 
// либо определить специфичные для темной темы, если это необходимо) ---
val RecDrinkWaterActive = Color(0xFF4FC3F7)
val RecUVActive = Color(0xFFFFD54F)
val RecTiresActive = Color(0xFF81D4FA) 
val RecUmbrellaActive = Color(0xFFFFB74D)
val RecDressWarmlyActive = Color(0xFFB0BEC5)
val RecLimitActivityActive = Color(0xFFEF5350)
