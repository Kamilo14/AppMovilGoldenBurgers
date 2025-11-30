plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
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
    // [NUEVO] Configuración para usar JUnit5 con Kotest
    testOptions {
        unitTests.all {
            it.useJUnitPlatform()
        }
    }
}

dependencies {
    // --- Firebase ---
    implementation(platform("com.google.firebase:firebase-bom:34.6.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-analytics")

    // --- Retrofit para comunicación con API ---
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // --- Coil para la carga de imágenes ---
    implementation("io.coil-kt:coil-compose:2.6.0")

    // --- ViewModel para MVVM en Compose ---
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4")

    // --- Navigation para la navegación entre pantallas en Compose ---
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // --- DataStore para persistencia de datos local (sesión, preferencias) ---
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // --- Material Icons ---
    implementation("androidx.compose.material:material-icons-extended")

    // --- Lifecycle para observar estados de forma segura en Compose ---
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.4")

    // --- AndroidX Core ---
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    // --- Location Services ---
    implementation("com.google.android.gms:play-services-location:21.0.1")

    // --- Dependencias de Testeo ---
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    
    // [NUEVO] Dependencias para Pruebas Unitarias Modernas
    testImplementation("io.kotest:kotest-runner-junit5:5.8.0")
    testImplementation("io.kotest:kotest-assertions-core:5.8.0")
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
}

// Nota: Se eliminaron las dependencias de Room ya que ahora todo se maneja a través del backend