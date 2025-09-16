package ru.devsoland.drydrive

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.isSystemInDarkTheme // <-- Импорт для isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable // <-- Добавлен импорт Composable
import androidx.compose.runtime.collectAsState // <-- Импорт для collectAsState
import androidx.compose.runtime.getValue // <-- Импорт для getValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import ru.devsoland.drydrive.common.util.setAppLocale
import ru.devsoland.drydrive.common.model.AppLanguage
import ru.devsoland.drydrive.common.model.ThemeSetting // <-- Импорт ThemeSetting
import ru.devsoland.drydrive.data.preferences.UserPreferencesManager
import ru.devsoland.drydrive.di.UserPreferencesEntryPoint
import ru.devsoland.drydrive.feature_weather.ui.WeatherViewModel
import ru.devsoland.drydrive.ui.theme.DryDriveTheme
import javax.inject.Inject // <-- Импорт для @Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject // Внедряем UserPreferencesManager
    lateinit var userPreferencesManager: UserPreferencesManager

    private val weatherViewModel: WeatherViewModel by viewModels()

    override fun attachBaseContext(newBase: Context) {
        Log.d(
            "MainActivityLifecycle",
            "attachBaseContext CALLED. Initial base context locale: ${newBase.resources.configuration.locale}"
        )

        // Логика для языка остается здесь, так как она нужна очень рано
        val tempUserPreferencesManager: UserPreferencesManager = try {
            val entryPoint = EntryPointAccessors.fromApplication(
                newBase.applicationContext,
                UserPreferencesEntryPoint::class.java
            )
            entryPoint.getUserPreferencesManager().also {
                Log.d(
                    "MainActivityLifecycle",
                    "UserPreferencesManager obtained successfully via EntryPoint for language."
                )
            }
        } catch (e: Exception) {
            Log.e(
                "MainActivityLifecycle",
                "Error getting UserPreferencesManager in attachBaseContext: ${e.message}. Using default base context.",
                e
            )
            super.attachBaseContext(newBase)
            return
        }

        val currentLanguageCode = try {
            Log.d(
                "MainActivityLifecycle",
                "attachBaseContext: Attempting to get language code via runBlocking..."
            )
            runBlocking {
                tempUserPreferencesManager.selectedLanguageFlow.first().code
            }
        } catch (e: Exception) {
            Log.e(
                "MainActivityLifecycle",
                "Error getting language code for attachBaseContext: ${e.message}. Defaulting to SYSTEM code.",
                e
            )
            AppLanguage.SYSTEM.code
        }

        Log.d(
            "MainActivityLifecycle",
            "attachBaseContext: Applying language code: '$currentLanguageCode'"
        )
        val contextWithLocale = newBase.setAppLocale(currentLanguageCode)
        super.attachBaseContext(contextWithLocale)
        Log.d(
            "MainActivityLifecycle",
            "attachBaseContext FINISHED. Applied locale from context: ${contextWithLocale.resources.configuration.locale}"
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Log.d(
                "MainActivityLifecycle",
                "attachBaseContext: AppCompatDelegate locales after set: ${
                    AppCompatDelegate.getApplicationLocales().toLanguageTags()
                }"
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // ... ваш существующий код onCreate для логов локали ...
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Log.d(
                "MainActivityLifecycle",
                "onCreate: Current resources locales (from resources.configuration): ${resources.configuration.locales.toLanguageTags()}"
            )
        } else {
            @Suppress("DEPRECATION")
            Log.d(
                "MainActivityLifecycle",
                "onCreate: Current resources locale (from resources.configuration): ${resources.configuration.locale}"
            )
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Log.d(
                "MainActivityLifecycle",
                "onCreate: Current AppCompatDelegate locales: ${
                    AppCompatDelegate.getApplicationLocales().toLanguageTags()
                }"
            )
        }

        WindowCompat.setDecorFitsSystemWindows(window, false)

        lifecycleScope.launch {
            weatherViewModel.recreateActivityEvent.collect {
                Log.d("MainActivityLifecycle", "recreateActivityEvent received, calling recreate()")
                this@MainActivity.recreate()
            }
        }

        setContent {
            // Получаем текущую настройку темы из UserPreferencesManager
            val currentThemeSetting by userPreferencesManager.selectedThemeFlow.collectAsState(
                initial = ThemeSetting.SYSTEM // Начальное значение, пока Flow не эмитирует первое
            )

            // Определяем, использовать ли темную тему
            val useDarkTheme = when (currentThemeSetting) {
                ThemeSetting.LIGHT -> false
                ThemeSetting.DARK -> true
                ThemeSetting.SYSTEM -> isSystemInDarkTheme()
            }

            DryDriveTheme(darkTheme = useDarkTheme) { // Передаем результат в DryDriveTheme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DryDriveApp(weatherViewModel = weatherViewModel)
                }
            }
        }
    }
}
