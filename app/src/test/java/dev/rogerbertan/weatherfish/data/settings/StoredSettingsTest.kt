package dev.rogerbertan.weatherfish.data.settings

import androidx.datastore.core.CorruptionException
import dev.rogerbertan.weatherfish.domain.TemperatureUnit
import dev.rogerbertan.weatherfish.ui.testPlace
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test

class StoredSettingsTest {

    @Test
    fun `should restore the same place after storing it`() {
        // Arrange
        val stored = testPlace.toStored()

        // Act
        val restored = stored.toDomain()

        // Assert
        assertEquals(testPlace, restored)
    }

    @Test
    fun `should read back the settings it wrote`() = runTest {
        // Arrange
        val settings = StoredSettings(listOf(testPlace.toStored()), testPlace.toStored(), TemperatureUnit.Fahrenheit)
        val output = ByteArrayOutputStream()
        SettingsSerializer.writeTo(settings, output)

        // Act
        val read = SettingsSerializer.readFrom(output.toByteArray().inputStream())

        // Assert
        assertEquals(settings, read)
    }

    @Test
    fun `should use defaults for missing fields`() = runTest {
        // Arrange
        val json = "{}"

        // Act
        val read = SettingsSerializer.readFrom(json.byteInputStream())

        // Assert
        assertEquals(StoredSettings(), read)
    }

    @Test
    fun `should report corruption when settings file is invalid`() = runTest {
        // Arrange
        val json = "{\"favorites\":"

        // Act
        val error = runCatching { SettingsSerializer.readFrom(json.byteInputStream()) }.exceptionOrNull()

        // Assert
        assertInstanceOf(CorruptionException::class.java, error)
    }
}
