import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.android.junit)
    alias(libs.plugins.pitest)
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
        resValue("string", "mapbox_access_token", localProperties.getProperty("MAPBOX_ACCESS_TOKEN", ""))
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
        resValues = true
    }
}

pitest {
    pitestVersion = libs.versions.pitest
    junit5PluginVersion = libs.versions.pitestJunit5
    targetClasses = setOf("dev.rogerbertan.weatherfish.*")
    excludedClasses = setOf(
        "dev.rogerbertan.weatherfish.di.*",
        "dev.rogerbertan.weatherfish.ui.theme.*",
        "dev.rogerbertan.weatherfish.ui.WeatherFishAppKt*",
        "dev.rogerbertan.weatherfish.ui.*ScreenKt*",
        "dev.rogerbertan.weatherfish.ui.share.*Kt*",
        "dev.rogerbertan.weatherfish.ui.map.WeatherMapViewKt*",
        "dev.rogerbertan.weatherfish.ui.common.LoadStateContentKt*",
        "dev.rogerbertan.weatherfish.ui.common.PlaceholderKt*",
        "dev.rogerbertan.weatherfish.ui.common.FavoriteButtonKt*",
        "dev.rogerbertan.weatherfish.ui.common.ConditionIconKt*",
        "dev.rogerbertan.weatherfish.BuildConfig",
        "dev.rogerbertan.weatherfish.MainActivity*",
        "dev.rogerbertan.weatherfish.WeatherFishApplication*",
        "*Hilt_*",
        "*_Factory*",
        "*_HiltModules*",
        "*_MembersInjector*",
        "*_GeneratedInjector",
        "*_ComponentTreeDeps",
        "*_Impl*",
        "Dagger*",
        "hilt_aggregated_deps.*",
        "*\$\$serializer",
        "*ComposableSingletons*",
    )
    excludedTestClasses = setOf("dev.rogerbertan.weatherfish.ArchitectureTest")
    avoidCallsTo = setOf(
        "kotlin.jvm.internal",
        "kotlin.ResultKt",
        "kotlin.coroutines.jvm.internal",
        "kotlinx.coroutines",
    )
    threads = 4
    outputFormats = setOf("HTML", "XML")
    timestampedReports = false
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
    implementation(libs.mapsgl)
    implementation(libs.mapbox.maps)
    implementation(libs.mapbox.maps.compose)
    implementation(libs.ktor.client.okhttp)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.junit.jupiter.params)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.konsist)
    testImplementation(libs.mockk)
    testImplementation(libs.ktor.client.mock)
    androidTestImplementation(platform(libs.junit.bom))
    androidTestImplementation(libs.junit.jupiter.api)
    debugImplementation(libs.androidx.compose.ui.tooling)
}