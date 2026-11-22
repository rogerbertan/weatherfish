package dev.rogerbertan.weatherfish.ui.share

import androidx.lifecycle.SavedStateHandle
import dev.rogerbertan.weatherfish.domain.GeoPoint
import dev.rogerbertan.weatherfish.domain.UserPreferences
import dev.rogerbertan.weatherfish.domain.WeatherError
import dev.rogerbertan.weatherfish.domain.WeatherRepository
import dev.rogerbertan.weatherfish.ui.MainDispatcherExtension
import dev.rogerbertan.weatherfish.ui.otherPlace
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class ImportPlaceViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    private val preferences = mockk<UserPreferences>(relaxUnitFun = true) {
        every { favorites } returns flowOf(emptyList())
    }
    private val repository = mockk<WeatherRepository>()

    private val linkPoint = GeoPoint(-25.4284, -49.2733)

    private fun viewModel(name: String = "Curitiba Centro", lat: String = "-25.4284", lon: String = "-49.2733") =
        ImportPlaceViewModel(SavedStateHandle(mapOf("name" to name, "lat" to lat, "lon" to lon)), preferences, repository)

    @Test
    fun `should ask for confirmation without saving when link opens`() = runTest(mainDispatcher.dispatcher) {
        // Arrange
        val viewModel = viewModel()

        // Act
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 0) { repository.placeAt(any()) }
        coVerify(exactly = 0) { preferences.addFavorite(any()) }
        assertEquals(ImportState.Confirm("Curitiba Centro"), viewModel.uiState.value)
    }

    @Test
    fun `should save linked place with name and point from link when confirmed`() = runTest(mainDispatcher.dispatcher) {
        // Arrange
        coEvery { repository.placeAt(linkPoint) } returns otherPlace
        val viewModel = viewModel()

        // Act
        viewModel.save()
        advanceUntilIdle()

        // Assert
        coVerify { preferences.addFavorite(otherPlace.copy(name = "Curitiba Centro", point = linkPoint)) }
        assertEquals(ImportState.Saved("Curitiba Centro", "Paraná, Brasil"), viewModel.uiState.value)
    }

    @Test
    fun `should show already saved without saving when place is a favorite`() = runTest(mainDispatcher.dispatcher) {
        // Arrange
        every { preferences.favorites } returns flowOf(listOf(otherPlace.copy(name = "Curitiba", point = linkPoint)))
        val viewModel = viewModel()

        // Act
        viewModel.save()
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 0) { repository.placeAt(any()) }
        coVerify(exactly = 0) { preferences.addFavorite(any()) }
        assertEquals(ImportState.AlreadySaved("Curitiba"), viewModel.uiState.value)
    }

    @Test
    fun `should show invalid link when coordinates are out of range`() = runTest(mainDispatcher.dispatcher) {
        // Arrange
        val viewModel = viewModel(lat = "999")

        // Act
        viewModel.save()
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 0) { repository.placeAt(any()) }
        assertEquals(ImportState.InvalidLink, viewModel.uiState.value)
    }

    @Test
    fun `should show invalid link when name is missing`() = runTest(mainDispatcher.dispatcher) {
        // Arrange
        val viewModel = viewModel(name = "")

        // Act
        advanceUntilIdle()

        // Assert
        assertEquals(ImportState.InvalidLink, viewModel.uiState.value)
    }

    @Test
    fun `should show failure when place lookup fails`() = runTest(mainDispatcher.dispatcher) {
        // Arrange
        coEvery { repository.placeAt(linkPoint) } throws WeatherError.Network
        val viewModel = viewModel()

        // Act
        viewModel.save()
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 0) { preferences.addFavorite(any()) }
        assertEquals(ImportState.Failed, viewModel.uiState.value)
    }

    @Test
    fun `should save place when retried after failure`() = runTest(mainDispatcher.dispatcher) {
        // Arrange
        coEvery { repository.placeAt(linkPoint) } throws WeatherError.Network andThen otherPlace
        val viewModel = viewModel()
        viewModel.save()
        advanceUntilIdle()

        // Act
        viewModel.save()
        advanceUntilIdle()

        // Assert
        assertEquals(ImportState.Saved("Curitiba Centro", "Paraná, Brasil"), viewModel.uiState.value)
    }

    @Test
    fun `should request place once when save is tapped twice`() = runTest(mainDispatcher.dispatcher) {
        // Arrange
        coEvery { repository.placeAt(linkPoint) } returns otherPlace
        val viewModel = viewModel()

        // Act
        viewModel.save()
        viewModel.save()
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) { repository.placeAt(linkPoint) }
    }
}