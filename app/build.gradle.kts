plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-parcelize")
    id("com.google.dagger.hilt.android")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "ru.devsoland.drydrive"
    compileSdk = 36

    defaultConfig {
        applicationId = "ru.devsoland.drydrive"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        buildConfigField("String", "WEATHER_API_KEY", "\"${project.findProperty("WEATHER_API_KEY") ?: "your_api_key_here"}\"")
        buildConfigField("String", "YANDEX_MAPS_API_KEY", "\"${project.findProperty("YANDEX_MAPS_API_KEY") ?: "your_api_key_here"}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            buildConfigField("String", "WEATHER_API_KEY", "\"${project.findProperty("WEATHER_API_KEY") ?: "your_api_key_here"}\"")
            manifestPlaceholders["YANDEX_MAPS_API_KEY"] = project.findProperty("YANDEX_MAPS_API_KEY") ?: "КЛЮЧ_ЗАГЛУШКА_ЕСЛИ_НЕ_НАЙДЕН"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {

    // Import the Compose BOM
    val composeBom = platform("androidx.compose:compose-bom:2025.08.00")
    implementation(composeBom)
    androidTestImplementation(composeBom) // For UI tests

    // --- Jetpack Compose - Material 3 ---
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material3.adaptive:adaptive") // Optional - For window size utils

    // --- Jetpack Compose - Core UI & Foundation ---
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")

    // --- Jetpack Compose - Tooling & Preview ---
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest") // Often used with ui-tooling for tests

    // --- Jetpack Compose - Icons (Material 2 style icons, if still needed with M3) ---
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.material:material-icons-extended")

    // --- Jetpack Compose - Integration ---
    implementation("androidx.activity:activity-compose") // Version from BoM
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose") // Version from BoM
    implementation("androidx.compose.runtime:runtime-livedata") // Optional - For LiveData integration, version from BoM
    implementation("androidx.navigation:navigation-compose:2.9.4") // Navigation usually has its own version cycle

    // --- AndroidX Core & Lifecycle ---
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.3")

    // --- XML Material Components Theme ---
    implementation("com.google.android.material:material:1.13.0") // <<=== ВОТ ЭТА СТРОКА ВОЗВРАЩЕНА

    // --- Network ---
    implementation("com.squareup.retrofit2:retrofit:3.0.0")
    implementation("com.squareup.okhttp3:okhttp:5.1.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0")

    // --- Persistence (Room & DataStore) ---
    implementation("androidx.room:room-runtime:2.8.0")
    implementation("androidx.room:room-ktx:2.8.0")
    ksp("androidx.room:room-compiler:2.8.0")
    implementation("androidx.datastore:datastore-preferences:1.1.7")

    // --- Dependency Injection (Hilt) ---
    implementation("com.google.dagger:hilt-android:2.57.1")
    ksp("com.google.dagger:hilt-compiler:2.57.1")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0") // For ViewModel with Hilt in Compose Navigation

    // --- Coroutines ---
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")

    // --- Image Loading (Coil) ---
    implementation("io.coil-kt.coil3:coil-compose:3.3.0")

    // --- Animations (Lottie) ---
    implementation("com.airbnb.android:lottie-compose:6.6.7")

    // --- Mapping (Yandex Maps) ---
    implementation("com.yandex.android:maps.mobile:4.22.0-full")

    // --- Debugging ---
    debugImplementation("com.squareup.leakcanary:leakcanary-android:2.14")

    // --- Testing ---
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4") // Version from BoM
}
