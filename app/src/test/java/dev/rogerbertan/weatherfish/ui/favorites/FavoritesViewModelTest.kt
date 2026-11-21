package dev.rogerbertan.weatherfish.ui.favorites

import dev.rogerbertan.weatherfish.domain.ActiveLocation
import dev.rogerbertan.weatherfish.domain.TemperatureUnit
import dev.rogerbertan.weatherfish.domain.UserPreferences
import dev.rogerbertan.weatherfish.ui.MainDispatcherExtension
import dev.rogerbertan.weatherfish.ui.common.LoadState
import dev.rogerbertan.weatherfish.ui.testPlace
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class FavoritesViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    private val preferences = mockk<UserPreferences>(relaxUnitFun = true) {
        every { favorites } returns flowOf(listOf(testPlace))
        every { temperatureUnit } returns flowOf(TemperatureUnit.Celsius)
    }
    private val activeLocation = mockk<ActiveLocation>(relaxUnitFun = true)

    private fun viewModel() = FavoritesViewModel(preferences, activeLocation)

    @Test
    fun `should show favorites and temperature unit from preferences`() = runTest(mainDispatcher.dispatcher) {
        // Arrange
        val viewModel = viewModel()

        // Act
        advanceUntilIdle()

        // Assert
        assertEquals(
            LoadState.Loaded(FavoritesUiState(listOf(testPlace), TemperatureUnit.Celsius)),
            viewModel.uiState.value,
        )
    }

    @Test
    fun `should select favorite as active location`() = runTest(mainDispatcher.dispatcher) {
        // Arrange
        val viewModel = viewModel()

        // Act
        viewModel.select(testPlace)

        // Assert
        verify { activeLocation.select(testPlace) }
    }

    @Test
    fun `should remove favorite`() = runTest(mainDispatcher.dispatcher) {
        // Arrange
        val viewModel = viewModel()

        // Act
        viewModel.remove(testPlace)
        advanceUntilIdle()

        // Assert
        coVerify { preferences.removeFavorite(testPlace) }
    }

    @Test
    fun `should save temperature unit when changed`() = runTest(mainDispatcher.dispatcher) {
        // Arrange
        val viewModel = viewModel()

        // Act
        viewModel.setUnit(TemperatureUnit.Fahrenheit)
        advanceUntilIdle()

        // Assert
        coVerify { preferences.setTemperatureUnit(TemperatureUnit.Fahrenheit) }
    }
}
