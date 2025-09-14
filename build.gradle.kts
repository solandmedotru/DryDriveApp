plugins {
    id("com.android.application") version "8.13.0" apply false
    id("org.jetbrains.kotlin.android") version "2.2.20" apply false  // Обновлена версия Kotlin для стабильности
    id("com.google.dagger.hilt.android") version "2.57.1" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.2.20" apply false  // Версия совпадает с Kotlin
    id("com.google.devtools.ksp") version "2.2.20-2.0.3" apply false  // Обновлена версия KSP
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.20" apply false  // Новый плагин для Compose
}