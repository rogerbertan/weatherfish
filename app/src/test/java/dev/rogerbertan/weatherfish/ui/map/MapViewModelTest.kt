package dev.rogerbertan.weatherfish.ui.map

import dev.rogerbertan.weatherfish.domain.ActiveLocation
import dev.rogerbertan.weatherfish.domain.MapCredentials
import dev.rogerbertan.weatherfish.domain.MapCredentialsProvider
import dev.rogerbertan.weatherfish.domain.Place
import dev.rogerbertan.weatherfish.ui.MainDispatcherExtension
import dev.rogerbertan.weatherfish.ui.otherPlace
import dev.rogerbertan.weatherfish.ui.testPlace
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class MapViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    private val activePlace = MutableStateFlow<Place>(testPlace)
    private val activeLocation = mockk<ActiveLocation> {
        every { place } returns activePlace
    }
    private val credentialsProvider = mockk<MapCredentialsProvider> {
        every { mapCredentials } returns MapCredentials("id", "secret")
    }

    private fun viewModel() = MapViewModel(activeLocation, credentialsProvider)

    @Test
    fun `should center map on active place`() = runTest(mainDispatcher.dispatcher) {
        // Arrange
        val viewModel = viewModel()

        // Act
        advanceUntilIdle()

        // Assert
        assertEquals(testPlace.point, viewModel.uiState.value.center)
    }

    @Test
    fun `should move center when active place changes`() = runTest(mainDispatcher.dispatcher) {
        // Arrange
        val viewModel = viewModel()
        advanceUntilIdle()

        // Act
        activePlace.value = otherPlace
        advanceUntilIdle()

        // Assert
        assertEquals(otherPlace.point, viewModel.uiState.value.center)
    }

    @Test
    fun `should start with wind layer hidden`() = runTest(mainDispatcher.dispatcher) {
        // Arrange
        val viewModel = viewModel()

        // Act
        advanceUntilIdle()

        // Assert
        assertFalse(viewModel.uiState.value.showWind)
    }

    @Test
    fun `should show wind layer when toggled`() = runTest(mainDispatcher.dispatcher) {
        // Arrange
        val viewModel = viewModel()
        advanceUntilIdle()

        // Act
        viewModel.toggleWind()
        advanceUntilIdle()

        // Assert
        assertTrue(viewModel.uiState.value.showWind)
    }

    @Test
    fun `should hide wind layer when toggled twice`() = runTest(mainDispatcher.dispatcher) {
        // Arrange
        val viewModel = viewModel()
        advanceUntilIdle()

        // Act
        viewModel.toggleWind()
        viewModel.toggleWind()
        advanceUntilIdle()

        // Assert
        assertFalse(viewModel.uiState.value.showWind)
    }

}
