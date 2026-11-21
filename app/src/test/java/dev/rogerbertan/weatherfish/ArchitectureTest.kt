package dev.rogerbertan.weatherfish

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.declaration.KoFileDeclaration
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ArchitectureTest {

    private fun filesInLayer(layer: String) = Konsist.scopeFromProduction().files.filter { file ->
        val root = "dev.rogerbertan.weatherfish.$layer"
        val pkg = file.packagee?.name.orEmpty()
        pkg == root || pkg.startsWith("$root.")
    }

    private fun List<KoFileDeclaration>.importing(vararg prefixes: String) =
        filter { file -> file.imports.any { import -> prefixes.any(import.name::startsWith) } }.map { it.path }

    @Test
    fun `should keep domain and ui free of data and sdk imports`() {
        // Arrange
        val files = filesInLayer("domain") + filesInLayer("ui")

        // Act
        val offenders = files.importing("dev.rogerbertan.weatherfish.data", "com.xweather", "com.mapbox")

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
}