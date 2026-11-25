package dev.rogerbertan.weatherfish

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.declaration.KoFileDeclaration
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ArchitectureTest {

    private val productionFiles = Konsist.scopeFromProduction().files

    private fun KoFileDeclaration.isInLayer(layer: String): Boolean {
        val root = "dev.rogerbertan.weatherfish.$layer"
        val pkg = packagee?.name.orEmpty()
        return pkg == root || pkg.startsWith("$root.")
    }

    private fun filesInLayer(layer: String) = productionFiles.filter { it.isInLayer(layer) }

    private fun filesOutsideLayer(layer: String) = productionFiles.filterNot { it.isInLayer(layer) }

    private fun List<KoFileDeclaration>.importing(vararg prefixes: String) =
        filter { file -> file.imports.any { import -> prefixes.any(import.name::startsWith) } }.map { it.path }

    @Test
    fun `should keep domain and ui free of data and sdk imports`() {
        // Arrange
        val files = filesInLayer("domain") + filesInLayer("ui")

        // Act
        val offenders = files.importing(
            "dev.rogerbertan.weatherfish.data",
            "com.google.gson",
            "com.google.android.gms",
            "androidx.datastore",
            "io.ktor",
            "okhttp3",
        )

        // Assert
        assertEquals(emptyList<String>(), offenders)
    }

    @Test
    fun `should import mapbox and mapsgl only in the map view`() {
        // Arrange
        val files = productionFiles.filterNot { it.name == MAP_VIEW }

        // Act
        val offenders = files.importing("com.mapbox", "com.xweather.mapsgl")

        // Assert
        assertEquals(emptyList<String>(), offenders)
    }

    @Test
    fun `should import ml kit in domain and ui only in the camera view`() {
        // Arrange
        val files = (filesInLayer("domain") + filesInLayer("ui")).filterNot { it.name == CAMERA_VIEW }

        // Act
        val offenders = files.importing("com.google.mlkit")

        // Assert
        assertEquals(emptyList<String>(), offenders)
    }

    @Test
    fun `should import camerax only in the camera view`() {
        // Arrange
        val files = productionFiles.filterNot { it.name == CAMERA_VIEW }

        // Act
        val offenders = files.importing("androidx.camera")

        // Assert
        assertEquals(emptyList<String>(), offenders)
    }

    @Test
    fun `should keep domain free of qr rendering imports`() {
        // Arrange
        val files = filesInLayer("domain")

        // Act
        val offenders = files.importing("com.google.zxing")

        // Assert
        assertEquals(emptyList<String>(), offenders)
    }

    @Test
    fun `should keep domain free of android imports`() {
        // Arrange
        val files = filesInLayer("domain")

        // Act
        val offenders = files.importing("android")

        // Assert
        assertEquals(emptyList<String>(), offenders)
    }

    @Test
    fun `should import aeris sdk only in data xweather`() {
        // Arrange
        val files = filesOutsideLayer("data.xweather")

        // Act
        val offenders = files.importing("com.aerisweather")

        // Assert
        assertEquals(emptyList<String>(), offenders)
    }

    @Test
    fun `should read xweather keys only in data xweather`() {
        // Arrange
        val files = filesOutsideLayer("data.xweather")

        // Act
        val offenders = files.filter { it.text.contains("XWEATHER_CLIENT") }.map { it.path }

        // Assert
        assertEquals(emptyList<String>(), offenders)
    }

    private companion object {
        const val CAMERA_VIEW = "QrCameraView"
        const val MAP_VIEW = "WeatherMapView"
    }
}
