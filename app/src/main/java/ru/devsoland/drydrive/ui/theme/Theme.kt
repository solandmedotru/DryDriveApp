package ru.devsoland.drydrive.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color // Необходим, если напрямую используются цвета, не входящие в схему
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkPrimary, // Можно использовать тот же DarkPrimary или другой акцентный цвет, если есть
    onSecondary = DarkOnPrimary,
    // secondaryContainer = ..., // Определите, если нужен для элементов вроде фильтров
    // onSecondaryContainer = ...,
    tertiary = Pink80, // Оставим пока, если не используется активно, или подберем другой
    onTertiary = Color.Black, // Соответственно
    // tertiaryContainer = ..., 
    // onTertiaryContainer = ..., 
    error = DarkError,
    onError = DarkOnError,
    // errorContainer = ..., 
    // onErrorContainer = ..., 
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurface, // Можно использовать DarkSurface или чуть отличающийся оттенок
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOnSurfaceVariant.copy(alpha = 0.5f), // Для границ, например, в OutlinedTextField
    // inverseOnSurface = ..., 
    // inverseSurface = ..., 
    // inversePrimary = ..., 
    // surfaceTint = ..., 
    // outlineVariant = ..., 
    // scrim = ...
)

// LightColorScheme остается без изменений, если задача касается только темной темы
private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40,
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F)
    /*
    Можно дополнить остальными цветами Material 3 для полноты:
    primaryContainer = ..., 
    onPrimaryContainer = ..., 
    secondaryContainer = ..., 
    onSecondaryContainer = ..., 
    tertiaryContainer = ..., 
    onTertiaryContainer = ..., 
    error = ..., 
    onError = ..., 
    errorContainer = ..., 
    onErrorContainer = ..., 
    surfaceVariant = ..., 
    onSurfaceVariant = ..., 
    outline = ..., 
    inverseOnSurface = ..., 
    inverseSurface = ..., 
    inversePrimary = ..., 
    surfaceTint = ..., 
    outlineVariant = ..., 
    scrim = ...
    */
)

@Composable
fun DryDriveTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true, 
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography, // Убедитесь, что Typography определена
        content = content
    )
}
