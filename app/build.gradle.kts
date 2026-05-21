plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.mameen.isomessage"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.mameen.isomessage"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Configurable host for Mockoon simulator — change this in local.properties or here
        buildConfigField("String", "BASE_URL", "\"http://10.0.2.2:5000/\"")
        buildConfigField("String", "APP_VERSION", "\"1.0.0-demo\"")
    }

    buildTypes {
        debug {
            isDebuggable = true
            buildConfigField("Boolean", "ENABLE_LOGGING", "true")
        }
        release {
            isMinifyEnabled = false
            buildConfigField("Boolean", "ENABLE_LOGGING", "false")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    // ── Core ─────────────────────────────────────────────────────────────────
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // ── Jetpack Compose ───────────────────────────────────────────────────────
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    implementation(libs.androidx.navigation.compose)

    // ── Hilt (Dependency Injection) ───────────────────────────────────────────
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // ── Networking ─────────────────────────────────────────────────────────────
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    // ── Coroutines ─────────────────────────────────────────────────────────────
    implementation(libs.coroutines.android)
    implementation(libs.coroutines.core)

    // ── Serialization ──────────────────────────────────────────────────────────
    implementation(libs.gson)

    // ── Testing ────────────────────────────────────────────────────────────────
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.coroutines.test)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
