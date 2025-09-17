package ru.devsoland.drydrive

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import ru.devsoland.drydrive.common.model.ThemeSetting
import ru.devsoland.drydrive.data.preferences.UserPreferencesManager
import ru.devsoland.drydrive.di.UserPreferencesEntryPoint
import ru.devsoland.drydrive.feature_weather.ui.WeatherViewModel
import ru.devsoland.drydrive.ui.theme.DryDriveTheme
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var userPreferencesManager: UserPreferencesManager // Эта зависимость может остаться, так как UserPreferencesManager реализует репозиторий
                                                                // и Hilt знает, как его предоставить. Либо можно будет поменять на UserPreferencesRepository.
                                                                // Для EntryPoint это тоже должно работать.

    private val weatherViewModel: WeatherViewModel by viewModels()
    private val tag = "MainActivityLifecycle"

    override fun attachBaseContext(newBase: Context) {
        Log.d(tag, "attachBaseContext CALLED. Initial locale: ${newBase.resources.configuration.locale}")

        val tempUserPrefsManager: UserPreferencesManager = try {
            val entryPoint = EntryPointAccessors.fromApplication(
                newBase.applicationContext,
                UserPreferencesEntryPoint::class.java
            )
            entryPoint.getUserPreferencesManager()
        } catch (e: Exception) {
            Log.e(tag, "Error getting UserPreferencesManager in attachBaseContext: ${e.message}. Using default base context.", e)
            super.attachBaseContext(newBase)
            return
        }

        val currentLanguageCode = try {
            runBlocking {
                tempUserPrefsManager.selectedLanguage.first().code // ИСПРАВЛЕНО
            }
        } catch (e: Exception) {
            Log.e(tag, "Error getting language code for attachBaseContext: ${e.message}. Defaulting to SYSTEM code.", e)
            AppLanguage.SYSTEM.code
        }

        Log.d(tag, "Applying language code: '$currentLanguageCode' in attachBaseContext.")
        val contextWithLocale = newBase.setAppLocale(currentLanguageCode)
        super.attachBaseContext(contextWithLocale)
        Log.d(tag, "attachBaseContext FINISHED. Applied locale: ${contextWithLocale.resources.configuration.locale}")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Log.d(tag, "onCreate: Current locales (from resources.configuration): ${resources.configuration.locales.toLanguageTags()}")
        } else {
            @Suppress("DEPRECATION")
            Log.d(tag, "onCreate: Current locale (from resources.configuration): ${resources.configuration.locale}")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Log.d(tag, "onCreate: Current AppCompatDelegate locales: ${AppCompatDelegate.getApplicationLocales().toLanguageTags()}")
        }

        WindowCompat.setDecorFitsSystemWindows(window, false)

        lifecycleScope.launch {
            weatherViewModel.recreateActivityEvent.collect {
                Log.d(tag, "recreateActivityEvent received. Calling recreate().")
                this@MainActivity.recreate()
            }
        }

        setContent {
            val currentThemeSetting by userPreferencesManager.selectedTheme.collectAsState( // ИСПРАВЛЕНО
                initial = ThemeSetting.SYSTEM
            )

            val useDarkTheme = when (currentThemeSetting) {
                ThemeSetting.LIGHT -> false
                ThemeSetting.DARK -> true
                ThemeSetting.SYSTEM -> isSystemInDarkTheme()
            }

            DryDriveTheme(darkTheme = useDarkTheme) {
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
