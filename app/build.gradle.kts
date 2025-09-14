plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.compose")  // Новый плагин для Compose
}

android {
    namespace = "ru.devsoland.drydrive"
    compileSdk = 36

    defaultConfig {
        applicationId = "ru.devsoland.drydrive"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        buildConfigField("String", "WEATHER_API_KEY", "\"${project.findProperty("WEATHER_API_KEY") ?: "your_api_key_here"}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            buildConfigField("String", "WEATHER_API_KEY", "\"${project.findProperty("WEATHER_API_KEY") ?: "your_api_key_here"}\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) // Новый DSL для JVM-таргета
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    // Удалён блок composeOptions, так как плагин управляет версией автоматически
    // Если нужно настроить опции компилятора, используй composeCompiler { ... }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Core libraries
    implementation("androidx.core:core-ktx:1.17.0") // Обновлено
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.3") // Обновлено
    implementation("androidx.activity:activity-compose:1.11.0") // Последняя версия

    // Jetpack Compose
    implementation("androidx.compose.ui:ui:1.9.1") // Последняя версия
    implementation("androidx.compose.material3:material3:1.3.2") // Последняя версия
    implementation("androidx.navigation:navigation-compose:2.9.4") // Последняя версия
    implementation("androidx.compose.ui:ui-tooling:1.9.1") // Добавлено для @Preview
    debugImplementation("androidx.compose.ui:ui-tooling-data:1.9.1") // Добавлено для улучшенного превью

    // Retrofit + OkHttp + Serialization
    implementation("com.squareup.retrofit2:retrofit:3.0.0")
    implementation("com.squareup.okhttp3:okhttp:5.1.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0")

    // Room
    implementation("androidx.room:room-runtime:2.8.0") // Последняя версия
    implementation("androidx.room:room-ktx:2.8.0") // Последняя версия
    ksp("androidx.room:room-compiler:2.8.0") // Последняя версия

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.1.7") // Последняя версия

    // Hilt
    implementation("com.google.dagger:hilt-android:2.57.1") // Обновлено
    ksp("com.google.dagger:hilt-compiler:2.57.1") // Обновлено

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2") // Последняя версия

    // Coil
    implementation("io.coil-kt.coil3:coil-compose:3.3.0") // Обновлено

    // Lottie
    implementation("com.airbnb.android:lottie-compose:6.6.7") // Обновлено

    // LeakCanary (только для debug)
    debugImplementation("com.squareup.leakcanary:leakcanary-android:2.14") // Обновлено

    // Тестовые зависимости
    testImplementation("junit:junit:4.13.2") // Последняя версия
    androidTestImplementation("androidx.test.ext:junit:1.3.0") // Последняя версия
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0") // Последняя версия
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.9.1") // Последняя версия
    debugImplementation("androidx.compose.ui:ui-test-manifest:1.9.1") // Последняя версия
}