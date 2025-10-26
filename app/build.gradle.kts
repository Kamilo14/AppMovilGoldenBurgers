plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("kotlin-kapt")
}


android {
    namespace = "com.example.goldenburgers"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.example.goldenburgers"
        minSdk = 33
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    // --- Room ---
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    kapt(libs.androidx.room.compiler)

    // [NUEVO] Coil para la carga de imágenes
    implementation("io.coil-kt:coil-compose:2.6.0")

    // ViewModel para MVVM en Compose
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4")

    // Navigation para la navegación entre pantallas en Compose
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // DataStore para persistencia de datos local
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    implementation("androidx.compose.material:material-icons-extended")

    // Lifecycle para observar estados de forma segura en Compose
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.4")

    implementation("io.coil-kt:coil-compose:2.5.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    // Location Services
    implementation("com.google.android.gms:play-services-location:21.0.1")

    // --- Test Dependencies ---
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}