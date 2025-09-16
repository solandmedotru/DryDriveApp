package ru.devsoland.drydrive.feature_settings.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext // Для appLanguageDisplayName
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.devsoland.drydrive.feature_weather.ui.WeatherViewModel // Используем наш основной ViewModel
import ru.devsoland.drydrive.R
import ru.devsoland.drydrive.common.model.AppLanguage

/**
 * Composable функция для отображения отображаемого имени языка.
 * Мы определяем ее здесь, так как LanguageManager не должен знать о Composable.
 */
@Composable
private fun appLanguageDisplayName(language: AppLanguage): String {
    return LocalContext.current.getString(language.displayNameResId)
}

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier, // Modifier для применения padding из Scaffold
    viewModel: WeatherViewModel = viewModel()
) {
    // Подписываемся на StateFlow текущего языка из ViewModel
    val currentLanguage by viewModel.currentLanguage.collectAsState()

    Column(
        modifier = modifier // Применяем padding, переданный от Scaffold
            .padding(all = 16.dp) // Дополнительный внутренний padding для контента экрана
    ) {
        Text(
            text = stringResource(R.string.settings_language_title), // "Язык приложения"
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Перебираем все доступные языки из нашего enum AppLanguage
        AppLanguage.entries.forEach { language ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { // Делаем всю строку кликабельной
                        // Вызываем метод ViewModel для сохранения выбранного языка
                        if (currentLanguage != language) { // Чтобы не вызывать лишний раз, если язык уже выбран
                            viewModel.onLanguageSelected(language)
                            // Пока НЕ вызываем recreate() Activity
                        }
                    }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = (currentLanguage == language),
                    onClick = {
                        // Этот onClick для RadioButton также вызывает смену языка
                        if (currentLanguage != language) {
                            viewModel.onLanguageSelected(language)
                        }
                    }
                )
                Spacer(Modifier.width(16.dp))
                Text(
                    text = appLanguageDisplayName(language), // Отображаем имя языка
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}


