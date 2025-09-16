package ru.devsoland.drydrive.feature_settings.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme // <-- Import for system theme check
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch // <-- Import Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.devsoland.drydrive.R
import ru.devsoland.drydrive.common.model.AppLanguage
import ru.devsoland.drydrive.common.model.ThemeSetting

/**
 * Composable функция для отображения отображаемого имени языка.
 */
@Composable
private fun appLanguageDisplayName(language: AppLanguage): String {
    return LocalContext.current.getString(language.displayNameResId)
}

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    settingsViewModel: SettingsViewModel = viewModel(), // Renamed for clarity
    onLanguageConfirmed: (AppLanguage) -> Unit // *** НОВЫЙ ПАРАМЕТР ***
) {
    val currentLanguage by settingsViewModel.currentLanguage.collectAsState()
    val currentTheme by settingsViewModel.currentTheme.collectAsState()
    val isSystemDark = isSystemInDarkTheme() // Check actual system theme

    Column(
        modifier = modifier
            .padding(all = 16.dp)
    ) {
        // --- Секция выбора языка ---
        Text(
            text = stringResource(R.string.settings_language_title),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        AppLanguage.entries.forEach { language ->
            SelectableRow(
                text = appLanguageDisplayName(language),
                selected = currentLanguage == language,
                onClick = {
                    if (currentLanguage != language) {
                        // viewModel.onLanguageSelected(language) // *** УДАЛЕНО ***
                        onLanguageConfirmed(language) // *** ИСПОЛЬЗУЕМ НОВУЮ ЛЯМБДУ ***
                    }
                }
            )
        }

        Spacer(Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(Modifier.height(24.dp))

        // --- Секция выбора темы ---
        Text(
            text = stringResource(R.string.settings_theme_title),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 16.dp) // Added a bit more bottom padding
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    val newTheme = if (currentTheme == ThemeSetting.DARK || (currentTheme == ThemeSetting.SYSTEM && isSystemDark)) {
                        ThemeSetting.LIGHT
                    } else {
                        ThemeSetting.DARK
                    }
                    settingsViewModel.onThemeSelected(newTheme) // Оставляем вызов для темы
                }
                .padding(vertical = 8.dp), // Adjusted padding for the row
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween // To push Switch to the end
        ) {
            Text(
                text = stringResource(R.string.settings_theme_dark_mode_toggle), // e.g., "Темная тема"
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f) // Allow text to take available space
            )
            Switch(
                checked = when (currentTheme) {
                    ThemeSetting.LIGHT -> false
                    ThemeSetting.DARK -> true
                    ThemeSetting.SYSTEM -> isSystemDark // Switch reflects system if "System" is chosen
                },
                onCheckedChange = { isChecked ->
                    settingsViewModel.onThemeSelected(if (isChecked) ThemeSetting.DARK else ThemeSetting.LIGHT)
                },
                modifier = Modifier.padding(start = 16.dp)
            )
        }
    }
}

/**
 * Вспомогательный Composable для строки с RadioButton и текстом.
 */
@Composable
private fun SelectableRow(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
