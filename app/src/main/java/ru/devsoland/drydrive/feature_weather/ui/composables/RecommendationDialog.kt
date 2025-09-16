package ru.devsoland.drydrive.feature_weather.ui.composables // Замените на ваш точный путь к пакету

import androidx.annotation.StringRes
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import ru.devsoland.drydrive.R // Убедитесь, что ваш R класс импортирован правильно

@Composable
fun RecommendationInfoDialog(
    @StringRes titleResId: Int?,         // ID ресурса для заголовка
    @StringRes descriptionResId: Int?,  // ID ресурса для описания
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Показываем диалог только если есть что показывать
    if (titleResId != null && descriptionResId != null) {
        AlertDialog(
            onDismissRequest = onDismiss, // Вызывается, когда пользователь нажимает вне диалога или кнопку "назад"
            title = {
                Text(text = stringResource(id = titleResId))
            },
            text = {
                Text(text = stringResource(id = descriptionResId))
            },
            confirmButton = {
                TextButton(onClick = onDismiss) { // Кнопка "OK" также вызывает onDismiss
                    Text(stringResource(id = R.string.action_ok)) // Используем вашу существующую строку "OK"
                }
            },
            modifier = modifier
        )
    }
}

// Preview для диалога
@Preview(showBackground = false, name = "Recommendation Dialog Preview")
@Composable
fun RecommendationInfoDialogPreview() {
    // Для корректного отображения Preview с AlertDialog,
    // MaterialTheme обычно нужен, если его нет в вашем Preview по умолчанию.
    // Если ваш проект уже настроен для Preview с MaterialTheme, эта обертка может быть не нужна.
    // DryDriveTheme { // Если у вас есть своя тема, используйте ее
    RecommendationInfoDialog(
        titleResId = R.string.rec_drink_water_active, // Пример заголовка из ваших строк
        descriptionResId = R.string.rec_drink_water_desc_active, // Пример описания
        onDismiss = {}
    )
    // }
}
