package dev.rogerbertan.weatherfish.data

import dev.rogerbertan.weatherfish.data.location.LocationProvider
import dev.rogerbertan.weatherfish.data.settings.DataStoreUserPreferences
import dev.rogerbertan.weatherfish.domain.GeoPoint
import dev.rogerbertan.weatherfish.domain.WeatherError
import dev.rogerbertan.weatherfish.domain.WeatherRepository
import dev.rogerbertan.weatherfish.ui.otherPlace
import dev.rogerbertan.weatherfish.ui.testPlace
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DeviceActiveLocationTest {

    private val locationProvider = mockk<LocationProvider> {
        coEvery { currentLocation() } returns testPlace.point
    }
    private val repository = mockk<WeatherRepository> {
        coEvery { placeAt(testPlace.point) } returns testPlace
    }
    private val preferences = mockk<DataStoreUserPreferences>(relaxUnitFun = true) {
        coEvery { lastPlace() } returns null
    }

    private fun TestScope.activeLocation() =
        DeviceActiveLocation(locationProvider, repository, preferences, this)

    @Test
    fun `should start with last saved place before current location arrives`() = runTest {
        // Arrange
        coEvery { locationProvider.currentLocation() } coAnswers { slowLocation() }
        coEvery { preferences.lastPlace() } returns otherPlace

        // Act
        val place = activeLocation().place.first()

        // Assert
        assertEquals(otherPlace, place)
    }

    @Test
    fun `should switch to current location and save it`() = runTest {
        // Arrange
        coEvery { preferences.lastPlace() } returns otherPlace
        val activeLocation = activeLocation()
        activeLocation.place.first()

        // Act
        advanceUntilIdle()

        // Assert
        assertEquals(testPlace, activeLocation.place.first())
        coVerify { preferences.saveLastPlace(testPlace) }
    }

    @Test
    fun `should keep last saved place when location is unavailable`() = runTest {
        // Arrange
        coEvery { locationProvider.currentLocation() } returns null
        coEvery { preferences.lastPlace() } returns otherPlace
        val activeLocation = activeLocation()
        activeLocation.place.first()

        // Act
        advanceUntilIdle()

        // Assert
        assertEquals(otherPlace, activeLocation.place.first())
    }

    @Test
    fun `should keep last saved place when reverse geocoding fails`() = runTest {
        // Arrange
        coEvery { repository.placeAt(testPlace.point) } throws WeatherError.Network
        coEvery { preferences.lastPlace() } returns otherPlace
        val activeLocation = activeLocation()
        activeLocation.place.first()

        // Act
        advanceUntilIdle()

        // Assert
        assertEquals(otherPlace, activeLocation.place.first())
    }

    @Test
    fun `should start with default place when nothing is saved`() = runTest {
        // Arrange
        coEvery { locationProvider.currentLocation() } returns null

        // Act
        val place = activeLocation().place.first()

        // Assert
        assertEquals(defaultPlace, place)
    }

    @Test
    fun `should emit and save selected place`() = runTest {
        // Arrange
        val activeLocation = activeLocation()
        activeLocation.place.first()
        advanceUntilIdle()

        // Act
        activeLocation.select(otherPlace)
        advanceUntilIdle()

        // Assert
        assertEquals(otherPlace, activeLocation.place.first())
        coVerify { preferences.saveLastPlace(otherPlace) }
    }

    @Test
    fun `should switch to current location when requested`() = runTest {
        // Arrange
        coEvery { locationProvider.currentLocation() } returns null andThen testPlace.point
        val activeLocation = activeLocation()
        activeLocation.place.first()
        advanceUntilIdle()

        // Act
        activeLocation.useCurrentLocation()
        advanceUntilIdle()

        // Assert
        assertEquals(testPlace, activeLocation.place.first())
    }

    @Test
    fun `should keep selected place when current location arrives later`() = runTest {
        // Arrange
        coEvery { locationProvider.currentLocation() } coAnswers { slowLocation() }
        val activeLocation = activeLocation()
        activeLocation.place.first()

        // Act
        activeLocation.select(otherPlace)
        advanceUntilIdle()

        // Assert
        assertEquals(otherPlace, activeLocation.place.first())
        coVerify(exactly = 0) { repository.placeAt(any()) }
    }

    @Test
    fun `should report locating until current location arrives`() = runTest {
        // Arrange
        coEvery { locationProvider.currentLocation() } coAnswers { slowLocation() }
        val activeLocation = activeLocation()

        // Act
        activeLocation.place.first()
        val whileLocating = activeLocation.isLocating.first()
        advanceUntilIdle()

        // Assert
        assertEquals(true, whileLocating)
        assertEquals(false, activeLocation.isLocating.first())
    }

    @Test
    fun `should stop reporting locating when a place is selected`() = runTest {
        // Arrange
        coEvery { locationProvider.currentLocation() } coAnswers { slowLocation() }
        val activeLocation = activeLocation()
        activeLocation.place.first()

        // Act
        activeLocation.select(otherPlace)
        advanceUntilIdle()

        // Assert
        assertEquals(false, activeLocation.isLocating.first())
    }

    @Test
    fun `should locate only once for concurrent collectors`() = runTest {
        // Arrange
        coEvery { preferences.lastPlace() } coAnswers {
            delay(100)
            null
        }
        val activeLocation = activeLocation()

        // Act
        List(2) { async { activeLocation.place.first() } }.awaitAll()
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) { locationProvider.currentLocation() }
    }

    private suspend fun slowLocation(): GeoPoint {
        delay(1_000)
        return testPlace.point
    }
}
