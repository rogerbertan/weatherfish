package dev.rogerbertan.weatherfish.data.settings

import androidx.datastore.core.DataStore
import dev.rogerbertan.weatherfish.domain.TemperatureUnit
import dev.rogerbertan.weatherfish.ui.testPlace
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import java.io.IOException
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DataStoreUserPreferencesTest {

    private val dataStore = mockk<DataStore<StoredSettings>> {
        every { data } returns flowOf(StoredSettings())
    }

    private fun preferences() = DataStoreUserPreferences(dataStore)

    @Test
    fun `should keep reading settings after a read error`() = runTest {
        // Arrange
        var reads = 0
        every { dataStore.data } returns flow {
            if (reads++ == 0) throw IOException("disk")
            emit(StoredSettings(temperatureUnit = TemperatureUnit.Fahrenheit))
        }

        // Act
        val units = preferences().temperatureUnit.take(2).toList()

        // Assert
        assertEquals(listOf(TemperatureUnit.Celsius, TemperatureUnit.Fahrenheit), units)
    }

    @Test
    fun `should not throw when saving fails`() = runTest {
        // Arrange
        coEvery { dataStore.updateData(any()) } throws IOException("disk full")

        // Act
        val result = runCatching { preferences().addFavorite(testPlace) }

        // Assert
        assertTrue(result.isSuccess)
    }

    @Test
    fun `should not duplicate a favorite already saved`() = runTest {
        // Arrange
        val transform = slot<suspend (StoredSettings) -> StoredSettings>()
        coEvery { dataStore.updateData(capture(transform)) } returns StoredSettings()
        val saved = StoredSettings(favorites = listOf(testPlace.toStored()))

        // Act
        preferences().addFavorite(testPlace)
        val updated = transform.captured(saved)

        // Assert
        assertEquals(saved, updated)
    }
}
