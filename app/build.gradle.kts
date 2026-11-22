import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.android.junit)
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use(::load)
}

android {
    namespace = "dev.rogerbertan.weatherfish"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "dev.rogerbertan.weatherfish"
        minSdk = 35
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField(
            "String",
            "XWEATHER_CLIENT_ID",
            "\"${localProperties.getProperty("XWEATHER_CLIENT_ID", "")}\"",
        )
        buildConfigField(
            "String",
            "XWEATHER_CLIENT_SECRET",
            "\"${localProperties.getProperty("XWEATHER_CLIENT_SECRET", "")}\"",
        )
        buildConfigField(
            "String",
            "MAPBOX_ACCESS_TOKEN",
            "\"${localProperties.getProperty("MAPBOX_ACCESS_TOKEN", "")}\"",
        )
    }

    buildTypes {
        release {
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    ksp(libs.hilt.compiler)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.androidx.datastore)
    implementation(libs.play.services.location)
    implementation(libs.play.services.code.scanner)
    implementation(libs.zxing.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.camera.mlkit.vision)
    implementation(libs.mlkit.barcode.scanning)
    implementation(libs.aeris.core) {
        isTransitive = false
    }
    implementation(libs.gson)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.junit.jupiter.params)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.konsist)
    testImplementation(libs.mockk)
    androidTestImplementation(platform(libs.junit.bom))
    androidTestImplementation(libs.junit.jupiter.api)
    debugImplementation(libs.androidx.compose.ui.tooling)
}